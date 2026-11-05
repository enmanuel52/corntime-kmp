package com.enmanuelbergling.corntime.feature.auth.presentation.login

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginFieldError
import com.enmanuelbergling.corntime.feature.auth.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.auth.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { LoginRobot(rule) }

    @Test
    fun givenEmptyForm_whenShown_thenWelcomeAndFieldsAreVisible() {
        robot.show(LoginState())
            .assertFormVisible()
            .assertTextVisible("Username")
            .assertTextVisible("Password")
            .assertSubmitEnabled()
    }

    @Test
    fun givenLoading_whenShown_thenOnlyTheLoaderIsVisible() {
        robot.show(LoginState(isLoading = true))
            .assertLoadingVisible()
            .assertFormAbsent()
    }

    @Test
    fun givenFieldErrors_whenShown_thenMessagesAreShownAndSubmitIsDisabled() {
        robot.show(
            LoginState(
                usernameError = LoginFieldError.UsernameRequired,
                passwordError = LoginFieldError.PasswordTooShort,
            ),
        )
            .assertTextVisible("Username is required")
            .assertTextVisible("Password must be at least 8 characters")
            .assertSubmitDisabled()
    }

    @Test
    fun givenWrongCredentials_whenShown_thenTheDialogSaysSoAndDismissSendsAnAction() {
        robot.show(LoginState(error = UiError.Unauthorized))
            .assertTextVisible("Wrong username or password")
            .clickDismissDialog("Accept")

        assertThat(robot.actions).containsExactly(LoginAction.OnDismissError)
    }

    @Test
    fun givenNoInternet_whenShown_thenTheNetworkMessageIsShownInsteadOfWrongCredentials() {
        robot.show(LoginState(error = UiError.NoInternet))
            .assertTextAbsent("Wrong username or password")
    }

    @Test
    fun givenTyping_whenEnteringBothFields_thenActionsCarryTheText() {
        robot.show(LoginState())
            .typeUsername("tim")
            .typePassword("secret123")

        // the screen is stateless and shown with a fixed state, so it may also echo the unchanged field
        assertThat(robot.actions).contains(LoginAction.OnUsernameChange("tim"))
        assertThat(robot.actions).contains(LoginAction.OnPasswordChange("secret123"))
    }

    @Test
    fun givenForm_whenTappingSubmitAndVisibilityAndBack_thenEachSendsItsActionOrCallback() {
        robot.show(LoginState())
            .clickPasswordVisibility()
            .clickSubmit()
            .clickBack()

        assertThat(robot.actions).containsExactly(
            LoginAction.OnPasswordVisibilityClick,
            LoginAction.OnLoginClick,
        )
        assertThat(robot.backs).isEqualTo(1)
    }
}
