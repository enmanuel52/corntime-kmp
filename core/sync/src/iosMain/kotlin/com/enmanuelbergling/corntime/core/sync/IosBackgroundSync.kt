package com.enmanuelbergling.corntime.core.sync

import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler

/** Must match `BGTaskSchedulerPermittedIdentifiers` in the app's Info.plist. */
const val SYNC_TASK_IDENTIFIER = "com.enmanuelbergling.corntime-kmp.sync"

/**
 * iOS counterpart of the WorkManager scheduler: operations are queued on disk and tried straight
 * away; what is left (offline, killed mid-way) is retried by a BGProcessingTask that iOS runs when a
 * connection is available, and again whenever the app comes to the foreground.
 */
@OptIn(ExperimentalForeignApi::class)
@Single
class IosBackgroundSync(queue: PendingSyncQueue, runner: SyncOperationRunner) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal val coordinator = BackgroundSyncCoordinator(queue, runner, ::requestBackgroundRun, scope)

    /** Registers the background task. Call once, before the app finishes launching. */
    fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(SYNC_TASK_IDENTIFIER, null) { task ->
            if (task is BGProcessingTask) handle(task) else task?.setTaskCompletedWithSuccess(false)
        }
    }

    /** Sends whatever is pending now. Called when the app becomes active. */
    fun flush() {
        scope.launch { coordinator.syncNow() }
    }

    private fun handle(task: BGTask) {
        val job: Job = scope.launch {
            coordinator.syncNow()
            task.setTaskCompletedWithSuccess(true)
        }
        task.expirationHandler = {
            job.cancel()
            task.setTaskCompletedWithSuccess(false)
        }
    }

    private fun requestBackgroundRun() {
        val request = BGProcessingTaskRequest(SYNC_TASK_IDENTIFIER).apply {
            requiresNetworkConnectivity = true
            requiresExternalPower = false
        }
        // Fails on a simulator or when background refresh is disabled; the foreground flush still covers it.
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }
}

/** Entry points for Swift, which has no access to Koin. */
object IosSync {
    private val background get() = KoinPlatform.getKoin().get<IosBackgroundSync>()

    fun register() = background.register()
    fun flush() = background.flush()
}

@Single
class IosMovieSyncScheduler(private val background: IosBackgroundSync) : MovieSyncScheduler {
    override fun watchlist(deletedMovieIds: List<Int>, favoriteMovieIds: List<Int>) {
        background.coordinator.enqueue(SyncOperations.movieWatchlist(deletedMovieIds, favoriteMovieIds))
    }

    override fun deleteFromFavorite(movieIds: List<Int>) {
        background.coordinator.enqueue(SyncOperations.movieFavoriteRemovals(movieIds))
    }
}

@Single
class IosTvShowSyncScheduler(private val background: IosBackgroundSync) : TvShowSyncScheduler {
    override fun watchlist(deletedTvShowIds: List<Int>, favoriteTvShowIds: List<Int>) {
        background.coordinator.enqueue(SyncOperations.tvWatchlist(deletedTvShowIds, favoriteTvShowIds))
    }

    override fun deleteFromFavorite(tvShowIds: List<Int>) {
        background.coordinator.enqueue(SyncOperations.tvFavoriteRemovals(tvShowIds))
    }
}
