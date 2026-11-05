package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieFilter
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoviesFilterViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource()
    private val perPage = FakeMovieData.MOVIES.size
    private val debounceMs = FILTER_DEBOUNCE.inWholeMilliseconds
    private val action = FakeMovieData.GENRES[0]
    private val drama = FakeMovieData.GENRES[1]

    private fun viewModel() = MoviesFilterViewModel(movies)

    private fun discoveries() = movies.callsTo(MovieRemoteFunction.GetMoviesByGenre)

    private fun MoviesFilterViewModel.send(action: MoviesFilterAction) = onAction(action)

    @Test
    fun givenNewViewModel_whenCreated_thenDefaultFilterLoadsImmediatelyAndGenresAreFetched() = runTest {
        val viewModel = viewModel()

        assertThat(discoveries()).isEqualTo(listOf(listOf<Any?>("", "popularity.desc", 1)))
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage)
        assertThat(viewModel.state.value.genres).isEqualTo(FakeMovieData.GENRES)
        assertThat(viewModel.state.value.isLoadingGenres).isFalse()
        assertThat(viewModel.state.value.hasActiveFilter).isFalse()
    }

    @Test
    fun givenGenresFail_whenCreated_thenErrorIsShownAndRetryRecovers() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieGenres, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.genresError).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.genres).isEmpty()

        movies.recover(MovieRemoteFunction.GetMovieGenres)
        viewModel.send(MoviesFilterAction.OnRetryGenres)

        assertThat(viewModel.state.value.genresError).isNull()
        assertThat(viewModel.state.value.genres.size).isEqualTo(FakeMovieData.GENRES.size)
    }

    @Test
    fun givenGenresFail_whenDismissed_thenErrorDisappearsButMoviesStayLoaded() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieGenres)
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnDismissError)

        assertThat(viewModel.state.value.genresError).isNull()
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage)
    }

    @Test
    fun givenGenreToggled_whenSelectedTwice_thenItIsAddedThenRemoved() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        assertThat(viewModel.state.value.isSelected(action)).isTrue()
        assertThat(viewModel.state.value.hasActiveFilter).isTrue()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        assertThat(viewModel.state.value.isSelected(action)).isFalse()
        assertThat(viewModel.state.value.hasActiveFilter).isFalse()
    }

    @Test
    fun givenFilterChange_whenWithinDebounceWindow_thenNothingIsRequestedYet() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        advanceTimeBy(debounceMs - 1)

        assertThat(discoveries().size).isEqualTo(1)
    }

    @Test
    fun givenFilterChange_whenDebounceElapses_thenListIsReplacedWithTheFilteredFirstPage() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        viewModel.send(MoviesFilterAction.OnToggleGenre(drama))
        viewModel.send(MoviesFilterAction.OnPickSortCriteria(SortCriteria.Revenue))
        advanceTimeBy(debounceMs + 1)

        assertThat(discoveries().last()).isEqualTo(listOf<Any?>("${action.id},${drama.id}", "revenue.desc", 1))
        assertThat(discoveries().size).isEqualTo(2)
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage)
    }

    @Test
    fun givenEverySortCriteria_whenPicked_thenTheMatchingApiValueIsRequested() = runTest {
        val viewModel = viewModel()
        val expected = mapOf(
            SortCriteria.VoteAverage to "vote_average.desc",
            SortCriteria.VoteCount to "vote_count.desc",
            SortCriteria.Revenue to "revenue.desc",
        )

        expected.forEach { (criteria, apiValue) ->
            viewModel.send(MoviesFilterAction.OnPickSortCriteria(criteria))
            advanceTimeBy(debounceMs + 1)
            assertThat(discoveries().last()[1]).isEqualTo(apiValue)
        }
    }

    @Test
    fun givenFilterToggledBackBeforeDebounce_whenSettled_thenItDoesNotReloadTheSameFilter() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        advanceTimeBy(debounceMs + 1)

        assertThat(discoveries().size).isEqualTo(1)
    }

    @Test
    fun givenActiveFilter_whenCleared_thenDefaultFilterIsLoadedAgain() = runTest {
        val viewModel = viewModel()
        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        advanceTimeBy(debounceMs + 1)

        viewModel.send(MoviesFilterAction.OnClearFilter)
        advanceTimeBy(debounceMs + 1)

        assertThat(viewModel.state.value.filter).isEqualTo(MovieFilter())
        assertThat(discoveries().last()).isEqualTo(listOf<Any?>("", "popularity.desc", 1))
    }

    @Test
    fun givenDefaultFilter_whenCleared_thenNothingIsReloaded() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnClearFilter)
        advanceTimeBy(debounceMs + 1)

        assertThat(discoveries().size).isEqualTo(1)
    }

    @Test
    fun givenResults_whenLoadingNextPage_thenRequestsTheNextPageOfTheActiveFilter() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        advanceTimeBy(debounceMs + 1)

        viewModel.send(MoviesFilterAction.OnLoadNextPage)

        assertThat(discoveries().last()).isEqualTo(listOf<Any?>("${action.id}", "popularity.desc", 2))
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage * 2)
    }

    @Test
    fun givenNewFilterStillDebouncing_whenLoadingNextPage_thenPageIsRequestedForTheActiveFilter() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        viewModel.send(MoviesFilterAction.OnLoadNextPage)

        assertThat(discoveries().last()).isEqualTo(listOf<Any?>("", "popularity.desc", 2))
    }

    @Test
    fun givenLastPage_whenLoadingNextPage_thenNothingMoreIsRequested() = runTest {
        movies.totalPages = 1
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnLoadNextPage)

        assertThat(discoveries().size).isEqualTo(1)
        assertThat(viewModel.state.value.paging.endReached).isTrue()
    }

    @Test
    fun givenNoMovieMatches_whenLoaded_thenPagingIsEmpty() = runTest {
        movies.totalPages = 0

        val viewModel = viewModel()

        assertThat(viewModel.state.value.paging.isEmpty).isTrue()
    }

    @Test
    fun givenPageFails_whenRetrying_thenLoadedMoviesAreKeptAndItRecovers() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        movies.fail(MovieRemoteFunction.GetMoviesByGenre, DataError.Network.RequestTimeout)

        viewModel.send(MoviesFilterAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.Timeout)
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage)

        movies.recover(MovieRemoteFunction.GetMoviesByGenre)
        viewModel.send(MoviesFilterAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.error).isNull()
        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage * 2)
    }

    @Test
    fun givenFilterChangesWhileLoading_whenFirstResponseArrivesLate_thenOnlyLatestResultsRemain() = runTest {
        movies.hold()
        val viewModel = viewModel()
        viewModel.send(MoviesFilterAction.OnToggleGenre(action))
        advanceTimeBy(debounceMs + 1)

        movies.release()
        advanceUntilIdle()

        assertThat(viewModel.state.value.movies.size).isEqualTo(perPage)
        assertThat(viewModel.state.value.paging.isLoading).isFalse()
    }

    @Test
    fun givenMovieClick_whenHandled_thenNavigatesToDetailsOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.send(MoviesFilterAction.OnMovieClick(movieId = 9))

            assertThat(awaitItem()).isEqualTo(MoviesFilterEvent.NavigateToDetails(9))
            expectNoEvents()
        }
    }

    @Test
    fun givenBack_whenHandled_thenNavigatesBack() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.send(MoviesFilterAction.OnBack)

            assertThat(awaitItem()).isEqualTo(MoviesFilterEvent.NavigateBack)
        }
    }

    @Test
    fun givenSelectedGenres_whenListed_thenOrderOfSelectionIsKept() = runTest {
        val viewModel = viewModel()

        viewModel.send(MoviesFilterAction.OnToggleGenre(drama))
        viewModel.send(MoviesFilterAction.OnToggleGenre(action))

        assertThat(viewModel.state.value.filter.genres).containsExactly(drama, action)
    }
}
