package com.smartcampusassist.jpui.profile

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "student",
    val department: String = "",
    val enrollment: String = "",
    val semester: Int = 0,
    val academicYear: String = "",
    val subject: String = "",
    val teacherId: String = "",
    val employeeId: String = ""
)
