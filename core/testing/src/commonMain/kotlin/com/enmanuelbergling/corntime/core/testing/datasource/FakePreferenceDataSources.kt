package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.OnboardingPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthPreferenceDataSource(initialSessionId: String = "") : AuthPreferenceDataSource {
    val sessionId = MutableStateFlow(initialSessionId)

    override suspend fun saveSessionId(sessionId: String) {
        this.sessionId.value = sessionId
    }

    override fun getSessionId(): Flow<String> = sessionId

    override suspend fun clear() {
        sessionId.value = ""
    }
}

class FakeUserPreferenceDataSource(initialUser: UserDetails? = null) : UserPreferenceDataSource {
    val user = MutableStateFlow(initialUser)

    override fun getCurrentUser(): Flow<UserDetails?> = user

    override suspend fun updateUser(userDetails: UserDetails) {
        user.value = userDetails
    }

    override suspend fun clear() {
        user.value = null
    }
}

class FakeSettingsPreferenceDataSource(
    initialTheme: DarkTheme = DarkTheme.System,
    initialDynamicColor: Boolean = false,
) : SettingsPreferenceDataSource {
    val darkTheme = MutableStateFlow(initialTheme)
    val dynamicColor = MutableStateFlow(initialDynamicColor)

    override fun getDarkTheme(): Flow<DarkTheme> = darkTheme
    override suspend fun setDarkTheme(theme: DarkTheme) {
        darkTheme.value = theme
    }

    override fun getDynamicColor(): Flow<Boolean> = dynamicColor
    override suspend fun setDynamicColor(active: Boolean) {
        dynamicColor.value = active
    }
}

class FakeOnboardingPreferenceDataSource(initiallyOnboarding: Boolean = true) : OnboardingPreferenceDataSource {
    val onboarding = MutableStateFlow(initiallyOnboarding)

    override fun isOnboarding(): Flow<Boolean> = onboarding
    override suspend fun finishOnboarding() {
        onboarding.value = false
    }
}
