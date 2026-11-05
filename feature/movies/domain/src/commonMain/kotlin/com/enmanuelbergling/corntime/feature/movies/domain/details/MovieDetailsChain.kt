package com.enmanuelbergling.corntime.feature.movies.domain.details

import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/** Mutable holder the details chain fills. Parts that are already present are not requested again. */
class MovieDetailsChainRequest(
    val movieId: Int,
    var details: MovieDetails? = null,
    var credits: MovieCredits? = null,
    var accountStates: MovieAccountStates? = null,
)

class MovieDetailsChainHandler(
    private val dataSource: MovieRemoteDataSource,
    override val next: ChainHandler<MovieDetailsChainRequest>? = null,
) : ChainHandler<MovieDetailsChainRequest> {
    override suspend fun handle(request: MovieDetailsChainRequest) {
        if (request.details != null) return
        request.details = dataSource.getMovieDetails(request.movieId).getOrThrow()
    }
}

class MovieCreditsChainHandler(
    private val dataSource: MovieRemoteDataSource,
    override val next: ChainHandler<MovieDetailsChainRequest>? = null,
) : ChainHandler<MovieDetailsChainRequest> {
    override suspend fun handle(request: MovieDetailsChainRequest) {
        if (request.credits != null) return
        request.credits = dataSource.getMovieCredits(request.movieId).getOrThrow()
    }
}

/**
 * Last on purpose: it fails with `Unauthorized` while logged out, and by then details and credits
 * are already in the request.
 */
class MovieAccountStatesChainHandler(
    private val dataSource: MovieRemoteDataSource,
    override val next: ChainHandler<MovieDetailsChainRequest>? = null,
) : ChainHandler<MovieDetailsChainRequest> {
    override suspend fun handle(request: MovieDetailsChainRequest) {
        request.accountStates = dataSource.getMovieAccountStates(request.movieId).getOrThrow()
    }
}

/** details → credits → account states. */
@Factory
class MovieDetailsChain(private val dataSource: MovieRemoteDataSource) {
    fun create(): ChainHandler<MovieDetailsChainRequest> = MovieDetailsChainHandler(
        dataSource,
        next = MovieCreditsChainHandler(
            dataSource,
            next = MovieAccountStatesChainHandler(dataSource),
        ),
    )
}
