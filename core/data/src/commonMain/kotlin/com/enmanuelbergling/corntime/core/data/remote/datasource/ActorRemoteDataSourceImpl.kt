package com.enmanuelbergling.corntime.core.data.remote.datasource

import com.enmanuelbergling.corntime.core.data.network.safeCall
import com.enmanuelbergling.corntime.core.data.remote.dto.actor.ActorDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.actor.ActorPageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.actor.MovieCreditsCastDto
import com.enmanuelbergling.corntime.core.data.remote.mapper.toModel
import com.enmanuelbergling.corntime.core.domain.datasource.remote.ActorRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.koin.core.annotation.Single

@Single
internal class ActorRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : ActorRemoteDataSource {

    override suspend fun getPopularActors(page: Int) =
        safeCall<ActorPageDto> { httpClient.get("person/popular") { parameter("page", page) } }
            .map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }

    override suspend fun getActorDetails(id: Int) =
        safeCall<ActorDetailsDto> { httpClient.get("person/$id") }.map { it.toModel() }

    override suspend fun getMoviesByActor(actorId: Int) =
        safeCall<MovieCreditsCastDto> { httpClient.get("person/$actorId/movie_credits") }
            .map { dto -> dto.cast.map { it.toModel() } }
}
