package com.enmanuelbergling.corntime.feature.tvshows.domain.home

import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/**
 * Mutable holder every handler of the TV home chain fills. Sections that already hold shows are not
 * requested again, so retrying after a partial failure only fetches what is missing.
 */
class TvShowsHomeRequest(initial: Map<TvShowsSection, List<TvShow>> = emptyMap()) {
    val sections: MutableMap<TvShowsSection, List<TvShow>> = initial.toMutableMap()
}

/** Loads the first page of one [section] into the request, unless it was already loaded. */
class TvShowsSectionChainHandler(
    private val section: TvShowsSection,
    private val dataSource: TvRemoteDataSource,
    override val next: ChainHandler<TvShowsHomeRequest>? = null,
) : ChainHandler<TvShowsHomeRequest> {

    override suspend fun handle(request: TvShowsHomeRequest) {
        if (request.sections[section].orEmpty().isNotEmpty()) return
        val page = when (section) {
            TvShowsSection.Popular -> dataSource.getPopularTv()
            TvShowsSection.TopRated -> dataSource.getTopRatedTv()
            TvShowsSection.OnTheAir -> dataSource.getOnTheAirTv()
            TvShowsSection.AiringToday -> dataSource.getAiringTodayTv()
        }.getOrThrow()
        request.sections[section] = page.results
    }
}

/** Builds the home chain: popular → top rated → on the air → airing today. */
@Factory
class TvShowsHomeChain(private val dataSource: TvRemoteDataSource) {
    fun create(): ChainHandler<TvShowsHomeRequest> = HOME_ORDER
        .foldRight<TvShowsSection, ChainHandler<TvShowsHomeRequest>?>(null) { section, next ->
            TvShowsSectionChainHandler(section, dataSource, next)
        }!!

    companion object {
        val HOME_ORDER = listOf(
            TvShowsSection.Popular,
            TvShowsSection.TopRated,
            TvShowsSection.OnTheAir,
            TvShowsSection.AiringToday,
        )
    }
}
