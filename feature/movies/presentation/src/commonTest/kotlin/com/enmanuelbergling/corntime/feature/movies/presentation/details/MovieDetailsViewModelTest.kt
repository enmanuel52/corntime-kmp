package com.enmanuelbergling.corntime.feature.movies.presentation.details

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.movies.domain.details.MovieDetailsChain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MovieDetailsViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource()
    private val user = FakeUserRemoteDataSource()

    private fun viewModel(movieId: Int = 7) =
        MovieDetailsViewModel(movieId, MovieDetailsChain(movies), user)

    private val MovieDetailsViewModel.current get() = state.value

    @Test
    fun givenAllEndpointsSucceed_whenCreated_thenDetailsCreditsAndAccountStatesLoadedForThatMovie() = runTest {
        val state = viewModel(movieId = 7).current

        assertThat(state.details?.id).isEqualTo(7)
        assertThat(state.credits?.id).isEqualTo(7)
        assertThat(state.accountStates?.id).isEqualTo(7)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.canManageAccount).isTrue()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenIsLoadingUntilFinished() = runTest {
        movies.hold()
        val viewModel = viewModel()
        assertThat(viewModel.current.isLoading).isTrue()
        assertThat(viewModel.current.details).isNull()

        movies.release()

        assertThat(viewModel.current.isLoading).isFalse()
        assertThat(viewModel.current.details).isNotNull()
    }

    @Test
    fun givenCredits_whenMappingPeople_thenCastAndCrewBecomePersonUi() = runTest {
        val state = viewModel().current

        assertThat(state.cast.map { it.name }).isEqualTo(state.credits!!.cast.map { it.name })
        assertThat(state.crew.map { it.id }).isEqualTo(state.credits!!.crew.map { it.id })
    }

    @Test
    fun givenDetailsFail_whenCreated_thenErrorAndNothingElseRequestedThenRetryRecovers() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieDetails, DataError.Network.ServerError)
        val viewModel = viewModel()

        assertThat(viewModel.current.error).isEqualTo(UiError.Server)
        assertThat(viewModel.current.details).isNull()
        assertThat(movies.callCount(MovieRemoteFunction.GetMovieCredits)).isEqualTo(0)

        movies.recover(MovieRemoteFunction.GetMovieDetails)
        viewModel.onAction(MovieDetailsAction.OnRetry)

        assertThat(viewModel.current.error).isNull()
        assertThat(viewModel.current.details).isNotNull()
    }

    @Test
    fun givenCreditsFail_whenRetried_thenDetailsAreNotRequestedAgain() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieCredits)
        val viewModel = viewModel()
        assertThat(viewModel.current.details).isNotNull()
        assertThat(viewModel.current.error).isEqualTo(UiError.Unknown)

        movies.recover(MovieRemoteFunction.GetMovieCredits)
        viewModel.onAction(MovieDetailsAction.OnRetry)

        assertThat(movies.callCount(MovieRemoteFunction.GetMovieDetails)).isEqualTo(1)
        assertThat(viewModel.current.credits).isNotNull()
    }

    @Test
    fun givenLoggedOut_whenCreated_thenNoErrorAndAccountActionsUnavailable() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)

        val state = viewModel().current

        assertThat(state.error).isNull()
        assertThat(state.details).isNotNull()
        assertThat(state.credits).isNotNull()
        assertThat(state.accountStates).isNull()
        assertThat(state.canManageAccount).isFalse()
    }

    @Test
    fun givenUnauthorizedOnDetailsItself_whenCreated_thenItIsStillShownAsAnError() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieDetails, DataError.Network.Unauthorized)

        assertThat(viewModel().current.error).isEqualTo(UiError.Unauthorized)
    }

    @Test
    fun givenRetryWhileLoading_whenRequested_thenItIsIgnored() = runTest {
        movies.hold()
        val viewModel = viewModel()

        viewModel.onAction(MovieDetailsAction.OnRetry)
        movies.release()

        assertThat(movies.callCount(MovieRemoteFunction.GetMovieDetails)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenClearedAndDataKept() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieCredits)
        val viewModel = viewModel()

        viewModel.onAction(MovieDetailsAction.OnDismissError)

        assertThat(viewModel.current.error).isNull()
        assertThat(viewModel.current.details).isNotNull()
    }

    @Test
    fun givenMovieNotInWatchlist_whenToggled_thenAddedAndStateFlips() = runTest {
        val viewModel = viewModel(movieId = 7)

        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isTrue()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
        assertThat(user.callsTo(UserRemoteFunction.AddMovieToAccountWatchlist)).containsExactly(listOf<Any?>(7))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenMovieInWatchlist_whenToggled_thenRemovedAndStateFlips() = runTest {
        movies.accountStates = MovieAccountStates(id = 0, favorite = false, watchlist = true)
        val viewModel = viewModel(movieId = 7)

        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isFalse()
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).containsExactly(listOf<Any?>(7))
    }

    @Test
    fun givenWatchlistToggleFails_whenToggled_thenStateUnchangedAndErrorShown() = runTest {
        user.fail(UserRemoteFunction.AddMovieToAccountWatchlist, DataError.Network.NoInternet)
        val viewModel = viewModel()

        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isFalse()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
        assertThat(viewModel.current.error).isEqualTo(UiError.NoInternet)
    }

    @Test
    fun givenToggleInFlight_whenClickedAgain_thenOnlyOneRequestIsMade() = runTest {
        val viewModel = viewModel()
        user.hold()

        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)
        assertThat(viewModel.current.isWatchlistLoading).isTrue()
        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)
        user.release()

        assertThat(user.callCount(UserRemoteFunction.AddMovieToAccountWatchlist)).isEqualTo(1)
        assertThat(viewModel.current.isInWatchlist).isTrue()
    }

    @Test
    fun givenLoggedOut_whenTogglingWatchlistOrFavorite_thenNothingIsRequested() = runTest {
        movies.fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
        val viewModel = viewModel()

        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)
        viewModel.onAction(MovieDetailsAction.OnFavoriteClick)

        assertThat(user.callCount(UserRemoteFunction.AddMovieToAccountWatchlist)).isEqualTo(0)
        assertThat(user.callCount(UserRemoteFunction.AddMovieToFavorites)).isEqualTo(0)
    }

    @Test
    fun givenFavoriteToggleWhileWatchlistToggleInFlight_whenWatchlistFinishes_thenFavoriteStaysLoading() = runTest {
        val viewModel = viewModel()
        user.hold()
        viewModel.onAction(MovieDetailsAction.OnFavoriteClick)
        viewModel.onAction(MovieDetailsAction.OnWatchlistClick)
        assertThat(viewModel.current.isFavoriteLoading).isTrue()
        assertThat(viewModel.current.isWatchlistLoading).isTrue()

        user.release()

        assertThat(viewModel.current.isFavorite).isTrue()
        assertThat(viewModel.current.isInWatchlist).isTrue()
        assertThat(viewModel.current.isFavoriteLoading).isFalse()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
    }

    @Test
    fun givenFavoriteMovie_whenToggled_thenRemovedFromFavorites() = runTest {
        movies.accountStates = MovieAccountStates(id = 0, favorite = true, watchlist = false)
        val viewModel = viewModel(movieId = 3)

        viewModel.onAction(MovieDetailsAction.OnFavoriteClick)

        assertThat(viewModel.current.isFavorite).isFalse()
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromFavorites)).containsExactly(listOf<Any?>(3))
    }

    @Test
    fun givenPersonAndBackActions_whenHandled_thenEachEmitsItsEventOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MovieDetailsAction.OnPersonClick(personId = 55))
            assertThat(awaitItem()).isEqualTo(MovieDetailsEvent.NavigateToActor(55))

            viewModel.onAction(MovieDetailsAction.OnBack)
            assertThat(awaitItem()).isEqualTo(MovieDetailsEvent.NavigateBack)
            expectNoEvents()
        }
    }

    @Test
    fun givenNoUnexpectedEvents_whenCreatedAndRetried_thenNoNavigationIsEmitted() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MovieDetailsAction.OnRetry)
            expectNoEvents()
        }
        assertThat(viewModel.current.cast.isEmpty()).isFalse()
    }
}
