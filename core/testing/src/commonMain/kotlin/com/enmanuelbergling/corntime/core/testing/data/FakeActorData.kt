package com.enmanuelbergling.corntime.core.testing.data

import com.enmanuelbergling.corntime.core.domain.model.actor.Actor
import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie

object FakeActorData {

    val ACTOR_DETAILS = ActorDetails(
        adult = false,
        alsoKnownAs = listOf(),
        biography = "",
        birthday = "",
        deathday = null,
        gender = 1,
        homepage = null,
        id = 0,
        imdbId = "",
        knownForDepartment = "",
        name = "Tim Bergling",
        placeOfBirth = "Sweden",
        popularity = 10.0,
        profilePath = "tim_path"
    )

    val ACTOR = Actor(
        adult = false,
        gender = 1,
        id = 0,
        knownForDepartment = "",
        name = "Tim Bergling",
        popularity = 10.0,
        profilePath = "tim_path",
        originalName = "Tim"
    )

    val KNOWN_MOVIES = listOf(
        KnownMovie(id = 1, posterPath = "/one.jpg", title = "First Known", voteAverage = 4.0),
        KnownMovie(id = 2, posterPath = null, title = "Second Known", voteAverage = 3.5),
    )
}
