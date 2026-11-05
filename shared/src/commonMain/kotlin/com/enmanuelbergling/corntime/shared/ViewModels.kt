package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.feature.actor.presentation.details.ActorDetailsViewModel
import com.enmanuelbergling.corntime.feature.actor.presentation.home.ActorsViewModel
import com.enmanuelbergling.corntime.feature.auth.presentation.login.LoginViewModel
import com.enmanuelbergling.corntime.feature.settings.presentation.SettingsViewModel
import com.enmanuelbergling.corntime.feature.favorites.presentation.home.FavoriteMoviesViewModel
import com.enmanuelbergling.corntime.feature.watchlists.presentation.created.CreatedWatchListsViewModel
import com.enmanuelbergling.corntime.feature.watchlists.presentation.details.WatchListDetailsViewModel
import com.enmanuelbergling.corntime.feature.watchlists.presentation.home.WatchlistHomeViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.details.MovieDetailsViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.home.MoviesViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.section.MovieSectionViewModel
import com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows.FavoriteTvShowsViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.details.TvShowDetailsViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails.EpisodeDetailsViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes.EpisodesViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.home.TvShowsViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.search.TvShowSearchViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons.SeasonsViewModel
import com.enmanuelbergling.corntime.feature.tvshows.presentation.section.TvShowSectionViewModel
import com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows.WatchlistTvShowsViewModel
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

    fun favoriteMovies(): ScopedViewModel<FavoriteMoviesViewModel> = scoped { koin.get<FavoriteMoviesViewModel>() }

    fun watchlistHome(): ScopedViewModel<WatchlistHomeViewModel> = scoped { koin.get<WatchlistHomeViewModel>() }

    fun createdWatchLists(): ScopedViewModel<CreatedWatchListsViewModel> =
        scoped { koin.get<CreatedWatchListsViewModel>() }

    fun watchListDetails(listId: Int): ScopedViewModel<WatchListDetailsViewModel> =
        scoped { koin.get<WatchListDetailsViewModel> { parametersOf(listId) } }

    fun movies(): ScopedViewModel<MoviesViewModel> = scoped { koin.get<MoviesViewModel>() }

    fun movieDetails(movieId: Int): ScopedViewModel<MovieDetailsViewModel> =
        scoped { koin.get<MovieDetailsViewModel> { parametersOf(movieId) } }

    fun movieSection(section: MovieSection): ScopedViewModel<MovieSectionViewModel> =
        scoped { koin.get<MovieSectionViewModel> { parametersOf(section) } }

    fun movieSearch(): ScopedViewModel<MovieSearchViewModel> = scoped { koin.get<MovieSearchViewModel>() }

    fun tvShows(): ScopedViewModel<TvShowsViewModel> = scoped { koin.get<TvShowsViewModel>() }

    fun tvShowDetails(tvShowId: Int): ScopedViewModel<TvShowDetailsViewModel> =
        scoped { koin.get<TvShowDetailsViewModel> { parametersOf(tvShowId) } }

    fun tvShowSection(section: TvShowsSection): ScopedViewModel<TvShowSectionViewModel> =
        scoped { koin.get<TvShowSectionViewModel> { parametersOf(section) } }

    fun tvShowSearch(): ScopedViewModel<TvShowSearchViewModel> = scoped { koin.get<TvShowSearchViewModel>() }

    fun seasons(tvShowId: Int): ScopedViewModel<SeasonsViewModel> =
        scoped { koin.get<SeasonsViewModel> { parametersOf(tvShowId) } }

    fun episodes(tvShowId: Int, seasonNumber: Int): ScopedViewModel<EpisodesViewModel> =
        scoped { koin.get<EpisodesViewModel> { parametersOf(tvShowId, seasonNumber) } }

    fun episodeDetails(tvShowId: Int, seasonNumber: Int, episodeNumber: Int): ScopedViewModel<EpisodeDetailsViewModel> =
        scoped { koin.get<EpisodeDetailsViewModel> { parametersOf(tvShowId, seasonNumber, episodeNumber) } }

    fun favoriteTvShows(): ScopedViewModel<FavoriteTvShowsViewModel> = scoped { koin.get<FavoriteTvShowsViewModel>() }

    fun watchlistTvShows(): ScopedViewModel<WatchlistTvShowsViewModel> = scoped { koin.get<WatchlistTvShowsViewModel>() }
}
