package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.remote.ActorRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.core.testing.data.fakePage
import com.enmanuelbergling.corntime.core.testing.util.FakeBehavior

enum class ActorRemoteFunction { GetPopularActors, GetActorDetails, GetMoviesByActor }

class FakeActorRemoteDataSource : FakeBehavior<ActorRemoteFunction>(), ActorRemoteDataSource {
    var totalPages: Int = 1
    var knownMovies = FakeActorData.KNOWN_MOVIES

    override suspend fun getPopularActors(page: Int) =
        respond(ActorRemoteFunction.GetPopularActors, page) {
            fakePage(listOf(FakeActorData.ACTOR, FakeActorData.ACTOR.copy(name = "Ann Actor")), page, totalPages) {
                copy(id = it)
            }
        }

    override suspend fun getActorDetails(id: Int) =
        respond(ActorRemoteFunction.GetActorDetails, id) { FakeActorData.ACTOR_DETAILS.copy(id = id) }

    override suspend fun getMoviesByActor(actorId: Int) =
        respond(ActorRemoteFunction.GetMoviesByActor, actorId) { knownMovies }
}
