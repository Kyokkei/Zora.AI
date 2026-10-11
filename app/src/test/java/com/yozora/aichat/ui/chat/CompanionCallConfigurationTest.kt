package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionCallConfigurationTest {
    @Test
    fun companionPromptIsIncludedOnlyForCompanionCalls() {
        val ordinary = composeGeminiLiveSystemInstruction(
            masterPrompt = "master",
            skillContent = "skills",
            personaPrompt = "persona",
            companionMode = false,
            companionPrompt = "companion-only",
        )
        val companion = composeGeminiLiveSystemInstruction(
            masterPrompt = "master",
            skillContent = "skills",
            personaPrompt = "persona",
            companionMode = true,
            companionPrompt = "companion-only",
        )

        assertEquals("master\n\n---\n\nskills\n\n---\n\npersona", ordinary)
        assertTrue(companion.endsWith("\n\n---\n\ncompanion-only"))
        assertFalse(ordinary.contains("companion-only"))
    }

    @Test
    fun blankCompanionPromptLeavesExistingInstructionUnchanged() {
        assertEquals(
            "persona prompt",
            composeGeminiLiveSystemInstruction(
                masterPrompt = "",
                skillContent = "",
                personaPrompt = "persona prompt",
                companionMode = true,
                companionPrompt = "  ",
            ),
        )
    }

    @Test
    fun voiceRosterMatchesTheUserTestedGenderGroups() {
        assertEquals(
            setOf(
                GeminiLiveVoice.Zephyr,
                GeminiLiveVoice.Leda,
                GeminiLiveVoice.Aoede,
                GeminiLiveVoice.Callirrhoe,
                GeminiLiveVoice.Autonoe,
                GeminiLiveVoice.Despina,
                GeminiLiveVoice.Erinome,
                GeminiLiveVoice.Laomedeia,
            ),
            GeminiLiveVoice.entries.filter { it.gender == GeminiLiveVoiceGender.Female }.toSet(),
        )
        assertEquals(
            setOf(
                GeminiLiveVoice.Puck,
                GeminiLiveVoice.Charon,
                GeminiLiveVoice.Kore,
                GeminiLiveVoice.Fenrir,
                GeminiLiveVoice.Orus,
                GeminiLiveVoice.Enceladus,
                GeminiLiveVoice.Lapetus,
                GeminiLiveVoice.Umbriel,
                GeminiLiveVoice.Algieba,
                GeminiLiveVoice.Achernar,
            ),
            GeminiLiveVoice.entries.filter { it.gender == GeminiLiveVoiceGender.Male }.toSet(),
        )
        assertEquals(
            setOf(GeminiLiveVoice.Glow, GeminiLiveVoice.Flare),
            GeminiLiveVoice.entries.filter { it.gender == null }.toSet(),
        )
    }
}
