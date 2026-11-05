package com.enmanuelbergling.corntime.feature.watchlists.domain.lists

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.TvRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MoveTvShowToFavoritesUseCaseTest {
    private val tv = FakeTvRemoteDataSource()

    @Test
    fun givenAShow_whenMovingToFavorites_thenItIsAddedThenRemovedFromTheWatchlist() = runTest {
        val result = MoveTvShowToFavoritesUseCase(tv)(tvShowId = 5)

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(tv.callsTo(TvRemoteFunction.AddTvToFavorites).map { it.single() }).containsExactly(5)
        assertThat(tv.callsTo(TvRemoteFunction.RemoveTvFromAccountWatchlist).map { it.single() }).containsExactly(5)
    }

    @Test
    fun givenAddingToFavoritesFails_whenMoving_thenTheShowIsNotRemovedFromTheWatchlist() = runTest {
        tv.fail(TvRemoteFunction.AddTvToFavorites, DataError.Network.NoInternet)

        val result = MoveTvShowToFavoritesUseCase(tv)(tvShowId = 5)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.NoInternet))
        assertThat(tv.callCount(TvRemoteFunction.RemoveTvFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenRemovingFails_whenMoving_thenTheFailureIsReportedSoARetryRepeatsBothSteps() = runTest {
        tv.fail(TvRemoteFunction.RemoveTvFromAccountWatchlist, DataError.Network.ServerError)

        val result = MoveTvShowToFavoritesUseCase(tv)(tvShowId = 5)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.ServerError))
        assertThat(tv.callCount(TvRemoteFunction.AddTvToFavorites)).isEqualTo(1)
    }
}
