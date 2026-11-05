package com.enmanuelbergling.corntime.feature.favorites.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.feature.favorites.presentation.home.FavoriteMoviesRoot
import com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows.FavoriteTvShowsRoot
import kotlinx.serialization.Serializable

@Serializable
data object FavoriteMoviesKey : NavKey

@Serializable
data object FavoriteTvShowsKey : NavKey

fun EntryProviderScope<NavKey>.favoriteEntries(
    onMovieDetails: (movieId: Int) -> Unit,
    onTvShowDetails: (tvShowId: Int) -> Unit,
    onBack: () -> Unit,
) {
    entry<FavoriteMoviesKey> { FavoriteMoviesRoot(onMovieDetails = onMovieDetails, onBack = onBack) }
    entry<FavoriteTvShowsKey> { FavoriteTvShowsRoot(onTvShowDetails = onTvShowDetails, onBack = onBack) }
}
