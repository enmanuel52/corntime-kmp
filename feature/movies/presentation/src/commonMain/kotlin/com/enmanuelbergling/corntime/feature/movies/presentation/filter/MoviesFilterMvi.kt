package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieFilter
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class MoviesFilterState(
    val filter: MovieFilter = MovieFilter(),
    /** Every genre TMDB offers; empty until loaded. */
    val genres: List<Genre> = emptyList(),
    val isLoadingGenres: Boolean = false,
    val genresError: UiError? = null,
    /** Movies matching [filter]; restarts from page one a moment after the filter stops changing. */
    val paging: PagingState<Movie> = PagingState(),
) {
    /** The loaded movies with a concrete element type (generic `items` is untyped in Swift). */
    val movies: List<Movie> get() = paging.items

    val hasActiveFilter: Boolean get() = filter != MovieFilter()

    fun isSelected(genre: Genre): Boolean = genre in filter.genres
}

sealed interface MoviesFilterAction {
    data object OnBack : MoviesFilterAction
    data class OnToggleGenre(val genre: Genre) : MoviesFilterAction
    data class OnPickSortCriteria(val criteria: SortCriteria) : MoviesFilterAction
    data object OnClearFilter : MoviesFilterAction
    data object OnLoadNextPage : MoviesFilterAction
    data object OnRetryGenres : MoviesFilterAction
    data object OnDismissError : MoviesFilterAction
    data class OnMovieClick(val movieId: Int) : MoviesFilterAction
}

sealed interface MoviesFilterEvent {
    data object NavigateBack : MoviesFilterEvent
    data class NavigateToDetails(val movieId: Int) : MoviesFilterEvent
}
