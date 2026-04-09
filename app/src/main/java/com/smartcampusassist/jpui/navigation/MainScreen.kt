package com.smartcampusassist.jpui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.smartcampusassist.jpui.assignments.AssignmentsScreen
import com.smartcampusassist.jpui.assignments.UploadAssignmentScreen
import com.smartcampusassist.jpui.assistant.ChatScreen
import com.smartcampusassist.jpui.components.BottomBar
import com.smartcampusassist.jpui.events.EventsScreen
import com.smartcampusassist.jpui.home.StudentHomeScreen
import com.smartcampusassist.jpui.home.TeacherHomeScreen
import com.smartcampusassist.jpui.navigation.AppViewModel
import com.smartcampusassist.jpui.navigation.Screen
import com.smartcampusassist.jpui.profile.ProfileScreen
import com.smartcampusassist.jpui.reminders.ReminderScreen
import com.smartcampusassist.jpui.schedule.ScheduleScreen

@Composable
fun MainScreen(
    navController: NavHostController,
    destination: MainScreenDestination? = null,
    appViewModel: AppViewModel
) {
    val sessionState by appViewModel.sessionState.collectAsStateWithLifecycle()
    val selectedMainDestination by appViewModel.mainDestination.collectAsStateWithLifecycle()
    val role = sessionState.role
    val activeDestination = selectedMainDestination

    LaunchedEffect(role) {
        if (role == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Main.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(destination) {
        if (destination != null) {
            appViewModel.setMainDestination(destination)
        }
    }

    val onSectionBack: () -> Unit = {
        if (destination != null) {
            navController.popBackStack()
        } else {
            appViewModel.setMainDestination(MainScreenDestination.Home)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomBar(
                currentRole = role,
                selectedDestination = selectedMainDestination,
                onDestinationSelected = { selectedDestination ->
                    appViewModel.setMainDestination(selectedDestination)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (role == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                return@Box
            }

            when (activeDestination) {
                MainScreenDestination.Home -> {
                    if (role == "teacher") {
                        TeacherHomeScreen(
                            onOpenSchedule = {
                                appViewModel.setMainDestination(MainScreenDestination.Schedule)
                            },
                            onOpenUploadAssignment = {
                                appViewModel.setMainDestination(MainScreenDestination.UploadAssignment)
                            },
                            onOpenReminders = {
                                appViewModel.setMainDestination(MainScreenDestination.Reminders)
                            },
                            onOpenEvents = {
                                appViewModel.setMainDestination(MainScreenDestination.Events)
                            },
                            onOpenAdminProvision = {
                                navController.navigate(Screen.AdminProvision.route)
                            },
                            onOpenAssistant = {
                                appViewModel.setMainDestination(MainScreenDestination.Assistant)
                            },
                            onOpenProfile = {
                                appViewModel.setMainDestination(MainScreenDestination.Profile)
                            }
                        )
                    } else {
                        StudentHomeScreen(
                            onOpenSchedule = {
                                appViewModel.setMainDestination(MainScreenDestination.Schedule)
                            },
                            onOpenAssignments = {
                                appViewModel.setMainDestination(MainScreenDestination.Assignments)
                            },
                            onOpenReminders = {
                                appViewModel.setMainDestination(MainScreenDestination.Reminders)
                            },
                            onOpenEvents = {
                                appViewModel.setMainDestination(MainScreenDestination.Events)
                            },
                            onOpenAssistant = {
                                appViewModel.setMainDestination(MainScreenDestination.Assistant)
                            },
                            onOpenProfile = {
                                appViewModel.setMainDestination(MainScreenDestination.Profile)
                            }
                        )
                    }
                }

                MainScreenDestination.Assistant -> {
                    ChatScreen(navController)
                }

                MainScreenDestination.Profile -> {
                    ProfileScreen(
                        onOpenAdminProvision = {
                            navController.navigate(Screen.AdminProvision.route)
                        },
                        onLogout = {
                            appViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                MainScreenDestination.Schedule -> {
                    ScheduleScreen(
                        navController = navController,
                        onBack = onSectionBack
                    )
                }

                MainScreenDestination.Assignments -> {
                    if (role == "student" || role == "teacher") {
                        AssignmentsScreen(
                            navController = navController,
                            onBack = onSectionBack
                        )
                    } else {
                        AccessDeniedMessage("Assignments screen is unavailable for this account.")
                    }
                }

                MainScreenDestination.UploadAssignment -> {
                    if (role == "teacher") {
                        UploadAssignmentScreen(
                            navController = navController,
                            onBack = onSectionBack
                        )
                    } else {
                        AccessDeniedMessage("Upload Assignment is only available for teachers.")
                    }
                }

                MainScreenDestination.Reminders -> {
                    ReminderScreen(
                        navController = navController,
                        onBack = onSectionBack
                    )
                }

                MainScreenDestination.Events -> {
                    EventsScreen(
                        navController = navController,
                        onBack = onSectionBack
                    )
                }
            }
        }
    }
}

sealed interface MainScreenDestination {
    data object Home : MainScreenDestination
    data object Assistant : MainScreenDestination
    data object Profile : MainScreenDestination
    data object Schedule : MainScreenDestination
    data object Assignments : MainScreenDestination
    data object UploadAssignment : MainScreenDestination
    data object Reminders : MainScreenDestination
    data object Events : MainScreenDestination
}

@Composable
private fun AccessDeniedMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
