package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCardPlaceholder
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.favorites.presentation.FavoritesGridScreen
import com.enmanuelbergling.corntime.feature.favorites.presentation.R
import org.koin.compose.viewmodel.koinViewModel

object FavoriteTvShowsTags {
    const val GRID = "favorite tv shows grid"
    const val RETRY_FOOTER = "favorite tv shows retry footer"
}

@Composable
fun FavoriteTvShowsRoot(
    onTvShowDetails: (tvShowId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<FavoriteTvShowsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(FavoriteTvShowsAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FavoriteTvShowsEvent.NavigateToDetails -> onTvShowDetails(event.tvShowId)
            FavoriteTvShowsEvent.NavigateBack -> onBack()
        }
    }

    FavoriteTvShowsScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun FavoriteTvShowsScreen(
    state: FavoriteTvShowsState,
    onAction: (FavoriteTvShowsAction) -> Unit,
) {
    FavoritesGridScreen(
        title = stringResource(R.string.favorite_tv_shows),
        items = state.tvShows,
        itemId = { it.id },
        paging = state.paging,
        isEmpty = state.isEmpty,
        undoable = state.undoable,
        removalError = state.removalError,
        removedMessage = stringResource(R.string.tv_show_removed_from_favorites),
        emptyMessage = stringResource(R.string.no_favorite_tv_shows_yet),
        gridTag = FavoriteTvShowsTags.GRID,
        retryFooterTag = FavoriteTvShowsTags.RETRY_FOOTER,
        onLoadNextPage = { onAction(FavoriteTvShowsAction.OnLoadNextPage) },
        onBack = { onAction(FavoriteTvShowsAction.OnBack) },
        onOpen = { onAction(FavoriteTvShowsAction.OnTvShowClick(it)) },
        onRemove = { onAction(FavoriteTvShowsAction.OnRemove(it)) },
        onUndoRemove = { onAction(FavoriteTvShowsAction.OnUndoRemove(it)) },
        onRetryRemoval = { onAction(FavoriteTvShowsAction.OnRetryRemoval) },
        onDismissRemovalError = { onAction(FavoriteTvShowsAction.OnDismissRemovalError) },
        placeholder = { TvShowCardPlaceholder() },
    ) { tvShow ->
        TvShowCard(
            imageUrl = tvShow.posterPath.orEmpty(),
            title = tvShow.name,
            rating = tvShow.voteAverage,
            titleLines = 1,
        ) { onAction(FavoriteTvShowsAction.OnTvShowClick(tvShow.id)) }
    }
}
