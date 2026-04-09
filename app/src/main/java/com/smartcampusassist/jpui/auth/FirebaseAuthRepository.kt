package com.smartcampusassist.jpui.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.smartcampusassist.BuildConfig
import kotlinx.coroutines.tasks.await

data class AuthorizedSession(
    val user: FirebaseUser,
    val role: String
)

class UnauthorizedAccountException(message: String) : Exception(message)

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /* ---------------- GET USER ROLE ---------------- */

    suspend fun getUserRole(uid: String): String {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.uid != uid) {
            throw UnauthorizedAccountException("Your session is no longer valid. Please sign in again.")
        }

        return authorizeCurrentUser().role
    }

    /* ---------------- EMAIL LOGIN ---------------- */

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): AuthorizedSession {
        val normalizedEmail = email.trim()
        val result = auth
            .signInWithEmailAndPassword(normalizedEmail, password)
            .await()

        val user = result.user
            ?: throw Exception("User not found")

        return authorizeSignedInUser(user)
    }

    suspend fun submitAccountRequest(input: AccountProfileInput) {
        val normalizedEmail = input.normalizedEmail()
        val existingUser = firestore.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (existingUser != null) {
            throw Exception("An account already exists for this email.")
        }

        val requestDocument = firestore.collection("accountRequests")
            .document(normalizedEmail)
            .get()
            .await()

        val currentStatus = requestDocument.getString("status").orEmpty()
        if (requestDocument.exists() && currentStatus == "pending") {
            throw Exception("A request for this email is already pending admin approval.")
        }

        val payload = hashMapOf<String, Any>(
            "email" to normalizedEmail,
            "fullName" to input.fullName.trim(),
            "role" to input.role.trim().ifBlank { "student" },
            "department" to input.department.trim(),
            "enrollment" to input.enrollment.trim(),
            "semester" to input.semester,
            "academicYear" to input.academicYear.trim(),
            "subject" to input.subject.trim(),
            "teacherId" to input.teacherId.trim(),
            "employeeId" to input.employeeId.trim(),
            "status" to "pending",
            "requestedAt" to System.currentTimeMillis(),
            "approvedAt" to 0L,
            "approvedBy" to "",
            "rejectedAt" to 0L,
            "rejectedBy" to ""
        )

        firestore.collection("accountRequests")
            .document(normalizedEmail)
            .set(payload, SetOptions.merge())
            .await()
    }

    /* ---------------- GOOGLE LOGIN ---------------- */

    suspend fun loginWithGoogle(
        idToken: String
    ): AuthorizedSession {

        val credential = GoogleAuthProvider
            .getCredential(idToken, null)

        val result = auth
            .signInWithCredential(credential)
            .await()

        val user = result.user
            ?: throw Exception("Google login failed")

        return authorizeSignedInUser(user)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        ensureEmailIsAuthorized(email)
        auth
            .sendPasswordResetEmail(email.trim())
            .await()
    }

    suspend fun restoreAuthorizedSession(): AuthorizedSession {
        auth.currentUser
            ?: throw UnauthorizedAccountException("No active session found.")
        return authorizeCurrentUser()
    }

    fun observeManageableAccountRequests(
        onChange: (List<AccountRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("accountRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(Exception(error))
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        document.toObject(AccountRequest::class.java)
                    }
                    .filter { request ->
                        request.status == "pending" ||
                            (request.status == "approved" && request.processingState != "completed")
                    }
                    .sortedByDescending { it.requestedAt }

                onChange(requests)
            }
    }

    suspend fun approveAccountRequest(request: AccountRequest) {
        val normalizedEmail = request.email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            throw Exception("Request email is missing.")
        }

        firestore.collection("accountRequests")
            .document(normalizedEmail)
            .set(
                mapOf(
                    "status" to "approved",
                    "approvedAt" to System.currentTimeMillis(),
                    "approvedBy" to auth.currentUser?.email.orEmpty(),
                    "processingState" to "queued",
                    "deliveryError" to ""
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun rejectAccountRequest(request: AccountRequest) {
        val normalizedEmail = request.email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            throw Exception("Request email is missing.")
        }

        firestore.collection("accountRequests")
            .document(normalizedEmail)
            .set(
                mapOf(
                    "status" to "rejected",
                    "rejectedAt" to System.currentTimeMillis(),
                    "rejectedBy" to auth.currentUser?.email.orEmpty()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun retryAccountRequestDelivery(request: AccountRequest) {
        val normalizedEmail = request.email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            throw Exception("Request email is missing.")
        }

        firestore.collection("accountRequests")
            .document(normalizedEmail)
            .set(
                mapOf(
                    "status" to "approved",
                    "processingState" to "queued",
                    "deliveryError" to ""
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun findUserDocument(
        uid: String,
        email: String?
    ) = run {
        val users = firestore.collection("users")
        val normalizedEmail = email?.trim()?.lowercase().orEmpty()

        val uidDocument = users
            .document(uid)
            .get()
            .await()

        if (uidDocument.exists()) {
            return@run uidDocument
        }

        if (normalizedEmail.isBlank()) {
            return@run uidDocument
        }

        val emailDocument = users
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        emailDocument ?: uidDocument
    }

    private suspend fun syncUserDocument(
        uid: String,
        input: AccountProfileInput,
        preserveExistingData: Boolean = false
    ) {
        val users = firestore.collection("users")
        val normalizedEmail = input.normalizedEmail()
        val uidDocument = users.document(uid).get().await()
        val emailDocument = if (normalizedEmail.isBlank()) {
            null
        } else {
            users
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
        }

        val baseData = when {
            uidDocument.exists() -> uidDocument.data.orEmpty()
            emailDocument != null -> emailDocument.data.orEmpty()
            else -> emptyMap()
        }

        val mergedData = hashMapOf<String, Any>(
            "uid" to uid,
            "email" to chooseValue(
                preferred = normalizedEmail,
                existing = baseData["email"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "fullName" to chooseValue(
                preferred = input.fullName,
                existing = baseData["fullName"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "role" to chooseValue(
                preferred = input.role.ifBlank { roleForEmail(normalizedEmail) },
                existing = baseData["role"]?.toString(),
                default = roleForEmail(normalizedEmail),
                preserveExistingData = preserveExistingData
            ),
            "department" to chooseValue(
                preferred = input.department,
                existing = baseData["department"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "enrollment" to chooseValue(
                preferred = input.enrollment,
                existing = baseData["enrollment"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "semester" to chooseSemester(
                preferred = input.semester,
                existing = baseData["semester"],
                preserveExistingData = preserveExistingData
            ),
            "academicYear" to chooseValue(
                preferred = input.academicYear,
                existing = baseData["academicYear"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "subject" to chooseValue(
                preferred = input.subject,
                existing = baseData["subject"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "teacherId" to chooseValue(
                preferred = input.teacherId,
                existing = baseData["teacherId"]?.toString(),
                preserveExistingData = preserveExistingData
            ),
            "employeeId" to chooseValue(
                preferred = input.employeeId,
                existing = baseData["employeeId"]?.toString(),
                preserveExistingData = preserveExistingData
            )
        )

        users
            .document(uid)
            .set(mergedData, SetOptions.merge())
            .await()

        if (emailDocument != null && emailDocument.id != uid) {
            emailDocument.reference
                .set(
                    mapOf(
                        "uid" to uid,
                        "email" to normalizedEmail
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    private fun chooseValue(
        preferred: String,
        existing: String?,
        default: String = "",
        preserveExistingData: Boolean = false
    ): String {
        val cleanedExisting = existing?.trim().orEmpty()
        val cleanedPreferred = preferred.trim()

        if (preserveExistingData && cleanedExisting.isNotBlank()) {
            return cleanedExisting
        }

        return cleanedPreferred.ifBlank {
            cleanedExisting.ifBlank { default }
        }
    }

    private fun chooseSemester(
        preferred: Int,
        existing: Any?,
        preserveExistingData: Boolean = false
    ): Int {
        val existingSemester = existing?.toString()?.toIntOrNull() ?: 0

        if (preserveExistingData && existingSemester > 0) return existingSemester
        if (preferred > 0) return preferred
        return existingSemester
    }

    /* ---------------- CURRENT USER ---------------- */

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /* ---------------- LOGOUT ---------------- */

    fun logout() {
        auth.signOut()
    }

    private suspend fun authorizeCurrentUser(): AuthorizedSession {
        val currentUser = auth.currentUser
            ?: throw UnauthorizedAccountException("No active session found.")

        return authorizeSignedInUser(currentUser)
    }

    private suspend fun authorizeSignedInUser(user: FirebaseUser): AuthorizedSession {
        try {
            val normalizedEmail = user.email?.trim()?.lowercase().orEmpty()
            if (normalizedEmail.isBlank()) {
                throw UnauthorizedAccountException("This account does not have a valid email address.")
            }

            val userDocument = findUserDocument(
                uid = user.uid,
                email = normalizedEmail
            )

            val adminEmail = BuildConfig.ALLOWED_LOGIN_EMAIL.trim().lowercase()
            if (adminEmail.isNotBlank() && normalizedEmail == adminEmail) {
                ensureAdminProfile(user, userDocument)
                return AuthorizedSession(user = user, role = "admin")
            }

            if (userDocument.exists()) {
                validateUserDocumentOwnership(
                    document = userDocument,
                    user = user,
                    normalizedEmail = normalizedEmail
                )

                val role = userDocument.getString("role")
                    ?.trim()
                    ?.ifBlank { null }
                    ?: "student"

                return AuthorizedSession(user = user, role = role)
            }

            val requestDocument = firestore.collection("accountRequests")
                .document(normalizedEmail)
                .get()
                .await()

            val status = requestDocument.getString("status").orEmpty()
            when (status) {
                "pending" -> throw UnauthorizedAccountException(
                    "Your account request is still pending admin approval."
                )

                "rejected" -> throw UnauthorizedAccountException(
                    "This account request was rejected. Contact the admin."
                )

                "approved" -> {
                    syncApprovedUserDocument(user, requestDocument)
                    val approvedRole = requestDocument.getString("role")
                        ?.trim()
                        ?.ifBlank { null }
                        ?: "student"
                    return AuthorizedSession(user = user, role = approvedRole)
                }
            }

            throw UnauthorizedAccountException(
                "This email is not approved for Smart Campus Assist access."
            )
        } catch (error: UnauthorizedAccountException) {
            auth.signOut()
            throw error
        }
    }

    private suspend fun ensureEmailIsAuthorized(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            throw UnauthorizedAccountException("Enter a valid email address.")
        }

        val adminEmail = BuildConfig.ALLOWED_LOGIN_EMAIL.trim().lowercase()
        if (adminEmail.isNotBlank() && normalizedEmail == adminEmail) {
            return
        }

        val userDocument = firestore.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (userDocument != null) {
            return
        }

        val requestDocument = firestore.collection("accountRequests")
            .document(normalizedEmail)
            .get()
            .await()

        if (requestDocument.getString("status") == "approved") {
            return
        }

        throw UnauthorizedAccountException(
            "This email is not approved for Smart Campus Assist access."
        )
    }

    private suspend fun ensureAdminProfile(
        user: FirebaseUser,
        userDocument: DocumentSnapshot
    ) {
        syncUserDocument(
            uid = user.uid,
            input = AccountProfileInput(
                fullName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                role = "admin"
            ),
            preserveExistingData = userDocument.exists()
        )
    }

    private fun validateUserDocumentOwnership(
        document: DocumentSnapshot,
        user: FirebaseUser,
        normalizedEmail: String
    ) {
        val storedEmail = document.getString("email")?.trim()?.lowercase().orEmpty()
        if (storedEmail.isNotBlank() && storedEmail != normalizedEmail) {
            throw UnauthorizedAccountException(
                "This login email does not match the approved campus account."
            )
        }

        val storedUid = document.getString("uid")?.trim().orEmpty()
        if (storedUid.isNotBlank() && storedUid != user.uid) {
            throw UnauthorizedAccountException(
                "This email is already linked to another approved account."
            )
        }
    }

    private suspend fun syncApprovedUserDocument(
        user: FirebaseUser,
        requestDocument: DocumentSnapshot
    ) {
        val normalizedEmail = user.email?.trim()?.lowercase().orEmpty()
        val approvedUid = requestDocument.getString("approvedUserUid")?.trim().orEmpty()
        if (approvedUid.isNotBlank() && approvedUid != user.uid) {
            throw UnauthorizedAccountException(
                "This email is already linked to another approved account."
            )
        }

        syncUserDocument(
            uid = user.uid,
            input = AccountProfileInput(
                fullName = requestDocument.getString("fullName").orEmpty(),
                email = normalizedEmail,
                role = requestDocument.getString("role").orEmpty(),
                department = requestDocument.getString("department").orEmpty(),
                enrollment = requestDocument.getString("enrollment").orEmpty(),
                semester = requestDocument.getLong("semester")?.toInt() ?: 0,
                academicYear = requestDocument.getString("academicYear").orEmpty(),
                subject = requestDocument.getString("subject").orEmpty(),
                teacherId = requestDocument.getString("teacherId").orEmpty(),
                employeeId = requestDocument.getString("employeeId").orEmpty()
            )
        )
    }

    private fun roleForEmail(email: String?): String {
        val normalizedEmail = email?.trim()?.lowercase().orEmpty()
        val adminEmail = BuildConfig.ALLOWED_LOGIN_EMAIL.trim().lowercase()
        return if (adminEmail.isNotBlank() && normalizedEmail == adminEmail) {
            "admin"
        } else {
            "student"
        }
    }
}
