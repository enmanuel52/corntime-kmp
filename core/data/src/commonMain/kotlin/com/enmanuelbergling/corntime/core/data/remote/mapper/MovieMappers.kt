package com.enmanuelbergling.corntime.core.data.remote.mapper

import com.enmanuelbergling.corntime.core.data.remote.dto.movie.BelongsToCollectionDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.CastDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.CrewDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.GenreDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.AuthorDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieAccountStatesDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieCreditsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.MovieDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.ProductionCompanyDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.ReviewDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.ProductionCountryDto
import com.enmanuelbergling.corntime.core.data.remote.dto.movie.SpokenLanguageDto
import com.enmanuelbergling.corntime.core.domain.model.movie.BelongsToCollection
import com.enmanuelbergling.corntime.core.domain.model.movie.Cast
import com.enmanuelbergling.corntime.core.domain.model.movie.Crew
import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.domain.model.movie.ProductionCompany
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.domain.model.movie.ReviewAuthorDetails
import com.enmanuelbergling.corntime.core.domain.model.movie.ProductionCountry
import com.enmanuelbergling.corntime.core.domain.model.movie.SpokenLanguage

internal fun BelongsToCollectionDto.toModel() = BelongsToCollection(
    id = id,
    name = name,
    posterPath = posterPath,
    backdropPath = backdropPath.orEmpty()
)

internal fun GenreDto.toModel() = Genre(id, name)

internal fun MovieDto.toModel() = Movie(
    adult = adult,
    backdropPath = backdropPath,
    genreIds = genreIds,
    id = id,
    originalLanguage = originalLanguage,
    originalTitle = originalTitle,
    overview = overview,
    popularity = popularity,
    posterPath = posterPath.orEmpty(),
    releaseDate = releaseDate,
    title = title,
    video = video,
    voteAverage = voteAverage / 2,
    voteCount = voteCount
)

internal fun MovieDetailsDto.toModel() = MovieDetails(
    adult = adult,
    backdropPath = backdropPath,
    belongsToCollection = belongsToCollection?.toModel(),
    budget = budget,
    genres = genres.map { it.toModel() },
    homepage = homepage,
    id = id,
    imdbId = imdbId,
    originalLanguage = originalLanguage,
    originalTitle = originalTitle,
    overview = overview,
    popularity = popularity,
    posterPath = posterPath,
    productionCompanies = productionCompanies.map { it.toModel() },
    productionCountries = productionCountries.map { it.toModel() },
    releaseDate = releaseDate,
    revenue = revenue,
    runtime = runtime,
    spokenLanguages = spokenLanguages.map { it.toModel() },
    status = status,
    tagline = tagline,
    title = title,
    video = video,
    voteAverage = voteAverage / 2,
    voteCount = voteCount
)

internal fun ProductionCompanyDto.toModel() = ProductionCompany(
    id = id,
    logoPath = logoPath,
    name = name,
    originCountry = originCountry
)

internal fun ProductionCountryDto.toModel() = ProductionCountry(iso31661 = iso31661, name = name)

internal fun SpokenLanguageDto.toModel() = SpokenLanguage(
    englishName = englishName,
    iso6391 = iso6391,
    name = name
)

internal fun CastDto.toModel() = Cast(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = knownForDepartment,
    name = name,
    originalName = originalName,
    popularity = popularity,
    profilePath = profilePath,
    castId = castId,
    character = character,
    creditId = creditId,
    order = order
)

internal fun CrewDto.toModel() = Crew(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = knownForDepartment,
    name = name,
    originalName = originalName,
    popularity = popularity,
    profilePath = profilePath,
    creditId = creditId,
    department = department,
    job = job
)

internal fun MovieCreditsDto.toModel() =
    MovieCredits(id = id, cast = cast.map { it.toModel() }, crew = crew.map { it.toModel() })

internal fun MovieAccountStatesDto.toModel() = MovieAccountStates(
    id = id,
    favorite = favorite,
    watchlist = watchlist,
    rated = rated?.value?.div(2)
)

internal fun ReviewDto.toModel() = Review(
    id = id,
    author = author,
    content = content,
    createdAt = createdAt,
    url = url,
    rating = authorDetails.rating?.div(2),
    authorDetails = authorDetails.toModel()
)

internal fun AuthorDetailsDto.toModel() = ReviewAuthorDetails(
    name = name,
    username = username,
    avatarPath = avatarPath
)