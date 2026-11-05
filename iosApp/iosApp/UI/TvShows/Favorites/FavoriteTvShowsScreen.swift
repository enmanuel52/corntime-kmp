import Shared
import SwiftUI

struct FavoriteTvShowsScreen: View {
    @State private var model = ScreenModels.favoriteTvShows()
    let navToTvShowDetails: (Int32) -> Void

    private func send(_ action: FavoriteTvShowsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        SwipeableTvShowList(
            tvShows: state.tvShows,
            paging: state.paging,
            isEmpty: state.isEmpty,
            emptyText: "No favorite TV shows yet",
            onRemove: { send(FavoriteTvShowsActionOnRemove(tvShowId: $0)) },
            onTvShow: { send(FavoriteTvShowsActionOnTvShowClick(tvShowId: $0)) },
            onLoadNext: { send(FavoriteTvShowsActionOnLoadNextPage.shared) }
        )
        .navigationTitle("Favorite TV shows")
        .undoBanner(
            state.undoable,
            removed: String(localized: "TV show removed from favorites"),
            movedToFavorites: String(localized: "TV show removed from favorites"),
            onUndo: { send(FavoriteTvShowsActionOnUndoRemove(tvShowId: $0)) }
        )
        .errorAlert(
            state.removalError,
            onDismiss: { send(FavoriteTvShowsActionOnDismissRemovalError.shared) },
            onRetry: { send(FavoriteTvShowsActionOnRetryRemoval.shared) }
        )
        .onAppear { send(FavoriteTvShowsActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToTvShowDetails(details.tvShowId)
                case .navigateBack: break
                }
            }
        }
    }
}
