package com.smartcampusassist.jpui.auth

data class AccountRequest(
    val email: String = "",
    val fullName: String = "",
    val role: String = "student",
    val department: String = "",
    val enrollment: String = "",
    val semester: Int = 0,
    val academicYear: String = "",
    val subject: String = "",
    val teacherId: String = "",
    val employeeId: String = "",
    val status: String = "pending",
    val requestedAt: Long = 0L,
    val approvedAt: Long = 0L,
    val approvedBy: String = "",
    val approvedUserUid: String = "",
    val rejectedAt: Long = 0L,
    val rejectedBy: String = "",
    val processingState: String = "",
    val deliveryError: String = "",
    val emailedAt: Long = 0L
)
