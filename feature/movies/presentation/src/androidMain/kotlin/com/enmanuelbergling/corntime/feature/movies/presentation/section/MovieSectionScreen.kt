package com.enmanuelbergling.corntime.feature.movies.presentation.section

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.listItemWindAnimation
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.isScrollingForward
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.movies.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.material3.MaterialTheme
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR

object MovieSectionTags {
    const val GRID = "movie_section_grid"
    const val RETRY_FOOTER = "movie_section_retry_footer"
}

@Composable
fun MovieSectionRoot(
    section: MovieSection,
    onMovie: (movieId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<MovieSectionViewModel> { parametersOf(section) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is MovieSectionEvent.NavigateToDetails -> onMovie(event.movieId)
            MovieSectionEvent.NavigateBack -> onBack()
        }
    }

    MovieSectionScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSectionScreen(
    state: MovieSectionState,
    onAction: (MovieSectionAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val paging = state.paging

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(state.section.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(MovieSectionAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val error = paging.error
        when {
            error != null && paging.items.isEmpty() -> ErrorScreen(
                errorMessage = stringResource(error.toUiError().messageResource),
                onRetry = { onAction(MovieSectionAction.OnLoadNextPage) },
                modifier = Modifier.padding(paddingValues),
            )

            paging.isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_movies_found)) }

            else -> {
                val listState = rememberLazyGridState()
                listState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
                    onAction(MovieSectionAction.OnLoadNextPage)
                }

                Box(
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .testTag(MovieSectionTags.GRID)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .shimmerIf { paging.isLoadingFirstPage },
                        state = listState,
                        columns = GridCells.Adaptive(150.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        userScrollEnabled = !paging.isLoadingFirstPage,
                        contentPadding = PaddingValues(MaterialTheme.dimen.small),
                    ) {
                        items(paging.items, key = { movie -> movie.id }) { movie ->
                            MovieCard(
                                imageUrl = movie.posterPath,
                                title = movie.title,
                                rating = movie.voteAverage,
                                titleLines = 1,
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .listItemWindAnimation(
                                        isScrollingForward = listState.isScrollingForward(),
                                        rotation = 4f,
                                        durationMillis = 200,
                                    ),
                            ) {
                                onAction(MovieSectionAction.OnMovieClick(movie.id))
                            }
                        }

                        if (paging.isLoadingFirstPage) {
                            items(50) { MovieCardPlaceholder() }
                        }
                        if (paging.isLoadingMore) {
                            items(6) { MovieCardPlaceholder() }
                        }
                        if (error != null && paging.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    OutlinedButton(
                                        onClick = { onAction(MovieSectionAction.OnLoadNextPage) },
                                        modifier = Modifier.testTag(MovieSectionTags.RETRY_FOOTER),
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

@get:StringRes
private val MovieSection.titleRes: Int
    get() = when (this) {
        MovieSection.Upcoming -> R.string.upcoming_movies
        MovieSection.TopRated -> R.string.top_rated_movies
        MovieSection.NowPlaying -> R.string.now_playing_movies
        MovieSection.Popular -> R.string.popular_movies
    }
