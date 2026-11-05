package com.enmanuelbergling.corntime.feature.movies.presentation.details

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.model.movie.Cast
import com.enmanuelbergling.corntime.core.domain.model.movie.Crew
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBreakdownSimulator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class MovieDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { MovieDetailsRobot(rule) }

    private val details = FakeMovieData.DEFAULT_MOVIE_DETAILS
    private val credits = MovieCredits(
        id = details.id,
        cast = listOf(person("Al Pacino", 11), person("Diane Keaton", 12)).map { it.first },
        crew = listOf(crewMember("Francis Coppola", 21)),
    )

    private fun person(name: String, id: Int) = Cast(
        adult = false, gender = 1, id = id, knownForDepartment = "", name = name, originalName = name,
        popularity = 1.0, profilePath = null, castId = id, character = "", creditId = "", order = 0,
    ) to id

    private fun crewMember(name: String, id: Int) = Crew(
        adult = false, gender = 1, id = id, knownForDepartment = "", name = name, originalName = name,
        popularity = 1.0, profilePath = null, creditId = "", department = "", job = "Director",
    )

    private fun state(
        loggedIn: Boolean = true,
        favorite: Boolean = false,
        watchlist: Boolean = false,
    ) = MovieDetailsState(
        movieId = details.id,
        details = details,
        credits = credits,
        accountStates = if (loggedIn) MovieAccountStates(details.id, favorite, watchlist) else null,
    )

    @Test
    fun givenLoadedMovie_whenShown_thenTitleYearAndGenresAreDisplayed() {
        robot.show(state())
            .assertContentVisible(true)
            .assertTextVisible("${details.title} (${details.releaseYear})")
            .assertTextVisible(details.formattedGenres, substring = true)
    }

    @Test
    fun givenLoadedMovie_whenShown_thenRatingSummaryShowsTheVoteCount() {
        robot.show(state().copy(ratingBreakdown = RatingBreakdownSimulator.get(details.voteAverage, details.voteCount)))
            .assertRatingSummary(true)
            .assertTextVisible("${details.voteCount} review", substring = true)
    }

    @Test
    fun givenRatingSummary_whenTapped_thenOpensReviews() {
        robot.show(state()).clickRatingSummary()

        assertThat(robot.actions).containsExactly(MovieDetailsAction.OnReviewsClick)
    }

    @Test
    fun givenLoggedOut_whenShown_thenRatingSummaryIsStillVisibleButOffersNoRating() {
        robot.show(state(loggedIn = false)).assertRatingSummary(true)
            .assertTextAbsent("Rate this movie")
    }

    @Test
    fun givenCredits_whenShown_thenCastAndCrewNamesAreDisplayed() {
        robot.show(state())
            .assertTextVisible("Al Pacino")
            .assertTextVisible("Diane Keaton")
    }

    @Test
    fun givenFirstLoad_whenShown_thenOnlyAProgressStateWithoutContentOrError() {
        robot.show(MovieDetailsState(movieId = 1, isLoading = true))
            .assertContentVisible(false)
            .assertFavoriteButton(false)
            .assertWatchlistButton(false)
    }

    @Test
    fun givenFailureWithoutData_whenShown_thenFullScreenErrorAndRetryDispatchesRetry() {
        robot.show(MovieDetailsState(movieId = 1, error = UiError.Server))
            .assertTextVisible("An error just happened, check your network connection and try again")
            .clickText("Retry")

        assertThat(robot.actions).containsExactly(MovieDetailsAction.OnRetry)
    }

    @Test
    fun givenFailureWithData_whenShown_thenDialogOverContentAndAcceptDismisses() {
        robot.show(state().copy(error = UiError.NoInternet))
            .assertTextVisible("No internet connection, check your network and try again")
            .assertContentVisible(true)
            .clickText("Accept")

        assertThat(robot.actions).containsExactly(MovieDetailsAction.OnDismissError)
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
        robot.show(state())
            .clickWatchlist()
            .clickFavorite()

        assertThat(robot.actions).containsExactly(
            MovieDetailsAction.OnWatchlistClick,
            MovieDetailsAction.OnFavoriteClick,
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
    fun givenMovieAlreadyFavorite_whenShown_thenFavoriteButtonStillRenders() {
        robot.show(state(favorite = true, watchlist = true))
            .assertFavoriteButton(true)
            .assertWatchlistButton(true)
    }

    @Test
    fun givenCastMember_whenTapped_thenDispatchesPersonClickWithTheirId() {
        robot.show(state()).clickPersonCardAt(1)

        assertThat(robot.actions).containsExactly(MovieDetailsAction.OnPersonClick(12))
    }

    @Test
    fun givenBackTapped_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(MovieDetailsAction.OnBack)
    }

    @Test
    fun givenNothingTapped_whenShown_thenNoActionIsDispatched() {
        robot.show(state())

        assertThat(robot.actions).isEmpty()
        assertThat(state().cast.map { it.name }).isEqualTo(listOf("Al Pacino", "Diane Keaton"))
    }
}
