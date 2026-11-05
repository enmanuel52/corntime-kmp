import Shared
import SwiftUI

/// The "TV Shows" segment of the watchlist screen; the screen itself owns the title and the segment picker.
struct WatchlistTvShowsContent: View {
    @State private var model = ScreenModels.watchlistTvShows()
    let navToTvShowDetails: (Int32) -> Void

    private func send(_ action: WatchlistTvShowsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        SwipeableTvShowList(
            tvShows: state.tvShows,
            paging: state.paging,
            isEmpty: state.isEmpty,
            emptyText: "Your TV watchlist is empty",
            onFavorite: { send(WatchlistTvShowsActionOnMoveToFavorites(tvShowId: $0)) },
            onRemove: { send(WatchlistTvShowsActionOnRemove(tvShowId: $0)) },
            onTvShow: { send(WatchlistTvShowsActionOnTvShowClick(tvShowId: $0)) },
            onLoadNext: { send(WatchlistTvShowsActionOnLoadNextPage.shared) }
        )
        .undoBanner(
            state.undoable,
            removed: String(localized: "TV show removed from watchlist"),
            movedToFavorites: String(localized: "TV show added to favorites"),
            onUndo: { send(WatchlistTvShowsActionOnUndo(tvShowId: $0)) }
        )
        .errorAlert(
            state.removalError,
            onDismiss: { send(WatchlistTvShowsActionOnDismissRemovalError.shared) },
            onRetry: { send(WatchlistTvShowsActionOnRetryRemoval.shared) }
        )
        .onAppear { send(WatchlistTvShowsActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToTvShowDetails(details.tvShowId)
                }
            }
        }
    }
}
