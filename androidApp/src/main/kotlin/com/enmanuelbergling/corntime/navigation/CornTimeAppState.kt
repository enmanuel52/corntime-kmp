package com.enmanuelbergling.corntime.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.enmanuelbergling.corntime.feature.auth.presentation.navigation.LoginKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MoviesHomeKey
import com.enmanuelbergling.corntime.feature.watchlists.presentation.navigation.ListDetailsKey

@Composable
fun rememberCornTimeAppState(isOnline: Boolean = true, isLoggedIn: Boolean = false): CornTimeAppState {
    val backStack = rememberNavBackStack(MoviesHomeKey)
    return remember(backStack, isOnline, isLoggedIn) { CornTimeAppState(backStack, isOnline, isLoggedIn) }
}

/**
 * Navigation state of the app: a single back stack whose bottom is always the movies home, so back
 * from any top destination returns to it (as Pop-Corns did with `popUpTo`).
 */
@Stable
class CornTimeAppState(
    val backStack: MutableList<NavKey>,
    val isOnline: Boolean = true,
    val isLoggedIn: Boolean = false,
) {
    val currentKey: NavKey? get() = backStack.lastOrNull()

    val currentTopDestination: TopDestination?
        get() = TopDestination.entries.firstOrNull { it.key == currentKey }

    /** The drawer is only dragged open from top screens that show the menu button. */
    val mainDrawerEnabled: Boolean get() = currentTopDestination?.drawerGesture == true

    val drawerDestinations: List<TopDestination>
        get() = TopDestination.entries.filter { !it.loginRequired || isLoggedIn }

    fun isSelected(destination: TopDestination) = currentTopDestination == destination

    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun navigateToDrawerDestination(destination: TopDestination) {
        backStack.clear()
        backStack.add(MoviesHomeKey)
        if (destination != TopDestination.Movies) backStack.add(destination.key)
    }

    fun navigateToLogin() = navigate(LoginKey)

    /** Opens a list from a launcher shortcut, with the movies home below it. */
    fun openList(listId: Int, listName: String) {
        backStack.clear()
        backStack.add(MoviesHomeKey)
        backStack.add(ListDetailsKey(listId, listName))
    }

    /** After a successful login the app starts over from the movies home, now with the account. */
    fun onLoginSucceeded() {
        backStack.clear()
        backStack.add(MoviesHomeKey)
    }
}
