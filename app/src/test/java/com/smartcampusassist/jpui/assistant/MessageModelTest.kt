package com.smartcampusassist.jpui.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageModelTest {

    @Test
    fun message_toChatMessage_preservesRichFields() {
        val message = Message(
            text = "Assignments loaded",
            isUser = false,
            timestamp = 1234L,
            summary = "2 assignment items found.",
            chips = listOf("Latest first", "Campus data"),
            cards = listOf(
                MessageCard(
                    title = "Math Assignment",
                    subtitle = "Faculty update",
                    supportingText = "Read chapter 3",
                    meta = "Text update"
                )
            )
        )

        val chatMessage = message.toChatMessage()

        assertEquals(1234L, chatMessage.id)
        assertEquals("Assignments loaded", chatMessage.text)
        assertEquals("2 assignment items found.", chatMessage.summary)
        assertEquals(listOf("Latest first", "Campus data"), chatMessage.chips)
        assertEquals(1, chatMessage.cards.size)
        assertEquals("Math Assignment", chatMessage.cards.first().title)
    }

    @Test
    fun chatMessage_toMessage_restoresRichFields() {
        val chatMessage = ChatMessage(
            id = 5678L,
            text = "Today's schedule",
            isUser = false,
            summary = "3 class slot(s) for today.",
            chips = listOf("Today"),
            cards = listOf(
                MessageCard(
                    title = "Physics",
                    subtitle = "09:00 AM - 10:00 AM",
                    supportingText = "Lecture",
                    meta = "Room 101"
                )
            )
        )

        val message = chatMessage.toMessage()

        assertEquals(5678L, message.timestamp)
        assertEquals("3 class slot(s) for today.", message.summary)
        assertEquals(listOf("Today"), message.chips)
        assertTrue(message.cards.isNotEmpty())
        assertEquals("Physics", message.cards.first().title)
    }
}
