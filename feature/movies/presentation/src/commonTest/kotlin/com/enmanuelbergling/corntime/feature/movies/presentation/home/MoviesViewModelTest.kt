package com.enmanuelbergling.corntime.feature.movies.presentation.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.movies.domain.home.MoviesHomeChain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoviesViewModelTest : MainDispatcherTest() {

    private val movies = FakeMovieRemoteDataSource()
    private val userRemote = FakeUserRemoteDataSource()
    private val userPreferences = FakeUserPreferenceDataSource()

    private fun viewModel() = MoviesViewModel(
        homeChain = MoviesHomeChain(movies),
        syncUserDetails = SyncUserDetailsUseCase(userRemote, userPreferences),
        userPreferences = userPreferences,
    )

    @Test
    fun givenAllEndpointsSucceed_whenCreated_thenEverySectionIsLoadedAndNotLoading() = runTest {
        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        MovieSection.entries.forEach { assertThat(state.sections.getValue(it)).isNotEmpty() }
        assertThat(state.isFeedEmpty).isFalse()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingIsTrueUntilTheChainFinishes() = runTest {
        movies.hold()

        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.isFeedEmpty).isTrue()

        movies.release()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isFeedEmpty).isFalse()
    }

    @Test
    fun givenFirstEndpointFails_whenCreated_thenErrorAndNothingLoaded() = runTest {
        movies.fail(MovieRemoteFunction.GetUpcomingMovies, DataError.Network.RequestTimeout)

        val state = viewModel().state.value

        assertThat(state.error).isEqualTo(UiError.Timeout)
        assertThat(state.isLoading).isFalse()
        assertThat(state.isFeedEmpty).isTrue()
    }

    @Test
    fun givenThirdEndpointFails_whenCreated_thenEarlierSectionsAreKept() = runTest {
        movies.fail(MovieRemoteFunction.GetNowPlayingMovies, DataError.Network.NoInternet)

        val state = viewModel().state.value

        assertThat(state.error).isEqualTo(UiError.NoInternet)
        assertThat(state.sections.getValue(MovieSection.Upcoming)).isNotEmpty()
        assertThat(state.sections.getValue(MovieSection.TopRated)).isNotEmpty()
        assertThat(state.sections[MovieSection.NowPlaying].orEmpty()).isEmpty()
        assertThat(state.sections[MovieSection.Popular].orEmpty()).isEmpty()
    }

    @Test
    fun givenError_whenRetried_thenRecoversFetchingOnlyMissingSections() = runTest {
        movies.fail(MovieRemoteFunction.GetNowPlayingMovies)
        val viewModel = viewModel()

        movies.recover(MovieRemoteFunction.GetNowPlayingMovies)
        viewModel.onAction(MoviesAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.sections.getValue(MovieSection.Popular)).isNotEmpty()
        assertThat(movies.callCount(MovieRemoteFunction.GetUpcomingMovies)).isEqualTo(1)
        assertThat(movies.callCount(MovieRemoteFunction.GetNowPlayingMovies)).isEqualTo(2)
    }

    @Test
    fun givenRetryWhileLoading_whenRequestedAgain_thenItIsIgnored() = runTest {
        movies.hold()
        val viewModel = viewModel()

        viewModel.onAction(MoviesAction.OnRetry)
        viewModel.onAction(MoviesAction.OnRetry)
        movies.release()

        assertThat(movies.callCount(MovieRemoteFunction.GetUpcomingMovies)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenErrorIsClearedButDataStays() = runTest {
        movies.fail(MovieRemoteFunction.GetPopularMovies)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Unknown)

        viewModel.onAction(MoviesAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.sections.getValue(MovieSection.Upcoming)).isNotEmpty()
    }

    @Test
    fun givenEmptyEndpoints_whenCreated_thenFeedIsEmptyWithoutError() = runTest {
        movies.totalPages = 0

        val state = viewModel().state.value

        assertThat(state.error).isNull()
        assertThat(state.isFeedEmpty).isTrue()
        assertThat(state.orderedSections).isEmpty()
    }

    @Test
    fun givenLoadedFeed_whenOrdering_thenNowPlayingIsFirstThenTheRestInDeclarationOrder() = runTest {
        val sections = viewModel().state.value.orderedSections.map { it.section }

        assertThat(sections).containsExactly(
            MovieSection.NowPlaying,
            MovieSection.Upcoming,
            MovieSection.TopRated,
            MovieSection.Popular,
        )
    }

    @Test
    fun givenLoggedOut_whenCreated_thenNoUserAndSyncFailureIsNotShownAsError() = runTest {
        userRemote.fail(UserRemoteFunction.GetAccount, DataError.Network.Unauthorized)

        val state = viewModel().state.value

        assertThat(state.user).isNull()
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun givenLoggedInUser_whenCreated_thenAccountIsSyncedIntoState() = runTest {
        userRemote.account = FakeUserData.FAKE_USER

        val viewModel = viewModel()

        assertThat(viewModel.state.value.user).isEqualTo(FakeUserData.FAKE_USER)
        assertThat(viewModel.state.value.isLoggedIn).isTrue()
    }

    @Test
    fun givenLoggedInUser_whenLoggingOut_thenStateReflectsLoggedOut() = runTest {
        userPreferences.updateUser(FakeUserData.FAKE_USER)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoggedIn).isTrue()

        userPreferences.clear()

        assertThat(viewModel.state.value.isLoggedIn).isFalse()
    }

    @Test
    fun givenMovieClick_whenHandled_thenNavigatesToDetailsOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MoviesAction.OnMovieClick(movieId = 7))

            assertThat(awaitItem()).isEqualTo(MoviesEvent.NavigateToDetails(7))
            expectNoEvents()
        }
    }

    @Test
    fun givenSectionClick_whenHandled_thenNavigatesToThatSection() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(MoviesAction.OnSectionClick(MovieSection.TopRated))

            assertThat(awaitItem()).isEqualTo(MoviesEvent.NavigateToSection(MovieSection.TopRated))
        }
    }
}
