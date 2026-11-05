package com.enmanuelbergling.corntime.feature.movies.presentation.section

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent

private const val ANIMATION_SETTLE_MS = 2_000L

/** Encapsulates every interaction with [MovieSectionScreen] so tests read as intent. */
class MovieSectionRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<MovieSectionAction>()

    fun show(state: MovieSectionState) = apply {
        rule.setThemedContent { MovieSectionScreen(state = state, onAction = { actions += it }) }
        // list items animate in (scale/rotation); assert only once they have settled
        rule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
    }

    fun assertTitle(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }

    fun assertMovieVisible(title: String) = apply { rule.onNodeWithText(title).assertIsDisplayed() }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }

    fun assertRetryFooterVisible() = apply { rule.onNodeWithTag(MovieSectionTags.RETRY_FOOTER).assertIsDisplayed() }

    fun assertRetryFooterAbsent() = apply { rule.onNodeWithTag(MovieSectionTags.RETRY_FOOTER).assertDoesNotExist() }

    /** Posters are the clickable part of a movie card. */
    fun clickMovieAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }

    fun clickRetry(label: String = "Retry") = apply { rule.onNodeWithText(label).performClick() }

    fun clickRetryFooter() = apply { rule.onNodeWithTag(MovieSectionTags.RETRY_FOOTER).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
}
