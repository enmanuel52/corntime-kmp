package com.enmanuelbergling.corntime.feature.watchlists.presentation.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.koin.core.parameter.parametersOf

/** What the app does with launcher shortcuts for a list (Android only). */
interface ListShortcuts {
    fun isPinned(listId: Int): Boolean
    fun add(listId: Int, listName: String)
    fun remove(listId: Int)

    companion object {
        val None = object : ListShortcuts {
            override fun isPinned(listId: Int) = false
            override fun add(listId: Int, listName: String) = Unit
            override fun remove(listId: Int) = Unit
        }
    }
}

@Composable
fun WatchListDetailsRoot(
    listId: Int,
    listName: String,
    shortcuts: ListShortcuts,
    onMovieDetails: (movieId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<WatchListDetailsViewModel> { parametersOf(listId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isPinned by remember(listId) { mutableStateOf(shortcuts.isPinned(listId)) }

    LaunchedEffect(Unit) { viewModel.onAction(WatchListDetailsAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is WatchListDetailsEvent.NavigateToDetails -> onMovieDetails(event.movieId)
            WatchListDetailsEvent.NavigateBack -> onBack()
            is WatchListDetailsEvent.AddShortcut -> {
                shortcuts.add(event.listId, listName)
                isPinned = shortcuts.isPinned(event.listId)
            }

            is WatchListDetailsEvent.RemoveShortcut -> {
                shortcuts.remove(event.listId)
                isPinned = shortcuts.isPinned(event.listId)
            }
        }
    }

    WatchListDetailsScreen(listName = listName, state = state, isPinned = isPinned, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListDetailsScreen(
    listName: String,
    state: WatchListDetailsState,
    isPinned: Boolean,
    onAction: (WatchListDetailsAction) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UndoSnackbarEffect(
        undoable = state.undoable,
        hostState = snackbarHostState,
        removedMessage = stringResource(R.string.movie_removed_from_list),
        movedToFavoritesMessage = stringResource(R.string.movie_added_to_favorites),
        undoLabel = stringResource(DesignR.string.undo),
        onUndo = { onAction(WatchListDetailsAction.OnUndo(it)) },
    )

    state.removalError?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(WatchListDetailsAction.OnDismissRemovalError) },
            message = stringResource(error.messageResource),
            onRetry = { onAction(WatchListDetailsAction.OnRetryRemoval) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = listName) },
                navigationIcon = {
                    IconButton(onClick = { onAction(WatchListDetailsAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onAction(
                                if (isPinned) WatchListDetailsAction.OnRemoveShortcut
                                else WatchListDetailsAction.OnAddShortcut,
                            )
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPinned) DesignR.drawable.bookmark_solid else DesignR.drawable.bookmark_outline,
                            ),
                            contentDescription = stringResource(R.string.shortcut_icon),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        SwipeableMovieList(
            movies = state.movies,
            paging = state.paging,
            isEmpty = state.isEmpty,
            emptyMessage = stringResource(R.string.list_is_empty),
            onMovieClick = { onAction(WatchListDetailsAction.OnMovieClick(it)) },
            onMoveToFavorites = { onAction(WatchListDetailsAction.OnMoveToFavorites(it)) },
            onRemove = { onAction(WatchListDetailsAction.OnRemove(it)) },
            onLoadNextPage = { onAction(WatchListDetailsAction.OnLoadNextPage) },
            modifier = Modifier.padding(paddingValues),
        )
    }
}
