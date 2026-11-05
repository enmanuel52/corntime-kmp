package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ReviewsViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource().apply { reviews = FakeMovieData.REVIEWS }
    private val user = FakeUserRemoteDataSource()
    private val preferences = FakeUserPreferenceDataSource(FakeUserData.FAKE_USER)

    private fun viewModel(average: Double = 4.0, total: Int = 200) =
        ReviewsViewModel(movieId = 7, averageRating = average, totalReviews = total, movies, user, preferences)

    private fun ratings(function: UserRemoteFunction) = user.callsTo(function)

    @Test
    fun givenMovie_whenCreated_thenFirstPageOfReviewsIsLoadedForThatMovie() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.reviews.size).isEqualTo(FakeMovieData.REVIEWS.size)
        assertThat(movies.callsTo(MovieRemoteFunction.GetMovieReviews)).isEqualTo(listOf(listOf<Any?>(7, 1)))
    }

    @Test
    fun givenArguments_whenCreated_thenStateCarriesTheSummaryAndASimulatedBreakdown() = runTest {
        val state = viewModel(average = 4.0, total = 200).state.value

        assertThat(state.averageRating).isEqualTo(4f)
        assertThat(state.totalReviews).isEqualTo(200)
        assertThat(state.breakdown.map { it.stars }).isEqualTo(listOf(5, 4, 3, 2, 1))
        assertThat(state.breakdown.sumOf { it.count }).isEqualTo(200)
    }

    @Test
    fun givenNoVotes_whenCreated_thenBreakdownIsEmpty() = runTest {
        assertThat(viewModel(average = 0.0, total = 0).state.value.breakdown).isEmpty()
    }

    @Test
    fun givenNoReviews_whenLoaded_thenPagingIsEmpty() = runTest {
        movies.reviews = emptyList()

        assertThat(viewModel().state.value.paging.isEmpty).isTrue()
    }

    @Test
    fun givenMorePages_whenLoadingNext_thenAppendsThem() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()

        viewModel.onAction(ReviewsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.reviews.size).isEqualTo(FakeMovieData.REVIEWS.size * 2)
        assertThat(movies.callsTo(MovieRemoteFunction.GetMovieReviews).last()).isEqualTo(listOf<Any?>(7, 2))
    }

    @Test
    fun givenPageFails_whenRetrying_thenReviewsAreKeptAndItRecovers() = runTest {
        movies.totalPages = 3
        val viewModel = viewModel()
        movies.fail(MovieRemoteFunction.GetMovieReviews, DataError.Network.ServerError)

        viewModel.onAction(ReviewsAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.uiError).isEqualTo(UiError.Server)
        assertThat(viewModel.state.value.reviews.size).isEqualTo(FakeMovieData.REVIEWS.size)

        movies.recover(MovieRemoteFunction.GetMovieReviews)
        viewModel.onAction(ReviewsAction.OnLoadNextPage)

        assertThat(viewModel.state.value.reviews.size).isEqualTo(FakeMovieData.REVIEWS.size * 2)
    }

    @Test
    fun givenLoggedIn_whenCreated_thenAccountStatesAndUserAreLoadedSoRatingIsAvailable() = runTest {
        movies.accountStates = MovieAccountStates(id = 7, favorite = false, watchlist = false, rated = 3.5f)

        val state = viewModel().state.value

        assertThat(state.rated).isEqualTo(3.5f)
        assertThat(state.user).isEqualTo(FakeUserData.FAKE_USER)
        assertThat(state.canRate).isTrue()
    }

    @Test
    fun givenLoggedOut_whenCreated_thenRatingIsUnavailableAndTheSheetCannotOpen() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
        val viewModel = viewModel()

        viewModel.onAction(ReviewsAction.OnToggleRateSheet)

        assertThat(viewModel.state.value.canRate).isFalse()
        assertThat(viewModel.state.value.isRateSheetOpen).isFalse()
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.reviews.size).isEqualTo(FakeMovieData.REVIEWS.size)
    }

    @Test
    fun givenRatableMovie_whenTogglingTheSheet_thenItOpensAndCloses() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ReviewsAction.OnToggleRateSheet)
        assertThat(viewModel.state.value.isRateSheetOpen).isTrue()

        viewModel.onAction(ReviewsAction.OnToggleRateSheet)
        assertThat(viewModel.state.value.isRateSheetOpen).isFalse()
    }

    @Test
    fun givenRating_whenSubmitted_thenItIsSentStoredAndTheSheetCloses() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReviewsAction.OnToggleRateSheet)

        viewModel.onAction(ReviewsAction.OnSubmitRating(4.5f))

        assertThat(ratings(UserRemoteFunction.AddMovieRating)).isEqualTo(listOf(listOf<Any?>(7, 4.5f)))
        assertThat(viewModel.state.value.rated).isEqualTo(4.5f)
        assertThat(viewModel.state.value.isRateSheetOpen).isFalse()
        assertThat(viewModel.state.value.isRating).isFalse()
    }

    @Test
    fun givenSlowNetwork_whenSubmittingRating_thenItIsLoadingAndASecondSubmitIsIgnored() = runTest {
        val viewModel = viewModel()
        user.hold()

        viewModel.onAction(ReviewsAction.OnSubmitRating(4f))
        assertThat(viewModel.state.value.isRating).isTrue()
        viewModel.onAction(ReviewsAction.OnSubmitRating(2f))
        user.release()

        assertThat(ratings(UserRemoteFunction.AddMovieRating).size).isEqualTo(1)
        assertThat(viewModel.state.value.isRating).isFalse()
        assertThat(viewModel.state.value.rated).isEqualTo(4f)
    }

    @Test
    fun givenRatingFails_whenSubmitted_thenErrorIsShownAndThePreviousRatingIsKept() = runTest {
        movies.accountStates = MovieAccountStates(id = 7, favorite = false, watchlist = false, rated = 2f)
        val viewModel = viewModel()
        viewModel.onAction(ReviewsAction.OnToggleRateSheet)
        user.fail(UserRemoteFunction.AddMovieRating, DataError.Network.NoInternet)

        viewModel.onAction(ReviewsAction.OnSubmitRating(5f))

        assertThat(viewModel.state.value.error).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.rated).isEqualTo(2f)
        assertThat(viewModel.state.value.isRating).isFalse()
        assertThat(viewModel.state.value.isRateSheetOpen).isTrue()
    }

    @Test
    fun givenLoggedOut_whenSubmittingRating_thenNothingIsSent() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
        val viewModel = viewModel()

        viewModel.onAction(ReviewsAction.OnSubmitRating(5f))

        assertThat(ratings(UserRemoteFunction.AddMovieRating)).isEmpty()
    }

    @Test
    fun givenRatedMovie_whenRatingIsDeleted_thenItIsRemovedAndTheSheetCloses() = runTest {
        movies.accountStates = MovieAccountStates(id = 7, favorite = true, watchlist = false, rated = 3f)
        val viewModel = viewModel()
        viewModel.onAction(ReviewsAction.OnToggleRateSheet)

        viewModel.onAction(ReviewsAction.OnDeleteRating)

        assertThat(ratings(UserRemoteFunction.DeleteMovieRating)).isEqualTo(listOf(listOf<Any?>(7)))
        assertThat(viewModel.state.value.rated).isNull()
        assertThat(viewModel.state.value.accountStates?.favorite).isEqualTo(true)
        assertThat(viewModel.state.value.isRateSheetOpen).isFalse()
    }

    @Test
    fun givenDeleteFails_whenDeleting_thenErrorIsShownAndTheRatingStays() = runTest {
        movies.accountStates = MovieAccountStates(id = 7, favorite = false, watchlist = false, rated = 3f)
        val viewModel = viewModel()
        user.fail(UserRemoteFunction.DeleteMovieRating)

        viewModel.onAction(ReviewsAction.OnDeleteRating)

        assertThat(viewModel.state.value.error).isEqualTo(UiError.Unknown)
        assertThat(viewModel.state.value.rated).isEqualTo(3f)
    }

    @Test
    fun givenError_whenDismissed_thenItDisappears() = runTest {
        val viewModel = viewModel()
        user.fail(UserRemoteFunction.AddMovieRating)
        viewModel.onAction(ReviewsAction.OnSubmitRating(3f))

        viewModel.onAction(ReviewsAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenReviews_whenToggled_thenOnlyOneIsExpandedAtATime() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ReviewsAction.OnToggleReview("review-1"))
        assertThat(viewModel.state.value.expandedReviewId).isEqualTo("review-1")

        viewModel.onAction(ReviewsAction.OnToggleReview("review-2"))
        assertThat(viewModel.state.value.expandedReviewId).isEqualTo("review-2")

        viewModel.onAction(ReviewsAction.OnToggleReview("review-2"))
        assertThat(viewModel.state.value.expandedReviewId).isNull()
    }

    @Test
    fun givenBack_whenHandled_thenNavigatesBackOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReviewsAction.OnBack)

            assertThat(awaitItem()).isEqualTo(ReviewsEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
