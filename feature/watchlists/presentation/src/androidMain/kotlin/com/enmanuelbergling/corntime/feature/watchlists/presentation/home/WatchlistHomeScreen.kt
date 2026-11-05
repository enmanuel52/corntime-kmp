package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.watchlists.presentation.R
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsAction
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsContent
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsEvent
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsState
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsViewModel
import org.koin.compose.viewmodel.koinViewModel

/** The segments of the watchlist: movies and TV shows, switched from the navigation bar. */
enum class WatchlistTab(
    @StringRes val labelRes: Int,
    @DrawableRes val selectedIconRes: Int,
    @DrawableRes val unselectedIconRes: Int,
) {
    Movies(R.string.watchlist_movies, DesignR.drawable.film_solid, DesignR.drawable.film_outline),
    TvShows(R.string.watchlist_tv_shows, DesignR.drawable.tv_solid, DesignR.drawable.tv_outline),
}

object WatchlistHomeTags {
    fun tab(tab: WatchlistTab) = "watchlist_tab_${tab.name}"
}

@Composable
fun WatchlistHomeRoot(
    onMovieDetails: (movieId: Int) -> Unit,
    onTvShowDetails: (tvShowId: Int) -> Unit,
    onNavigateToLists: () -> Unit,
    onBack: () -> Unit,
    initialTab: WatchlistTab = WatchlistTab.Movies,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    val moviesViewModel = koinViewModel<WatchlistHomeViewModel>()
    val tvShowsViewModel = koinViewModel<WatchlistTvShowsViewModel>()
    val moviesState by moviesViewModel.state.collectAsStateWithLifecycle()
    val tvShowsState by tvShowsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        moviesViewModel.onAction(WatchlistHomeAction.OnScreenShown)
        tvShowsViewModel.onAction(WatchlistTvShowsAction.OnScreenShown)
    }

    ObserveAsEvents(moviesViewModel.events) { event ->
        when (event) {
            is WatchlistHomeEvent.NavigateToDetails -> onMovieDetails(event.movieId)
            WatchlistHomeEvent.NavigateToLists -> onNavigateToLists()
        }
    }
    ObserveAsEvents(tvShowsViewModel.events) { event ->
        when (event) {
            is WatchlistTvShowsEvent.NavigateToDetails -> onTvShowDetails(event.tvShowId)
        }
    }

    WatchlistHomeScreen(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        moviesState = moviesState,
        onMoviesAction = moviesViewModel::onAction,
        tvShowsState = tvShowsState,
        onTvShowsAction = tvShowsViewModel::onAction,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistHomeScreen(
    selectedTab: WatchlistTab,
    onTabSelected: (WatchlistTab) -> Unit,
    moviesState: WatchlistHomeState,
    onMoviesAction: (WatchlistHomeAction) -> Unit,
    tvShowsState: WatchlistTvShowsState,
    onTvShowsAction: (WatchlistTvShowsAction) -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            WatchlistTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                item(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.testTag(WatchlistHomeTags.tab(tab)),
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) tab.selectedIconRes else tab.unselectedIconRes),
                            contentDescription = stringResource(tab.labelRes),
                        )
                    },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        },
    ) {
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
                        IconButton(onClick = { onMoviesAction(WatchlistHomeAction.OnListsClick) }) {
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
            when (selectedTab) {
                WatchlistTab.Movies -> WatchlistMoviesContent(
                    state = moviesState,
                    onAction = onMoviesAction,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(paddingValues),
                )

                WatchlistTab.TvShows -> WatchlistTvShowsContent(
                    state = tvShowsState,
                    onAction = onTvShowsAction,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
