package com.yozora.aichat.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApiKeyManagerTest {
    @Test
    fun addUpdateDeleteAndResolveVaultEntries() = withManager { manager ->
        val firstId = manager.addVaultEntry("First", "key-one")!!
        val secondId = manager.addVaultEntry("Second", "key-two")!!
        assertEquals("key-one", manager.resolveVaultKey(firstId))

        assertTrue(manager.updateVaultEntry(firstId, "Renamed", "key-one-new"))
        assertEquals("key-one-new", manager.resolveVaultKey(firstId))

        manager.deleteVaultEntry(firstId)
        assertNull(manager.resolveVaultKey(firstId))
        assertEquals(listOf(secondId), manager.vaultEntries().first().map { it.id })
    }

    @Test
    fun vaultRejectsEntryTwentyOne() = withManager { manager ->
        repeat(ApiKeyManager.MAX_VAULT_ENTRIES) { index ->
            manager.addVaultEntry("Key $index", "secret-$index")
        }
        assertNull(manager.addVaultEntry("Too many", "secret-20"))
        assertEquals(ApiKeyManager.MAX_VAULT_ENTRIES, manager.vaultEntries().first().size)
    }

    private fun withManager(block: suspend (ApiKeyManager) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File.createTempFile("api-key-manager", ".preferences_pb").apply { delete() }
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            block(ApiKeyManager(store))
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
