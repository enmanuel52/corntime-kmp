package com.enmanuelbergling.corntime.feature.auth.domain.login

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.AuthRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.usecase.LogoutUseCase
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/** Mutable holder the login chain fills: each handler adds what it obtained for the next one. */
class LoginRequest(
    val username: String,
    val password: String,
    var requestToken: String? = null,
    var sessionId: String? = null,
)

class CreateRequestTokenChainHandler(
    private val dataSource: AuthRemoteDataSource,
    override val next: ChainHandler<LoginRequest>? = null,
) : ChainHandler<LoginRequest> {
    override suspend fun handle(request: LoginRequest) {
        request.requestToken = dataSource.createRequestToken().getOrThrow()
    }
}

/** Fails as unauthorized when TMDB rejects the credentials. */
class ValidateLoginChainHandler(
    private val dataSource: AuthRemoteDataSource,
    override val next: ChainHandler<LoginRequest>? = null,
) : ChainHandler<LoginRequest> {
    override suspend fun handle(request: LoginRequest) {
        val token = checkNotNull(request.requestToken) { "a request token must be created first" }
        request.requestToken = dataSource
            .validateWithLogin(CreateSessionPost(request.username, request.password, token))
            .getOrThrow()
    }
}

class CreateSessionChainHandler(
    private val dataSource: AuthRemoteDataSource,
    override val next: ChainHandler<LoginRequest>? = null,
) : ChainHandler<LoginRequest> {
    override suspend fun handle(request: LoginRequest) {
        val token = checkNotNull(request.requestToken) { "a request token must be validated first" }
        request.sessionId = dataSource.createSessionId(token).getOrThrow()
    }
}

/** Only reached after every earlier step succeeded, so a failed login never leaves a session behind. */
class StoreSessionChainHandler(
    private val authPreferenceDataSource: AuthPreferenceDataSource,
    override val next: ChainHandler<LoginRequest>? = null,
) : ChainHandler<LoginRequest> {
    override suspend fun handle(request: LoginRequest) {
        authPreferenceDataSource.saveSessionId(checkNotNull(request.sessionId) { "no session was created" })
    }
}

/**
 * Fetches the account (its id is needed by every account endpoint). When that fails the stored
 * session is discarded again: a session without an account would look logged in but fail every call.
 */
class SyncAccountChainHandler(
    private val syncUserDetails: SyncUserDetailsUseCase,
    private val logout: LogoutUseCase,
    override val next: ChainHandler<LoginRequest>? = null,
) : ChainHandler<LoginRequest> {
    override suspend fun handle(request: LoginRequest) {
        try {
            syncUserDetails().getOrThrow()
        } catch (exception: DataException) {
            logout()
            throw exception
        }
    }
}

/** request token → validate with credentials → session → store session → sync account. */
@Factory
class LoginChain(
    private val dataSource: AuthRemoteDataSource,
    private val authPreferenceDataSource: AuthPreferenceDataSource,
    private val syncUserDetails: SyncUserDetailsUseCase,
    private val logout: LogoutUseCase,
) {
    fun create(): ChainHandler<LoginRequest> = CreateRequestTokenChainHandler(
        dataSource,
        next = ValidateLoginChainHandler(
            dataSource,
            next = CreateSessionChainHandler(
                dataSource,
                next = StoreSessionChainHandler(
                    authPreferenceDataSource,
                    next = SyncAccountChainHandler(syncUserDetails, logout),
                ),
            ),
        ),
    )
}
