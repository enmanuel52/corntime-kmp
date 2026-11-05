package com.enmanuelbergling.corntime.core.data.remote.dto.tv

import com.enmanuelbergling.corntime.core.data.remote.dto.movie.CastDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.CrewDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TvCreditsDto(
    @SerialName("id")
    val id: Int,
    @SerialName("cast")
    val cast: List<CastDto>,
    @SerialName("crew")
    val crew: List<CrewDto>,
)
