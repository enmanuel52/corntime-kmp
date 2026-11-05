package com.enmanuelbergling.corntime.feature.movies.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchAction
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchState
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent

class MoviesRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<MoviesAction>()
    val searchActions = mutableListOf<MovieSearchAction>()
    var drawerClicks = 0
    var filterClicks = 0
    var favoritesClicks = 0

    fun show(
        state: MoviesState,
        withDrawer: Boolean = false,
        withFilter: Boolean = false,
        withFavorites: Boolean = false,
    ) = apply {
        rule.setThemedContent {
            MoviesScreen(
                state = state,
                searchState = MovieSearchState(),
                onAction = { actions += it },
                onSearchAction = { searchActions += it },
                onOpenDrawer = if (withDrawer) ({ drawerClicks++ }) else null,
                onFilter = if (withFilter) ({ filterClicks++ }) else null,
                onFavorites = if (withFavorites) ({ favoritesClicks++ }) else null,
            )
        }
        rule.mainClock.advanceTimeBy(2_000)
    }

    fun scrollFeedTo(text: String) = apply {
        rule.onNodeWithTag(MoviesTags.FEED).performScrollToNode(hasText(text))
        rule.mainClock.advanceTimeBy(1_000)
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }

    fun assertTextAbsent(text: String) = apply { rule.onNodeWithText(text).assertDoesNotExist() }

    fun assertTagVisible(tag: String, visible: Boolean) = apply {
        if (visible) rule.onNodeWithTag(tag).assertIsDisplayed() else rule.onNodeWithTag(tag).assertDoesNotExist()
    }

    fun clickText(text: String) = apply { rule.onNodeWithText(text).performClick() }

    fun clickTag(tag: String) = apply { rule.onNodeWithTag(tag).performClick() }

    fun clickMoreAt(index: Int) = apply {
        rule.onAllNodesWithContentDescription("more icon")[index].performClick()
    }

    fun clickPosterAt(index: Int) = apply {
        rule.onAllNodesWithContentDescription("movie image")[index].performClick()
    }
}
