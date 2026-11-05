package com.enmanuelbergling.corntime.feature.movies.presentation.search

import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class MovieSearchState(
    /** Exactly what the user typed; results follow it after a short debounce. */
    val query: String = "",
    val paging: PagingState<Movie> = PagingState(),
    /** Recent searches, most recent first; shown while the query is blank. */
    val suggestions: List<String> = emptyList(),
) {
    /** The loaded movies with a concrete element type (generic `items` is untyped in Swift). */
    val movies: List<Movie> get() = paging.items

    val hasQuery: Boolean get() = query.isNotBlank()

    val showSuggestions: Boolean get() = !hasQuery && suggestions.isNotEmpty()

    /** A finished search that found nothing for a non-blank query. */
    val showNoResults: Boolean get() = hasQuery && paging.isEmpty
}

sealed interface MovieSearchAction {
    data class OnQueryChange(val query: String) : MovieSearchAction
    data object OnClear : MovieSearchAction

    /** The keyboard's search key: remembers the query as a suggestion. */
    data object OnSearchSubmit : MovieSearchAction
    data class OnSuggestionClick(val query: String) : MovieSearchAction
    data class OnDeleteSuggestion(val query: String) : MovieSearchAction
    data object OnClearSuggestions : MovieSearchAction
    data object OnLoadNextPage : MovieSearchAction
    data class OnMovieClick(val movieId: Int) : MovieSearchAction
}

sealed interface MovieSearchEvent {
    data class NavigateToDetails(val movieId: Int) : MovieSearchEvent
}
