package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.UndoSnackbarEffect
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.feature.watchlists.presentation.R
import com.enmanuelbergling.corntime.feature.watchlists.presentation.SwipeableMovieList

/** The Movies segment of the watchlist: the swipeable list plus its undo snackbar and retry dialog. */
@Composable
internal fun WatchlistMoviesContent(
    state: WatchlistHomeState,
    onAction: (WatchlistHomeAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    UndoSnackbarEffect(
        undoable = state.undoable,
        hostState = snackbarHostState,
        removedMessage = stringResource(R.string.movie_removed_from_watchlist),
        movedToFavoritesMessage = stringResource(R.string.movie_added_to_favorites),
        undoLabel = stringResource(DesignR.string.undo),
        onUndo = { onAction(WatchlistHomeAction.OnUndo(it)) },
    )

    state.removalError?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(WatchlistHomeAction.OnDismissRemovalError) },
            message = stringResource(error.messageResource),
            onRetry = { onAction(WatchlistHomeAction.OnRetryRemoval) },
        )
    }

    SwipeableMovieList(
        movies = state.movies,
        paging = state.paging,
        isEmpty = state.isEmpty,
        emptyMessage = stringResource(R.string.watchlist_is_empty),
        onMovieClick = { onAction(WatchlistHomeAction.OnMovieClick(it)) },
        onMoveToFavorites = { onAction(WatchlistHomeAction.OnMoveToFavorites(it)) },
        onRemove = { onAction(WatchlistHomeAction.OnRemove(it)) },
        onLoadNextPage = { onAction(WatchlistHomeAction.OnLoadNextPage) },
        modifier = modifier,
    )
}
