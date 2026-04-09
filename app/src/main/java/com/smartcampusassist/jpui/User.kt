package com.smartcampusassist.jpui.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val enrollment: String = "",
    val department: String = "",
    val semester: Int = 0,
    val semesterStart: String = "",
    val semesterEnd: String = "",
    val academicYear: String = "",
    val role: String = ""   // student / teacher
)