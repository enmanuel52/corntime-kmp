package com.enmanuelbergling.corntime.core.sync

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SyncOperationTest {
    @Test
    fun givenDeletedAndFavorites_whenPlanningTheWatchlist_thenEveryoneLeavesTheWatchlistBeforeFavoritesAreAdded() {
        val plan = SyncOperations.movieWatchlist(deleted = listOf(1, 2), favorites = listOf(3))

        assertThat(plan).containsExactly(
            SyncOperation(SyncType.RemoveMovieFromWatchlist, 1),
            SyncOperation(SyncType.RemoveMovieFromWatchlist, 2),
            SyncOperation(SyncType.RemoveMovieFromWatchlist, 3),
            SyncOperation(SyncType.AddMovieToFavorites, 3),
        )
    }

    @Test
    fun givenRepeatedIds_whenPlanning_thenEachOperationAppearsOnce() {
        val plan = SyncOperations.movieWatchlist(deleted = listOf(1, 1), favorites = listOf(1))

        assertThat(plan).containsExactly(
            SyncOperation(SyncType.RemoveMovieFromWatchlist, 1),
            SyncOperation(SyncType.AddMovieToFavorites, 1),
        )
    }

    @Test
    fun givenNothingToDo_whenPlanning_thenTheListIsEmpty() {
        assertThat(SyncOperations.movieWatchlist(emptyList(), emptyList())).isEmpty()
        assertThat(SyncOperations.tvFavoriteRemovals(emptyList())).isEmpty()
    }

    @Test
    fun givenTvIds_whenPlanning_thenTvOperationsAreUsed() {
        assertThat(SyncOperations.tvWatchlist(listOf(1), listOf(2)).map { it.type }).containsExactly(
            SyncType.RemoveTvFromWatchlist,
            SyncType.RemoveTvFromWatchlist,
            SyncType.AddTvToFavorites,
        )
        assertThat(SyncOperations.tvFavoriteRemovals(listOf(4)).single().type).isEqualTo(SyncType.RemoveTvFromFavorites)
    }

    @Test
    fun givenOperations_whenEncodedAndDecoded_thenTheyComeBackEqual() {
        val operations = SyncOperations.movieWatchlist(listOf(1), listOf(2)) + SyncOperations.tvFavoriteRemovals(listOf(9))

        assertThat(decodeSyncOperations(operations.encode())).isEqualTo(operations)
    }

    @Test
    fun givenCorruptOrMissingPayload_whenDecoding_thenNoOperationsInsteadOfACrash() {
        assertThat(decodeSyncOperations(null)).isEmpty()
        assertThat(decodeSyncOperations("")).isEmpty()
        assertThat(decodeSyncOperations("{not json")).isEmpty()
        assertThat(decodeSyncOperations("""[{"type":"NoSuchType","id":1}]""")).isEmpty()
    }
}
