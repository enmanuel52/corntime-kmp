package com.enmanuelbergling.corntime.core.presentation.removal

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class RemovalKind {
    /** Delete the item from the list it is in. */
    Remove,

    /** Add it to favorites and delete it from the list it is in. */
    MoveToFavorites,
}

data class PendingRemoval(val id: Int, val kind: RemovalKind)

data class FailedRemoval(val removal: PendingRemoval, val error: UiError)

/**
 * What a list screen needs to show removals the user can still take back.
 *
 * An item the user removes disappears right away, then the server call happens once the undo window
 * has passed. The screen shows [undoable] as "removed - undo".
 */
data class RemovalState(
    /** Removed on screen, not yet confirmed with the server (still inside the undo window, or in flight). */
    val pending: List<PendingRemoval> = emptyList(),
    /** Confirmed with the server; the item stays hidden although it may still be in already loaded pages. */
    val completed: Set<Int> = emptySet(),
    /** The server call failed. The item stays hidden until the user retries or dismisses the failure. */
    val failed: List<FailedRemoval> = emptyList(),
) {
    val hiddenIds: Set<Int> get() = pending.map { it.id }.toSet() + completed + failed.map { it.removal.id }

    /** The latest removal the user may still undo. */
    val undoable: PendingRemoval? get() = pending.lastOrNull()

    val failure: FailedRemoval? get() = failed.firstOrNull()
}

/**
 * Optimistic removals with an undo window, shared by every list screen that supports swipe-to-remove.
 *
 * It is not a ViewModel: a ViewModel owns one, passing its `viewModelScope` and the server call, and
 * copies [state] into its own MVI state. Time is the scope's (virtual in tests).
 */
class RemovalTracker(
    private val scope: CoroutineScope,
    private val undoWindow: Duration = DEFAULT_UNDO_WINDOW,
    private val commit: suspend (PendingRemoval) -> NetworkResult<Unit>,
) {
    private val _state = MutableStateFlow(RemovalState())
    val state: StateFlow<RemovalState> = _state.asStateFlow()

    private val waiting = mutableMapOf<Int, Job>()
    private val inFlight = mutableSetOf<Int>()

    /** Hides [id] now and confirms it after the undo window. Ignored if it is already hidden. */
    fun remove(id: Int, kind: RemovalKind) {
        if (id in _state.value.hiddenIds) return
        val removal = PendingRemoval(id, kind)
        _state.update { it.copy(pending = it.pending + removal) }
        waiting[id] = scope.launch {
            delay(undoWindow)
            commit(removal)
        }
    }

    /** Takes a removal back. Too late once its server call has started. */
    fun undo(id: Int) {
        if (id in inFlight) return
        val job = waiting.remove(id) ?: return
        job.cancel()
        _state.update { it.copy(pending = it.pending.filterNot { removal -> removal.id == id }) }
    }

    /** Tries the oldest failed removal again. */
    fun retry() {
        val failure = _state.value.failure ?: return
        _state.update { it.copy(failed = it.failed - failure) }
        _state.update { it.copy(pending = it.pending + failure.removal) }
        waiting[failure.removal.id] = scope.launch { commit(failure.removal) }
    }

    /** Gives up on the oldest failed removal: the item comes back. */
    fun dismissFailure() {
        val failure = _state.value.failure ?: return
        _state.update { it.copy(failed = it.failed - failure) }
    }

    /** Removals that never reached the server: hand them to the background scheduler when the screen is left. */
    fun unfinished(): List<PendingRemoval> = _state.value.pending + _state.value.failed.map { it.removal }

    private suspend fun commit(removal: PendingRemoval) {
        inFlight += removal.id
        val result = try {
            commit.invoke(removal)
        } finally {
            inFlight -= removal.id
            waiting.remove(removal.id)
        }
        _state.update { state ->
            val pending = state.pending - removal
            when (result) {
                is Result.Success -> state.copy(pending = pending, completed = state.completed + removal.id)
                is Result.Failure ->
                    state.copy(pending = pending, failed = state.failed + FailedRemoval(removal, result.error.toUiError()))
            }
        }
    }

    companion object {
        val DEFAULT_UNDO_WINDOW = 5.seconds
    }
}
