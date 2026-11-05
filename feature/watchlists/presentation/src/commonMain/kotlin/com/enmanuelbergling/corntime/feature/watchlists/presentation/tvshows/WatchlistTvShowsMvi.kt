package com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows

import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState

data class WatchlistTvShowsState(
    val paging: PagingState<TvShow> = PagingState(),
    val removals: RemovalState = RemovalState(),
) {
    /** Loaded TV shows minus the ones the user just removed or moved to favorites (typed list for Swift). */
    val tvShows: List<TvShow> get() = paging.items.filterNot { it.id in removals.hiddenIds }

    val isEmpty: Boolean get() = paging.endReached && tvShows.isEmpty()

    /** The latest change that can still be taken back: shown as "removed - undo". */
    val undoable: PendingRemoval? get() = removals.undoable

    val removalError: UiError? get() = removals.failure?.error
}

sealed interface WatchlistTvShowsAction {
    /**
     * The screen became visible. The first time nothing happens (the first page is already loading); when
     * it comes back (for example from a TV show's details) the list reloads, since it may have changed there.
     */
    data object OnScreenShown : WatchlistTvShowsAction
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : WatchlistTvShowsAction
    data class OnTvShowClick(val tvShowId: Int) : WatchlistTvShowsAction
    data class OnRemove(val tvShowId: Int) : WatchlistTvShowsAction
    data class OnMoveToFavorites(val tvShowId: Int) : WatchlistTvShowsAction
    data class OnUndo(val tvShowId: Int) : WatchlistTvShowsAction
    data object OnRetryRemoval : WatchlistTvShowsAction

    /** Gives up on a change that could not be sent: the TV show comes back. */
    data object OnDismissRemovalError : WatchlistTvShowsAction
}

sealed interface WatchlistTvShowsEvent {
    data class NavigateToDetails(val tvShowId: Int) : WatchlistTvShowsEvent
}
