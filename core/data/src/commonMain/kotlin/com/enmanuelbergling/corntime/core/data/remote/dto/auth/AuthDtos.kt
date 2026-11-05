package com.enmanuelbergling.corntime.core.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RequestTokenDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("request_token") val token: String = "",
)

@Serializable
internal data class SessionDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("session_id") val sessionId: String = "",
)

@Serializable
internal data class CreateSessionBody(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("request_token") val requestToken: String,
)

@Serializable
internal data class RequestTokenBody(
    @SerialName("request_token") val token: String,
)
