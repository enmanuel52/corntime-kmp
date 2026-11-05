package com.enmanuelbergling.corntime.feature.actor.presentation.details

import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie
import com.enmanuelbergling.corntime.core.presentation.error.UiError

data class ActorDetailsState(
    val actorId: Int,
    val details: ActorDetails? = null,
    /** Null until loaded; an empty list is a loaded actor without movies. */
    val loadedMovies: List<KnownMovie>? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
) {
    val knownMovies: List<KnownMovie> get() = loadedMovies.orEmpty()
}

sealed interface ActorDetailsAction {
    data object OnBack : ActorDetailsAction
    data object OnRetry : ActorDetailsAction
    data object OnDismissError : ActorDetailsAction
    data class OnMovieClick(val movieId: Int) : ActorDetailsAction
}

sealed interface ActorDetailsEvent {
    data object NavigateBack : ActorDetailsEvent
    data class NavigateToMovie(val movieId: Int) : ActorDetailsEvent
}
