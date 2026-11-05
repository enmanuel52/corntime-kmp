package com.enmanuelbergling.corntime.feature.movies.domain.details

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
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MovieDetailsChainTest {
    private val dataSource = FakeMovieRemoteDataSource()
    private val chain get() = MovieDetailsChain(dataSource).create()

    @Test
    fun givenAllSucceed_whenInvoked_thenDetailsCreditsAndAccountStatesAreFilledForThatMovie() = runTest {
        val request = MovieDetailsChainRequest(movieId = 7)

        chain.invoke(request)

        assertThat(request.details?.id).isEqualTo(7)
        assertThat(request.credits?.id).isEqualTo(7)
        assertThat(request.accountStates?.id).isEqualTo(7)
    }

    @Test
    fun givenLoggedOut_whenInvoked_thenDetailsAndCreditsSurviveTheUnauthorizedAccountStates() = runTest {
        dataSource.fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
        val request = MovieDetailsChainRequest(movieId = 1)

        val result = chain.invokeForResult(request)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
        assertThat(request.details).isNotNull()
        assertThat(request.credits).isNotNull()
        assertThat(request.accountStates).isNull()
    }

    @Test
    fun givenDetailsFail_whenInvoked_thenCreditsAreNeverRequested() = runTest {
        dataSource.fail(MovieRemoteFunction.GetMovieDetails, DataError.Network.ServerError)

        assertFailure { chain.invoke(MovieDetailsChainRequest(1)) }.isInstanceOf<DataException>()

        assertThat(dataSource.callCount(MovieRemoteFunction.GetMovieCredits)).isEqualTo(0)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetMovieAccountStates)).isEqualTo(0)
    }

    @Test
    fun givenCreditsFail_whenInvoked_thenDetailsAreKept() = runTest {
        dataSource.fail(MovieRemoteFunction.GetMovieCredits)
        val request = MovieDetailsChainRequest(1)

        assertFailure { chain.invoke(request) }

        assertThat(request.details).isNotNull()
        assertThat(request.credits).isNull()
    }

    @Test
    fun givenAlreadyLoadedDetailsAndCredits_whenRetried_thenOnlyAccountStatesAreRequested() = runTest {
        dataSource.fail(MovieRemoteFunction.GetMovieAccountStates)
        val request = MovieDetailsChainRequest(1)
        chain.invokeForResult(request)

        dataSource.recover(MovieRemoteFunction.GetMovieAccountStates)
        chain.invoke(request)

        assertThat(dataSource.callCount(MovieRemoteFunction.GetMovieDetails)).isEqualTo(1)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetMovieCredits)).isEqualTo(1)
        assertThat(dataSource.callCount(MovieRemoteFunction.GetMovieAccountStates)).isEqualTo(2)
        assertThat(request.accountStates).isNotNull()
    }
}
