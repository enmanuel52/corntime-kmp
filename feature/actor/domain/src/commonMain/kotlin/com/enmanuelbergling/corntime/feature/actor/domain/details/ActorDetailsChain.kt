package com.enmanuelbergling.corntime.feature.actor.domain.details

import com.enmanuelbergling.corntime.core.domain.datasource.remote.ActorRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/** Mutable holder the chain fills. A part that is already present is not requested again (retry loads only what is missing). */
class ActorDetailsChainRequest(
    val actorId: Int,
    var details: ActorDetails? = null,
    var knownMovies: List<KnownMovie>? = null,
)

class ActorDetailsChainHandler(
    private val dataSource: ActorRemoteDataSource,
    override val next: ChainHandler<ActorDetailsChainRequest>? = null,
) : ChainHandler<ActorDetailsChainRequest> {
    override suspend fun handle(request: ActorDetailsChainRequest) {
        if (request.details != null) return
        request.details = dataSource.getActorDetails(request.actorId).getOrThrow()
    }
}

class ActorKnownMoviesChainHandler(
    private val dataSource: ActorRemoteDataSource,
    override val next: ChainHandler<ActorDetailsChainRequest>? = null,
) : ChainHandler<ActorDetailsChainRequest> {
    override suspend fun handle(request: ActorDetailsChainRequest) {
        // an actor with no movies is a valid, loaded answer: only null means "not loaded"
        if (request.knownMovies != null) return
        request.knownMovies = dataSource.getMoviesByActor(request.actorId).getOrThrow()
    }
}

/** actor details → known movies. */
@Factory
class ActorDetailsChain(private val dataSource: ActorRemoteDataSource) {
    fun create(): ChainHandler<ActorDetailsChainRequest> =
        ActorDetailsChainHandler(dataSource, next = ActorKnownMoviesChainHandler(dataSource))
}
