package com.enmanuelbergling.corntime.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Sends the operations in its input to TMDB; retries with backoff while any can still succeed. */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val runner: SyncOperationRunner by inject()

    override suspend fun doWork(): Result {
        val operations = decodeSyncOperations(inputData.getString(KEY_OPERATIONS))
        if (operations.isEmpty()) return Result.success()

        return when {
            runner.run(operations).isEmpty() -> Result.success()
            runAttemptCount >= MAX_ATTEMPTS -> Result.failure()
            else -> Result.retry()
        }
    }

    companion object {
        const val KEY_OPERATIONS = "sync_operations"
        const val TAG = "corntime-sync"
        const val MAX_ATTEMPTS = 8
    }
}
