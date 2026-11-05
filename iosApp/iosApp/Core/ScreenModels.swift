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

    static func login() -> ScreenModel<LoginViewModel, LoginState> {
        ScreenModel(make: { ViewModels.shared.login() }, state: { $0.state })
    }

    static func settings() -> ScreenModel<SettingsViewModel, SettingsState> {
        ScreenModel(make: { ViewModels.shared.settings() }, state: { $0.state })
    }
}
