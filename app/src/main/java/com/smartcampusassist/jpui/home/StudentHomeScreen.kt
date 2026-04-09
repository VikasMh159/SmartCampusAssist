package com.smartcampusassist.jpui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartcampusassist.jpui.components.DashboardHeader
import com.smartcampusassist.jpui.components.DashboardCard

@Composable
fun StudentHomeScreen(
    onOpenSchedule: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit
) {

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
                    title = "Assignments",
                    subtitle = "View your coursework",
                    modifier = Modifier.weight(1f)
                ) {
                    onOpenAssignments()
                }

                DashboardCard(
                    title = "Schedule",
                    subtitle = "See class timings",
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
    }
}
