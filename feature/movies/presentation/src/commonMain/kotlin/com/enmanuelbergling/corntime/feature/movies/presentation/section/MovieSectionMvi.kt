package com.enmanuelbergling.corntime.feature.movies.presentation.section

import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class MovieSectionState(
    val section: MovieSection,
    val paging: PagingState<Movie> = PagingState(),
)

sealed interface MovieSectionAction {
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : MovieSectionAction
    data class OnMovieClick(val movieId: Int) : MovieSectionAction
    data object OnBack : MovieSectionAction
}

sealed interface MovieSectionEvent {
    data class NavigateToDetails(val movieId: Int) : MovieSectionEvent
    data object NavigateBack : MovieSectionEvent
}
