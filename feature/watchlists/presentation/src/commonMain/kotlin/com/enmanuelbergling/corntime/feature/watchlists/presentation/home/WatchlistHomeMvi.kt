package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState

data class WatchlistHomeState(
    val paging: PagingState<Movie> = PagingState(),
    val removals: RemovalState = RemovalState(),
) {
    /** Loaded movies minus the ones the user just removed or moved to favorites (typed list for Swift). */
    val movies: List<Movie> get() = paging.items.filterNot { it.id in removals.hiddenIds }

    val isEmpty: Boolean get() = paging.endReached && movies.isEmpty()

    /** The latest change that can still be taken back: shown as "removed - undo". */
    val undoable: PendingRemoval? get() = removals.undoable

    val removalError: UiError? get() = removals.failure?.error
}

sealed interface WatchlistHomeAction {
    /**
     * The screen became visible. The first time nothing happens (the first page is already loading); when
     * it comes back (for example from a movie's details) the list reloads, since it may have changed there.
     */
    data object OnScreenShown : WatchlistHomeAction
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : WatchlistHomeAction
    data class OnMovieClick(val movieId: Int) : WatchlistHomeAction
    data object OnListsClick : WatchlistHomeAction
    data class OnRemove(val movieId: Int) : WatchlistHomeAction
    data class OnMoveToFavorites(val movieId: Int) : WatchlistHomeAction
    data class OnUndo(val movieId: Int) : WatchlistHomeAction
    data object OnRetryRemoval : WatchlistHomeAction

    /** Gives up on a change that could not be sent: the movie comes back. */
    data object OnDismissRemovalError : WatchlistHomeAction
}

sealed interface WatchlistHomeEvent {
    data class NavigateToDetails(val movieId: Int) : WatchlistHomeEvent
    data object NavigateToLists : WatchlistHomeEvent
}
