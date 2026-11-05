package com.enmanuelbergling.corntime.feature.actor.presentation.details

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.feature.actor.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.actor.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class ActorDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { ActorDetailsRobot(rule) }
    private val details = FakeActorData.ACTOR_DETAILS.copy(biography = "A Swedish DJ and producer.")

    private fun loaded() = ActorDetailsState(
        actorId = 0,
        details = details,
        loadedMovies = FakeActorData.KNOWN_MOVIES,
    )

    @Test
    fun givenLoadedDetails_whenShown_thenNameBiographyAndMoviesAreVisible() {
        robot.show(loaded())
            .assertTextVisible("Tim Bergling")
            .assertTextVisible("A Swedish DJ and producer.")
            .assertTextVisible("Known movies")
            .assertTextVisible("First Known")
            .assertTextVisible("Second Known")
    }

    @Test
    fun givenBlankBiography_whenShown_thenTheAboutSectionIsHidden() {
        robot.show(loaded().copy(details = details.copy(biography = "")))
            .assertTextAbsent("About")
    }

    @Test
    fun givenStillLoading_whenShown_thenTheNavigationNameIsAlreadyShown() {
        robot.show(ActorDetailsState(actorId = 0, isLoading = true), name = "Passed Along")
            .assertTextVisible("Passed Along")
            .assertTextAbsent("Known movies")
    }

    @Test
    fun givenFailureWithoutDetails_whenShown_thenARetryIsOfferedAndSendsTheAction() {
        robot.show(ActorDetailsState(actorId = 0, error = UiError.NoInternet))
            .assertTextVisible("Retry")
            .clickRetry()

        assertThat(robot.actions).containsExactly(ActorDetailsAction.OnRetry)
    }

    @Test
    fun givenFailureAfterDetailsLoaded_whenShown_thenADialogIsShownAndDismissSendsTheAction() {
        robot.show(loaded().copy(error = UiError.Timeout))
            .assertTextVisible("Tim Bergling")
            .clickAcceptOnDialog()

        assertThat(robot.actions).containsExactly(ActorDetailsAction.OnDismissError)
    }

    @Test
    fun givenKnownMovies_whenTappingOne_thenItsIdIsSent() {
        robot.show(loaded()).clickMovieAt(1)

        assertThat(robot.actions).containsExactly(ActorDetailsAction.OnMovieClick(2))
    }

    @Test
    fun givenTheScreen_whenTappingBack_thenBackIsSent() {
        robot.show(loaded()).clickBack()

        assertThat(robot.actions).containsExactly(ActorDetailsAction.OnBack)
    }
}
