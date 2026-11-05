package com.enmanuelbergling.corntime.feature.favorites.presentation.home

import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState

data class FavoriteMoviesState(
    val paging: PagingState<Movie> = PagingState(),
    val removals: RemovalState = RemovalState(),
) {
    /** Loaded movies minus the ones the user just removed (typed list for Swift). */
    val movies: List<Movie> get() = paging.items.filterNot { it.id in removals.hiddenIds }

    /** Nothing to show: loading finished and everything was either never there or removed. */
    val isEmpty: Boolean get() = paging.endReached && movies.isEmpty()

    /** The latest removal that can still be taken back: shown as "removed - undo". */
    val undoable: PendingRemoval? get() = removals.undoable

    val removalError: UiError? get() = removals.failure?.error
}

sealed interface FavoriteMoviesAction {
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : FavoriteMoviesAction
    data class OnMovieClick(val movieId: Int) : FavoriteMoviesAction
    data object OnBack : FavoriteMoviesAction
    data class OnRemove(val movieId: Int) : FavoriteMoviesAction
    data class OnUndoRemove(val movieId: Int) : FavoriteMoviesAction
    data object OnRetryRemoval : FavoriteMoviesAction

    /** Gives up on a removal that could not be sent: the movie comes back. */
    data object OnDismissRemovalError : FavoriteMoviesAction
}

sealed interface FavoriteMoviesEvent {
    data class NavigateToDetails(val movieId: Int) : FavoriteMoviesEvent
    data object NavigateBack : FavoriteMoviesEvent
}
