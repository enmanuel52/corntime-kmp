package com.enmanuelbergling.corntime.core.domain.model.movie

data class MovieAccountStates(
    val id: Int,
    val favorite: Boolean,
    val watchlist: Boolean,
    val rated: Float? = null,
)