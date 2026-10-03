package com.yozora.aichat.ui

import com.yozora.aichat.ui.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSearchTest {
    @Test
    fun findsEveryMatchingMessageInChronologicalOrderBeyondOldEightResultLimit() {
        val messages = (0 until 40).map { index ->
            ChatMessage(id = "message-$index", role = "model", content = "The library scene $index")
        }
        val results = conversationSearchResults(messages, "  LIBRARY  ")

        assertEquals(messages.map { it.id }, results.map { it.id })
        assertEquals("message-0", results.first().id)
        assertEquals("message-39", results.last().id)
    }

    @Test
    fun findsSpeakerNamesAndContentButSkipsPendingImagePlaceholders() {
        val messages = listOf(
            ChatMessage(id = "speaker", role = "model", content = "Hello", speakerName = "Aster"),
            ChatMessage(id = "body", role = "user", content = "I turn to Aster."),
            ChatMessage(id = "loading", role = "model", content = "Aster", isImageLoading = true)
        )

        assertEquals(listOf("speaker", "body"), conversationSearchResults(messages, "aster").map { it.id })
        assertTrue(conversationSearchResults(messages, "   ").isEmpty())
        assertTrue(conversationSearchResults(messages, "missing").isEmpty())
    }
}
