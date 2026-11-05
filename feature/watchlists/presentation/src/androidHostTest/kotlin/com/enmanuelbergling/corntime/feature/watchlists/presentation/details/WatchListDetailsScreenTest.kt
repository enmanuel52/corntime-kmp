package com.enmanuelbergling.corntime.feature.watchlists.presentation.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class WatchListDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val movies: List<Movie> =
        FakeMovieData.MOVIES.mapIndexed { index, movie -> movie.copy(id = index + 1, title = "Movie ${'A' + index}") }
    private val allActions = mutableListOf<WatchListDetailsAction>()

    /** What the user caused; the list's own near-the-end paging requests are excluded. */
    private val actions get() = allActions.filter { it != WatchListDetailsAction.OnLoadNextPage }

    private fun show(
        items: List<Movie> = movies,
        status: PagingStatus = PagingStatus.Idle,
        removals: RemovalState = RemovalState(),
        isPinned: Boolean = false,
    ) = rule.setThemedContent {
        WatchListDetailsScreen(
            listName = "Weekend",
            state = WatchListDetailsState(7, PagingState(items = items, status = status), removals),
            isPinned = isPinned,
            onAction = { allActions += it },
        )
    }

    @Test
    fun givenAList_whenShown_thenItsNameAndMoviesAreVisible() {
        show()

        rule.onNodeWithText("Weekend").assertIsDisplayed()
        rule.onNodeWithText("Movie A", substring = true).assertIsDisplayed()
    }

    @Test
    fun givenAnEmptyList_whenShown_thenTheEmptyMessageIsShown() {
        show(items = emptyList(), status = PagingStatus.EndReached)

        rule.onNodeWithText("This list has no movies").assertIsDisplayed()
    }

    @Test
    fun givenAPendingRemoval_whenShown_thenTheSnackbarMentionsTheListAndUndoIsSent() {
        val removed = PendingRemoval(movies.first().id, RemovalKind.Remove)

        show(removals = RemovalState(pending = listOf(removed)))

        rule.onNodeWithText("Movie removed from the list").assertIsDisplayed()
        rule.onNodeWithText("Undo").performClick()
        assertThat(actions).containsExactly(WatchListDetailsAction.OnUndo(removed.id))
    }

    @Test
    fun givenNoShortcutYet_whenTappingTheBookmark_thenItAsksToAddOne() {
        show(isPinned = false)

        rule.onNodeWithContentDescription("shortcut icon").performClick()

        assertThat(actions).containsExactly(WatchListDetailsAction.OnAddShortcut)
    }

    @Test
    fun givenAPinnedShortcut_whenTappingTheBookmark_thenItAsksToRemoveIt() {
        show(isPinned = true)

        rule.onNodeWithContentDescription("shortcut icon").performClick()

        assertThat(actions).containsExactly(WatchListDetailsAction.OnRemoveShortcut)
    }

    @Test
    fun givenTheScreen_whenTappingBack_thenBackIsSent() {
        show()

        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions).containsExactly(WatchListDetailsAction.OnBack)
    }
}
