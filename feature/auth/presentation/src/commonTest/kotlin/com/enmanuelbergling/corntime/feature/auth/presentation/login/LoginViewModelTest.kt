package com.enmanuelbergling.corntime.feature.auth.presentation.login

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.usecase.LogoutUseCase
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.datasource.AuthRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginChain
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginFieldError
import com.enmanuelbergling.corntime.feature.auth.domain.login.ValidateLoginUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoginViewModelTest : MainDispatcherTest() {
    private val remote = FakeAuthRemoteDataSource()
    private val auth = FakeAuthPreferenceDataSource()
    private val user = FakeUserPreferenceDataSource()
    private val account = FakeUserRemoteDataSource()

    private fun viewModel() = LoginViewModel(
        LoginChain(remote, auth, SyncUserDetailsUseCase(account, user), LogoutUseCase(user, auth)),
        ValidateLoginUseCase(),
    )

    private fun LoginViewModel.fill(username: String = "tim", password: String = "secret123") {
        onAction(LoginAction.OnUsernameChange(username))
        onAction(LoginAction.OnPasswordChange(password))
    }

    @Test
    fun givenNewViewModel_whenCreated_thenEmptyFormWithNothingLoadingOrFailed() {
        val state = viewModel().state.value

        assertThat(state).isEqualTo(LoginState())
    }

    @Test
    fun givenBlankForm_whenLoggingIn_thenBothErrorsAndNoRequest() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(LoginAction.OnLoginClick)

        assertThat(viewModel.state.value.usernameError).isEqualTo(LoginFieldError.UsernameRequired)
        assertThat(viewModel.state.value.passwordError).isEqualTo(LoginFieldError.PasswordRequired)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(remote.callCount(AuthRemoteFunction.CreateRequestToken)).isEqualTo(0)
    }

    @Test
    fun givenShortPassword_whenLoggingIn_thenTooShortAndNoRequest() = runTest {
        val viewModel = viewModel()
        viewModel.fill(password = "1234567")

        viewModel.onAction(LoginAction.OnLoginClick)

        assertThat(viewModel.state.value.passwordError).isEqualTo(LoginFieldError.PasswordTooShort)
        assertThat(remote.callCount(AuthRemoteFunction.CreateRequestToken)).isEqualTo(0)
    }

    @Test
    fun givenFieldErrors_whenEditingAField_thenOnlyThatFieldsErrorClears() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(LoginAction.OnLoginClick)

        viewModel.onAction(LoginAction.OnUsernameChange("t"))

        assertThat(viewModel.state.value.usernameError).isNull()
        assertThat(viewModel.state.value.passwordError).isEqualTo(LoginFieldError.PasswordRequired)

        viewModel.onAction(LoginAction.OnPasswordChange("x"))
        assertThat(viewModel.state.value.passwordError).isNull()
    }

    @Test
    fun givenPasswordHidden_whenToggled_thenVisibilityFlipsBackAndForth() {
        val viewModel = viewModel()

        viewModel.onAction(LoginAction.OnPasswordVisibilityClick)
        assertThat(viewModel.state.value.isPasswordVisible).isTrue()

        viewModel.onAction(LoginAction.OnPasswordVisibilityClick)
        assertThat(viewModel.state.value.isPasswordVisible).isFalse()
    }

    @Test
    fun givenValidCredentials_whenLoggingIn_thenSuccessEventOnceSessionStoredAndNotLoading() = runTest {
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginClick)

            assertThat(awaitItem()).isEqualTo(LoginEvent.LoginSuccess)
            expectNoEvents()
        }
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.error).isNull()
        assertThat(auth.getSessionId().first()).isEqualTo("session-id")
        assertThat(user.getCurrentUser().first()?.username).isEqualTo(account.account.username)
    }

    @Test
    fun givenUsernameWithSurroundingSpaces_whenLoggingIn_thenItIsTrimmedForTmdb() = runTest {
        val viewModel = viewModel()
        viewModel.fill(username = "  tim  ")

        viewModel.onAction(LoginAction.OnLoginClick)

        val validation = remote.callsTo(AuthRemoteFunction.ValidateWithLogin).single().single().toString()
        assertThat(validation.contains("username=tim,")).isTrue()
    }

    @Test
    fun givenRequestInFlight_whenLoggingInIsShown_thenLoadingIsTrueAndSecondTapIsIgnored() = runTest {
        remote.hold()
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.onAction(LoginAction.OnLoginClick)
        viewModel.onAction(LoginAction.OnLoginClick)

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(remote.callCount(AuthRemoteFunction.CreateRequestToken)).isEqualTo(1)
        remote.release()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun givenWrongCredentials_whenLoggingIn_thenUnauthorizedErrorNoEventAndFormKept() = runTest {
        remote.fail(AuthRemoteFunction.ValidateWithLogin, DataError.Network.Unauthorized)
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginClick)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Unauthorized)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.username).isEqualTo("tim")
        assertThat(viewModel.state.value.password).isEqualTo("secret123")
    }

    @Test
    fun givenNoInternet_whenLoggingIn_thenNoInternetErrorThenRetryCanSucceed() = runTest {
        remote.fail(AuthRemoteFunction.CreateRequestToken, DataError.Network.NoInternet)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onAction(LoginAction.OnLoginClick)
        assertThat(viewModel.state.value.error).isEqualTo(UiError.NoInternet)

        remote.recoverAll()
        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginClick)

            assertThat(awaitItem()).isEqualTo(LoginEvent.LoginSuccess)
        }
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenAccountSyncFails_whenLoggingIn_thenErrorAndNoSessionRemains() = runTest {
        account.fail(UserRemoteFunction.GetAccount, DataError.Network.ServerError)
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.onAction(LoginAction.OnLoginClick)

        assertThat(viewModel.state.value.error).isEqualTo(UiError.Server)
        assertThat(auth.getSessionId().first()).isEqualTo("")
    }

    @Test
    fun givenAnError_whenDismissed_thenItIsCleared() = runTest {
        remote.fail(AuthRemoteFunction.CreateRequestToken)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onAction(LoginAction.OnLoginClick)

        viewModel.onAction(LoginAction.OnDismissError)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenAnError_whenLoggingInAgain_thenTheOldErrorIsClearedWhileLoading() = runTest {
        remote.fail(AuthRemoteFunction.CreateRequestToken)
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onAction(LoginAction.OnLoginClick)
        remote.recoverAll()
        remote.hold()

        viewModel.onAction(LoginAction.OnLoginClick)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.isLoading).isTrue()
        remote.release()
    }
}
