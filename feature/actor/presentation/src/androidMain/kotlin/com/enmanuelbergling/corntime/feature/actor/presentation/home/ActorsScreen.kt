package com.enmanuelbergling.corntime.feature.actor.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.common.ActorCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.ActorPlaceHolder
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.actor.presentation.R
import org.koin.compose.viewmodel.koinViewModel

object ActorsTags {
    const val GRID = "actors grid"
    const val RETRY_FOOTER = "actors retry footer"
}

/** What the details screen shows right away, before its own request finishes. */
data class ActorPreview(val id: Int, val name: String, val imagePath: String?)

@Composable
fun ActorsRoot(
    onDetails: (ActorPreview) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val viewModel = koinViewModel<ActorsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ActorsEvent.NavigateToDetails -> onDetails(ActorPreview(event.actorId, event.name, event.profilePath))
        }
    }

    ActorsScreen(state = state, onAction = viewModel::onAction, onOpenDrawer = onOpenDrawer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorsScreen(
    state: ActorsState,
    onAction: (ActorsAction) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val paging = state.paging

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(DesignR.string.actors)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            painter = painterResource(DesignR.drawable.bars_bottom_left),
                            contentDescription = stringResource(R.string.actor_menu_icon),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        val error = paging.error
        when {
            error != null && paging.items.isEmpty() -> ErrorScreen(
                errorMessage = stringResource(error.toUiError().messageResource),
                onRetry = { onAction(ActorsAction.OnLoadNextPage) },
                modifier = Modifier.padding(paddingValues),
            )

            paging.isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_actors_found)) }

            else -> {
                val gridState = rememberLazyStaggeredGridState()
                gridState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
                    onAction(ActorsAction.OnLoadNextPage)
                }

                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .testTag(ActorsTags.GRID)
                        .padding(paddingValues)
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimen.verySmall)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .shimmerIf { paging.isLoadingFirstPage },
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(110.dp),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                    verticalItemSpacing = MaterialTheme.dimen.small,
                    userScrollEnabled = !paging.isLoadingFirstPage,
                ) {
                    items(paging.items, key = { actor -> actor.id }) { actor ->
                        ActorCard(
                            imageUrl = actor.profilePath,
                            name = actor.originalName,
                            onCLick = { onAction(ActorsAction.OnActorClick(actor)) },
                        )
                    }

                    if (paging.isLoadingFirstPage) items(50) { ActorPlaceHolder() }
                    if (paging.isLoadingMore) items(6) { ActorPlaceHolder() }
                    if (error != null && paging.items.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { onAction(ActorsAction.OnLoadNextPage) },
                                    modifier = Modifier.testTag(ActorsTags.RETRY_FOOTER),
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
