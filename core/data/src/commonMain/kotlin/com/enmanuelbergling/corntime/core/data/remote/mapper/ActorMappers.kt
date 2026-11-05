package com.enmanuelbergling.corntime.core.data.remote.mapper

import com.enmanuelbergling.corntime.core.data.remote.dto.actor.ActorDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.actor.ActorDto
import com.enmanuelbergling.corntime.core.data.remote.dto.actor.KnownMovieDto
import com.enmanuelbergling.corntime.core.domain.model.actor.Actor
import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie

internal fun ActorDto.toModel() = Actor(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = knownForDepartment,
    name = name,
    originalName = originalName,
    popularity = popularity,
    profilePath = profilePath,
)

internal fun ActorDetailsDto.toModel() = ActorDetails(
    adult = adult,
    alsoKnownAs = alsoKnownAs,
    biography = biography.orEmpty(),
    birthday = birthday.orEmpty(),
    deathday = deathday,
    gender = gender,
    homepage = homepage,
    id = id,
    imdbId = imdbId.orEmpty(),
    knownForDepartment = knownForDepartment.orEmpty(),
    name = name,
    placeOfBirth = placeOfBirth.orEmpty(),
    popularity = popularity,
    profilePath = profilePath.orEmpty(),
)

/** TMDB rates out of 10; the apps show five stars. */
internal fun KnownMovieDto.toModel() = KnownMovie(
    id = id,
    posterPath = posterPath,
    title = title,
    voteAverage = voteAverage / 2,
)
