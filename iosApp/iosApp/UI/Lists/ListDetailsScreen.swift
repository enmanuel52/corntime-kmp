import Shared
import SwiftUI

struct ListDetailsScreen: View {
    @State private var model: ScreenModel<WatchListDetailsViewModel, WatchListDetailsState>
    let name: String
    let navToMovieDetails: (Int32) -> Void

    init(listId: Int32, name: String, navToMovieDetails: @escaping (Int32) -> Void) {
        self.name = name
        self.navToMovieDetails = navToMovieDetails
        _model = State(initialValue: ScreenModels.watchListDetails(listId: listId))
    }

    private func send(_ action: WatchListDetailsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        SwipeableMovieList(
            movies: state.movies,
            paging: state.paging,
            isEmpty: state.isEmpty,
            emptyText: "This list has no movies",
            onFavorite: { send(WatchListDetailsActionOnMoveToFavorites(movieId: $0)) },
            onRemove: { send(WatchListDetailsActionOnRemove(movieId: $0)) },
            onMovie: { send(WatchListDetailsActionOnMovieClick(movieId: $0)) },
            onLoadNext: { send(WatchListDetailsActionOnLoadNextPage.shared) }
        )
        .navigationTitle(name)
        .undoBanner(
            state.undoable,
            removed: String(localized: "Movie removed from the list"),
            movedToFavorites: String(localized: "Movie added to favorites"),
            onUndo: { send(WatchListDetailsActionOnUndo(movieId: $0)) }
        )
        .errorAlert(
            state.removalError,
            onDismiss: { send(WatchListDetailsActionOnDismissRemovalError.shared) },
            onRetry: { send(WatchListDetailsActionOnRetryRemoval.shared) }
        )
        .onAppear { send(WatchListDetailsActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                case .navigateBack, .addShortcut, .removeShortcut: break // shortcuts are Android only
                }
            }
        }
    }
}
