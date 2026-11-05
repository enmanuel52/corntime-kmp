import Shared
import SwiftUI

private let columns = [
    GridItem(.adaptive(minimum: 150), spacing: AppDimensions.medium),
]

struct SectionScreen: View {
    @State private var model: ScreenModel<MovieSectionViewModel, MovieSectionState>
    let section: MovieSection
    let navToMovieDetails: (Int32) -> Void

    init(section: MovieSection, navToMovieDetails: @escaping (Int32) -> Void) {
        self.section = section
        self.navToMovieDetails = navToMovieDetails
        _model = State(initialValue: ScreenModels.movieSection(section))
    }

    private var paging: PagingState<Movie> { model.state.paging }
    private var movies: [Movie] { model.state.movies }

    var body: some View {
        ScrollView(showsIndicators: false) {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("section.loading")
            } else if paging.isEmpty {
                Text("No movies to show").accessibilityIdentifier("section.empty")
            }

            if let error = paging.uiError, movies.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { model.viewModel.onAction(action: MovieSectionActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("section.retry")
                }
            }

            if !movies.isEmpty {
                LazyVGrid(columns: columns) {
                    ForEach(movies, id: \.id) { movie in
                        MovieItemCard(
                            title: movie.title,
                            posterUrl: movie.posterPath,
                            voteAverage: movie.voteAverage,
                            onItem: { navToMovieDetails(movie.id) }
                        )
                        .onAppear {
                            if movie.id == movies.last?.id {
                                model.viewModel.onAction(action: MovieSectionActionOnLoadNextPage.shared)
                            }
                        }
                    }
                }
                .accessibilityIdentifier("section.grid")

                if paging.isLoadingMore {
                    ProgressView().padding()
                }
                if paging.uiError != nil {
                    Button("Retry") { model.viewModel.onAction(action: MovieSectionActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .padding()
                        .accessibilityIdentifier("section.retry")
                }
            }
        }
        .contentMargins(.horizontal, AppDimensions.medium)
        .scrollBounceBehavior(.basedOnSize)
        .navigationTitle(section.title)
    }
}
