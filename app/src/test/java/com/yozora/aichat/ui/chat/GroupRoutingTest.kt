package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupRoutingTest {
    private val members = listOf("Haruka", "Minori", "Shizuku", "Airi").mapIndexed { index, name ->
        GroupMember(id = "member-$index", persona = PersonaUiState(displayName = name))
    }

    @Test
    fun callAllPhrasesSelectEveryMember() {
        val selected = explicitlyRequestedGroupMembers("What do all of you think?", members, emptyList())
        assertEquals(4, selected?.size)
        assertEquals(members.map { it.id }, selected?.map { it.id })
        assertEquals(4, explicitlyRequestedGroupMembers("@all respond", members, emptyList())?.size)
        assertNull(explicitlyRequestedGroupMembers("@alligator hello", members, emptyList()))
    }

    @Test
    fun mentionsOverrideInTextOrder() {
        val selected = explicitlyRequestedGroupMembers("@Airi then @Haruka, thoughts?", members, emptyList())
        assertEquals(listOf("member-3", "member-0"), selected?.map { it.id })
    }

    @Test
    fun ordinaryMessageHasNoExplicitOverride() {
        assertNull(explicitlyRequestedGroupMembers("What should we watch?", members, emptyList()))
    }

    @Test
    fun fairRotationPrefersNeverAndLeastRecentlySpoken() {
        val history = listOf(
            ChatMessage(role = "model", content = "one", speakerId = "member-1"),
            ChatMessage(role = "model", content = "two", speakerId = "member-0")
        )
        assertEquals(
            listOf("member-2", "member-3", "member-1"),
            fairGroupSpeakers(members, history, 3).map { it.id }
        )
    }

    @Test
    fun directorAliasesAreValidatedAndDeduplicated() {
        val selected = membersForDirectorAliases(listOf("m4", "M2", "M4", "M9", "bad"), members)
        assertEquals(listOf("member-3", "member-1"), selected.map { it.id })
    }

    @Test
    fun legacyResponseRoundsMigrateToFixedMode() {
        assertEquals(GroupResponseMode.Three, restoredGroupResponseMode("", 3))
        assertEquals(GroupResponseMode.Auto, restoredGroupResponseMode("Auto", 3))
    }
}
