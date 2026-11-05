package com.enmanuelbergling.corntime.feature.favorites.presentation.home

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.presentation.removal.FailedRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.favorites.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.favorites.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class FavoriteMoviesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { FavoriteMoviesRobot(rule) }
    private val movies: List<Movie> = FakeMovieData.MOVIES.mapIndexed { index, movie -> movie.copy(id = index + 1, title = "Movie ${'A' + index}") }

    private fun state(
        items: List<Movie> = movies,
        status: PagingStatus = PagingStatus.Idle,
        removals: RemovalState = RemovalState(),
    ) = FavoriteMoviesState(PagingState(items = items, status = status), removals)

    @Test
    fun givenLoadedFavorites_whenShown_thenTheirTitlesAreVisible() {
        robot.show(state()).assertTextVisible(movies.first().title).assertTextAbsent("No favorites yet")
    }

    @Test
    fun givenNoFavorites_whenLoaded_thenTheEmptyMessageIsShown() {
        robot.show(state(items = emptyList(), status = PagingStatus.EndReached)).assertTextVisible("No favorites yet")
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenAnErrorScreenWithRetry() {
        robot.show(state(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet)))
            .assertTextVisible("Retry")
            .clickRetry()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenALaterPageFailed_whenShown_thenMoviesStayWithAFooterRetry() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.RequestTimeout)))
            .assertTextVisible(movies.first().title)
            .assertRetryFooterVisible()
            .clickRetryFooter()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenAShortList_whenShown_thenTheNextPageIsRequestedAutomatically() {
        robot.show(state())

        assertThat(robot.pagingRequests).isGreaterThan(0)
    }

    @Test
    fun givenAFailedPage_whenShown_thenNoAutomaticRequestUntilTheUserRetries() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.NoInternet)))

        assertThat(robot.pagingRequests).isEqualTo(0)
    }

    @Test
    fun givenAMovie_whenTappingItsPoster_thenItIsClicked() {
        robot.show(state()).clickMovieAt(1)

        assertThat(robot.actions).containsExactly(FavoriteMoviesAction.OnMovieClick(movies[1].id))
    }

    @Test
    fun givenAPendingRemoval_whenShown_thenTheUndoSnackbarOffersToTakeItBack() {
        val removed = PendingRemoval(movies.first().id, RemovalKind.Remove)

        robot.show(state(removals = RemovalState(pending = listOf(removed))))
            .assertTextVisible("Movie removed from favorites")
            .clickUndo()

        assertThat(robot.actions).containsExactly(FavoriteMoviesAction.OnUndoRemove(removed.id))
    }

    @Test
    fun givenAFailedRemoval_whenShown_thenADialogOffersRetryOrDismissing() {
        val failed = FailedRemoval(PendingRemoval(movies.first().id, RemovalKind.Remove), UiError.NoInternet)

        robot.show(state(removals = RemovalState(failed = listOf(failed))))
            .clickAccept()

        assertThat(robot.actions).containsExactly(FavoriteMoviesAction.OnDismissRemovalError)
    }

    @Test
    fun givenTheScreen_whenTappingBack_thenBackIsSent() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(FavoriteMoviesAction.OnBack)
    }
}
