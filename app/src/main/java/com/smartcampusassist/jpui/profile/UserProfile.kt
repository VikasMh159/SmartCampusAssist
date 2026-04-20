package com.smartcampusassist.jpui.profile

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "student",
    val instituteId: String = "",
    val instituteName: String = "",
    val department: String = "",
    val branch: String = "",
    val enrollment: String = "",
    val division: String = "",
    val semester: Int = 0,
    val academicYear: String = "",
    val subject: String = "",
    val teacherId: String = "",
    val employeeId: String = "",
    val subjects: List<String> = emptyList(),
    val semesters: List<Int> = emptyList(),
    val assignedClasses: List<String> = emptyList()
)
