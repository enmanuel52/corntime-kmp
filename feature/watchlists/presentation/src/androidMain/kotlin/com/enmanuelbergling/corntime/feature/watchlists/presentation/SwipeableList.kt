package com.enmanuelbergling.corntime.feature.watchlists.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.OnceLottieAnimation
import com.enmanuelbergling.corntime.core.designsystem.components.SwipeToDismissContainer
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieLandCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieLandCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowLandCard
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

object WatchlistTags {
    const val LIST = "watchlist movies"
    const val RETRY_FOOTER = "watchlist retry footer"
}

/**
 * A list of movies the user can swipe right to move to favorites and left to remove. The watchlist
 * and every user-created list use it; only what the swipes do differs.
 */
@Composable
internal fun SwipeableMovieList(
    movies: List<Movie>,
    paging: PagingState<Movie>,
    isEmpty: Boolean,
    emptyMessage: String,
    onMovieClick: (movieId: Int) -> Unit,
    onMoveToFavorites: (movieId: Int) -> Unit,
    onRemove: (movieId: Int) -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeableList(
        items = movies,
        itemId = { it.id },
        paging = paging,
        isEmpty = isEmpty,
        emptyMessage = emptyMessage,
        onMoveToFavorites = onMoveToFavorites,
        onRemove = onRemove,
        onLoadNextPage = onLoadNextPage,
        placeholder = { MovieLandCardPlaceholder(Modifier.fillMaxWidth()) },
        modifier = modifier,
    ) { movie ->
        MovieLandCard(
            movie = movie,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) { onMovieClick(movie.id) }
    }
}

/** The same swipeable list for TV shows (the watchlist's TV segment). */
@Composable
internal fun SwipeableTvShowList(
    tvShows: List<TvShow>,
    paging: PagingState<TvShow>,
    isEmpty: Boolean,
    emptyMessage: String,
    onTvShowClick: (tvShowId: Int) -> Unit,
    onMoveToFavorites: (tvShowId: Int) -> Unit,
    onRemove: (tvShowId: Int) -> Unit,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeableList(
        items = tvShows,
        itemId = { it.id },
        paging = paging,
        isEmpty = isEmpty,
        emptyMessage = emptyMessage,
        onMoveToFavorites = onMoveToFavorites,
        onRemove = onRemove,
        onLoadNextPage = onLoadNextPage,
        placeholder = { MovieLandCardPlaceholder(Modifier.fillMaxWidth()) },
        modifier = modifier,
    ) { tvShow ->
        TvShowLandCard(
            tvShow = tvShow,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) { onTvShowClick(tvShow.id) }
    }
}

@Composable
private fun <T> SwipeableList(
    items: List<T>,
    itemId: (T) -> Int,
    paging: PagingState<T>,
    isEmpty: Boolean,
    emptyMessage: String,
    onMoveToFavorites: (id: Int) -> Unit,
    onRemove: (id: Int) -> Unit,
    onLoadNextPage: () -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val error = paging.error
    when {
        error != null && paging.items.isEmpty() -> ErrorScreen(
            errorMessage = stringResource(error.toUiError().messageResource),
            onRetry = onLoadNextPage,
            modifier = modifier,
        )

        isEmpty -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyMessage) }

        else -> {
            val listState = rememberLazyListState()
            listState.OnLoadMore(itemCount = paging.items.size, enabled = error == null, onLoadMore = onLoadNextPage)

            LazyColumn(
                modifier = modifier
                    .testTag(WatchlistTags.LIST)
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.dimen.verySmall)
                    .shimmerIf { paging.isLoadingFirstPage },
                state = listState,
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                userScrollEnabled = !paging.isLoadingFirstPage,
            ) {
                items(items, key = { item -> itemId(item) }) { item ->
                    SwipeToDismissContainer(
                        visible = true,
                        onDismissFromStartToEnd = { onMoveToFavorites(itemId(item)) },
                        onDismissFromEndToStart = { onRemove(itemId(item)) },
                        containerColorDismissFromStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .3f),
                        containerColorDismissFromEnd = MaterialTheme.colorScheme.errorContainer.copy(alpha = .3f),
                        backgroundIcon = { direction ->
                            when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> OnceLottieAnimation(
                                    resId = DesignR.raw.add_to_favorite,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .scale(1.5f),
                                )

                                SwipeToDismissBoxValue.EndToStart -> OnceLottieAnimation(
                                    resId = DesignR.raw.delete,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .scale(2.2f),
                                )

                                SwipeToDismissBoxValue.Settled -> Unit
                            }
                        },
                        modifier = Modifier.animateItem(),
                    ) {
                        itemContent(item)
                    }
                }

                if (paging.isLoadingFirstPage) items(12) { placeholder() }
                if (paging.isLoadingMore) items(3) { placeholder() }
                if (error != null && paging.items.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = onLoadNextPage,
                                modifier = Modifier.testTag(WatchlistTags.RETRY_FOOTER),
                                shape = RoundedCornerShape(50),
                            ) { Text(stringResource(DesignR.string.retry)) }
                        }
                    }
                }
            }
        }
    }
}
