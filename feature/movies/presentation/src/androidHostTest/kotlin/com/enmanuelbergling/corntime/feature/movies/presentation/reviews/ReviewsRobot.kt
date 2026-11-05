package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import com.enmanuelbergling.corntime.feature.movies.presentation.components.RateSheetTags
import com.enmanuelbergling.corntime.feature.movies.presentation.components.RatingSummaryTags
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent

/** Every interaction with [ReviewsScreen], so tests read as intent. */
class ReviewsRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<ReviewsAction>()

    fun show(state: ReviewsState) = apply {
        rule.setThemedContent { ReviewsScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(2_000)
    }

    fun assertTextVisible(text: String) = apply { rule.onAllNodesWithText(text, substring = true).onFirst().assertIsDisplayed() }

    fun assertTextAbsent(text: String) = apply { rule.onAllNodesWithText(text, substring = true).assertCountEquals(0) }

    fun assertTagVisible(tag: String) = apply { rule.onNodeWithTag(tag).assertIsDisplayed() }

    fun assertTagAbsent(tag: String) = apply { rule.onNodeWithTag(tag).assertDoesNotExist() }

    fun assertRateChipVisible() = assertTagVisible(RatingSummaryTags.RATE)

    fun assertRatedChipVisible() = assertTagVisible(RatingSummaryTags.RATED)

    fun assertNoRatingChip() = apply {
        rule.onNodeWithTag(RatingSummaryTags.RATE).assertDoesNotExist()
        rule.onNodeWithTag(RatingSummaryTags.RATED).assertDoesNotExist()
    }

    fun assertSheetVisible() = assertTagVisible(ReviewsTags.SHEET)

    fun assertSheetAbsent() = assertTagAbsent(ReviewsTags.SHEET)

    fun scrollToReview(id: String) = apply {
        rule.onNodeWithTag(ReviewsTags.LIST).performScrollToNode(hasTestTag("${ReviewsTags.REVIEW}$id"))
    }

    fun clickReview(id: String) = apply { rule.onNodeWithTag("${ReviewsTags.REVIEW}$id").performClick() }

    fun clickRateChip() = apply { rule.onNodeWithTag(RatingSummaryTags.RATE).performClick() }

    fun clickRatedChip() = apply { rule.onNodeWithTag(RatingSummaryTags.RATED).performClick() }

    fun clickStar(index: Int) = apply { rule.onNodeWithTag("${RateSheetTags.STAR}$index").performClick() }

    fun clickSubmit() = apply { rule.onNodeWithTag(RateSheetTags.SUBMIT).performClick() }

    fun clickDeleteRating() = apply { rule.onNodeWithTag(RateSheetTags.DELETE).performClick() }

    fun clickRetry() = apply { rule.onNodeWithTag(ReviewsTags.RETRY).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
}
