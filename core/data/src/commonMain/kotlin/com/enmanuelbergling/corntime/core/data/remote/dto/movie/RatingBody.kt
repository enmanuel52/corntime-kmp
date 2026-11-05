package com.enmanuelbergling.corntime.core.data.remote.dto.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RatingBody(
    @SerialName("value")
    val value: Float,
)
