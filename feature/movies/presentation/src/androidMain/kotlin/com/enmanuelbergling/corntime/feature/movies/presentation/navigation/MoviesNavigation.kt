package com.enmanuelbergling.corntime.feature.movies.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.feature.movies.presentation.details.MovieDetailsRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.home.MoviesRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.section.MovieSectionRoot
import kotlinx.serialization.Serializable

@Serializable
data object MoviesHomeKey : NavKey

@Serializable
data class MovieDetailsKey(val movieId: Int) : NavKey

@Serializable
data class MovieSectionKey(val section: MovieSection) : NavKey

/**
 * Destinations of the movies feature. Cross-feature destinations (actor, filter, favorites, drawer)
 * are callbacks so the feature never depends on another one; the app wires them.
 */
fun EntryProviderScope<NavKey>.moviesEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onActor: (actorId: Int) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    entry<MoviesHomeKey> {
        MoviesRoot(
            onDetails = { onNavigate(MovieDetailsKey(it)) },
            onSection = { onNavigate(MovieSectionKey(it)) },
            onOpenDrawer = onOpenDrawer,
            onFilter = onFilter,
            onFavorites = onFavorites,
        )
    }

    entry<MovieDetailsKey> { key ->
        MovieDetailsRoot(
            movieId = key.movieId,
            onActor = onActor,
            onBack = onBack,
        )
    }

    entry<MovieSectionKey> { key ->
        MovieSectionRoot(
            section = key.section,
            onMovie = { onNavigate(MovieDetailsKey(it)) },
            onBack = onBack,
        )
    }
}
