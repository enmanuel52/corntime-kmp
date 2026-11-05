package com.enmanuelbergling.corntime.core.domain.model.tv

import com.enmanuelbergling.corntime.core.domain.model.movie.Cast
import com.enmanuelbergling.corntime.core.domain.model.movie.Crew

data class TvCredits(
    val id: Int,
    val cast: List<Cast>,
    val crew: List<Crew>,
)
