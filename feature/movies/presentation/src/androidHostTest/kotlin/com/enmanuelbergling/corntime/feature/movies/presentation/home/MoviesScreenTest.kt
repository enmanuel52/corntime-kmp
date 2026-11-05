package com.enmanuelbergling.corntime.feature.movies.presentation.home

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class MoviesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { MoviesRobot(rule) }
    private val movies = FakeMovieData.MOVIES

    private fun loaded(user: Boolean = false) = MoviesState(
        sections = MovieSection.entries.associateWith { movies },
        user = if (user) FakeUserData.FAKE_USER else null,
    )

    @Test
    fun givenLoadedFeed_whenShown_thenSectionHeadersAreDisplayed() {
        robot.show(loaded())
            .assertTextVisible("Upcoming")
            .scrollFeedTo("For you")
            .assertTextVisible("For you")
            .scrollFeedTo("Top Rated")
            .assertTextVisible("Top Rated")
            .scrollFeedTo("Now playing")
            .assertTextVisible("Now playing")
            .scrollFeedTo("Popular")
            .assertTextVisible("Popular")
    }

    @Test
    fun givenFirstLoad_whenShown_thenSectionsAreNotRenderedYet() {
        robot.show(MoviesState(isLoading = true))
            .assertTextAbsent("Upcoming")
            .assertTextAbsent("Top Rated")
    }

    @Test
    fun givenFailureWithEmptyFeed_whenShown_thenFullScreenErrorAndRetry() {
        robot.show(MoviesState(error = UiError.Timeout))
            .assertTextVisible("Waiting time for response has ended, check your internet connection and try again")
            .clickText("Retry")

        assertThat(robot.actions).containsExactly(MoviesAction.OnRetry)
    }

    @Test
    fun givenFailureWithPartialFeed_whenShown_thenDialogOverTheFeed() {
        robot.show(loaded().copy(error = UiError.NoInternet))
            .assertTextVisible("No internet connection, check your network and try again")
            .assertTextVisible("Upcoming")
            .clickText("Accept")

        assertThat(robot.actions).containsExactly(MoviesAction.OnDismissError)
    }

    @Test
    fun givenPartialFeedError_whenRetryChosenInDialog_thenDispatchesRetry() {
        robot.show(loaded().copy(error = UiError.Server)).clickText("Retry")

        assertThat(robot.actions).containsExactly(MoviesAction.OnRetry)
    }

    @Test
    fun givenSectionMoreButton_whenClicked_thenDispatchesThatSection() {
        robot.show(loaded()).clickMoreAt(0)

        assertThat(robot.actions).containsExactly(MoviesAction.OnSectionClick(MovieSection.Upcoming))
    }

    @Test
    fun givenPoster_whenClicked_thenDispatchesMovieClick() {
        robot.show(loaded()).scrollFeedTo("Top Rated").clickPosterAt(0)

        assertThat(robot.actions.single()).isEqualTo(MoviesAction.OnMovieClick(movies.first().id))
    }

    @Test
    fun givenNoOptionalCallbacks_whenShown_thenDrawerFilterAndFavoritesAreHidden() {
        robot.show(loaded(user = true))
            .assertTagVisible(MoviesTags.DRAWER, false)
            .assertTagVisible(MoviesTags.FILTER, false)
            .assertTagVisible(MoviesTags.FAVORITES, false)
    }

    @Test
    fun givenCallbacksAndLoggedOut_whenShown_thenDrawerAndFilterShowButFavoritesStayHidden() {
        robot.show(loaded(user = false), withDrawer = true, withFilter = true, withFavorites = true)
            .assertTagVisible(MoviesTags.DRAWER, true)
            .assertTagVisible(MoviesTags.FILTER, true)
            .assertTagVisible(MoviesTags.FAVORITES, false)
    }

    @Test
    fun givenCallbacksAndLoggedIn_whenShown_thenFavoritesButtonAppearsAndWorks() {
        robot.show(loaded(user = true), withFavorites = true)
            .assertTagVisible(MoviesTags.FAVORITES, true)
            .clickTag(MoviesTags.FAVORITES)

        assertThat(robot.favoritesClicks).isEqualTo(1)
    }

    @Test
    fun givenDrawerAndFilter_whenClicked_thenTheirCallbacksRun() {
        robot.show(loaded(), withDrawer = true, withFilter = true)
            .clickTag(MoviesTags.DRAWER)
            .clickTag(MoviesTags.FILTER)

        assertThat(robot.drawerClicks).isEqualTo(1)
        assertThat(robot.filterClicks).isEqualTo(1)
    }

    @Test
    fun givenFreshScreen_whenShown_thenOnlyTheInitialEmptyQueryIsReportedToSearch() {
        robot.show(loaded())

        assertThat(robot.searchActions.map { it.toString() })
            .isEqualTo(listOf("OnQueryChange(query=)"))
        assertThat(robot.actions).isEmpty()
    }
}
