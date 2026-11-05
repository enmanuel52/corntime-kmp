import Shared
import SwiftUI

private let columns = [
    GridItem(.adaptive(minimum: 150), spacing: AppDimensions.medium),
]

struct MoviesFilterScreen: View {
    @State private var model = ScreenModels.moviesFilter()
    let navToMovieDetails: (Int32) -> Void

    private func send(_ action: MoviesFilterAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        ScrollView(showsIndicators: false) {
            if state.genres.isEmpty {
                if state.isLoadingGenres {
                    ProgressView().accessibilityIdentifier("filter.loading")
                } else if let error = state.genresError {
                    VStack(spacing: AppDimensions.medium) {
                        Text(error.message)
                        Button("Retry") { send(MoviesFilterActionOnRetryGenres.shared) }
                            .buttonStyle(.bordered)
                            .accessibilityIdentifier("filter.retryGenres")
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: AppDimensions.large) {
                    SortSection(selected: state.filter.sortBy) {
                        send(MoviesFilterActionOnPickSortCriteria(criteria: $0))
                    }
                    GenresSection(state: state) { send(MoviesFilterActionOnToggleGenre(genre: $0)) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, AppDimensions.large)
            }

            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("filter.moviesLoading")
            } else if paging.isEmpty {
                Text("No movies to show").accessibilityIdentifier("filter.empty")
            }

            if let error = paging.uiError, state.movies.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(MoviesFilterActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("filter.retry")
                }
            }

            if !state.movies.isEmpty {
                LazyVGrid(columns: columns) {
                    ForEach(state.movies, id: \.id) { movie in
                        MovieItemCard(
                            title: movie.title,
                            posterUrl: movie.posterPath,
                            voteAverage: movie.voteAverage,
                            onItem: { send(MoviesFilterActionOnMovieClick(movieId: movie.id)) }
                        )
                        .onAppear {
                            if movie.id == state.movies.last?.id {
                                send(MoviesFilterActionOnLoadNextPage.shared)
                            }
                        }
                    }
                }
                .accessibilityIdentifier("filter.grid")

                if paging.isLoadingMore {
                    ProgressView().padding()
                }
                if paging.uiError != nil {
                    Button("Retry") { send(MoviesFilterActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .padding()
                        .accessibilityIdentifier("filter.retry")
                }
            }
        }
        .contentMargins(.horizontal, AppDimensions.medium)
        .scrollBounceBehavior(.basedOnSize)
        .navigationTitle("Advanced Movie Filter")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if state.hasActiveFilter {
                Button("Clear") { send(MoviesFilterActionOnClearFilter.shared) }
                    .accessibilityIdentifier("filter.clear")
            }
        }
        .errorAlert(
            state.genres.isEmpty ? nil : state.genresError,
            onDismiss: { send(MoviesFilterActionOnDismissError.shared) },
            onRetry: { send(MoviesFilterActionOnRetryGenres.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }
}

private struct SortSection: View {
    let selected: SortCriteria
    let onPick: (SortCriteria) -> Void

    private let all: [SortCriteria] = [.popularity, .voteAverage, .voteCount, .revenue]

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimensions.medium) {
            Text("Order By").font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: AppDimensions.medium) {
                    ForEach(all, id: \.self) { criteria in
                        Chip(title: criteria.title, isSelected: criteria == selected) { onPick(criteria) }
                            .accessibilityIdentifier("filter.sort.\(criteria.name)")
                    }
                }
            }
        }
    }
}

private struct GenresSection: View {
    let state: MoviesFilterState
    let onToggle: (Genre) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimensions.medium) {
            Text("Genres").font(.headline)
            FlowLayout {
                ForEach(state.genres, id: \.id) { genre in
                    Chip(title: genre.name, isSelected: state.isSelected(genre: genre)) { onToggle(genre) }
                        .accessibilityIdentifier("filter.genre.\(genre.id)")
                }
            }
        }
    }
}

private struct Chip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, AppDimensions.large)
                .padding(.vertical, AppDimensions.medium)
                .background(isSelected ? Color.accentColor : Color(.secondarySystemFill), in: Capsule())
                .foregroundStyle(isSelected ? Color.white : Color.primary)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

extension SortCriteria {
    var title: String {
        switch self {
        case .popularity: return String(localized: "Popularity")
        case .voteAverage: return String(localized: "Vote Average")
        case .voteCount: return String(localized: "Vote Count")
        case .revenue: return String(localized: "Revenue")
        }
    }
}
