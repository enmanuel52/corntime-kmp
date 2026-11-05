package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EpisodeDetailsViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()

    private fun viewModel() = EpisodeDetailsViewModel(tvShowId = 4, seasonNumber = 2, episodeNumber = 3, dataSource = tv)

    @Test
    fun givenSuccess_whenCreated_thenThatEpisodeIsRequestedAndLoaded() = runTest {
        val state = viewModel().state.value

        assertThat(state.details?.seasonNumber).isEqualTo(2)
        assertThat(state.details?.episodeNumber).isEqualTo(3)
        assertThat(tv.callsTo(TvRemoteFunction.GetEpisodeDetails)).isEqualTo(listOf(listOf<Any?>(4, 2, 3)))
        assertThat(state.error).isNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingUntilFinished() = runTest {
        tv.hold()
        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.guestStars).isEmpty()

        tv.release()

        assertThat(viewModel.state.value.details).isNotNull()
    }

    @Test
    fun givenFailure_whenCreated_thenErrorThenRetryRecovers() = runTest {
        tv.fail(TvRemoteFunction.GetEpisodeDetails, DataError.Network.RequestTimeout)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Timeout)

        tv.recover(TvRemoteFunction.GetEpisodeDetails)
        viewModel.onAction(EpisodeDetailsAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.details).isNotNull()
    }

    @Test
    fun givenError_whenDismissed_thenCleared() = runTest {
        tv.fail(TvRemoteFunction.GetEpisodeDetails)
        val viewModel = viewModel()

        viewModel.onAction(EpisodeDetailsAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenAGuestStar_whenClicked_thenEventCarriesTheirNameAndPhoto() = runTest {
        val viewModel = viewModel()
        val person = viewModel.state.value.guestStars.first()

        viewModel.events.test {
            viewModel.onAction(EpisodeDetailsAction.OnPersonClick(person.id))

            assertThat(awaitItem())
                .isEqualTo(EpisodeDetailsEvent.NavigateToActor(person.id, person.name, person.profilePath))
        }
    }

    @Test
    fun givenAnUnknownPerson_whenClicked_thenStillNavigatesWithBlankPreview() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(EpisodeDetailsAction.OnPersonClick(personId = 999_999))

            assertThat(awaitItem()).isEqualTo(EpisodeDetailsEvent.NavigateToActor(999_999, "", null))
        }
    }

    @Test
    fun givenBack_whenHandled_thenNavigatesBackOnce() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(EpisodeDetailsAction.OnBack)

            assertThat(awaitItem()).isEqualTo(EpisodeDetailsEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
