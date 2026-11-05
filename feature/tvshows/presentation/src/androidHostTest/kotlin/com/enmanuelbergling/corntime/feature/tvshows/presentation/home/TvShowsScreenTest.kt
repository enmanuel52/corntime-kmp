package com.enmanuelbergling.corntime.feature.tvshows.presentation.home

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class TvShowsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { TvShowsRobot(rule) }
    private val tvShows = FakeTvData.TV_SHOWS

    private fun loaded(user: Boolean = false) = TvShowsState(
        sections = TvShowsSection.entries.associateWith { tvShows },
        user = if (user) FakeUserData.FAKE_USER else null,
    )

    @Test
    fun givenLoadedFeed_whenShown_thenSectionHeadersAreDisplayed() {
        robot.show(loaded())
            .assertTextVisible("Popular")
            .scrollFeedTo("For you")
            .assertTextVisible("For you")
            .scrollFeedTo("Top Rated")
            .assertTextVisible("Top Rated")
            .scrollFeedTo("On The Air")
            .assertTextVisible("On The Air")
            .scrollFeedTo("Airing Today")
            .assertTextVisible("Airing Today")
    }

    @Test
    fun givenFirstLoad_whenShown_thenSectionsAreNotRenderedYet() {
        robot.show(TvShowsState(isLoading = true))
            .assertTextAbsent("Popular")
            .assertTextAbsent("Top Rated")
    }

    @Test
    fun givenFailureWithEmptyFeed_whenShown_thenFullScreenErrorAndRetry() {
        robot.show(TvShowsState(error = UiError.Timeout))
            .assertTextVisible("Waiting time for response has ended, check your internet connection and try again")
            .clickText("Retry")

        assertThat(robot.actions).containsExactly(TvShowsAction.OnRetry)
    }

    @Test
    fun givenFailureWithPartialFeed_whenShown_thenDialogOverTheFeed() {
        robot.show(loaded().copy(error = UiError.NoInternet))
            .assertTextVisible("No internet connection, check your network and try again")
            .assertTextVisible("Popular")
            .clickText("Accept")

        assertThat(robot.actions).containsExactly(TvShowsAction.OnDismissError)
    }

    @Test
    fun givenPartialFeedError_whenRetryChosenInDialog_thenDispatchesRetry() {
        robot.show(loaded().copy(error = UiError.Server)).clickText("Retry")

        assertThat(robot.actions).containsExactly(TvShowsAction.OnRetry)
    }

    @Test
    fun givenSectionMoreButton_whenClicked_thenDispatchesThatSection() {
        robot.show(loaded()).clickMoreAt(0)

        assertThat(robot.actions).containsExactly(TvShowsAction.OnSectionClick(TvShowsSection.Popular))
    }

    @Test
    fun givenPoster_whenClicked_thenDispatchesTvShowClick() {
        robot.show(loaded()).scrollFeedTo("Top Rated").clickPosterAt(0)

        assertThat(robot.actions.single()).isEqualTo(TvShowsAction.OnTvShowClick(tvShows.first().id))
    }

    @Test
    fun givenNoOptionalCallbacks_whenShown_thenDrawerAndFavoritesAreHidden() {
        robot.show(loaded(user = true))
            .assertTagVisible(TvShowsTags.DRAWER, false)
            .assertTagVisible(TvShowsTags.FAVORITES, false)
    }

    @Test
    fun givenCallbacksAndLoggedOut_whenShown_thenDrawerShowsButFavoritesStayHidden() {
        robot.show(loaded(user = false), withDrawer = true, withFavorites = true)
            .assertTagVisible(TvShowsTags.DRAWER, true)
            .assertTagVisible(TvShowsTags.FAVORITES, false)
    }

    @Test
    fun givenCallbacksAndLoggedIn_whenShown_thenFavoritesButtonAppearsAndWorks() {
        robot.show(loaded(user = true), withFavorites = true)
            .assertTagVisible(TvShowsTags.FAVORITES, true)
            .clickTag(TvShowsTags.FAVORITES)

        assertThat(robot.favoritesClicks).isEqualTo(1)
    }

    @Test
    fun givenDrawer_whenClicked_thenItsCallbackRuns() {
        robot.show(loaded(), withDrawer = true).clickTag(TvShowsTags.DRAWER)

        assertThat(robot.drawerClicks).isEqualTo(1)
    }

    @Test
    fun givenFreshScreen_whenShown_thenOnlyTheInitialEmptyQueryIsReportedToSearch() {
        robot.show(loaded())

        assertThat(robot.searchActions.map { it.toString() })
            .isEqualTo(listOf("OnQueryChange(query=)"))
        assertThat(robot.actions).isEmpty()
    }
}
