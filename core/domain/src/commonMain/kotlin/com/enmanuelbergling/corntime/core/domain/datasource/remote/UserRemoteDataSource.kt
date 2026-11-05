package com.enmanuelbergling.corntime.core.domain.datasource.remote

import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.domain.model.user.WatchList
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse

/** Account endpoints. Every call requires a logged-in session, otherwise it fails as unauthorized. */
interface UserRemoteDataSource {
    suspend fun getAccount(): NetworkResult<UserDetails>
    suspend fun createWatchList(listPost: CreateListPost): NetworkResult<WatchResponse>
    suspend fun deleteMovieFromList(movieId: Int, listId: Int): NetworkResult<WatchResponse>
    suspend fun addMovieToList(movieId: Int, listId: Int): NetworkResult<WatchResponse>
    suspend fun deleteList(listId: Int): NetworkResult<WatchResponse>
    suspend fun checkItemStatus(listId: Int, movieId: Int): NetworkResult<Boolean>
    suspend fun getWatchListMovies(listId: Int, page: Int): NetworkResult<PageModel<Movie>>
    suspend fun getWatchLists(page: Int): NetworkResult<PageModel<WatchList>>
    suspend fun getAccountWatchlistMovies(page: Int): NetworkResult<PageModel<Movie>>
    suspend fun addMovieToAccountWatchlist(movieId: Int): NetworkResult<WatchResponse>
    suspend fun removeMovieFromAccountWatchlist(movieId: Int): NetworkResult<WatchResponse>
    suspend fun getAccountFavoriteMovies(page: Int): NetworkResult<PageModel<Movie>>
    suspend fun addMovieToFavorites(movieId: Int): NetworkResult<WatchResponse>
    suspend fun removeMovieFromFavorites(movieId: Int): NetworkResult<WatchResponse>
    suspend fun addMovieRating(movieId: Int, value: Float): NetworkResult<WatchResponse>
    suspend fun deleteMovieRating(movieId: Int): NetworkResult<WatchResponse>
}
