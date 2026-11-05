package com.enmanuelbergling.corntime.feature.tvshows.domain.details

import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.ChainHandler
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.domain.util.getOrThrow
import org.koin.core.annotation.Factory

/** Mutable holder the details chain fills. Parts that are already present are not requested again. */
class TvShowDetailsChainRequest(
    val tvShowId: Int,
    var details: TvShowDetails? = null,
    var credits: TvCredits? = null,
    var accountStates: TvAccountStates? = null,
)

class TvShowDetailsChainHandler(
    private val dataSource: TvRemoteDataSource,
    override val next: ChainHandler<TvShowDetailsChainRequest>? = null,
) : ChainHandler<TvShowDetailsChainRequest> {
    override suspend fun handle(request: TvShowDetailsChainRequest) {
        if (request.details != null) return
        request.details = dataSource.getTvDetails(request.tvShowId).getOrThrow()
    }
}

class TvCreditsChainHandler(
    private val dataSource: TvRemoteDataSource,
    override val next: ChainHandler<TvShowDetailsChainRequest>? = null,
) : ChainHandler<TvShowDetailsChainRequest> {
    override suspend fun handle(request: TvShowDetailsChainRequest) {
        if (request.credits != null) return
        request.credits = dataSource.getTvCredits(request.tvShowId).getOrThrow()
    }
}

/**
 * Last on purpose: it fails with `Unauthorized` while logged out, and by then details and credits
 * are already in the request.
 */
class TvAccountStatesChainHandler(
    private val dataSource: TvRemoteDataSource,
    override val next: ChainHandler<TvShowDetailsChainRequest>? = null,
) : ChainHandler<TvShowDetailsChainRequest> {
    override suspend fun handle(request: TvShowDetailsChainRequest) {
        request.accountStates = dataSource.getTvAccountStates(request.tvShowId).getOrThrow()
    }
}

/** details → credits → account states. */
@Factory
class TvShowDetailsChain(private val dataSource: TvRemoteDataSource) {
    fun create(): ChainHandler<TvShowDetailsChainRequest> = TvShowDetailsChainHandler(
        dataSource,
        next = TvCreditsChainHandler(
            dataSource,
            next = TvAccountStatesChainHandler(dataSource),
        ),
    )
}
