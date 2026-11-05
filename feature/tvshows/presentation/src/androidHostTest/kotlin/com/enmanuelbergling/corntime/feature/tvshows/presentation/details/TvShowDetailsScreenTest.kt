package com.enmanuelbergling.corntime.feature.tvshows.presentation.details

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class TvShowDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { TvShowDetailsRobot(rule) }

    private val details = FakeTvData.DEFAULT_TV_SHOW_DETAILS.let { fake ->
        fake.copy(seasons = fake.seasons.map { it.copy(name = "Season ${it.seasonNumber}") })
    }
    private val credits = TvCredits(
        id = details.id,
        cast = listOf(cast("Bryan Cranston", 11), cast("Aaron Paul", 12)),
        crew = listOf(crew("Vince Gilligan", 21)),
    )

    private fun cast(name: String, id: Int) = FakeTvData.DEFAULT_TV_CREDITS.cast.first().copy(
        id = id, name = name, originalName = name,
    )

    private fun crew(name: String, id: Int) = FakeTvData.DEFAULT_TV_CREDITS.crew.first().copy(
        id = id, name = name, originalName = name,
    )

    private fun state(
        loggedIn: Boolean = true,
        favorite: Boolean = false,
        watchlist: Boolean = false,
    ) = TvShowDetailsState(
        tvShowId = details.id,
        details = details,
        credits = credits,
        accountStates = if (loggedIn) TvAccountStates(details.id, favorite, watchlist) else null,
    )

    @Test
    fun givenLoadedTvShow_whenShown_thenTitleYearAndGenresAreDisplayed() {
        robot.show(state())
            .assertContentVisible(true)
            .assertTextVisible("${details.name} (${details.firstAirYear})")
            .assertTextVisible(details.formattedGenres, substring = true)
            .assertTextVisible("5 seasons", substring = true)
    }

    @Test
    fun givenSeasons_whenShown_thenTheStripListsThemWithSeeAll() {
        robot.show(state())
            .assertTextVisible("Seasons")
            .assertTextVisible("Season 1")
            .scrollSeasonsToSeeAll()
            .assertTextVisible("See all")
    }

    @Test
    fun givenCredits_whenShown_thenCastAndCrewNamesAreDisplayed() {
        robot.show(state())
            .scrollContentTo("Bryan Cranston")
            .assertTextVisible("Bryan Cranston")
            .assertTextVisible("Aaron Paul")
            .scrollContentTo("Vince Gilligan")
            .assertTextVisible("Vince Gilligan")
    }

    @Test
    fun givenFirstLoad_whenShown_thenOnlyAProgressStateWithoutContentOrError() {
        robot.show(TvShowDetailsState(tvShowId = 1, isLoading = true))
            .assertContentVisible(false)
            .assertFavoriteButton(false)
            .assertWatchlistButton(false)
    }

    @Test
    fun givenFailureWithoutData_whenShown_thenFullScreenErrorAndRetryDispatchesRetry() {
        robot.show(TvShowDetailsState(tvShowId = 1, error = UiError.Server))
            .assertTextVisible("An error just happened, check your network connection and try again")
            .clickText("Retry")

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnRetry)
    }

    @Test
    fun givenFailureWithData_whenShown_thenDialogOverContentAndAcceptDismisses() {
        robot.show(state().copy(error = UiError.NoInternet))
            .assertTextVisible("No internet connection, check your network and try again")
            .assertContentVisible(true)
            .clickText("Accept")

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnDismissError)
    }

    @Test
    fun givenLoggedOut_whenShown_thenAccountActionsAreHidden() {
        robot.show(state(loggedIn = false))
            .assertFavoriteButton(false)
            .assertWatchlistButton(false)
    }

    @Test
    fun givenLoggedIn_whenShown_thenAccountActionsAreAvailable() {
        robot.show(state(loggedIn = true))
            .assertFavoriteButton(true)
            .assertWatchlistButton(true)
    }

    @Test
    fun givenLoggedIn_whenTappingWatchlistAndFavorite_thenEachActionIsDispatched() {
        robot.show(state()).clickWatchlist().clickFavorite()

        assertThat(robot.actions).containsExactly(
            TvShowDetailsAction.OnWatchlistClick,
            TvShowDetailsAction.OnFavoriteClick,
        )
    }

    @Test
    fun givenWatchlistRequestInFlight_whenShown_thenSpinnerReplacesOnlyTheWatchlistButton() {
        robot.show(state().copy(isWatchlistLoading = true))
            .assertWatchlistLoading(true)
            .assertWatchlistButton(false)
            .assertFavoriteButton(true)
            .assertFavoriteLoading(false)
    }

    @Test
    fun givenFavoriteRequestInFlight_whenShown_thenSpinnerReplacesOnlyTheFavoriteButton() {
        robot.show(state().copy(isFavoriteLoading = true))
            .assertFavoriteLoading(true)
            .assertFavoriteButton(false)
            .assertWatchlistButton(true)
    }

    @Test
    fun givenSeasonPoster_whenTapped_thenDispatchesSeasonClickWithItsNumber() {
        robot.show(state()).clickSeasonPosterAt(1)

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnSeasonClick(2))
    }

    @Test
    fun givenSeeAll_whenTapped_thenDispatchesSeeAllSeasons() {
        robot.show(state()).scrollSeasonsToSeeAll().clickSeeAllSeasons()

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnSeeAllSeasons)
    }

    @Test
    fun givenCastMember_whenTapped_thenDispatchesPersonClickWithTheirId() {
        robot.show(state()).scrollContentTo("Bryan Cranston").clickPersonCardAt(1)

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnPersonClick(12))
    }

    @Test
    fun givenBackTapped_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(TvShowDetailsAction.OnBack)
    }

    @Test
    fun givenNothingTapped_whenShown_thenNoActionIsDispatched() {
        robot.show(state())

        assertThat(robot.actions).isEmpty()
    }
}
