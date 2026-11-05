package com.enmanuelbergling.corntime.feature.actor.domain.details

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.design.invoke
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.core.testing.datasource.ActorRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.FakeActorRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ActorDetailsChainTest {
    private val dataSource = FakeActorRemoteDataSource()
    private val chain get() = ActorDetailsChain(dataSource).create()

    @Test
    fun givenAllSucceed_whenInvoked_thenDetailsAndKnownMoviesAreFilledForThatActor() = runTest {
        val request = ActorDetailsChainRequest(actorId = 7)

        chain.invoke(request)

        assertThat(request.details?.id).isEqualTo(7)
        assertThat(request.knownMovies).isEqualTo(FakeActorData.KNOWN_MOVIES)
    }

    @Test
    fun givenDetailsFail_whenInvoked_thenKnownMoviesAreNeverRequested() = runTest {
        dataSource.fail(ActorRemoteFunction.GetActorDetails, DataError.Network.ServerError)

        assertFailure { chain.invoke(ActorDetailsChainRequest(1)) }.isInstanceOf<DataException>()

        assertThat(dataSource.callCount(ActorRemoteFunction.GetMoviesByActor)).isEqualTo(0)
    }

    @Test
    fun givenKnownMoviesFail_whenInvoked_thenDetailsAreKeptInTheRequest() = runTest {
        dataSource.fail(ActorRemoteFunction.GetMoviesByActor, DataError.Network.NoInternet)
        val request = ActorDetailsChainRequest(1)

        val result = chain.invokeForResult(request)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.NoInternet))
        assertThat(request.details).isNotNull()
        assertThat(request.knownMovies).isNull()
    }

    @Test
    fun givenDetailsAlreadyLoaded_whenRetrying_thenOnlyTheMissingPartIsRequested() = runTest {
        val request = ActorDetailsChainRequest(1, details = FakeActorData.ACTOR_DETAILS)

        chain.invoke(request)

        assertThat(dataSource.callCount(ActorRemoteFunction.GetActorDetails)).isEqualTo(0)
        assertThat(dataSource.callCount(ActorRemoteFunction.GetMoviesByActor)).isEqualTo(1)
    }

    @Test
    fun givenAnActorWithoutMovies_whenInvokedTwice_thenTheEmptyListCountsAsLoadedAndIsNotRefetched() = runTest {
        dataSource.knownMovies = emptyList()
        val request = ActorDetailsChainRequest(1)

        chain.invoke(request)
        chain.invoke(request)

        assertThat(request.knownMovies!!).isEmpty()
        assertThat(dataSource.callCount(ActorRemoteFunction.GetMoviesByActor)).isEqualTo(1)
    }
}
