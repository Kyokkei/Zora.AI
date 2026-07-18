package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCloneTest {
    private val firstMember = GroupMember(
        id = "member-one",
        persona = PersonaUiState(displayName = "One"),
        apiKey = "member-key-one"
    )
    private val secondMember = GroupMember(
        id = "member-two",
        persona = PersonaUiState(displayName = "Two"),
        apiKey = "member-key-two"
    )
    private val firstMessage = ChatMessage(
        id = "message-one",
        role = "model",
        content = "First reply",
        speakerId = firstMember.id,
        speakerName = "One",
        reaction = "❤"
    )
    private val secondMessage = ChatMessage(
        id = "message-two",
        role = "model",
        content = "Second reply",
        speakerId = secondMember.id,
        speakerName = "Two"
    )
    private val source = ChatSession(
        id = "source-session",
        persona = secondMember.persona,
        members = listOf(firstMember, secondMember),
        activeMemberId = secondMember.id,
        groupResponseMode = GroupResponseMode.Auto,
        directorApiKey = "director-key",
        archivedContext = "Archived scene",
        archivedMessageIds = setOf(firstMessage.id),
        messages = listOf(firstMessage, secondMessage)
    )

    @Test
    fun configCloneKeepsKeysButClearsConversation() {
        val clone = source.localClone(includeMessages = false)

        assertNotEquals(source.id, clone.id)
        assertEquals(listOf("member-key-one", "member-key-two"), clone.members.map { it.apiKey })
        assertEquals("director-key", clone.directorApiKey)
        assertEquals(GroupResponseMode.Auto, clone.groupResponseMode)
        assertEquals(secondMember.persona, clone.persona)
        assertTrue(clone.messages.isEmpty())
        assertTrue(clone.archivedMessageIds.isEmpty())
        assertEquals("", clone.archivedContext)
        assertEquals("No messages yet", clone.preview)
        assertFalse(clone.members.map { it.id }.any { it in source.members.map(GroupMember::id) })
    }

    @Test
    fun fullCloneKeepsKeysAndRemapsConversationReferences() {
        val clone = source.localClone(includeMessages = true)

        assertEquals(listOf("member-key-one", "member-key-two"), clone.members.map { it.apiKey })
        assertEquals("director-key", clone.directorApiKey)
        assertEquals("Archived scene", clone.archivedContext)
        assertEquals(2, clone.messages.size)
        assertFalse(clone.messages.map { it.id }.any { it in source.messages.map(ChatMessage::id) })
        assertEquals(clone.members.map { it.id }, clone.messages.map { it.speakerId })
        assertEquals("❤", clone.messages.first().reaction)
        assertEquals(setOf(clone.messages.first().id), clone.archivedMessageIds)
        assertEquals(clone.members[1].id, clone.activeMemberId)
    }
}
