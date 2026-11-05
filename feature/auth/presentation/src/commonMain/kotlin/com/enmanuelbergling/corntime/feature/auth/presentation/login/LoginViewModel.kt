package com.enmanuelbergling.corntime.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.util.onFailure
import com.enmanuelbergling.corntime.core.domain.util.onSuccess
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginChain
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginRequest
import com.enmanuelbergling.corntime.feature.auth.domain.login.ValidateLoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
    private val loginChain: LoginChain,
    private val validateLogin: ValidateLoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnUsernameChange ->
                _state.update { it.copy(username = action.value, usernameError = null) }

            is LoginAction.OnPasswordChange ->
                _state.update { it.copy(password = action.value, passwordError = null) }

            LoginAction.OnPasswordVisibilityClick ->
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

            LoginAction.OnLoginClick -> submit()
            LoginAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val current = _state.value
        // a second tap while the first login is running must not start another chain
        if (current.isLoading) return

        val validation = validateLogin(current.username, current.password)
        _state.update {
            it.copy(usernameError = validation.usernameError, passwordError = validation.passwordError)
        }
        if (!validation.isValid) return

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            loginChain.create()
                .invokeForResult(LoginRequest(current.username.trim(), current.password))
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.LoginSuccess)
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toUiError()) } }
        }
    }
}
