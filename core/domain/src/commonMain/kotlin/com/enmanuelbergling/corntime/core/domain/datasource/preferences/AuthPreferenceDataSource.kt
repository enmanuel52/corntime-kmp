package com.enmanuelbergling.corntime.core.domain.datasource.preferences

import kotlinx.coroutines.flow.Flow

/** Stores the TMDB session id securely (Android Keystore / iOS Keychain). Empty means logged out. */
interface AuthPreferenceDataSource {
    suspend fun saveSessionId(sessionId: String)
    fun getSessionId(): Flow<String>
    suspend fun clear()
}
