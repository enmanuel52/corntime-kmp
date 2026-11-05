import Shared
import SwiftUI

private enum WatchlistKind: Hashable {
    case movies, tvShows
}

/// The account's watchlist, with a Movies | TV Shows segment; "My lists" only applies to movies.
struct WatchlistScreen: View {
    @State private var kind = WatchlistKind.movies
    let navToMovieDetails: (Int32) -> Void
    let navToTvShowDetails: (Int32) -> Void
    let navToLists: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Picker("Watchlist", selection: $kind) {
                Text("Movies").tag(WatchlistKind.movies)
                Text("TV Shows").tag(WatchlistKind.tvShows)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.bottom, AppDimensions.medium)
            .accessibilityIdentifier("watchlist.kind")

            switch kind {
            case .movies:
                WatchlistMoviesContent(navToMovieDetails: navToMovieDetails, navToLists: navToLists)
            case .tvShows:
                WatchlistTvShowsContent(navToTvShowDetails: navToTvShowDetails)
            }
        }
        .navigationTitle("Watchlist")
    }
}

private struct WatchlistMoviesContent: View {
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
