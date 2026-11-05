package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState

data class FavoriteTvShowsState(
    val paging: PagingState<TvShow> = PagingState(),
    val removals: RemovalState = RemovalState(),
) {
    /** Loaded TV shows minus the ones the user just removed (typed list for Swift). */
    val tvShows: List<TvShow> get() = paging.items.filterNot { it.id in removals.hiddenIds }

    /** Nothing to show: loading finished and everything was either never there or removed. */
    val isEmpty: Boolean get() = paging.endReached && tvShows.isEmpty()

    /** The latest removal that can still be taken back: shown as "removed - undo". */
    val undoable: PendingRemoval? get() = removals.undoable

    val removalError: UiError? get() = removals.failure?.error
}

sealed interface FavoriteTvShowsAction {
    /**
     * The screen became visible. The first time nothing happens (the first page is already loading); when
     * it comes back (for example from a TV show's details) the list reloads, since it may have changed there.
     */
    data object OnScreenShown : FavoriteTvShowsAction
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : FavoriteTvShowsAction
    data class OnTvShowClick(val tvShowId: Int) : FavoriteTvShowsAction
    data object OnBack : FavoriteTvShowsAction
    data class OnRemove(val tvShowId: Int) : FavoriteTvShowsAction
    data class OnUndoRemove(val tvShowId: Int) : FavoriteTvShowsAction
    data object OnRetryRemoval : FavoriteTvShowsAction

    /** Gives up on a removal that could not be sent: the TV show comes back. */
    data object OnDismissRemovalError : FavoriteTvShowsAction
}

sealed interface FavoriteTvShowsEvent {
    data class NavigateToDetails(val tvShowId: Int) : FavoriteTvShowsEvent
    data object NavigateBack : FavoriteTvShowsEvent
}
