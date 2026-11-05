import Shared
import SwiftUI

struct MoviesScreen: View {
    @State private var model = ScreenModels.movies()
    let navToMovieSection: (MovieSection) -> Void
    let navToMovieDetails: (Int32) -> Void
    let navToSettings: () -> Void
    let navToFilter: () -> Void
    let isLoggedIn: Bool
    let navToFavorites: () -> Void

    private func onMovie(_ movieId: Int32) {
        model.viewModel.onAction(action: MoviesActionOnMovieClick(movieId: movieId))
    }

    private func onSection(_ section: MovieSection) {
        model.viewModel.onAction(action: MoviesActionOnSectionClick(section: section))
    }

    var body: some View {
        let state = model.state

        List {
            if state.isLoading && state.isFeedEmpty {
                ProgressView("Loading movies")
                    .listSectionSeparator(.hidden)
                    .accessibilityIdentifier("movies.loading")
            }

            ForEach(state.orderedSections, id: \.section) { item in
                switch item.section {
                case .nowPlaying:
                    MainMovieRow(
                        label: item.section.title,
                        movies: item.movies,
                        onItem: onMovie
                    ) { onSection(item.section) }
                default:
                    MovieRow(
                        label: item.section.title,
                        movies: item.movies,
                        onItem: onMovie
                    ) { onSection(item.section) }
                }
            }
        }
        .listStyle(.plain)
        .listSectionSpacing(AppDimensions.medium)
        .scrollIndicators(.hidden)
        .navigationTitle("Movies")
        .toolbar {
            if isLoggedIn {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: navToFavorites) {
                        Image(systemName: "heart")
                    }
                    .accessibilityLabel("Favorites")
                    .accessibilityIdentifier("movies.favorites")
                }
            }
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button(action: navToFilter) {
                    Image(systemName: "slider.horizontal.3")
                }
                .accessibilityLabel("Filter")
                .accessibilityIdentifier("movies.filter")

                Button(action: navToSettings) {
                    Image(systemName: "person.crop.circle")
                }
                .accessibilityLabel("Profile")
                .accessibilityIdentifier("movies.profile")
            }
        }
        .accessibilityIdentifier("movies.list")
        .errorAlert(
            state.error,
            onDismiss: { model.viewModel.onAction(action: MoviesActionOnDismissError.shared) },
            onRetry: { model.viewModel.onAction(action: MoviesActionOnRetry.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToMovieDetails(details.movieId)
                case .navigateToSection(let section): navToMovieSection(section.section)
                }
            }
        }
    }
}

struct MovieRow: View {
    let label: String
    let movies: [Movie]
    let onItem: (Int32) -> Void
    let onSection: () -> Void

    var body: some View {
        if !movies.isEmpty {
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: AppDimensions.medium) {
                        ForEach(movies, id: \.id) { movie in
                            MovieItemCard(
                                title: movie.title,
                                posterUrl: movie.posterPath,
                                voteAverage: movie.voteAverage,
                                onItem: { onItem(movie.id) }
                            )
                            .frame(width: 200)
                            .hStackPlacing()
                        }
                    }
                }
                .contentMargins(.horizontal, AppDimensions.large)
            } header: {
                SectionLabelRow(label: label, onSeeAll: onSection)
            }
            .listRowInsets(.init())
            .listSectionSeparator(.hidden)
        }
    }
}

struct MainMovieRow: View {
    let label: String
    let movies: [Movie]
    let onItem: (Int32) -> Void
    let onSection: () -> Void

    @Environment(\.verticalSizeClass) var verticalSizeClass

    var body: some View {
        if !movies.isEmpty {
            Section {
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 0) {
                        ForEach(movies, id: \.id) { movie in
                            MovieMainCard(
                                title: movie.title,
                                backdropPath: movie.backdropPath,
                                voteAverage: movie.voteAverage
                            ) {
                                onItem(movie.id)
                            }
                            .containerRelativeFrame(
                                .horizontal,
                                count: verticalSizeClass == .regular ? 1 : 2,
                                spacing: AppDimensions.medium
                            )
                            .intagramStory()
                        }
                    }
                    .scrollTargetLayout()
                }
                .scrollIndicators(.visible)
                .contentMargins(AppDimensions.medium, for: .scrollContent)
                .scrollTargetBehavior(.viewAligned)
            } header: {
                SectionLabelRow(label: label, onSeeAll: onSection)
            }
            .listRowInsets(.init())
            .listSectionSeparator(.hidden)
        }
    }
}

struct SectionLabelRow: View {
    let label: String
    let onSeeAll: () -> Void

    var body: some View {
        HStack(alignment: .center) {
            SectionLabel(label: label)

            Spacer()

            Button("See all") {
                onSeeAll()
            }
            .padding()
            .buttonStyle(.bordered)
            .accessibilityIdentifier("seeall.\(label)")
        }
    }
}

#Preview {
    SectionLabelRow(label: "Now playing") {}
}
