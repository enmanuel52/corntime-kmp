package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

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
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.components.listItemWindAnimation
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.isScrollingForward
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.material3.MaterialTheme
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR

object TvShowSectionTags {
    const val GRID = "tv_show_section_grid"
    const val RETRY_FOOTER = "tv_show_section_retry_footer"
}

@Composable
fun TvShowSectionRoot(
    section: TvShowsSection,
    onTvShow: (tvShowId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<TvShowSectionViewModel> { parametersOf(section) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is TvShowSectionEvent.NavigateToDetails -> onTvShow(event.tvShowId)
            TvShowSectionEvent.NavigateBack -> onBack()
        }
    }

    TvShowSectionScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowSectionScreen(
    state: TvShowSectionState,
    onAction: (TvShowSectionAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val paging = state.paging

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(state.section.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(TvShowSectionAction.OnBack) }) {
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
                onRetry = { onAction(TvShowSectionAction.OnLoadNextPage) },
                modifier = Modifier.padding(paddingValues),
            )

            paging.isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_tv_shows_found)) }

            else -> {
                val listState = rememberLazyGridState()
                listState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
                    onAction(TvShowSectionAction.OnLoadNextPage)
                }

                Box(
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .testTag(TvShowSectionTags.GRID)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .shimmerIf { paging.isLoadingFirstPage },
                        state = listState,
                        columns = GridCells.Adaptive(150.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        userScrollEnabled = !paging.isLoadingFirstPage,
                        contentPadding = PaddingValues(MaterialTheme.dimen.small),
                    ) {
                        items(paging.items, key = { tvShow -> tvShow.id }) { tvShow ->
                            TvShowCard(
                                imageUrl = tvShow.posterPath.orEmpty(),
                                title = tvShow.name,
                                rating = tvShow.voteAverage,
                                titleLines = 1,
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .listItemWindAnimation(
                                        isScrollingForward = listState.isScrollingForward(),
                                        rotation = 4f,
                                        durationMillis = 200,
                                    ),
                            ) {
                                onAction(TvShowSectionAction.OnTvShowClick(tvShow.id))
                            }
                        }

                        if (paging.isLoadingFirstPage) {
                            items(50) { TvShowCardPlaceholder() }
                        }
                        if (paging.isLoadingMore) {
                            items(6) { TvShowCardPlaceholder() }
                        }
                        if (error != null && paging.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    OutlinedButton(
                                        onClick = { onAction(TvShowSectionAction.OnLoadNextPage) },
                                        modifier = Modifier.testTag(TvShowSectionTags.RETRY_FOOTER),
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
private val TvShowsSection.titleRes: Int
    get() = when (this) {
        TvShowsSection.Popular -> R.string.popular_tv_shows
        TvShowsSection.TopRated -> R.string.top_rated_tv_shows
        TvShowsSection.OnTheAir -> R.string.on_the_air_tv_shows
        TvShowsSection.AiringToday -> R.string.airing_today_tv_shows
    }
