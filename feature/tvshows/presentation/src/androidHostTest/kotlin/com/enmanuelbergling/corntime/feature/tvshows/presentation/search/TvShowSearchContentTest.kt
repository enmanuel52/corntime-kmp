package com.enmanuelbergling.corntime.feature.tvshows.presentation.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class TvShowSearchContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<TvShowSearchAction>()
    private val tvShows = FakeTvData.TV_SHOWS

    private fun show(
        query: String = "wire",
        items: List<TvShow> = tvShows,
        status: PagingStatus = PagingStatus.Idle,
    ) {
        rule.setThemedContent {
            TvShowSearchContent(
                state = TvShowSearchState(query, PagingState(items = items, status = status)),
                onAction = { actions += it },
            )
        }
        rule.mainClock.advanceTimeBy(1_000)
    }

    private fun tag(tag: String) = rule.onNodeWithTag(tag)

    @Test
    fun givenResults_whenShown_thenTvShowTitlesAreListed() {
        show()

        rule.onNodeWithText(tvShows[1].name, substring = true).assertIsDisplayed()
        tag(TvShowSearchTags.RESULTS).assertIsDisplayed()
    }

    @Test
    fun givenFinishedSearchWithoutMatches_whenShown_thenNoResultsMessage() {
        show(items = emptyList(), status = PagingStatus.EndReached)

        tag(TvShowSearchTags.NO_RESULTS).assertIsDisplayed()
        rule.onNodeWithText("No TV shows found").assertIsDisplayed()
    }

    @Test
    fun givenBlankQueryWithoutResults_whenShown_thenNeitherNoResultsNorLoading() {
        show(query = "", items = emptyList(), status = PagingStatus.EndReached)

        tag(TvShowSearchTags.NO_RESULTS).assertDoesNotExist()
        tag(TvShowSearchTags.LOADING).assertDoesNotExist()
    }

    @Test
    fun givenFirstPageLoadingForAQuery_whenShown_thenLoadingIndicator() {
        show(items = emptyList(), status = PagingStatus.LoadingFirstPage)

        tag(TvShowSearchTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenMessageAndRetry() {
        show(items = emptyList(), status = PagingStatus.Failed(DataError.Network.RequestTimeout))

        rule.onNodeWithText("Waiting time for response has ended", substring = true).assertIsDisplayed()
        tag(TvShowSearchTags.RETRY).performClick()

        assertThat(actions).containsExactly(TvShowSearchAction.OnLoadNextPage)
    }

    @Test
    fun givenLaterPageFailed_whenShown_thenResultsStayAndRetryIsOfferedWithoutAutoRetrying() {
        show(status = PagingStatus.Failed(DataError.Network.ServerError))

        rule.onNodeWithText(tvShows[1].name, substring = true).assertIsDisplayed()
        assertThat(actions).isEmpty()
        tag(TvShowSearchTags.RETRY).performClick()

        assertThat(actions).containsExactly(TvShowSearchAction.OnLoadNextPage)
    }

    @Test
    fun givenResult_whenTapped_thenDispatchesTvShowClick() {
        show()

        rule.onNodeWithText(tvShows[1].name, substring = true).performClick()

        assertThat(actions).contains(TvShowSearchAction.OnTvShowClick(tvShows[1].id))
    }
}
