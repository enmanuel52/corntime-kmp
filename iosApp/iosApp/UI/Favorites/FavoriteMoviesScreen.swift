import Shared
import SwiftUI

struct FavoriteMoviesScreen: View {
    @State private var model = ScreenModels.favoriteMovies()
    let navToMovieDetails: (Int32) -> Void

    private func send(_ action: FavoriteMoviesAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        SwipeableMovieList(
            movies: state.movies,
            paging: state.paging,
            isEmpty: state.isEmpty,
            emptyText: "No favorites yet",
            onRemove: { send(FavoriteMoviesActionOnRemove(movieId: $0)) },
            onMovie: { send(FavoriteMoviesActionOnMovieClick(movieId: $0)) },
            onLoadNext: { send(FavoriteMoviesActionOnLoadNextPage.shared) }
        )
        .navigationTitle("Favorites")
        .undoBanner(
            state.undoable,
            removed: String(localized: "Movie removed from favorites"),
            movedToFavorites: String(localized: "Movie removed from favorites"),
            onUndo: { send(FavoriteMoviesActionOnUndoRemove(movieId: $0)) }
        )
        .errorAlert(
            state.removalError,
            onDismiss: { send(FavoriteMoviesActionOnDismissRemovalError.shared) },
            onRetry: { send(FavoriteMoviesActionOnRetryRemoval.shared) }
        )
        .onAppear { send(FavoriteMoviesActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                case .navigateBack: break
                }
            }
        }
    }
}
