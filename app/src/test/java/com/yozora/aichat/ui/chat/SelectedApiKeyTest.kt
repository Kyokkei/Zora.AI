package com.yozora.aichat.ui.chat

import com.yozora.aichat.data.datastore.SavedApiKeyEntry
import com.yozora.aichat.data.datastore.decodeVaultEntries
import com.yozora.aichat.data.datastore.encodeVaultEntries
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SelectedApiKeyTest {
    @Test
    fun memberKeyIsUsedWithoutConsultingMissingProviderKey() = runBlocking {
        val member = GroupMember(selectedKey = SelectedApiKey.RawKey("individual-key"))
        assertEquals("individual-key", resolveMemberApiKey(member,
            resolveProvider = { error("Individual key must take priority over the provider key") },
            resolveSelected = { resolveSelectedApiKey(it) { null } }))
    }

    @Test
    fun savedMemberKeyResolvesFromVaultWithoutProviderSelection() = runBlocking {
        val member = GroupMember(selectedKey = SelectedApiKey.VaultEntry("saved-id"))
        assertEquals("saved-key", resolveMemberApiKey(member,
            resolveProvider = { error("Saved member key must take priority") },
            resolveSelected = { selected -> resolveSelectedApiKey(selected) { id ->
                if (id == "saved-id") "saved-key" else null
            } }))
    }

    @Test
    fun memberWithoutOverrideUsesItsMatchingProvider() = runBlocking {
        val member = GroupMember(persona = PersonaUiState(vendor = ApiVendor.Custom))
        assertEquals("custom-provider-key", resolveMemberApiKey(member,
            resolveProvider = { id -> assertEquals("custom", id); "custom-provider-key" },
            resolveSelected = { error("No individual key was selected") }))
    }

    @Test
    fun deletedMemberKeyDoesNotSilentlyUseDifferentProviderKey() = runBlocking {
        val member = GroupMember(selectedKey = SelectedApiKey.VaultEntry("deleted"))
        assertEquals(null, resolveMemberApiKey(member,
            resolveProvider = { error("Do not replace a deleted individual selection with another key") },
            resolveSelected = { null }))
    }

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
