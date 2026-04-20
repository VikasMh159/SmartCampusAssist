package com.smartcampusassist.campus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartcampusassist.jpui.components.DashboardCard
import com.smartcampusassist.jpui.components.DashboardHeader
import com.smartcampusassist.jpui.session.SessionState

@Composable
fun CampusDashboardScreen(
    sessionState: SessionState,
    onOpenSchedule: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCampusAdmin: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenReminders: () -> Unit
) {
    val role = sessionState.role.orEmpty()
    val canManageCampus = role in CampusRoles.privilegedRoles
    val canHandleClasses = role == CampusRoles.TEACHER || role == CampusRoles.HOD || role == CampusRoles.PRINCIPAL
    val roleTitle = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DashboardHeader() }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Campus Admin",
                    subtitle = if (canManageCampus) "Institutes, students, staff, class assignments" else "$roleTitle access is limited",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCampusAdmin
                )
                DashboardCard(
                    title = "Schedule",
                    subtitle = if (canHandleClasses) "Semester and class operations" else "View assigned schedule",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSchedule
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Assignments",
                    subtitle = if (canHandleClasses) "Track class ownership and coursework" else "View institute work items",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAssignments
                )
                DashboardCard(
                    title = "Profile",
                    subtitle = "Role, institute, permissions",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenProfile
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Events",
                    subtitle = "Institute-wide notices and events",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenEvents
                )
                DashboardCard(
                    title = "Reminders",
                    subtitle = "Daily workflow and notices",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReminders
                )
            }
        }

        item {
            DashboardCard(
                title = "AI Chat Bot",
                subtitle = "Campus assistance for staff and students",
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenAssistant
            )
        }
    }
}
