package com.enmanuelbergling.corntime.feature.watchlists.presentation.details

import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState

data class WatchListDetailsState(
    val listId: Int,
    val paging: PagingState<Movie> = PagingState(),
    val removals: RemovalState = RemovalState(),
) {
    /** Loaded movies minus the ones the user just removed or moved to favorites (typed list for Swift). */
    val movies: List<Movie> get() = paging.items.filterNot { it.id in removals.hiddenIds }

    val isEmpty: Boolean get() = paging.endReached && movies.isEmpty()
    val undoable: PendingRemoval? get() = removals.undoable
    val removalError: UiError? get() = removals.failure?.error
}

sealed interface WatchListDetailsAction {
    /**
     * The screen became visible. The first time nothing happens (the first page is already loading); when
     * it comes back (for example from a movie's details) the list reloads, since it may have changed there.
     */
    data object OnScreenShown : WatchListDetailsAction
    data object OnLoadNextPage : WatchListDetailsAction
    data class OnMovieClick(val movieId: Int) : WatchListDetailsAction
    data object OnBack : WatchListDetailsAction
    data class OnRemove(val movieId: Int) : WatchListDetailsAction
    data class OnMoveToFavorites(val movieId: Int) : WatchListDetailsAction
    data class OnUndo(val movieId: Int) : WatchListDetailsAction
    data object OnRetryRemoval : WatchListDetailsAction
    data object OnDismissRemovalError : WatchListDetailsAction
    data object OnAddShortcut : WatchListDetailsAction
    data object OnRemoveShortcut : WatchListDetailsAction
}

sealed interface WatchListDetailsEvent {
    data class NavigateToDetails(val movieId: Int) : WatchListDetailsEvent
    data object NavigateBack : WatchListDetailsEvent

    /** Android only: the app pins or unpins a launcher shortcut for this list. */
    data class AddShortcut(val listId: Int) : WatchListDetailsEvent
    data class RemoveShortcut(val listId: Int) : WatchListDetailsEvent
}
