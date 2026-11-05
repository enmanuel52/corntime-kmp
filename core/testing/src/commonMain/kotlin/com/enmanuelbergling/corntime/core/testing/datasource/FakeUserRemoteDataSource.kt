package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.data.fakePage
import com.enmanuelbergling.corntime.core.testing.util.FakeBehavior

enum class UserRemoteFunction {
    GetAccount, CreateWatchList, DeleteMovieFromList, AddMovieToList, DeleteList, CheckItemStatus,
    GetWatchListMovies, GetWatchLists, GetAccountWatchlistMovies, AddMovieToAccountWatchlist,
    RemoveMovieFromAccountWatchlist, GetAccountFavoriteMovies, AddMovieToFavorites,
    RemoveMovieFromFavorites, AddMovieRating, DeleteMovieRating,
}

class FakeUserRemoteDataSource : FakeBehavior<UserRemoteFunction>(), UserRemoteDataSource {
    var totalPages: Int = 1
    var account = FakeUserData.FAKE_USER
    var itemOnList: Boolean = false
    val createdLists = mutableListOf<CreateListPost>()

    private val ok = WatchResponse("Success.")
    private fun moviePage(page: Int) = fakePage(FakeMovieData.MOVIES, page, totalPages) { copy(id = it) }

    override suspend fun getAccount() = respond(UserRemoteFunction.GetAccount) { account }

    override suspend fun createWatchList(listPost: CreateListPost) =
        respond(UserRemoteFunction.CreateWatchList, listPost) { createdLists += listPost; ok }

    override suspend fun deleteMovieFromList(movieId: Int, listId: Int) =
        respond(UserRemoteFunction.DeleteMovieFromList, movieId, listId) { ok }

    override suspend fun addMovieToList(movieId: Int, listId: Int) =
        respond(UserRemoteFunction.AddMovieToList, movieId, listId) { ok }

    override suspend fun deleteList(listId: Int) = respond(UserRemoteFunction.DeleteList, listId) { ok }

    override suspend fun checkItemStatus(listId: Int, movieId: Int) =
        respond(UserRemoteFunction.CheckItemStatus, listId, movieId) { itemOnList }

    override suspend fun getWatchListMovies(listId: Int, page: Int) =
        respond(UserRemoteFunction.GetWatchListMovies, listId, page) { moviePage(page) }

    override suspend fun getWatchLists(page: Int) =
        respond(UserRemoteFunction.GetWatchLists, page) {
            fakePage(listOf(FakeUserData.FAKE_WATCH_LIST), page, totalPages) { copy(id = it) }
        }

    override suspend fun getAccountWatchlistMovies(page: Int) =
        respond(UserRemoteFunction.GetAccountWatchlistMovies, page) { moviePage(page) }

    override suspend fun addMovieToAccountWatchlist(movieId: Int) =
        respond(UserRemoteFunction.AddMovieToAccountWatchlist, movieId) { ok }

    override suspend fun removeMovieFromAccountWatchlist(movieId: Int) =
        respond(UserRemoteFunction.RemoveMovieFromAccountWatchlist, movieId) { ok }

    override suspend fun getAccountFavoriteMovies(page: Int) =
        respond(UserRemoteFunction.GetAccountFavoriteMovies, page) { moviePage(page) }

    override suspend fun addMovieToFavorites(movieId: Int) =
        respond(UserRemoteFunction.AddMovieToFavorites, movieId) { ok }

    override suspend fun removeMovieFromFavorites(movieId: Int) =
        respond(UserRemoteFunction.RemoveMovieFromFavorites, movieId) { ok }

    override suspend fun addMovieRating(movieId: Int, value: Float) =
        respond(UserRemoteFunction.AddMovieRating, movieId, value) { ok }

    override suspend fun deleteMovieRating(movieId: Int) =
        respond(UserRemoteFunction.DeleteMovieRating, movieId) { ok }
}
