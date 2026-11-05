package com.enmanuelbergling.corntime.feature.favorites.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.TinderSwipeToDismissContainer
import com.enmanuelbergling.corntime.core.designsystem.components.UndoSnackbarEffect
import com.enmanuelbergling.corntime.core.designsystem.components.blendMode
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError

import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
/**
 * The favorites grid shared by movies and TV shows: swipe left to remove (with an undo snackbar and a retry
 * dialog when the removal fails), swipe right to open, paging with a retry footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> FavoritesGridScreen(
    title: String,
    items: List<T>,
    itemId: (T) -> Int,
    paging: PagingState<T>,
    isEmpty: Boolean,
    undoable: PendingRemoval?,
    removalError: UiError?,
    removedMessage: String,
    emptyMessage: String,
    gridTag: String,
    retryFooterTag: String,
    onLoadNextPage: () -> Unit,
    onBack: () -> Unit,
    onOpen: (id: Int) -> Unit,
    onRemove: (id: Int) -> Unit,
    onUndoRemove: (id: Int) -> Unit,
    onRetryRemoval: () -> Unit,
    onDismissRemovalError: () -> Unit,
    placeholder: @Composable () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    UndoSnackbarEffect(
        undoable = undoable,
        hostState = snackbarHostState,
        removedMessage = removedMessage,
        movedToFavoritesMessage = removedMessage,
        undoLabel = stringResource(DesignR.string.undo),
        onUndo = onUndoRemove,
    )

    removalError?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = onDismissRemovalError,
            message = stringResource(error.messageResource),
            onRetry = onRetryRemoval,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val error = paging.error
        when {
            error != null && paging.items.isEmpty() -> ErrorScreen(
                errorMessage = stringResource(error.toUiError().messageResource),
                onRetry = onLoadNextPage,
                modifier = Modifier.padding(paddingValues),
            )

            isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(emptyMessage) }

            else -> {
                val gridState = rememberLazyGridState()
                gridState.OnLoadMore(itemCount = paging.items.size, enabled = error == null, onLoadMore = onLoadNextPage)

                Box(
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .testTag(gridTag)
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .shimmerIf { paging.isLoadingFirstPage },
                        state = gridState,
                        columns = GridCells.Adaptive(150.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                        userScrollEnabled = !paging.isLoadingFirstPage,
                        contentPadding = PaddingValues(MaterialTheme.dimen.small),
                    ) {
                        items(items, key = { item -> itemId(item) }) { item ->
                            TinderSwipeToDismissContainer(
                                visible = true,
                                onDismissFromStartToEnd = { onOpen(itemId(item)) },
                                onDismissFromEndToStart = { onRemove(itemId(item)) },
                                endToStartIcon = { TrashIcon() },
                                startToEndIcon = { SeeIcon() },
                                modifier = Modifier.animateItem(),
                            ) {
                                itemContent(item)
                            }
                        }

                        if (paging.isLoadingFirstPage) items(50) { placeholder() }
                        if (paging.isLoadingMore) items(6) { placeholder() }
                        if (error != null && paging.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    OutlinedButton(
                                        onClick = onLoadNextPage,
                                        modifier = Modifier.testTag(retryFooterTag),
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

@Composable
private fun TrashIcon() {
    Icon(
        painterResource(DesignR.drawable.trash_filled),
        contentDescription = "remove",
        modifier = Modifier
            .padding(8.dp)
            .size(48.dp),
        tint = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun SeeIcon() {
    Icon(
        painterResource(DesignR.drawable.play_filled),
        contentDescription = "see",
        modifier = Modifier
            .padding(8.dp)
            .size(48.dp)
            .blendMode(BlendMode.Difference),
        tint = Color.White,
    )
}
