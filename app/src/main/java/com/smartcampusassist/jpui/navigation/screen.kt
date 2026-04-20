package com.smartcampusassist.jpui.navigation

import android.net.Uri

sealed class Screen(val route: String) {

    object Splash : Screen("splash")

    object Login : Screen("login")

    object SignUp : Screen("sign_up")

    object Main : Screen("main")

    object StudentHome : Screen("student_home")

    object TeacherHome : Screen("teacher_home")

    object Assistant : Screen("assistant")

    object Profile : Screen("profile")

    object AdminProvision : Screen("admin_provision")

    object CampusAdmin : Screen("campus_admin")

    object Schedule : Screen("schedule")

    object Assignments : Screen("assignments")

    object UploadAssignment : Screen("upload_assignment")

    object Reminders : Screen("reminders")

    object Events : Screen("events")

    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String): String {
            return "event_detail/${Uri.encode(eventId)}"
        }
    }

    object AssignmentViewer : Screen("assignment_viewer/{url}/{title}") {
        fun createRoute(url: String, title: String): String {
            return "assignment_viewer/${Uri.encode(url)}/${Uri.encode(title)}"
        }
    }
}
