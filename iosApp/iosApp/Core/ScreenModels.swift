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
}
