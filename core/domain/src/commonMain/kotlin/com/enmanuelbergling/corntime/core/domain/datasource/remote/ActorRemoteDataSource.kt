package com.enmanuelbergling.corntime.core.domain.datasource.remote

import com.enmanuelbergling.corntime.core.domain.model.actor.Actor
import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel

interface ActorRemoteDataSource {
    suspend fun getPopularActors(page: Int): NetworkResult<PageModel<Actor>>
    suspend fun getActorDetails(id: Int): NetworkResult<ActorDetails>

    /** Movies the actor appears in (their movie credits' cast). */
    suspend fun getMoviesByActor(actorId: Int): NetworkResult<List<KnownMovie>>
}
