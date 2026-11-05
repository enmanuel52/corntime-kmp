package com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.UndoSnackbarEffect
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.feature.watchlists.presentation.R
import com.enmanuelbergling.corntime.feature.watchlists.presentation.SwipeableTvShowList

/** The TV segment of the watchlist: the swipeable list plus its undo snackbar and retry dialog. */
@Composable
internal fun WatchlistTvShowsContent(
    state: WatchlistTvShowsState,
    onAction: (WatchlistTvShowsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    UndoSnackbarEffect(
        undoable = state.undoable,
        hostState = snackbarHostState,
        removedMessage = stringResource(R.string.tv_show_removed_from_watchlist),
        movedToFavoritesMessage = stringResource(R.string.tv_show_added_to_favorites),
        undoLabel = stringResource(DesignR.string.undo),
        onUndo = { onAction(WatchlistTvShowsAction.OnUndo(it)) },
    )

    state.removalError?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(WatchlistTvShowsAction.OnDismissRemovalError) },
            message = stringResource(error.messageResource),
            onRetry = { onAction(WatchlistTvShowsAction.OnRetryRemoval) },
        )
    }

    SwipeableTvShowList(
        tvShows = state.tvShows,
        paging = state.paging,
        isEmpty = state.isEmpty,
        emptyMessage = stringResource(R.string.watchlist_tv_shows_is_empty),
        onTvShowClick = { onAction(WatchlistTvShowsAction.OnTvShowClick(it)) },
        onMoveToFavorites = { onAction(WatchlistTvShowsAction.OnMoveToFavorites(it)) },
        onRemove = { onAction(WatchlistTvShowsAction.OnRemove(it)) },
        onLoadNextPage = { onAction(WatchlistTvShowsAction.OnLoadNextPage) },
        modifier = modifier,
    )
}
