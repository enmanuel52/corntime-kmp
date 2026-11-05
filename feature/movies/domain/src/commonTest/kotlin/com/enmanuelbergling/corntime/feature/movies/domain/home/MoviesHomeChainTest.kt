package com.enmanuelbergling.corntime.feature.movies.domain.home

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isInstanceOf
import com.enmanuelbergling.corntime.core.domain.design.invoke
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoviesHomeChainTest {
    private val dataSource = FakeMovieRemoteDataSource()
    private val chain get() = MoviesHomeChain(dataSource).create()

    @Test
    fun givenAllEndpointsSucceed_whenInvoked_thenEverySectionIsFilled() = runTest {
        val request = MoviesHomeRequest()

        chain.invoke(request)

        MovieSection.entries.forEach { assertThat(request.sections[it].orEmpty()).isNotEmpty() }
        assertThat(request.sections.keys).isEqualTo(MovieSection.entries.toSet())
    }

    @Test
    fun givenAnyChain_whenInvoked_thenSectionsAreFetchedInHomeOrder() = runTest {
        chain.invoke(MoviesHomeRequest())

        val order = listOf(
            MovieRemoteFunction.GetUpcomingMovies,
            MovieRemoteFunction.GetTopRatedMovies,
            MovieRemoteFunction.GetNowPlayingMovies,
            MovieRemoteFunction.GetPopularMovies,
        )
        // each endpoint was requested exactly once
        order.forEach { assertThat(dataSource.callCount(it)).isEqualTo(1) }
    }

    @Test
    fun givenThirdEndpointFails_whenInvoked_thenFirstTwoAreKeptAndLaterOnesNeverRequested() = runTest {
        dataSource.fail(MovieRemoteFunction.GetNowPlayingMovies, DataError.Network.RequestTimeout)
        val request = MoviesHomeRequest()

        assertFailure { chain.invoke(request) }
            .isInstanceOf<DataException>()

        assertThat(request.sections[MovieSection.Upcoming].orEmpty()).isNotEmpty()
        assertThat(request.sections[MovieSection.TopRated].orEmpty()).isNotEmpty()
        assertThat(request.sections[MovieSection.NowPlaying].orEmpty()).isEmpty()
        assertThat(request.sections[MovieSection.Popular].orEmpty()).isEmpty()
        assertThat(dataSource.callCount(MovieRemoteFunction.GetPopularMovies)).isEqualTo(0)
    }

    @Test
    fun givenFirstEndpointFails_whenInvoked_thenNothingIsLoaded() = runTest {
        dataSource.fail(MovieRemoteFunction.GetUpcomingMovies)
        val request = MoviesHomeRequest()

        assertFailure { chain.invoke(request) }

        assertThat(request.sections.values.flatten()).isEmpty()
        assertThat(dataSource.callCount(MovieRemoteFunction.GetTopRatedMovies)).isEqualTo(0)
    }

    @Test
    fun givenRetryAfterPartialFailure_whenInvokedAgain_thenOnlyMissingSectionsAreRequested() = runTest {
        dataSource.fail(MovieRemoteFunction.GetNowPlayingMovies)
        val request = MoviesHomeRequest()
        assertFailure { chain.invoke(request) }

        dataSource.recover(MovieRemoteFunction.GetNowPlayingMovies)
        chain.invoke(MoviesHomeRequest(request.sections))

        assertThat(dataSource.callCount(MovieRemoteFunction.GetUpcomingMovies)).isEqualTo(1)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetTopRatedMovies)).isEqualTo(1)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetNowPlayingMovies)).isEqualTo(2)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetPopularMovies)).isEqualTo(1)
    }

    @Test
    fun givenEmptyEndpointResult_whenInvoked_thenSectionStaysEmptyAndIsRequestedAgainOnRetry() = runTest {
        dataSource.totalPages = 0
        val request = MoviesHomeRequest()

        chain.invoke(request)
        chain.invoke(MoviesHomeRequest(request.sections))

        assertThat(request.sections[MovieSection.Popular].orEmpty()).isEmpty()
        assertThat(dataSource.callCount(MovieRemoteFunction.GetPopularMovies)).isEqualTo(2)
    }

    @Test
    fun givenFirstPageResults_whenInvoked_thenSectionHoldsExactlyThatPage() = runTest {
        val request = MoviesHomeRequest()

        chain.invoke(request)

        assertThat(request.sections.getValue(MovieSection.Popular).map { it.title })
            .containsExactly(*FakeMovieData.MOVIES.map { it.title }.toTypedArray())
    }
}
