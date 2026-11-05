package com.enmanuelbergling.corntime.feature.movies.presentation.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.common.HeaderMovieInfo
import com.enmanuelbergling.corntime.core.designsystem.components.common.HeaderMoviePlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.listItemWindAnimation
import com.enmanuelbergling.corntime.core.designsystem.components.walkthrough.components.InstagramPager
import com.enmanuelbergling.corntime.core.designsystem.components.walkthrough.components.ShiftIndicator
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.isScrollingForward
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchAction
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchContent
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchEvent
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchState
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

object MoviesTags {
    const val FEED = "movies_feed"
    const val DRAWER = "movies_drawer"
    const val FILTER = "movies_filter"
    const val FAVORITES = "movies_favorites"
}

@Composable
fun MoviesRoot(
    onDetails: (movieId: Int) -> Unit,
    onSection: (MovieSection) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<MoviesViewModel>()
    val searchViewModel = koinViewModel<MovieSearchViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is MoviesEvent.NavigateToDetails -> onDetails(event.movieId)
            is MoviesEvent.NavigateToSection -> onSection(event.section)
        }
    }
    ObserveAsEvents(searchViewModel.events) { event ->
        when (event) {
            is MovieSearchEvent.NavigateToDetails -> onDetails(event.movieId)
        }
    }

    MoviesScreen(
        state = state,
        searchState = searchState,
        onAction = viewModel::onAction,
        onSearchAction = searchViewModel::onAction,
        onOpenDrawer = onOpenDrawer,
        onFilter = onFilter,
        onFavorites = onFavorites,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    state: MoviesState,
    searchState: MovieSearchState,
    onAction: (MoviesAction) -> Unit,
    onSearchAction: (MovieSearchAction) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Collapsed)
    val textFieldState = rememberTextFieldState("")

    LaunchedEffect(textFieldState.text) {
        onSearchAction(MovieSearchAction.OnQueryChange(textFieldState.text.toString()))
    }

    // the text field is the source of truth for what is typed, so a picked suggestion has to land there too
    val searchAction: (MovieSearchAction) -> Unit = { action ->
        if (action is MovieSearchAction.OnSuggestionClick) textFieldState.setTextAndPlaceCursorAtEnd(action.query)
        onSearchAction(action)
    }

    val errorMessage = state.error?.messageResource

    if (errorMessage != null && !state.isFeedEmpty) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(MoviesAction.OnDismissError) },
            message = stringResource(errorMessage),
            onRetry = { onAction(MoviesAction.OnRetry) },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            MoviesTopAppBar(
                onOpenDrawer = onOpenDrawer,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onFilter = onFilter,
                onFavorites = onFavorites.takeIf { state.isLoggedIn },
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        if (errorMessage != null && state.isFeedEmpty) {
            ErrorScreen(
                errorMessage = stringResource(errorMessage),
                onRetry = { onAction(MoviesAction.OnRetry) },
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = {
                        ExpandedSearchBarInputField(
                            textFieldState = textFieldState,
                            searchBarState = searchBarState,
                            onSearch = { searchAction(MovieSearchAction.OnSearchSubmit) },
                        )
                    },
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent),
                ) {
                    MovieSearchContent(state = searchState, onAction = searchAction)
                }

                MoviesGrid(
                    state = state,
                    onDetails = { onAction(MoviesAction.OnMovieClick(it)) },
                    onMore = { onAction(MoviesAction.OnSectionClick(it)) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MoviesTopAppBar(
    onOpenDrawer: (() -> Unit)?,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onFilter: (() -> Unit)?,
    onFavorites: (() -> Unit)?,
) {
    TopAppBar(
        navigationIcon = {
            if (onOpenDrawer != null) IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag(MoviesTags.DRAWER),
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.bars_bottom_left),
                    contentDescription = stringResource(DesignR.string.drawer_icon),
                )
            }
        },
        title = {
            SearchBar(
                searchBarState,
                inputField = {
                    SearchBarInputField(
                        textFieldState = textFieldState,
                        searchBarState = searchBarState,
                        trailingIcon = onFilter?.let { filter ->
                            {
                                IconButton(
                                    onClick = filter,
                                    modifier = Modifier.testTag(MoviesTags.FILTER),
                                ) {
                                    Icon(
                                        painter = painterResource(DesignR.drawable.funnel),
                                        contentDescription = stringResource(DesignR.string.filter_icon),
                                    )
                                }
                            }
                        },
                    )
                },
            )
        },
        actions = {
            if (onFavorites != null) {
                IconButton(onClick = onFavorites, modifier = Modifier.testTag(MoviesTags.FAVORITES)) {
                    Icon(
                        painter = painterResource(DesignR.drawable.heart_outline),
                        contentDescription = stringResource(DesignR.string.favorites),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = {},
        placeholder = {
            Text(
                text = stringResource(DesignR.string.movies),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        trailingIcon = trailingIcon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedSearchBarInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = {
            onSearch()
            keyboardController?.hide()
        },
        leadingIcon = {
            IconButton(onClick = {
                textFieldState.clearText()
                scope.launch { searchBarState.animateToCollapsed() }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = stringResource(id = DesignR.string.back_icon),
                )
            }
        },
        trailingIcon = {
            if (textFieldState.text.isNotBlank()) IconButton(onClick = { textFieldState.clearText() }) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = stringResource(DesignR.string.clear_string_icon),
                )
            }
        },
        placeholder = { Text(text = stringResource(DesignR.string.movies)) },
    )
}

@Composable
private fun MoviesGrid(
    state: MoviesState,
    onDetails: (id: Int) -> Unit,
    onMore: (MovieSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = state.sections
    val isLoading = state.isLoading

    LazyColumn(
        modifier = modifier
            .testTag(MoviesTags.FEED)
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimen.verySmall),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
    ) {
        headersMovies(sections[MovieSection.Upcoming].orEmpty().take(5), onDetails, isLoading) {
            onMore(MovieSection.Upcoming)
        }

        forYouText()

        moviesSection(
            title = DesignR.string.top_rated,
            movies = sections[MovieSection.TopRated].orEmpty(),
            onDetails = onDetails,
            isLoading = isLoading,
        ) { onMore(MovieSection.TopRated) }

        moviesSection(
            title = DesignR.string.now_playing,
            movies = sections[MovieSection.NowPlaying].orEmpty(),
            onDetails = onDetails,
            isLoading = isLoading,
        ) { onMore(MovieSection.NowPlaying) }

        moviesSection(
            title = DesignR.string.popular,
            movies = sections[MovieSection.Popular].orEmpty(),
            onDetails = onDetails,
            isLoading = isLoading,
        ) { onMore(MovieSection.Popular) }
    }
}

private fun LazyListScope.moviesSection(
    @StringRes title: Int,
    movies: List<Movie>,
    onDetails: (id: Int) -> Unit,
    isLoading: Boolean,
    onMore: () -> Unit,
) {
    item {
        if (movies.isEmpty() && isLoading) {
            Row(Modifier.shimmer()) {
                repeat(5) {
                    MovieCardPlaceholder(
                        modifier = Modifier
                            .padding(start = MaterialTheme.dimen.small)
                            .width(180.dp)
                    )
                }
            }
        } else if (movies.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = stringResource(title),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
                    onMore = onMore,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

                val listState = rememberLazyListState()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                    state = listState,
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieCard(
                            imageUrl = movie.posterPath,
                            title = movie.title,
                            rating = movie.voteAverage,
                            modifier = Modifier
                                .width(180.dp)
                                .listItemWindAnimation(
                                    listState.isScrollingForward(),
                                    Orientation.Horizontal,
                                ),
                        ) {
                            onDetails(movie.id)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.headersMovies(
    upcoming: List<Movie>,
    onDetails: (id: Int) -> Unit,
    isLoading: Boolean,
    onMore: () -> Unit,
) {
    item {
        if (upcoming.isEmpty() && isLoading) {
            HeaderMoviePlaceholder(Modifier.shimmer())
        } else if (upcoming.isNotEmpty()) {
            val pagerState = rememberPagerState { upcoming.count() }
            Column {
                SectionHeader(
                    title = stringResource(DesignR.string.upcoming),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
                    onMore = onMore,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

                InstagramPager(
                    state = pagerState,
                    pageSpacing = MaterialTheme.dimen.verySmall,
                    boxAngle = 20,
                ) { page, pageModifier ->
                    val movie = upcoming.getOrNull(page)
                    movie?.let {
                        ElevatedCard(onClick = { onDetails(movie.id) }, modifier = pageModifier) {
                            AsyncImage(
                                model = ImageUrls.BACKDROP + movie.backdropPath.orEmpty(),
                                contentDescription = "header image",
                                placeholder = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
                                error = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.78f),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) togetherWith
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                    },
                    label = "movie info animation",
                    modifier = Modifier.padding(MaterialTheme.dimen.small),
                ) { page ->
                    val movie = upcoming.getOrNull(page)

                    HeaderMovieInfo(
                        title = movie?.title.orEmpty(),
                        rating = movie?.voteAverage ?: .0,
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimen.verySmall))

                ShiftIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    stepSize = MaterialTheme.dimen.small,
                    spaceBetween = MaterialTheme.dimen.small,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onMore) {
            Icon(
                painter = painterResource(DesignR.drawable.double_right),
                contentDescription = stringResource(DesignR.string.more_icon),
            )
        }
    }
}

private fun LazyListScope.forYouText() {
    item {
        Text(
            text = stringResource(DesignR.string.for_you),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
        )
    }
}
