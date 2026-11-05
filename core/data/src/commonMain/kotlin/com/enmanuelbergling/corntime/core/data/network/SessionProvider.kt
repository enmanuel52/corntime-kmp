package com.enmanuelbergling.corntime.core.data.network

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Factory

/**
 * Resolves the credentials account endpoints need. The account id comes from the logged-in user
 * (synced at login) instead of a hard-coded build constant.
 */
@Factory
class SessionProvider(
    private val authPreferenceDataSource: AuthPreferenceDataSource,
    private val userPreferenceDataSource: UserPreferenceDataSource,
) {
    suspend fun sessionId(): String? =
        authPreferenceDataSource.getSessionId().firstOrNull()?.takeIf { it.isNotBlank() }

    suspend fun accountId(): Int? =
        userPreferenceDataSource.getCurrentUser().firstOrNull()?.id?.takeIf { it > 0 }

    /** Runs [block] with the session id, or fails as unauthorized when logged out. */
    suspend inline fun <T> withSession(block: (sessionId: String) -> NetworkResult<T>): NetworkResult<T> {
        val sessionId = sessionId() ?: return Result.Failure(DataError.Network.Unauthorized)
        return block(sessionId)
    }

    /** Runs [block] with session and account ids, or fails as unauthorized when either is missing. */
    suspend inline fun <T> withAccount(
        block: (sessionId: String, accountId: Int) -> NetworkResult<T>,
    ): NetworkResult<T> {
        val sessionId = sessionId() ?: return Result.Failure(DataError.Network.Unauthorized)
        val accountId = accountId() ?: return Result.Failure(DataError.Network.Unauthorized)
        return block(sessionId, accountId)
    }
}
