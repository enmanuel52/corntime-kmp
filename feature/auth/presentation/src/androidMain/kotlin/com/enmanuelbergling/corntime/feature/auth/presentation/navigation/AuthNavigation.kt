package com.enmanuelbergling.corntime.feature.auth.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.feature.auth.presentation.login.LoginRoot
import kotlinx.serialization.Serializable

@Serializable
data object LoginKey : NavKey

fun EntryProviderScope<NavKey>.authEntries(onLoginSucceed: () -> Unit, onBack: () -> Unit) {
    entry<LoginKey> { LoginRoot(onLoginSucceed = onLoginSucceed, onBack = onBack) }
}
