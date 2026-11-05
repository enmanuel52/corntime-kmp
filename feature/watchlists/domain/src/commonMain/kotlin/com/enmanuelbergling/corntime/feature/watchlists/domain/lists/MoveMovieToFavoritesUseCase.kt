package com.enmanuelbergling.corntime.feature.watchlists.domain.lists

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.domain.util.map
import org.koin.core.annotation.Factory

/**
 * Adds a movie to favorites and takes it out of where it was: the account watchlist, or the user's
 * list [listId]. Favorites go first, so a failure never loses the movie from both places; repeating
 * the whole thing after a failure is harmless because TMDB's add and remove are idempotent.
 */
@Factory
class MoveMovieToFavoritesUseCase(private val userRemote: UserRemoteDataSource) {

    suspend operator fun invoke(movieId: Int, listId: Int? = null): NetworkResult<Unit> {
        val added = userRemote.addMovieToFavorites(movieId)
        if (added is Result.Failure) return added.map { }

        val removed = if (listId == null) userRemote.removeMovieFromAccountWatchlist(movieId)
        else userRemote.deleteMovieFromList(movieId, listId)
        return removed.map { }
    }
}
