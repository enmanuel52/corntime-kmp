package com.enmanuelbergling.corntime.feature.movies.presentation.section

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class MovieSectionScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { MovieSectionRobot(rule) }
    private val movies = FakeMovieData.MOVIES

    private fun state(
        section: MovieSection = MovieSection.Popular,
        items: List<com.enmanuelbergling.corntime.core.domain.model.movie.Movie> = movies,
        status: PagingStatus = PagingStatus.Idle,
    ) = MovieSectionState(section, PagingState(items = items, status = status))

    @Test
    fun givenLoadedMovies_whenShown_thenEveryTitleIsVisible() {
        robot.show(state())
            .assertMovieVisible(movies[0].title)
            .assertMovieVisible(movies[1].title)
    }

    @Test
    fun givenUpcoming_whenShown_thenTopBarTitleIsUpcomingMovies() {
        robot.show(state(MovieSection.Upcoming)).assertTitle("Upcoming Movies")
    }

    @Test
    fun givenTopRated_whenShown_thenTopBarTitleIsTopRatedMovies() {
        robot.show(state(MovieSection.TopRated)).assertTitle("Top Rated Movies")
    }

    @Test
    fun givenNowPlaying_whenShown_thenTopBarTitleIsNowPlayingMovies() {
        robot.show(state(MovieSection.NowPlaying)).assertTitle("Now Playing Movies")
    }

    @Test
    fun givenPopular_whenShown_thenTopBarTitleIsPopularMovies() {
        robot.show(state(MovieSection.Popular)).assertTitle("Popular Movies")
    }

    @Test
    fun givenFinishedWithNoMovies_whenShown_thenEmptyMessage() {
        robot.show(state(items = emptyList(), status = PagingStatus.EndReached))
            .assertTextVisible("No movies found")
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenFullScreenErrorAndRetryDispatchesLoad() {
        robot.show(state(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet)))
            .assertTextVisible("No internet connection, check your network and try again")
            .clickRetry()

        assertThat(robot.actions).containsExactly(MovieSectionAction.OnLoadNextPage)
    }

    @Test
    fun givenLaterPageFailed_whenShown_thenMoviesStayAndFooterRetryIsOffered() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.ServerError)))
            .assertMovieVisible(movies[0].title)
            .assertRetryFooterVisible()
            .clickRetryFooter()

        assertThat(robot.actions).containsExactly(MovieSectionAction.OnLoadNextPage)
    }

    @Test
    fun givenNoFailure_whenShown_thenNoRetryFooter() {
        robot.show(state()).assertRetryFooterAbsent()
    }

    @Test
    fun givenMovieTapped_whenClicked_thenDispatchesItsId() {
        robot.show(state()).clickMovieAt(1)

        assertThat(robot.actions).contains(MovieSectionAction.OnMovieClick(movies[1].id))
    }

    @Test
    fun givenBackPressed_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions.last()).isEqualTo(MovieSectionAction.OnBack)
    }

    @Test
    fun givenOnlyTheFirstPageIsLoading_whenShown_thenNoActionIsDispatchedBySimplyShowing() {
        robot.show(state(items = emptyList(), status = PagingStatus.LoadingFirstPage))

        assertThat(robot.actions).isEmpty()
    }
}
