package com.enmanuelbergling.corntime.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.usecase.LogoutUseCase
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val settings: SettingsPreferenceDataSource,
    private val userPreference: UserPreferenceDataSource,
    private val logout: LogoutUseCase,
    private val syncUserDetails: SyncUserDetailsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Each preference is its own source of truth, so one slow flow never holds the others back.
        settings.getDarkTheme().onEach { theme -> _state.update { it.copy(darkTheme = theme) } }
            .launchIn(viewModelScope)
        settings.getDynamicColor().onEach { active -> _state.update { it.copy(dynamicColor = active) } }
            .launchIn(viewModelScope)
        userPreference.getCurrentUser()
            .onEach { user ->
                _state.update { it.copy(user = user?.let { UserUi(user.username, user.avatarPath) }) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnDarkThemeChange -> viewModelScope.launch { settings.setDarkTheme(action.theme) }
            is SettingsAction.OnDynamicColorChange ->
                viewModelScope.launch { settings.setDynamicColor(action.active) }

            SettingsAction.OnLogoutClick -> viewModelScope.launch { logout() }
            SettingsAction.OnRefreshAccount -> viewModelScope.launch {
                // best effort: the cached account stays when offline or when the session expired
                if (userPreference.getCurrentUser().first() != null) syncUserDetails()
            }
        }
    }
}
