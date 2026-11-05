package com.enmanuelbergling.corntime.core.sync

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncOperationRunnerTest {
    private val user = FakeUserRemoteDataSource()
    private val tv = FakeTvRemoteDataSource()
    private val runner = SyncOperationRunner(user, tv)

    @Test
    fun givenEveryOperationSucceeds_whenRunning_thenEachCallsItsEndpointInOrderAndNothingRemains() = runTest {
        val remaining = runner.run(SyncOperations.movieWatchlist(deleted = listOf(1), favorites = listOf(2)))

        assertThat(remaining).isEmpty()
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist).map { it.single() })
            .containsExactly(1, 2)
        assertThat(user.callsTo(UserRemoteFunction.AddMovieToFavorites).map { it.single() }).containsExactly(2)
    }

    @Test
    fun givenAllSixTypes_whenRunning_thenEachGoesToTheRightDataSource() = runTest {
        val operations = SyncType.entries.map { SyncOperation(it, 7) }

        val remaining = runner.run(operations)

        assertThat(remaining).isEmpty()
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(1)
        assertThat(user.callCount(UserRemoteFunction.AddMovieToFavorites)).isEqualTo(1)
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromFavorites)).isEqualTo(1)
        assertThat(tv.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(1)
        assertThat(tv.callCount(TvRemoteFunction.AddTvToFavorites)).isEqualTo(1)
        assertThat(tv.callCount(TvRemoteFunction.RemoveTvFromFavorites)).isEqualTo(1)
    }

    @Test
    fun givenNoInternet_whenRunning_thenTheOperationsAreKeptForALaterRun() = runTest {
        user.fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.NoInternet)
        val operation = SyncOperation(SyncType.RemoveMovieFromWatchlist, 1)

        assertThat(runner.run(listOf(operation))).containsExactly(operation)
    }

    @Test
    fun givenTransientFailures_whenRunning_thenTimeoutServerAndRateLimitAreAllRetried() = runTest {
        listOf(
            DataError.Network.RequestTimeout,
            DataError.Network.ServerError,
            DataError.Network.ServiceUnavailable,
            DataError.Network.TooManyRequests,
            DataError.Network.Unknown,
        ).forEach { error ->
            user.fail(UserRemoteFunction.AddMovieToFavorites, error)

            assertThat(runner.run(listOf(SyncOperation(SyncType.AddMovieToFavorites, 1))).size).isEqualTo(1)
        }
    }

    @Test
    fun givenPermanentFailures_whenRunning_thenTheyAreDroppedBecauseRetryingCannotHelp() = runTest {
        listOf(
            DataError.Network.Unauthorized,
            DataError.Network.Forbidden,
            DataError.Network.NotFound,
            DataError.Network.BadRequest,
        ).forEach { error ->
            user.fail(UserRemoteFunction.RemoveMovieFromFavorites, error)

            assertThat(runner.run(listOf(SyncOperation(SyncType.RemoveMovieFromFavorites, 1)))).isEmpty()
        }
    }

    @Test
    fun givenOneFailureAmongMany_whenRunning_thenTheOthersStillGoThroughAndOnlyTheFailedRemains() = runTest {
        user.fail(UserRemoteFunction.AddMovieToFavorites, DataError.Network.NoInternet)
        val operations = SyncOperations.movieWatchlist(deleted = listOf(1), favorites = listOf(2))

        val remaining = runner.run(operations)

        assertThat(remaining).containsExactly(SyncOperation(SyncType.AddMovieToFavorites, 2))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(2)
    }

    @Test
    fun givenNoOperations_whenRunning_thenNothingIsCalled() = runTest {
        assertThat(runner.run(emptyList())).isEmpty()
    }
}
