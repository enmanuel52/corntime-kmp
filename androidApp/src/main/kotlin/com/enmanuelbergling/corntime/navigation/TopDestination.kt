package com.enmanuelbergling.corntime.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.core.designsystem.R
import com.enmanuelbergling.corntime.feature.actor.presentation.navigation.ActorsKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MoviesHomeKey
import com.enmanuelbergling.corntime.feature.settings.presentation.navigation.SettingsKey
import com.enmanuelbergling.corntime.feature.tvshows.presentation.navigation.TvShowsHomeKey
import com.enmanuelbergling.corntime.feature.watchlists.presentation.navigation.WatchlistKey

/**
 * Destinations reachable from the drawer.
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
    TvShows(R.string.tv_shows, R.drawable.tv_solid, R.drawable.tv_outline, TvShowsHomeKey),
    Actors(R.string.actors, R.drawable.check_badge_solid, R.drawable.check_badge_outline, ActorsKey),
    Lists(
        R.string.watch_lists,
        R.drawable.bookmark_solid,
        R.drawable.bookmark_outline,
        WatchlistKey,
        loginRequired = true,
        drawerGesture = false,
    ),
    Settings(R.string.settings, R.drawable.cog_solid, R.drawable.cog_outline, SettingsKey, drawerGesture = false),
}
