package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import assertk.assertThat
import assertk.assertions.containsExactly
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.tvshows.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class EpisodesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<EpisodesAction>()
    private val season = FakeTvData.DEFAULT_SEASON_DETAILS.let { fake ->
        fake.copy(
            episodes = fake.episodes.map {
                it.copy(name = "Episode title ${it.episodeNumber}", overview = "Plot of episode ${it.episodeNumber}")
            },
        )
    }

    private fun show(state: EpisodesState) {
        rule.setThemedContent { EpisodesScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(1_000)
    }

    private fun loaded(expanded: Int? = null) =
        EpisodesState(tvShowId = 1, seasonNumber = 1, seasonDetails = season, expandedEpisodeId = expanded)

    @Test
    fun givenLoadedEpisodes_whenShown_thenTheSeasonNameAndEpisodesAreListed() {
        show(loaded())

        rule.onNodeWithText(season.name).assertIsDisplayed()
        rule.onNodeWithText("1. Episode title 1").assertIsDisplayed()
        rule.onNodeWithText("2. Episode title 2").assertIsDisplayed()
    }

    @Test
    fun givenFirstLoad_whenShown_thenNoEpisodesAreListedYet() {
        show(EpisodesState(tvShowId = 1, seasonNumber = 1, isLoading = true))

        rule.onNodeWithText("1. Episode title 1").assertDoesNotExist()
        rule.onNodeWithText("Episodes").assertIsDisplayed()
    }

    @Test
    fun givenFailureWithoutData_whenShown_thenFullScreenErrorAndRetry() {
        show(EpisodesState(tvShowId = 1, seasonNumber = 1, error = UiError.Timeout))

        rule.onNodeWithText("Waiting time for response has ended, check your internet connection and try again")
            .assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertThat(actions).containsExactly(EpisodesAction.OnRetry)
    }

    @Test
    fun givenFailureWithData_whenShown_thenDialogOverTheListAndAcceptDismisses() {
        show(loaded().copy(error = UiError.NoInternet))

        rule.onNodeWithText("No internet connection, check your network and try again").assertIsDisplayed()
        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(EpisodesAction.OnDismissError)
    }

    @Test
    fun givenExpandedEpisode_whenShown_thenOnlyItsOverviewIsVisible() {
        show(loaded(expanded = season.episodes[1].id))

        rule.onNodeWithText("Plot of episode 2").assertIsDisplayed()
        rule.onNodeWithText("Plot of episode 1").assertDoesNotExist()
    }

    @Test
    fun givenEpisode_whenTapped_thenDispatchesEpisodeClickWithItsNumber() {
        show(loaded())

        rule.onNodeWithText("2. Episode title 2").performClick()

        assertThat(actions).containsExactly(EpisodesAction.OnEpisodeClick(2))
    }

    @Test
    fun givenEpisode_whenLongPressed_thenDispatchesToggleWithItsId() {
        show(loaded())

        rule.onNodeWithText("1. Episode title 1").performTouchInput { longClick() }

        assertThat(actions).containsExactly(EpisodesAction.OnEpisodeToggle(season.episodes[0].id))
    }

    @Test
    fun givenBackIcon_whenTapped_thenDispatchesBack() {
        show(loaded())

        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions).containsExactly(EpisodesAction.OnBack)
    }
}
