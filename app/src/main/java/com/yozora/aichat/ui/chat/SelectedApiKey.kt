package com.yozora.aichat.ui.chat

import org.json.JSONObject

sealed class SelectedApiKey {
    data object None : SelectedApiKey()
    data class VaultEntry(val id: String) : SelectedApiKey()
    data class RawKey(val key: String) : SelectedApiKey()
}

internal suspend fun resolveSelectedApiKey(
    selected: SelectedApiKey,
    resolveVault: suspend (String) -> String?
): String? = when (selected) {
    SelectedApiKey.None -> null
    is SelectedApiKey.VaultEntry -> resolveVault(selected.id)
    is SelectedApiKey.RawKey -> selected.key.trim().takeIf { it.isNotBlank() }
}

internal fun SelectedApiKey.toStoredString(): String = when (this) {
    SelectedApiKey.None -> ""
    is SelectedApiKey.VaultEntry -> JSONObject()
        .put("type", "vault")
        .put("id", id)
        .toString()
    is SelectedApiKey.RawKey -> JSONObject()
        .put("type", "raw")
        .put("key", key)
        .toString()
}

internal fun selectedApiKeyFromStored(value: String?): SelectedApiKey {
    val raw = value.orEmpty().trim()
    if (raw.isEmpty()) return SelectedApiKey.None
    val json = runCatching { JSONObject(raw) }.getOrNull()
        ?: return SelectedApiKey.RawKey(raw)
    return when (json.optString("type")) {
        "vault" -> json.optString("id")
            .takeIf { it.isNotBlank() }
            ?.let(SelectedApiKey::VaultEntry)
            ?: SelectedApiKey.None
        "raw" -> json.optString("key")
            .takeIf { it.isNotBlank() }
            ?.let(SelectedApiKey::RawKey)
            ?: SelectedApiKey.None
        else -> SelectedApiKey.RawKey(raw)
    }
}

internal fun migrateSelectedApiKeyValue(value: String): String {
    if (value.isBlank()) return value
    val parsed = runCatching { JSONObject(value) }.getOrNull()
    if (parsed?.optString("type") in setOf("raw", "vault")) return value
    return SelectedApiKey.RawKey(value).toStoredString()
}
