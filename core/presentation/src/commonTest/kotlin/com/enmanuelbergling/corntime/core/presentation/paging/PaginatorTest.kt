package com.enmanuelbergling.corntime.core.presentation.paging

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.util.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PaginatorTest {

    /** Serves `pageSize` items per page numbered globally (page 1 -> 1,2; page 2 -> 3,4), recording requests. */
    private class Source(
        private val totalPages: Int,
        private val pageSize: Int = 2,
    ) {
        val requestedPages = mutableListOf<Int>()
        var failure: DataError.Network? = null

        suspend fun load(page: Int): NetworkResult<PageModel<Int>> {
            requestedPages += page
            failure?.let { return Result.Failure(it) }
            val items = if (page > totalPages) emptyList() else List(pageSize) { (page - 1) * pageSize + it + 1 }
            return Result.Success(PageModel(items, totalPages))
        }
    }

    private fun paginator(source: Source) = Paginator(source::load)

    @Test
    fun givenFreshPaginator_whenCreated_thenIdleWithNoItemsAndNothingRequested() {
        val source = Source(totalPages = 3)

        val state = paginator(source).state.value

        assertThat(state.items).isEmpty()
        assertThat(state.status).isEqualTo(PagingStatus.Idle)
        assertThat(source.requestedPages).isEmpty()
    }

    @Test
    fun givenSeveralPages_whenLoadingNextRepeatedly_thenItemsAreAppendedInOrder() = runTest {
        val source = Source(totalPages = 3)
        val paginator = paginator(source)

        paginator.loadNext()
        assertThat(paginator.state.value.items).containsExactly(1, 2)
        assertThat(paginator.state.value.status).isEqualTo(PagingStatus.Idle)

        paginator.loadNext()
        paginator.loadNext()

        assertThat(paginator.state.value.items).containsExactly(1, 2, 3, 4, 5, 6)
        assertThat(source.requestedPages).containsExactly(1, 2, 3)
    }

    @Test
    fun givenLastPageLoaded_whenLoadingNextAgain_thenEndReachedAndNoMoreRequests() = runTest {
        val source = Source(totalPages = 2)
        val paginator = paginator(source)

        paginator.loadNext()
        paginator.loadNext()
        assertThat(paginator.state.value.endReached).isTrue()

        paginator.loadNext()
        paginator.loadNext()

        assertThat(source.requestedPages).containsExactly(1, 2)
        assertThat(paginator.state.value.items.size).isEqualTo(4)
    }

    @Test
    fun givenSinglePage_whenLoaded_thenEndReachedImmediately() = runTest {
        val paginator = paginator(Source(totalPages = 1))

        paginator.loadNext()

        assertThat(paginator.state.value.endReached).isTrue()
        assertThat(paginator.state.value.isEmpty).isFalse()
    }

    @Test
    fun givenZeroTotalPages_whenLoading_thenEmptyListThatIsFinished() = runTest {
        val source = Source(totalPages = 0)
        val paginator = paginator(source)

        paginator.loadNext()
        paginator.loadNext()

        assertThat(paginator.state.value.isEmpty).isTrue()
        assertThat(source.requestedPages).containsExactly(1)
    }

    @Test
    fun givenServerReportsMorePagesButReturnsNothing_whenLoading_thenStopsInsteadOfLoopingForever() = runTest {
        val paginator = Paginator<Int> { Result.Success(PageModel(emptyList(), totalPages = 50)) }

        paginator.loadNext()

        assertThat(paginator.state.value.endReached).isTrue()
    }

    @Test
    fun givenRequestInFlight_whenLoadNextIsCalledAgain_thenItIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val requested = mutableListOf<Int>()
        val paginator = Paginator { page ->
            requested += page
            gate.await()
            Result.Success(PageModel(listOf(page), totalPages = 5))
        }

        val first = launch { paginator.loadNext() }
        advanceUntilIdle()
        assertThat(paginator.state.value.status).isEqualTo(PagingStatus.LoadingFirstPage)

        paginator.loadNext()
        paginator.loadNext()
        gate.complete(Unit)
        first.join()

        assertThat(requested).containsExactly(1)
        assertThat(paginator.state.value.items).containsExactly(1)
    }

    @Test
    fun givenItemsLoaded_whenLoadingMore_thenStatusIsLoadingMoreAndItemsStayVisible() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val paginator = Paginator { page ->
            if (++calls > 1) gate.await()
            Result.Success(PageModel(listOf(page), totalPages = 5))
        }
        paginator.loadNext()

        paginator.state.test {
            assertThat(awaitItem().status).isEqualTo(PagingStatus.Idle)
            val loading = launch { paginator.loadNext() }
            val during = awaitItem()
            assertThat(during.status).isEqualTo(PagingStatus.LoadingMore)
            assertThat(during.items).containsExactly(1)
            gate.complete(Unit)
            loading.join()
            assertThat(awaitItem().items).containsExactly(1, 2)
        }
    }

    @Test
    fun givenFirstPageFails_whenLoading_thenFailedWithoutItemsAndRetryStartsAtPageOne() = runTest {
        val source = Source(totalPages = 2).apply { failure = DataError.Network.NoInternet }
        val paginator = paginator(source)

        paginator.loadNext()

        assertThat(paginator.state.value.error).isEqualTo(DataError.Network.NoInternet)
        assertThat(paginator.state.value.items).isEmpty()

        source.failure = null
        paginator.loadNext()

        assertThat(source.requestedPages).containsExactly(1, 1)
        assertThat(paginator.state.value.items).containsExactly(1, 2)
        assertThat(paginator.state.value.error).isEqualTo(null)
    }

    @Test
    fun givenLaterPageFails_whenLoading_thenItemsAreKeptAndRetryRequestsThatSamePage() = runTest {
        val source = Source(totalPages = 3)
        val paginator = paginator(source)
        paginator.loadNext()

        source.failure = DataError.Network.ServerError
        paginator.loadNext()
        assertThat(paginator.state.value.items).containsExactly(1, 2)
        assertThat(paginator.state.value.error).isEqualTo(DataError.Network.ServerError)

        source.failure = null
        paginator.loadNext()

        assertThat(source.requestedPages).containsExactly(1, 2, 2)
        assertThat(paginator.state.value.items).containsExactly(1, 2, 3, 4)
    }

    @Test
    fun givenLoadedList_whenReset_thenBackToPristineState() = runTest {
        val source = Source(totalPages = 3)
        val paginator = paginator(source)
        paginator.loadNext()
        paginator.loadNext()

        paginator.reset()

        assertThat(paginator.state.value).isEqualTo(PagingState())
    }

    @Test
    fun givenLoadedList_whenRefreshed_thenReloadsFromPageOneReplacingItems() = runTest {
        val source = Source(totalPages = 3)
        val paginator = paginator(source)
        paginator.loadNext()
        paginator.loadNext()

        paginator.refresh()

        assertThat(source.requestedPages).containsExactly(1, 2, 1)
        assertThat(paginator.state.value.items).containsExactly(1, 2)
    }

    @Test
    fun givenResponseArrivesAfterReset_whenLoading_thenStaleResultIsDropped() = runTest {
        val gate = CompletableDeferred<Unit>()
        var firstCall = true
        val paginator = Paginator { page ->
            if (firstCall) {
                firstCall = false
                gate.await()
                Result.Success(PageModel(listOf(-1), totalPages = 9)) // stale "old query" data
            } else {
                Result.Success(PageModel(listOf(page * 10), totalPages = 9))
            }
        }

        val stale = launch { paginator.loadNext() }
        advanceUntilIdle()
        paginator.reset()
        paginator.loadNext()
        gate.complete(Unit)
        stale.join()

        assertThat(paginator.state.value.items).containsExactly(10)
        assertThat(paginator.state.value.nextPage).isEqualTo(2)
    }

    @Test
    fun givenFailureAfterReset_whenStaleRequestFails_thenCurrentListIsNotMarkedFailed() = runTest {
        val gate = CompletableDeferred<Unit>()
        var firstCall = true
        val paginator = Paginator { page ->
            if (firstCall) {
                firstCall = false
                gate.await()
                Result.Failure(DataError.Network.Unknown)
            } else Result.Success(PageModel(listOf(page), totalPages = 9))
        }

        val stale = launch { paginator.loadNext() }
        advanceUntilIdle()
        paginator.reset()
        paginator.loadNext()
        gate.complete(Unit)
        stale.join()

        assertThat(paginator.state.value.error).isEqualTo(null)
        assertThat(paginator.state.value.items).containsExactly(1)
    }
}
