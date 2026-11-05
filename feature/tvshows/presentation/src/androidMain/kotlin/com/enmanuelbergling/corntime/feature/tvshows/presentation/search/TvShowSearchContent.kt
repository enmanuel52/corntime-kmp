package com.enmanuelbergling.corntime.feature.tvshows.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.FromDirection
import com.enmanuelbergling.corntime.core.designsystem.components.ShowUpFrom
import com.enmanuelbergling.corntime.core.designsystem.components.common.TvShowLandCard
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import kotlinx.coroutines.launch

object TvShowSearchTags {
    const val RESULTS = "tv_show_search_results"
    const val LOADING = "tv_show_search_loading"
    const val RETRY = "tv_show_search_retry"
    const val NO_RESULTS = "tv_show_search_no_results"
}

/** Results of the TV show search, shown inside the expanded search bar. */
@Composable
fun TvShowSearchContent(
    state: TvShowSearchState,
    onAction: (TvShowSearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val paging = state.paging
    val atStart by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
    val scope = rememberCoroutineScope()
    val error = paging.error

    lazyListState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
        onAction(TvShowSearchAction.OnLoadNextPage)
    }

    Box(modifier) {
        if (paging.isLoadingFirstPage && state.hasQuery) {
            WavyLoading(Modifier.testTag(TvShowSearchTags.LOADING))
        } else {
            HorizontalDivider()
        }

        when {
            error != null && paging.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = stringResource(error.toUiError().messageResource))
                OutlinedButton(
                    onClick = { onAction(TvShowSearchAction.OnLoadNextPage) },
                    modifier = Modifier.testTag(TvShowSearchTags.RETRY),
                ) { Text(stringResource(DesignR.string.retry)) }
            }

            state.showNoResults -> Box(
                Modifier
                    .fillMaxSize()
                    .testTag(TvShowSearchTags.NO_RESULTS),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_tv_shows_found)) }

            else -> LazyColumn(
                contentPadding = WindowInsets.navigationBars.asPaddingValues() +
                    PaddingValues(top = MaterialTheme.dimen.small),
                state = lazyListState,
                modifier = Modifier.testTag(TvShowSearchTags.RESULTS),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
            ) {
                if (paging.items.isNotEmpty()) {
                    stickyHeader {
                        Text(
                            stringResource(DesignR.string.tv_shows),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                items(paging.items, key = { it.id }) { tvShow ->
                    TvShowLandCard(tvShow = tvShow, Modifier.fillMaxWidth()) {
                        onAction(TvShowSearchAction.OnTvShowClick(tvShow.id))
                    }
                }

                if (paging.isLoadingMore) item { WavyLoading() }

                if (error != null && paging.items.isNotEmpty()) item {
                    OutlinedButton(
                        onClick = { onAction(TvShowSearchAction.OnLoadNextPage) },
                        modifier = Modifier.testTag(TvShowSearchTags.RETRY),
                    ) { Text(stringResource(DesignR.string.retry)) }
                }
            }
        }

        ShowUpFrom(
            visible = !atStart,
            fromDirection = FromDirection.Bottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SmallFloatingActionButton(
                onClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                modifier = Modifier.padding(bottom = MaterialTheme.dimen.small),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WavyLoading(modifier: Modifier = Modifier) {
    LinearWavyProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        waveSpeed = MaterialTheme.dimen.medium,
    )
}
