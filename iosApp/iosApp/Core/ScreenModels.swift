import Shared

/// One factory per shared ViewModel, so views never touch Koin or `ScopedViewModel` directly.
@MainActor
enum ScreenModels {
    static func movies() -> ScreenModel<MoviesViewModel, MoviesState> {
        ScreenModel(make: { ViewModels.shared.movies() }, state: { $0.state })
    }

    static func movieDetails(movieId: Int32) -> ScreenModel<MovieDetailsViewModel, MovieDetailsState> {
        ScreenModel(make: { ViewModels.shared.movieDetails(movieId: movieId) }, state: { $0.state })
    }

    static func movieSection(_ section: MovieSection) -> ScreenModel<MovieSectionViewModel, MovieSectionState> {
        ScreenModel(make: { ViewModels.shared.movieSection(section: section) }, state: { $0.state })
    }

    static func movieSearch() -> ScreenModel<MovieSearchViewModel, MovieSearchState> {
        ScreenModel(make: { ViewModels.shared.movieSearch() }, state: { $0.state })
    }

    static func app() -> ScreenModel<AppViewModel, AppState> {
        ScreenModel(make: { ViewModels.shared.app() }, state: { $0.state })
    }

    static func actors() -> ScreenModel<ActorsViewModel, ActorsState> {
        ScreenModel(make: { ViewModels.shared.actors() }, state: { $0.state })
    }

    static func actorDetails(actorId: Int32) -> ScreenModel<ActorDetailsViewModel, ActorDetailsState> {
        ScreenModel(make: { ViewModels.shared.actorDetails(actorId: actorId) }, state: { $0.state })
    }

    static func favoriteMovies() -> ScreenModel<FavoriteMoviesViewModel, FavoriteMoviesState> {
        ScreenModel(make: { ViewModels.shared.favoriteMovies() }, state: { $0.state })
    }

    static func watchlistHome() -> ScreenModel<WatchlistHomeViewModel, WatchlistHomeState> {
        ScreenModel(make: { ViewModels.shared.watchlistHome() }, state: { $0.state })
    }

    static func createdWatchLists() -> ScreenModel<CreatedWatchListsViewModel, CreatedWatchListsState> {
        ScreenModel(make: { ViewModels.shared.createdWatchLists() }, state: { $0.state })
    }

    static func watchListDetails(listId: Int32) -> ScreenModel<WatchListDetailsViewModel, WatchListDetailsState> {
        ScreenModel(make: { ViewModels.shared.watchListDetails(listId: listId) }, state: { $0.state })
    }

    static func login() -> ScreenModel<LoginViewModel, LoginState> {
        ScreenModel(make: { ViewModels.shared.login() }, state: { $0.state })
    }

    static func settings() -> ScreenModel<SettingsViewModel, SettingsState> {
        ScreenModel(make: { ViewModels.shared.settings() }, state: { $0.state })
    }

    static func tvShows() -> ScreenModel<TvShowsViewModel, TvShowsState> {
        ScreenModel(make: { ViewModels.shared.tvShows() }, state: { $0.state })
    }

    static func tvShowDetails(tvShowId: Int32) -> ScreenModel<TvShowDetailsViewModel, TvShowDetailsState> {
        ScreenModel(make: { ViewModels.shared.tvShowDetails(tvShowId: tvShowId) }, state: { $0.state })
    }

    static func tvShowSection(_ section: TvShowsSection) -> ScreenModel<TvShowSectionViewModel, TvShowSectionState> {
        ScreenModel(make: { ViewModels.shared.tvShowSection(section: section) }, state: { $0.state })
    }

    static func tvShowSearch() -> ScreenModel<TvShowSearchViewModel, TvShowSearchState> {
        ScreenModel(make: { ViewModels.shared.tvShowSearch() }, state: { $0.state })
    }

    static func seasons(tvShowId: Int32) -> ScreenModel<SeasonsViewModel, SeasonsState> {
        ScreenModel(make: { ViewModels.shared.seasons(tvShowId: tvShowId) }, state: { $0.state })
    }

    static func episodes(tvShowId: Int32, seasonNumber: Int32) -> ScreenModel<EpisodesViewModel, EpisodesState> {
        ScreenModel(
            make: { ViewModels.shared.episodes(tvShowId: tvShowId, seasonNumber: seasonNumber) },
            state: { $0.state }
        )
    }

    static func episodeDetails(
        tvShowId: Int32,
        seasonNumber: Int32,
        episodeNumber: Int32
    ) -> ScreenModel<EpisodeDetailsViewModel, EpisodeDetailsState> {
        ScreenModel(
            make: {
                ViewModels.shared.episodeDetails(
                    tvShowId: tvShowId,
                    seasonNumber: seasonNumber,
                    episodeNumber: episodeNumber
                )
            },
            state: { $0.state }
        )
    }

    static func favoriteTvShows() -> ScreenModel<FavoriteTvShowsViewModel, FavoriteTvShowsState> {
        ScreenModel(make: { ViewModels.shared.favoriteTvShows() }, state: { $0.state })
    }

    static func watchlistTvShows() -> ScreenModel<WatchlistTvShowsViewModel, WatchlistTvShowsState> {
        ScreenModel(make: { ViewModels.shared.watchlistTvShows() }, state: { $0.state })
    }
}
