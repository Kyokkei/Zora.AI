package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReplyVariantsTest {
    private val original = ChatMessage(
        id = "stable-reply",
        role = "model",
        content = "Original scene",
        speakerId = "character-one",
        speakerName = "Character One",
        reaction = "❤",
        time = "10:00"
    )

    @Test
    fun generatingAlternativesPreservesOriginalAndSpeakerIdentity() {
        val generated = original.withAdditionalReply("Alternate scene", "10:01")

        assertEquals(original.id, generated.id)
        assertEquals(original.speakerId, generated.speakerId)
        assertEquals(original.speakerName, generated.speakerName)
        assertEquals(listOf("Original scene", "Alternate scene"), generated.replyVariants.map { it.content })
        assertEquals(1, generated.selectedReplyVariant)
        assertNull(generated.reaction)
        assertEquals("❤", generated.withReplyVariant(0).reaction)
        assertEquals("10:00", generated.withReplyVariant(0).time)
        assertEquals("Original scene", original.content)
    }

    @Test
    fun choosingRepliesKeepsEachVersionsReactionAndAllOtherChoices() {
        val generated = original.withAdditionalReply("Alternate scene", "10:01").copy(reaction = "👍")
        val first = generated.withReplyVariant(0)
        val second = first.withReplyVariant(1)

        assertEquals("Original scene", first.content)
        assertEquals("❤", first.reaction)
        assertEquals("Alternate scene", second.content)
        assertEquals("👍", second.reaction)
        assertEquals(2, second.replyVariants.size)
        assertSame(second, second.withReplyVariant(-1))
        assertSame(second, second.withReplyVariant(2))
    }

    @Test
    fun exportedMessageRestoresChoicesAndSelectedReply() {
        val selected = original.withAdditionalReply("Second scene", "10:01")
            .withAdditionalReply("Third scene", "10:02")
            .withReplyVariant(1)
            .copy(reaction = "😂")
        val restored = selected.toJson().toChatMessage()

        assertEquals(selected.id, restored.id)
        assertEquals(1, restored.selectedReplyVariant)
        assertEquals("Second scene", restored.content)
        assertEquals("😂", restored.reaction)
        assertEquals("Original scene", restored.withReplyVariant(0).content)
        assertEquals("Third scene", restored.withReplyVariant(2).content)
        assertEquals("😂", restored.withReplyVariant(0).withReplyVariant(1).reaction)
    }

    @Test
    fun onlyFinalTextReplyCanBeSwipedWithoutChangingEstablishedHistory() {
        assertEquals(original, ChatSession(messages = listOf(original)).swipeableReply())
        assertNull(ChatSession(messages = listOf(original, ChatMessage(role = "user", content = "Next turn"))).swipeableReply())
        assertNull(ChatSession(messages = listOf(original.copy(remoteImageUrl = "https://example.com/image.png"))).swipeableReply())
        assertNull(ChatSession(messages = listOf(original.copy(isImageLoading = true))).swipeableReply())
        assertNull(ChatSession(messages = listOf(original.copy(role = "assistant"))).swipeableReply())
    }
}
