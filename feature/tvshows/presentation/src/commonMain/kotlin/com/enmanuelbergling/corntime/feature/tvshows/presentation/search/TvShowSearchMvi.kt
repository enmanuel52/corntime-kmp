package com.enmanuelbergling.corntime.feature.tvshows.presentation.search

import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class TvShowSearchState(
    /** Exactly what the user typed; results follow it after a short debounce. */
    val query: String = "",
    val paging: PagingState<TvShow> = PagingState(),
) {
    /** The loaded shows with a concrete element type (generic `items` is untyped in Swift). */
    val tvShows: List<TvShow> get() = paging.items

    val hasQuery: Boolean get() = query.isNotBlank()

    /** A finished search that found nothing for a non-blank query. */
    val showNoResults: Boolean get() = hasQuery && paging.isEmpty
}

sealed interface TvShowSearchAction {
    data class OnQueryChange(val query: String) : TvShowSearchAction
    data object OnClear : TvShowSearchAction
    data object OnLoadNextPage : TvShowSearchAction
    data class OnTvShowClick(val tvShowId: Int) : TvShowSearchAction
}

sealed interface TvShowSearchEvent {
    data class NavigateToDetails(val tvShowId: Int) : TvShowSearchEvent
}
