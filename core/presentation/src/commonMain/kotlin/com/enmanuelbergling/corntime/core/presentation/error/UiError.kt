package com.enmanuelbergling.corntime.core.presentation.error

import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.Error

/**
 * User-facing failure categories. State holds this instead of a string so each platform can localize
 * it with its own resources (Android string resources, iOS String Catalog).
 */
enum class UiError {
    Unauthorized,
    NoInternet,
    Timeout,
    Server,
    Unknown,
}

fun DataError.toUiError(): UiError = when (this) {
    DataError.Network.Unauthorized, DataError.Network.Forbidden -> UiError.Unauthorized
    DataError.Network.NoInternet -> UiError.NoInternet
    DataError.Network.RequestTimeout -> UiError.Timeout
    DataError.Network.ServerError, DataError.Network.ServiceUnavailable -> UiError.Server
    else -> UiError.Unknown
}

/** Any typed [Error] that is not a [DataError] (e.g. validation) is reported as unknown unless mapped by its feature. */
fun Error.toUiError(): UiError = (this as? DataError)?.toUiError() ?: UiError.Unknown
