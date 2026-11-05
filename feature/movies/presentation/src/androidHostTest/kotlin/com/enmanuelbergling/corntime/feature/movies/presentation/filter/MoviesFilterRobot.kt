package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent

/** Every interaction with [MoviesFilterScreen], so tests read as intent. */
class MoviesFilterRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<MoviesFilterAction>()

    fun show(state: MoviesFilterState) = apply {
        rule.setThemedContent { MoviesFilterScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(2_000)
    }

    fun assertTextVisible(text: String) = apply { rule.onAllNodesWithText(text, substring = true).onFirst().assertIsDisplayed() }

    fun assertTagVisible(tag: String) = apply { rule.onNodeWithTag(tag).assertIsDisplayed() }

    fun assertTagAbsent(tag: String) = apply { rule.onNodeWithTag(tag).assertDoesNotExist() }

    fun clickGenre(id: Int) = apply { rule.onNodeWithTag("${MoviesFilterTags.GENRE}$id").performClick() }

    fun clickSort(criteria: SortCriteria) =
        apply { rule.onNodeWithTag("${MoviesFilterTags.SORT}${criteria.name}").performClick() }

    fun clickClear() = apply { rule.onNodeWithTag(MoviesFilterTags.CLEAR).performClick() }

    fun clickText(text: String) = apply { rule.onNodeWithText(text).performClick() }

    /** Posters are the clickable part of a movie card. */
    fun clickMovieAt(index: Int) = apply { rule.onAllNodesWithContentDescription("movie image")[index].performClick() }

    fun clickRetry() = apply { rule.onNodeWithTag(MoviesFilterTags.RETRY).performClick() }

    fun clickBack() = apply { rule.onNodeWithContentDescription("back icon").performClick() }
}
