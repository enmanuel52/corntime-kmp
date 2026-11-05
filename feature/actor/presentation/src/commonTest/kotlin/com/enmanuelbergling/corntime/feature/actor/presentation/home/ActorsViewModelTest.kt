package com.enmanuelbergling.corntime.feature.actor.presentation.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.core.testing.datasource.ActorRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.FakeActorRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ActorsViewModelTest : MainDispatcherTest() {
    private val actors = FakeActorRemoteDataSource()

    private fun viewModel() = ActorsViewModel(actors)

    @Test
    fun givenNewViewModel_whenCreated_thenFirstPageIsLoaded() {
        val state = viewModel().state.value

        assertThat(state.actors.size).isEqualTo(2)
        assertThat(actors.callsTo(ActorRemoteFunction.GetPopularActors)).isEqualTo(listOf(listOf<Any?>(1)))
    }

    @Test
    fun givenMorePages_whenLoadingNext_thenItemsAreAppendedAndPageTwoIsRequested() = runTest {
        actors.totalPages = 3
        val viewModel = viewModel()

        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.actors.size).isEqualTo(4)
        assertThat(actors.callsTo(ActorRemoteFunction.GetPopularActors).map { it.single() }).isEqualTo(listOf<Any?>(1, 2))
        assertThat(viewModel.state.value.paging.endReached).isFalse()
    }

    @Test
    fun givenLastPage_whenLoadingNextAgain_thenNoFurtherRequestIsMade() = runTest {
        actors.totalPages = 1
        val viewModel = viewModel()

        viewModel.onAction(ActorsAction.OnLoadNextPage)
        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.endReached).isTrue()
        assertThat(actors.callCount(ActorRemoteFunction.GetPopularActors)).isEqualTo(1)
    }

    @Test
    fun givenLoadingInFlight_whenLoadingNext_thenItIsIgnored() = runTest {
        actors.hold()
        val viewModel = viewModel()

        viewModel.onAction(ActorsAction.OnLoadNextPage)
        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.isLoadingFirstPage).isTrue()
        assertThat(actors.callCount(ActorRemoteFunction.GetPopularActors)).isEqualTo(1)
        actors.release()
    }

    @Test
    fun givenFirstPageFails_whenCreated_thenErrorAndNoItemsThenRetryRecovers() = runTest {
        actors.fail(ActorRemoteFunction.GetPopularActors, DataError.Network.NoInternet)
        val viewModel = viewModel()

        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.actors.size).isEqualTo(0)

        actors.recoverAll()
        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.uiError).isNull()
        assertThat(viewModel.state.value.actors.size).isEqualTo(2)
    }

    @Test
    fun givenSecondPageFails_whenLoadingNext_thenLoadedActorsAreKeptAndRetryFetchesThatSamePage() = runTest {
        actors.totalPages = 3
        val viewModel = viewModel()
        actors.fail(ActorRemoteFunction.GetPopularActors, DataError.Network.RequestTimeout)

        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.actors.size).isEqualTo(2)
        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.Timeout)

        actors.recoverAll()
        viewModel.onAction(ActorsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.actors.size).isEqualTo(4)
        assertThat(actors.callsTo(ActorRemoteFunction.GetPopularActors).map { it.single() }).isEqualTo(listOf<Any?>(1, 2, 2))
    }

    @Test
    fun givenNoPagesAtAll_whenCreated_thenEmptyAndEnded() {
        actors.totalPages = 0

        val state = viewModel().state.value

        assertThat(state.paging.isEmpty).isTrue()
    }

    @Test
    fun givenAnActor_whenClicked_thenNavigationCarriesItsIdOriginalNameAndPhotoOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ActorsAction.OnActorClick(FakeActorData.ACTOR.copy(id = 9, profilePath = "/x.jpg")))

            assertThat(awaitItem()).isEqualTo(ActorsEvent.NavigateToDetails(9, FakeActorData.ACTOR.originalName, "/x.jpg"))
            expectNoEvents()
        }
    }
}
