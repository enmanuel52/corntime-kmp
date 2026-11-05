package com.enmanuelbergling.corntime.core.sync

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkManagerSyncTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val user = FakeUserRemoteDataSource()
    private val tv = FakeTvRemoteDataSource()

    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        startKoin { modules(module { single { SyncOperationRunner(user, tv) } }) }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() = stopKoin()

    private val works get() = workManager.getWorkInfosByTag(SyncWorker.TAG).get()

    private fun movieScheduler() = WorkManagerMovieSyncScheduler(WorkManagerSyncEnqueuer(context))

    private fun runQueuedWork(): WorkInfo {
        val info = works.single()
        WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(info.id)
        return works.single()
    }

    @Test
    fun givenNoOperations_whenScheduling_thenNothingIsQueued() {
        movieScheduler().watchlist(emptyList(), emptyList())
        movieScheduler().deleteFromFavorite(emptyList())

        assertThat(works).isEmpty()
    }

    @Test
    fun givenADeletionAndAFavorite_whenScheduling_thenOneWorkWaitsForTheNetwork() {
        movieScheduler().watchlist(deletedMovieIds = listOf(1), favoriteMovieIds = listOf(2))

        val work = works.single()
        assertThat(work.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(work.constraints.requiredNetworkType).isEqualTo(androidx.work.NetworkType.CONNECTED)
    }

    @Test
    fun givenQueuedWork_whenTheNetworkArrives_thenTheOperationsAreSentInOrderAndTheWorkSucceeds() {
        movieScheduler().watchlist(deletedMovieIds = listOf(1), favoriteMovieIds = listOf(2))

        val finished = runQueuedWork()

        assertThat(finished.state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist).map { it.single() })
            .containsExactly(1, 2)
        assertThat(user.callsTo(UserRemoteFunction.AddMovieToFavorites).map { it.single() }).containsExactly(2)
    }

    @Test
    fun givenAFavoriteRemoval_whenTheWorkRuns_thenItCallsTheFavoritesEndpoint() {
        movieScheduler().deleteFromFavorite(listOf(5))

        runQueuedWork()

        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromFavorites).map { it.single() }).containsExactly(5)
    }

    @Test
    fun givenTvShows_whenScheduling_thenTheTvEndpointsAreUsed() {
        WorkManagerTvShowSyncScheduler(WorkManagerSyncEnqueuer(context)).deleteFromFavorite(listOf(8))

        runQueuedWork()

        assertThat(tv.callsTo(com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction.RemoveTvFromFavorites)
            .map { it.single() }).containsExactly(8)
    }

    @Test
    fun givenTheDeviceIsOffline_whenTheWorkRuns_thenItIsRetriedLaterInsteadOfFailing() {
        user.fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.NoInternet)
        movieScheduler().watchlist(deletedMovieIds = listOf(1), favoriteMovieIds = emptyList())

        assertThat(runQueuedWork().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun givenTheSessionWasRejected_whenTheWorkRuns_thenItStopsInsteadOfRetryingForever() {
        user.fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.Unauthorized)
        movieScheduler().watchlist(deletedMovieIds = listOf(1), favoriteMovieIds = emptyList())

        assertThat(runQueuedWork().state).isEqualTo(WorkInfo.State.SUCCEEDED)
    }
}
