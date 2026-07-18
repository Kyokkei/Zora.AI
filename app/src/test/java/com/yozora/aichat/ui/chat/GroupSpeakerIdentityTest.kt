package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupSpeakerIdentityTest {
    private val haruka = GroupMember(
        id = "haruka-id",
        persona = PersonaUiState(displayName = "Haruka")
    )
    private val airi = GroupMember(
        id = "airi-id",
        persona = PersonaUiState(displayName = "Airi")
    )
    private val members = listOf(haruka, airi)

    @Test
    fun otherMembersAreContextNotAssistantHistory() {
        val entities = listOf(
            ChatMessage(id = "user", role = "user", content = "Hello"),
            ChatMessage(
                id = "haruka",
                role = "model",
                content = "Haruka's reply",
                speakerId = haruka.id,
                speakerName = "Haruka"
            ),
            ChatMessage(
                id = "airi",
                role = "model",
                content = "Airi's reply",
                speakerId = airi.id,
                speakerName = "Airi"
            )
        ).toEntitiesForGroupMember(
            chatId = "chat",
            targetMemberId = airi.id,
            groupMembers = members
        )

        assertEquals(listOf("user", "user", "model"), entities.map { it.role })
        assertEquals("[Message from Haruka]\nHaruka's reply", entities[1].content)
        assertEquals("Airi's reply", entities[2].content)
    }

    @Test
    fun mismatchedStoredLabelIsReclassifiedAsTheDeclaredMember() {
        val entity = listOf(
            ChatMessage(
                role = "model",
                content = "Haruka: *steps forward*",
                speakerId = airi.id,
                speakerName = "Airi"
            )
        ).toEntitiesForGroupMember(
            chatId = "chat",
            targetMemberId = airi.id,
            groupMembers = members
        ).single()

        assertEquals("user", entity.role)
        assertEquals("[Message from Haruka]\n*steps forward*", entity.content)
    }

    @Test
    fun leadingSpeakerDetectionHandlesCommonModelPrefixes() {
        assertEquals(haruka, leadingGroupSpeaker("Haruka: hello", members))
        assertEquals(haruka, leadingGroupSpeaker("**Haruka**: hello", members))
        assertEquals(airi, leadingGroupSpeaker("[Airi] hello", members))
        assertNull(leadingGroupSpeaker("\"Haruka, are you there?\"", members))
    }
}
