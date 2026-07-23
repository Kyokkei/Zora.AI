package com.yozora.aichat.ui.chat

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.yozora.aichat.ui.OnboardingAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OnboardingStateMachineTest {
    @Test
    fun freshDataStoreDefaultsToRoleplayUi() = withStore { store ->
        assertTrue(store.data.first().roleplayUiModeEnabled())
    }

    @Test
    fun advanceFromLastStepCompletesAndRequestsPersistence() {
        val result = reduceOnboarding(
            OnboardingUiState(visible = true, stepIndex = ONBOARDING_STEP_COUNT - 1),
            OnboardingEvent.Advance
        )

        assertFalse(result.state.visible)
        assertTrue(result.state.completed)
        assertTrue(OnboardingEffect.PersistCompleted in result.effects)
    }

    @Test
    fun retreatAtFirstStepIsNoOp() {
        val initial = OnboardingUiState(visible = true, stepIndex = 0)
        assertEquals(initial, reduceOnboarding(initial, OnboardingEvent.Retreat).state)
    }

    @Test
    fun setRoleplayActionRequestsImmediateModeUpdate() {
        val result = reduceOnboarding(
            OnboardingUiState(visible = true),
            OnboardingEvent.Action(OnboardingAction.SetRoleplayMode(true))
        )

        var selectedMode: Boolean? = null
        dispatchOnboardingEffects(
            effects = result.effects,
            setRoleplayMode = { selectedMode = it },
            persistCompleted = {},
            openApiKey = {},
            openCreateCharacter = {}
        )
        assertEquals(true, selectedMode)
    }

    @Test
    fun completeMarksStateAndPersistsToDataStore() = withStore { store ->
        val result = reduceOnboarding(
            OnboardingUiState(visible = true),
            OnboardingEvent.Complete
        )
        var persistRequested = false
        dispatchOnboardingEffects(
            effects = result.effects,
            setRoleplayMode = {},
            persistCompleted = { persistRequested = true },
            openApiKey = {},
            openCreateCharacter = {}
        )
        if (persistRequested) store.persistOnboardingCompleted()

        assertEquals(OnboardingUiState(visible = false, completed = true), result.state)
        assertTrue(store.data.first().onboardingCompleted())
    }

    @Test
    fun showIfNeededDoesNothingAfterCompletion() {
        val completed = OnboardingUiState(completed = true, stepIndex = 6)
        assertEquals(completed, reduceOnboarding(completed, OnboardingEvent.ShowIfNeeded).state)
    }

    @Test
    fun replayAlwaysShowsFromFirstStep() {
        val result = reduceOnboarding(
            OnboardingUiState(visible = false, completed = true, stepIndex = 8),
            OnboardingEvent.Replay
        )

        assertEquals(OnboardingUiState(visible = true, completed = true, stepIndex = 0), result.state)
    }

    private fun withStore(block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File.createTempFile("onboarding", ".preferences_pb").apply { delete() }
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            block(store)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
