package com.enmanuelbergling.corntime.core.data.remote.mapper

import com.enmanuelbergling.corntime.core.domain.model.tv.Episode
import com.enmanuelbergling.corntime.core.domain.model.tv.EpisodeDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.Season
import com.enmanuelbergling.corntime.core.domain.model.tv.SeasonDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.EpisodeDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.EpisodeDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.SeasonDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.SeasonDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvAccountStatesDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvShowDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvCreditsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvShowDetailsDto

internal fun TvCreditsDto.toModel() =
    TvCredits(id = id, cast = cast.map { it.toModel() }, crew = crew.map { it.toModel() })

internal fun TvShowDto.toModel() = TvShow(
    id = id,
    name = name,
    originalName = originalName,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    genreIds = genreIds,
    originalLanguage = originalLanguage,
    popularity = popularity,
    firstAirDate = firstAirDate,
    voteAverage = voteAverage / 2,
    voteCount = voteCount,
)

internal fun TvShowDetailsDto.toModel() = TvShowDetails(
    id = id,
    name = name,
    originalName = originalName,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    genres = genres.map { it.toModel() },
    firstAirDate = firstAirDate,
    lastAirDate = lastAirDate,
    numberOfSeasons = numberOfSeasons,
    numberOfEpisodes = numberOfEpisodes,
    status = status,
    tagline = tagline,
    voteAverage = voteAverage / 2,
    voteCount = voteCount,
    seasons = seasons.map { it.toModel() },
)

internal fun SeasonDto.toModel() = Season(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    episodeCount = episodeCount,
    airDate = airDate,
    voteAverage = voteAverage / 2,
)

internal fun SeasonDetailsDto.toModel() = SeasonDetails(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    airDate = airDate,
    episodes = episodes.map { it.toModel() },
)

internal fun EpisodeDto.toModel() = Episode(
    id = id,
    episodeNumber = episodeNumber,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    stillPath = stillPath,
    airDate = airDate,
    runtime = runtime,
    voteAverage = voteAverage / 2,
    voteCount = voteCount,
)

internal fun EpisodeDetailsDto.toModel() = EpisodeDetails(
    id = id,
    episodeNumber = episodeNumber,
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    stillPath = stillPath,
    airDate = airDate,
    runtime = runtime,
    voteAverage = voteAverage / 2,
    voteCount = voteCount,
    guestStars = guestStars.map { it.toModel() },
    crew = crew.map { it.toModel() },
)

internal fun TvAccountStatesDto.toModel() = TvAccountStates(
    id = id,
    favorite = favorite,
    watchlist = watchlist,
)
