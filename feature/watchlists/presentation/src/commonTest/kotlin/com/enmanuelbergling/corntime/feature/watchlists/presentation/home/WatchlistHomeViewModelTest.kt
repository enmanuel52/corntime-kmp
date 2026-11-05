package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalTracker
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.sync.FakeMovieSyncScheduler
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.core.testing.util.clearForTest
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.MoveMovieToFavoritesUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WatchlistHomeViewModelTest : MainDispatcherTest() {
    private val user = FakeUserRemoteDataSource()
    private val scheduler = FakeMovieSyncScheduler()
    private val window = RemovalTracker.DEFAULT_UNDO_WINDOW.inWholeMilliseconds
    private val ids get() = FakeMovieData.MOVIES.indices.map { 1_000 + it }

    private fun viewModel() = WatchlistHomeViewModel(user, MoveMovieToFavoritesUseCase(user), scheduler)

    private fun TestScope.pastTheUndoWindow() = advanceTimeBy(window + 1)

    @Test
    fun givenNewViewModel_whenCreated_thenTheFirstPageOfTheWatchlistIsLoaded() {
        assertThat(viewModel().state.value.movies.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAnEmptyWatchlist_whenLoaded_thenItIsEmpty() {
        user.totalPages = 0

        assertThat(viewModel().state.value.isEmpty).isTrue()
    }

    @Test
    fun givenLoadingFails_whenCreated_thenAnErrorIsShown() {
        user.fail(UserRemoteFunction.GetAccountWatchlistMovies, DataError.Network.Unauthorized)

        assertThat(viewModel().state.value.paging.uiError).isEqualTo(UiError.Unauthorized)
    }

    @Test
    fun givenAMovie_whenRemoved_thenItIsHiddenAtOnceAndOnlyRemovedFromTheWatchlistAfterTheWindow() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchlistHomeAction.OnRemove(id))

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids - id)
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)

        pastTheUndoWindow()

        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist).map { it.single() }).containsExactly(id)
        assertThat(user.callCount(UserRemoteFunction.AddMovieToFavorites)).isEqualTo(0)
    }

    @Test
    fun givenAMovie_whenMovedToFavorites_thenItIsAddedToFavoritesAndRemovedFromTheWatchlistAfterTheWindow() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchlistHomeAction.OnMoveToFavorites(id))
        assertThat(viewModel.state.value.undoable).isEqualTo(PendingRemoval(id, RemovalKind.MoveToFavorites))
        pastTheUndoWindow()

        assertThat(user.callsTo(UserRemoteFunction.AddMovieToFavorites).map { it.single() }).containsExactly(id)
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist).map { it.single() }).containsExactly(id)
    }

    @Test
    fun givenARemovalAndAnUndo_whenTheWindowPasses_thenNothingIsSentAndTheMovieIsBack() = runTest {
        val viewModel = viewModel()
        val id = ids.first()
        viewModel.onAction(WatchlistHomeAction.OnRemove(id))

        viewModel.onAction(WatchlistHomeAction.OnUndo(id))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenFavoritingFails_whenTheWindowPasses_thenTheMovieIsNotRemovedFromTheWatchlistAndTheErrorShows() = runTest {
        user.fail(UserRemoteFunction.AddMovieToFavorites, DataError.Network.NoInternet)
        val viewModel = viewModel()

        viewModel.onAction(WatchlistHomeAction.OnMoveToFavorites(ids.first()))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.removalError).isEqualTo(UiError.NoInternet)
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)

        viewModel.onAction(WatchlistHomeAction.OnDismissRemovalError)
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAFailedRemoval_whenRetrying_thenItIsSentAgain() = runTest {
        user.fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.ServerError)
        val viewModel = viewModel()
        viewModel.onAction(WatchlistHomeAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        user.recoverAll()
        viewModel.onAction(WatchlistHomeAction.OnRetryRemoval)
        advanceTimeBy(1)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(2)
    }

    @Test
    fun givenPendingDeletionsAndFavorites_whenTheScreenIsLeft_thenTheSchedulerGetsBothGroups() {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistHomeAction.OnRemove(ids[0]))
        viewModel.onAction(WatchlistHomeAction.OnMoveToFavorites(ids[1]))

        viewModel.clearForTest()

        assertThat(scheduler.watchlistSyncs)
            .containsExactly(FakeMovieSyncScheduler.WatchlistSync(deleted = listOf(ids[0]), favorites = listOf(ids[1])))
    }

    @Test
    fun givenNothingPendingOrAlreadyConfirmed_whenTheScreenIsLeft_thenTheSchedulerIsNotCalled() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistHomeAction.OnRemove(ids[0]))
        pastTheUndoWindow()

        viewModel.clearForTest()

        assertThat(scheduler.watchlistSyncs).isEmpty()
    }

    @Test
    fun givenMovieAndListsClicks_whenDispatched_thenEachEventIsSentOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(WatchlistHomeAction.OnMovieClick(3))
            viewModel.onAction(WatchlistHomeAction.OnListsClick)

            assertThat(awaitItem()).isEqualTo(WatchlistHomeEvent.NavigateToDetails(3))
            assertThat(awaitItem()).isEqualTo(WatchlistHomeEvent.NavigateToLists)
            expectNoEvents()
        }
    }

    @Test
    fun givenTheFirstTimeTheScreenIsShown_whenReportingIt_thenNothingIsReloaded() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WatchlistHomeAction.OnScreenShown)

        assertThat(user.callCount(UserRemoteFunction.GetAccountWatchlistMovies)).isEqualTo(1)
    }

    @Test
    fun givenTheScreenCameBack_whenReportingItAgain_thenTheListReloadsFromTheFirstPage() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistHomeAction.OnScreenShown)

        viewModel.onAction(WatchlistHomeAction.OnScreenShown)

        assertThat(user.callCount(UserRemoteFunction.GetAccountWatchlistMovies)).isEqualTo(2)
    }
}
