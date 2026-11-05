import Shared
import SwiftUI

private let columns = [
    GridItem(.adaptive(minimum: 150), spacing: AppDimensions.medium),
]

struct TvShowSectionScreen: View {
    @State private var model: ScreenModel<TvShowSectionViewModel, TvShowSectionState>
    let section: TvShowsSection
    let navToTvShowDetails: (Int32) -> Void

    init(section: TvShowsSection, navToTvShowDetails: @escaping (Int32) -> Void) {
        self.section = section
        self.navToTvShowDetails = navToTvShowDetails
        _model = State(initialValue: ScreenModels.tvShowSection(section))
    }

    private func loadNextPage() {
        model.viewModel.onAction(action: TvShowSectionActionOnLoadNextPage.shared)
    }

    private var paging: PagingState<TvShow> { model.state.paging }
    private var tvShows: [TvShow] { model.state.tvShows }

    var body: some View {
        ScrollView(showsIndicators: false) {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("tvsection.loading")
            } else if paging.isEmpty {
                Text("No TV shows to show").accessibilityIdentifier("tvsection.empty")
            }

            if let error = paging.uiError, tvShows.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry", action: loadNextPage)
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("tvsection.retry")
                }
            }

            if !tvShows.isEmpty {
                LazyVGrid(columns: columns) {
                    ForEach(tvShows, id: \.id) { tvShow in
                        TvShowItemCard(
                            name: tvShow.name,
                            posterPath: tvShow.posterPath,
                            voteAverage: tvShow.voteAverage,
                            onItem: {
                                model.viewModel.onAction(action: TvShowSectionActionOnTvShowClick(tvShowId: tvShow.id))
                            }
                        )
                        .onAppear {
                            if tvShow.id == tvShows.last?.id { loadNextPage() }
                        }
                    }
                }
                .accessibilityIdentifier("tvsection.grid")

                if paging.isLoadingMore {
                    ProgressView().padding()
                }
                if paging.uiError != nil {
                    Button("Retry", action: loadNextPage)
                        .buttonStyle(.bordered)
                        .padding()
                        .accessibilityIdentifier("tvsection.retry")
                }
            }
        }
        .contentMargins(.horizontal, AppDimensions.medium)
        .scrollBounceBehavior(.basedOnSize)
        .navigationTitle(section.title)
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToTvShowDetails(details.tvShowId)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }
}
