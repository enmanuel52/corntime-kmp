package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class TvShowSectionScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { TvShowSectionRobot(rule) }
    private val tvShows = FakeTvData.TV_SHOWS

    private fun state(
        section: TvShowsSection = TvShowsSection.Popular,
        items: List<TvShow> = tvShows,
        status: PagingStatus = PagingStatus.Idle,
    ) = TvShowSectionState(section, PagingState(items = items, status = status))

    @Test
    fun givenLoadedTvShows_whenShown_thenEveryTitleIsVisible() {
        robot.show(state()).assertTextVisible(tvShows[0].name).assertTextVisible(tvShows[1].name)
    }

    @Test
    fun givenPopular_whenShown_thenTheTopBarTitleNamesIt() {
        robot.show(state(TvShowsSection.Popular)).assertTextVisible("Popular TV Shows")
    }

    @Test
    fun givenTopRated_whenShown_thenTheTopBarTitleNamesIt() {
        robot.show(state(TvShowsSection.TopRated)).assertTextVisible("Top Rated TV Shows")
    }

    @Test
    fun givenOnTheAir_whenShown_thenTheTopBarTitleNamesIt() {
        robot.show(state(TvShowsSection.OnTheAir)).assertTextVisible("On The Air TV Shows")
    }

    @Test
    fun givenAiringToday_whenShown_thenTheTopBarTitleNamesIt() {
        robot.show(state(TvShowsSection.AiringToday)).assertTextVisible("Airing Today TV Shows")
    }

    @Test
    fun givenFinishedWithNoTvShows_whenShown_thenEmptyMessage() {
        robot.show(state(items = emptyList(), status = PagingStatus.EndReached))
            .assertTextVisible("No TV shows found")
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenFullScreenErrorAndRetryDispatchesLoad() {
        robot.show(state(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet)))
            .assertTextVisible("No internet connection, check your network and try again")
            .clickRetry()

        assertThat(robot.actions).containsExactly(TvShowSectionAction.OnLoadNextPage)
    }

    @Test
    fun givenLaterPageFailed_whenShown_thenListStaysWithAFooterRetry() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.ServerError)))
            .assertTextVisible(tvShows[0].name)
            .assertRetryFooterVisible()
            .clickRetryFooter()

        assertThat(robot.actions).containsExactly(TvShowSectionAction.OnLoadNextPage)
    }

    @Test
    fun givenNoFailure_whenShown_thenThereIsNoRetryFooter() {
        robot.show(state()).assertRetryFooterAbsent()
    }

    @Test
    fun givenPoster_whenClicked_thenDispatchesTvShowClick() {
        robot.show(state()).clickTvShowAt(1)

        assertThat(robot.actions.filterNot { it == TvShowSectionAction.OnLoadNextPage })
            .containsExactly(TvShowSectionAction.OnTvShowClick(tvShows[1].id))
    }

    @Test
    fun givenBackIcon_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions.filterNot { it == TvShowSectionAction.OnLoadNextPage })
            .isEqualTo(listOf(TvShowSectionAction.OnBack))
    }
}
