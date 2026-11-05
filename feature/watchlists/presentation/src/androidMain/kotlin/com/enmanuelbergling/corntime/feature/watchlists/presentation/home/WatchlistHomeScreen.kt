package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.UndoSnackbarEffect
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.watchlists.presentation.R
import com.enmanuelbergling.corntime.feature.watchlists.presentation.SwipeableMovieList
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WatchlistHomeRoot(
    onMovieDetails: (movieId: Int) -> Unit,
    onNavigateToLists: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<WatchlistHomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(WatchlistHomeAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is WatchlistHomeEvent.NavigateToDetails -> onMovieDetails(event.movieId)
            WatchlistHomeEvent.NavigateToLists -> onNavigateToLists()
        }
    }

    WatchlistHomeScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistHomeScreen(
    state: WatchlistHomeState,
    onAction: (WatchlistHomeAction) -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(DesignR.string.watchlist)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(WatchlistHomeAction.OnListsClick) }) {
                        Icon(
                            painter = painterResource(DesignR.drawable.bookmark_outline),
                            contentDescription = stringResource(R.string.lists_icon),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        SwipeableMovieList(
            movies = state.movies,
            paging = state.paging,
            isEmpty = state.isEmpty,
            emptyMessage = stringResource(R.string.watchlist_is_empty),
            onMovieClick = { onAction(WatchlistHomeAction.OnMovieClick(it)) },
            onMoveToFavorites = { onAction(WatchlistHomeAction.OnMoveToFavorites(it)) },
            onRemove = { onAction(WatchlistHomeAction.OnRemove(it)) },
            onLoadNextPage = { onAction(WatchlistHomeAction.OnLoadNextPage) },
            modifier = Modifier.padding(paddingValues),
        )
    }
}
