package com.enmanuelbergling.corntime.core.data.remote.dto.user


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AvatarDto(
    @SerialName("gravatar")
    val gravatar: GravatarDto,
    @SerialName("tmdb")
    val tmdb: TmdbDto
)