package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class TvShowSectionState(
    val section: TvShowsSection,
    val paging: PagingState<TvShow> = PagingState(),
) {
    /** The loaded shows with a concrete element type (generic `items` is untyped in Swift). */
    val tvShows: List<TvShow> get() = paging.items
}

sealed interface TvShowSectionAction {
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : TvShowSectionAction
    data class OnTvShowClick(val tvShowId: Int) : TvShowSectionAction
    data object OnBack : TvShowSectionAction
}

sealed interface TvShowSectionEvent {
    data class NavigateToDetails(val tvShowId: Int) : TvShowSectionEvent
    data object NavigateBack : TvShowSectionEvent
}
