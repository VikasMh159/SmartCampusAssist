package com.smartcampusassist.jpui.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AssistantUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Hi, I'm your campus assistant. Ask about classes, assignments, events, schedules, exams, or study help.",
            isUser = false
        )
    ),
    val savedChats: List<AssistantSavedChat> = emptyList(),
    val isLoadingHistory: Boolean = true,
    val lastSyncedAt: Long? = null,
    val hasPendingSync: Boolean = false,
    val isSending: Boolean = false,
    val statusMessage: String? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AssistantChatRepository()
    private val campusContextRepository = AssistantCampusContextRepository()
    private val localCache = AssistantLocalCache(application.applicationContext)
    private var chatListener: ListenerRegistration? = null
    private var activeChatId: Long? = null

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        observeChatHistory()
    }

    fun sendMessage(rawQuestion: String) {
        val question = rawQuestion.trim()
        val currentState = _uiState.value
        if (question.isBlank() || currentState.isSending) return

        if (activeChatId == null) {
            activeChatId = System.currentTimeMillis()
        }

        val updatedMessages = currentState.messages + ChatMessage(text = question, isUser = true)

        _uiState.value = currentState.copy(
            messages = updatedMessages,
            lastSyncedAt = currentState.lastSyncedAt,
            hasPendingSync = currentState.hasPendingSync,
            isSending = true,
            statusMessage = "Assistant is typing..."
        )

        viewModelScope.launch {
            try {
                persistMessages(updatedMessages)
                val campusContext = withContext(Dispatchers.IO) {
                    campusContextRepository.buildContext()
                }

                val response = withContext(Dispatchers.IO) {
                    GeminiHelper.askGemini(
                        question = question,
                        groundedContext = campusContext.groundedContext,
                        userRole = campusContext.profile?.role ?: "student"
                    )
                }

                val responseMessages = _uiState.value.messages + ChatMessage(text = response, isUser = false)
                _uiState.value = _uiState.value.copy(
                    messages = responseMessages,
                    isSending = false,
                    statusMessage = null
                )
                persistMessages(responseMessages)
            } catch (e: Exception) {
                val fallbackMessages = _uiState.value.messages + ChatMessage(
                    text = "Assistant is unavailable right now. Please try again in a moment.",
                    isUser = false
                )
                _uiState.value = _uiState.value.copy(
                    messages = fallbackMessages,
                    isSending = false,
                    statusMessage = null
                )
                persistMessages(fallbackMessages)
            }
        }
    }

    fun startNewChat() {
        val archivedChats = archiveCurrentChat(_uiState.value.messages, _uiState.value.savedChats)
        val messages = defaultMessages()
        activeChatId = null
        _uiState.value = _uiState.value.copy(
            messages = messages,
            savedChats = archivedChats,
            isSending = false,
            statusMessage = null
        )

        viewModelScope.launch {
            persistMessages(messages)
        }
    }

    fun deleteMessage(messageId: Long) {
        val remainingMessages = _uiState.value.messages.filterNot { it.id == messageId }
        val messages = if (remainingMessages.isEmpty()) defaultMessages() else remainingMessages

        _uiState.value = _uiState.value.copy(
            messages = messages
        )

        viewModelScope.launch {
            persistMessages(messages)
        }
    }

    fun openSavedChat(chatId: Long) {
        val selectedChat = _uiState.value.savedChats.firstOrNull { it.id == chatId } ?: return
        activeChatId = selectedChat.id
        _uiState.value = _uiState.value.copy(
            messages = selectedChat.messages.sortedBy { it.id },
            lastSyncedAt = selectedChat.updatedAt.takeIf { it > 0L } ?: _uiState.value.lastSyncedAt,
            statusMessage = null
        )
    }

    fun deleteSavedChat(chatId: Long) {
        val updatedSavedChats = _uiState.value.savedChats.filterNot { it.id == chatId }
        val resetCurrentChat = activeChatId == chatId
        if (resetCurrentChat) {
            activeChatId = null
        }
        _uiState.value = _uiState.value.copy(
            savedChats = updatedSavedChats,
            messages = if (resetCurrentChat) defaultMessages() else _uiState.value.messages,
            statusMessage = null
        )

        viewModelScope.launch {
            persistMessages(_uiState.value.messages)
        }
    }

    fun renameSavedChat(chatId: Long, title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return

        val updatedSavedChats = _uiState.value.savedChats.map { chat ->
            if (chat.id == chatId) chat.copy(title = normalizedTitle) else chat
        }
        _uiState.value = _uiState.value.copy(savedChats = updatedSavedChats)

        viewModelScope.launch {
            persistMessages(_uiState.value.messages)
        }
    }

    override fun onCleared() {
        chatListener?.remove()
        super.onCleared()
    }

    private fun observeChatHistory() {
        val userId = repository.currentUserId()
        if (userId == null || !repository.hasSignedInUser()) {
            _uiState.value = _uiState.value.copy(isLoadingHistory = false)
            return
        }

        val cachedHistory = localCache.load(userId)
        if (cachedHistory.messages.isNotEmpty()) {
            activeChatId = cachedHistory.activeChatId
            _uiState.value = _uiState.value.copy(
                messages = cachedHistory.messages.sortedBy { it.id },
                savedChats = cachedHistory.savedChats.sortedByDescending { it.updatedAt },
                lastSyncedAt = cachedHistory.updatedAt.takeIf { it > 0L },
                isLoadingHistory = true
            )
        } else {
            activeChatId = cachedHistory.activeChatId
            if (cachedHistory.savedChats.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    savedChats = cachedHistory.savedChats.sortedByDescending { it.updatedAt }
                )
            }
        }

        chatListener?.remove()
        chatListener = repository.observeChatHistory(
            onChange = { remoteHistory ->
                val messages = if (remoteHistory.messages.isEmpty() && cachedHistory.messages.isNotEmpty()) {
                    cachedHistory.messages
                } else if (remoteHistory.messages.isEmpty()) {
                    defaultMessages()
                } else {
                    remoteHistory.messages.sortedBy { it.id }
                }

                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    savedChats = cachedHistory.savedChats.sortedByDescending { it.updatedAt },
                    lastSyncedAt = remoteHistory.updatedAt.takeIf { it > 0L } ?: cachedHistory.updatedAt.takeIf { it > 0L },
                    hasPendingSync = false,
                    isLoadingHistory = false,
                    statusMessage = null
                )

                val updatedAt = remoteHistory.updatedAt.takeIf { it > 0L } ?: cachedHistory.updatedAt
                localCache.save(
                    userId = userId,
                    messages = messages,
                    updatedAt = updatedAt,
                    savedChats = _uiState.value.savedChats,
                    activeChatId = activeChatId
                )

                if (remoteHistory.messages.isEmpty()) {
                    viewModelScope.launch {
                        persistMessages(messages)
                    }
                }
            },
            onError = {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    hasPendingSync = true,
                    statusMessage = "Chat sync is unavailable right now. Local cache is active."
                )
            }
        )
    }

    private suspend fun persistMessages(messages: List<ChatMessage>) {
        val userId = repository.currentUserId()
        val updatedAt = System.currentTimeMillis()
        if (userId != null) {
            val savedChats = updateSavedChats(messages, updatedAt)
            _uiState.value = _uiState.value.copy(savedChats = savedChats)
            localCache.save(
                userId = userId,
                messages = messages,
                updatedAt = updatedAt,
                savedChats = savedChats,
                activeChatId = activeChatId
            )
        }

        try {
            withContext(Dispatchers.IO) {
                repository.saveMessages(messages)
            }
            _uiState.value = _uiState.value.copy(
                lastSyncedAt = updatedAt,
                hasPendingSync = false
            )
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(
                lastSyncedAt = updatedAt,
                hasPendingSync = true,
                statusMessage = "Changes saved locally. Cloud sync will resume when a connection is available."
            )
        }
    }

    private fun defaultMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                text = "Hi, I'm your campus assistant. Ask about classes, assignments, events, schedules, exams, or study help.",
                isUser = false
            )
        )
    }

    private fun updateSavedChats(messages: List<ChatMessage>, updatedAt: Long): List<AssistantSavedChat> {
        val currentState = _uiState.value
        val hasConversation = hasRealConversation(messages)
        val baseChats = currentState.savedChats.filterNot { it.id == activeChatId }

        if (!hasConversation || activeChatId == null) {
            return baseChats.sortedByDescending { it.updatedAt }
        }

        val updatedChat = AssistantSavedChat(
            id = activeChatId ?: updatedAt,
            title = buildChatTitle(messages),
            preview = buildChatPreview(messages),
            messages = messages.sortedBy { it.id },
            updatedAt = updatedAt
        )

        return (listOf(updatedChat) + baseChats).sortedByDescending { it.updatedAt }
    }

    private fun archiveCurrentChat(
        messages: List<ChatMessage>,
        existingChats: List<AssistantSavedChat>
    ): List<AssistantSavedChat> {
        if (!hasRealConversation(messages)) {
            return existingChats
        }

        val chatId = activeChatId ?: System.currentTimeMillis()
        val archivedChat = AssistantSavedChat(
            id = chatId,
            title = buildChatTitle(messages),
            preview = buildChatPreview(messages),
            messages = messages.sortedBy { it.id },
            updatedAt = System.currentTimeMillis()
        )

        return (listOf(archivedChat) + existingChats.filterNot { it.id == chatId })
            .sortedByDescending { it.updatedAt }
    }

    private fun hasRealConversation(messages: List<ChatMessage>): Boolean {
        return messages.any { it.isUser }
    }

    private fun buildChatTitle(messages: List<ChatMessage>): String {
        val firstUserMessage = messages.firstOrNull { it.isUser }?.text?.trim().orEmpty()
        return firstUserMessage.takeIf { it.isNotBlank() }?.take(32) ?: "Saved chat"
    }

    private fun buildChatPreview(messages: List<ChatMessage>): String {
        val latestMessage = messages.lastOrNull()?.text?.trim().orEmpty()
        return latestMessage.takeIf { it.isNotBlank() }?.take(72) ?: "No preview available"
    }
}
