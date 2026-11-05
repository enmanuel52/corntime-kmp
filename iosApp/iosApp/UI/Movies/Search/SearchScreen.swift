import Shared
import SwiftUI

/// What the search tab looks through; picked with the search scope bar.
enum SearchScopeKind: Hashable {
    case movies, tvShows
}

struct SearchScreen: View {
    var query: String
    var scope: SearchScopeKind
    let navToMovieDetails: (Int32) -> Void
    let navToTvShowDetails: (Int32) -> Void

    var body: some View {
        switch scope {
        case .movies:
            MovieSearchContent(query: query, navToMovieDetails: navToMovieDetails)
        case .tvShows:
            TvShowSearchContent(query: query, navToTvShowDetails: navToTvShowDetails)
        }
    }
}

private struct MovieSearchContent: View {
    @State private var model = ScreenModels.movieSearch()
    var query: String
    let navToMovieDetails: (Int32) -> Void

    private func send(_ action: MovieSearchAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        VStack {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("search.loading")
            } else if state.showNoResults {
                Text("No movies to show").accessibilityIdentifier("search.empty")
            }

            if let error = paging.uiError, state.movies.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(MovieSearchActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("search.retry")
                }
            }

            if !state.movies.isEmpty {
                List {
                    ForEach(state.movies, id: \.id) { movie in
                        Button {
                            send(MovieSearchActionOnMovieClick(movieId: movie.id))
                        } label: {
                            MovieItemRow(
                                title: movie.title,
                                backdropPath: movie.backdropPath,
                                voteAverage: movie.voteAverage
                            ).frame(height: 90)
                        }
                        .buttonStyle(.plain)
                        .onAppear {
                            if movie.id == state.movies.last?.id {
                                send(MovieSearchActionOnLoadNextPage.shared)
                            }
                        }
                    }

                    if paging.isLoadingMore {
                        ProgressView().frame(maxWidth: .infinity)
                    }
                    if paging.uiError != nil {
                        Button("Retry") { send(MovieSearchActionOnLoadNextPage.shared) }
                            .accessibilityIdentifier("search.retry")
                    }
                }
                .listStyle(.plain)
                .accessibilityIdentifier("search.results")
            }
        }
        .onChange(of: query) { _, newValue in
            send(MovieSearchActionOnQueryChange(query: newValue))
        }
        .onAppear {
            // coming back from the other scope: the typed text is still there, search it again
            if !query.isEmpty { send(MovieSearchActionOnQueryChange(query: query)) }
        }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                }
            }
        }
    }
}
