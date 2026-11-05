package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.enmanuelbergling.corntime.feature.watchlists.presentation.WatchlistTags
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsState
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class WatchlistHomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val movies: List<Movie> =
        FakeMovieData.MOVIES.mapIndexed { index, movie -> movie.copy(id = index + 1, title = "Movie ${'A' + index}") }
    private val allActions = mutableListOf<WatchlistHomeAction>()
    private var backs = 0

    private val actions get() = allActions.filter { it != WatchlistHomeAction.OnLoadNextPage }
    private val pagingRequests get() = allActions.count { it == WatchlistHomeAction.OnLoadNextPage }

    private fun show(
        items: List<Movie> = movies,
        status: PagingStatus = PagingStatus.Idle,
        removals: RemovalState = RemovalState(),
    ) = rule.setThemedContent {
        WatchlistHomeScreen(
            selectedTab = WatchlistTab.Movies,
            onTabSelected = {},
            moviesState = WatchlistHomeState(PagingState(items = items, status = status), removals),
            onMoviesAction = { allActions += it },
            tvShowsState = WatchlistTvShowsState(),
            onTvShowsAction = {},
            onBack = { backs++ },
        )
    }

    private fun ComposeContentTestRule.text(text: String) = onNodeWithText(text, substring = true)

    @Test
    fun givenLoadedMovies_whenShown_thenTheirTitlesAreVisible() {
        show()

        rule.text("Movie A").assertIsDisplayed()
        rule.text("Your watchlist is empty").assertDoesNotExist()
    }

    @Test
    fun givenAnEmptyWatchlist_whenShown_thenTheEmptyMessageIsShown() {
        show(items = emptyList(), status = PagingStatus.EndReached)

        rule.text("Your watchlist is empty").assertIsDisplayed()
    }

    @Test
    fun givenEverythingRemoved_whenTheListEnds_thenTheEmptyMessageIsShown() {
        val hidden = movies.map { it.id }.toSet()

        show(status = PagingStatus.EndReached, removals = RemovalState(completed = hidden))

        rule.text("Your watchlist is empty").assertIsDisplayed()
    }

    @Test
    fun givenARemovedMovie_whenShown_thenItIsNotInTheList() {
        show(removals = RemovalState(completed = setOf(movies.first().id)))

        rule.text("Movie A").assertDoesNotExist()
        rule.text("Movie B").assertIsDisplayed()
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenRetryRequestsThePageAgain() {
        show(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet))

        rule.onNodeWithText("Retry").performClick()

        assertThat(pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenALaterPageFailed_whenShown_thenAFooterRetryIsOfferedAndNoAutomaticRequestIsMade() {
        show(status = PagingStatus.Failed(DataError.Network.RequestTimeout))

        assertThat(pagingRequests).isEqualTo(0)
        rule.onNodeWithTag(WatchlistTags.RETRY_FOOTER).assertIsDisplayed().performClick()
        assertThat(pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenAShortList_whenShown_thenTheNextPageIsRequested() {
        show()

        assertThat(pagingRequests).isGreaterThan(0)
    }

    @Test
    fun givenAPendingRemoval_whenShown_thenTheUndoSnackbarSaysRemovedAndUndoSendsTheAction() {
        val removed = PendingRemoval(movies.first().id, RemovalKind.Remove)

        show(removals = RemovalState(pending = listOf(removed)))

        rule.text("Movie removed from watchlist").assertIsDisplayed()
        rule.onNodeWithText("Undo").performClick()
        assertThat(actions).containsExactly(WatchlistHomeAction.OnUndo(removed.id))
    }

    @Test
    fun givenAMoveToFavorites_whenShown_thenTheSnackbarSaysAddedToFavorites() {
        show(removals = RemovalState(pending = listOf(PendingRemoval(movies.first().id, RemovalKind.MoveToFavorites))))

        rule.text("Movie added to favorites").assertIsDisplayed()
    }

    @Test
    fun givenAFailedRemoval_whenShown_thenADialogOffersRetryAndDismissSendsTheAction() {
        val failed = FailedRemoval(PendingRemoval(movies.first().id, RemovalKind.Remove), UiError.NoInternet)

        show(removals = RemovalState(failed = listOf(failed)))
        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(WatchlistHomeAction.OnDismissRemovalError)
    }

    @Test
    fun givenMovies_whenTappingAPosterTheListsIconAndBack_thenEachSendsItsActionOrCallback() {
        show()

        rule.onAllNodesWithContentDescription("movie image")[0].performClick()
        rule.onNodeWithContentDescription("lists icon").performClick()
        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions).containsExactly(
            WatchlistHomeAction.OnMovieClick(movies.first().id),
            WatchlistHomeAction.OnListsClick,
        )
        assertThat(backs).isEqualTo(1)
    }
}
