package com.enmanuelbergling.corntime.core.domain.error

sealed interface DataError : Error {
    enum class Network : DataError {
        BadRequest,
        RequestTimeout,
        Unauthorized,
        Forbidden,
        NotFound,
        Conflict,
        TooManyRequests,
        NoInternet,
        ServerError,
        ServiceUnavailable,
        Serialization,
        Unknown,
    }

    enum class Local : DataError {
        DiskFull,
        NotFound,
        Unknown,
    }
}
