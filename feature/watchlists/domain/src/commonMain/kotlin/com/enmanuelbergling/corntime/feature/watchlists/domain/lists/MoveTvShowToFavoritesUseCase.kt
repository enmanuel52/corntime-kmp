package com.enmanuelbergling.corntime.feature.watchlists.domain.lists

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.domain.util.map
import org.koin.core.annotation.Factory

/**
 * Adds a TV show to favorites and takes it out of the account watchlist. Favorites go first, so a
 * failure never loses the show from both places; repeating the whole thing after a failure is harmless
 * because TMDB's add and remove are idempotent.
 */
@Factory
class MoveTvShowToFavoritesUseCase(private val dataSource: TvRemoteDataSource) {

    suspend operator fun invoke(tvShowId: Int): NetworkResult<Unit> {
        val added = dataSource.addTvToFavorites(tvShowId)
        if (added is Result.Failure) return added.map { }

        return dataSource.removeTvFromAccountWatchlist(tvShowId).map { }
    }
}
