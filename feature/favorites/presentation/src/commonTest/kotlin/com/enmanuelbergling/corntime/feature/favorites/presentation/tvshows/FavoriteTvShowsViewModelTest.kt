package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FavoriteTvShowsViewModelTest : MainDispatcherTest() {
    private val user = FakeTvRemoteDataSource()
    private val scheduler = FakeTvShowSyncScheduler()
    private val window = RemovalTracker.DEFAULT_UNDO_WINDOW.inWholeMilliseconds
    private val ids get() = FakeTvData.TV_SHOWS.indices.map { 1_000 + it }

    private fun viewModel() = FavoriteTvShowsViewModel(user, scheduler)

    private fun TestScope.pastTheUndoWindow() = advanceTimeBy(window + 1)

    @Test
    fun givenNewViewModel_whenCreated_thenTheFirstPageOfFavoritesIsLoaded() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(TvRemoteFunction.GetAccountFavoriteTv)).isEqualTo(1)
    }

    @Test
    fun givenNoFavorites_whenLoaded_thenTheListIsEmpty() {
        user.totalPages = 0

        assertThat(viewModel().state.value.isEmpty).isTrue()
    }

    @Test
    fun givenLoadingFails_whenCreated_thenTheErrorIsShownAndRetryRecovers() = runTest {
        user.fail(TvRemoteFunction.GetAccountFavoriteTv, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.NoInternet)

        user.recoverAll()
        viewModel.onAction(FavoriteTvShowsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.uiError).isNull()
        assertThat(viewModel.state.value.tvShows.size).isEqualTo(ids.size)
    }

    @Test
    fun givenATvShow_whenRemoved_thenItDisappearsAtOnceAndCanBeUndone() = runTest {
        val viewModel = viewModel()
        val tvShowId = ids.first()

        viewModel.onAction(FavoriteTvShowsAction.OnRemove(tvShowId))

        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids - tvShowId)
        assertThat(viewModel.state.value.undoable).isEqualTo(PendingRemoval(tvShowId, RemovalKind.Remove))
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromFavorites)).isEqualTo(0)

        viewModel.onAction(FavoriteTvShowsAction.OnUndoRemove(tvShowId))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromFavorites)).isEqualTo(0)
    }

    @Test
    fun givenARemoval_whenTheUndoWindowPasses_thenTheTvShowIsRemovedOnTmdbAndStaysHidden() = runTest {
        val viewModel = viewModel()
        val tvShowId = ids.first()

        viewModel.onAction(FavoriteTvShowsAction.OnRemove(tvShowId))
        pastTheUndoWindow()

        assertThat(user.callsTo(TvRemoteFunction.RemoveTvFromFavorites).map { it.single() }).containsExactly(tvShowId)
        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids - tvShowId)
        assertThat(viewModel.state.value.undoable).isNull()
    }

    @Test
    fun givenRemovingFails_whenTheWindowPasses_thenTheTvShowStaysHiddenWithAnErrorUntilDismissedOrRetried() = runTest {
        user.fail(TvRemoteFunction.RemoveTvFromFavorites, DataError.Network.NoInternet)
        val viewModel = viewModel()
        val tvShowId = ids.first()

        viewModel.onAction(FavoriteTvShowsAction.OnRemove(tvShowId))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.removalError).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids - tvShowId)

        viewModel.onAction(FavoriteTvShowsAction.OnDismissRemovalError)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(viewModel.state.value.tvShows.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAFailedRemoval_whenRetryingAfterRecovering_thenItIsSent() = runTest {
        user.fail(TvRemoteFunction.RemoveTvFromFavorites, DataError.Network.ServerError)
        val viewModel = viewModel()
        viewModel.onAction(FavoriteTvShowsAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        user.recoverAll()
        viewModel.onAction(FavoriteTvShowsAction.OnRetryRemoval)
        advanceTimeBy(1)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(user.callCount(TvRemoteFunction.RemoveTvFromFavorites)).isEqualTo(2)
    }

    @Test
    fun givenAllTvShowsRemoved_whenTheListEnds_thenItIsEmpty() = runTest {
        user.totalPages = 1
        val viewModel = viewModel()

        ids.forEach { viewModel.onAction(FavoriteTvShowsAction.OnRemove(it)) }

        assertThat(viewModel.state.value.tvShows).isEmpty()
        assertThat(viewModel.state.value.isEmpty).isTrue()
    }

    @Test
    fun givenAPendingRemoval_whenTheScreenIsLeft_thenItIsHandedToTheBackgroundScheduler() {
        val viewModel = viewModel()
        viewModel.onAction(FavoriteTvShowsAction.OnRemove(ids.first()))

        viewModel.clearForTest()

        assertThat(scheduler.favoriteDeletions).containsExactly(listOf(ids.first()))
    }

    @Test
    fun givenNothingPending_whenTheScreenIsLeft_thenTheSchedulerIsNotCalled() {
        val viewModel = viewModel()

        viewModel.clearForTest()

        assertThat(scheduler.favoriteDeletions).isEmpty()
    }

    @Test
    fun givenAConfirmedRemoval_whenTheScreenIsLeft_thenItIsNotSentAgain() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(FavoriteTvShowsAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        viewModel.clearForTest()

        assertThat(scheduler.favoriteDeletions).isEmpty()
    }

    @Test
    fun givenATvShowAndBack_whenClicked_thenEachEventIsSentOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(FavoriteTvShowsAction.OnTvShowClick(5))
            viewModel.onAction(FavoriteTvShowsAction.OnBack)

            assertThat(awaitItem()).isEqualTo(FavoriteTvShowsEvent.NavigateToDetails(5))
            assertThat(awaitItem()).isEqualTo(FavoriteTvShowsEvent.NavigateBack)
            expectNoEvents()
        }
    }

    @Test
    fun givenTheFirstTimeTheScreenIsShown_whenReportingIt_thenNothingIsReloaded() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(FavoriteTvShowsAction.OnScreenShown)

        assertThat(user.callCount(TvRemoteFunction.GetAccountFavoriteTv)).isEqualTo(1)
    }

    @Test
    fun givenTheScreenCameBack_whenReportingItAgain_thenTheListReloadsFromTheFirstPage() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(FavoriteTvShowsAction.OnScreenShown)

        viewModel.onAction(FavoriteTvShowsAction.OnScreenShown)

        assertThat(user.callCount(TvRemoteFunction.GetAccountFavoriteTv)).isEqualTo(2)
    }
}
