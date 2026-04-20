package com.smartcampusassist.campus

import com.google.firebase.firestore.DocumentId

object CampusCollections {
    const val INSTITUTES = "institutes"
    const val USERS = "users"
    const val STUDENTS = "students"
    const val STAFF = "staff"
    const val CLASSES = "classes"
    const val SUBJECTS = "subjects"
    const val ATTENDANCE = "attendance"
    const val NOTICES = "notices"
    const val TEACHER_ASSIGNMENTS = "teacherAssignments"
}

object CampusRoles {
    const val ADMIN = "admin"
    const val PRINCIPAL = "principal"
    const val TEACHER = "teacher"
    const val PEON = "peon"
    const val HOD = "hod"
    const val CLERK = "clerk"
    const val STUDENT = "student"

    val privilegedRoles = setOf(ADMIN, PRINCIPAL, HOD, CLERK)
    val staffRoles = setOf(PRINCIPAL, TEACHER, ADMIN, HOD, CLERK)
}

data class CampusInstitute(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val type: String = "",
    val status: String = "active",
    val studentCount: Long = 0,
    val staffCount: Long = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CampusUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = CampusRoles.STUDENT,
    val instituteId: String = "",
    val instituteName: String = "",
    val department: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val division: String = "",
    val enrollmentNumber: String = "",
    val employeeId: String = "",
    val searchableName: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class StudentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val instituteId: String = "",
    val instituteName: String = "",
    val fullName: String = "",
    val searchableName: String = "",
    val enrollmentNumber: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val division: String = "",
    val email: String = "",
    val role: String = CampusRoles.STUDENT,
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class StaffRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val instituteId: String = "",
    val instituteName: String = "",
    val fullName: String = "",
    val searchableName: String = "",
    val email: String = "",
    val employeeId: String = "",
    val role: String = CampusRoles.TEACHER,
    val department: String = "",
    val subjects: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CampusClass(
    @DocumentId val id: String = "",
    val instituteId: String = "",
    val instituteName: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val division: String = "",
    val className: String = "",
    val searchableName: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SubjectRecord(
    @DocumentId val id: String = "",
    val instituteId: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val code: String = "",
    val title: String = "",
    val searchableTitle: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class TeacherAssignment(
    @DocumentId val id: String = "",
    val instituteId: String = "",
    val instituteName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val searchableTeacherName: String = "",
    val staffRole: String = CampusRoles.TEACHER,
    val classId: String = "",
    val className: String = "",
    val subjectId: String = "",
    val subjectCode: String = "",
    val subjectTitle: String = "",
    val semester: Int = 0,
    val division: String = "",
    val branch: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class NoticeRecord(
    @DocumentId val id: String = "",
    val instituteId: String = "",
    val title: String = "",
    val body: String = "",
    val audienceRoles: List<String> = emptyList(),
    val status: String = "active",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class StudentFilter(
    val instituteId: String = "",
    val query: String = "",
    val enrollmentNumber: String = "",
    val branch: String = "",
    val semester: Int? = null,
    val division: String = ""
)

data class StaffFilter(
    val instituteId: String = "",
    val query: String = "",
    val role: String = "",
    val department: String = ""
)

data class AssignmentFilter(
    val instituteId: String = "",
    val teacherName: String = "",
    val role: String = "",
    val branch: String = "",
    val semester: Int? = null
)

data class PageResult<T>(
    val items: List<T>,
    val lastVisibleId: String? = null,
    val hasMore: Boolean = false
)

fun String.toSearchToken(): String = trim().lowercase()
