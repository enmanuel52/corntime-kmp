package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.favorites.presentation.util.setThemedContent

private const val ANIMATION_SETTLE_MS = 2_000L

/** Encapsulates every interaction with [FavoriteTvShowsScreen] so tests read as intent. */
class FavoriteTvShowsRobot(private val rule: ComposeContentTestRule) {
    private val allActions = mutableListOf<FavoriteTvShowsAction>()

    /** What the user caused; the grid's own near-the-end paging requests are excluded. */
    val actions get() = allActions.filter { it != FavoriteTvShowsAction.OnLoadNextPage }
    val pagingRequests get() = allActions.count { it == FavoriteTvShowsAction.OnLoadNextPage }

    fun show(state: FavoriteTvShowsState) = apply {
        rule.setThemedContent { FavoriteTvShowsScreen(state = state, onAction = { allActions += it }) }
        rule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text, substring = true).assertIsDisplayed() }
    fun assertTextAbsent(text: String) = apply { rule.onNodeWithText(text, substring = true).assertDoesNotExist() }
    fun assertRetryFooterVisible() = apply { rule.onNodeWithTag(FavoriteTvShowsTags.RETRY_FOOTER).assertIsDisplayed() }

    /** Posters are the clickable part of a movie card. */
    fun clickTvShowAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }
    fun clickRetry() = apply { rule.onNodeWithText("Retry").performClick() }
    fun clickRetryFooter() = apply { rule.onNodeWithTag(FavoriteTvShowsTags.RETRY_FOOTER).performClick() }
    fun clickUndo() = apply { rule.onNodeWithText("Undo").performClick() }
    fun clickAccept() = apply { rule.onNodeWithText("Accept").performClick() }
    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
}
