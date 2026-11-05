package com.enmanuelbergling.corntime.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MoviesHomeKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.moviesEntries

/**
 * Single Navigation 3 host. Each entry gets its own ViewModelStore, so screen ViewModels (and their
 * `parametersOf` arguments) live and die with their back stack entry.
 */
@Composable
fun CornTimeNavHost() {
    val backStack = rememberNavBackStack(MoviesHomeKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            moviesEntries(
                onNavigate = { key: NavKey -> backStack.add(key) },
                onBack = { backStack.removeLastOrNull() },
                // Actor details arrive with the actor feature.
                onActor = {},
            )
        },
    )
}
