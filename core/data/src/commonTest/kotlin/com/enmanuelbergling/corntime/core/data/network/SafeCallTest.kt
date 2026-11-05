package com.enmanuelbergling.corntime.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SafeCallTest {

    @Serializable
    private data class Payload(val value: Int)

    private suspend fun call(handler: io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        MockHttp(handler).let { http -> safeCall<Payload> { http.client.get("x") } }

    @Test
    fun given200WithValidJson_whenCalling_thenSuccessWithParsedBody() = runTest {
        assertThat(call { json("""{"value":7}""") }).isEqualTo(Result.Success(Payload(7)))
    }

    @Test
    fun givenUnknownJsonFields_whenCalling_thenTheyAreIgnored() = runTest {
        assertThat(call { json("""{"value":7,"extra":true}""") }).isEqualTo(Result.Success(Payload(7)))
    }

    @Test
    fun given200WithMalformedBody_whenCalling_thenSerializationError() = runTest {
        assertThat(call { json("""{"value":"not a number"}""") })
            .isEqualTo(Result.Failure(DataError.Network.Serialization))
    }

    @Test
    fun givenEmpty200Body_whenCalling_thenSerializationErrorInsteadOfCrash() = runTest {
        assertThat(call { json("") }).isEqualTo(Result.Failure(DataError.Network.Serialization))
    }

    @Test
    fun givenHttpErrorStatuses_whenCalling_thenEachMapsToItsTypedError() = runTest {
        val expected = mapOf(
            HttpStatusCode.BadRequest to DataError.Network.BadRequest,
            HttpStatusCode.Unauthorized to DataError.Network.Unauthorized,
            HttpStatusCode.Forbidden to DataError.Network.Forbidden,
            HttpStatusCode.NotFound to DataError.Network.NotFound,
            HttpStatusCode.RequestTimeout to DataError.Network.RequestTimeout,
            HttpStatusCode.Conflict to DataError.Network.Conflict,
            HttpStatusCode.TooManyRequests to DataError.Network.TooManyRequests,
            HttpStatusCode.InternalServerError to DataError.Network.ServerError,
            HttpStatusCode.BadGateway to DataError.Network.ServerError,
            HttpStatusCode.ServiceUnavailable to DataError.Network.ServiceUnavailable,
            HttpStatusCode.fromValue(418) to DataError.Network.Unknown,
        )
        expected.forEach { (status, error) ->
            assertThat(call { respond("{}", status) }).isEqualTo(Result.Failure(error))
        }
    }

    @Test
    fun givenRequestTimeout_whenCalling_thenRequestTimeoutError() = runTest {
        val result = call { throw HttpRequestTimeoutException("x", 1) }
        assertThat(result).isEqualTo(Result.Failure(DataError.Network.RequestTimeout))
    }

    @Test
    fun givenNoConnectivity_whenCalling_thenNoInternetError() = runTest {
        assertThat(call { throw IOException("offline") }).isEqualTo(Result.Failure(DataError.Network.NoInternet))
    }

    @Test
    fun givenUnexpectedException_whenCalling_thenUnknownError() = runTest {
        assertThat(call { throw IllegalStateException("boom") }).isEqualTo(Result.Failure(DataError.Network.Unknown))
    }

    @Test
    fun givenCancellation_whenCalling_thenItIsRethrownNotSwallowed() = runTest {
        assertFailsWith<CancellationException> { call { throw CancellationException("cancelled") } }
    }
}
