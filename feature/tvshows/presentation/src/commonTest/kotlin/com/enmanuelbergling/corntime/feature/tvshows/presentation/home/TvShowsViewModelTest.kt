package com.enmanuelbergling.corntime.feature.tvshows.presentation.home

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
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.tvshows.domain.home.TvShowsHomeChain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TvShowsViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()
    private val userRemote = FakeUserRemoteDataSource()
    private val userPreferences = FakeUserPreferenceDataSource()

    private fun viewModel() = TvShowsViewModel(
        homeChain = TvShowsHomeChain(tv),
        syncUserDetails = SyncUserDetailsUseCase(userRemote, userPreferences),
        userPreferences = userPreferences,
    )

    @Test
    fun givenAllEndpointsSucceed_whenCreated_thenEverySectionIsLoadedAndNotLoading() = runTest {
        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        TvShowsSection.entries.forEach { assertThat(state.sections.getValue(it)).isNotEmpty() }
        assertThat(state.isFeedEmpty).isFalse()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingIsTrueUntilTheChainFinishes() = runTest {
        tv.hold()

        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.isFeedEmpty).isTrue()

        tv.release()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isFeedEmpty).isFalse()
    }

    @Test
    fun givenFirstEndpointFails_whenCreated_thenErrorAndNothingLoaded() = runTest {
        tv.fail(TvRemoteFunction.GetPopularTv, DataError.Network.RequestTimeout)

        val state = viewModel().state.value

        assertThat(state.error).isEqualTo(UiError.Timeout)
        assertThat(state.isLoading).isFalse()
        assertThat(state.isFeedEmpty).isTrue()
    }

    @Test
    fun givenThirdEndpointFails_whenCreated_thenEarlierSectionsAreKept() = runTest {
        tv.fail(TvRemoteFunction.GetOnTheAirTv, DataError.Network.NoInternet)

        val state = viewModel().state.value

        assertThat(state.error).isEqualTo(UiError.NoInternet)
        assertThat(state.sections.getValue(TvShowsSection.Popular)).isNotEmpty()
        assertThat(state.sections.getValue(TvShowsSection.TopRated)).isNotEmpty()
        assertThat(state.sections[TvShowsSection.OnTheAir].orEmpty()).isEmpty()
        assertThat(state.sections[TvShowsSection.AiringToday].orEmpty()).isEmpty()
    }

    @Test
    fun givenError_whenRetried_thenRecoversFetchingOnlyMissingSections() = runTest {
        tv.fail(TvRemoteFunction.GetOnTheAirTv)
        val viewModel = viewModel()

        tv.recover(TvRemoteFunction.GetOnTheAirTv)
        viewModel.onAction(TvShowsAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.sections.getValue(TvShowsSection.AiringToday)).isNotEmpty()
        assertThat(tv.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(1)
        assertThat(tv.callCount(TvRemoteFunction.GetOnTheAirTv)).isEqualTo(2)
    }

    @Test
    fun givenRetryWhileLoading_whenRequestedAgain_thenItIsIgnored() = runTest {
        tv.hold()
        val viewModel = viewModel()

        viewModel.onAction(TvShowsAction.OnRetry)
        viewModel.onAction(TvShowsAction.OnRetry)
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenErrorIsClearedButDataStays() = runTest {
        tv.fail(TvRemoteFunction.GetAiringTodayTv)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Unknown)

        viewModel.onAction(TvShowsAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.sections.getValue(TvShowsSection.Popular)).isNotEmpty()
    }

    @Test
    fun givenEmptyEndpoints_whenCreated_thenFeedIsEmptyWithoutError() = runTest {
        tv.totalPages = 0

        val state = viewModel().state.value

        assertThat(state.error).isNull()
        assertThat(state.isFeedEmpty).isTrue()
        assertThat(state.orderedSections).isEmpty()
    }

    @Test
    fun givenLoadedFeed_whenOrdering_thenSectionsFollowDeclarationOrder() = runTest {
        val sections = viewModel().state.value.orderedSections.map { it.section }

        assertThat(sections).containsExactly(
            TvShowsSection.Popular,
            TvShowsSection.TopRated,
            TvShowsSection.OnTheAir,
            TvShowsSection.AiringToday,
        )
    }

    @Test
    fun givenLoadedFeed_whenOrdering_thenTopRatedIsSortedByScoreDescending() = runTest {
        val topRated = viewModel().state.value.orderedSections
            .first { it.section == TvShowsSection.TopRated }.tvShows

        assertThat(topRated.map(TvShow::voteAverage)).isEqualTo(FakeTvData.TV_SHOWS.map(TvShow::voteAverage).sortedDescending())
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
    fun givenTvShowClick_whenHandled_thenNavigatesToDetailsOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowsAction.OnTvShowClick(tvShowId = 7))

            assertThat(awaitItem()).isEqualTo(TvShowsEvent.NavigateToDetails(7))
            expectNoEvents()
        }
    }

    @Test
    fun givenSectionClick_whenHandled_thenNavigatesToThatSection() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowsAction.OnSectionClick(TvShowsSection.TopRated))

            assertThat(awaitItem()).isEqualTo(TvShowsEvent.NavigateToSection(TvShowsSection.TopRated))
        }
    }
}
