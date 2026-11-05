package com.enmanuelbergling.corntime.core.domain.sync

/** TV counterpart of [MovieSyncScheduler]. */
interface TvShowSyncScheduler {
    /** Removes [deletedTvShowIds] and [favoriteTvShowIds] from the watchlist, then adds [favoriteTvShowIds] to favorites. */
    fun watchlist(deletedTvShowIds: List<Int>, favoriteTvShowIds: List<Int>)

    fun deleteFromFavorite(tvShowIds: List<Int>)
}
