package com.enmanuelbergling.corntime.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** Test client built exactly like production (same defaults/plugins) but over a [MockEngine]. */
class MockHttp(
    private val handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()

    val client: HttpClient = createTmdbHttpClient(
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
        apiKey = "test-key",
        language = "es-MX",
    )

    val lastRequest get() = requests.last()
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

fun MockRequestHandleScope.status(status: HttpStatusCode) = respond("{}", status)

fun HttpRequestData.query(name: String): String? = url.parameters[name]

val HttpRequestData.path: String get() = url.encodedPath

object Json {
    fun moviePage(totalPages: Int, vararg ids: Int) = """
        {"page":1,"total_pages":$totalPages,"total_results":${ids.size},
         "results":[${ids.joinToString(",") { movie(it) }}]}
    """.trimIndent()

    fun movie(id: Int, posterPath: String? = "/p$id.jpg") = """
        {"adult":false,"backdrop_path":null,"genre_ids":[1],"id":$id,"original_language":"en",
         "original_title":"Movie $id","overview":"o","popularity":1.0,
         "poster_path":${posterPath?.let { "\"$it\"" } ?: "null"},"release_date":"2020-01-01",
         "title":"Movie $id","video":false,"vote_average":8.0,"vote_count":10,"unknown_field":"ignored"}
    """.trimIndent()

    const val WATCH_RESPONSE = """{"status_code":1,"status_message":"Success."}"""

    fun account(id: Int) = """
        {"avatar":{"gravatar":{"hash":"h"},"tmdb":{"avatar_path":null}},"id":$id,"iso_639_1":"en",
         "iso_3166_1":"US","name":"Tim","include_adult":false,"username":"timcorn"}
    """.trimIndent()
}
