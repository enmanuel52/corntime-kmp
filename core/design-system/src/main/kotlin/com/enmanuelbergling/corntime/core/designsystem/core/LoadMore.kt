package com.enmanuelbergling.corntime.core.designsystem.core

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

private const val DEFAULT_PREFETCH = 6

/**
 * Calls [onLoadMore] when the last visible item is within [prefetch] items of the end. Re-evaluates
 * when [itemCount] changes, so a page that is still short of filling the screen triggers the next one.
 * Safe to call repeatedly: the Paginator ignores requests while loading or finished. Pass
 * `enabled = false` while an error is shown so a failed page is only retried by the user.
 */
@Composable
fun LazyGridState.OnLoadMore(
    itemCount: Int,
    enabled: Boolean = true,
    prefetch: Int = DEFAULT_PREFETCH,
    onLoadMore: () -> Unit,
) {
    val nearEnd by remember(this, prefetch) {
        derivedStateOf {
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
            last >= layoutInfo.totalItemsCount - 1 - prefetch
        }
    }
    val latest by rememberUpdatedState(onLoadMore)
    LaunchedEffect(nearEnd, itemCount, enabled) { if (enabled && nearEnd && itemCount > 0) latest() }
}

@Composable
fun LazyListState.OnLoadMore(
    itemCount: Int,
    enabled: Boolean = true,
    prefetch: Int = DEFAULT_PREFETCH,
    onLoadMore: () -> Unit,
) {
    val nearEnd by remember(this, prefetch) {
        derivedStateOf {
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
            last >= layoutInfo.totalItemsCount - 1 - prefetch
        }
    }
    val latest by rememberUpdatedState(onLoadMore)
    LaunchedEffect(nearEnd, itemCount, enabled) { if (enabled && nearEnd && itemCount > 0) latest() }
}
