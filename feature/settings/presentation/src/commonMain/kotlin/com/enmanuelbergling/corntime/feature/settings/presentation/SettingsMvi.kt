package com.enmanuelbergling.corntime.feature.settings.presentation

import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme

data class UserUi(
    val username: String,
    val avatarPath: String,
)

data class SettingsState(
    /** Null while logged out. */
    val user: UserUi? = null,
    val darkTheme: DarkTheme = DarkTheme.System,
    val dynamicColor: Boolean = false,
) {
    val isLoggedIn: Boolean get() = user != null
}

sealed interface SettingsAction {
    data class OnDarkThemeChange(val theme: DarkTheme) : SettingsAction
    data class OnDynamicColorChange(val active: Boolean) : SettingsAction
    data object OnLogoutClick : SettingsAction

    /** Refreshes the cached account (name, avatar) when the screen is shown; a no-op while logged out. */
    data object OnRefreshAccount : SettingsAction
}
