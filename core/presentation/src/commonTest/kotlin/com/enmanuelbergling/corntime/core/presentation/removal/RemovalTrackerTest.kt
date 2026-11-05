package com.enmanuelbergling.corntime.core.presentation.removal

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RemovalTrackerTest {
    private val window = 5.seconds
    private val committed = mutableListOf<PendingRemoval>()
    private var result: NetworkResult<Unit> = Result.Success(Unit)
    private var gate: CompletableDeferred<Unit>? = null

    private fun TestScope.advance(duration: Duration) = advanceTimeBy(duration.inWholeMilliseconds)

    private fun TestScope.tracker() = RemovalTracker(backgroundScope, window) {
        committed += it
        gate?.await()
        result
    }

    @Test
    fun givenAnItem_whenRemoved_thenItIsHiddenAtOnceButNothingIsSentYet() = runTest {
        val tracker = tracker()

        tracker.remove(1, RemovalKind.Remove)

        assertThat(tracker.state.value.hiddenIds).isEqualTo(setOf(1))
        assertThat(tracker.state.value.undoable).isEqualTo(PendingRemoval(1, RemovalKind.Remove))
        assertThat(committed).isEmpty()
    }

    @Test
    fun givenTheUndoWindowPassed_whenWaiting_thenTheRemovalIsSentOnceAndStaysHidden() = runTest {
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)

        advance(window + 1.seconds)

        assertThat(committed).containsExactly(PendingRemoval(1, RemovalKind.Remove))
        assertThat(tracker.state.value.pending).isEmpty()
        assertThat(tracker.state.value.completed).isEqualTo(setOf(1))
        assertThat(tracker.state.value.hiddenIds).isEqualTo(setOf(1))
    }

    @Test
    fun givenAnUndoInsideTheWindow_whenWaiting_thenNothingIsEverSentAndTheItemIsBack() = runTest {
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        advance(window - 1.seconds)

        tracker.undo(1)
        advance(window * 2)

        assertThat(committed).isEmpty()
        assertThat(tracker.state.value.hiddenIds).isEqualTo(emptySet())
    }

    @Test
    fun givenTheCallAlreadyStarted_whenUndoing_thenItIsTooLateAndStaysRemoved() = runTest {
        gate = CompletableDeferred()
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        advance(window + 1.seconds)

        tracker.undo(1)

        assertThat(tracker.state.value.hiddenIds).isEqualTo(setOf(1))
        gate!!.complete(Unit)
    }

    @Test
    fun givenTwoRemovals_whenUndoingTheLatest_thenOnlyTheEarlierOneIsConfirmed() = runTest {
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        tracker.remove(2, RemovalKind.MoveToFavorites)
        assertThat(tracker.state.value.undoable).isEqualTo(PendingRemoval(2, RemovalKind.MoveToFavorites))

        tracker.undo(2)
        advance(window + 1.seconds)

        assertThat(committed).containsExactly(PendingRemoval(1, RemovalKind.Remove))
        assertThat(tracker.state.value.hiddenIds).isEqualTo(setOf(1))
    }

    @Test
    fun givenAnItemAlreadyRemoved_whenRemovingItAgain_thenItIsIgnored() = runTest {
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)

        tracker.remove(1, RemovalKind.MoveToFavorites)
        advance(window + 1.seconds)

        assertThat(committed).containsExactly(PendingRemoval(1, RemovalKind.Remove))
    }

    @Test
    fun givenTheServerFails_whenConfirming_thenTheItemStaysHiddenWithTheErrorUntilDismissed() = runTest {
        result = Result.Failure(DataError.Network.NoInternet)
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)

        advance(window + 1.seconds)

        assertThat(tracker.state.value.failure?.error).isEqualTo(UiError.NoInternet)
        assertThat(tracker.state.value.hiddenIds).isEqualTo(setOf(1))

        tracker.dismissFailure()

        assertThat(tracker.state.value.failure).isNull()
        assertThat(tracker.state.value.hiddenIds).isEqualTo(emptySet())
    }

    @Test
    fun givenAFailure_whenRetryingAndItSucceeds_thenItIsConfirmedWithoutWaitingAgain() = runTest {
        result = Result.Failure(DataError.Network.ServerError)
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        advance(window + 1.seconds)

        result = Result.Success(Unit)
        tracker.retry()
        advance(1.seconds)

        assertThat(committed.size).isEqualTo(2)
        assertThat(tracker.state.value.failure).isNull()
        assertThat(tracker.state.value.completed).isEqualTo(setOf(1))
    }

    @Test
    fun givenNothingFailed_whenRetryingOrDismissing_thenNothingHappens() = runTest {
        val tracker = tracker()

        tracker.retry()
        tracker.dismissFailure()
        advance(window)

        assertThat(committed).isEmpty()
        assertThat(tracker.state.value).isEqualTo(RemovalState())
    }

    @Test
    fun givenPendingAndFailedRemovals_whenAskingWhatNeverReachedTheServer_thenBothAreListed() = runTest {
        result = Result.Failure(DataError.Network.RequestTimeout)
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        advance(window + 1.seconds)
        result = Result.Success(Unit)
        tracker.remove(2, RemovalKind.MoveToFavorites)

        assertThat(tracker.unfinished()).containsExactly(
            PendingRemoval(2, RemovalKind.MoveToFavorites),
            PendingRemoval(1, RemovalKind.Remove),
        )
    }

    @Test
    fun givenConfirmedRemovals_whenAskingWhatNeverReachedTheServer_thenNothingIsLeft() = runTest {
        val tracker = tracker()
        tracker.remove(1, RemovalKind.Remove)
        advance(window + 1.seconds)

        assertThat(tracker.unfinished()).isEmpty()
    }
}
