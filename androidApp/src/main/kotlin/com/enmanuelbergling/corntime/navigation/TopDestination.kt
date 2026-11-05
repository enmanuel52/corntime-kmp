package com.enmanuelbergling.corntime.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.core.designsystem.R
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MoviesHomeKey
import com.enmanuelbergling.corntime.feature.settings.presentation.navigation.SettingsKey

/**
 * Destinations reachable from the drawer. TV shows, actors and lists are added here as their
 * features land.
 */
enum class TopDestination(
    @StringRes val label: Int,
    @DrawableRes val selectedIconRes: Int,
    @DrawableRes val unselectedIconRes: Int,
    val key: NavKey,
    /** Only offered while logged in. */
    val loginRequired: Boolean = false,
    /** Whether swiping from the screen's edge opens the drawer. */
    val drawerGesture: Boolean = true,
) {
    Movies(R.string.movies, R.drawable.film_solid, R.drawable.film_outline, MoviesHomeKey),
    Settings(R.string.settings, R.drawable.cog_solid, R.drawable.cog_outline, SettingsKey, drawerGesture = false),
}
