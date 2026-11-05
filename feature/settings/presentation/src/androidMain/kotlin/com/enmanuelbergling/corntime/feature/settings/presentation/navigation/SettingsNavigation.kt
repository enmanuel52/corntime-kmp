package com.enmanuelbergling.corntime.feature.settings.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.feature.settings.presentation.SettingsRoot
import kotlinx.serialization.Serializable

@Serializable
data object SettingsKey : NavKey

fun EntryProviderScope<NavKey>.settingsEntries(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLoggedOut: () -> Unit = {},
) {
    entry<SettingsKey> { SettingsRoot(onBack = onBack, onLogin = onLogin, onLoggedOut = onLoggedOut) }
}
