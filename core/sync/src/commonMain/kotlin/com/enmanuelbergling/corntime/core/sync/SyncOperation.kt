package com.enmanuelbergling.corntime.core.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One account change that has to reach TMDB, even if the app is closed or offline when it is made. */
@Serializable
data class SyncOperation(val type: SyncType, val id: Int, val listId: Int? = null)

@Serializable
enum class SyncType {
    RemoveMovieFromWatchlist,
    AddMovieToFavorites,
    RemoveMovieFromFavorites,
    /** Needs [SyncOperation.listId]. */
    RemoveMovieFromList,
    RemoveTvFromWatchlist,
    AddTvToFavorites,
    RemoveTvFromFavorites,
}

/** Builds the operations behind each scheduler call. */
object SyncOperations {
    /** Removes [deleted] and [favorites] from the watchlist first, then adds [favorites] to favorites. */
    fun movieWatchlist(deleted: List<Int>, favorites: List<Int>): List<SyncOperation> =
        (deleted + favorites).distinct().map { SyncOperation(SyncType.RemoveMovieFromWatchlist, it) } +
            favorites.distinct().map { SyncOperation(SyncType.AddMovieToFavorites, it) }

    /** Removes [deleted] and [favorites] from the list [listId] first, then adds [favorites] to favorites. */
    fun movieList(listId: Int, deleted: List<Int>, favorites: List<Int>): List<SyncOperation> =
        (deleted + favorites).distinct().map { SyncOperation(SyncType.RemoveMovieFromList, it, listId) } +
            favorites.distinct().map { SyncOperation(SyncType.AddMovieToFavorites, it) }

    fun movieFavoriteRemovals(movieIds: List<Int>): List<SyncOperation> =
        movieIds.distinct().map { SyncOperation(SyncType.RemoveMovieFromFavorites, it) }

    fun tvWatchlist(deleted: List<Int>, favorites: List<Int>): List<SyncOperation> =
        (deleted + favorites).distinct().map { SyncOperation(SyncType.RemoveTvFromWatchlist, it) } +
            favorites.distinct().map { SyncOperation(SyncType.AddTvToFavorites, it) }

    fun tvFavoriteRemovals(tvShowIds: List<Int>): List<SyncOperation> =
        tvShowIds.distinct().map { SyncOperation(SyncType.RemoveTvFromFavorites, it) }
}

private val json = Json { ignoreUnknownKeys = true }

fun List<SyncOperation>.encode(): String = json.encodeToString(this)

/** An unreadable payload yields no operations instead of crashing a background job. */
fun decodeSyncOperations(text: String?): List<SyncOperation> =
    if (text.isNullOrBlank()) emptyList()
    else try {
        json.decodeFromString(text)
    } catch (_: Exception) {
        emptyList()
    }
