package com.enmanuelbergling.corntime.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.OnboardingPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** App-wide state shared by both apps: theme, account and the first-run walkthrough. */
@KoinViewModel
class AppViewModel(
    settings: SettingsPreferenceDataSource,
    userPreference: UserPreferenceDataSource,
    private val onboarding: OnboardingPreferenceDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        combine(
            settings.getDarkTheme(),
            settings.getDynamicColor(),
            userPreference.getCurrentUser(),
            onboarding.isOnboarding(),
        ) { darkTheme, dynamicColor, user, isOnboarding ->
            AppState(
                isLoading = false,
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                user = user,
                isOnboarding = isOnboarding,
            )
        }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun onAction(action: AppAction) {
        when (action) {
            AppAction.OnFinishOnboarding -> viewModelScope.launch { onboarding.finishOnboarding() }
        }
    }
}
