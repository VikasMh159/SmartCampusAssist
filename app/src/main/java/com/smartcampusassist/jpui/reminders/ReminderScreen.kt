package com.smartcampusassist.jpui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard

data class ReminderItem(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val role: String = "",
    val department: String = "",
    val semester: String = "",
    val subject: String = "",
    val teacherUid: String = "",
    val createdForUid: String = ""
)

@Composable
fun ReminderScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userRepository = remember { UserRepository() }

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var reminders by remember { mutableStateOf(listOf<ReminderItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            profile = userRepository.getUserProfile()
                ?: throw IllegalStateException("User profile not found.")
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load reminders."
            isLoading = false
        }
    }

    DisposableEffect(profile?.uid, profile?.role) {
        val currentProfile = profile
        var registration: ListenerRegistration? = null

        if (currentProfile != null) {
            registration = firestore.collection("reminders")
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage ?: "Unable to load reminders."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    reminders = snapshot?.documents.orEmpty()
                        .mapNotNull { it.toObject(ReminderItem::class.java) }
                        .filter { shouldIncludeReminder(it, currentProfile) }
                    errorMessage = null
                    isLoading = false
                }
        } else if (errorMessage == null) {
            isLoading = true
        }

        onDispose {
            registration?.remove()
        }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Reminders",
                    subtitle = "Important updates",
                    onBack = onBack
                )
            }

            if (!isLoading && errorMessage == null) {
                item {
                    ReminderHero(
                        count = reminders.size,
                        role = profile?.role.orEmpty()
                    )
                }
            }

            when {
                isLoading -> item { LoadingState("Loading reminders") }
                errorMessage != null -> item {
                    MessageState("Reminders unavailable", errorMessage ?: "")
                }
                reminders.isEmpty() -> item {
                    MessageState("No reminders", "You're all caught up for now.")
                }
                else -> {
                    items(
                        items = reminders,
                        key = { reminder ->
                            "${reminder.title}|${reminder.date}|${reminder.createdForUid}|${reminder.teacherUid}"
                        },
                        contentType = { "reminder" }
                    ) { reminder ->
                        ReminderCard(reminder = reminder)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderHero(
    count: Int,
    role: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(20.dp)
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (role == "teacher") {
                    "Check staff and department updates."
                } else {
                    "Check upcoming alerts."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "$count reminder${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReminderCard(reminder: ReminderItem) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                ReminderPill(text = reminder.date.ifBlank { "Upcoming" })

                Text(
                    text = reminder.title.ifBlank { "Reminder" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )

                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminder.department.takeIf { it.isNotBlank() }?.let {
                        ReminderMetaChip(Icons.Outlined.School, it)
                    }
                    reminder.role.takeIf { it.isNotBlank() }?.let {
                        ReminderMetaChip(Icons.Outlined.Person, it.replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                        })
                    }
                    reminder.subject.takeIf { it.isNotBlank() }?.let {
                        ReminderMetaChip(Icons.Outlined.Campaign, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ReminderMetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun shouldIncludeReminder(
    reminder: ReminderItem,
    profile: UserProfile
): Boolean {
    if (reminder.createdForUid.isNotBlank() && reminder.createdForUid == profile.uid) {
        return true
    }

    return if (profile.role == "teacher") {
        val teacherMatches = listOf(
            reminder.teacherUid,
            reminder.createdForUid
        ).any { it.isNotBlank() && it == profile.uid }

        teacherMatches ||
            reminder.role.isBlank() ||
            reminder.role.equals("teacher", ignoreCase = true) ||
            reminder.department.isBlank() ||
            reminder.department.equals(profile.department, ignoreCase = true) ||
            reminder.subject.isBlank() ||
            reminder.subject.equals(profile.subject, ignoreCase = true)
    } else {
        val semesterMatches =
            reminder.semester.isBlank() || reminder.semester == profile.semester.toString()

        val roleMatches =
            reminder.role.isBlank() || reminder.role.equals("student", ignoreCase = true)

        val departmentMatches =
            reminder.department.isBlank() ||
                reminder.department.equals(profile.department, ignoreCase = true)

        semesterMatches && roleMatches && departmentMatches
    }
}
