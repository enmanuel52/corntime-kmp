package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.core.testing.data.fakePage
import com.enmanuelbergling.corntime.core.testing.util.FakeBehavior

enum class MovieRemoteFunction {
    GetMovieDetails, GetMovieCredits, GetNowPlayingMovies, GetUpcomingMovies, GetTopRatedMovies,
    GetPopularMovies, GetMovieGenres, GetMoviesByGenre, SearchMovie, GetMovieAccountStates, GetMovieReviews,
}

class FakeMovieRemoteDataSource : FakeBehavior<MovieRemoteFunction>(), MovieRemoteDataSource {
    /** Pages each paged endpoint reports; set to 0 to simulate an empty result. */
    var totalPages: Int = 1
    var accountStates: MovieAccountStates = MovieAccountStates(id = 0, favorite = false, watchlist = false)
    var reviews: List<Review> = emptyList()

    private fun moviePage(page: Int) = fakePage(FakeMovieData.MOVIES, page, totalPages) { copy(id = it) }

    override suspend fun getMovieDetails(id: Int) =
        respond(MovieRemoteFunction.GetMovieDetails, id) { FakeMovieData.DEFAULT_MOVIE_DETAILS.copy(id = id) }

    override suspend fun getMovieCredits(id: Int) =
        respond(MovieRemoteFunction.GetMovieCredits, id) { FakeMovieData.DEFAULT_MOVIE_CREDITS.copy(id = id) }

    override suspend fun getNowPlayingMovies(page: Int) =
        respond(MovieRemoteFunction.GetNowPlayingMovies, page) { moviePage(page) }

    override suspend fun getUpcomingMovies(page: Int) =
        respond(MovieRemoteFunction.GetUpcomingMovies, page) { moviePage(page) }

    override suspend fun getTopRatedMovies(page: Int) =
        respond(MovieRemoteFunction.GetTopRatedMovies, page) { moviePage(page) }

    override suspend fun getPopularMovies(page: Int) =
        respond(MovieRemoteFunction.GetPopularMovies, page) { moviePage(page) }

    override suspend fun getMovieGenres() =
        respond(MovieRemoteFunction.GetMovieGenres) { FakeMovieData.GENRES }

    override suspend fun getMoviesByGenre(genres: String, sortBy: String, page: Int) =
        respond(MovieRemoteFunction.GetMoviesByGenre, genres, sortBy, page) { moviePage(page) }

    override suspend fun searchMovie(query: String, page: Int) =
        respond(MovieRemoteFunction.SearchMovie, query, page) { moviePage(page) }

    override suspend fun getMovieAccountStates(movieId: Int) =
        respond(MovieRemoteFunction.GetMovieAccountStates, movieId) { accountStates.copy(id = movieId) }

    override suspend fun getMovieReviews(movieId: Int, page: Int) =
        respond(MovieRemoteFunction.GetMovieReviews, movieId, page) {
            fakePage(reviews, page, if (reviews.isEmpty()) 0 else totalPages) { copy(id = "$it") }
        }
}
