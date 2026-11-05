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
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.presentation.removal.FailedRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.watchlists.presentation.WatchlistTags
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsAction
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
class WatchlistTvShowsTabTest {

    @get:Rule
    val rule = createComposeRule()

    private val tvShows: List<TvShow> =
        FakeTvData.TV_SHOWS.mapIndexed { index, show -> show.copy(id = index + 1, name = "Show ${'A' + index}") }
    private val allTvActions = mutableListOf<WatchlistTvShowsAction>()
    private val moviesActions = mutableListOf<WatchlistHomeAction>()
    private val tabs = mutableListOf<WatchlistTab>()

    private val actions get() = allTvActions.filter { it != WatchlistTvShowsAction.OnLoadNextPage }
    private val pagingRequests get() = allTvActions.count { it == WatchlistTvShowsAction.OnLoadNextPage }

    private fun show(
        selected: WatchlistTab = WatchlistTab.TvShows,
        items: List<TvShow> = tvShows,
        status: PagingStatus = PagingStatus.Idle,
        removals: RemovalState = RemovalState(),
    ) = rule.setThemedContent {
        WatchlistHomeScreen(
            selectedTab = selected,
            onTabSelected = { tabs += it },
            moviesState = WatchlistHomeState(),
            onMoviesAction = { moviesActions += it },
            tvShowsState = WatchlistTvShowsState(PagingState(items = items, status = status), removals),
            onTvShowsAction = { allTvActions += it },
            onBack = {},
        )
    }

    private fun ComposeContentTestRule.text(text: String) = onNodeWithText(text, substring = true)

    @Test
    fun givenTheTvTab_whenShown_thenTvShowTitlesAreVisible() {
        show()

        rule.text("Show A").assertIsDisplayed()
        rule.text("Your TV shows watchlist is empty").assertDoesNotExist()
    }

    @Test
    fun givenTheMoviesTab_whenShown_thenTvShowsAreNotListed() {
        show(selected = WatchlistTab.Movies)

        rule.text("Show A").assertDoesNotExist()
    }

    @Test
    fun givenAnEmptyTvWatchlist_whenShown_thenTheEmptyMessageIsShown() {
        show(items = emptyList(), status = PagingStatus.EndReached)

        rule.text("Your TV shows watchlist is empty").assertIsDisplayed()
    }

    @Test
    fun givenARemovedTvShow_whenShown_thenItIsNotInTheList() {
        show(removals = RemovalState(completed = setOf(tvShows.first().id)))

        rule.text("Show A").assertDoesNotExist()
        rule.text("Show B").assertIsDisplayed()
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
        val removed = PendingRemoval(tvShows.first().id, RemovalKind.Remove)

        show(removals = RemovalState(pending = listOf(removed)))

        rule.text("TV show removed from watchlist").assertIsDisplayed()
        rule.onNodeWithText("Undo").performClick()
        assertThat(actions).containsExactly(WatchlistTvShowsAction.OnUndo(removed.id))
    }

    @Test
    fun givenAMoveToFavorites_whenShown_thenTheSnackbarSaysAddedToFavorites() {
        show(removals = RemovalState(pending = listOf(PendingRemoval(tvShows.first().id, RemovalKind.MoveToFavorites))))

        rule.text("TV show added to favorites").assertIsDisplayed()
    }

    @Test
    fun givenAFailedRemoval_whenShown_thenADialogOffersRetryAndDismissSendsTheAction() {
        val failed = FailedRemoval(PendingRemoval(tvShows.first().id, RemovalKind.Remove), UiError.NoInternet)

        show(removals = RemovalState(failed = listOf(failed)))
        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(WatchlistTvShowsAction.OnDismissRemovalError)
    }

    @Test
    fun givenATvShow_whenTappingItsCard_thenTheClickIsSent() {
        show()

        rule.onAllNodesWithContentDescription("tv show image")[0].performClick()

        assertThat(actions).containsExactly(WatchlistTvShowsAction.OnTvShowClick(tvShows.first().id))
    }

    @Test
    fun givenTheTvTab_whenTappingTheListsIcon_thenTheMoviesViewModelNavigatesToLists() {
        show()

        rule.onNodeWithContentDescription("lists icon").performClick()

        assertThat(moviesActions).containsExactly(WatchlistHomeAction.OnListsClick)
        assertThat(actions).isEmpty()
    }

    @Test
    fun givenTheTvTab_whenTappingTheMoviesSegment_thenTheTabSwitchIsRequested() {
        show()

        rule.onNodeWithTag(WatchlistHomeTags.tab(WatchlistTab.Movies)).performClick()

        assertThat(tabs).containsExactly(WatchlistTab.Movies)
    }
}
