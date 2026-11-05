package com.enmanuelbergling.corntime.core.data.remote.dto.tv


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TvShowPageDto(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<TvShowDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)
