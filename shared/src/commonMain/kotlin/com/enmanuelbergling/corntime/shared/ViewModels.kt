package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.feature.actor.presentation.details.ActorDetailsViewModel
import com.enmanuelbergling.corntime.feature.actor.presentation.home.ActorsViewModel
import com.enmanuelbergling.corntime.feature.auth.presentation.login.LoginViewModel
import com.enmanuelbergling.corntime.feature.settings.presentation.SettingsViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.details.MovieDetailsViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.home.MoviesViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.section.MovieSectionViewModel
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/** The way SwiftUI obtains ViewModels: one scoped instance per screen, built by the Koin graph. */
object ViewModels {
    private val koin get() = KoinPlatform.getKoin()

    fun app(): ScopedViewModel<AppViewModel> = scoped { koin.get<AppViewModel>() }

    fun login(): ScopedViewModel<LoginViewModel> = scoped { koin.get<LoginViewModel>() }

    fun settings(): ScopedViewModel<SettingsViewModel> = scoped { koin.get<SettingsViewModel>() }

    fun actors(): ScopedViewModel<ActorsViewModel> = scoped { koin.get<ActorsViewModel>() }

    fun actorDetails(actorId: Int): ScopedViewModel<ActorDetailsViewModel> =
        scoped { koin.get<ActorDetailsViewModel> { parametersOf(actorId) } }

    fun movies(): ScopedViewModel<MoviesViewModel> = scoped { koin.get<MoviesViewModel>() }

    fun movieDetails(movieId: Int): ScopedViewModel<MovieDetailsViewModel> =
        scoped { koin.get<MovieDetailsViewModel> { parametersOf(movieId) } }

    fun movieSection(section: MovieSection): ScopedViewModel<MovieSectionViewModel> =
        scoped { koin.get<MovieSectionViewModel> { parametersOf(section) } }

    fun movieSearch(): ScopedViewModel<MovieSearchViewModel> = scoped { koin.get<MovieSearchViewModel>() }
}
