package com.enmanuelbergling.corntime.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test

private fun testDataStore(scope: CoroutineScope): DataStore<Preferences> {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sync-test-${Random.nextLong()}.preferences_pb"
    return PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path.toString().toPath() })
}

class PendingSyncQueueTest {
    private val remove1 = SyncOperation(SyncType.RemoveMovieFromWatchlist, 1)
    private val remove2 = SyncOperation(SyncType.RemoveMovieFromWatchlist, 2)

    private fun TestScope.queue() = PendingSyncQueue(testDataStore(backgroundScope))

    @Test
    fun givenAnEmptyQueue_whenReading_thenNothingIsPending() = runTest {
        assertThat(queue().all()).isEmpty()
    }

    @Test
    fun givenOperationsAdded_whenReading_thenTheyAreKeptInOrderWithoutDuplicates() = runTest {
        val queue = queue()

        queue.add(listOf(remove1, remove2))
        queue.add(listOf(remove1))

        assertThat(queue.all()).containsExactly(remove1, remove2)
    }

    @Test
    fun givenNothingToAdd_whenAdding_thenTheStoreIsUntouched() = runTest {
        val queue = queue()

        queue.add(emptyList())

        assertThat(queue.all()).isEmpty()
    }

    @Test
    fun givenAProcessThatClearsEverything_whenProcessing_thenTheQueueIsEmpty() = runTest {
        val queue = queue()
        queue.add(listOf(remove1, remove2))

        val remaining = queue.process { emptyList() }

        assertThat(remaining).isEmpty()
        assertThat(queue.all()).isEmpty()
    }

    @Test
    fun givenAProcessThatKeepsSome_whenProcessing_thenOnlyThoseStayPending() = runTest {
        val queue = queue()
        queue.add(listOf(remove1, remove2))

        queue.process { listOf(remove2) }

        assertThat(queue.all()).containsExactly(remove2)
    }

    @Test
    fun givenOperationsAddedWhileProcessing_whenProcessingEnds_thenTheyAreNotLost() = runTest {
        val queue = queue()
        queue.add(listOf(remove1))

        queue.process { snapshot ->
            queue.add(listOf(remove2))
            snapshot.filterNot { it == remove1 }
        }

        assertThat(queue.all()).containsExactly(remove2)
    }

    @Test
    fun givenAnEmptyQueue_whenProcessing_thenTheBlockIsNotCalled() = runTest {
        var called = false

        queue().process { called = true; it }

        assertThat(called).isFalse()
    }

    @Test
    fun givenAnOperationThatFailsOffline_whenTheCoordinatorSyncs_thenItStaysQueuedAndABackgroundRunIsRequested() = runTest {
        val user = FakeUserRemoteDataSource().apply {
            fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.NoInternet)
        }
        val queue = queue()
        var requests = 0
        val coordinator = BackgroundSyncCoordinator(
            queue, SyncOperationRunner(user, FakeTvRemoteDataSource()), { requests++ }, backgroundScope,
        )

        coordinator.enqueue(listOf(remove1)).join()

        assertThat(queue.all()).containsExactly(remove1)
        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun givenAQueuedOperationAndBackOnline_whenTheBackgroundRunHappens_thenItIsSentAndNothingMoreIsRequested() = runTest {
        val user = FakeUserRemoteDataSource().apply {
            fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.NoInternet)
        }
        val queue = queue()
        var requests = 0
        val coordinator = BackgroundSyncCoordinator(
            queue, SyncOperationRunner(user, FakeTvRemoteDataSource()), { requests++ }, backgroundScope,
        )
        coordinator.enqueue(listOf(remove1)).join()

        user.recoverAll()
        val stillPending = coordinator.syncNow()

        assertThat(stillPending).isFalse()
        assertThat(queue.all()).isEmpty()
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(2)
        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun givenSuccess_whenEnqueueing_thenItIsSentRightAwayWithoutABackgroundRequest() = runTest {
        val user = FakeUserRemoteDataSource()
        var requests = 0
        val coordinator = BackgroundSyncCoordinator(
            queue(), SyncOperationRunner(user, FakeTvRemoteDataSource()), { requests++ }, backgroundScope,
        )

        coordinator.enqueue(listOf(remove1)).join()

        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(1)
        assertThat(requests).isEqualTo(0)
    }

    @Test
    fun givenNothingToSync_whenSyncing_thenNothingIsPendingAndNoRequestIsMade() = runTest {
        var requests = 0
        val coordinator = BackgroundSyncCoordinator(
            queue(), SyncOperationRunner(FakeUserRemoteDataSource(), FakeTvRemoteDataSource()), { requests++ }, backgroundScope,
        )

        assertThat(coordinator.syncNow()).isFalse()
        assertThat(requests).isEqualTo(0)
    }

    @Test
    fun givenAPermanentFailure_whenSyncing_thenItIsDroppedAndNeverRequestsAnotherRun() = runTest {
        val user = FakeUserRemoteDataSource().apply {
            fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.Unauthorized)
        }
        val queue = queue()
        queue.add(listOf(remove1))
        var requests = 0
        val coordinator = BackgroundSyncCoordinator(
            queue, SyncOperationRunner(user, FakeTvRemoteDataSource()), { requests++ }, backgroundScope,
        )

        assertThat(coordinator.syncNow()).isFalse()
        assertThat(queue.all()).isEmpty()
        assertThat(requests).isEqualTo(0)
    }
}
