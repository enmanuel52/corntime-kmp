package com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons

import com.enmanuelbergling.corntime.core.domain.model.tv.Season
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

data class SeasonsState(
    val tvShowId: Int,
    val details: TvShowDetails? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
    /** The season whose overview is open; at most one at a time. */
    val expandedSeasonId: Int? = null,
) {
    val seasons: List<Season> get() = details?.seasons.orEmpty()
}

sealed interface SeasonsAction {
    data object OnBack : SeasonsAction
    data object OnRetry : SeasonsAction
    data object OnDismissError : SeasonsAction
    data class OnSeasonClick(val seasonNumber: Int) : SeasonsAction

    /** Opens the season's overview, or closes it when it already is the open one. */
    data class OnSeasonToggle(val seasonId: Int) : SeasonsAction
}

sealed interface SeasonsEvent {
    data object NavigateBack : SeasonsEvent
    data class NavigateToEpisodes(val seasonNumber: Int) : SeasonsEvent
}
