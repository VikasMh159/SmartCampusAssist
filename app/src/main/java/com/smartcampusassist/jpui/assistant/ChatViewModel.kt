package com.smartcampusassist.jpui.assistant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.smartcampusassist.jpui.assignments.Assignment
import com.smartcampusassist.jpui.events.EventItem
import com.smartcampusassist.jpui.schedule.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AssistantChatRepository()
    private val localCache = AssistantLocalCache(application.applicationContext)
    private val campusRepository = AssistantCampusContextRepository()
    private var listenerRegistration: ListenerRegistration? = null
    private var pendingReplyJob: Job? = null
    private var activeConversationToken: Long = System.currentTimeMillis()

    val messages = mutableStateListOf<Message>()

    var isTyping by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        messages += welcomeMessage()
        observeChatHistory()
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || isTyping) return

        val conversationToken = activeConversationToken
        messages += Message(
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )

        pendingReplyJob?.cancel()
        pendingReplyJob = viewModelScope.launch {
            persistMessages()
            isTyping = true

            val response = try {
                delay(750)
                generateResponse(text)
            } catch (_: Exception) {
                buildFallbackMessage()
            }

            if (!isActive || conversationToken != activeConversationToken) {
                return@launch
            }

            messages += response
            isTyping = false
            persistMessages()
        }
    }

    fun resetChat() {
        activeConversationToken = System.currentTimeMillis()
        pendingReplyJob?.cancel()
        pendingReplyJob = null
        isTyping = false
        messages.clear()
        messages += welcomeMessage()
        viewModelScope.launch {
            persistMessages()
        }
    }

    override fun onCleared() {
        pendingReplyJob?.cancel()
        listenerRegistration?.remove()
        super.onCleared()
    }

    private fun observeChatHistory() {
        val userId = repository.currentUserId()
        if (userId == null || !repository.hasSignedInUser()) {
            isLoading = false
            return
        }

        val cachedHistory = localCache.load(userId)
        if (cachedHistory.messages.isNotEmpty()) {
            replaceMessages(cachedHistory.messages.map { it.toMessage() })
            isLoading = false
        }

        listenerRegistration?.remove()
        listenerRegistration = repository.observeChatHistory(
            onChange = { history ->
                val mergedMessages = mergeMessages(
                    remoteMessages = history.messages.map { it.toMessage() },
                    localMessages = messages.toList()
                )
                replaceMessages(mergedMessages.ifEmpty { listOf(welcomeMessage()) })
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }

    private suspend fun generateResponse(question: String): Message = withContext(Dispatchers.IO) {
        val normalized = question.trim().lowercase()

        when {
            normalized in setOf("hi", "hello", "hey", "hello assistant", "hi assistant") -> {
                Message(
                    text = "Hello! I can help with assignments, events, and today's schedule.",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    summary = "Quick campus assistant ready.",
                    chips = listOf("Assignments", "Events", "Today's schedule")
                )
            }

            "assignment" in normalized || "assignments" in normalized || "homework" in normalized -> {
                buildAssignmentsMessage(campusRepository.loadAssignments())
            }

            "event" in normalized || "events" in normalized -> {
                buildEventsMessage(campusRepository.loadEvents())
            }

            "schedule" in normalized || "class" in normalized || "today" in normalized || "timetable" in normalized -> {
                buildScheduleMessage(campusRepository.loadTodaySchedule())
            }

            else -> {
                Message(
                    text = "I can help with assignments, events, and today's schedule. Try one of the quick prompts below.",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    summary = "Ask with campus keywords for faster results.",
                    chips = listOf("Show assignments", "Show events", "Today's schedule")
                )
            }
        }
    }

    private suspend fun persistMessages() {
        val userId = repository.currentUserId() ?: return
        val chatMessages = messages.map { it.toChatMessage() }
        val updatedAt = System.currentTimeMillis()

        localCache.save(
            userId = userId,
            messages = chatMessages,
            updatedAt = updatedAt
        )

        try {
            withContext(Dispatchers.IO) {
                repository.saveMessages(chatMessages)
            }
        } catch (_: Exception) {
        }
    }

    private fun replaceMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages.ifEmpty { listOf(welcomeMessage()) })
    }

    private fun mergeMessages(remoteMessages: List<Message>, localMessages: List<Message>): List<Message> {
        if (remoteMessages.isEmpty()) return localMessages

        val localByTimestamp = localMessages.associateBy { it.timestamp }
        return remoteMessages.map { remote ->
            val local = localByTimestamp[remote.timestamp]
            if (local != null && remote.hasOnlyBaseContent()) {
                local
            } else {
                remote
            }
        }
    }

    private fun Message.hasOnlyBaseContent(): Boolean {
        return summary.isNullOrBlank() && chips.isEmpty() && cards.isEmpty()
    }

    private fun buildAssignmentsMessage(assignments: List<Assignment>): Message {
        if (assignments.isEmpty()) {
            return Message(
                text = "I could not find any assignments right now.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                summary = "No assignment records are currently available."
            )
        }

        return Message(
            text = buildString {
                appendLine("Latest assignments")
                assignments.take(4).forEachIndexed { index, assignment ->
                    append(index + 1)
                    append(". ")
                    append(assignment.title.ifBlank { "Untitled assignment" })
                    appendLine()
                }
            }.trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            summary = "${assignments.size} assignment item(s) found.",
            chips = listOf("Latest first", "Campus data"),
            cards = assignments.take(4).map { assignment ->
                MessageCard(
                    title = assignment.title.ifBlank { "Untitled assignment" },
                    subtitle = assignment.teacherName.ifBlank { "Faculty update" },
                    supportingText = assignment.description.ifBlank { "No description provided." },
                    meta = if (assignment.fileName.isNotBlank()) assignment.fileName else "Text update"
                )
            }
        )
    }

    private fun buildEventsMessage(events: List<EventItem>): Message {
        if (events.isEmpty()) {
            return Message(
                text = "I could not find any upcoming events right now.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                summary = "No future campus events are currently listed."
            )
        }

        return Message(
            text = buildString {
                appendLine("Upcoming events")
                events.take(4).forEachIndexed { index, event ->
                    append(index + 1)
                    append(". ")
                    append(event.title.ifBlank { "Campus event" })
                    appendLine()
                }
            }.trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            summary = "${events.size} event item(s) available.",
            chips = listOf("Upcoming", "Campus events"),
            cards = events.take(4).map { event ->
                MessageCard(
                    title = event.title.ifBlank { "Campus event" },
                    subtitle = event.date.ifBlank { "Date not available" },
                    supportingText = event.description.ifBlank { "No event description available." },
                    meta = event.location.ifBlank { "Campus venue" }
                )
            }
        )
    }

    private fun buildScheduleMessage(schedule: List<ScheduleItem>): Message {
        if (schedule.isEmpty()) {
            return Message(
                text = "You do not have any classes scheduled for today.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                summary = "Today's timetable is clear."
            )
        }

        return Message(
            text = buildString {
                appendLine("Today's schedule")
                schedule.forEachIndexed { index, item ->
                    append(index + 1)
                    append(". ")
                    append(item.subject.ifBlank { "Class" })
                    append(" at ")
                    append(item.startTime.ifBlank { "Time not set" })
                    appendLine()
                }
            }.trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            summary = "${schedule.size} class slot(s) for today.",
            chips = listOf("Today", "Timetable"),
            cards = schedule.map { item ->
                MessageCard(
                    title = item.subject.ifBlank { "Class" },
                    subtitle = buildString {
                        append(item.startTime.ifBlank { "Time not set" })
                        if (item.endTime.isNotBlank()) {
                            append(" - ")
                            append(item.endTime)
                        }
                    },
                    supportingText = item.type.ifBlank { "Scheduled session" },
                    meta = item.room.ifBlank { "Room not set" }
                )
            }
        )
    }

    private fun buildFallbackMessage(): Message {
        return Message(
            text = "I could not load campus data right now. Please try again in a moment.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            summary = "The request failed temporarily."
        )
    }

    private fun welcomeMessage(): Message {
        return Message(
            text = "Hello! Ask me about assignments, events, or today's schedule.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            summary = "Campus data replies are available in chat.",
            chips = listOf("Show assignments", "Show events", "Today's schedule")
        )
    }
}
