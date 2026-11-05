package com.enmanuelbergling.corntime.feature.movies.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.feature.movies.presentation.details.MovieDetailsRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.filter.MoviesFilterRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.home.MoviesRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.reviews.ReviewsRoot
import com.enmanuelbergling.corntime.feature.movies.presentation.section.MovieSectionRoot
import kotlinx.serialization.Serializable

@Serializable
data object MoviesHomeKey : NavKey

@Serializable
data class MovieDetailsKey(val movieId: Int) : NavKey

@Serializable
data class MovieSectionKey(val section: MovieSection) : NavKey

/** Advanced filter: discover movies by genres and sort order. */
@Serializable
data object MoviesFilterKey : NavKey

/** [averageRating] is on the 0..5 star scale, as the movie details report it. */
@Serializable
data class MovieReviewsKey(val movieId: Int, val averageRating: Double, val totalReviews: Int) : NavKey

/**
 * Destinations of the movies feature. Cross-feature destinations (actor, favorites, drawer)
 * are callbacks so the feature never depends on another one; the app wires them.
 */
fun EntryProviderScope<NavKey>.moviesEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onActor: (actorId: Int, name: String, imagePath: String?) -> Unit = { _, _, _ -> },
    onOpenDrawer: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    entry<MoviesHomeKey> {
        MoviesRoot(
            onDetails = { onNavigate(MovieDetailsKey(it)) },
            onSection = { onNavigate(MovieSectionKey(it)) },
            onOpenDrawer = onOpenDrawer,
            onFilter = { onNavigate(MoviesFilterKey) },
            onFavorites = onFavorites,
        )
    }

    entry<MovieDetailsKey> { key ->
        MovieDetailsRoot(
            movieId = key.movieId,
            onActor = onActor,
            onReviews = { id, average, total -> onNavigate(MovieReviewsKey(id, average, total)) },
            onBack = onBack,
        )
    }

    entry<MovieReviewsKey> { key ->
        ReviewsRoot(
            movieId = key.movieId,
            averageRating = key.averageRating,
            totalReviews = key.totalReviews,
            onBack = onBack,
        )
    }

    entry<MoviesFilterKey> {
        MoviesFilterRoot(
            onBack = onBack,
            onMovie = { onNavigate(MovieDetailsKey(it)) },
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
