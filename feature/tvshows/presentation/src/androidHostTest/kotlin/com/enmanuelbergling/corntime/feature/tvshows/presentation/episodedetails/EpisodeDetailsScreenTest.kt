package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
class EpisodeDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<EpisodeDetailsAction>()
    private val details = FakeTvData.DEFAULT_EPISODE_DETAILS.let { fake ->
        fake.copy(
            guestStars = listOf(
                fake.guestStars.first().copy(id = 11, name = "Giancarlo Esposito", profilePath = null),
                fake.guestStars.first().copy(id = 12, name = "Jonathan Banks", profilePath = null),
            ),
            crew = listOf(fake.crew.first().copy(id = 21, name = "Michelle MacLaren", profilePath = null)),
        )
    }

    private fun show(state: EpisodeDetailsState) {
        rule.setThemedContent { EpisodeDetailsScreen(state = state, onAction = { actions += it }) }
        rule.mainClock.advanceTimeBy(1_000)
    }

    private fun loaded() = EpisodeDetailsState(1, 1, 1, details = details)

    private fun scrollTo(text: String) {
        rule.onNodeWithTag(EpisodeDetailsTags.CONTENT).performScrollToNode(hasText(text))
        rule.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun givenLoadedEpisode_whenShown_thenTitleDurationAndOverviewAreDisplayed() {
        show(loaded())

        rule.onNodeWithText("Episode Details").assertIsDisplayed()
        rule.onNodeWithText(details.name).assertIsDisplayed()
        rule.onNodeWithText("Duration ${details.duration} (${details.airYear})").assertIsDisplayed()
        rule.onNodeWithText(details.overview).assertIsDisplayed()
    }

    @Test
    fun givenGuestStarsAndCrew_whenShown_thenTheirNamesAreDisplayed() {
        show(loaded())

        scrollTo("Giancarlo Esposito")
        rule.onNodeWithText("Giancarlo Esposito").assertIsDisplayed()
        rule.onNodeWithText("Jonathan Banks").assertIsDisplayed()
        scrollTo("Michelle MacLaren")
        rule.onNodeWithText("Michelle MacLaren").assertExists()
    }

    @Test
    fun givenFirstLoad_whenShown_thenNoContentIsDisplayed() {
        show(EpisodeDetailsState(1, 1, 1, isLoading = true))

        rule.onNodeWithTag(EpisodeDetailsTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun givenFailureWithoutData_whenShown_thenFullScreenErrorAndRetry() {
        show(EpisodeDetailsState(1, 1, 1, error = UiError.Server))

        rule.onNodeWithText("An error just happened, check your network connection and try again").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertThat(actions).containsExactly(EpisodeDetailsAction.OnRetry)
    }

    @Test
    fun givenFailureWithData_whenShown_thenDialogOverContentAndAcceptDismisses() {
        show(loaded().copy(error = UiError.NoInternet))

        rule.onNodeWithText("No internet connection, check your network and try again").assertIsDisplayed()
        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(EpisodeDetailsAction.OnDismissError)
    }

    @Test
    fun givenGuestStar_whenTapped_thenDispatchesPersonClickWithTheirId() {
        show(loaded())
        scrollTo("Giancarlo Esposito")

        rule.onAllNodesWithContentDescription("movie image")[1].performClick()

        assertThat(actions).containsExactly(EpisodeDetailsAction.OnPersonClick(12))
    }

    @Test
    fun givenBackIcon_whenTapped_thenDispatchesBack() {
        show(loaded())

        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions).containsExactly(EpisodeDetailsAction.OnBack)
    }
}
