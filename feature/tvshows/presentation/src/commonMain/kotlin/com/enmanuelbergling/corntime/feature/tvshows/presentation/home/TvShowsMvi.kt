package com.enmanuelbergling.corntime.feature.tvshows.presentation.home

import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError

/** One displayed home section; a plain type rather than a Pair so Swift gets named properties. */
data class SectionTvShows(
    val section: TvShowsSection,
    val tvShows: List<TvShow>,
)

data class TvShowsState(
    /** First page of every home section; a section is missing/empty until it loads. */
    val sections: Map<TvShowsSection, List<TvShow>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: UiError? = null,
    /** Null while logged out. */
    val user: UserDetails? = null,
) {
    val isLoggedIn: Boolean get() = user != null

    val isFeedEmpty: Boolean get() = sections.values.all { it.isEmpty() }

    /** Sections in display order, the best rated one by score. */
    val orderedSections: List<SectionTvShows>
        get() = TvShowsSection.entries.mapNotNull { section ->
            sections[section]?.takeIf { it.isNotEmpty() }?.let { shows ->
                SectionTvShows(
                    section = section,
                    tvShows = if (section == TvShowsSection.TopRated) shows.sortedByDescending { it.voteAverage } else shows,
                )
            }
        }
}

sealed interface TvShowsAction {
    /** Loads whatever is missing; used for the first load and for retrying after an error. */
    data object OnRetry : TvShowsAction
    data object OnDismissError : TvShowsAction
    data class OnTvShowClick(val tvShowId: Int) : TvShowsAction
    data class OnSectionClick(val section: TvShowsSection) : TvShowsAction
}

sealed interface TvShowsEvent {
    data class NavigateToDetails(val tvShowId: Int) : TvShowsEvent
    data class NavigateToSection(val section: TvShowsSection) : TvShowsEvent
}
