package com.smartcampusassist.jpui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.GlassCard

@Composable
fun DashboardHeader() {
    val repository = remember { UserRepository() }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        profile = runCatching { repository.getUserProfile() }.getOrNull()
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.fullName?.ifBlank { "Logged in user" } ?: "Loading profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildHeaderSubtitle(profile),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
            ) {
                Text(
                    text = buildRoleLabel(profile),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun buildRoleLabel(profile: UserProfile?): String {
    val role = profile?.role?.ifBlank { "student" } ?: "user"
    return role.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}

private fun buildHeaderSubtitle(profile: UserProfile?): String {
    if (profile == null) return "Account details"

    return when {
        profile.instituteName.isNotBlank() && profile.role != "student" -> {
            "${buildRoleLabel(profile)} • ${profile.instituteName}"
        }
        profile.role == "student" && profile.enrollment.isNotBlank() -> profile.enrollment
        profile.role == "teacher" && profile.teacherId.isNotBlank() -> profile.teacherId
        profile.role == "teacher" && profile.employeeId.isNotBlank() -> profile.employeeId
        profile.instituteName.isNotBlank() -> profile.instituteName
        profile.email.isNotBlank() -> profile.email
        else -> buildRoleLabel(profile)
    }
}
