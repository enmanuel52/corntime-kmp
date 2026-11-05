package com.enmanuelbergling.corntime.feature.tvshows.presentation.details

import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

/** A cast or crew member as the details screens show them. */
data class PersonUi(
    val id: Int,
    val name: String,
    val profilePath: String?,
)

data class TvShowDetailsState(
    val tvShowId: Int,
    val details: TvShowDetails? = null,
    val credits: TvCredits? = null,
    /** Null while logged out: watchlist and favorite actions are unavailable. */
    val accountStates: TvAccountStates? = null,
    val isWatchlistLoading: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiError? = null,
) {
    val canManageAccount: Boolean get() = accountStates != null
    val isInWatchlist: Boolean get() = accountStates?.watchlist == true
    val isFavorite: Boolean get() = accountStates?.favorite == true

    val cast: List<PersonUi>
        get() = credits?.cast.orEmpty().map { PersonUi(it.id, it.name, it.profilePath) }

    val crew: List<PersonUi>
        get() = credits?.crew.orEmpty().map { PersonUi(it.id, it.name, it.profilePath) }
}

sealed interface TvShowDetailsAction {
    data object OnBack : TvShowDetailsAction
    data object OnRetry : TvShowDetailsAction
    data object OnDismissError : TvShowDetailsAction
    data object OnWatchlistClick : TvShowDetailsAction
    data object OnFavoriteClick : TvShowDetailsAction
    data object OnSeeAllSeasons : TvShowDetailsAction
    data class OnSeasonClick(val seasonNumber: Int) : TvShowDetailsAction
    data class OnPersonClick(val personId: Int) : TvShowDetailsAction
}

sealed interface TvShowDetailsEvent {
    data object NavigateBack : TvShowDetailsEvent
    data object NavigateToSeasons : TvShowDetailsEvent
    data class NavigateToEpisodes(val seasonNumber: Int) : TvShowDetailsEvent

    /** Name and photo are what the actor screen shows right away, before its own request finishes. */
    data class NavigateToActor(val actorId: Int, val name: String, val profilePath: String?) : TvShowDetailsEvent
}
