package com.enmanuelbergling.corntime.feature.tvshows.presentation.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
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
import com.enmanuelbergling.corntime.core.designsystem.components.blendMode
import com.enmanuelbergling.corntime.core.designsystem.components.common.HeaderTvShowPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.common.HeaderTvShowTitle
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowLandCard
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchAction
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchContent
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchEvent
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchState
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

object TvShowsTags {
    const val FEED = "tv_shows_feed"
    const val DRAWER = "tv_shows_drawer"
    const val FAVORITES = "tv_shows_favorites"
}

/** How many of the popular shows the header carousel shows. */
private const val HEADER_SIZE = 5

@Composable
fun TvShowsRoot(
    onDetails: (tvShowId: Int) -> Unit,
    onSection: (TvShowsSection) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<TvShowsViewModel>()
    val searchViewModel = koinViewModel<TvShowSearchViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is TvShowsEvent.NavigateToDetails -> onDetails(event.tvShowId)
            is TvShowsEvent.NavigateToSection -> onSection(event.section)
        }
    }
    ObserveAsEvents(searchViewModel.events) { event ->
        when (event) {
            is TvShowSearchEvent.NavigateToDetails -> onDetails(event.tvShowId)
        }
    }

    TvShowsScreen(
        state = state,
        searchState = searchState,
        onAction = viewModel::onAction,
        onSearchAction = searchViewModel::onAction,
        onOpenDrawer = onOpenDrawer,
        onFavorites = onFavorites,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowsScreen(
    state: TvShowsState,
    searchState: TvShowSearchState,
    onAction: (TvShowsAction) -> Unit,
    onSearchAction: (TvShowSearchAction) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Collapsed)
    val textFieldState = rememberTextFieldState("")

    LaunchedEffect(textFieldState.text) {
        onSearchAction(TvShowSearchAction.OnQueryChange(textFieldState.text.toString()))
    }

    val errorMessage = state.error?.messageResource

    if (errorMessage != null && !state.isFeedEmpty) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(TvShowsAction.OnDismissError) },
            message = stringResource(errorMessage),
            onRetry = { onAction(TvShowsAction.OnRetry) },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TvShowsTopAppBar(
                onOpenDrawer = onOpenDrawer,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                // favorites belong to the account
                onFavorites = onFavorites.takeIf { state.isLoggedIn },
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        if (errorMessage != null && state.isFeedEmpty) {
            ErrorScreen(
                errorMessage = stringResource(errorMessage),
                onRetry = { onAction(TvShowsAction.OnRetry) },
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
                        )
                    },
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent),
                ) {
                    TvShowSearchContent(state = searchState, onAction = onSearchAction)
                }

                TvShowsFeed(
                    state = state,
                    onDetails = { onAction(TvShowsAction.OnTvShowClick(it)) },
                    onMore = { onAction(TvShowsAction.OnSectionClick(it)) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TvShowsTopAppBar(
    onOpenDrawer: (() -> Unit)?,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onFavorites: (() -> Unit)?,
) {
    TopAppBar(
        navigationIcon = {
            if (onOpenDrawer != null) IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag(TvShowsTags.DRAWER),
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
                    SearchBarDefaults.InputField(
                        textFieldState = textFieldState,
                        searchBarState = searchBarState,
                        onSearch = {},
                        placeholder = {
                            Text(
                                text = stringResource(DesignR.string.tv_shows),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                },
            )
        },
        actions = {
            if (onFavorites != null) {
                IconButton(onClick = onFavorites, modifier = Modifier.testTag(TvShowsTags.FAVORITES)) {
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
private fun ExpandedSearchBarInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = { keyboardController?.hide() },
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
        placeholder = { Text(text = stringResource(DesignR.string.tv_shows)) },
    )
}

@Composable
private fun TvShowsFeed(
    state: TvShowsState,
    onDetails: (id: Int) -> Unit,
    onMore: (TvShowsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = state.sections
    val isLoading = state.isLoading

    val popular = sections[TvShowsSection.Popular].orEmpty().take(HEADER_SIZE)
    val topRated = sections[TvShowsSection.TopRated].orEmpty().sortedByDescending { it.voteAverage }
    val onTheAir = sections[TvShowsSection.OnTheAir].orEmpty()
    val airingToday = sections[TvShowsSection.AiringToday].orEmpty().take(HEADER_SIZE)

    LazyColumn(
        modifier = modifier
            .testTag(TvShowsTags.FEED)
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimen.verySmall),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
    ) {
        headerTvShows(popular, onDetails, isLoading) { onMore(TvShowsSection.Popular) }

        if (isLoading || (topRated + onTheAir + airingToday).isNotEmpty()) forYouText()

        tvShowRowSection(
            title = DesignR.string.top_rated,
            tvShows = topRated,
            onDetails = onDetails,
            isLoading = isLoading,
        ) { onMore(TvShowsSection.TopRated) }

        tvShowRowSection(
            title = DesignR.string.on_the_air,
            tvShows = onTheAir,
            onDetails = onDetails,
            isLoading = isLoading,
        ) { onMore(TvShowsSection.OnTheAir) }

        tvShowColumnSection(
            title = DesignR.string.airing_today,
            tvShows = airingToday,
            onDetails = onDetails,
        ) { onMore(TvShowsSection.AiringToday) }
    }
}

private fun LazyListScope.tvShowRowSection(
    @StringRes title: Int,
    tvShows: List<TvShow>,
    onDetails: (id: Int) -> Unit,
    isLoading: Boolean,
    onMore: () -> Unit,
) {
    item {
        if (tvShows.isEmpty() && isLoading) {
            Row(Modifier.shimmer()) {
                repeat(5) {
                    TvShowCardPlaceholder(
                        modifier = Modifier
                            .padding(start = MaterialTheme.dimen.small)
                            .width(120.dp),
                    )
                }
            }
        } else if (tvShows.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = stringResource(title),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
                    onMore = onMore,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.dimen.small),
                ) {
                    items(tvShows, key = { it.id }) { tvShow ->
                        TvShowCard(
                            imageUrl = tvShow.posterPath.orEmpty(),
                            title = tvShow.name,
                            rating = tvShow.voteAverage,
                            modifier = Modifier.width(120.dp),
                        ) {
                            onDetails(tvShow.id)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.tvShowColumnSection(
    @StringRes title: Int,
    tvShows: List<TvShow>,
    onDetails: (id: Int) -> Unit,
    onMore: () -> Unit,
) {
    if (tvShows.isEmpty()) return

    item {
        SectionHeader(
            title = stringResource(title),
            modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
            onMore = onMore,
        )
    }
    items(tvShows, key = { "airing_${it.id}" }) { tvShow ->
        TvShowLandCard(
            tvShow = tvShow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimen.small),
        ) {
            onDetails(tvShow.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.headerTvShows(
    popular: List<TvShow>,
    onDetails: (id: Int) -> Unit,
    isLoading: Boolean,
    onMore: () -> Unit,
) {
    item {
        if (popular.isEmpty() && isLoading) {
            HeaderTvShowPlaceholder(Modifier.shimmer())
        } else if (popular.isNotEmpty()) {
            val carouselState = rememberCarouselState { popular.count() }
            Column {
                SectionHeader(
                    title = stringResource(DesignR.string.popular),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
                    onMore = onMore,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

                HorizontalCenteredHeroCarousel(
                    state = carouselState,
                    itemSpacing = MaterialTheme.dimen.small,
                    maxItemWidth = 300.dp,
                    contentPadding = PaddingValues(horizontal = MaterialTheme.dimen.small),
                ) { page ->
                    popular.getOrNull(page)?.let { tvShow ->
                        TvShowsHeaderCard(
                            tvShow = tvShow,
                            modifier = Modifier.maskClip(CardDefaults.shape),
                        ) {
                            onDetails(tvShow.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvShowsHeaderCard(
    tvShow: TvShow,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Box(contentAlignment = Alignment.BottomStart) {
            AsyncImage(
                model = ImageUrls.BACKDROP + tvShow.backdropPath.orEmpty(),
                contentDescription = "header image",
                placeholder = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
                error = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f),
                contentScale = ContentScale.Crop,
            )

            HeaderTvShowTitle(
                title = tvShow.name,
                modifier = Modifier
                    .padding(MaterialTheme.dimen.small)
                    .blendMode(BlendMode.Difference),
            )
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
