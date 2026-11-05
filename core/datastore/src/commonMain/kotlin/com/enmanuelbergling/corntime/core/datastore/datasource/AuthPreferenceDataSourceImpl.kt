package com.enmanuelbergling.corntime.core.datastore.datasource

import com.enmanuelbergling.corntime.core.datastore.secure.SecureStorage
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

internal const val SESSION_ID_KEY = "session_id"

/** Session id kept in [SecureStorage]; cached in memory so collectors get updates without re-reading. */
@Single
internal class AuthPreferenceDataSourceImpl(
    private val secureStorage: SecureStorage,
) : AuthPreferenceDataSource {

    private val mutex = Mutex()
    private val cache = MutableStateFlow<String?>(null)

    override suspend fun saveSessionId(sessionId: String) = mutex.withLock {
        secureStorage.write(SESSION_ID_KEY, sessionId)
        cache.value = sessionId
    }

    override fun getSessionId(): Flow<String> = flow {
        mutex.withLock {
            if (cache.value == null) cache.value = secureStorage.read(SESSION_ID_KEY).orEmpty()
        }
        emitAll(cache.filterNotNull())
    }

    override suspend fun clear() = mutex.withLock {
        secureStorage.delete(SESSION_ID_KEY)
        cache.value = ""
    }
}
