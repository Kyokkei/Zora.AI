package com.yozora.aichat.ui.companion

import android.content.Context
import android.net.Uri

internal class CompanionSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun vrmUri(sessionId: String): Uri? = preferences.getString(vrmKey(sessionId), null)?.let(Uri::parse)

    fun saveVrmUri(sessionId: String, uri: Uri?) {
        preferences.edit().apply {
            if (uri == null) remove(vrmKey(sessionId)) else putString(vrmKey(sessionId), uri.toString())
        }.apply()
    }

    fun systemPrompt(sessionId: String): String =
        preferences.getString(promptKey(sessionId), "").orEmpty()

    fun saveSystemPrompt(sessionId: String, prompt: String) {
        preferences.edit().putString(promptKey(sessionId), prompt).apply()
    }

    fun voiceId(sessionId: String): String? = preferences.getString(voiceKey(sessionId), null)

    fun saveVoiceId(sessionId: String, voiceId: String) {
        preferences.edit().putString(voiceKey(sessionId), voiceId).apply()
    }

    private fun vrmKey(sessionId: String) = "vrm_$sessionId"
    private fun promptKey(sessionId: String) = "companion_prompt_$sessionId"
    private fun voiceKey(sessionId: String) = "companion_voice_$sessionId"

    private companion object {
        const val PREFERENCES_NAME = "companion_vrm_prefs"
    }
}
