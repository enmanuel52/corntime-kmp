package com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
class SeasonsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<SeasonsAction>()
    private val details = FakeTvData.DEFAULT_TV_SHOW_DETAILS.let { fake ->
        fake.copy(
            seasons = fake.seasons.map {
                it.copy(name = "Season ${it.seasonNumber}", overview = "Overview of season ${it.seasonNumber}")
            },
        )
    }

    private fun show(state: SeasonsState) {
        rule.setThemedContent { SeasonsScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(1_000)
    }

    private fun loaded(expanded: Int? = null) =
        SeasonsState(tvShowId = details.id, details = details, expandedSeasonId = expanded)

    @Test
    fun givenLoadedSeasons_whenShown_thenTheShowNameAndEverySeasonAreListed() {
        show(loaded())

        rule.onNodeWithText(details.name).assertIsDisplayed()
        rule.onNodeWithText("Season 1").assertIsDisplayed()
        rule.onNodeWithText("Season 2").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("season poster")[0].assertIsDisplayed()
    }

    @Test
    fun givenFirstLoad_whenShown_thenNoSeasonsAreListedYet() {
        show(SeasonsState(tvShowId = 1, isLoading = true))

        rule.onNodeWithText("Season 1").assertDoesNotExist()
        rule.onNodeWithText("Seasons").assertIsDisplayed()
    }

    @Test
    fun givenFailureWithoutData_whenShown_thenFullScreenErrorAndRetry() {
        show(SeasonsState(tvShowId = 1, error = UiError.Server))

        rule.onNodeWithText("An error just happened, check your network connection and try again").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertThat(actions).containsExactly(SeasonsAction.OnRetry)
    }

    @Test
    fun givenFailureWithData_whenShown_thenDialogOverTheListAndAcceptDismisses() {
        show(loaded().copy(error = UiError.NoInternet))

        rule.onNodeWithText("No internet connection, check your network and try again").assertIsDisplayed()
        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(SeasonsAction.OnDismissError)
    }

    @Test
    fun givenExpandedSeason_whenShown_thenOnlyItsOverviewIsVisible() {
        show(loaded(expanded = details.seasons[1].id))

        rule.onNodeWithText("Overview of season 2").assertIsDisplayed()
        rule.onNodeWithText("Overview of season 1").assertDoesNotExist()
    }

    @Test
    fun givenNothingExpanded_whenShown_thenNoOverviewIsVisible() {
        show(loaded())

        rule.onNodeWithText("Overview of season 1").assertDoesNotExist()
    }

    @Test
    fun givenSeason_whenTapped_thenDispatchesSeasonClickWithItsNumber() {
        show(loaded())

        rule.onNodeWithText("Season 2").performClick()

        assertThat(actions).containsExactly(SeasonsAction.OnSeasonClick(2))
    }

    @Test
    fun givenSeason_whenLongPressed_thenDispatchesToggleWithItsId() {
        show(loaded())

        rule.onNodeWithText("Season 1").performTouchInput { longClick() }

        assertThat(actions).containsExactly(SeasonsAction.OnSeasonToggle(details.seasons[0].id))
    }

    @Test
    fun givenBackIcon_whenTapped_thenDispatchesBack() {
        show(loaded())

        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions).containsExactly(SeasonsAction.OnBack)
    }
}
