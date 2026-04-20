"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const nodemailer = require("nodemailer");

admin.initializeApp();

const SMTP_HOST = defineSecret("SMTP_HOST");
const SMTP_PORT = defineSecret("SMTP_PORT");
const SMTP_USER = defineSecret("SMTP_USER");
const SMTP_PASS = defineSecret("SMTP_PASS");
const SMTP_FROM = defineSecret("SMTP_FROM");
const SMTP_SECURE = defineSecret("SMTP_SECURE");
const ADMIN_NOTIFICATION_EMAILS = defineSecret("ADMIN_NOTIFICATION_EMAILS");

exports.notifyAdminsOfAccountRequest = onDocumentWritten(
  {
    document: "accountRequests/{requestId}",
    region: "us-central1",
    secrets: [SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, SMTP_SECURE, ADMIN_NOTIFICATION_EMAILS],
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    const requestRef = event.data?.after?.ref;

    if (!after || !requestRef || after.status !== "pending") {
      return;
    }

    if (before?.status === "pending" || after.adminNotifiedAt) {
      return;
    }

    try {
      const recipients = await resolveAdminRecipients(ADMIN_NOTIFICATION_EMAILS.value());
      if (recipients.length === 0) {
        logger.warn("No admin recipients configured for account request notification", {
          requestId: event.params.requestId,
        });
        return;
      }

      await sendAdminNotificationEmail({
        recipients,
        request: after,
      });

      await requestRef.set(
        {
          adminNotifiedAt: Date.now(),
          adminNotificationError: "",
        },
        { merge: true }
      );
    } catch (error) {
      logger.error("Failed to notify admins about account request", {
        requestId: event.params.requestId,
        error,
      });
      await requestRef.set(
        {
          adminNotificationError: error instanceof Error ? error.message : String(error),
        },
        { merge: true }
      );
      throw error;
    }
  }
);

exports.processApprovedAccountRequest = onDocumentWritten(
  {
    document: "accountRequests/{requestId}",
    region: "us-central1",
    secrets: [SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, SMTP_SECURE],
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    const requestRef = event.data?.after?.ref;

    if (!after || !requestRef) {
      return;
    }

    if (after.status !== "approved") {
      return;
    }

    if (after.processingState === "completed") {
      return;
    }

    const statusChangedToApproved = before?.status !== "approved";
    const queuedForRetry = after.processingState === "queued";
    const previouslyFailed = after.processingState === "failed";

    if (!statusChangedToApproved && !queuedForRetry && !previouslyFailed) {
      return;
    }

    const email = normalizeEmail(after.email);
    if (!email) {
      await requestRef.set(
        {
          processingState: "failed",
          deliveryError: "Request email is missing.",
        },
        { merge: true }
      );
      return;
    }

    await requestRef.set(
      {
        processingState: "processing",
        deliveryError: "",
      },
      { merge: true }
    );

    try {
      const authUser = await ensureAuthUser(email, after.fullName);
      await syncUserProfile(authUser.uid, after);

      const temporaryPassword = generateTemporaryPassword();
      await admin.auth().updateUser(authUser.uid, {
        password: temporaryPassword,
      });

      const resetLink = await admin.auth().generatePasswordResetLink(email);
      await sendApprovalEmail({
        recipient: email,
        fullName: after.fullName,
        temporaryPassword,
        resetLink,
      });

      await requestRef.set(
        {
          approvedUserUid: authUser.uid,
          emailedAt: Date.now(),
          processingState: "completed",
          deliveryError: "",
        },
        { merge: true }
      );
    } catch (error) {
      logger.error("Failed to process account approval", { email, error });
      await requestRef.set(
        {
          processingState: "failed",
          deliveryError: error instanceof Error ? error.message : String(error),
        },
        { merge: true }
      );
      throw error;
    }
  }
);

async function ensureAuthUser(email, fullName) {
  try {
    return await admin.auth().getUserByEmail(email);
  } catch (error) {
    if (error?.code !== "auth/user-not-found") {
      throw error;
    }

    return admin.auth().createUser({
      email,
      displayName: (fullName || "").trim(),
      emailVerified: false,
    });
  }
}

async function syncUserProfile(uid, request) {
  const normalizedEmail = normalizeEmail(request.email);
  const role = (request.role || "").trim() || "student";
  const instituteId = String(request.instituteId || "").trim();
  const instituteName = String(request.instituteName || "").trim();
  const branch = String(request.branch || "").trim();
  const division = String(request.division || "").trim().toUpperCase();
  const enrollment = String(request.enrollment || "").trim().toUpperCase();
  const employeeId = String(request.employeeId || "").trim().toUpperCase();
  const fullName = (request.fullName || "").trim();
  const semester = Number(request.semester || 0);
  const timestamp = Date.now();

  await admin
    .firestore()
    .collection("users")
    .doc(uid)
    .set(
      {
        uid,
        email: normalizedEmail,
        fullName,
        role,
        instituteId,
        instituteName,
        department: (request.department || "").trim(),
        branch,
        enrollment,
        division,
        semester,
        academicYear: (request.academicYear || "").trim(),
        subject: (request.subject || "").trim(),
        teacherId: (request.teacherId || "").trim(),
        employeeId,
        searchableName: fullName.toLowerCase(),
        updatedAt: timestamp,
      },
      { merge: true }
    );

  if (role === "student") {
    await admin
      .firestore()
      .collection("students")
      .doc(uid)
      .set(
        {
          userId: uid,
          instituteId,
          instituteName,
          fullName,
          searchableName: fullName.toLowerCase(),
          enrollmentNumber: enrollment,
          branch,
          semester,
          division,
          email: normalizedEmail,
          role: "student",
          status: "active",
          updatedAt: timestamp,
        },
        { merge: true }
      );
  } else {
    await admin
      .firestore()
      .collection("staff")
      .doc(uid)
      .set(
        {
          userId: uid,
          instituteId,
          instituteName,
          fullName,
          searchableName: fullName.toLowerCase(),
          email: normalizedEmail,
          employeeId,
          role,
          branch,
          subjects: request.subject ? [String(request.subject).trim()] : [],
          status: "active",
          updatedAt: timestamp,
        },
        { merge: true }
      );
  }
}

async function sendApprovalEmail({
  recipient,
  fullName,
  temporaryPassword,
  resetLink,
}) {
  const transporter = createTransporter();

  const resolvedName = (fullName || "").trim() || recipient;

  await transporter.sendMail({
    from: SMTP_FROM.value(),
    to: recipient,
    subject: "Smart Campus Assist account approved",
    text: [
      `Hello ${resolvedName},`,
      "",
      "Your Smart Campus Assist account has been approved.",
      `Login ID: ${recipient}`,
      `Temporary Password: ${temporaryPassword}`,
      "",
      "Please change your password after your first login.",
      `Password reset link: ${resetLink}`,
    ].join("\n"),
  });
}

async function sendAdminNotificationEmail({ recipients, request }) {
  const transporter = createTransporter();
  const role = String(request.role || "student").trim() || "student";
  const resolvedName = String(request.fullName || "").trim() || "Unknown user";
  const requestEmail = normalizeEmail(request.email) || "No email provided";

  await transporter.sendMail({
    from: SMTP_FROM.value(),
    to: recipients.join(", "),
    subject: `New ${capitalize(role)} account request`,
    text: [
      "A new Smart Campus Assist account request was submitted.",
      "",
      `Name: ${resolvedName}`,
      `Email: ${requestEmail}`,
      `Role: ${capitalize(role)}`,
      `Institute: ${String(request.instituteName || "").trim() || "-"}`,
      `Department: ${String(request.department || "").trim() || "-"}`,
      `Branch: ${String(request.branch || "").trim() || "-"}`,
      `Employee ID: ${String(request.employeeId || "").trim() || "-"}`,
      `Teacher ID: ${String(request.teacherId || "").trim() || "-"}`,
      `Enrollment: ${String(request.enrollment || "").trim() || "-"}`,
      `Semester: ${Number(request.semester || 0) || "-"}`,
      `Division: ${String(request.division || "").trim() || "-"}`,
      `Academic Year: ${String(request.academicYear || "").trim() || "-"}`,
      "",
      "Open the admin requests screen in Smart Campus Assist to review it.",
    ].join("\n"),
  });
}

function createTransporter() {
  return nodemailer.createTransport({
    host: SMTP_HOST.value(),
    port: Number(SMTP_PORT.value()),
    secure: parseBoolean(SMTP_SECURE.value()),
    auth: {
      user: SMTP_USER.value(),
      pass: SMTP_PASS.value(),
    },
  });
}

async function resolveAdminRecipients(configuredRecipients) {
  const configured = String(configuredRecipients || "")
    .split(/[,\n;]+/)
    .map((value) => normalizeEmail(value))
    .filter(Boolean);

  const adminSnapshot = await admin
    .firestore()
    .collection("users")
    .where("role", "==", "admin")
    .get();

  const dynamic = adminSnapshot.docs
    .map((document) => normalizeEmail(document.get("email")))
    .filter(Boolean);

  return [...new Set([...configured, ...dynamic])];
}

function generateTemporaryPassword(length = 10) {
  const charset = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
  let value = "";

  for (let index = 0; index < length; index += 1) {
    const randomIndex = Math.floor(Math.random() * charset.length);
    value += charset[randomIndex];
  }

  return value;
}

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function parseBoolean(value) {
  return ["1", "true", "yes", "on"].includes(String(value || "").trim().toLowerCase());
}

function capitalize(value) {
  const normalized = String(value || "").trim();
  if (!normalized) {
    return "";
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}
