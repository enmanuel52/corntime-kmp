package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TvShowSectionViewModelTest : MainDispatcherTest() {

    private val tv = FakeTvRemoteDataSource()
    private val perPage = FakeTvData.TV_SHOWS.size

    private fun viewModel(section: TvShowsSection = TvShowsSection.Popular) =
        TvShowSectionViewModel(section, tv)

    @Test
    fun givenEachSection_whenCreated_thenFirstPageComesFromItsOwnEndpoint() = runTest {
        val expected = mapOf(
            TvShowsSection.OnTheAir to TvRemoteFunction.GetOnTheAirTv,
            TvShowsSection.TopRated to TvRemoteFunction.GetTopRatedTv,
            TvShowsSection.AiringToday to TvRemoteFunction.GetAiringTodayTv,
            TvShowsSection.Popular to TvRemoteFunction.GetPopularTv,
        )
        expected.forEach { (section, function) ->
            val source = FakeTvRemoteDataSource()

            val state = TvShowSectionViewModel(section, source).state.value

            assertThat(state.section).isEqualTo(section)
            assertThat(state.paging.items.size).isEqualTo(perPage)
            assertThat(source.callCount(function)).isEqualTo(1)
        }
    }

    @Test
    fun givenSlowNetwork_whenCreated_thenLoadingFirstPageThenItems() = runTest {
        tv.hold()
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.isLoadingFirstPage).isTrue()

        tv.release()

        assertThat(viewModel.state.value.paging.isLoadingFirstPage).isFalse()
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenSeveralPages_whenLoadingNextPage_thenItemsAreAppended() = runTest {
        tv.totalPages = 3
        val viewModel = viewModel()

        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage * 2)
        assertThat(tv.callsTo(TvRemoteFunction.GetPopularTv)).isEqualTo(listOf(listOf<Any?>(1), listOf<Any?>(2)))
    }

    @Test
    fun givenLastPage_whenLoadingNextPage_thenNoMoreRequests() = runTest {
        tv.totalPages = 2
        val viewModel = viewModel()
        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.endReached).isTrue()

        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)
        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)

        assertThat(tv.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(2)
    }

    @Test
    fun givenNoShows_whenCreated_thenEmptyAndFinished() = runTest {
        tv.totalPages = 0

        val paging = viewModel().state.value.paging

        assertThat(paging.items).isEmpty()
        assertThat(paging.isEmpty).isTrue()
    }

    @Test
    fun givenFirstPageFails_whenCreated_thenErrorThenRetryRecovers() = runTest {
        tv.fail(TvRemoteFunction.GetPopularTv, DataError.Network.NoInternet)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.paging.error).isEqualTo(DataError.Network.NoInternet)

        tv.recover(TvRemoteFunction.GetPopularTv)
        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.error).isNull()
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
    }

    @Test
    fun givenLaterPageFails_whenRetried_thenLoadedItemsAreKeptAndSamePageRequestedAgain() = runTest {
        tv.totalPages = 3
        val viewModel = viewModel()
        tv.fail(TvRemoteFunction.GetPopularTv)

        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)
        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage)
        assertThat(viewModel.state.value.paging.error).isEqualTo(DataError.Network.Unknown)

        tv.recover(TvRemoteFunction.GetPopularTv)
        viewModel.onAction(TvShowSectionAction.OnLoadNextPage)

        assertThat(viewModel.state.value.paging.items.size).isEqualTo(perPage * 2)
        assertThat(tv.callsTo(TvRemoteFunction.GetPopularTv).map { it.single() })
            .isEqualTo(listOf<Any?>(1, 2, 2))
    }

    @Test
    fun givenLoadingNextPage_whenTriggeredRepeatedly_thenOnlyOneRequestIsMade() = runTest {
        tv.totalPages = 5
        val viewModel = viewModel()
        tv.hold()

        repeat(5) { viewModel.onAction(TvShowSectionAction.OnLoadNextPage) }
        tv.release()

        assertThat(tv.callCount(TvRemoteFunction.GetPopularTv)).isEqualTo(2)
    }

    @Test
    fun givenClickAndBack_whenHandled_thenEmitsNavigationEvents() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(TvShowSectionAction.OnTvShowClick(tvShowId = 9))
            assertThat(awaitItem()).isEqualTo(TvShowSectionEvent.NavigateToDetails(9))

            viewModel.onAction(TvShowSectionAction.OnBack)
            assertThat(awaitItem()).isEqualTo(TvShowSectionEvent.NavigateBack)
        }
    }
}
