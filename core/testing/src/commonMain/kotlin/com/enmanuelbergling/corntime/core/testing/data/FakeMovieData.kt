package com.enmanuelbergling.corntime.core.testing.data

import com.enmanuelbergling.corntime.core.domain.model.movie.Cast
import com.enmanuelbergling.corntime.core.domain.model.movie.Crew
import com.enmanuelbergling.corntime.core.domain.model.movie.Genre
import com.enmanuelbergling.corntime.core.domain.model.movie.Movie
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieCredits
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieDetails
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.domain.model.movie.ReviewAuthorDetails

object FakeMovieData {

    private val GODFATHER = Movie(
        adult = false,
        backdropPath = null,
        genreIds = listOf(),
        id = 0,
        originalLanguage = "English",
        originalTitle = "The Godfather",
        overview = "This is a mafia movie",
        popularity = 9.0,
        posterPath = "godfather_path",
        releaseDate = "",
        title = "The Godfather",
        video = false,
        voteAverage = 8.0,
        voteCount = 2873
    )

    private val GODFATHER_2 = Movie(
        adult = false,
        backdropPath = null,
        genreIds = listOf(),
        id = 1,
        originalLanguage = "English",
        originalTitle = "The Godfather 2",
        overview = "This is a mafia movie, 2nd part",
        popularity = 9.0,
        posterPath = "godfather2_path",
        releaseDate = "",
        title = "The Godfather 2",
        video = false,
        voteAverage = 7.0,
        voteCount = 2673
    )

    val MOVIES = listOf(GODFATHER, GODFATHER_2)

    private val GODFATHER_GENRE = Genre(1, "mafia")

    private val GODFATHER_DETAILS = MovieDetails(
        adult = false,
        id = 0,
        originalLanguage = "English",
        originalTitle = "The Godfather 2",
        overview = "This is a mafia movie, 2nd part",
        popularity = 9.0,
        posterPath = "godfather2_path",
        releaseDate = "",
        title = "The Godfather 2",
        video = false,
        voteAverage = 7.0,
        voteCount = 2673, productionCompanies = listOf(),
        productionCountries = listOf(),
        genres = (1..4).map { GODFATHER_GENRE.copy(id = it) },
        budget = 0,
        belongsToCollection = null,
        backdropPath = "godfather_backdrop",
        homepage = "",
        runtime = 0,
        imdbId = "",
        revenue = 2_000_000,
        spokenLanguages = listOf(),
        status = "",
        tagline = ""
    )

    private val DEFAULT_GENRE = GODFATHER_GENRE

    val GENRES = (1..5).map { DEFAULT_GENRE.copy(id = it) }

    val DEFAULT_MOVIE_DETAILS = GODFATHER_DETAILS

    private val DEFAULT_CAST = Cast(
        adult = false,
        gender = 1,
        id = 0,
        knownForDepartment = "",
        name = "Tim Bergling",
        popularity = 10.0,
        profilePath = "tim_path",
        castId = 0,
        character = "Avicii",
        creditId = "",
        order = 0,
        originalName = "Tim"
    )

    private val DEFAULT_CREW = Crew(
        adult = false,
        gender = 1,
        id = 0,
        knownForDepartment = "",
        name = "Tim Bergling",
        popularity = 10.0,
        profilePath = "tim_path",
        creditId = "",
        originalName = "Tim",
        department = "",
        job = "Dj"
    )

    val DEFAULT_MOVIE_CREDITS = MovieCredits(
        id = 0,
        cast = (1..4).map { DEFAULT_CAST.copy(id = it) },
        crew = (1..4).map { DEFAULT_CREW.copy(id = it) },
    )

    /** Three reviews with distinct ids; the first two carry a rating, the last does not. */
    val REVIEWS = listOf(
        Review(
            id = "review-1",
            author = "Tim",
            content = "A masterpiece. The pacing, the score and the performances all work together.",
            createdAt = "2024-01-05T10:00:00.000Z",
            url = "https://www.themoviedb.org/review/review-1",
            rating = 4.5f,
            authorDetails = ReviewAuthorDetails(name = "Tim", username = "timcorn", avatarPath = null),
        ),
        Review(
            id = "review-2",
            author = "Ana",
            content = "Good, but a little long.",
            createdAt = "2023-06-20T18:30:00.000Z",
            url = "https://www.themoviedb.org/review/review-2",
            rating = 3f,
            authorDetails = ReviewAuthorDetails(name = "Ana", username = "ana", avatarPath = "/ana.jpg"),
        ),
        Review(
            id = "review-3",
            author = "Leo",
            content = "Not for me.",
            createdAt = "2022-11-11T08:15:00.000Z",
            url = "https://www.themoviedb.org/review/review-3",
            rating = null,
            authorDetails = ReviewAuthorDetails(name = "Leo", username = "leo", avatarPath = null),
        ),
    )
}
