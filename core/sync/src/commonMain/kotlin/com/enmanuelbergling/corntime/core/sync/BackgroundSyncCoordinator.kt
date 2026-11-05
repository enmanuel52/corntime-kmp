package com.enmanuelbergling.corntime.core.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Persist, try right away, and ask the OS for a later run when something is left. The platform glue
 * ([requestBackgroundRun]) is the only part that differs.
 */
class BackgroundSyncCoordinator(
    private val queue: PendingSyncQueue,
    private val runner: SyncOperationRunner,
    private val requestBackgroundRun: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** Fire-and-forget: queues [operations] and starts a sync without blocking the caller. */
    fun enqueue(operations: List<SyncOperation>): Job = scope.launch {
        queue.add(operations)
        if (operations.isNotEmpty()) syncNow()
    }

    /** Runs everything pending. Returns whether anything is left for a later run. */
    suspend fun syncNow(): Boolean {
        val remaining = queue.process(runner::run)
        if (remaining.isNotEmpty()) requestBackgroundRun()
        return remaining.isNotEmpty()
    }
}
