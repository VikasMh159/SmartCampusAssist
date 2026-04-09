package com.smartcampusassist.jpui.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AssistantCachedHistory(
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = 0L,
    val savedChats: List<AssistantSavedChat> = emptyList(),
    val activeChatId: Long? = null
)

data class AssistantSavedChat(
    val id: Long,
    val title: String,
    val preview: String,
    val messages: List<ChatMessage>,
    val updatedAt: Long
)

class AssistantLocalCache(context: Context) {

    private val preferences = context.getSharedPreferences("assistant_chat_cache", Context.MODE_PRIVATE)

    fun load(userId: String): AssistantCachedHistory {
        val raw = preferences.getString(cacheKey(userId), null) ?: return AssistantCachedHistory()
        return try {
            val payload = JSONObject(raw)
            AssistantCachedHistory(
                messages = loadMessages(payload.optJSONArray("messages") ?: JSONArray()),
                updatedAt = payload.optLong("updatedAt", 0L),
                savedChats = loadSavedChats(payload.optJSONArray("savedChats") ?: JSONArray()),
                activeChatId = payload.takeIf { it.has("activeChatId") }?.optLong("activeChatId")
            )
        } catch (_: Exception) {
            AssistantCachedHistory()
        }
    }

    fun save(
        userId: String,
        messages: List<ChatMessage>,
        updatedAt: Long,
        savedChats: List<AssistantSavedChat> = emptyList(),
        activeChatId: Long? = null
    ) {
        val payload = JSONObject().apply {
            put("updatedAt", updatedAt)
            if (activeChatId != null) {
                put("activeChatId", activeChatId)
            }
            put("messages", saveMessages(messages))
            put(
                "savedChats",
                JSONArray().apply {
                    savedChats.forEach { chat ->
                        put(
                            JSONObject().apply {
                                put("id", chat.id)
                                put("title", chat.title)
                                put("preview", chat.preview)
                                put("updatedAt", chat.updatedAt)
                                put("messages", saveMessages(chat.messages))
                            }
                        )
                    }
                }
            )
        }

        preferences.edit().putString(cacheKey(userId), payload.toString()).apply()
    }

    private fun cacheKey(userId: String): String = "history_$userId"

    private fun saveMessages(messages: List<ChatMessage>): JSONArray {
        return JSONArray().apply {
            messages.forEach { message ->
                put(
                    JSONObject().apply {
                        put("id", message.id)
                        put("text", message.text)
                        put("isUser", message.isUser)
                        put("summary", message.summary)
                        put("chips", JSONArray(message.chips))
                        put(
                            "cards",
                            JSONArray().apply {
                                message.cards.forEach { card ->
                                    put(
                                        JSONObject().apply {
                                            put("title", card.title)
                                            put("subtitle", card.subtitle)
                                            put("supportingText", card.supportingText)
                                            put("meta", card.meta)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    private fun loadMessages(messagesArray: JSONArray): List<ChatMessage> {
        return buildList {
            for (index in 0 until messagesArray.length()) {
                val item = messagesArray.optJSONObject(index) ?: continue
                add(
                    ChatMessage(
                        id = item.optLong("id", System.currentTimeMillis()),
                        text = item.optString("text"),
                        isUser = item.optBoolean("isUser", false),
                        summary = item.optString("summary"),
                        chips = loadStringList(item.optJSONArray("chips") ?: JSONArray()),
                        cards = loadMessageCards(item.optJSONArray("cards") ?: JSONArray())
                    )
                )
            }
        }
    }

    private fun loadStringList(array: JSONArray): List<String> {
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun loadMessageCards(array: JSONArray): List<MessageCard> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    MessageCard(
                        title = item.optString("title"),
                        subtitle = item.optString("subtitle"),
                        supportingText = item.optString("supportingText"),
                        meta = item.optString("meta")
                    )
                )
            }
        }
    }

    private fun loadSavedChats(savedChatsArray: JSONArray): List<AssistantSavedChat> {
        return buildList {
            for (index in 0 until savedChatsArray.length()) {
                val item = savedChatsArray.optJSONObject(index) ?: continue
                add(
                    AssistantSavedChat(
                        id = item.optLong("id", System.currentTimeMillis()),
                        title = item.optString("title"),
                        preview = item.optString("preview"),
                        messages = loadMessages(item.optJSONArray("messages") ?: JSONArray()),
                        updatedAt = item.optLong("updatedAt", 0L)
                    )
                )
            }
        }
    }
}
