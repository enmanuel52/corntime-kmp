package com.enmanuelbergling.corntime.core.presentation.paging

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * Replaces Paging 3 with a tiny state machine that behaves the same on Android and iOS.
 *
 * - [loadNext] is a no-op while a request is in flight or after the end was reached, so triggering it
 *   from every "last item appeared" callback is safe.
 * - A failure keeps the loaded items and moves to [PagingStatus.Failed]; calling [loadNext] again
 *   retries the same page.
 * - [reset] discards the list; a response that was in flight when it happened is dropped.
 *
 * Calls are expected from a single dispatcher (a ViewModel's `viewModelScope`).
 */
class Paginator<T>(
    private val loadPage: suspend (page: Int) -> NetworkResult<PageModel<T>>,
) {
    private val _state = MutableStateFlow(PagingState<T>())
    val state: StateFlow<PagingState<T>> = _state.asStateFlow()

    private var generation = 0

    suspend fun loadNext() {
        val before = _state.getAndUpdate { current ->
            if (!current.canLoadNext) current
            else current.copy(
                status = if (current.items.isEmpty()) PagingStatus.LoadingFirstPage else PagingStatus.LoadingMore,
            )
        }
        if (!before.canLoadNext) return

        val requestGeneration = generation
        val page = before.nextPage
        val result = loadPage(page)
        if (requestGeneration != generation) return // reset while loading: stale response

        _state.update { current ->
            when (result) {
                is Result.Success -> {
                    val loaded = result.data
                    val exhausted = loaded.results.isEmpty() || page >= loaded.totalPages
                    current.copy(
                        items = current.items + loaded.results,
                        status = if (exhausted) PagingStatus.EndReached else PagingStatus.Idle,
                        nextPage = page + 1,
                    )
                }

                is Result.Failure -> current.copy(status = PagingStatus.Failed(result.error))
            }
        }
    }

    /** Starts over from the first page, dropping everything loaded so far. */
    fun reset() {
        generation++
        _state.value = PagingState()
    }

    /** [reset] followed by loading the first page. */
    suspend fun refresh() {
        reset()
        loadNext()
    }
}
