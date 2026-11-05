package com.enmanuelbergling.corntime.feature.auth.presentation.login

import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginFieldError

data class LoginState(
    val username: String = "",
    val password: String = "",
    val usernameError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    /** [UiError.Unauthorized] here means TMDB rejected the credentials. */
    val error: UiError? = null,
)

sealed interface LoginAction {
    data class OnUsernameChange(val value: String) : LoginAction
    data class OnPasswordChange(val value: String) : LoginAction
    data object OnPasswordVisibilityClick : LoginAction
    data object OnLoginClick : LoginAction
    data object OnDismissError : LoginAction
}

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
}
