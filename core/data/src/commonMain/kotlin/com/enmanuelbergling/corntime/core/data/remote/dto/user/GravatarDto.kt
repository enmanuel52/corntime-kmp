package com.enmanuelbergling.corntime.core.data.remote.dto.user


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GravatarDto(
    @SerialName("hash")
    val hash: String
)