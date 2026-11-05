package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails

/** What the whole app needs before drawing anything: theme, logged-in user and onboarding progress. */
data class AppState(
    /** True until every preference has been read once, so the UI never flashes defaults. */
    val isLoading: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.System,
    val dynamicColor: Boolean = false,
    val user: UserDetails? = null,
    val isOnboarding: Boolean = false,
) {
    val isLoggedIn: Boolean get() = user != null
}

sealed interface AppAction {
    data object OnFinishOnboarding : AppAction
}
