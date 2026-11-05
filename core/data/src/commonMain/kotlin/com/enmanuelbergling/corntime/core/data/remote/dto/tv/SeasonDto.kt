package com.enmanuelbergling.corntime.core.data.remote.dto.tv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SeasonDto(
    @SerialName("id")
    val id: Int,
    @SerialName("season_number")
    val seasonNumber: Int,
    @SerialName("name")
    val name: String = "",
    @SerialName("overview")
    val overview: String = "",
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("episode_count")
    val episodeCount: Int = 0,
    @SerialName("air_date")
    val airDate: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = .0,
)
