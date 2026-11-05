package com.enmanuelbergling.corntime.core.data.remote.dto.movie

import kotlinx.serialization.Serializable

@Serializable
internal data class MovieAccountStatesDto(
    val id: Int,
    val favorite: Boolean,
    val watchlist: Boolean,
    @Serializable(with = RatedDTOSerializer::class)
    val rated: RatedDto? = null,
)