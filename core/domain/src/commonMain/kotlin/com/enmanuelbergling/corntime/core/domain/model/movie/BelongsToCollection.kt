package com.enmanuelbergling.corntime.core.domain.model.movie


data class BelongsToCollection(
    val id: Int,
    val name: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
)