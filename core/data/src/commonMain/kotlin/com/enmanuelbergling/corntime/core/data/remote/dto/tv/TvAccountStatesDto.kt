package com.enmanuelbergling.corntime.core.data.remote.dto.tv

import kotlinx.serialization.Serializable

@Serializable
internal data class TvAccountStatesDto(
    val id: Int,
    val favorite: Boolean,
    val watchlist: Boolean,
)
