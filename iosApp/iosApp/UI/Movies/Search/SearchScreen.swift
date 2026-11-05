import Shared
import SwiftUI

struct SearchScreen: View {
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
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                }
            }
        }
    }
}
