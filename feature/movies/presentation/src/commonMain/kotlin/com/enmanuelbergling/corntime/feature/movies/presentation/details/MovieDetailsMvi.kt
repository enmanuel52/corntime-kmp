package com.enmanuelbergling.corntime.feature.movies.presentation.details

import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBucket

/** A cast or crew member as the details screens show them. */
data class PersonUi(
    val id: Int,
    val name: String,
    val profilePath: String?,
)

data class MovieDetailsState(
    val movieId: Int,
    val details: MovieDetails? = null,
    val credits: MovieCredits? = null,
    /** Null while logged out: watchlist and favorite actions are unavailable. */
    val accountStates: MovieAccountStates? = null,
    val isWatchlistLoading: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiError? = null,
    /** Share of voters per star, 5 down to 1; empty without votes. */
    val ratingBreakdown: List<RatingBucket> = emptyList(),
) {
    val canManageAccount: Boolean get() = accountStates != null
    val isInWatchlist: Boolean get() = accountStates?.watchlist == true
    val isFavorite: Boolean get() = accountStates?.favorite == true

    val cast: List<PersonUi>
        get() = credits?.cast.orEmpty().map { PersonUi(it.id, it.name, it.profilePath) }

    val crew: List<PersonUi>
        get() = credits?.crew.orEmpty().map { PersonUi(it.id, it.name, it.profilePath) }
}

sealed interface MovieDetailsAction {
    data object OnBack : MovieDetailsAction
    data object OnRetry : MovieDetailsAction
    data object OnDismissError : MovieDetailsAction
    data object OnWatchlistClick : MovieDetailsAction
    data object OnFavoriteClick : MovieDetailsAction
    data object OnReviewsClick : MovieDetailsAction
    data class OnPersonClick(val personId: Int) : MovieDetailsAction
}

sealed interface MovieDetailsEvent {
    data object NavigateBack : MovieDetailsEvent

    /** [averageRating] is on the 0..5 star scale. */
    data class NavigateToReviews(val movieId: Int, val averageRating: Double, val totalReviews: Int) : MovieDetailsEvent

    /** Name and photo are what the actor screen shows right away, before its own request finishes. */
    data class NavigateToActor(val actorId: Int, val name: String, val profilePath: String?) : MovieDetailsEvent
}
