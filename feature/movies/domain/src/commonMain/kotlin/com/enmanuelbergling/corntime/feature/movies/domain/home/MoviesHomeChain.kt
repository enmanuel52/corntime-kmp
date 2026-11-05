package com.enmanuelbergling.corntime.feature.movies.domain.home

import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/**
 * Mutable holder every handler of the home chain fills. Sections that already hold movies are not
 * requested again, so retrying after a partial failure only fetches what is missing.
 */
class MoviesHomeRequest(initial: Map<MovieSection, List<Movie>> = emptyMap()) {
    val sections: MutableMap<MovieSection, List<Movie>> = initial.toMutableMap()
}

/** Loads the first page of one [section] into the request, unless it was already loaded. */
class MovieSectionChainHandler(
    private val section: MovieSection,
    private val dataSource: MovieRemoteDataSource,
    override val next: ChainHandler<MoviesHomeRequest>? = null,
) : ChainHandler<MoviesHomeRequest> {

    override suspend fun handle(request: MoviesHomeRequest) {
        if (request.sections[section].orEmpty().isNotEmpty()) return
        val page = when (section) {
            MovieSection.Upcoming -> dataSource.getUpcomingMovies()
            MovieSection.TopRated -> dataSource.getTopRatedMovies()
            MovieSection.NowPlaying -> dataSource.getNowPlayingMovies()
            MovieSection.Popular -> dataSource.getPopularMovies()
        }.getOrThrow()
        request.sections[section] = page.results
    }
}

/** Builds the home chain in the order the sections are fetched: upcoming, top rated, now playing, popular. */
@Factory
class MoviesHomeChain(private val dataSource: MovieRemoteDataSource) {
    fun create(): ChainHandler<MoviesHomeRequest> = HOME_ORDER
        .foldRight<MovieSection, ChainHandler<MoviesHomeRequest>?>(null) { section, next ->
            MovieSectionChainHandler(section, dataSource, next)
        }!!

    companion object {
        val HOME_ORDER = listOf(
            MovieSection.Upcoming,
            MovieSection.TopRated,
            MovieSection.NowPlaying,
            MovieSection.Popular,
        )
    }
}
