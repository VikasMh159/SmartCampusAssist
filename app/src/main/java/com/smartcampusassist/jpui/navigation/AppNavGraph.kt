package com.smartcampusassist.jpui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartcampusassist.jpui.assignments.AssignmentViewerScreen
import com.smartcampusassist.jpui.assignments.AssignmentsScreen
import com.smartcampusassist.jpui.assignments.UploadAssignmentScreen
import com.smartcampusassist.jpui.auth.AdminProvisionScreen
import com.smartcampusassist.jpui.auth.LoginScreen
import com.smartcampusassist.jpui.auth.SignUpScreen
import com.smartcampusassist.campus.CampusAdminScreen
import com.smartcampusassist.jpui.events.EventDetailScreen
import com.smartcampusassist.jpui.events.EventsScreen
import com.smartcampusassist.jpui.main.MainScreen
import com.smartcampusassist.jpui.main.MainScreenDestination
import com.smartcampusassist.jpui.schedule.ScheduleScreen
import com.smartcampusassist.jpui.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appViewModel: AppViewModel
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                navController = navController,
                destination = null,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Assistant.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Assistant,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Profile.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Profile,
                appViewModel = appViewModel
            )
        }

        composable(Screen.AdminProvision.route) {
            AdminProvisionScreen(
                navController = navController
            )
        }

        composable(Screen.CampusAdmin.route) {
            CampusAdminScreen(
                navController = navController
            )
        }

        composable(Screen.Schedule.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Schedule,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Assignments.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Assignments,
                appViewModel = appViewModel
            )
        }

        composable(Screen.UploadAssignment.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.UploadAssignment,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Reminders.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Reminders,
                appViewModel = appViewModel
            )
        }

        composable(Screen.Events.route) {
            MainScreen(
                navController = navController,
                destination = MainScreenDestination.Events,
                appViewModel = appViewModel
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EventDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            )
        }

        composable(
            route = Screen.AssignmentViewer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AssignmentViewerScreen(
                navController = navController,
                fileUrl = backStackEntry.arguments?.getString("url").orEmpty(),
                title = backStackEntry.arguments?.getString("title").orEmpty()
            )
        }
    }
}
