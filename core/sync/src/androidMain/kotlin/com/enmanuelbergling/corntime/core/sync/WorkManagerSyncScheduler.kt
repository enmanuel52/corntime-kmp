package com.enmanuelbergling.corntime.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/** Queues a [SyncWorker] that waits for a network connection. WorkManager keeps it across app restarts. */
@Single
class WorkManagerSyncEnqueuer(private val context: Context) {
    fun enqueue(operations: List<SyncOperation>) {
        if (operations.isEmpty()) return
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(SyncWorker.KEY_OPERATIONS to operations.encode()))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(SyncWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

@Single
class WorkManagerMovieSyncScheduler(private val enqueuer: WorkManagerSyncEnqueuer) : MovieSyncScheduler {
    override fun watchlist(deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>) =
        enqueuer.enqueue(SyncOperations.movieWatchlist(deletedMovieIds, favoriteMovieIds))

    override fun deleteFromList(listId: Int, deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>) =
        enqueuer.enqueue(SyncOperations.movieList(listId, deletedMovieIds, favoriteMovieIds))

    override fun deleteFromFavorite(movieIds: List<Int>) =
        enqueuer.enqueue(SyncOperations.movieFavoriteRemovals(movieIds))
}

@Single
class WorkManagerTvShowSyncScheduler(private val enqueuer: WorkManagerSyncEnqueuer) : TvShowSyncScheduler {
    override fun watchlist(deletedTvShowIds: List<Int>, favoriteTvShowIds: List<Int>) =
        enqueuer.enqueue(SyncOperations.tvWatchlist(deletedTvShowIds, favoriteTvShowIds))

    override fun deleteFromFavorite(tvShowIds: List<Int>) =
        enqueuer.enqueue(SyncOperations.tvFavoriteRemovals(tvShowIds))
}
