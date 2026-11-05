package com.enmanuelbergling.corntime.core.domain.model.movie


data class MovieCredits(
    val id: Int,
    val cast: List<Cast>,
    val crew: List<Crew>
)