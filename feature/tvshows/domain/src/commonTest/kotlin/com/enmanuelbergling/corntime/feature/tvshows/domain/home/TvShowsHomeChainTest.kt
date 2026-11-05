package com.enmanuelbergling.corntime.feature.tvshows.domain.home

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import com.enmanuelbergling.corntime.core.domain.design.invoke
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TvShowsHomeChainTest {
    private val dataSource = FakeTvRemoteDataSource()
    private val chain get() = TvShowsHomeChain(dataSource).create()

    @Test
    fun givenAllEndpointsSucceed_whenInvoked_thenEverySectionIsFilled() = runTest {
        val request = TvShowsHomeRequest()

        chain.invoke(request)

        TvShowsSection.entries.forEach { assertThat(request.sections[it].orEmpty()).isNotEmpty() }
        assertThat(request.sections.keys).isEqualTo(TvShowsSection.entries.toSet())
    }

    @Test
    fun givenAnyChain_whenInvoked_thenEachEndpointIsRequestedExactlyOnce() = runTest {
        chain.invoke(TvShowsHomeRequest())

        listOf(
            TvRemoteFunction.GetPopularTv,
            TvRemoteFunction.GetTopRatedTv,
            TvRemoteFunction.GetOnTheAirTv,
            TvRemoteFunction.GetAiringTodayTv,
        ).forEach { assertThat(dataSource.callCount(it)).isEqualTo(1) }
    }

    @Test
    fun givenThirdEndpointFails_whenInvoked_thenFirstTwoAreKeptAndLastIsNeverRequested() = runTest {
        dataSource.fail(TvRemoteFunction.GetOnTheAirTv, DataError.Network.RequestTimeout)
        val request = TvShowsHomeRequest()

        assertFailure { chain.invoke(request) }.isInstanceOf<DataException>()

        assertThat(request.sections[TvShowsSection.Popular].orEmpty()).isNotEmpty()
        assertThat(request.sections[TvShowsSection.TopRated].orEmpty()).isNotEmpty()
        assertThat(request.sections[TvShowsSection.OnTheAir].orEmpty()).isEmpty()
        assertThat(dataSource.callCount(TvRemoteFunction.GetAiringTodayTv)).isEqualTo(0)
    }

    @Test
    fun givenFirstEndpointFails_whenInvoked_thenNothingIsLoaded() = runTest {
        dataSource.fail(TvRemoteFunction.GetPopularTv)
        val request = TvShowsHomeRequest()

        assertFailure { chain.invoke(request) }

        assertThat(request.sections.values.flatten()).isEmpty()
        assertThat(dataSource.callCount(TvRemoteFunction.GetTopRatedTv)).isEqualTo(0)
    }

    @Test
    fun givenRetryAfterPartialFailure_whenInvokedAgain_thenOnlyMissingSectionsAreRequested() = runTest {
        dataSource.fail(TvRemoteFunction.GetOnTheAirTv)
        val request = TvShowsHomeRequest()
        assertFailure { chain.invoke(request) }

        dataSource.recover(TvRemoteFunction.GetOnTheAirTv)
        chain.invoke(TvShowsHomeRequest(request.sections))

        assertThat(dataSource.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(1)
        assertThat(dataSource.callCount(TvRemoteFunction.GetTopRatedTv)).isEqualTo(1)
        assertThat(dataSource.callCount(TvRemoteFunction.GetOnTheAirTv)).isEqualTo(2)
        assertThat(dataSource.callCount(TvRemoteFunction.GetAiringTodayTv)).isEqualTo(1)
    }

    @Test
    fun givenEmptyEndpointResult_whenInvoked_thenSectionStaysEmptyAndIsRequestedAgainOnRetry() = runTest {
        dataSource.totalPages = 0
        val request = TvShowsHomeRequest()

        chain.invoke(request)
        chain.invoke(TvShowsHomeRequest(request.sections))

        assertThat(request.sections[TvShowsSection.Popular].orEmpty()).isEmpty()
        assertThat(dataSource.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(2)
    }

    @Test
    fun givenFirstPageResults_whenInvoked_thenSectionHoldsExactlyThatPage() = runTest {
        val request = TvShowsHomeRequest()

        chain.invoke(request)

        assertThat(request.sections.getValue(TvShowsSection.Popular).map { it.name })
            .containsExactly(*FakeTvData.TV_SHOWS.map { it.name }.toTypedArray())
    }
}
