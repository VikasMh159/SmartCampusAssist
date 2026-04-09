package com.smartcampusassist.jpui.assistant

data class Message(
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val chips: List<String> = emptyList(),
    val cards: List<MessageCard> = emptyList()
)

data class MessageCard(
    val title: String = "",
    val subtitle: String = "",
    val supportingText: String = "",
    val meta: String = ""
)

internal fun Message.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = timestamp,
        text = text,
        isUser = isUser,
        summary = summary.orEmpty(),
        chips = chips,
        cards = cards
    )
}

internal fun ChatMessage.toMessage(): Message {
    return Message(
        text = text,
        isUser = isUser,
        timestamp = id,
        summary = summary.takeIf { it.isNotBlank() },
        chips = chips,
        cards = cards
    )
}
