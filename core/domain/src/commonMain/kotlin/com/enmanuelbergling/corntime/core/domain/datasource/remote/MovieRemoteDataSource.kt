package com.enmanuelbergling.corntime.core.domain.datasource.remote

import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.domain.model.movie.Review

typealias NetworkResult<T> = Result<T, DataError.Network>

interface MovieRemoteDataSource {
    suspend fun getMovieDetails(id: Int): NetworkResult<MovieDetails>
    suspend fun getMovieCredits(id: Int): NetworkResult<MovieCredits>
    suspend fun getNowPlayingMovies(page: Int = 1): NetworkResult<PageModel<Movie>>
    suspend fun getUpcomingMovies(page: Int = 1): NetworkResult<PageModel<Movie>>
    suspend fun getTopRatedMovies(page: Int = 1): NetworkResult<PageModel<Movie>>
    suspend fun getPopularMovies(page: Int = 1): NetworkResult<PageModel<Movie>>
    suspend fun getMovieGenres(): NetworkResult<List<Genre>>

    /** @param genres comma separated genre ids, @param sortBy TMDB sort key, e.g. popularity.desc. */
    suspend fun getMoviesByGenre(genres: String, sortBy: String, page: Int): NetworkResult<PageModel<Movie>>
    suspend fun searchMovie(query: String, page: Int): NetworkResult<PageModel<Movie>>

    /** Requires a logged-in session, otherwise [DataError.Network.Unauthorized]. */
    suspend fun getMovieAccountStates(movieId: Int): NetworkResult<MovieAccountStates>
    suspend fun getMovieReviews(movieId: Int, page: Int): NetworkResult<PageModel<Review>>
}
