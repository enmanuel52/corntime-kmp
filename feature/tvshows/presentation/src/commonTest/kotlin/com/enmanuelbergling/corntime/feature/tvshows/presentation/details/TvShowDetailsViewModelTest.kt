package com.enmanuelbergling.corntime.feature.tvshows.presentation.details

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
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.tvshows.domain.details.TvShowDetailsChain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TvShowDetailsViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()

    private fun viewModel(tvShowId: Int = 7) =
        TvShowDetailsViewModel(tvShowId, TvShowDetailsChain(tv), tv)

    private val TvShowDetailsViewModel.current get() = state.value

    @Test
    fun givenAllEndpointsSucceed_whenCreated_thenDetailsCreditsAndAccountStatesLoadedForThatMovie() = runTest {
        val state = viewModel(tvShowId = 7).current

        assertThat(state.details?.id).isEqualTo(7)
        assertThat(state.credits?.id).isEqualTo(7)
        assertThat(state.accountStates?.id).isEqualTo(7)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.canManageAccount).isTrue()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenIsLoadingUntilFinished() = runTest {
        tv.hold()
        val viewModel = viewModel()
        assertThat(viewModel.current.isLoading).isTrue()
        assertThat(viewModel.current.details).isNull()

        tv.release()

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
        tv.fail(TvRemoteFunction.GetTvDetails, DataError.Network.ServerError)
        val viewModel = viewModel()

        assertThat(viewModel.current.error).isEqualTo(UiError.Server)
        assertThat(viewModel.current.details).isNull()
        assertThat(tv.callCount(TvRemoteFunction.GetTvCredits)).isEqualTo(0)

        tv.recover(TvRemoteFunction.GetTvDetails)
        viewModel.onAction(TvShowDetailsAction.OnRetry)

        assertThat(viewModel.current.error).isNull()
        assertThat(viewModel.current.details).isNotNull()
    }

    @Test
    fun givenCreditsFail_whenRetried_thenDetailsAreNotRequestedAgain() = runTest {
        tv.fail(TvRemoteFunction.GetTvCredits)
        val viewModel = viewModel()
        assertThat(viewModel.current.details).isNotNull()
        assertThat(viewModel.current.error).isEqualTo(UiError.Unknown)

        tv.recover(TvRemoteFunction.GetTvCredits)
        viewModel.onAction(TvShowDetailsAction.OnRetry)

        assertThat(tv.callCount(TvRemoteFunction.GetTvDetails)).isEqualTo(1)
        assertThat(viewModel.current.credits).isNotNull()
    }

    @Test
    fun givenLoggedOut_whenCreated_thenNoErrorAndAccountActionsUnavailable() = runTest {
        tv.fail(TvRemoteFunction.GetTvAccountStates, DataError.Network.Unauthorized)

        val state = viewModel().current

        assertThat(state.error).isNull()
        assertThat(state.details).isNotNull()
        assertThat(state.credits).isNotNull()
        assertThat(state.accountStates).isNull()
        assertThat(state.canManageAccount).isFalse()
    }

    @Test
    fun givenUnauthorizedOnDetailsItself_whenCreated_thenItIsStillShownAsAnError() = runTest {
        tv.fail(TvRemoteFunction.GetTvDetails, DataError.Network.Unauthorized)

        assertThat(viewModel().current.error).isEqualTo(UiError.Unauthorized)
    }

    @Test
    fun givenRetryWhileLoading_whenRequested_thenItIsIgnored() = runTest {
        tv.hold()
        val viewModel = viewModel()

        viewModel.onAction(TvShowDetailsAction.OnRetry)
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.GetTvDetails)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenClearedAndDataKept() = runTest {
        tv.fail(TvRemoteFunction.GetTvCredits)
        val viewModel = viewModel()

        viewModel.onAction(TvShowDetailsAction.OnDismissError)

        assertThat(viewModel.current.error).isNull()
        assertThat(viewModel.current.details).isNotNull()
    }

    @Test
    fun givenShowNotInWatchlist_whenToggled_thenAddedAndStateFlips() = runTest {
        val viewModel = viewModel(tvShowId = 7)

        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isTrue()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
        assertThat(tv.callsTo(TvRemoteFunction.AddTvToAccountWatchlist)).containsExactly(listOf<Any?>(7))
        assertThat(tv.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenShowInWatchlist_whenToggled_thenRemovedAndStateFlips() = runTest {
        tv.accountStates = TvAccountStates(id = 0, favorite = false, watchlist = true)
        val viewModel = viewModel(tvShowId = 7)

        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isFalse()
        assertThat(tv.callsTo(TvRemoteFunction.RemoveTvFromAccountWatchlist)).containsExactly(listOf<Any?>(7))
    }

    @Test
    fun givenWatchlistToggleFails_whenToggled_thenStateUnchangedAndErrorShown() = runTest {
        tv.fail(TvRemoteFunction.AddTvToAccountWatchlist, DataError.Network.NoInternet)
        val viewModel = viewModel()

        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)

        assertThat(viewModel.current.isInWatchlist).isFalse()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
        assertThat(viewModel.current.error).isEqualTo(UiError.NoInternet)
    }

    @Test
    fun givenToggleInFlight_whenClickedAgain_thenOnlyOneRequestIsMade() = runTest {
        val viewModel = viewModel()
        tv.hold()

        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)
        assertThat(viewModel.current.isWatchlistLoading).isTrue()
        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.AddTvToAccountWatchlist)).isEqualTo(1)
        assertThat(viewModel.current.isInWatchlist).isTrue()
    }

    @Test
    fun givenLoggedOut_whenTogglingWatchlistOrFavorite_thenNothingIsRequested() = runTest {
        tv.fail(TvRemoteFunction.GetTvAccountStates, DataError.Network.Unauthorized)
        val viewModel = viewModel()

        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)
        viewModel.onAction(TvShowDetailsAction.OnFavoriteClick)

        assertThat(tv.callCount(TvRemoteFunction.AddTvToAccountWatchlist)).isEqualTo(0)
        assertThat(tv.callCount(TvRemoteFunction.AddTvToFavorites)).isEqualTo(0)
    }

    @Test
    fun givenFavoriteToggleWhileWatchlistToggleInFlight_whenWatchlistFinishes_thenFavoriteStaysLoading() = runTest {
        val viewModel = viewModel()
        tv.hold()
        viewModel.onAction(TvShowDetailsAction.OnFavoriteClick)
        viewModel.onAction(TvShowDetailsAction.OnWatchlistClick)
        assertThat(viewModel.current.isFavoriteLoading).isTrue()
        assertThat(viewModel.current.isWatchlistLoading).isTrue()

        tv.release()

        assertThat(viewModel.current.isFavorite).isTrue()
        assertThat(viewModel.current.isInWatchlist).isTrue()
        assertThat(viewModel.current.isFavoriteLoading).isFalse()
        assertThat(viewModel.current.isWatchlistLoading).isFalse()
    }

    @Test
    fun givenFavoriteShow_whenToggled_thenRemovedFromFavorites() = runTest {
        tv.accountStates = TvAccountStates(id = 0, favorite = true, watchlist = false)
        val viewModel = viewModel(tvShowId = 3)

        viewModel.onAction(TvShowDetailsAction.OnFavoriteClick)

        assertThat(viewModel.current.isFavorite).isFalse()
        assertThat(tv.callsTo(TvRemoteFunction.RemoveTvFromFavorites)).containsExactly(listOf<Any?>(3))
    }

    @Test
    fun givenACastMember_whenClicked_thenTheEventCarriesTheirNameAndPhoto() = runTest {
        val viewModel = viewModel()
        val person = viewModel.current.cast.first()

        viewModel.events.test {
            viewModel.onAction(TvShowDetailsAction.OnPersonClick(person.id))

            assertThat(awaitItem())
                .isEqualTo(TvShowDetailsEvent.NavigateToActor(person.id, person.name, person.profilePath))
        }
    }

    @Test
    fun givenAnUnknownPerson_whenClicked_thenTheEventStillNavigatesWithBlankPreview() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowDetailsAction.OnPersonClick(personId = 999_999))

            assertThat(awaitItem()).isEqualTo(TvShowDetailsEvent.NavigateToActor(999_999, "", null))
        }
    }

    @Test
    fun givenPersonAndBackActions_whenHandled_thenEachEmitsItsEventOnce() = runTest {
        val viewModel = viewModel()
        val person = viewModel.current.cast.first()

        viewModel.events.test {
            viewModel.onAction(TvShowDetailsAction.OnPersonClick(personId = person.id))
            assertThat(awaitItem()).isEqualTo(TvShowDetailsEvent.NavigateToActor(person.id, person.name, person.profilePath))

            viewModel.onAction(TvShowDetailsAction.OnBack)
            assertThat(awaitItem()).isEqualTo(TvShowDetailsEvent.NavigateBack)
            expectNoEvents()
        }
    }

    @Test
    fun givenNoUnexpectedEvents_whenCreatedAndRetried_thenNoNavigationIsEmitted() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowDetailsAction.OnRetry)
            expectNoEvents()
        }
        assertThat(viewModel.current.cast.isEmpty()).isFalse()
    }

    @Test
    fun givenSeasonsAction_whenHandled_thenNavigatesToSeasonsAndEpisodes() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowDetailsAction.OnSeeAllSeasons)
            assertThat(awaitItem()).isEqualTo(TvShowDetailsEvent.NavigateToSeasons)

            viewModel.onAction(TvShowDetailsAction.OnSeasonClick(seasonNumber = 3))
            assertThat(awaitItem()).isEqualTo(TvShowDetailsEvent.NavigateToEpisodes(3))
            expectNoEvents()
        }
    }

    @Test
    fun givenDetails_whenCreated_thenSeasonsComeFromTheDetails() = runTest {
        val state = viewModel().current

        assertThat(state.details!!.seasons.map { it.seasonNumber }).isEqualTo(listOf(1, 2, 3, 4, 5))
    }
}
