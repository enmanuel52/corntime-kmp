import Shared
import SwiftUI

/// Movies the user can swipe: right to move to favorites, left to remove. The watchlist, the user's
/// lists and favorites use it; only what the swipes do differs.
struct SwipeableMovieList: View {
    let movies: [Movie]
    let paging: PagingState<Movie>
    let isEmpty: Bool
    let emptyText: LocalizedStringKey
    var onFavorite: ((Int32) -> Void)? = nil
    let onRemove: (Int32) -> Void
    let onMovie: (Int32) -> Void
    let onLoadNext: () -> Void

    var body: some View {
        VStack {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("movies.list.loading")
            } else if isEmpty {
                Text(emptyText).accessibilityIdentifier("movies.list.empty")
            }

            if let error = paging.uiError, movies.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry", action: onLoadNext)
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("movies.list.retry")
                }
            }

            if !movies.isEmpty {
                List {
                    ForEach(movies, id: \.id) { movie in
                        Button { onMovie(movie.id) } label: {
                            MovieItemRow(
                                title: movie.title,
                                backdropPath: movie.backdropPath,
                                voteAverage: movie.voteAverage
                            )
                            .frame(height: 90)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("movie.row.\(movie.title)")
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) { onRemove(movie.id) } label: {
                                Label("Remove", systemImage: "trash")
                            }
                            .accessibilityIdentifier("movie.remove.\(movie.title)")
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            if let onFavorite {
                                Button { onFavorite(movie.id) } label: {
                                    Label("Favorite", systemImage: "heart")
                                }
                                .tint(.pink)
                                .accessibilityIdentifier("movie.favorite.\(movie.title)")
                            }
                        }
                        .onAppear {
                            if movie.id == movies.last?.id { onLoadNext() }
                        }
                    }

                    if paging.isLoadingMore {
                        ProgressView().frame(maxWidth: .infinity)
                    }
                    if paging.uiError != nil {
                        Button("Retry", action: onLoadNext)
                            .buttonStyle(.bordered)
                            .frame(maxWidth: .infinity)
                            .accessibilityIdentifier("movies.list.retry")
                    }
                }
                .listStyle(.plain)
                .scrollBounceBehavior(.basedOnSize)
                .accessibilityIdentifier("movies.list")
            }
        }
    }
}
