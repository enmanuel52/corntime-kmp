package com.enmanuelbergling.corntime.core.data.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.data.network.Json
import com.enmanuelbergling.corntime.core.data.network.MockHttp
import com.enmanuelbergling.corntime.core.data.network.json
import com.enmanuelbergling.corntime.core.data.network.path
import com.enmanuelbergling.corntime.core.data.network.query
import com.enmanuelbergling.corntime.core.data.network.status
import com.enmanuelbergling.corntime.core.data.remote.datasource.UserRemoteDataSourceImpl
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserRemoteDataSourceImplTest {

    private fun dataSource(http: MockHttp, session: Session) = UserRemoteDataSourceImpl(http.client, session.provider)

    private fun HttpRequestData.bodyText() = (body as TextContent).text

    @Test
    fun givenLoggedOut_whenCallingAnyAccountEndpoint_thenUnauthorizedAndNoRequestIsSent() = runTest {
        val http = MockHttp { json(Json.WATCH_RESPONSE) }
        val user = dataSource(http, Session.LoggedOut)
        val unauthorized = Result.Failure(DataError.Network.Unauthorized)

        assertThat(user.getAccount()).isEqualTo(unauthorized)
        assertThat(user.createWatchList(CreateListPost("n", "d"))).isEqualTo(unauthorized)
        assertThat(user.deleteList(1)).isEqualTo(unauthorized)
        assertThat(user.getWatchLists(1)).isEqualTo(unauthorized)
        assertThat(user.getAccountWatchlistMovies(1)).isEqualTo(unauthorized)
        assertThat(user.getAccountFavoriteMovies(1)).isEqualTo(unauthorized)
        assertThat(user.addMovieToAccountWatchlist(1)).isEqualTo(unauthorized)
        assertThat(user.removeMovieFromFavorites(1)).isEqualTo(unauthorized)
        assertThat(user.addMovieRating(1, 4f)).isEqualTo(unauthorized)
        assertThat(http.requests.size).isEqualTo(0)
    }

    @Test
    fun givenSessionButNoSyncedAccountId_whenCallingAccountEndpoints_thenUnauthorizedInsteadOfUsingAWrongId() = runTest {
        val http = MockHttp { json(Json.moviePage(1, 1)) }
        val session = Session(sessionId = "s", user = null)

        val result = dataSource(http, session).getAccountWatchlistMovies(1)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
        assertThat(http.requests.size).isEqualTo(0)
    }

    @Test
    fun givenLoggedIn_whenGettingAccount_thenResolvesAccountFromSessionAndMapsIt() = runTest {
        val http = MockHttp { json(Json.account(42)) }

        val user = (dataSource(http, Session.LoggedIn).getAccount() as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/account")
        assertThat(http.lastRequest.query("session_id")).isEqualTo("session-1")
        assertThat(user.id).isEqualTo(42)
        assertThat(user.username).isEqualTo("timcorn")
        assertThat(user.avatarPath).isEqualTo("")
    }

    @Test
    fun givenLoggedIn_whenAddingToWatchlist_thenPostsToTheRealAccountIdWithWatchlistTrue() = runTest {
        val http = MockHttp { json(Json.WATCH_RESPONSE) }

        dataSource(http, Session.LoggedIn).addMovieToAccountWatchlist(movieId = 7)

        assertThat(http.lastRequest.method).isEqualTo(HttpMethod.Post)
        assertThat(http.lastRequest.path).isEqualTo("/3/account/42/watchlist")
        assertThat(http.lastRequest.query("session_id")).isEqualTo("session-1")
        val body = http.lastRequest.bodyText()
        assertThat(body.contains("\"media_type\":\"movie\"")).isEqualTo(true)
        assertThat(body.contains("\"media_id\":7")).isEqualTo(true)
        assertThat(body.contains("\"watchlist\":true")).isEqualTo(true)
    }

    @Test
    fun givenLoggedIn_whenRemovingFromWatchlist_thenSendsWatchlistFalse() = runTest {
        val http = MockHttp { json(Json.WATCH_RESPONSE) }

        dataSource(http, Session.LoggedIn).removeMovieFromAccountWatchlist(movieId = 7)

        assertThat(http.lastRequest.bodyText().contains("\"watchlist\":false")).isEqualTo(true)
    }

    @Test
    fun givenRating_whenAdding_thenFiveStarScaleIsDoubledForTmdb() = runTest {
        val http = MockHttp { json(Json.WATCH_RESPONSE) }

        dataSource(http, Session.LoggedIn).addMovieRating(movieId = 3, value = 3.5f)

        assertThat(http.lastRequest.path).isEqualTo("/3/movie/3/rating")
        assertThat(http.lastRequest.bodyText().contains("\"value\":7.0")).isEqualTo(true)
    }

    @Test
    fun givenListItems_whenFetching_thenIsASingleCompletePage() = runTest {
        val http = MockHttp { json("""{"created_by":"tim","description":"d","favorite_count":0,"id":"9","items":[${Json.movie(1)},${Json.movie(2)}],"item_count":2,"iso_639_1":"en","name":"l","poster_path":null}""") }

        val page = (dataSource(http, Session.LoggedOut).getWatchListMovies(listId = 9, page = 1) as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/list/9")
        assertThat(page.results.size).isEqualTo(2)
        assertThat(page.totalPages).isEqualTo(1)
    }

    @Test
    fun givenTmdbRejectsSession_whenMutating_thenUnauthorizedIsSurfaced() = runTest {
        val http = MockHttp { status(HttpStatusCode.Unauthorized) }

        val result = dataSource(http, Session.LoggedIn).addMovieToFavorites(1)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
    }
}
