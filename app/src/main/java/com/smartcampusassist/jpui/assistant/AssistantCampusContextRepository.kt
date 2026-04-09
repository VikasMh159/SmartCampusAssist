package com.smartcampusassist.jpui.assistant

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcampusassist.jpui.assignments.Assignment
import com.smartcampusassist.jpui.events.EventItem
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.jpui.reminders.ReminderItem
import com.smartcampusassist.jpui.schedule.ScheduleItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantCampusContext(
    val profile: UserProfile? = null,
    val groundedContext: String = ""
)

class AssistantCampusContextRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {

    suspend fun loadAssignments(limit: Long = 5): List<Assignment> {
        return fetchAssignments(limit)
    }

    suspend fun loadEvents(limit: Long = 5): List<EventItem> {
        return fetchEvents(limit)
    }

    suspend fun loadTodaySchedule(): List<ScheduleItem> {
        val profile = userRepository.getUserProfile() ?: return emptyList()
        val today = SimpleDateFormat("EEEE", Locale.US).format(Date())
        return fetchSchedule(profile)
            .filter { it.day.equals(today, ignoreCase = true) }
    }

    suspend fun buildContext(): AssistantCampusContext = coroutineScope {
        val profile = userRepository.getUserProfile()
        val cacheKey = listOf(
            profile?.uid.orEmpty(),
            profile?.role.orEmpty(),
            profile?.department.orEmpty(),
            profile?.semester?.toString().orEmpty(),
            profile?.subject.orEmpty()
        ).joinToString("|")

        if (cachedContext != null && cachedContextKey == cacheKey) {
            val isFresh = System.currentTimeMillis() - cachedAtMillis <= CACHE_TTL_MILLIS
            if (isFresh) {
                return@coroutineScope cachedContext!!
            }
        }

        val scheduleDeferred = async {
            val currentProfile = profile ?: return@async emptyList()
            fetchSchedule(currentProfile).take(8)
        }

        val assignmentsDeferred = async {
            fetchAssignments(limit = 6)
        }

        val remindersDeferred = async {
            val currentProfile = profile ?: return@async emptyList()
            firestore.collection("reminders")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ReminderItem::class.java) }
                .filter { shouldIncludeReminder(it, currentProfile) }
                .take(6)
        }

        val eventsDeferred = async {
            fetchEvents(limit = 6)
        }

        val results = awaitAll(scheduleDeferred, assignmentsDeferred, remindersDeferred, eventsDeferred)
        @Suppress("UNCHECKED_CAST")
        val schedule = results[0] as List<ScheduleItem>
        @Suppress("UNCHECKED_CAST")
        val assignments = results[1] as List<Assignment>
        @Suppress("UNCHECKED_CAST")
        val reminders = results[2] as List<ReminderItem>
        @Suppress("UNCHECKED_CAST")
        val events = results[3] as List<EventItem>

        AssistantCampusContext(
            profile = profile,
            groundedContext = buildString {
                appendLine("User profile:")
                if (profile == null) {
                    appendLine("- Profile unavailable")
                } else {
                    appendLine("- Role: ${profile.role}")
                    appendLine("- Name: ${profile.fullName.ifBlank { "Unknown" }}")
                    appendLine("- Department: ${profile.department.ifBlank { "N/A" }}")
                    appendLine("- Semester: ${profile.semester.takeIf { it > 0 } ?: 0}")
                    appendLine("- Subject: ${profile.subject.ifBlank { "N/A" }}")
                }

                appendLine()
                appendLine("Grounded campus data:")
                appendLine("- Today: ${buildTodayLabel()}")

                appendLine("Schedule:")
                if (schedule.isEmpty()) appendLine("- No schedule entries found")
                schedule.forEach { item ->
                    appendLine("- ${item.day}: ${item.subject} ${item.startTime}-${item.endTime}, room ${item.room.ifBlank { "-" }}, ${item.type.ifBlank { "class" }}")
                }

                appendLine("Assignments:")
                if (assignments.isEmpty()) appendLine("- No assignments found")
                assignments.forEach { item ->
                    appendLine("- ${item.title}: ${item.description.ifBlank { "No description" }}")
                }

                appendLine("Reminders:")
                if (reminders.isEmpty()) appendLine("- No reminders found")
                reminders.forEach { item ->
                    appendLine("- ${item.title} on ${item.date.ifBlank { "date not set" }}: ${item.description.ifBlank { "No description" }}")
                }

                appendLine("Events:")
                if (events.isEmpty()) appendLine("- No events found")
                events.forEach { item ->
                    appendLine("- ${item.title} on ${item.date.ifBlank { "date not set" }} at ${item.location.ifBlank { "campus venue" }}")
                }
            }.trim()
        ).also { context ->
            cachedContextKey = cacheKey
            cachedContext = context
            cachedAtMillis = System.currentTimeMillis()
        }
    }

    private fun shouldIncludeSchedule(item: ScheduleItem, profile: UserProfile): Boolean {
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

    private suspend fun fetchSchedule(profile: UserProfile): List<ScheduleItem> {
        return firestore.collection("schedule")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ScheduleItem::class.java) }
            .filter { shouldIncludeSchedule(it, profile) }
            .sortedWith(compareBy<ScheduleItem> { dayOrder(it.day) }.thenBy { timeOrder(it.startTime) })
    }

    private suspend fun fetchAssignments(limit: Long): List<Assignment> {
        return firestore.collection("assignments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.toObject(Assignment::class.java)?.copy(id = document.id) }
    }

    private suspend fun fetchEvents(limit: Long): List<EventItem> {
        return firestore.collection("events")
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.toObject(EventItem::class.java)?.copy(id = document.id) }
    }

    private fun shouldIncludeReminder(reminder: ReminderItem, profile: UserProfile): Boolean {
        if (reminder.createdForUid.isNotBlank() && reminder.createdForUid == profile.uid) {
            return true
        }

        return if (profile.role == "teacher") {
            val teacherMatches = listOf(reminder.teacherUid, reminder.createdForUid)
                .any { it.isNotBlank() && it == profile.uid }

            teacherMatches ||
                reminder.role.isBlank() ||
                reminder.role.equals("teacher", ignoreCase = true) ||
                reminder.department.isBlank() ||
                reminder.department.equals(profile.department, ignoreCase = true) ||
                reminder.subject.isBlank() ||
                reminder.subject.equals(profile.subject, ignoreCase = true)
        } else {
            val semesterMatches = reminder.semester.isBlank() || reminder.semester == profile.semester.toString()
            val roleMatches = reminder.role.isBlank() || reminder.role.equals("student", ignoreCase = true)
            val departmentMatches = reminder.department.isBlank() || reminder.department.equals(profile.department, ignoreCase = true)
            semesterMatches && roleMatches && departmentMatches
        }
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

    private fun buildTodayLabel(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun timeOrder(value: String): Int {
        val match = Regex("""^\s*(\d{1,2}):(\d{2})\s*([AP]M)\s*$""", RegexOption.IGNORE_CASE)
            .find(value) ?: return Int.MAX_VALUE

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

    private companion object {
        const val CACHE_TTL_MILLIS = 30_000L
        var cachedContextKey: String? = null
        var cachedContext: AssistantCampusContext? = null
        var cachedAtMillis: Long = 0L
    }
}
