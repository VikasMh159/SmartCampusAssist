package com.smartcampusassist.jpui.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(forceRefresh: Boolean = false): UserProfile? {

        val currentUser = auth.currentUser ?: return null
        val currentUid = currentUser.uid

        if (!forceRefresh && cachedProfile != null && cachedUid == currentUid) {
            val isFresh = System.currentTimeMillis() - cachedAtMillis <= CACHE_TTL_MILLIS
            if (isFresh) {
                return cachedProfile
            }
        }

        val document = findUserDocument(
            uid = currentUid,
            email = currentUser.email
        ) ?: return null

        val data = document.data ?: return null

        return UserProfile(
            uid = currentUid,
            email = data["email"]?.toString()?.ifBlank { currentUser.email.orEmpty() }
                ?: currentUser.email.orEmpty(),
            fullName = data["fullName"]?.toString().orEmpty(),
            role = data["role"]?.toString()?.ifBlank { "student" } ?: "student",
            department = data["department"]?.toString().orEmpty(),
            enrollment = data["enrollment"]?.toString().orEmpty(),
            semester = data["semester"]?.toString()?.toIntOrNull() ?: 0,
            academicYear = data["academicYear"]?.toString().orEmpty(),
            subject = data["subject"]?.toString().orEmpty(),
            teacherId = data["teacherId"]?.toString().orEmpty(),
            employeeId = data["employeeId"]?.toString().orEmpty()
        ).also { profile ->
            cachedUid = currentUid
            cachedProfile = profile
            cachedAtMillis = System.currentTimeMillis()
        }
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
            return@run null
        }

        users
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    }

    private companion object {
        const val CACHE_TTL_MILLIS = 60_000L
        var cachedUid: String? = null
        var cachedProfile: UserProfile? = null
        var cachedAtMillis: Long = 0L
    }
}
