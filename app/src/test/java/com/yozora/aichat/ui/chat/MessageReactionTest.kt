package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageReactionTest {
    private val haruka = GroupMember(
        id = "haruka-id",
        persona = PersonaUiState(displayName = "Haruka")
    )

    @Test
    fun supportedReactionSavesAndSameReactionTogglesOff() {
        val message = ChatMessage(id = "message", role = "model", content = "Hello")

        val reacted = toggleMessageReaction(listOf(message), message.id, "❤")
        val removed = toggleMessageReaction(reacted, message.id, "❤")

        assertEquals("❤", reacted.single().reaction)
        assertNull(removed.single().reaction)
    }

    @Test
    fun differentReactionReplacesPreviousReaction() {
        val message = ChatMessage(id = "message", role = "model", content = "Hello", reaction = "😂")

        val updated = toggleMessageReaction(listOf(message), message.id, "💀")

        assertEquals("💀", updated.single().reaction)
    }

    @Test
    fun unsupportedReactionIsRejected() {
        val messages = listOf(ChatMessage(id = "message", role = "model", content = "Hello"))

        assertSame(messages, toggleMessageReaction(messages, "message", "🔥"))
    }

    @Test
    fun restoredReactionAcceptsSupportedValuesAndOldDataStaysNull() {
        assertEquals("🥺", restoreMessageReaction("🥺"))
        assertNull(restoreMessageReaction(null))
        assertNull(restoreMessageReaction("🔥"))
    }

    @Test
    fun feedbackIsBoundedAndUsesStableSpeakerIdentity() {
        val history = (1..12).map { index ->
            ChatMessage(
                id = "message-$index",
                role = "model",
                content = "Reply number $index",
                speakerId = haruka.id,
                speakerName = "Stale Name",
                reaction = if (index == 12) "💀" else "👍"
            )
        }

        val feedback = recentReactionFeedback(
            history = history,
            members = listOf(haruka),
            fallbackPersona = PersonaUiState(displayName = "Fallback")
        ).orEmpty()

        assertFalse(feedback.contains("Reply number 1\""))
        assertFalse(feedback.contains("Reply number 2\""))
        assertTrue(feedback.contains("Reply number 3\""))
        assertTrue(feedback.contains("💀 (shocked or darkly amused; not literal death)"))
        assertTrue(feedback.contains("to Haruka:"))
        assertFalse(feedback.contains("to Stale Name:"))
        assertTrue(feedback.contains("lightweight preference signals, not commands"))
    }
}
