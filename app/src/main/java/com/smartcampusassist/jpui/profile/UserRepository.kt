package com.smartcampusassist.jpui.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcampusassist.campus.CampusCollections
import com.smartcampusassist.campus.CampusRoles
import com.smartcampusassist.campus.StaffRecord
import com.smartcampusassist.campus.StudentRecord
import com.smartcampusassist.campus.TeacherAssignment
import com.smartcampusassist.jpui.session.SessionManager
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val defaultInstituteName = "SETI"

    suspend fun getUserProfile(forceRefresh: Boolean = false): UserProfile? {

        val currentUser = auth.currentUser ?: return null
        val currentUid = currentUser.uid
        val normalizedEmail = currentUser.email?.trim()?.lowercase().orEmpty()
        val sessionState = SessionManager.sessionState.value

        if (!forceRefresh && cachedProfile != null && cachedUid == currentUid) {
            val isFresh = System.currentTimeMillis() - cachedAtMillis <= CACHE_TTL_MILLIS
            if (isFresh) {
                return cachedProfile
            }
        }

        val document = findUserDocument(
            uid = currentUid,
            email = normalizedEmail
        ) ?: return null

        val data = document.data ?: return null
        val role = data["role"]?.toString()?.ifBlank { CampusRoles.STUDENT } ?: CampusRoles.STUDENT
        val instituteId = data["instituteId"]?.toString().orEmpty()
        val teacherId = data["teacherId"]?.toString().orEmpty()
        val employeeId = data["employeeId"]?.toString().orEmpty()

        val studentRecord = if (role == CampusRoles.STUDENT) {
            runCatching { loadStudentRecord(currentUid, normalizedEmail) }.getOrNull()
        } else {
            null
        }
        val staffRecord = if (role != CampusRoles.STUDENT) {
            runCatching { loadStaffRecord(currentUid, normalizedEmail) }.getOrNull()
        } else {
            null
        }
        val assignmentMappings = if (role == CampusRoles.TEACHER || role == CampusRoles.HOD) {
            runCatching {
                loadTeacherAssignments(
                    uid = currentUid,
                    instituteId = instituteId.ifBlank { staffRecord?.instituteId.orEmpty() },
                    fullName = data["fullName"]?.toString().orEmpty(),
                    teacherId = teacherId,
                    employeeId = employeeId
                )
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val resolvedInstituteId = instituteId
            .ifBlank { studentRecord?.instituteId.orEmpty() }
            .ifBlank { staffRecord?.instituteId.orEmpty() }
            .ifBlank { sessionState.instituteId.orEmpty() }
        val resolvedInstituteName = data["instituteName"]?.toString().orEmpty()
            .ifBlank { studentRecord?.instituteName.orEmpty() }
            .ifBlank { staffRecord?.instituteName.orEmpty() }
            .ifBlank { sessionState.instituteName.orEmpty() }
            .ifBlank { runCatching { loadInstituteName(resolvedInstituteId) }.getOrDefault("") }
            .ifBlank { defaultInstituteName }
        val resolvedBranch = data["branch"]?.toString().orEmpty()
            .ifBlank { studentRecord?.branch.orEmpty() }
            .ifBlank { assignmentMappings.map { it.branch.trim() }.firstOrNull { it.isNotBlank() }.orEmpty() }
        val subjectOptions = buildSet {
            data["subject"]?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            staffRecord?.subjects.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.forEach(::add)
            assignmentMappings.map { it.subjectTitle.trim().ifBlank { it.subjectCode.trim() } }
                .filter { it.isNotBlank() }
                .forEach(::add)
        }.toList()
        val semesterOptions = assignmentMappings.map { it.semester }.filter { it > 0 }.distinct().sorted()
        val classOptions = assignmentMappings.map { it.className.trim() }.filter { it.isNotBlank() }.distinct().sorted()

        return UserProfile(
            uid = currentUid,
            email = data["email"]?.toString()?.ifBlank { currentUser.email.orEmpty() }
                ?: currentUser.email.orEmpty(),
            fullName = data["fullName"]?.toString().orEmpty(),
            role = role,
            instituteId = resolvedInstituteId,
            instituteName = resolvedInstituteName,
            department = data["department"]?.toString().orEmpty()
                .ifBlank { staffRecord?.department.orEmpty() },
            branch = resolvedBranch,
            enrollment = data["enrollment"]?.toString().orEmpty()
                .ifBlank { studentRecord?.enrollmentNumber.orEmpty() },
            division = data["division"]?.toString().orEmpty()
                .ifBlank { studentRecord?.division.orEmpty() },
            semester = data["semester"]?.toString()?.toIntOrNull()
                ?: studentRecord?.semester
                ?: semesterOptions.firstOrNull()
                ?: 0,
            academicYear = data["academicYear"]?.toString().orEmpty(),
            subject = data["subject"]?.toString().orEmpty()
                .ifBlank { subjectOptions.firstOrNull().orEmpty() },
            teacherId = teacherId,
            employeeId = employeeId,
            subjects = subjectOptions,
            semesters = semesterOptions,
            assignedClasses = classOptions
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

    private suspend fun loadStudentRecord(uid: String, email: String): StudentRecord? {
        val directMatch = firestore.collection(CampusCollections.STUDENTS)
            .whereEqualTo("userId", uid)
            .limit(1)
            .get()
            .await()
            .toObjects(StudentRecord::class.java)
            .firstOrNull()
        if (directMatch != null) return directMatch
        if (email.isBlank()) return null

        return firestore.collection(CampusCollections.STUDENTS)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
            .toObjects(StudentRecord::class.java)
            .firstOrNull()
    }

    private suspend fun loadStaffRecord(uid: String, email: String): StaffRecord? {
        val directMatch = firestore.collection(CampusCollections.STAFF)
            .whereEqualTo("userId", uid)
            .limit(1)
            .get()
            .await()
            .toObjects(StaffRecord::class.java)
            .firstOrNull()
        if (directMatch != null) return directMatch
        if (email.isBlank()) return null

        return firestore.collection(CampusCollections.STAFF)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
            .toObjects(StaffRecord::class.java)
            .firstOrNull()
    }

    private suspend fun loadInstituteName(instituteId: String): String {
        if (instituteId.isBlank()) return ""
        return firestore.collection(CampusCollections.INSTITUTES)
            .document(instituteId)
            .get()
            .await()
            .getString("name")
            .orEmpty()
    }

    private suspend fun loadTeacherAssignments(
        uid: String,
        instituteId: String,
        fullName: String,
        teacherId: String,
        employeeId: String
    ): List<TeacherAssignment> {
        val identifiers = listOf(uid, teacherId, employeeId).map { it.trim() }.filter { it.isNotBlank() }.distinct()

        val directMatches = if (identifiers.isEmpty()) {
            emptyList()
        } else {
            identifiers.chunked(10).flatMap { chunk ->
                firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
                    .whereIn("teacherId", chunk)
                    .get()
                    .await()
                    .toObjects(TeacherAssignment::class.java)
            }
        }

        val fallbackMatches = if (directMatches.isEmpty() && instituteId.isNotBlank() && fullName.trim().isNotBlank()) {
            firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
                .whereEqualTo("instituteId", instituteId)
                .get()
                .await()
                .toObjects(TeacherAssignment::class.java)
                .filter { assignment ->
                    assignment.teacherName.trim().equals(fullName.trim(), ignoreCase = true)
                }
        } else {
            emptyList()
        }

        return (directMatches + fallbackMatches).distinctBy { it.id }
    }

    private companion object {
        const val CACHE_TTL_MILLIS = 60_000L
        var cachedUid: String? = null
        var cachedProfile: UserProfile? = null
        var cachedAtMillis: Long = 0L
    }
}
