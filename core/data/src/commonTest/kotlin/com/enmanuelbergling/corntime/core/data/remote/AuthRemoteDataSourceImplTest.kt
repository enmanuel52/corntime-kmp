package com.enmanuelbergling.corntime.core.data.remote

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.data.network.MockHttp
import com.enmanuelbergling.corntime.core.data.network.json
import com.enmanuelbergling.corntime.core.data.network.path
import com.enmanuelbergling.corntime.core.data.network.status
import com.enmanuelbergling.corntime.core.data.remote.datasource.AuthRemoteDataSourceImpl
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AuthRemoteDataSourceImplTest {

    private fun HttpRequestData.bodyText() = (body as TextContent).text

    @Test
    fun givenTmdbAnswers_whenCreatingRequestToken_thenReturnsTheToken() = runTest {
        val http = MockHttp { json("""{"success":true,"request_token":"abc"}""") }

        val result = AuthRemoteDataSourceImpl(http.client).createRequestToken()

        assertThat(result).isEqualTo(Result.Success("abc"))
        assertThat(http.lastRequest.method).isEqualTo(HttpMethod.Get)
        assertThat(http.lastRequest.path).isEqualTo("/3/authentication/token/new")
    }

    @Test
    fun givenCredentials_whenValidating_thenPostsThemWithTheTokenAndReturnsTheValidatedToken() = runTest {
        val http = MockHttp { json("""{"success":true,"request_token":"validated"}""") }

        val result = AuthRemoteDataSourceImpl(http.client)
            .validateWithLogin(CreateSessionPost("tim", "secret123", "abc"))

        assertThat(result).isEqualTo(Result.Success("validated"))
        assertThat(http.lastRequest.path).isEqualTo("/3/authentication/token/validate_with_login")
        val body = http.lastRequest.bodyText()
        assertThat(body).contains("\"username\":\"tim\"")
        assertThat(body).contains("\"password\":\"secret123\"")
        assertThat(body).contains("\"request_token\":\"abc\"")
    }

    @Test
    fun givenWrongCredentials_whenValidating_thenUnauthorized() = runTest {
        val http = MockHttp { status(HttpStatusCode.Unauthorized) }

        val result = AuthRemoteDataSourceImpl(http.client)
            .validateWithLogin(CreateSessionPost("tim", "wrong-pass", "abc"))

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
    }

    @Test
    fun givenValidatedToken_whenCreatingSession_thenReturnsTheSessionId() = runTest {
        val http = MockHttp { json("""{"success":true,"session_id":"session-9"}""") }

        val result = AuthRemoteDataSourceImpl(http.client).createSessionId("validated")

        assertThat(result).isEqualTo(Result.Success("session-9"))
        assertThat(http.lastRequest.path).isEqualTo("/3/authentication/session/new")
        assertThat(http.lastRequest.bodyText()).contains("\"request_token\":\"validated\"")
    }

    @Test
    fun givenMalformedBody_whenCreatingRequestToken_thenSerializationError() = runTest {
        val http = MockHttp { json("[]") }

        val result = AuthRemoteDataSourceImpl(http.client).createRequestToken()

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Serialization))
    }
}
