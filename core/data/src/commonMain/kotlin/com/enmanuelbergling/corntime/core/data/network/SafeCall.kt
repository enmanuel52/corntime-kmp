package com.enmanuelbergling.corntime.core.data.network

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/** Executes a Ktor request and maps transport failures and HTTP statuses to [DataError.Network]. */
suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): NetworkResult<T> {
    val response = try {
        execute()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: HttpRequestTimeoutException) {
        return Result.Failure(DataError.Network.RequestTimeout)
    } catch (_: IOException) {
        return Result.Failure(DataError.Network.NoInternet)
    } catch (_: SerializationException) {
        return Result.Failure(DataError.Network.Serialization)
    } catch (_: Exception) {
        return Result.Failure(DataError.Network.Unknown)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): NetworkResult<T> =
    when (response.status.value) {
        in 200..299 -> try {
            Result.Success(response.body<T>())
        } catch (_: NoTransformationFoundException) {
            Result.Failure(DataError.Network.Serialization)
        } catch (_: JsonConvertException) {
            Result.Failure(DataError.Network.Serialization)
        } catch (_: SerializationException) {
            Result.Failure(DataError.Network.Serialization)
        }

        400 -> Result.Failure(DataError.Network.BadRequest)
        401 -> Result.Failure(DataError.Network.Unauthorized)
        403 -> Result.Failure(DataError.Network.Forbidden)
        404 -> Result.Failure(DataError.Network.NotFound)
        408 -> Result.Failure(DataError.Network.RequestTimeout)
        409 -> Result.Failure(DataError.Network.Conflict)
        429 -> Result.Failure(DataError.Network.TooManyRequests)
        503 -> Result.Failure(DataError.Network.ServiceUnavailable)
        in 500..599 -> Result.Failure(DataError.Network.ServerError)
        else -> Result.Failure(DataError.Network.Unknown)
    }
