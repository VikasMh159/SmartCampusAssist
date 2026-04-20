package com.smartcampusassist.jpui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.smartcampusassist.BuildConfig
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenAdminProvision: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    if (user == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingState("Loading profile")
        }
        return
    }

    AppBackground {
        when {
            uiState.isLoading -> LoadingState("Loading profile")
            uiState.errorMessage != null -> MessageState("Profile unavailable", uiState.errorMessage ?: "")
            profile == null -> MessageState("Profile not found", "No profile data is available for this account.")
            else -> {
                val profileData = profile
                val isAdminUser = profileData.role == "admin" || (
                    BuildConfig.ALLOWED_LOGIN_EMAIL.isNotBlank() &&
                        profileData.email.trim().equals(
                            BuildConfig.ALLOWED_LOGIN_EMAIL.trim(),
                            ignoreCase = true
                        )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ProfileHero(profile = profileData)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfileStatCard(
                                title = "Role",
                                value = profileData.role.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatCard(
                                title = profileSecondaryStatTitle(profileData),
                                value = profileSecondaryStatValue(profileData),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ProfileItem("Full Name", profileData.fullName, Icons.Outlined.Badge)
                    }
                    item {
                        ProfileItem("Email", profileData.email, Icons.Outlined.Email)
                    }
                    item {
                        ProfileItem("Institute", profileData.instituteName, Icons.Outlined.Workspaces)
                    }

                    if (profileData.role == "student") {
                        item {
                            ProfileItem("Branch", profileData.branch, Icons.Outlined.Workspaces)
                        }
                        item {
                            ProfileItem("Enrollment", profileData.enrollment, Icons.Outlined.Badge)
                        }
                        item {
                            ProfileItem("Semester", profileData.semester.toString(), Icons.Outlined.School)
                        }
                        item {
                            ProfileItem("Academic Year", profileData.academicYear, Icons.Outlined.School)
                        }
                    }

                    if (profileData.role != "student") {
                        item {
                            ProfileItem("Department", profileData.department, Icons.Outlined.Workspaces)
                        }
                    }

                    if (profileData.role != "student" && profileData.branch.isNotBlank()) {
                        item {
                            ProfileItem("Branch / Program", profileData.branch, Icons.Outlined.Workspaces)
                        }
                    }

                    if (profileData.role == "teacher" || profileData.role == "hod") {
                        item {
                            ProfileItem(
                                "Subjects",
                                profileData.subjects.joinToString().ifBlank {
                                    profileData.subject.ifBlank { "-" }
                                },
                                Icons.Outlined.School
                            )
                        }
                        if (profileData.semesters.isNotEmpty()) {
                            item {
                                ProfileItem(
                                    "Semesters",
                                    profileData.semesters.joinToString { "Sem $it" },
                                    Icons.Outlined.School
                                )
                            }
                        }
                        if (profileData.assignedClasses.isNotEmpty()) {
                            item {
                                ProfileItem(
                                    "Classes",
                                    profileData.assignedClasses.joinToString(),
                                    Icons.Outlined.Workspaces
                                )
                            }
                        }
                    }

                    item {
                        ProfileItem("User ID", user.uid, Icons.Outlined.Badge)
                    }

                    item {
                        Button(
                            onClick = { showLogoutConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null
                            )
                            Text(
                                text = "Logout",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (isAdminUser) {
                        item {
                            FilledTonalButton(
                                onClick = onOpenAdminProvision,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Manage Account Requests",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmation = false }
                ) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
private fun ProfileHero(profile: UserProfile) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = profile.fullName.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = buildProfileSubtitle(profile),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ProfileItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = value.ifBlank { "-" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun buildProfileSubtitle(profile: UserProfile): String {
    val role = profile.role.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    val secondary = when {
        profile.role == "student" && profile.enrollment.isNotBlank() -> profile.enrollment
        profile.role == "student" && profile.branch.isNotBlank() -> profile.branch
        profile.role != "student" && profile.department.isNotBlank() -> profile.department
        profile.role != "student" && profile.branch.isNotBlank() -> profile.branch
        (profile.role == "teacher" || profile.role == "hod") && profile.subjects.isNotEmpty() -> profile.subjects.joinToString()
        profile.role == "teacher" && profile.subject.isNotBlank() -> profile.subject
        profile.email.isNotBlank() -> profile.email
        else -> "Campus member"
    }

    return "$role | $secondary"
}

private fun profileSecondaryStatTitle(profile: UserProfile): String {
    return when {
        profile.role == "student" -> "Branch"
        profile.department.isNotBlank() -> "Department"
        profile.branch.isNotBlank() -> "Program"
        else -> "Institute"
    }
}

private fun profileSecondaryStatValue(profile: UserProfile): String {
    return when {
        profile.role == "student" -> profile.branch.ifBlank { "-" }
        profile.department.isNotBlank() -> profile.department.ifBlank { "-" }
        profile.branch.isNotBlank() -> profile.branch.ifBlank { "-" }
        else -> profile.instituteName.ifBlank { "-" }
    }
}
