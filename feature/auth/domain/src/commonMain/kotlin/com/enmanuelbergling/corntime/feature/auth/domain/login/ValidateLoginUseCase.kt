package com.enmanuelbergling.corntime.feature.auth.domain.login

import org.koin.core.annotation.Factory

enum class LoginFieldError { UsernameRequired, PasswordRequired, PasswordTooShort }

data class LoginValidation(
    val usernameError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
) {
    val isValid: Boolean get() = usernameError == null && passwordError == null
}

/** Checks the login form before any request is made. Each platform turns the errors into text. */
@Factory
class ValidateLoginUseCase {
    operator fun invoke(username: String, password: String) = LoginValidation(
        usernameError = LoginFieldError.UsernameRequired.takeIf { username.isBlank() },
        passwordError = when {
            password.isEmpty() -> LoginFieldError.PasswordRequired
            password.length < MIN_PASSWORD_LENGTH -> LoginFieldError.PasswordTooShort
            else -> null
        },
    )

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
