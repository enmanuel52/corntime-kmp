package com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons

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

class SeasonsViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()

    private fun viewModel(tvShowId: Int = 4) = SeasonsViewModel(tvShowId, tv)

    @Test
    fun givenSuccess_whenCreated_thenSeasonsOfThatShowAreLoaded() = runTest {
        val state = viewModel(tvShowId = 4).state.value

        assertThat(state.details?.id).isEqualTo(4)
        assertThat(state.seasons.map { it.seasonNumber }).isEqualTo(listOf(1, 2, 3, 4, 5))
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingUntilFinished() = runTest {
        tv.hold()
        val viewModel = viewModel()
        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.seasons).isEmpty()

        tv.release()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.details).isNotNull()
    }

    @Test
    fun givenFailure_whenCreated_thenErrorThenRetryRecovers() = runTest {
        tv.fail(TvRemoteFunction.GetTvDetails, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error).isEqualTo(UiError.NoInternet)

        tv.recover(TvRemoteFunction.GetTvDetails)
        viewModel.onAction(SeasonsAction.OnRetry)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.seasons.size).isEqualTo(5)
    }

    @Test
    fun givenRetryWhileLoading_whenRequested_thenIgnored() = runTest {
        tv.hold()
        val viewModel = viewModel()

        viewModel.onAction(SeasonsAction.OnRetry)
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.GetTvDetails)).isEqualTo(1)
    }

    @Test
    fun givenError_whenDismissed_thenCleared() = runTest {
        tv.fail(TvRemoteFunction.GetTvDetails)
        val viewModel = viewModel()

        viewModel.onAction(SeasonsAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenSeasonToggle_whenToggledTwice_thenOpensThenCloses() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(SeasonsAction.OnSeasonToggle(seasonId = 11))
        assertThat(viewModel.state.value.expandedSeasonId).isEqualTo(11)

        viewModel.onAction(SeasonsAction.OnSeasonToggle(seasonId = 11))
        assertThat(viewModel.state.value.expandedSeasonId).isNull()
    }

    @Test
    fun givenOneSeasonOpen_whenAnotherToggled_thenOnlyTheNewOneIsOpen() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(SeasonsAction.OnSeasonToggle(seasonId = 11))

        viewModel.onAction(SeasonsAction.OnSeasonToggle(seasonId = 12))

        assertThat(viewModel.state.value.expandedSeasonId).isEqualTo(12)
    }

    @Test
    fun givenClickAndBack_whenHandled_thenEmitsEvents() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SeasonsAction.OnSeasonClick(seasonNumber = 2))
            assertThat(awaitItem()).isEqualTo(SeasonsEvent.NavigateToEpisodes(2))

            viewModel.onAction(SeasonsAction.OnBack)
            assertThat(awaitItem()).isEqualTo(SeasonsEvent.NavigateBack)
            expectNoEvents()
        }
    }
}
