package com.yozora.aichat.ui.chat

import com.yozora.aichat.ui.OnboardingAction

const val DEFAULT_ROLEPLAY_UI_ENABLED = true
const val ONBOARDING_STEP_COUNT = 10

internal data class OnboardingUiState(
    val visible: Boolean = false,
    val completed: Boolean = false,
    val stepIndex: Int = 0
)

internal sealed interface OnboardingEvent {
    data object ShowIfNeeded : OnboardingEvent
    data object Replay : OnboardingEvent
    data object Advance : OnboardingEvent
    data object Retreat : OnboardingEvent
    data class Jump(val index: Int) : OnboardingEvent
    data object Complete : OnboardingEvent
    data class Action(val action: OnboardingAction) : OnboardingEvent
}

internal sealed interface OnboardingEffect {
    data class SetRoleplayMode(val enabled: Boolean) : OnboardingEffect
    data object PersistCompleted : OnboardingEffect
    data object OpenApiKey : OnboardingEffect
    data object OpenCreateCharacter : OnboardingEffect
}

internal data class OnboardingTransition(
    val state: OnboardingUiState,
    val effects: List<OnboardingEffect> = emptyList()
)

internal fun dispatchOnboardingEffects(
    effects: List<OnboardingEffect>,
    setRoleplayMode: (Boolean) -> Unit,
    persistCompleted: () -> Unit,
    openApiKey: () -> Unit,
    openCreateCharacter: () -> Unit
) {
    effects.forEach { effect ->
        when (effect) {
            is OnboardingEffect.SetRoleplayMode -> setRoleplayMode(effect.enabled)
            OnboardingEffect.PersistCompleted -> persistCompleted()
            OnboardingEffect.OpenApiKey -> openApiKey()
            OnboardingEffect.OpenCreateCharacter -> openCreateCharacter()
        }
    }
}

internal fun reduceOnboarding(
    state: OnboardingUiState,
    event: OnboardingEvent
): OnboardingTransition = when (event) {
    OnboardingEvent.ShowIfNeeded -> OnboardingTransition(
        if (state.completed) state else state.copy(visible = true, stepIndex = 0)
    )
    OnboardingEvent.Replay -> OnboardingTransition(state.copy(visible = true, stepIndex = 0))
    OnboardingEvent.Advance -> if (state.stepIndex < ONBOARDING_STEP_COUNT - 1) {
        OnboardingTransition(state.copy(stepIndex = state.stepIndex + 1))
    } else {
        completeOnboardingTransition(state)
    }
    OnboardingEvent.Retreat -> OnboardingTransition(
        state.copy(stepIndex = (state.stepIndex - 1).coerceAtLeast(0))
    )
    is OnboardingEvent.Jump -> OnboardingTransition(
        state.copy(stepIndex = event.index.coerceIn(0, ONBOARDING_STEP_COUNT - 1))
    )
    OnboardingEvent.Complete -> completeOnboardingTransition(state)
    is OnboardingEvent.Action -> when (val action = event.action) {
        is OnboardingAction.SetRoleplayMode -> OnboardingTransition(
            state,
            listOf(OnboardingEffect.SetRoleplayMode(action.enabled))
        )
        OnboardingAction.Next -> reduceOnboarding(state, OnboardingEvent.Advance)
        OnboardingAction.Finish -> completeOnboardingTransition(state)
        OnboardingAction.DeepLinkApiKey -> completeOnboardingTransition(
            state,
            OnboardingEffect.OpenApiKey
        )
        OnboardingAction.DeepLinkCreateChar -> completeOnboardingTransition(
            state,
            OnboardingEffect.OpenCreateCharacter
        )
    }
}

private fun completeOnboardingTransition(
    state: OnboardingUiState,
    afterPersist: OnboardingEffect? = null
): OnboardingTransition = OnboardingTransition(
    state = state.copy(visible = false, completed = true),
    effects = buildList {
        add(OnboardingEffect.PersistCompleted)
        if (afterPersist != null) add(afterPersist)
    }
)
