package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaNameVisibilityTest {
    private val haruka = GroupMember(
        id = "haruka-id",
        persona = PersonaUiState(displayName = "Haruka")
    )

    @Test
    fun blankLocalNameUsesVisibleFallback() {
        assertEquals("New Persona", visiblePersonaName("   "))
        assertEquals("AI", visiblePersonaName("\n", fallback = "AI"))
    }

    @Test
    fun unicodeLocalNameIsTrimmedButPreserved() {
        assertEquals("ミノリ Đào 🌸", visiblePersonaName("  ミノリ Đào 🌸  "))
    }

    @Test
    fun blankStoredSpeakerNameFallsBackToStableMemberId() {
        val message = ChatMessage(
            role = "model",
            content = "Hello",
            speakerId = haruka.id,
            speakerName = ""
        )

        assertEquals(
            "Haruka",
            visibleSpeakerName(message, listOf(haruka), PersonaUiState(displayName = "Other"))
        )
    }

    @Test
    fun stableMemberIdWinsOverAStaleStoredName() {
        val message = ChatMessage(
            role = "model",
            content = "Hello",
            speakerId = haruka.id,
            speakerName = "Airi"
        )

        assertEquals(
            "Haruka",
            visibleSpeakerName(message, listOf(haruka), PersonaUiState(displayName = "Other"))
        )
    }

    @Test
    fun unknownSpeakerNeverRendersAnEmptyLabel() {
        val message = ChatMessage(
            role = "model",
            content = "Hello",
            speakerId = "removed-member",
            speakerName = " "
        )

        assertEquals(
            "AI",
            visibleSpeakerName(message, emptyList(), PersonaUiState(displayName = ""))
        )
    }
}
