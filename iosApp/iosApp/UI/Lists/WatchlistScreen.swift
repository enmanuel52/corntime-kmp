import Shared
import SwiftUI

struct WatchlistScreen: View {
    @State private var model = ScreenModels.watchlistHome()
    let navToMovieDetails: (Int32) -> Void
    let navToLists: () -> Void

    private func send(_ action: WatchlistHomeAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        SwipeableMovieList(
            movies: state.movies,
            paging: state.paging,
            isEmpty: state.isEmpty,
            emptyText: "Your watchlist is empty",
            onFavorite: { send(WatchlistHomeActionOnMoveToFavorites(movieId: $0)) },
            onRemove: { send(WatchlistHomeActionOnRemove(movieId: $0)) },
            onMovie: { send(WatchlistHomeActionOnMovieClick(movieId: $0)) },
            onLoadNext: { send(WatchlistHomeActionOnLoadNextPage.shared) }
        )
        .navigationTitle("Watchlist")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { send(WatchlistHomeActionOnListsClick.shared) } label: {
                    Image(systemName: "list.bullet")
                }
                .accessibilityLabel("My lists")
                .accessibilityIdentifier("watchlist.lists")
            }
        }
        .undoBanner(
            state.undoable,
            removed: String(localized: "Movie removed from watchlist"),
            movedToFavorites: String(localized: "Movie added to favorites"),
            onUndo: { send(WatchlistHomeActionOnUndo(movieId: $0)) }
        )
        .errorAlert(
            state.removalError,
            onDismiss: { send(WatchlistHomeActionOnDismissRemovalError.shared) },
            onRetry: { send(WatchlistHomeActionOnRetryRemoval.shared) }
        )
        .onAppear { send(WatchlistHomeActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                case .navigateToLists: navToLists()
                }
            }
        }
    }
}
