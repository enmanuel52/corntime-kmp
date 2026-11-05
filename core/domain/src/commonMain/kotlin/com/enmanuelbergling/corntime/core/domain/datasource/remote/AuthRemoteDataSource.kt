package com.enmanuelbergling.corntime.core.domain.datasource.remote

import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost

/** TMDB's three-step login: request token → validate it with the credentials → exchange it for a session. */
interface AuthRemoteDataSource {
    /** Creates an intermediate request token that is later validated with the user's credentials. */
    suspend fun createRequestToken(): NetworkResult<String>

    /** Validates [sessionPost]'s token with the credentials. Fails as unauthorized when they are wrong. */
    suspend fun validateWithLogin(sessionPost: CreateSessionPost): NetworkResult<String>

    /** Exchanges a validated [requestToken] for the session id that account endpoints require. */
    suspend fun createSessionId(requestToken: String): NetworkResult<String>
}
