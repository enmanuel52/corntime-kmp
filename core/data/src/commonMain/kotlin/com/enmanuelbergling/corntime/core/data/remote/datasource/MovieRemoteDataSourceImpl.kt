package com.enmanuelbergling.corntime.core.data.remote.datasource

import com.enmanuelbergling.corntime.core.data.network.SessionProvider
import com.enmanuelbergling.corntime.core.data.network.safeCall
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.GenreListDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieAccountStatesDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieCreditsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MoviePageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.ReviewPageDto
import com.enmanuelbergling.corntime.core.data.remote.mapper.toModel
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.koin.core.annotation.Single

@Single
internal class MovieRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val sessionProvider: SessionProvider,
) : MovieRemoteDataSource {

    override suspend fun getMovieDetails(id: Int): NetworkResult<MovieDetails> =
        safeCall<MovieDetailsDto> { httpClient.get("movie/$id") }.map { it.toModel() }

    override suspend fun getMovieCredits(id: Int): NetworkResult<MovieCredits> =
        safeCall<MovieCreditsDto> { httpClient.get("movie/$id/credits") }.map { it.toModel() }

    override suspend fun getNowPlayingMovies(page: Int) = moviePage("movie/now_playing", page)

    override suspend fun getUpcomingMovies(page: Int) = moviePage("movie/upcoming", page)

    override suspend fun getTopRatedMovies(page: Int) = moviePage("movie/top_rated", page)

    override suspend fun getPopularMovies(page: Int) = moviePage("movie/popular", page)

    override suspend fun getMovieGenres(): NetworkResult<List<Genre>> =
        safeCall<GenreListDto> { httpClient.get("genre/movie/list") }
            .map { dto -> dto.genres.map { it.toModel() } }

    override suspend fun getMoviesByGenre(genres: String, sortBy: String, page: Int) =
        moviePage("discover/movie", page, "with_genres" to genres, "sort_by" to sortBy)

    override suspend fun searchMovie(query: String, page: Int) =
        moviePage("search/movie", page, "query" to query)

    override suspend fun getMovieAccountStates(movieId: Int): NetworkResult<MovieAccountStates> =
        sessionProvider.withSession { sessionId ->
            safeCall<MovieAccountStatesDto> {
                httpClient.get("movie/$movieId/account_states") { parameter("session_id", sessionId) }
            }.map { it.toModel() }
        }

    override suspend fun getMovieReviews(movieId: Int, page: Int): NetworkResult<PageModel<Review>> =
        safeCall<ReviewPageDto> {
            httpClient.get("movie/$movieId/reviews") { parameter("page", page) }
        }.map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }

    private suspend fun moviePage(
        path: String,
        page: Int,
        vararg query: Pair<String, Any>,
    ): NetworkResult<PageModel<Movie>> = safeCall<MoviePageDto> {
        httpClient.get(path) {
            parameter("page", page)
            query.forEach { (key, value) -> parameter(key, value) }
        }
    }.map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }
}
