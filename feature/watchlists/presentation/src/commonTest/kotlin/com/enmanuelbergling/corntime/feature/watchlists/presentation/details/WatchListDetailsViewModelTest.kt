package com.enmanuelbergling.corntime.feature.watchlists.presentation.details

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
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

class WatchListDetailsViewModelTest : MainDispatcherTest() {
    private val user = FakeUserRemoteDataSource()
    private val scheduler = FakeMovieSyncScheduler()
    private val window = RemovalTracker.DEFAULT_UNDO_WINDOW.inWholeMilliseconds
    private val listId = 12
    private val ids get() = FakeMovieData.MOVIES.indices.map { 1_000 + it }

    private fun viewModel() = WatchListDetailsViewModel(listId, user, MoveMovieToFavoritesUseCase(user), scheduler)

    private fun TestScope.pastTheUndoWindow() = advanceTimeBy(window + 1)

    @Test
    fun givenNewViewModel_whenCreated_thenTheMoviesOfThatListAreLoaded() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
        assertThat(user.callsTo(UserRemoteFunction.GetWatchListMovies)).containsExactly(listOf<Any?>(listId, 1))
    }

    @Test
    fun givenAnEmptyList_whenLoaded_thenItIsEmpty() {
        user.totalPages = 0

        assertThat(viewModel().state.value.isEmpty).isTrue()
    }

    @Test
    fun givenAMovie_whenRemoved_thenItLeavesThatListAfterTheWindow() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchListDetailsAction.OnRemove(id))
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids - id)
        pastTheUndoWindow()

        assertThat(user.callsTo(UserRemoteFunction.DeleteMovieFromList)).containsExactly(listOf<Any?>(id, listId))
    }

    @Test
    fun givenAMovie_whenMovedToFavorites_thenItIsFavoritedAndRemovedFromThatListNotTheWatchlist() = runTest {
        val viewModel = viewModel()
        val id = ids.first()

        viewModel.onAction(WatchListDetailsAction.OnMoveToFavorites(id))
        pastTheUndoWindow()

        assertThat(user.callsTo(UserRemoteFunction.AddMovieToFavorites).map { it.single() }).containsExactly(id)
        assertThat(user.callsTo(UserRemoteFunction.DeleteMovieFromList)).containsExactly(listOf<Any?>(id, listId))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenAnUndo_whenTheWindowPasses_thenNothingIsSent() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WatchListDetailsAction.OnRemove(ids.first()))

        viewModel.onAction(WatchListDetailsAction.OnUndo(ids.first()))
        pastTheUndoWindow()

        assertThat(user.callCount(UserRemoteFunction.DeleteMovieFromList)).isEqualTo(0)
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenRemovingFails_whenTheWindowPasses_thenTheErrorIsShownUntilDismissed() = runTest {
        user.fail(UserRemoteFunction.DeleteMovieFromList, DataError.Network.RequestTimeout)
        val viewModel = viewModel()

        viewModel.onAction(WatchListDetailsAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.removalError).isEqualTo(UiError.Timeout)
        viewModel.onAction(WatchListDetailsAction.OnDismissRemovalError)
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenPendingChanges_whenTheScreenIsLeft_thenTheSchedulerGetsThemForThatList() {
        val viewModel = viewModel()
        viewModel.onAction(WatchListDetailsAction.OnRemove(ids[0]))
        viewModel.onAction(WatchListDetailsAction.OnMoveToFavorites(ids[1]))

        viewModel.clearForTest()

        assertThat(scheduler.listSyncs).containsExactly(
            FakeMovieSyncScheduler.ListSync(listId, deleted = listOf(ids[0]), favorites = listOf(ids[1])),
        )
        assertThat(scheduler.watchlistSyncs).isEmpty()
    }

    @Test
    fun givenNothingPending_whenTheScreenIsLeft_thenTheSchedulerIsNotCalled() {
        viewModel().clearForTest()

        assertThat(scheduler.listSyncs).isEmpty()
    }

    @Test
    fun givenShortcutAndNavigationActions_whenDispatched_thenEachEventCarriesTheListOrMovie() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(WatchListDetailsAction.OnAddShortcut)
            viewModel.onAction(WatchListDetailsAction.OnRemoveShortcut)
            viewModel.onAction(WatchListDetailsAction.OnMovieClick(5))
            viewModel.onAction(WatchListDetailsAction.OnBack)

            assertThat(awaitItem()).isEqualTo(WatchListDetailsEvent.AddShortcut(listId))
            assertThat(awaitItem()).isEqualTo(WatchListDetailsEvent.RemoveShortcut(listId))
            assertThat(awaitItem()).isEqualTo(WatchListDetailsEvent.NavigateToDetails(5))
            assertThat(awaitItem()).isEqualTo(WatchListDetailsEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
