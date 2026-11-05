package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.remote.AuthRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.testing.util.FakeBehavior

enum class AuthRemoteFunction { CreateRequestToken, ValidateWithLogin, CreateSessionId }

class FakeAuthRemoteDataSource : FakeBehavior<AuthRemoteFunction>(), AuthRemoteDataSource {
    var requestToken = "request-token"
    var sessionId = "session-id"

    override suspend fun createRequestToken() =
        respond(AuthRemoteFunction.CreateRequestToken) { requestToken }

    override suspend fun validateWithLogin(sessionPost: CreateSessionPost) =
        respond(AuthRemoteFunction.ValidateWithLogin, sessionPost) { sessionPost.requestToken }

    override suspend fun createSessionId(requestToken: String) =
        respond(AuthRemoteFunction.CreateSessionId, requestToken) { sessionId }
}
