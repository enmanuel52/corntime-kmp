package com.enmanuelbergling.corntime.feature.actor.presentation.details

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.core.testing.datasource.ActorRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.FakeActorRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.actor.domain.details.ActorDetailsChain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ActorDetailsViewModelTest : MainDispatcherTest() {
    private val actors = FakeActorRemoteDataSource()

    private fun viewModel(actorId: Int = 5) = ActorDetailsViewModel(actorId, ActorDetailsChain(actors))

    @Test
    fun givenNewViewModel_whenCreated_thenDetailsAndMoviesAreLoadedForThatActor() {
        val state = viewModel(actorId = 5).state.value

        assertThat(state.details?.id).isEqualTo(5)
        assertThat(state.knownMovies).isEqualTo(FakeActorData.KNOWN_MOVIES)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun givenRequestInFlight_whenCreated_thenIsLoadingAndNothingShownYet() = runTest {
        actors.hold()

        val viewModel = viewModel()

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.details).isNull()
        actors.release()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun givenDetailsFail_whenCreated_thenErrorAndNothingElseIsRequestedThenRetryRecovers() = runTest {
        actors.fail(ActorRemoteFunction.GetActorDetails, DataError.Network.ServerError)
        val viewModel = viewModel()

        assertThat(viewModel.state.value.error).isEqualTo(UiError.Server)
        assertThat(viewModel.state.value.details).isNull()
        assertThat(actors.callCount(ActorRemoteFunction.GetMoviesByActor)).isEqualTo(0)

        actors.recoverAll()
        viewModel.onAction(ActorDetailsAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.details).isNotNull()
        assertThat(viewModel.state.value.knownMovies).isEqualTo(FakeActorData.KNOWN_MOVIES)
    }

    @Test
    fun givenMoviesFail_whenRetrying_thenDetailsAreKeptAndNotRefetched() = runTest {
        actors.fail(ActorRemoteFunction.GetMoviesByActor, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.details).isNotNull()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.NoInternet)

        actors.recoverAll()
        viewModel.onAction(ActorDetailsAction.OnRetry)

        assertThat(actors.callCount(ActorRemoteFunction.GetActorDetails)).isEqualTo(1)
        assertThat(viewModel.state.value.knownMovies).isEqualTo(FakeActorData.KNOWN_MOVIES)
    }

    @Test
    fun givenAnActorWithNoMovies_whenLoaded_thenEmptyListAndNoError() {
        actors.knownMovies = emptyList()

        val state = viewModel().state.value

        assertThat(state.knownMovies).isEmpty()
        assertThat(state.error).isNull()
    }

    @Test
    fun givenRetryWhileLoading_whenTapped_thenItIsIgnored() = runTest {
        actors.hold()
        val viewModel = viewModel()

        viewModel.onAction(ActorDetailsAction.OnRetry)

        assertThat(actors.callCount(ActorRemoteFunction.GetActorDetails)).isEqualTo(1)
        actors.release()
    }

    @Test
    fun givenAnError_whenDismissed_thenItIsCleared() = runTest {
        actors.fail(ActorRemoteFunction.GetActorDetails)
        val viewModel = viewModel()

        viewModel.onAction(ActorDetailsAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenBackAndMovieClicks_whenDispatched_thenEachEventIsSentOnceWithItsArguments() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ActorDetailsAction.OnMovieClick(42))
            viewModel.onAction(ActorDetailsAction.OnBack)

            assertThat(awaitItem()).isEqualTo(ActorDetailsEvent.NavigateToMovie(42))
            assertThat(awaitItem()).isEqualTo(ActorDetailsEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
