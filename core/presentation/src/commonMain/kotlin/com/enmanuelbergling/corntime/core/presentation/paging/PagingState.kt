package com.enmanuelbergling.corntime.core.presentation.paging

import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.error.toUiError

sealed interface PagingStatus {
    /** Ready to load the next page (or nothing requested yet). */
    data object Idle : PagingStatus

    /** First page of a (re)started list is loading; there is nothing to show yet. */
    data object LoadingFirstPage : PagingStatus

    /** A later page is loading while the already loaded items stay visible. */
    data object LoadingMore : PagingStatus

    /** The last page was loaded; no further requests are made. */
    data object EndReached : PagingStatus

    /** The last request failed. Items loaded so far are kept and the same page can be retried. */
    data class Failed(val error: DataError.Network) : PagingStatus
}

/**
 * Immutable snapshot of a paged list. It lives inside a screen's MVI State, so Compose and SwiftUI
 * render the same thing.
 */
data class PagingState<out T>(
    val items: List<T> = emptyList(),
    val status: PagingStatus = PagingStatus.Idle,
    val nextPage: Int = 1,
) {
    val isLoadingFirstPage: Boolean get() = status is PagingStatus.LoadingFirstPage
    val isLoadingMore: Boolean get() = status is PagingStatus.LoadingMore
    val isLoading: Boolean get() = isLoadingFirstPage || isLoadingMore
    val endReached: Boolean get() = status is PagingStatus.EndReached
    val error: DataError.Network? get() = (status as? PagingStatus.Failed)?.error

    /** The failure as a localizable category, ready for either platform to show. */
    val uiError: UiError? get() = error?.toUiError()

    /** Loaded, finished and nothing came back. */
    val isEmpty: Boolean get() = items.isEmpty() && endReached

    internal val canLoadNext: Boolean get() = status is PagingStatus.Idle || status is PagingStatus.Failed
}
