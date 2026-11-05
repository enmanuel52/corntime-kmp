package com.enmanuelbergling.corntime.feature.tvshows.presentation.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.setThemedContent

class TvShowDetailsRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<TvShowDetailsAction>()

    fun show(state: TvShowDetailsState) = apply {
        rule.setThemedContent { TvShowDetailsScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(1_000)
    }

    fun scrollContentTo(text: String) = apply {
        rule.onNodeWithTag(TvShowDetailsTags.CONTENT).performScrollToNode(hasText(text))
        rule.mainClock.advanceTimeBy(1_000)
    }

    fun assertTextVisible(text: String, substring: Boolean = false) =
        apply { rule.onNodeWithText(text, substring = substring).assertIsDisplayed() }

    fun assertTextAbsent(text: String, substring: Boolean = false) =
        apply { rule.onNodeWithText(text, substring = substring).assertDoesNotExist() }

    fun assertFavoriteButton(visible: Boolean) = apply { assertTag(TvShowDetailsTags.FAVORITE, visible) }

    fun assertWatchlistButton(visible: Boolean) = apply { assertTag(TvShowDetailsTags.WATCHLIST, visible) }

    fun assertFavoriteLoading(visible: Boolean) = apply { assertTag(TvShowDetailsTags.FAVORITE_LOADING, visible) }

    fun assertWatchlistLoading(visible: Boolean) = apply { assertTag(TvShowDetailsTags.WATCHLIST_LOADING, visible) }

    fun assertContentVisible(visible: Boolean) = apply { assertTag(TvShowDetailsTags.CONTENT, visible) }

    fun clickFavorite() = apply { rule.onNodeWithTag(TvShowDetailsTags.FAVORITE).performClick() }

    fun clickWatchlist() = apply { rule.onNodeWithTag(TvShowDetailsTags.WATCHLIST).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }

    /** The "see all" card closes the seasons strip, so it may need the strip scrolled first. */
    fun scrollSeasonsToSeeAll() = apply {
        rule.onNodeWithTag(TvShowDetailsTags.SEASONS)
            .performScrollToNode(hasTestTag(TvShowDetailsTags.SEE_ALL_SEASONS))
        rule.mainClock.advanceTimeBy(1_000)
    }

    fun clickSeeAllSeasons() = apply { rule.onNodeWithTag(TvShowDetailsTags.SEE_ALL_SEASONS).performClick() }

    fun clickSeasonPosterAt(index: Int) = apply {
        rule.onAllNodesWithContentDescription("season poster")[index].performClick()
    }

    /** Person cards (cast first, then crew) are clickable on their poster, not on the name. */
    fun clickPersonCardAt(index: Int) = apply {
        rule.onAllNodesWithContentDescription("movie image")[index].performClick()
    }

    fun clickText(text: String) = apply { rule.onNodeWithText(text).performClick() }

    private fun assertTag(tag: String, visible: Boolean) {
        if (visible) rule.onNodeWithTag(tag).assertIsDisplayed() else rule.onNodeWithTag(tag).assertDoesNotExist()
    }
}
