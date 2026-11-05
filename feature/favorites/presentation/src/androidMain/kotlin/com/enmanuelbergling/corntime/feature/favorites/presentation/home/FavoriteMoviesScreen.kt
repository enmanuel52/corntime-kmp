package com.enmanuelbergling.corntime.feature.favorites.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.TinderSwipeToDismissContainer
import com.enmanuelbergling.corntime.core.designsystem.components.UndoSnackbarEffect
import com.enmanuelbergling.corntime.core.designsystem.components.blendMode
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.favorites.presentation.R
import org.koin.compose.viewmodel.koinViewModel

object FavoriteMoviesTags {
    const val GRID = "favorites grid"
    const val RETRY_FOOTER = "favorites retry footer"
}

@Composable
fun FavoriteMoviesRoot(
    onMovieDetails: (movieId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<FavoriteMoviesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(FavoriteMoviesAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FavoriteMoviesEvent.NavigateToDetails -> onMovieDetails(event.movieId)
            FavoriteMoviesEvent.NavigateBack -> onBack()
        }
    }

    FavoriteMoviesScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteMoviesScreen(
    state: FavoriteMoviesState,
    onAction: (FavoriteMoviesAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val paging = state.paging

    UndoSnackbarEffect(
        undoable = state.undoable,
        hostState = snackbarHostState,
        removedMessage = stringResource(R.string.movie_removed_from_favorites),
        movedToFavoritesMessage = stringResource(R.string.movie_removed_from_favorites),
        undoLabel = stringResource(DesignR.string.undo),
        onUndo = { onAction(FavoriteMoviesAction.OnUndoRemove(it)) },
    )

    state.removalError?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(FavoriteMoviesAction.OnDismissRemovalError) },
            message = stringResource(error.messageResource),
            onRetry = { onAction(FavoriteMoviesAction.OnRetryRemoval) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.favorites)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(FavoriteMoviesAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val error = paging.error
        when {
            error != null && paging.items.isEmpty() -> ErrorScreen(
                errorMessage = stringResource(error.toUiError().messageResource),
                onRetry = { onAction(FavoriteMoviesAction.OnLoadNextPage) },
                modifier = Modifier.padding(paddingValues),
            )

            state.isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_favorites_yet)) }

            else -> {
                val gridState = rememberLazyGridState()
                gridState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
                    onAction(FavoriteMoviesAction.OnLoadNextPage)
                }

                Box(
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .testTag(FavoriteMoviesTags.GRID)
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .shimmerIf { paging.isLoadingFirstPage },
                        state = gridState,
                        columns = GridCells.Adaptive(150.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        userScrollEnabled = !paging.isLoadingFirstPage,
                        contentPadding = PaddingValues(MaterialTheme.dimen.small),
                    ) {
                        items(state.movies, key = { movie -> movie.id }) { movie ->
                            TinderSwipeToDismissContainer(
                                visible = true,
                                onDismissFromStartToEnd = { onAction(FavoriteMoviesAction.OnMovieClick(movie.id)) },
                                onDismissFromEndToStart = { onAction(FavoriteMoviesAction.OnRemove(movie.id)) },
                                endToStartIcon = { TrashIcon() },
                                startToEndIcon = { SeeIcon() },
                                modifier = Modifier.animateItem(),
                            ) {
                                MovieCard(
                                    imageUrl = movie.posterPath,
                                    title = movie.title,
                                    rating = movie.voteAverage,
                                    titleLines = 1,
                                ) { onAction(FavoriteMoviesAction.OnMovieClick(movie.id)) }
                            }
                        }

                        if (paging.isLoadingFirstPage) items(50) { MovieCardPlaceholder() }
                        if (paging.isLoadingMore) items(6) { MovieCardPlaceholder() }
                        if (error != null && paging.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    OutlinedButton(
                                        onClick = { onAction(FavoriteMoviesAction.OnLoadNextPage) },
                                        modifier = Modifier.testTag(FavoriteMoviesTags.RETRY_FOOTER),
                                        shape = RoundedCornerShape(50),
                                    ) { Text(stringResource(DesignR.string.retry)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashIcon() {
    Icon(
        painterResource(DesignR.drawable.trash_filled),
        contentDescription = "remove",
        modifier = Modifier
            .padding(8.dp)
            .size(48.dp),
        tint = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun SeeIcon() {
    Icon(
        painterResource(DesignR.drawable.play_filled),
        contentDescription = "see",
        modifier = Modifier
            .padding(8.dp)
            .size(48.dp)
            .blendMode(BlendMode.Difference),
        tint = Color.White,
    )
}
