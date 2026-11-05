package com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows

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
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.sync.FakeTvShowSyncScheduler
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.core.testing.util.clearForTest
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.MoveTvShowToFavoritesUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WatchlistTvShowsViewModelTest : MainDispatcherTest() {
    private val user = FakeTvRemoteDataSource()
    private val scheduler = FakeTvShowSyncScheduler()
    private val window = RemovalTracker.DEFAULT_UNDO_WINDOW.inWholeMilliseconds
    private val ids get() = FakeTvData.TV_SHOWS.indices.map { 1_000 + it }

    private fun viewModel() = WatchlistTvShowsViewModel(user, MoveTvShowToFavoritesUseCase(user), scheduler)

    private fun TestScope.pastTheUndoWindow() = advanceTimeBy(window + 1)

    @Test
    fun givenNewViewModel_whenCreated_thenTheFirstPageOfTheWatchlistIsLoaded() {
        assertThat(viewModel().state.value.tvShows.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAnEmptyWatchlist_whenLoaded_thenItIsEmpty() {
        user.totalPages = 0

        assertThat(viewModel().state.value.isEmpty).isTrue()
    }

    @Test
    fun givenLoadingFails_whenCreated_thenAnErrorIsShown() {
        user.fail(TvRemoteFunction.GetAccountWatchlistTv, DataError.Network.Unauthorized)

        assertThat(viewModel().state.value.paging.uiError).isEqualTo(UiError.Unauthorized)
    }

    @Test
    fun givenATvShow_whenRemoved_thenItIsHiddenAtOnceAndOnlyRemovedFromTheWatchlistAfterTheWindow() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchlistTvShowsAction.OnRemove(id))

        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids - id)
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(0)

        pastTheUndoWindow()

        assertThat(user.callsTo(TvRemoteFunction.RemoveTvFromAccountWatchlist).map { it.single() }).containsExactly(id)
        assertThat(user.callCount(TvRemoteFunction.AddTvToFavorites)).isEqualTo(0)
    }

    @Test
    fun givenATvShow_whenMovedToFavorites_thenItIsAddedToFavoritesAndRemovedFromTheWatchlistAfterTheWindow() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchlistTvShowsAction.OnMoveToFavorites(id))
        assertThat(viewModel.state.value.undoable).isEqualTo(PendingRemoval(id, RemovalKind.MoveToFavorites))
        pastTheUndoWindow()

        assertThat(user.callsTo(TvRemoteFunction.AddTvToFavorites).map { it.single() }).containsExactly(id)
        assertThat(user.callsTo(TvRemoteFunction.RemoveTvFromAccountWatchlist).map { it.single() }).containsExactly(id)
    }

    @Test
    fun givenARemovalAndAnUndo_whenTheWindowPasses_thenNothingIsSentAndTheTvShowIsBack() = runTest {
        val viewModel = viewModel()
        val id = ids.first()
        viewModel.onAction(WatchlistTvShowsAction.OnRemove(id))

        viewModel.onAction(WatchlistTvShowsAction.OnUndo(id))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenFavoritingFails_whenTheWindowPasses_thenTheTvShowIsNotRemovedFromTheWatchlistAndTheErrorShows() = runTest {
        user.fail(TvRemoteFunction.AddTvToFavorites, DataError.Network.NoInternet)
        val viewModel = viewModel()

        viewModel.onAction(WatchlistTvShowsAction.OnMoveToFavorites(ids.first()))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.removalError).isEqualTo(UiError.NoInternet)
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(0)

        viewModel.onAction(WatchlistTvShowsAction.OnDismissRemovalError)
        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAFailedRemoval_whenRetrying_thenItIsSentAgain() = runTest {
        user.fail(TvRemoteFunction.RemoveTvFromAccountWatchlist, DataError.Network.ServerError)
        val viewModel = viewModel()
        viewModel.onAction(WatchlistTvShowsAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        user.recoverAll()
        viewModel.onAction(WatchlistTvShowsAction.OnRetryRemoval)
        advanceTimeBy(1)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(2)
    }

    @Test
    fun givenPendingDeletionsAndFavorites_whenTheScreenIsLeft_thenTheSchedulerGetsBothGroups() {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistTvShowsAction.OnRemove(ids[0]))
        viewModel.onAction(WatchlistTvShowsAction.OnMoveToFavorites(ids[1]))

        viewModel.clearForTest()

        assertThat(scheduler.watchlistSyncs)
            .containsExactly(FakeTvShowSyncScheduler.WatchlistSync(deleted = listOf(ids[0]), favorites = listOf(ids[1])))
    }

    @Test
    fun givenNothingPendingOrAlreadyConfirmed_whenTheScreenIsLeft_thenTheSchedulerIsNotCalled() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistTvShowsAction.OnRemove(ids[0]))
        pastTheUndoWindow()

        viewModel.clearForTest()

        assertThat(scheduler.watchlistSyncs).isEmpty()
    }

    @Test
    fun givenTvShowClick_whenDispatched_thenTheEventIsSentOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(WatchlistTvShowsAction.OnTvShowClick(3))

            assertThat(awaitItem()).isEqualTo(WatchlistTvShowsEvent.NavigateToDetails(3))
            expectNoEvents()
        }
    }

    @Test
    fun givenTheFirstTimeTheScreenIsShown_whenReportingIt_thenNothingIsReloaded() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WatchlistTvShowsAction.OnScreenShown)

        assertThat(user.callCount(TvRemoteFunction.GetAccountWatchlistTv)).isEqualTo(1)
    }

    @Test
    fun givenTheScreenCameBack_whenReportingItAgain_thenTheListReloadsFromTheFirstPage() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WatchlistTvShowsAction.OnScreenShown)

        viewModel.onAction(WatchlistTvShowsAction.OnScreenShown)

        assertThat(user.callCount(TvRemoteFunction.GetAccountWatchlistTv)).isEqualTo(2)
    }
}
