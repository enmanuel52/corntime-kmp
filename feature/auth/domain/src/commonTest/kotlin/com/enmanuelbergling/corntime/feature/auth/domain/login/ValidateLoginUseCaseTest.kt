package com.enmanuelbergling.corntime.feature.auth.domain.login

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ValidateLoginUseCaseTest {
    private val validate = ValidateLoginUseCase()

    @Test
    fun givenValidCredentials_whenValidating_thenNoErrors() {
        val result = validate("tim", "12345678")

        assertThat(result.isValid).isTrue()
        assertThat(result.usernameError).isNull()
        assertThat(result.passwordError).isNull()
    }

    @Test
    fun givenBlankOrWhitespaceUsername_whenValidating_thenUsernameRequired() {
        assertThat(validate("", "12345678").usernameError).isEqualTo(LoginFieldError.UsernameRequired)
        assertThat(validate("   ", "12345678").usernameError).isEqualTo(LoginFieldError.UsernameRequired)
    }

    @Test
    fun givenEmptyPassword_whenValidating_thenPasswordRequired() {
        assertThat(validate("tim", "").passwordError).isEqualTo(LoginFieldError.PasswordRequired)
    }

    @Test
    fun givenSevenVersusEightCharacters_whenValidating_thenOnlySevenIsTooShort() {
        assertThat(validate("tim", "1234567").passwordError).isEqualTo(LoginFieldError.PasswordTooShort)
        assertThat(validate("tim", "12345678").passwordError).isNull()
    }

    @Test
    fun givenSpacesOnlyPasswordOfEightCharacters_whenValidating_thenItIsNotTrimmed() {
        // passwords are taken literally; only the username is trimmed
        assertThat(validate("tim", "        ").isValid).isTrue()
    }

    @Test
    fun givenBothFieldsInvalid_whenValidating_thenBothErrorsAreReported() {
        val result = validate("", "")

        assertThat(result.isValid).isFalse()
        assertThat(result.usernameError).isEqualTo(LoginFieldError.UsernameRequired)
        assertThat(result.passwordError).isEqualTo(LoginFieldError.PasswordRequired)
    }
}
