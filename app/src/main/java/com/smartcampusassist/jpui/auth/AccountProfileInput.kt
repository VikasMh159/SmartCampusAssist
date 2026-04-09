package com.smartcampusassist.jpui.auth

data class AccountProfileInput(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "",
    val department: String = "",
    val enrollment: String = "",
    val semester: Int = 0,
    val academicYear: String = "",
    val subject: String = "",
    val teacherId: String = "",
    val employeeId: String = ""
) {
    fun normalizedEmail(): String = email.trim().lowercase()
}
