package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRowOverflowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.FromDirection
import com.enmanuelbergling.corntime.core.designsystem.components.LinearLoading
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingScreen
import com.enmanuelbergling.corntime.core.designsystem.components.ShowUpFrom
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieLandCard
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.movies.presentation.R
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

object MoviesFilterTags {
    const val CONTENT = "movies_filter_content"
    const val GENRE = "movies_filter_genre_"
    const val SORT = "movies_filter_sort_"
    const val MOVIES = "movies_filter_movies"
    const val CLEAR = "movies_filter_clear"
    const val RETRY = "movies_filter_retry"
    const val NO_RESULTS = "movies_filter_no_results"
}

@Composable
fun MoviesFilterRoot(
    onBack: () -> Unit,
    onMovie: (movieId: Int) -> Unit,
) {
    val viewModel = koinViewModel<MoviesFilterViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            MoviesFilterEvent.NavigateBack -> onBack()
            is MoviesFilterEvent.NavigateToDetails -> onMovie(event.movieId)
        }
    }

    MoviesFilterScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesFilterScreen(
    state: MoviesFilterState,
    onAction: (MoviesFilterAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val genresError = state.genresError

    if (genresError != null && state.genres.isNotEmpty()) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(MoviesFilterAction.OnDismissError) },
            message = stringResource(genresError.messageResource),
            onRetry = { onAction(MoviesFilterAction.OnRetryGenres) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(DesignR.string.advanced_movie_filter)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(MoviesFilterAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                actions = {
                    if (state.hasActiveFilter) {
                        TextButton(
                            onClick = { onAction(MoviesFilterAction.OnClearFilter) },
                            modifier = Modifier.testTag(MoviesFilterTags.CLEAR),
                        ) { Text(stringResource(DesignR.string.clear)) }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        if (state.genres.isEmpty()) {
            if (state.isLoadingGenres) {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            } else if (genresError != null) {
                ErrorScreen(
                    errorMessage = stringResource(genresError.messageResource),
                    onRetry = { onAction(MoviesFilterAction.OnRetryGenres) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        } else {
            FilterContent(state, onAction, Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection))
        }
    }
}

@Composable
private fun FilterContent(
    state: MoviesFilterState,
    onAction: (MoviesFilterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paging = state.paging
    val lazyListState = rememberLazyListState()
    val atStart by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
    val scope = rememberCoroutineScope()

    // the filter controls are two items ahead of the movies
    lazyListState.OnLoadMore(itemCount = paging.items.size + 2, enabled = paging.error == null && !paging.endReached) {
        onAction(MoviesFilterAction.OnLoadNextPage)
    }

    Column(modifier.fillMaxSize()) {
        if (paging.isLoadingFirstPage) LinearLoading()

        Box {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                state = lazyListState,
                modifier = Modifier
                    .testTag(MoviesFilterTags.CONTENT)
                    .padding(horizontal = MaterialTheme.dimen.verySmall),
            ) {
                item { SortUi(state.filter.sortBy, onAction) }

                item { GenresUi(state, onAction) }

                items(paging.items, key = { it.id }) { movie ->
                    MovieLandCard(
                        movie = movie,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(MoviesFilterTags.MOVIES),
                    ) { onAction(MoviesFilterAction.OnMovieClick(movie.id)) }
                }

                if (paging.isLoadingMore) item { LinearLoading() }

                if (paging.isEmpty) item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.dimen.large)
                            .testTag(MoviesFilterTags.NO_RESULTS),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.no_movies_found)) }
                }

                paging.error?.let { error ->
                    item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(error.toUiError().messageResource))
                            OutlinedButton(
                                onClick = { onAction(MoviesFilterAction.OnLoadNextPage) },
                                modifier = Modifier.testTag(MoviesFilterTags.RETRY),
                            ) { Text(stringResource(DesignR.string.retry)) }
                        }
                    }
                }
            }

            ShowUpFrom(
                visible = !atStart,
                fromDirection = FromDirection.Bottom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerticalAlignTop,
                        contentDescription = stringResource(DesignR.string.to_start_icon),
                    )
                }
            }
        }
    }
}

@Composable
private fun SortUi(selected: SortCriteria, onAction: (MoviesFilterAction) -> Unit) {
    Column {
        Text(
            text = stringResource(DesignR.string.order_by),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small)) {
            items(SortCriteria.entries) { criteria ->
                ElevatedFilterChip(
                    selected = criteria == selected,
                    onClick = { onAction(MoviesFilterAction.OnPickSortCriteria(criteria)) },
                    label = { Text(text = stringResource(criteria.labelResource)) },
                    shape = CircleShape,
                    modifier = Modifier.testTag("${MoviesFilterTags.SORT}${criteria.name}"),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresUi(state: MoviesFilterState, onAction: (MoviesFilterAction) -> Unit) {
    val genres = state.genres

    Column {
        Text(
            text = stringResource(DesignR.string.genres),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

        var maxLines by remember { mutableIntStateOf(2) }

        ContextualFlowRow(
            itemCount = genres.size,
            maxLines = maxLines,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
            overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                expandIndicator = {
                    ElevatedAssistChip(
                        onClick = { maxLines += 2 },
                        label = { Text(text = stringResource(R.string.more_genres, itemsLeft)) },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        shape = CircleShape,
                    )
                },
                collapseIndicator = {
                    ElevatedAssistChip(
                        onClick = { maxLines = 2 },
                        label = { Text(text = stringResource(DesignR.string.hide)) },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = CircleShape,
                    )
                },
            ),
            modifier = Modifier.animateContentSize(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)),
        ) { index ->
            val genre: Genre = genres[index]
            ElevatedFilterChip(
                selected = state.isSelected(genre),
                onClick = { onAction(MoviesFilterAction.OnToggleGenre(genre)) },
                label = { Text(text = genre.name) },
                shape = CircleShape,
                modifier = Modifier.testTag("${MoviesFilterTags.GENRE}${genre.id}"),
            )
        }
    }
}

private val SortCriteria.labelResource: Int
    get() = when (this) {
        SortCriteria.Popularity -> DesignR.string.popularity
        SortCriteria.VoteAverage -> DesignR.string.vote_average
        SortCriteria.VoteCount -> DesignR.string.vote_count
        SortCriteria.Revenue -> DesignR.string.revenue
    }

@OptIn(ExperimentalLayoutApi::class)
private val FlowRowOverflowScope.itemsLeft: Int
    get() = totalItemCount - shownItemCount
