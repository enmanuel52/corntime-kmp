package com.enmanuelbergling.corntime.feature.movies.presentation.search

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MovieSearchViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource()
    private val perPage = FakeMovieData.MOVIES.size
    private val debounceMs = SEARCH_DEBOUNCE.inWholeMilliseconds

    private fun viewModel() = MovieSearchViewModel(movies)

    private fun searches() = movies.callsTo(MovieRemoteFunction.SearchMovie)

    private fun MovieSearchViewModel.type(query: String) = onAction(MovieSearchAction.OnQueryChange(query))

    @Test
    fun givenNewViewModel_whenCreated_thenBlankAndNothingRequested() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.hasQuery).isFalse()
        assertThat(searches()).isEmpty()
    }

    @Test
    fun givenTyping_whenWithinDebounceWindow_thenNothingIsRequestedYetButQueryIsEchoedImmediately() = runTest {
        val viewModel = viewModel()

        viewModel.type("avengers")
        advanceTimeBy(debounceMs - 1)

        assertThat(viewModel.state.value.query).isEqualTo("avengers")
        assertThat(searches()).isEmpty()
    }

    @Test
    fun givenTypingPause_whenDebounceElapses_thenSearchesOnceWithTheQueryAndFirstPage() = runTest {
        val viewModel = viewModel()

        viewModel.type("avengers")
        advanceTimeBy(debounceMs + 1)

        assertThat(searches()).isEqualTo(listOf(listOf<Any?>("avengers", 1)))
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenRapidTyping_whenPausing_thenOnlyTheLastQueryIsSearched() = runTest {
        val viewModel = viewModel()

        viewModel.type("a")
        advanceTimeBy(100)
        viewModel.type("av")
        advanceTimeBy(100)
        viewModel.type("ave")
        advanceTimeBy(debounceMs + 1)

        assertThat(searches()).isEqualTo(listOf(listOf<Any?>("ave", 1)))
    }

    @Test
    fun givenSameQueryWithSurroundingSpaces_whenTyped_thenQueryIsTrimmedAndNotSearchedAgain() = runTest {
        val viewModel = viewModel()
        viewModel.type("matrix")
        advanceTimeBy(debounceMs + 1)

        viewModel.type("matrix  ")
        advanceTimeBy(debounceMs + 1)
        viewModel.type("  matrix")
        advanceTimeBy(debounceMs + 1)

        assertThat(searches()).isEqualTo(listOf(listOf<Any?>("matrix", 1)))
    }

    @Test
    fun givenResults_whenQueryChanges_thenListIsReplacedAndPagingRestartsAtPageOne() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        viewModel.type("one")
        advanceTimeBy(debounceMs + 1)
        viewModel.onAction(MovieSearchAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage * 2)

        viewModel.type("two")
        advanceTimeBy(debounceMs + 1)

        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
        assertThat(searches().last()).isEqualTo(listOf<Any?>("two", 1))
    }

    @Test
    fun givenResults_whenLoadingNextPage_thenRequestsSameQueryPageTwo() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        viewModel.type("batman")
        advanceTimeBy(debounceMs + 1)

        viewModel.onAction(MovieSearchAction.OnLoadNextPage)

        assertThat(searches().last()).isEqualTo(listOf<Any?>("batman", 2))
    }

    @Test
    fun givenNewQueryStillDebouncing_whenLoadingNextPage_thenPageIsRequestedForTheActiveQueryNotTheTypedOne() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        viewModel.type("abc")
        advanceTimeBy(debounceMs + 1)

        viewModel.type("abcd")
        viewModel.onAction(MovieSearchAction.OnLoadNextPage)

        assertThat(searches().last()).isEqualTo(listOf<Any?>("abc", 2))
    }

    @Test
    fun givenBlankQuery_whenLoadingNextPage_thenIgnored() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MovieSearchAction.OnLoadNextPage)
        viewModel.type("   ")
        advanceTimeBy(debounceMs + 1)
        viewModel.onAction(MovieSearchAction.OnLoadNextPage)

        assertThat(searches()).isEmpty()
    }

    @Test
    fun givenResults_whenCleared_thenResultsDisappearImmediatelyWithoutRequest() = runTest {
        val viewModel = viewModel()
        viewModel.type("alien")
        advanceTimeBy(debounceMs + 1)
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)

        viewModel.onAction(MovieSearchAction.OnClear)
        advanceTimeBy(1)

        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.paging.items).isEmpty()
        assertThat(searches().size).isEqualTo(1)
    }

    @Test
    fun givenQueryWithNoMatches_whenSearched_thenShowNoResults() = runTest {
        movies.totalPages = 0
        val viewModel = viewModel()

        viewModel.type("zzzzzz")
        advanceTimeBy(debounceMs + 1)

        assertThat(viewModel.state.value.showNoResults).isTrue()
    }

    @Test
    fun givenBlankQuery_whenNothingSearched_thenNoResultsIsNotShown() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.showNoResults).isFalse()
    }

    @Test
    fun givenSearchFails_whenSearched_thenErrorAndRetryRecovers() = runTest {
        movies.fail(MovieRemoteFunction.SearchMovie, DataError.Network.NoInternet)
        val viewModel = viewModel()
        viewModel.type("dune")
        advanceTimeBy(debounceMs + 1)
        assertThat(viewModel.state.value.paging.error).isEqualTo(DataError.Network.NoInternet)

        movies.recover(MovieRemoteFunction.SearchMovie)
        viewModel.onAction(MovieSearchAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.error).isNull()
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenQueryChangesWhileRequestInFlight_whenFirstResponseArrivesLate_thenOnlyLatestResultsRemain() = runTest {
        movies.hold()
        val viewModel = viewModel()
        viewModel.type("a")
        advanceTimeBy(debounceMs + 1)
        viewModel.type("b")
        advanceTimeBy(debounceMs + 1)

        movies.release()
        advanceUntilIdle()

        assertThat(searches().map { it.first() }).isEqualTo(listOf<Any?>("a", "b"))
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
        assertThat(viewModel.state.value.paging.isLoading).isFalse()
    }

    @Test
    fun givenMovieClick_whenHandled_thenNavigatesToDetails() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MovieSearchAction.OnMovieClick(movieId = 4))

            assertThat(awaitItem()).isEqualTo(MovieSearchEvent.NavigateToDetails(4))
        }
    }
}
