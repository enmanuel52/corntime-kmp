package com.enmanuelbergling.corntime.core.data.remote.datasource

import com.enmanuelbergling.corntime.core.data.network.safeCall
import com.enmanuelbergling.corntime.core.data.remote.dto.auth.CreateSessionBody
import com.enmanuelbergling.corntime.core.data.remote.dto.auth.RequestTokenBody
import com.enmanuelbergling.corntime.core.data.remote.dto.auth.RequestTokenDto
import com.enmanuelbergling.corntime.core.data.remote.dto.auth.SessionDto
import com.enmanuelbergling.corntime.core.domain.datasource.remote.AuthRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

@Single
internal class AuthRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : AuthRemoteDataSource {

    override suspend fun createRequestToken(): NetworkResult<String> =
        safeCall<RequestTokenDto> { httpClient.get("authentication/token/new") }.map { it.token }

    override suspend fun validateWithLogin(sessionPost: CreateSessionPost): NetworkResult<String> =
        safeCall<RequestTokenDto> {
            httpClient.post("authentication/token/validate_with_login") {
                setBody(CreateSessionBody(sessionPost.username, sessionPost.password, sessionPost.requestToken))
            }
        }.map { it.token }

    override suspend fun createSessionId(requestToken: String): NetworkResult<String> =
        safeCall<SessionDto> {
            httpClient.post("authentication/session/new") { setBody(RequestTokenBody(requestToken)) }
        }.map { it.sessionId }
}
