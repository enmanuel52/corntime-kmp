package com.enmanuelbergling.corntime.core.data.network

import co.touchlab.kermit.Logger as KermitLogger
import com.enmanuelbergling.corntime.core.data.Secrets
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

internal val TmdbJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Platform engine: OkHttp on Android, Darwin (NSURLSession) on iOS. */
internal expect fun platformHttpEngine(): HttpClientEngine

/** Device language in TMDB format, e.g. `es-ES`. */
internal expect fun currentLanguage(): String

/**
 * Single TMDB client. Every request carries `api_key` and `language`; non-2xx responses are not
 * thrown (Ktor's default) so [safeCall] can map them to typed errors.
 */
fun createTmdbHttpClient(
    engine: HttpClientEngine = platformHttpEngine(),
    apiKey: String = Secrets.API_KEY,
    language: String = currentLanguage(),
): HttpClient = HttpClient(engine) {
    defaultRequest {
        url(TMDB_BASE_URL)
        url.parameters.append("api_key", apiKey)
        url.parameters.append("language", language)
        contentType(ContentType.Application.Json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000L
    }
    install(ContentNegotiation) {
        json(TmdbJson)
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) = KermitLogger.withTag("TMDB").d { message }
        }
        sanitizeHeader { true }
    }
}
