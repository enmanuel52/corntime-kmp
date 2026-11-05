package com.enmanuelbergling.corntime.feature.actor.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.actor.presentation.util.setThemedContent

/** Encapsulates every interaction with [ActorsScreen] so tests read as intent. */
class ActorsRobot(private val rule: ComposeContentTestRule) {
    private val allActions = mutableListOf<ActorsAction>()

    /** What the user caused; the grid's own near-the-end paging requests are excluded (see [pagingRequests]). */
    val actions get() = allActions.filter { it != ActorsAction.OnLoadNextPage }
    val pagingRequests get() = allActions.count { it == ActorsAction.OnLoadNextPage }
    var drawerOpens = 0
        private set

    fun show(state: ActorsState) = apply {
        rule.setThemedContent {
            ActorsScreen(state = state, onAction = { allActions += it }, onOpenDrawer = { drawerOpens++ })
        }
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }
    fun assertTextAbsent(text: String) = apply { rule.onNodeWithText(text).assertDoesNotExist() }
    fun assertRetryFooterVisible() = apply { rule.onNodeWithTag(ActorsTags.RETRY_FOOTER).assertIsDisplayed() }
    fun assertRetryFooterAbsent() = apply { rule.onNodeWithTag(ActorsTags.RETRY_FOOTER).assertDoesNotExist() }

    /** The photo is the clickable part of an actor card. */
    fun clickActorAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }
    fun clickRetry() = apply { rule.onNodeWithText("Retry").performClick() }
    fun clickRetryFooter() = apply { rule.onNodeWithTag(ActorsTags.RETRY_FOOTER).performClick() }
    fun clickMenu() = apply { rule.onNodeWithContentDescription("Sandwich menu icon").performClick() }
}
