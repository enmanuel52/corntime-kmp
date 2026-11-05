package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EpisodesViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()

    private fun viewModel(tvShowId: Int = 4, seasonNumber: Int = 2) = EpisodesViewModel(tvShowId, seasonNumber, tv)

    @Test
    fun givenSuccess_whenCreated_thenEpisodesOfThatSeasonAreRequestedAndLoaded() = runTest {
        val state = viewModel(tvShowId = 4, seasonNumber = 2).state.value

        assertThat(state.seasonDetails?.seasonNumber).isEqualTo(2)
        assertThat(state.episodes.size).isEqualTo(7)
        assertThat(tv.callsTo(TvRemoteFunction.GetSeasonDetails)).isEqualTo(listOf(listOf<Any?>(4, 2)))
        assertThat(state.error).isNull()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingUntilFinished() = runTest {
        tv.hold()
        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.episodes).isEmpty()

        tv.release()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.episodes.size).isEqualTo(7)
    }

    @Test
    fun givenFailure_whenCreated_thenErrorThenRetryRecovers() = runTest {
        tv.fail(TvRemoteFunction.GetSeasonDetails, DataError.Network.ServerError)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Server)

        tv.recover(TvRemoteFunction.GetSeasonDetails)
        viewModel.onAction(EpisodesAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.episodes.size).isEqualTo(7)
    }

    @Test
    fun givenRetryWhileLoading_whenRequested_thenIgnored() = runTest {
        tv.hold()
        val viewModel = viewModel()

        viewModel.onAction(EpisodesAction.OnRetry)
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.GetSeasonDetails)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenCleared() = runTest {
        tv.fail(TvRemoteFunction.GetSeasonDetails)
        val viewModel = viewModel()

        viewModel.onAction(EpisodesAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenEpisodeToggle_whenToggledTwice_thenOpensThenCloses() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(EpisodesAction.OnEpisodeToggle(episodeId = 101))
        assertThat(viewModel.state.value.expandedEpisodeId).isEqualTo(101)

        viewModel.onAction(EpisodesAction.OnEpisodeToggle(episodeId = 101))
        assertThat(viewModel.state.value.expandedEpisodeId).isNull()
    }

    @Test
    fun givenOneEpisodeOpen_whenAnotherToggled_thenOnlyTheNewOneIsOpen() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(EpisodesAction.OnEpisodeToggle(episodeId = 101))

        viewModel.onAction(EpisodesAction.OnEpisodeToggle(episodeId = 102))

        assertThat(viewModel.state.value.expandedEpisodeId).isEqualTo(102)
    }

    @Test
    fun givenClickAndBack_whenHandled_thenEmitsEvents() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(EpisodesAction.OnEpisodeClick(episodeNumber = 5))
            assertThat(awaitItem()).isEqualTo(EpisodesEvent.NavigateToEpisodeDetails(5))

            viewModel.onAction(EpisodesAction.OnBack)
            assertThat(awaitItem()).isEqualTo(EpisodesEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
