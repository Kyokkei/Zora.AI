package com.yozora.aichat.ui.chat

import com.yozora.aichat.data.datastore.SavedApiKeyEntry
import com.yozora.aichat.data.datastore.decodeVaultEntries
import com.yozora.aichat.data.datastore.encodeVaultEntries
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SelectedApiKeyTest {
    @Test
    fun selectedKeysRoundTrip() {
        val values = listOf(
            SelectedApiKey.None,
            SelectedApiKey.VaultEntry("vault-id"),
            SelectedApiKey.RawKey("secret-key")
        )
        values.forEach { assertEquals(it, selectedApiKeyFromStored(it.toStoredString())) }
    }

    @Test
    fun legacyRawKeyDecodesAsRawSelection() {
        assertEquals(SelectedApiKey.RawKey("old-secret"), selectedApiKeyFromStored("old-secret"))
    }

    @Test
    fun migrationWrapsRawButLeavesSelectedJsonUnchanged() {
        val rawJson = SelectedApiKey.RawKey("secret").toStoredString()
        val vaultJson = SelectedApiKey.VaultEntry("id").toStoredString()
        assertEquals(rawJson, migrateSelectedApiKeyValue("secret"))
        assertEquals(rawJson, migrateSelectedApiKeyValue(rawJson))
        assertEquals(vaultJson, migrateSelectedApiKeyValue(vaultJson))
    }

    @Test
    fun vaultEntriesRoundTrip() {
        val entries = listOf(
            SavedApiKeyEntry("1", "Gemini project", "AIza-secret"),
            SavedApiKeyEntry("2", "Claude", "sk-ant-secret")
        )
        assertEquals(entries, decodeVaultEntries(encodeVaultEntries(entries)))
    }

    @Test
    fun rawKeyResolvesDirectly() = runBlocking {
        assertEquals("secret", resolveSelectedApiKey(SelectedApiKey.RawKey(" secret ")) { null })
    }

    @Test
    fun deletedVaultReferenceResolvesToNull() = runBlocking {
        assertEquals(null, resolveSelectedApiKey(SelectedApiKey.VaultEntry("deleted")) { null })
    }
}
