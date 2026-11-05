import Shared
import SwiftUI

/// What the search tab looks through; picked with the search scope bar.
enum SearchScopeKind: Hashable {
    case movies, tvShows
}

struct SearchScreen: View {
    @Binding var query: String
    /// Bumped when the keyboard's search key is pressed.
    var submissions: Int
    var scope: SearchScopeKind
    let navToMovieDetails: (Int32) -> Void
    let navToTvShowDetails: (Int32) -> Void

    var body: some View {
        switch scope {
        case .movies:
            MovieSearchContent(
                query: $query,
                submissions: submissions,
                navToMovieDetails: navToMovieDetails
            )
        case .tvShows:
            TvShowSearchContent(query: query, navToTvShowDetails: navToTvShowDetails)
        }
    }
}

private struct MovieSearchContent: View {
    @State private var model = ScreenModels.movieSearch()
    @Binding var query: String
    var submissions: Int
    let navToMovieDetails: (Int32) -> Void

    private func send(_ action: MovieSearchAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        VStack {
            if state.showSuggestions {
                SuggestionsList(
                    suggestions: state.suggestions,
                    onPick: { picked in
                        // the search field owns what is typed, so the pick has to land there too
                        query = picked
                        send(MovieSearchActionOnSuggestionClick(query: picked))
                    },
                    onDelete: { send(MovieSearchActionOnDeleteSuggestion(query: $0)) },
                    onClear: { send(MovieSearchActionOnClearSuggestions.shared) }
                )
            } else if paging.isLoadingFirstPage {
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
        .onChange(of: submissions) { _, _ in
            send(MovieSearchActionOnSearchSubmit.shared)
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

/// Recent searches, shown while nothing is typed.
private struct SuggestionsList: View {
    let suggestions: [String]
    let onPick: (String) -> Void
    let onDelete: (String) -> Void
    let onClear: () -> Void

    var body: some View {
        List {
            Section {
                ForEach(suggestions, id: \.self) { suggestion in
                    HStack {
                        Button {
                            onPick(suggestion)
                        } label: {
                            Label(suggestion, systemImage: "clock.arrow.circlepath")
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("search.suggestion.\(suggestion)")

                        Button {
                            onDelete(suggestion)
                        } label: {
                            Image(systemName: "xmark")
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("Delete recent search")
                        .accessibilityIdentifier("search.deleteSuggestion.\(suggestion)")
                    }
                }
            } header: {
                HStack {
                    Text("Recent searches")
                    Spacer()
                    Button("Clear", action: onClear)
                        .accessibilityIdentifier("search.clearSuggestions")
                }
            }
        }
        .listStyle(.plain)
        .accessibilityIdentifier("search.suggestions")
    }
}
