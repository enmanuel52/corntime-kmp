package com.enmanuelbergling.corntime.feature.movies.presentation.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.movies.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class MovieSearchContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<MovieSearchAction>()
    private val movies = FakeMovieData.MOVIES

    private fun show(
        query: String = "godfather",
        items: List<Movie> = movies,
        status: PagingStatus = PagingStatus.Idle,
    ) {
        rule.setThemedContent {
            MovieSearchContent(
                state = MovieSearchState(query, PagingState(items = items, status = status)),
                onAction = { actions += it },
            )
        }
        rule.mainClock.advanceTimeBy(1_000)
    }

    private fun tag(tag: String) = rule.onNodeWithTag(tag)

    @Test
    fun givenResults_whenShown_thenMovieTitlesAreListed() {
        show()

        rule.onNodeWithText(movies[1].title, substring = true).assertIsDisplayed()
        tag(MovieSearchTags.RESULTS).assertIsDisplayed()
    }

    @Test
    fun givenFinishedSearchWithoutMatches_whenShown_thenNoResultsMessage() {
        show(items = emptyList(), status = PagingStatus.EndReached)

        tag(MovieSearchTags.NO_RESULTS).assertIsDisplayed()
        rule.onNodeWithText("No movies found").assertIsDisplayed()
    }

    @Test
    fun givenBlankQueryWithoutResults_whenShown_thenNeitherNoResultsNorLoading() {
        show(query = "", items = emptyList(), status = PagingStatus.EndReached)

        tag(MovieSearchTags.NO_RESULTS).assertDoesNotExist()
        tag(MovieSearchTags.LOADING).assertDoesNotExist()
    }

    @Test
    fun givenFirstPageLoadingForAQuery_whenShown_thenLoadingIndicator() {
        show(items = emptyList(), status = PagingStatus.LoadingFirstPage)

        tag(MovieSearchTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenMessageAndRetry() {
        show(items = emptyList(), status = PagingStatus.Failed(DataError.Network.RequestTimeout))

        rule.onNodeWithText("Waiting time for response has ended", substring = true).assertIsDisplayed()
        tag(MovieSearchTags.RETRY).performClick()

        assertThat(actions).containsExactly(MovieSearchAction.OnLoadNextPage)
    }

    @Test
    fun givenLaterPageFailed_whenShown_thenResultsStayAndRetryIsOfferedWithoutAutoRetrying() {
        show(status = PagingStatus.Failed(DataError.Network.ServerError))

        rule.onNodeWithText(movies[1].title, substring = true).assertIsDisplayed()
        assertThat(actions).isEmpty()
        tag(MovieSearchTags.RETRY).performClick()

        assertThat(actions).containsExactly(MovieSearchAction.OnLoadNextPage)
    }

    @Test
    fun givenResult_whenTapped_thenDispatchesMovieClick() {
        show()

        rule.onNodeWithText(movies[1].title, substring = true).performClick()

        assertThat(actions).contains(MovieSearchAction.OnMovieClick(movies[1].id))
    }
}
