package com.enmanuelbergling.corntime.core.data.remote

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.data.network.MockHttp
import com.enmanuelbergling.corntime.core.data.network.json
import com.enmanuelbergling.corntime.core.data.network.path
import com.enmanuelbergling.corntime.core.data.network.query
import com.enmanuelbergling.corntime.core.data.network.status
import com.enmanuelbergling.corntime.core.data.remote.datasource.ActorRemoteDataSourceImpl
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ActorRemoteDataSourceImplTest {

    private fun person(id: Int, profilePath: String? = "/p$id.jpg") = """
        {"adult":false,"gender":2,"id":$id,"known_for_department":"Acting","name":"Actor $id",
         "original_name":"Actor $id","popularity":5.5,"profile_path":${profilePath?.let { "\"$it\"" } ?: "null"},"extra":1}
    """.trimIndent()

    @Test
    fun givenPopularActors_whenFetchingPageThree_thenSendsThePageAndKeepsTotalPages() = runTest {
        val http = MockHttp {
            json("""{"page":3,"results":[${person(1)},${person(2, profilePath = null)}],"total_pages":40,"total_results":800}""")
        }

        val page = (ActorRemoteDataSourceImpl(http.client).getPopularActors(3) as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/person/popular")
        assertThat(http.lastRequest.query("page")).isEqualTo("3")
        assertThat(page.totalPages).isEqualTo(40)
        assertThat(page.results.map { it.name }).containsExactly("Actor 1", "Actor 2")
        assertThat(page.results[1].profilePath).isNull()
    }

    @Test
    fun givenAPersonWithNullFields_whenFetchingDetails_thenTheyBecomeEmptyInsteadOfFailing() = runTest {
        val http = MockHttp {
            json("""{"id":7,"name":"Mystery","birthday":null,"biography":null,"place_of_birth":null,"imdb_id":null,"profile_path":null,"known_for_department":null,"popularity":1.5}""")
        }

        val details = (ActorRemoteDataSourceImpl(http.client).getActorDetails(7) as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/person/7")
        assertThat(details.name).isEqualTo("Mystery")
        assertThat(details.birthday).isEqualTo("")
        assertThat(details.biography).isEqualTo("")
        assertThat(details.placeOfBirth).isEqualTo("")
        assertThat(details.profilePath).isEqualTo("")
        assertThat(details.deathday).isNull()
    }

    @Test
    fun givenMovieCredits_whenFetchingKnownMovies_thenOnlyTheCastIsMappedWithRatingOnFiveStars() = runTest {
        val http = MockHttp {
            json("""{"id":7,"cast":[{"id":1,"title":"A","poster_path":null,"vote_average":8.0}],"crew":[{"id":9,"title":"Crew only"}]}""")
        }

        val movies = (ActorRemoteDataSourceImpl(http.client).getMoviesByActor(7) as Result.Success).data

        assertThat(http.lastRequest.path).isEqualTo("/3/person/7/movie_credits")
        assertThat(movies.map { it.title }).containsExactly("A")
        assertThat(movies.single().voteAverage).isEqualTo(4.0)
        assertThat(movies.single().posterPath).isNull()
    }

    @Test
    fun givenAnActorWithNoMovies_whenFetchingKnownMovies_thenEmptyList() = runTest {
        val http = MockHttp { json("""{"id":7,"cast":[]}""") }

        val result = ActorRemoteDataSourceImpl(http.client).getMoviesByActor(7)

        assertThat(result).isEqualTo(Result.Success(emptyList()))
    }

    @Test
    fun givenUnknownPerson_whenFetchingDetails_thenNotFound() = runTest {
        val http = MockHttp { status(HttpStatusCode.NotFound) }

        val result = ActorRemoteDataSourceImpl(http.client).getActorDetails(404)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.NotFound))
    }
}
