package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails

import com.enmanuelbergling.corntime.core.domain.model.tv.EpisodeDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

/** A guest star or crew member as the episode screen shows them. */
data class EpisodePersonUi(
    val id: Int,
    val name: String,
    val profilePath: String?,
)

data class EpisodeDetailsState(
    val tvShowId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val details: EpisodeDetails? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
) {
    val guestStars: List<EpisodePersonUi>
        get() = details?.guestStars.orEmpty().map { EpisodePersonUi(it.id, it.name, it.profilePath) }

    val crew: List<EpisodePersonUi>
        get() = details?.crew.orEmpty().map { EpisodePersonUi(it.id, it.name, it.profilePath) }
}

sealed interface EpisodeDetailsAction {
    data object OnBack : EpisodeDetailsAction
    data object OnRetry : EpisodeDetailsAction
    data object OnDismissError : EpisodeDetailsAction
    data class OnPersonClick(val personId: Int) : EpisodeDetailsAction
}

sealed interface EpisodeDetailsEvent {
    data object NavigateBack : EpisodeDetailsEvent
    data class NavigateToActor(val actorId: Int, val name: String, val profilePath: String?) : EpisodeDetailsEvent
}
