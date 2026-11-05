package com.enmanuelbergling.corntime.core.testing.sync

import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler

class FakeMovieSyncScheduler : MovieSyncScheduler {
    data class WatchlistSync(val deleted: List<Int>, val favorites: List<Int>)

    val watchlistSyncs = mutableListOf<WatchlistSync>()
    val favoriteDeletions = mutableListOf<List<Int>>()

    override fun watchlist(deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>) {
        watchlistSyncs += WatchlistSync(deletedMovieIds, favoriteMovieIds)
    }

    override fun deleteFromFavorite(movieIds: List<Int>) {
        favoriteDeletions += movieIds
    }
}

class FakeTvShowSyncScheduler : TvShowSyncScheduler {
    data class WatchlistSync(val deleted: List<Int>, val favorites: List<Int>)

    val watchlistSyncs = mutableListOf<WatchlistSync>()
    val favoriteDeletions = mutableListOf<List<Int>>()

    override fun watchlist(deletedTvShowIds: List<Int>, favoriteTvShowIds: List<Int>) {
        watchlistSyncs += WatchlistSync(deletedTvShowIds, favoriteTvShowIds)
    }

    override fun deleteFromFavorite(tvShowIds: List<Int>) {
        favoriteDeletions += tvShowIds
    }
}
