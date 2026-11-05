package com.enmanuelbergling.corntime.core.data.remote.dto.actor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TMDB sends null for many person fields (birthday, place of birth, photo, ...), so they default
// instead of failing the whole response.

@Serializable
internal data class ActorDto(
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("gender") val gender: Int = 0,
    @SerialName("id") val id: Int,
    @SerialName("known_for_department") val knownForDepartment: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("popularity") val popularity: Double = 0.0,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
internal data class ActorPageDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<ActorDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
internal data class ActorDetailsDto(
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("also_known_as") val alsoKnownAs: List<String> = emptyList(),
    @SerialName("biography") val biography: String? = null,
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("deathday") val deathday: String? = null,
    @SerialName("gender") val gender: Int = 0,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("id") val id: Int,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("popularity") val popularity: Double = 0.0,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
internal data class KnownMovieDto(
    @SerialName("id") val id: Int,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("title") val title: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)

@Serializable
internal data class MovieCreditsCastDto(
    @SerialName("cast") val cast: List<KnownMovieDto> = emptyList(),
)
