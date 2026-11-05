package com.enmanuelbergling.corntime.feature.movies.presentation.section

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MovieSectionViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource()
    private val perPage = FakeMovieData.MOVIES.size

    private fun viewModel(section: MovieSection = MovieSection.Popular) =
        MovieSectionViewModel(section, movies)

    @Test
    fun givenEachSection_whenCreated_thenFirstPageComesFromItsOwnEndpoint() = runTest {
        val expected = mapOf(
            MovieSection.Upcoming to MovieRemoteFunction.GetUpcomingMovies,
            MovieSection.TopRated to MovieRemoteFunction.GetTopRatedMovies,
            MovieSection.NowPlaying to MovieRemoteFunction.GetNowPlayingMovies,
            MovieSection.Popular to MovieRemoteFunction.GetPopularMovies,
        )
        expected.forEach { (section, function) ->
            val source = FakeMovieRemoteDataSource()

            val state = MovieSectionViewModel(section, source).state.value

            assertThat(state.section).isEqualTo(section)
            assertThat(state.paging.items.size).isEqualTo(perPage)
            assertThat(source.callCount(function)).isEqualTo(1)
        }
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingFirstPageThenItems() = runTest {
        movies.hold()
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.isLoadingFirstPage).isTrue()

        movies.release()

        assertThat(viewModel.state.value.paging.isLoadingFirstPage).isFalse()
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenSeveralPages_whenLoadingNextPage_thenItemsAreAppended() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()

        viewModel.onAction(MovieSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage * 2)
        assertThat(movies.callsTo(MovieRemoteFunction.GetPopularMovies)).isEqualTo(listOf(listOf<Any?>(1), listOf<Any?>(2)))
    }

    @Test
    fun givenLastPage_whenLoadingNextPage_thenNoMoreRequests() = runTest {
        movies.totalPages = 2
        val viewModel = viewModel()
        viewModel.onAction(MovieSectionAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.endReached).isTrue()

        viewModel.onAction(MovieSectionAction.OnLoadNextPage)
        viewModel.onAction(MovieSectionAction.OnLoadNextPage)

        assertThat(movies.callCount(MovieRemoteFunction.GetPopularMovies)).isEqualTo(2)
    }

    @Test
    fun givenNoMovies_whenCreated_thenEmptyAndFinished() = runTest {
        movies.totalPages = 0

        val paging = viewModel().state.value.paging

        assertThat(paging.items).isEmpty()
        assertThat(paging.isEmpty).isTrue()
    }

    @Test
    fun givenFirstPageFails_whenCreated_thenErrorThenRetryRecovers() = runTest {
        movies.fail(MovieRemoteFunction.GetPopularMovies, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.error).isEqualTo(DataError.Network.NoInternet)

        movies.recover(MovieRemoteFunction.GetPopularMovies)
        viewModel.onAction(MovieSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.error).isNull()
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenLaterPageFails_whenRetried_thenLoadedItemsAreKeptAndSamePageRequestedAgain() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        movies.fail(MovieRemoteFunction.GetPopularMovies)

        viewModel.onAction(MovieSectionAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
        assertThat(viewModel.state.value.paging.error).isEqualTo(DataError.Network.Unknown)

        movies.recover(MovieRemoteFunction.GetPopularMovies)
        viewModel.onAction(MovieSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage * 2)
        assertThat(movies.callsTo(MovieRemoteFunction.GetPopularMovies).map { it.single() })
            .isEqualTo(listOf<Any?>(1, 2, 2))
    }

    @Test
    fun givenLoadingNextPage_whenTriggeredRepeatedly_thenOnlyOneRequestIsMade() = runTest {
        movies.totalPages = 5
        val viewModel = viewModel()
        movies.hold()

        repeat(5) { viewModel.onAction(MovieSectionAction.OnLoadNextPage) }
        movies.release()

        assertThat(movies.callCount(MovieRemoteFunction.GetPopularMovies)).isEqualTo(2)
    }

    @Test
    fun givenClickAndBack_whenHandled_thenEmitsNavigationEvents() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MovieSectionAction.OnMovieClick(movieId = 9))
            assertThat(awaitItem()).isEqualTo(MovieSectionEvent.NavigateToDetails(9))

            viewModel.onAction(MovieSectionAction.OnBack)
            assertThat(awaitItem()).isEqualTo(MovieSectionEvent.NavigateBack)
        }
    }
}
