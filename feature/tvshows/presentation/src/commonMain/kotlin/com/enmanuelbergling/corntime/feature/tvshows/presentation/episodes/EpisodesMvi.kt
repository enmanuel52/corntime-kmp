package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes

import com.enmanuelbergling.corntime.core.domain.model.tv.Episode
import com.enmanuelbergling.corntime.core.domain.model.tv.SeasonDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

data class EpisodesState(
    val tvShowId: Int,
    val seasonNumber: Int,
    val seasonDetails: SeasonDetails? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
    /** The episode whose overview is open; at most one at a time. */
    val expandedEpisodeId: Int? = null,
) {
    val episodes: List<Episode> get() = seasonDetails?.episodes.orEmpty()
}

sealed interface EpisodesAction {
    data object OnBack : EpisodesAction
    data object OnRetry : EpisodesAction
    data object OnDismissError : EpisodesAction
    data class OnEpisodeClick(val episodeNumber: Int) : EpisodesAction

    /** Opens the episode's overview, or closes it when it already is the open one. */
    data class OnEpisodeToggle(val episodeId: Int) : EpisodesAction
}

sealed interface EpisodesEvent {
    data object NavigateBack : EpisodesEvent
    data class NavigateToEpisodeDetails(val episodeNumber: Int) : EpisodesEvent
}
