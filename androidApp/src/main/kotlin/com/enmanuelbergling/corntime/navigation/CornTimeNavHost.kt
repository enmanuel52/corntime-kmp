package com.enmanuelbergling.corntime.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.enmanuelbergling.corntime.core.designsystem.core.LocalAnimatedContentScope
import com.enmanuelbergling.corntime.feature.actor.presentation.navigation.ActorDetailsKey
import com.enmanuelbergling.corntime.feature.actor.presentation.navigation.actorEntries
import com.enmanuelbergling.corntime.feature.auth.presentation.navigation.authEntries
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MovieDetailsKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.moviesEntries
import com.enmanuelbergling.corntime.feature.settings.presentation.navigation.settingsEntries

/**
 * Single Navigation 3 host. Each entry gets its own ViewModelStore, so screen ViewModels (and their
 * `parametersOf` arguments) live and die with their back stack entry.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CornTimeNavHost(
    state: CornTimeAppState,
    onOpenDrawer: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = state.backStack,
        onBack = state::back,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberSharedElementDecorator(),
        ),
        entryProvider = entryProvider {
            moviesEntries(
                onNavigate = state::navigate,
                onBack = state::back,
                onActor = { id, name, imagePath -> state.navigate(ActorDetailsKey(id, imagePath, name)) },
                onOpenDrawer = onOpenDrawer,
            )
            actorEntries(
                onNavigate = state::navigate,
                onBack = state::back,
                onMovie = { state.navigate(MovieDetailsKey(it)) },
                onOpenDrawer = onOpenDrawer,
            )
            settingsEntries(
                onBack = state::back,
                onLogin = state::navigateToLogin,
                onLoggedOut = onLoggedOut,
            )
            authEntries(onLoginSucceed = state::onLoginSucceeded, onBack = state::back)
        },
    )
}

/** Hands each entry's animated scope to the design system so shared elements can find it. */
@Composable
private fun rememberSharedElementDecorator(): NavEntryDecorator<NavKey> = androidx.compose.runtime.remember {
    NavEntryDecorator(
        decorate = { entry: NavEntry<NavKey> ->
            CompositionLocalProvider(LocalAnimatedContentScope provides LocalNavAnimatedContentScope.current) {
                entry.Content()
            }
        },
    )
}
