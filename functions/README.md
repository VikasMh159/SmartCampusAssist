# Cloud Functions Setup

This function listens to Firestore `accountRequests/{requestId}` documents.

Project:
- Firebase project id: `smartcampusassist`

Flow:
- App writes a request with `status = pending`.
- Cloud Function sends a notification email to admins for every new pending request.
- Admin approves it in the Android app.
- App updates the request to `status = approved`.
- Cloud Function creates or reuses the Firebase Auth user.
- Cloud Function syncs the `users/{uid}` profile.
- Cloud Function sends the approval email automatically.

Required secrets:

```bash
firebase functions:secrets:set SMTP_HOST
firebase functions:secrets:set SMTP_PORT
firebase functions:secrets:set SMTP_USER
firebase functions:secrets:set SMTP_PASS
firebase functions:secrets:set SMTP_FROM
firebase functions:secrets:set SMTP_SECURE
firebase functions:secrets:set ADMIN_NOTIFICATION_EMAILS
```

Example values:
- `SMTP_HOST`: `smtp.gmail.com`
- `SMTP_PORT`: `465`
- `SMTP_SECURE`: `true`
- `SMTP_FROM`: `Smart Campus Assist <no-reply@yourdomain.com>`
- `ADMIN_NOTIFICATION_EMAILS`: comma-separated admin emails like `admin1@college.edu,admin2@college.edu`
- If you are using Gmail, use an App Password instead of your normal password.

Deploy:

```bash
firebase login
firebase use smartcampusassist

firebase functions:secrets:set SMTP_HOST
firebase functions:secrets:set SMTP_PORT
firebase functions:secrets:set SMTP_USER
firebase functions:secrets:set SMTP_PASS
firebase functions:secrets:set SMTP_FROM
firebase functions:secrets:set SMTP_SECURE
firebase functions:secrets:set ADMIN_NOTIFICATION_EMAILS

cd functions
npm install
cd ..
firebase deploy --only functions,firestore:rules
```

One-command Windows setup:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-firebase-functions.ps1
```

Retry failed email:
- If any request gets `processingState = failed`, set `processingState` to `queued` in the same `accountRequests/{email}` document. The function will retry automatically.
