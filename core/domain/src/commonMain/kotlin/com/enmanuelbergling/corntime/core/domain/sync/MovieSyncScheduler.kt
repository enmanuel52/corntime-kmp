package com.enmanuelbergling.corntime.core.domain.sync

/**
 * Defers account mutations so they survive the app being closed and wait for connectivity.
 * Android implements it with WorkManager, iOS with BGTaskScheduler.
 */
interface MovieSyncScheduler {
    /** Removes [deletedMovieIds] and [favoriteMovieIds] from the watchlist, then adds [favoriteMovieIds] to favorites. */
    fun watchlist(deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>)

    /** Same as [watchlist] but for a list the user created: [listId]'s items instead of the account watchlist. */
    fun deleteFromList(listId: Int, deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>)

    fun deleteFromFavorite(movieIds: List<Int>)
}
