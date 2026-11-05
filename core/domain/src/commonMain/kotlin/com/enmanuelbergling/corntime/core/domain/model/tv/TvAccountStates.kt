package com.enmanuelbergling.corntime.core.domain.model.tv

data class TvAccountStates(
    val id: Int,
    val favorite: Boolean,
    val watchlist: Boolean,
)
