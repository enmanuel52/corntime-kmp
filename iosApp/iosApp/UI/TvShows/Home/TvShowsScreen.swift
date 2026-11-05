import Shared
import SwiftUI

struct TvShowsScreen: View {
    @State private var model = ScreenModels.tvShows()
    let navToTvShowSection: (TvShowsSection) -> Void
    let navToTvShowDetails: (Int32) -> Void
    let isLoggedIn: Bool
    let navToFavorites: () -> Void

    private func onTvShow(_ tvShowId: Int32) {
        model.viewModel.onAction(action: TvShowsActionOnTvShowClick(tvShowId: tvShowId))
    }

    private func onSection(_ section: TvShowsSection) {
        model.viewModel.onAction(action: TvShowsActionOnSectionClick(section: section))
    }

    var body: some View {
        let state = model.state

        List {
            if state.isLoading && state.isFeedEmpty {
                ProgressView("Loading TV shows")
                    .listSectionSeparator(.hidden)
                    .accessibilityIdentifier("tv.loading")
            }

            ForEach(state.orderedSections, id: \.section) { item in
                switch item.section {
                case .airingToday:
                    MainTvShowRow(
                        label: item.section.title,
                        tvShows: item.tvShows,
                        onItem: onTvShow
                    ) { onSection(item.section) }
                default:
                    TvShowRow(
                        label: item.section.title,
                        tvShows: item.tvShows,
                        onItem: onTvShow
                    ) { onSection(item.section) }
                }
            }
        }
        .listStyle(.plain)
        .listSectionSpacing(AppDimensions.medium)
        .scrollIndicators(.hidden)
        .navigationTitle("TV Shows")
        .toolbar {
            if isLoggedIn {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: navToFavorites) {
                        Image(systemName: "heart")
                    }
                    .accessibilityLabel("Favorites")
                    .accessibilityIdentifier("tv.favorites")
                }
            }
        }
        .accessibilityIdentifier("tv.list")
        .errorAlert(
            state.error,
            onDismiss: { model.viewModel.onAction(action: TvShowsActionOnDismissError.shared) },
            onRetry: { model.viewModel.onAction(action: TvShowsActionOnRetry.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToTvShowDetails(details.tvShowId)
                case .navigateToSection(let section): navToTvShowSection(section.section)
                }
            }
        }
    }
}

private struct TvShowRow: View {
    let label: String
    let tvShows: [TvShow]
    let onItem: (Int32) -> Void
    let onSection: () -> Void

    var body: some View {
        if !tvShows.isEmpty {
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: AppDimensions.medium) {
                        ForEach(tvShows, id: \.id) { tvShow in
                            TvShowItemCard(
                                name: tvShow.name,
                                posterPath: tvShow.posterPath,
                                voteAverage: tvShow.voteAverage,
                                onItem: { onItem(tvShow.id) }
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

private struct MainTvShowRow: View {
    let label: String
    let tvShows: [TvShow]
    let onItem: (Int32) -> Void
    let onSection: () -> Void

    @Environment(\.verticalSizeClass) var verticalSizeClass

    var body: some View {
        if !tvShows.isEmpty {
            Section {
                ScrollView(.horizontal) {
                    LazyHStack(spacing: 0) {
                        ForEach(tvShows, id: \.id) { tvShow in
                            TvShowMainCard(
                                name: tvShow.name,
                                backdropPath: tvShow.backdropPath,
                                voteAverage: tvShow.voteAverage
                            ) {
                                onItem(tvShow.id)
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
