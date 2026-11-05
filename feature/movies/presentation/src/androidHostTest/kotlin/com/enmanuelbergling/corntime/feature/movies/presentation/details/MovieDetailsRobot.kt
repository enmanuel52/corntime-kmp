package com.enmanuelbergling.corntime.feature.movies.presentation.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent

class MovieDetailsRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<MovieDetailsAction>()

    fun show(state: MovieDetailsState) = apply {
        rule.setThemedContent { MovieDetailsScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(1_000)
    }

    fun assertTextVisible(text: String, substring: Boolean = false) =
        apply { rule.onNodeWithText(text, substring = substring).assertIsDisplayed() }

    fun assertTextAbsent(text: String, substring: Boolean = false) =
        apply { rule.onNodeWithText(text, substring = substring).assertDoesNotExist() }

    fun assertFavoriteButton(visible: Boolean) = apply { assertTag(MovieDetailsTags.FAVORITE, visible) }

    fun assertWatchlistButton(visible: Boolean) = apply { assertTag(MovieDetailsTags.WATCHLIST, visible) }

    fun assertFavoriteLoading(visible: Boolean) = apply { assertTag(MovieDetailsTags.FAVORITE_LOADING, visible) }

    fun assertWatchlistLoading(visible: Boolean) = apply { assertTag(MovieDetailsTags.WATCHLIST_LOADING, visible) }

    fun assertContentVisible(visible: Boolean) = apply { assertTag(MovieDetailsTags.CONTENT, visible) }

    fun clickFavorite() = apply { rule.onNodeWithTag(MovieDetailsTags.FAVORITE).performClick() }

    fun clickWatchlist() = apply { rule.onNodeWithTag(MovieDetailsTags.WATCHLIST).performClick() }

    fun assertRatingSummary(visible: Boolean) = apply { assertTag(MovieDetailsTags.RATING, visible) }

    fun clickRatingSummary() = apply { rule.onNodeWithTag(MovieDetailsTags.RATING).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }

    /** Person cards (cast first, then crew) are clickable on their poster, not on the name. */
    fun clickPersonCardAt(index: Int) = apply {
        rule.onAllNodesWithContentDescription("movie image")[index].performClick()
    }

    fun clickText(text: String) = apply { rule.onNodeWithText(text).performClick() }

    private fun assertTag(tag: String, visible: Boolean) {
        if (visible) rule.onNodeWithTag(tag).assertIsDisplayed() else rule.onNodeWithTag(tag).assertDoesNotExist()
    }
}
