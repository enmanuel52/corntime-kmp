package com.enmanuelbergling.corntime.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TmdbHttpClientTest {

    @Test
    fun givenAnyRequest_whenSent_thenCarriesBaseUrlApiKeyAndLanguage() = runTest {
        val http = MockHttp { json("{}") }

        http.client.get("movie/popular")

        assertThat(http.lastRequest.url.host).isEqualTo("api.themoviedb.org")
        assertThat(http.lastRequest.path).isEqualTo("/3/movie/popular")
        assertThat(http.lastRequest.query("api_key")).isEqualTo("test-key")
        assertThat(http.lastRequest.query("language")).isEqualTo("es-MX")
    }
}
