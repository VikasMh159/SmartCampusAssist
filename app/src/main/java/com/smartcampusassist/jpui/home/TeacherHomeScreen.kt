package com.smartcampusassist.jpui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcampusassist.BuildConfig
import com.smartcampusassist.jpui.auth.AccountRequest
import com.smartcampusassist.jpui.components.DashboardHeader
import com.smartcampusassist.jpui.components.DashboardCard
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository

@Composable
fun TeacherHomeScreen(
    onOpenSchedule: () -> Unit,
    onOpenUploadAssignment: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenAdminProvision: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val userRepository = remember { UserRepository() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val adminEmail = BuildConfig.ALLOWED_LOGIN_EMAIL.trim().lowercase()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var pendingRequestCount by remember { mutableIntStateOf(0) }
    var hasFailedDeliveries by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        profile = userRepository.getUserProfile()
    }

    val isAdminUser = profile?.role == "admin" || (
        adminEmail.isNotBlank() &&
            profile?.email?.trim()?.equals(adminEmail, ignoreCase = true) == true
    )

    DisposableEffect(isAdminUser) {
        if (!isAdminUser) {
            pendingRequestCount = 0
            hasFailedDeliveries = false
            onDispose { }
        } else {
            val registration = firestore.collection("accountRequests")
                .addSnapshotListener { snapshot, _ ->
                    val requests = snapshot?.documents.orEmpty()
                        .mapNotNull { document -> document.toObject(AccountRequest::class.java) }
                    pendingRequestCount = requests.count { request ->
                            request.status == "pending" ||
                                (request.status == "approved" && request.processingState != "completed")
                        }
                    hasFailedDeliveries = requests.any { request ->
                        request.status == "approved" && request.processingState == "failed"
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DashboardHeader()
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "Upload Assignment",
                    subtitle = "Upload coursework",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenUploadAssignment()
                }

                DashboardCard(
                    title = "Schedule",
                    subtitle = "See your timetable",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenSchedule()
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "Reminders",
                    subtitle = "Stay updated",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenReminders()
                }

                DashboardCard(
                    title = "Events",
                    subtitle = "Campus activities",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenEvents()
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "AI Chat Bot",
                    subtitle = "Ask for help",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenAssistant()
                }

                DashboardCard(
                    title = "Profile",
                    subtitle = "Your account",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenProfile()
                }

            }
        }

        if (isAdminUser) {
            item {
                DashboardCard(
                    title = "Account Requests",
                    subtitle = "Manage requests",
                    badgeCount = pendingRequestCount,
                    badgeIsWarning = hasFailedDeliveries,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onOpenAdminProvision()
                }
            }
        }
    }
}
