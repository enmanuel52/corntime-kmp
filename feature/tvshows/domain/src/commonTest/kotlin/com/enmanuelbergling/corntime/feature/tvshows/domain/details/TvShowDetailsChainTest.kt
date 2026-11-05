package com.enmanuelbergling.corntime.feature.tvshows.domain.details

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.design.invoke
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TvShowDetailsChainTest {
    private val dataSource = FakeTvRemoteDataSource()
    private val chain get() = TvShowDetailsChain(dataSource).create()

    @Test
    fun givenAllSucceed_whenInvoked_thenDetailsCreditsAndAccountStatesAreFilledForThatShow() = runTest {
        val request = TvShowDetailsChainRequest(tvShowId = 7)

        chain.invoke(request)

        assertThat(request.details?.id).isEqualTo(7)
        assertThat(request.credits?.id).isEqualTo(7)
        assertThat(request.accountStates?.id).isEqualTo(7)
    }

    @Test
    fun givenLoggedOut_whenInvoked_thenDetailsAndCreditsSurviveTheUnauthorizedAccountStates() = runTest {
        dataSource.fail(TvRemoteFunction.GetTvAccountStates, DataError.Network.Unauthorized)
        val request = TvShowDetailsChainRequest(tvShowId = 1)

        val result = chain.invokeForResult(request)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
        assertThat(request.details).isNotNull()
        assertThat(request.credits).isNotNull()
        assertThat(request.accountStates).isNull()
    }

    @Test
    fun givenDetailsFail_whenInvoked_thenCreditsAreNeverRequested() = runTest {
        dataSource.fail(TvRemoteFunction.GetTvDetails, DataError.Network.ServerError)

        assertFailure { chain.invoke(TvShowDetailsChainRequest(1)) }.isInstanceOf<DataException>()

        assertThat(dataSource.callCount(TvRemoteFunction.GetTvCredits)).isEqualTo(0)
        assertThat(dataSource.callCount(TvRemoteFunction.GetTvAccountStates)).isEqualTo(0)
    }

    @Test
    fun givenCreditsFail_whenInvoked_thenDetailsAreKept() = runTest {
        dataSource.fail(TvRemoteFunction.GetTvCredits)
        val request = TvShowDetailsChainRequest(1)

        assertFailure { chain.invoke(request) }

        assertThat(request.details).isNotNull()
        assertThat(request.credits).isNull()
    }

    @Test
    fun givenAlreadyLoadedDetailsAndCredits_whenRetried_thenOnlyAccountStatesAreRequested() = runTest {
        dataSource.fail(TvRemoteFunction.GetTvAccountStates)
        val request = TvShowDetailsChainRequest(1)
        chain.invokeForResult(request)

        dataSource.recover(TvRemoteFunction.GetTvAccountStates)
        chain.invoke(request)

        assertThat(dataSource.callCount(TvRemoteFunction.GetTvDetails)).isEqualTo(1)
        assertThat(dataSource.callCount(TvRemoteFunction.GetTvCredits)).isEqualTo(1)
        assertThat(dataSource.callCount(TvRemoteFunction.GetTvAccountStates)).isEqualTo(2)
        assertThat(request.accountStates).isNotNull()
    }
}
