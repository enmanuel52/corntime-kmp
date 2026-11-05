import Shared
import SwiftUI

/// TV shows the user can swipe: right to move to favorites, left to remove. The TV watchlist and favorites
/// use it; only what the swipes do differs.
struct SwipeableTvShowList: View {
    let tvShows: [TvShow]
    let paging: PagingState<TvShow>
    let isEmpty: Bool
    let emptyText: LocalizedStringKey
    var onFavorite: ((Int32) -> Void)? = nil
    let onRemove: (Int32) -> Void
    let onTvShow: (Int32) -> Void
    let onLoadNext: () -> Void

    var body: some View {
        VStack {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("tv.list.loading")
            } else if isEmpty {
                Text(emptyText).accessibilityIdentifier("tv.list.empty")
            }

            if let error = paging.uiError, tvShows.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry", action: onLoadNext)
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("tv.list.retry")
                }
            }

            if !tvShows.isEmpty {
                List {
                    ForEach(tvShows, id: \.id) { tvShow in
                        Button { onTvShow(tvShow.id) } label: {
                            MovieItemRow(
                                title: tvShow.name,
                                backdropPath: tvShow.backdropPath,
                                voteAverage: tvShow.voteAverage
                            )
                            .frame(height: 90)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("tv.row.\(tvShow.name)")
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) { onRemove(tvShow.id) } label: {
                                Label("Remove", systemImage: "trash")
                            }
                            .accessibilityIdentifier("tv.remove.\(tvShow.name)")
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            if let onFavorite {
                                Button { onFavorite(tvShow.id) } label: {
                                    Label("Favorite", systemImage: "heart")
                                }
                                .tint(.pink)
                                .accessibilityIdentifier("tv.favorite.\(tvShow.name)")
                            }
                        }
                        .onAppear {
                            if tvShow.id == tvShows.last?.id { onLoadNext() }
                        }
                    }

                    if paging.isLoadingMore {
                        ProgressView().frame(maxWidth: .infinity)
                    }
                    if paging.uiError != nil {
                        Button("Retry", action: onLoadNext)
                            .buttonStyle(.bordered)
                            .frame(maxWidth: .infinity)
                            .accessibilityIdentifier("tv.list.retry")
                    }
                }
                .listStyle(.plain)
                .scrollBounceBehavior(.basedOnSize)
                .accessibilityIdentifier("tv.list")
            }
        }
    }
}
