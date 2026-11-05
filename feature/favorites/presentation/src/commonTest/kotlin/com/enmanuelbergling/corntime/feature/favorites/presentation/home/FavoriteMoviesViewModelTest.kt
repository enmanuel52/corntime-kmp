package com.enmanuelbergling.corntime.feature.favorites.presentation.home

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
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.sync.FakeMovieSyncScheduler
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.core.testing.util.clearForTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FavoriteMoviesViewModelTest : MainDispatcherTest() {
    private val user = FakeUserRemoteDataSource()
    private val scheduler = FakeMovieSyncScheduler()
    private val window = RemovalTracker.DEFAULT_UNDO_WINDOW.inWholeMilliseconds
    private val ids get() = FakeMovieData.MOVIES.indices.map { 1_000 + it }

    private fun viewModel() = FavoriteMoviesViewModel(user, scheduler)

    private fun TestScope.pastTheUndoWindow() = advanceTimeBy(window + 1)

    @Test
    fun givenNewViewModel_whenCreated_thenTheFirstPageOfFavoritesIsLoaded() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(UserRemoteFunction.GetAccountFavoriteMovies)).isEqualTo(1)
    }

    @Test
    fun givenNoFavorites_whenLoaded_thenTheListIsEmpty() {
        user.totalPages = 0

        assertThat(viewModel().state.value.isEmpty).isTrue()
    }

    @Test
    fun givenLoadingFails_whenCreated_thenTheErrorIsShownAndRetryRecovers() = runTest {
        user.fail(UserRemoteFunction.GetAccountFavoriteMovies, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.NoInternet)

        user.recoverAll()
        viewModel.onAction(FavoriteMoviesAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.uiError).isNull()
        assertThat(viewModel.state.value.movies.size).isEqualTo(ids.size)
    }

    @Test
    fun givenAMovie_whenRemoved_thenItDisappearsAtOnceAndCanBeUndone() = runTest {
        val viewModel = viewModel()
        val movieId = ids.first()

        viewModel.onAction(FavoriteMoviesAction.OnRemove(movieId))

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids - movieId)
        assertThat(viewModel.state.value.undoable).isEqualTo(PendingRemoval(movieId, RemovalKind.Remove))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromFavorites)).isEqualTo(0)

        viewModel.onAction(FavoriteMoviesAction.OnUndoRemove(movieId))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromFavorites)).isEqualTo(0)
    }

    @Test
    fun givenARemoval_whenTheUndoWindowPasses_thenTheMovieIsRemovedOnTmdbAndStaysHidden() = runTest {
        val viewModel = viewModel()
        val movieId = ids.first()

        viewModel.onAction(FavoriteMoviesAction.OnRemove(movieId))
        pastTheUndoWindow()

        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromFavorites).map { it.single() }).containsExactly(movieId)
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids - movieId)
        assertThat(viewModel.state.value.undoable).isNull()
    }

    @Test
    fun givenRemovingFails_whenTheWindowPasses_thenTheMovieStaysHiddenWithAnErrorUntilDismissedOrRetried() = runTest {
        user.fail(UserRemoteFunction.RemoveMovieFromFavorites, DataError.Network.NoInternet)
        val viewModel = viewModel()
        val movieId = ids.first()

        viewModel.onAction(FavoriteMoviesAction.OnRemove(movieId))
        pastTheUndoWindow()

        assertThat(viewModel.state.value.removalError).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids - movieId)

        viewModel.onAction(FavoriteMoviesAction.OnDismissRemovalError)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(viewModel.state.value.movies.map { it.id }).isEqualTo(ids)
    }

    @Test
    fun givenAFailedRemoval_whenRetryingAfterRecovering_thenItIsSent() = runTest {
        user.fail(UserRemoteFunction.RemoveMovieFromFavorites, DataError.Network.ServerError)
        val viewModel = viewModel()
        viewModel.onAction(FavoriteMoviesAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        user.recoverAll()
        viewModel.onAction(FavoriteMoviesAction.OnRetryRemoval)
        advanceTimeBy(1)

        assertThat(viewModel.state.value.removalError).isNull()
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromFavorites)).isEqualTo(2)
    }

    @Test
    fun givenAllMoviesRemoved_whenTheListEnds_thenItIsEmpty() = runTest {
        user.totalPages = 1
        val viewModel = viewModel()

        ids.forEach { viewModel.onAction(FavoriteMoviesAction.OnRemove(it)) }

        assertThat(viewModel.state.value.movies).isEmpty()
        assertThat(viewModel.state.value.isEmpty).isTrue()
    }

    @Test
    fun givenAPendingRemoval_whenTheScreenIsLeft_thenItIsHandedToTheBackgroundScheduler() {
        val viewModel = viewModel()
        viewModel.onAction(FavoriteMoviesAction.OnRemove(ids.first()))

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
        viewModel.onAction(FavoriteMoviesAction.OnRemove(ids.first()))
        pastTheUndoWindow()

        viewModel.clearForTest()

        assertThat(scheduler.favoriteDeletions).isEmpty()
    }

    @Test
    fun givenAMovieAndBack_whenClicked_thenEachEventIsSentOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(FavoriteMoviesAction.OnMovieClick(5))
            viewModel.onAction(FavoriteMoviesAction.OnBack)

            assertThat(awaitItem()).isEqualTo(FavoriteMoviesEvent.NavigateToDetails(5))
            assertThat(awaitItem()).isEqualTo(FavoriteMoviesEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
