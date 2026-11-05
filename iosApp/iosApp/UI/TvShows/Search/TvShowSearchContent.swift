import Shared
import SwiftUI

/// The "TV Shows" scope of the search tab, the counterpart of the movie results.
struct TvShowSearchContent: View {
    @State private var model = ScreenModels.tvShowSearch()
    var query: String
    let navToTvShowDetails: (Int32) -> Void

    private func send(_ action: TvShowSearchAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        VStack {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("tvsearch.loading")
            } else if state.showNoResults {
                Text("No TV shows to show").accessibilityIdentifier("tvsearch.empty")
            }

            if let error = paging.uiError, state.tvShows.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(TvShowSearchActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("tvsearch.retry")
                }
            }

            if !state.tvShows.isEmpty {
                List {
                    ForEach(state.tvShows, id: \.id) { tvShow in
                        Button {
                            send(TvShowSearchActionOnTvShowClick(tvShowId: tvShow.id))
                        } label: {
                            MovieItemRow(
                                title: tvShow.name,
                                backdropPath: tvShow.backdropPath,
                                voteAverage: tvShow.voteAverage
                            ).frame(height: 90)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("tvsearch.row.\(tvShow.name)")
                        .onAppear {
                            if tvShow.id == state.tvShows.last?.id {
                                send(TvShowSearchActionOnLoadNextPage.shared)
                            }
                        }
                    }

                    if paging.isLoadingMore {
                        ProgressView().frame(maxWidth: .infinity)
                    }
                    if paging.uiError != nil {
                        Button("Retry") { send(TvShowSearchActionOnLoadNextPage.shared) }
                            .accessibilityIdentifier("tvsearch.retry")
                    }
                }
                .listStyle(.plain)
                .accessibilityIdentifier("tvsearch.results")
            }
        }
        .onChange(of: query) { _, newValue in
            send(TvShowSearchActionOnQueryChange(query: newValue))
        }
        .onAppear {
            // switching scope keeps the typed text: search it for TV shows too
            if !query.isEmpty { send(TvShowSearchActionOnQueryChange(query: query)) }
        }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToTvShowDetails(details.tvShowId)
                }
            }
        }
    }
}
