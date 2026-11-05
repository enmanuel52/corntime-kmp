package com.enmanuelbergling.corntime.feature.actor.presentation.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.actor.presentation.util.setThemedContent

/** Encapsulates every interaction with [ActorDetailsScreen] so tests read as intent. */
class ActorDetailsRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<ActorDetailsAction>()

    fun show(state: ActorDetailsState, name: String = "Tim Bergling") = apply {
        rule.setThemedContent {
            ActorDetailsScreen(imagePath = null, name = name, state = state, onAction = { actions += it })
        }
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text, substring = true).assertIsDisplayed() }
    fun assertTextAbsent(text: String) = apply { rule.onNodeWithText(text, substring = true).assertDoesNotExist() }

    fun clickRetry() = apply { rule.onNodeWithText("Retry").performClick() }
    fun clickAcceptOnDialog() = apply { rule.onNodeWithText("Accept").performClick() }
    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
    /** Posters are the clickable part of a movie card. */
    fun clickMovieAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }
}
