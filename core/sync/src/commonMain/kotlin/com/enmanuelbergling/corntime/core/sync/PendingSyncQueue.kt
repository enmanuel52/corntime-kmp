package com.enmanuelbergling.corntime.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

private val PENDING_KEY = stringPreferencesKey("pending_sync_operations")

/**
 * Operations waiting to reach TMDB, kept on disk so they survive the app being killed. Used where
 * the OS has no persistent work queue (iOS); WorkManager persists its own on Android.
 */
@Single
class PendingSyncQueue(private val dataStore: DataStore<Preferences>) {
    private val mutex = Mutex()

    suspend fun all(): List<SyncOperation> = decodeSyncOperations(dataStore.data.first()[PENDING_KEY])

    suspend fun add(operations: List<SyncOperation>) {
        if (operations.isEmpty()) return
        mutex.withLock { write((all() + operations).distinct()) }
    }

    /**
     * Runs [block] over everything pending and keeps what it returns, merged with operations added in
     * the meantime. Nothing is lost if the process dies mid-run: the queue is only rewritten afterwards.
     */
    suspend fun process(block: suspend (List<SyncOperation>) -> List<SyncOperation>): List<SyncOperation> {
        val snapshot = mutex.withLock { all() }
        if (snapshot.isEmpty()) return emptyList()
        val remaining = block(snapshot)
        return mutex.withLock {
            val addedMeanwhile = all() - snapshot.toSet()
            (remaining + addedMeanwhile).distinct().also { write(it) }
        }
    }

    private suspend fun write(operations: List<SyncOperation>) {
        dataStore.edit { preferences ->
            if (operations.isEmpty()) preferences.remove(PENDING_KEY) else preferences[PENDING_KEY] = operations.encode()
        }
    }
}
