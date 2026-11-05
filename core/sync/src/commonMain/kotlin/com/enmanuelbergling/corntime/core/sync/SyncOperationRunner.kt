package com.enmanuelbergling.corntime.core.sync

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import org.koin.core.annotation.Single

/**
 * Sends sync operations to TMDB, in order. Both platforms' background machinery (WorkManager,
 * BGTaskScheduler) end up here, so what a sync does is written once.
 *
 * TMDB's add/remove calls are idempotent, so repeating an operation after a partial run is harmless.
 */
@Single
class SyncOperationRunner(
    private val user: UserRemoteDataSource,
    private val tv: TvRemoteDataSource,
) {
    /** Returns the operations that should be tried again later. Permanent failures are dropped. */
    suspend fun run(operations: List<SyncOperation>): List<SyncOperation> =
        operations.filter { operation ->
            when (val result = send(operation)) {
                is Result.Success -> false
                is Result.Failure -> result.error.isRetryable
            }
        }

    private suspend fun send(operation: SyncOperation): NetworkResult<*> = when (operation.type) {
        SyncType.RemoveMovieFromList -> operation.listId
            // a list removal without a list can never succeed: drop it
            ?.let { user.deleteMovieFromList(operation.id, it) }
            ?: Result.Failure(DataError.Network.BadRequest)
        SyncType.RemoveMovieFromWatchlist -> user.removeMovieFromAccountWatchlist(operation.id)
        SyncType.AddMovieToFavorites -> user.addMovieToFavorites(operation.id)
        SyncType.RemoveMovieFromFavorites -> user.removeMovieFromFavorites(operation.id)
        SyncType.RemoveTvFromWatchlist -> tv.removeTvFromAccountWatchlist(operation.id)
        SyncType.AddTvToFavorites -> tv.addTvToFavorites(operation.id)
        SyncType.RemoveTvFromFavorites -> tv.removeTvFromFavorites(operation.id)
    }
}

/** A later attempt can succeed. A rejected session, a missing item or a bad request never will. */
private val DataError.Network.isRetryable: Boolean
    get() = when (this) {
        DataError.Network.NoInternet,
        DataError.Network.RequestTimeout,
        DataError.Network.ServerError,
        DataError.Network.ServiceUnavailable,
        DataError.Network.TooManyRequests,
        DataError.Network.Unknown,
        -> true

        DataError.Network.BadRequest,
        DataError.Network.Unauthorized,
        DataError.Network.Forbidden,
        DataError.Network.NotFound,
        DataError.Network.Conflict,
        DataError.Network.Serialization,
        -> false
    }
