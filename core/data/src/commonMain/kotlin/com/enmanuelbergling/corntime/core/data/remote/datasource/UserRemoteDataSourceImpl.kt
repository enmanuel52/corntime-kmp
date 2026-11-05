package com.enmanuelbergling.corntime.core.data.remote.datasource

import com.enmanuelbergling.corntime.core.data.network.SessionProvider
import com.enmanuelbergling.corntime.core.data.network.safeCall
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MoviePageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.RatingBody
import com.enmanuelbergling.corntime.core.data.remote.dto.user.UserDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.AccountListsPageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.CheckItemListDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.FavoriteBody
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.MediaOnListBody
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.MovieListPageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchResponseDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchlistBody
import com.enmanuelbergling.corntime.core.data.remote.mapper.asBody
import com.enmanuelbergling.corntime.core.data.remote.mapper.toModel
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.domain.model.user.WatchList
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

@Single
internal class UserRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val sessionProvider: SessionProvider,
) : UserRemoteDataSource {

    /** `GET account` resolves the account behind the session, including the account id. */
    override suspend fun getAccount(): NetworkResult<UserDetails> =
        sessionProvider.withSession { sessionId ->
            safeCall<UserDetailsDto> {
                httpClient.get("account") { parameter("session_id", sessionId) }
            }.map { it.toModel() }
        }

    override suspend fun createWatchList(listPost: CreateListPost): NetworkResult<WatchResponse> =
        sessionProvider.withSession { sessionId ->
            safeCall<WatchResponseDto> {
                httpClient.post("list") {
                    parameter("session_id", sessionId)
                    setBody(listPost.asBody())
                }
            }.map { it.toModel() }
        }

    override suspend fun deleteMovieFromList(movieId: Int, listId: Int) =
        listItem(listId, movieId, action = "remove_item")

    override suspend fun addMovieToList(movieId: Int, listId: Int) =
        listItem(listId, movieId, action = "add_item")

    override suspend fun deleteList(listId: Int): NetworkResult<WatchResponse> =
        sessionProvider.withSession { sessionId ->
            safeCall<WatchResponseDto> {
                httpClient.delete("list/$listId") { parameter("session_id", sessionId) }
            }.map { it.toModel() }
        }

    override suspend fun checkItemStatus(listId: Int, movieId: Int): NetworkResult<Boolean> =
        safeCall<CheckItemListDto> {
            httpClient.get("list/$listId/item_status") { parameter("movie_id", movieId) }
        }.map { it.itemPresent }

    /** TMDB v3 returns every list item at once, so the list is a single page. */
    override suspend fun getWatchListMovies(listId: Int, page: Int): NetworkResult<PageModel<Movie>> =
        safeCall<MovieListPageDto> {
            httpClient.get("list/$listId") { parameter("page", page) }
        }.map { dto -> PageModel(dto.items.map { it.toModel() }, totalPages = 1) }

    override suspend fun getWatchLists(page: Int): NetworkResult<PageModel<WatchList>> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<AccountListsPageDto> {
                httpClient.get("account/$accountId/lists") {
                    parameter("page", page)
                    parameter("session_id", sessionId)
                }
            }.map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }
        }

    override suspend fun getAccountWatchlistMovies(page: Int) = accountMoviePage("watchlist/movies", page)

    override suspend fun addMovieToAccountWatchlist(movieId: Int) = markWatchlist(movieId, watchlist = true)

    override suspend fun removeMovieFromAccountWatchlist(movieId: Int) = markWatchlist(movieId, watchlist = false)

    override suspend fun getAccountFavoriteMovies(page: Int) = accountMoviePage("favorite/movies", page)

    override suspend fun addMovieToFavorites(movieId: Int) = markFavorite(movieId, favorite = true)

    override suspend fun removeMovieFromFavorites(movieId: Int) = markFavorite(movieId, favorite = false)

    /** UI ratings are 0..5 stars; TMDB expects 0.5..10. */
    override suspend fun addMovieRating(movieId: Int, value: Float): NetworkResult<WatchResponse> =
        sessionProvider.withSession { sessionId ->
            safeCall<WatchResponseDto> {
                httpClient.post("movie/$movieId/rating") {
                    parameter("session_id", sessionId)
                    setBody(RatingBody(value = value * 2))
                }
            }.map { it.toModel() }
        }

    override suspend fun deleteMovieRating(movieId: Int): NetworkResult<WatchResponse> =
        sessionProvider.withSession { sessionId ->
            safeCall<WatchResponseDto> {
                httpClient.delete("movie/$movieId/rating") { parameter("session_id", sessionId) }
            }.map { it.toModel() }
        }

    private suspend fun listItem(listId: Int, movieId: Int, action: String): NetworkResult<WatchResponse> =
        sessionProvider.withSession { sessionId ->
            safeCall<WatchResponseDto> {
                httpClient.post("list/$listId/$action") {
                    parameter("session_id", sessionId)
                    setBody(MediaOnListBody(movieId))
                }
            }.map { it.toModel() }
        }

    private suspend fun accountMoviePage(path: String, page: Int): NetworkResult<PageModel<Movie>> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<MoviePageDto> {
                httpClient.get("account/$accountId/$path") {
                    parameter("page", page)
                    parameter("session_id", sessionId)
                }
            }.map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }
        }

    private suspend fun markWatchlist(movieId: Int, watchlist: Boolean): NetworkResult<WatchResponse> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<WatchResponseDto> {
                httpClient.post("account/$accountId/watchlist") {
                    parameter("session_id", sessionId)
                    setBody(WatchlistBody(mediaId = movieId, watchlist = watchlist))
                }
            }.map { it.toModel() }
        }

    private suspend fun markFavorite(movieId: Int, favorite: Boolean): NetworkResult<WatchResponse> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<WatchResponseDto> {
                httpClient.post("account/$accountId/favorite") {
                    parameter("session_id", sessionId)
                    setBody(FavoriteBody(mediaId = movieId, favorite = favorite))
                }
            }.map { it.toModel() }
        }
}
