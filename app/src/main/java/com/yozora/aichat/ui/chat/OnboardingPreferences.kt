package com.yozora.aichat.ui.chat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit

internal val ROLEPLAY_UI_MODE_ENABLED_KEY = booleanPreferencesKey("roleplay_ui_mode_enabled_v2")
internal val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("roleplay_onboarding_completed_v1")

internal fun Preferences.roleplayUiModeEnabled(): Boolean =
    this[ROLEPLAY_UI_MODE_ENABLED_KEY] ?: DEFAULT_ROLEPLAY_UI_ENABLED

internal fun Preferences.onboardingCompleted(): Boolean =
    this[ONBOARDING_COMPLETED_KEY] ?: false

internal suspend fun DataStore<Preferences>.persistOnboardingCompleted() {
    edit { it[ONBOARDING_COMPLETED_KEY] = true }
}
