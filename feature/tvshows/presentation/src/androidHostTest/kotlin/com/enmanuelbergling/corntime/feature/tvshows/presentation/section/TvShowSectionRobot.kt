package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.setThemedContent

class TvShowSectionRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<TvShowSectionAction>()

    fun show(state: TvShowSectionState) = apply {
        rule.setThemedContent { TvShowSectionScreen(state = state, onAction = { actions += it }) }
        // list items animate in (scale/rotation); assert only once they have settled
        rule.mainClock.advanceTimeBy(2_000)
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }

    fun assertRetryFooterVisible() = apply { rule.onNodeWithTag(TvShowSectionTags.RETRY_FOOTER).assertIsDisplayed() }

    fun assertRetryFooterAbsent() = apply { rule.onNodeWithTag(TvShowSectionTags.RETRY_FOOTER).assertDoesNotExist() }

    /** Posters are the clickable part of a TV show card. */
    fun clickTvShowAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }

    fun clickRetry() = apply { rule.onNodeWithText("Retry").performClick() }

    fun clickRetryFooter() = apply { rule.onNodeWithTag(TvShowSectionTags.RETRY_FOOTER).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
}
