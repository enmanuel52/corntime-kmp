package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBreakdownSimulator
import com.enmanuelbergling.corntime.feature.movies.presentation.components.RatingSummaryTags
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class ReviewsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { ReviewsRobot(rule) }
    private val reviews = FakeMovieData.REVIEWS
    private val loggedIn = MovieAccountStates(id = 7, favorite = false, watchlist = false)

    private fun state(
        paging: PagingState<com.enmanuelbergling.corntime.core.domain.model.movie.Review> =
            PagingState(items = reviews, status = PagingStatus.EndReached),
        accountStates: MovieAccountStates? = null,
        withUser: Boolean = accountStates != null,
        sheetOpen: Boolean = false,
        expanded: String? = null,
        error: UiError? = null,
    ) = ReviewsState(
        movieId = 7,
        averageRating = 4.2f,
        totalReviews = 120,
        paging = paging,
        accountStates = accountStates,
        user = FakeUserData.FAKE_USER.takeIf { withUser },
        isRateSheetOpen = sheetOpen,
        expandedReviewId = expanded,
        error = error,
        breakdown = RatingBreakdownSimulator.get(4.2, 120),
    )

    @Test
    fun givenSummary_whenShown_thenAverageAndVoteCountAreVisible() {
        robot.show(state())
            .assertTagVisible(RatingSummaryTags.AVERAGE)
            .assertTextVisible("4.2")
            .assertTextVisible("120 reviews")
    }

    @Test
    fun givenReviews_whenShown_thenAuthorsAndContentAreListed() {
        robot.show(state())
            .assertTextVisible(reviews[0].author)
            .assertTextVisible("A masterpiece")
    }

    @Test
    fun givenReviewPostedLongAgo_whenShown_thenElapsedTimeIsLocalizedText() {
        robot.show(state()).assertTextVisible("years ago")
    }

    @Test
    fun givenNoReviews_whenFinished_thenEmptyMessage() {
        robot.show(state(paging = PagingState(status = PagingStatus.EndReached)))
            .assertTagVisible(ReviewsTags.EMPTY)
    }

    @Test
    fun givenLoadedReviewsAndFailedNextPage_whenShown_thenReviewsStayAndRetryDispatchesLoad() {
        robot.show(state(paging = PagingState(items = reviews, status = PagingStatus.Failed(DataError.Network.NoInternet))))
            .assertTextVisible(reviews[0].author)
            .assertTextVisible("No internet connection")
            .clickRetry()

        assertThat(robot.actions).contains(ReviewsAction.OnLoadNextPage)
    }

    @Test
    fun givenReviewTapped_whenClicked_thenTogglesItsExpansion() {
        robot.show(state()).clickReview(reviews[0].id)

        assertThat(robot.actions).contains(ReviewsAction.OnToggleReview(reviews[0].id))
    }

    @Test
    fun givenLoggedOut_whenShown_thenThereIsNoRatingChip() {
        robot.show(state()).assertNoRatingChip()
    }

    @Test
    fun givenLoggedInAndUnrated_whenShown_thenRateChipOpensTheSheet() {
        robot.show(state(accountStates = loggedIn))
            .assertRateChipVisible()
            .clickRateChip()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnToggleRateSheet)
    }

    @Test
    fun givenRated_whenShown_thenRatedChipReplacesTheRateChip() {
        robot.show(state(accountStates = loggedIn.copy(rated = 4f)))
            .assertRatedChipVisible()
            .assertTextVisible("You have rated this movie")
    }

    @Test
    fun givenSheetOpen_whenShown_thenRatingPromptAndSubmitAreVisible() {
        robot.show(state(accountStates = loggedIn, sheetOpen = true))
            .assertSheetVisible()
            .assertTextVisible("Leave a review")
            .assertTextVisible(FakeUserData.FAKE_USER.name)
    }

    @Test
    fun givenSheetClosed_whenShown_thenNoSheet() {
        robot.show(state(accountStates = loggedIn)).assertSheetAbsent()
    }

    @Test
    fun givenSheetOpenWithoutUser_whenShown_thenSheetCannotRenderWithoutProfile() {
        robot.show(state(accountStates = loggedIn, withUser = false, sheetOpen = true)).assertSheetAbsent()
    }

    @Test
    fun givenSheetOpen_whenSubmittingWithDefaultStars_thenSubmitsFive() {
        robot.show(state(accountStates = loggedIn, sheetOpen = true)).clickSubmit()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnSubmitRating(5f))
    }

    @Test
    fun givenSheetOpen_whenPickingThreeStars_thenSubmitsThree() {
        robot.show(state(accountStates = loggedIn, sheetOpen = true)).clickStar(3).clickSubmit()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnSubmitRating(3f))
    }

    @Test
    fun givenSheetOpen_whenTappingTheSelectedStarAgain_thenTakesHalfAStarOff() {
        robot.show(state(accountStates = loggedIn, sheetOpen = true)).clickStar(5).clickSubmit()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnSubmitRating(4.5f))
    }

    @Test
    fun givenRatedMovie_whenSheetOpens_thenStartsFromTheCurrentRatingAndOffersRemoval() {
        robot.show(state(accountStates = loggedIn.copy(rated = 2f), sheetOpen = true))
            .clickSubmit()
            .clickDeleteRating()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnSubmitRating(2f), ReviewsAction.OnDeleteRating)
    }

    @Test
    fun givenUnratedMovie_whenSheetOpens_thenThereIsNothingToRemove() {
        robot.show(state(accountStates = loggedIn, sheetOpen = true))
            .assertTagAbsent("rate_sheet_delete")

        assertThat(robot.actions).isEmpty()
    }

    @Test
    fun givenRatingFailed_whenShown_thenErrorDialogIsShown() {
        robot.show(state(accountStates = loggedIn, error = UiError.NoInternet))
            .assertTextVisible("No internet connection")
    }

    @Test
    fun givenBackPressed_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(ReviewsAction.OnBack)
    }
}
