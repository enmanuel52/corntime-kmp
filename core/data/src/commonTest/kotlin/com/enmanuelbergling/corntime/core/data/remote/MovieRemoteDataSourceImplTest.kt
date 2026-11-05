package com.enmanuelbergling.corntime.core.data.remote

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.data.network.Json
import com.enmanuelbergling.corntime.core.data.network.MockHttp
import com.enmanuelbergling.corntime.core.data.network.json
import com.enmanuelbergling.corntime.core.data.network.path
import com.enmanuelbergling.corntime.core.data.network.query
import com.enmanuelbergling.corntime.core.data.network.status
import com.enmanuelbergling.corntime.core.data.remote.datasource.MovieRemoteDataSourceImpl
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MovieRemoteDataSourceImplTest {

    private fun dataSource(http: MockHttp, session: Session = Session.LoggedOut) =
        MovieRemoteDataSourceImpl(http.client, session.provider)

    @Test
    fun givenPopularMovies_whenFetchingPageTwo_thenHitsEndpointWithPageAndMapsResults() = runTest {
        val http = MockHttp { json(Json.moviePage(totalPages = 9, 1, 2)) }

        val result = dataSource(http).getPopularMovies(page = 2)

        assertThat(http.lastRequest.path).isEqualTo("/3/movie/popular")
        assertThat(http.lastRequest.query("page")).isEqualTo("2")
        val page = (result as Result.Success).data
        assertThat(page.totalPages).isEqualTo(9)
        assertThat(page.results.map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun givenMovieWithoutPoster_whenMapping_thenPosterIsEmptyAndRatingIsHalvedToFiveStars() = runTest {
        val http = MockHttp { json("""{"page":1,"total_pages":1,"total_results":1,"results":[${Json.movie(5, posterPath = null)}]}""") }

        val movie = (dataSource(http).getTopRatedMovies() as Result.Success).data.results.single()

        assertThat(movie.posterPath).isEqualTo("")
        assertThat(movie.voteAverage).isEqualTo(4.0)
    }

    @Test
    fun givenEmptyResultsPage_whenFetching_thenSuccessWithNoItems() = runTest {
        val http = MockHttp { json(Json.moviePage(totalPages = 0)) }

        val page = (dataSource(http).getNowPlayingMovies() as Result.Success).data

        assertThat(page.results.size).isEqualTo(0)
        assertThat(page.totalPages).isEqualTo(0)
    }

    @Test
    fun givenSearchWithSpecialCharacters_whenFetching_thenQueryIsEncodedNotCorrupted() = runTest {
        val http = MockHttp { json(Json.moviePage(1, 1)) }

        dataSource(http).searchMovie("fast & furious: 100%", page = 1)

        assertThat(http.lastRequest.path).isEqualTo("/3/search/movie")
        assertThat(http.lastRequest.query("query")).isEqualTo("fast & furious: 100%")
    }

    @Test
    fun givenGenreFilter_whenFetching_thenSendsDiscoverQuery() = runTest {
        val http = MockHttp { json(Json.moviePage(1, 1)) }

        dataSource(http).getMoviesByGenre(genres = "28,12", sortBy = "vote_average.desc", page = 3)

        assertThat(http.lastRequest.path).isEqualTo("/3/discover/movie")
        assertThat(http.lastRequest.query("with_genres")).isEqualTo("28,12")
        assertThat(http.lastRequest.query("sort_by")).isEqualTo("vote_average.desc")
        assertThat(http.lastRequest.query("page")).isEqualTo("3")
    }

    @Test
    fun givenServerError_whenFetching_thenTypedServerError() = runTest {
        val http = MockHttp { status(HttpStatusCode.InternalServerError) }

        assertThat(dataSource(http).getPopularMovies()).isEqualTo(Result.Failure(DataError.Network.ServerError))
    }

    @Test
    fun givenLoggedOut_whenReadingAccountStates_thenUnauthorizedWithoutTouchingTheNetwork() = runTest {
        val http = MockHttp { json("{}") }

        val result = dataSource(http, Session.LoggedOut).getMovieAccountStates(1)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
        assertThat(http.requests.size).isEqualTo(0)
    }

    @Test
    fun givenLoggedIn_whenReadingAccountStates_thenSessionIdIsSent() = runTest {
        val http = MockHttp { json("""{"id":1,"favorite":true,"rated":false,"watchlist":true}""") }

        val states = (dataSource(http, Session.LoggedIn).getMovieAccountStates(1) as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/movie/1/account_states")
        assertThat(http.lastRequest.query("session_id")).isEqualTo("session-1")
        assertThat(states.watchlist).isEqualTo(true)
        assertThat(states.rated).isNull()
    }

    @Test
    fun givenBlankSessionId_whenReadingAccountStates_thenTreatedAsLoggedOut() = runTest {
        val http = MockHttp { json("{}") }

        val result = dataSource(http, Session("   ")).getMovieAccountStates(1)

        assertThat(result).isInstanceOf<Result.Failure<DataError.Network>>()
    }
}
