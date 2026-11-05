package com.enmanuelbergling.corntime.feature.movies.presentation.home

import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

data class MoviesState(
    /** First page of every home section; a section is missing/empty until it loads. */
    val sections: Map<MovieSection, List<Movie>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: UiError? = null,
    /** Null while logged out. */
    val user: UserDetails? = null,
) {
    val isLoggedIn: Boolean get() = user != null

    val isFeedEmpty: Boolean get() = sections.values.all { it.isEmpty() }

    /** Sections in the order they are displayed: now playing first, then the rest by declaration. */
    val orderedSections: List<Pair<MovieSection, List<Movie>>>
        get() = MovieSection.entries
            .sortedBy { if (it == MovieSection.NowPlaying) 0 else 1 }
            .mapNotNull { section -> sections[section]?.takeIf { it.isNotEmpty() }?.let { section to it } }
}

sealed interface MoviesAction {
    /** Loads whatever is missing; used for the first load and for retrying after an error. */
    data object OnRetry : MoviesAction
    data object OnDismissError : MoviesAction
    data class OnMovieClick(val movieId: Int) : MoviesAction
    data class OnSectionClick(val section: MovieSection) : MoviesAction
}

sealed interface MoviesEvent {
    data class NavigateToDetails(val movieId: Int) : MoviesEvent
    data class NavigateToSection(val section: MovieSection) : MoviesEvent
}
