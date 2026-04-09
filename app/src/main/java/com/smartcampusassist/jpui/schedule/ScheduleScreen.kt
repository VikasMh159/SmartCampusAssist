package com.smartcampusassist.jpui.schedule

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

data class ScheduleItem(
    val subject: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val type: String = "",
    val teacher: String = ""
)

@Composable
fun ScheduleScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {

    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userRepository = remember { UserRepository() }
    var scheduleList by remember { mutableStateOf(listOf<ScheduleItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            val profile = withContext(Dispatchers.IO) {
                userRepository.getUserProfile()
                    ?: throw IllegalStateException("User profile not found.")
            }

            withContext(Dispatchers.IO) {
                ensureTimetableAssetSynced(context, firestore)
            }

            val result = withContext(Dispatchers.IO) {
                firestore.collection("schedule").get().await()
            }
            scheduleList = result.documents
                .mapNotNull { document ->
                    document.toObject(ScheduleItem::class.java)
                }
                .filter { shouldIncludeSchedule(it, profile) }
                .sortedWith(
                    compareBy<ScheduleItem> { dayOrder(it.day) }
                        .thenBy { timeOrder(it.startTime) }
                        .thenBy { it.subject }
                )
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load schedule."
        } finally {
            isLoading = false
        }
    }

    val groupedSchedule = remember(scheduleList) {
        scheduleList
            .groupBy { it.day.ifBlank { "Other" } }
            .toList()
            .sortedBy { (day, _) -> dayOrder(day) }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Schedule",
                    subtitle = "Your timetable",
                    onBack = onBack
                )
            }

            when {
                isLoading -> {
                    item {
                        LoadingState("Syncing timetable")
                    }
                }

                errorMessage != null -> {
                    item {
                        MessageState("Schedule unavailable", errorMessage ?: "")
                    }
                }

                scheduleList.isEmpty() -> {
                    item {
                        MessageState("No schedule", "No classes are available right now.")
                    }
                }

                else -> {
                    item {
                        ScheduleHero(scheduleCount = scheduleList.size)
                    }

                    items(
                        items = groupedSchedule,
                        key = { (day, _) -> day },
                        contentType = { "schedule_day" }
                    ) { (day, entries) ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${entries.size} class${if (entries.size == 1) "" else "es"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            ScheduleTableHeader(modifier = Modifier.padding(top = 12.dp))

                            entries.forEach { item ->
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                ScheduleTableRow(item)
                                if (item.teacher.isNotBlank()) {
                                    Text(
                                        text = "Faculty: ${item.teacher}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shouldIncludeSchedule(
    item: ScheduleItem,
    profile: UserProfile
): Boolean {
    return if (profile.role == "teacher") {
        item.teacher.isBlank() ||
            item.teacher.equals(profile.fullName, ignoreCase = true) ||
            item.teacher.equals(profile.subject, ignoreCase = true) ||
            item.teacher.equals(profile.teacherId, ignoreCase = true) ||
            item.teacher.equals(profile.employeeId, ignoreCase = true)
    } else {
        true
    }
}

private fun buildTimeLabel(item: ScheduleItem): String {
    return listOfNotNull(
        item.startTime.takeIf { it.isNotBlank() },
        item.endTime.takeIf { it.isNotBlank() }
    ).joinToString(" - ")
}

@Composable
private fun ScheduleHero(scheduleCount: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(20.dp)
        ) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Check your classes.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "$scheduleCount classes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ScheduleTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScheduleTableCell(text = "Subject", weight = 1.1f, bold = true)
        ScheduleTableCell(text = "Time", weight = 1.2f, bold = true)
        ScheduleTableCell(text = "Room", weight = 0.9f, bold = true)
        ScheduleTableCell(text = "Type", weight = 0.8f, bold = true)
    }
}

@Composable
private fun ScheduleTableRow(item: ScheduleItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScheduleTableCell(text = item.subject.ifBlank { "-" }, weight = 1.1f)
        ScheduleTableCell(text = buildTimeLabel(item).ifBlank { "-" }, weight = 1.2f)
        ScheduleTableCell(
            text = item.room.ifBlank { "-" },
            weight = 0.9f,
            accent = true
        )
        ScheduleTableCell(text = item.type.ifBlank { "-" }, weight = 0.8f)
    }
}

@Composable
private fun RowScope.ScheduleTableCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    accent: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = when {
            accent -> MaterialTheme.colorScheme.primary
            bold -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

private suspend fun ensureTimetableAssetSynced(
    context: Context,
    firestore: FirebaseFirestore
) {
    val preferences = context.applicationContext
        .getSharedPreferences("schedule_sync", Context.MODE_PRIVATE)

    if (preferences.getBoolean("timetable_seeded_v1", false)) {
        return
    }

    val hasScheduleData = firestore.collection("schedule")
        .limit(1)
        .get()
        .await()
        .documents
        .isNotEmpty()

    if (hasScheduleData) {
        preferences.edit().putBoolean("timetable_seeded_v1", true).apply()
        return
    }

    syncTimetableAssetToFirestore(context, firestore)
    preferences.edit().putBoolean("timetable_seeded_v1", true).apply()
}

private suspend fun syncTimetableAssetToFirestore(
    context: Context,
    firestore: FirebaseFirestore
) {
    val timetableJson = context.assets.open("timetable.json").bufferedReader().use { it.readText() }
    val entries = JSONArray(timetableJson)
    val batch = firestore.batch()

    for (index in 0 until entries.length()) {
        val item = entries.getJSONObject(index)
        val day = item.optString("day").trim()
        val subject = item.optString("subject").trim()
        val startTime = item.optString("startTime").trim()
        val endTime = item.optString("endTime").trim()

        val documentId = buildScheduleDocumentId(
            day = day,
            subject = subject,
            startTime = startTime,
            endTime = endTime
        )

        val data = mapOf(
            "subject" to subject,
            "day" to day,
            "startTime" to startTime,
            "endTime" to endTime,
            "room" to item.optString("room").trim(),
            "type" to item.optString("type").trim(),
            "teacher" to item.optString("teacher").trim(),
            "source" to "asset:timetable.json"
        )

        batch.set(
            firestore.collection("schedule").document(documentId),
            data,
            SetOptions.merge()
        )
    }

    batch.commit().await()
}

private fun buildScheduleDocumentId(
    day: String,
    subject: String,
    startTime: String,
    endTime: String
): String {
    return listOf(day, subject, startTime, endTime)
        .joinToString("_")
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "schedule_item" }
}

private fun dayOrder(day: String): Int {
    return when (day.trim().lowercase(Locale.US)) {
        "monday" -> 1
        "tuesday" -> 2
        "wednesday" -> 3
        "thursday" -> 4
        "friday" -> 5
        "saturday" -> 6
        "sunday" -> 7
        else -> Int.MAX_VALUE
    }
}

private fun timeOrder(value: String): Int {
    val match = Regex("""^\s*(\d{1,2}):(\d{2})\s*([AP]M)\s*$""", RegexOption.IGNORE_CASE)
        .find(value)
        ?: return Int.MAX_VALUE

    val hourPart = match.groupValues[1].toIntOrNull() ?: return Int.MAX_VALUE
    val minutePart = match.groupValues[2].toIntOrNull() ?: return Int.MAX_VALUE
    val meridiem = match.groupValues[3].uppercase(Locale.US)

    val normalizedHour = when {
        meridiem == "AM" && hourPart == 12 -> 0
        meridiem == "PM" && hourPart != 12 -> hourPart + 12
        else -> hourPart
    }

    return normalizedHour * 60 + minutePart
}
