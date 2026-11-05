package com.enmanuelbergling.corntime.feature.favorites.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCardPlaceholder
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.favorites.presentation.FavoritesGridScreen
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

@Composable
fun FavoriteMoviesScreen(
    state: FavoriteMoviesState,
    onAction: (FavoriteMoviesAction) -> Unit,
) {
    FavoritesGridScreen(
        title = stringResource(R.string.favorites),
        items = state.movies,
        itemId = { it.id },
        paging = state.paging,
        isEmpty = state.isEmpty,
        undoable = state.undoable,
        removalError = state.removalError,
        removedMessage = stringResource(R.string.movie_removed_from_favorites),
        emptyMessage = stringResource(R.string.no_favorites_yet),
        gridTag = FavoriteMoviesTags.GRID,
        retryFooterTag = FavoriteMoviesTags.RETRY_FOOTER,
        onLoadNextPage = { onAction(FavoriteMoviesAction.OnLoadNextPage) },
        onBack = { onAction(FavoriteMoviesAction.OnBack) },
        onOpen = { onAction(FavoriteMoviesAction.OnMovieClick(it)) },
        onRemove = { onAction(FavoriteMoviesAction.OnRemove(it)) },
        onUndoRemove = { onAction(FavoriteMoviesAction.OnUndoRemove(it)) },
        onRetryRemoval = { onAction(FavoriteMoviesAction.OnRetryRemoval) },
        onDismissRemovalError = { onAction(FavoriteMoviesAction.OnDismissRemovalError) },
        placeholder = { MovieCardPlaceholder() },
    ) { movie ->
        MovieCard(
            imageUrl = movie.posterPath,
            title = movie.title,
            rating = movie.voteAverage,
            titleLines = 1,
        ) { onAction(FavoriteMoviesAction.OnMovieClick(movie.id)) }
    }
}
