import Shared
import SwiftUI

struct EpisodesScreen: View {
    @State private var model: ScreenModel<EpisodesViewModel, EpisodesState>
    let navToEpisodeDetails: (Int32) -> Void

    init(tvShowId: Int32, seasonNumber: Int32, navToEpisodeDetails: @escaping (Int32) -> Void) {
        self.navToEpisodeDetails = navToEpisodeDetails
        _model = State(initialValue: ScreenModels.episodes(tvShowId: tvShowId, seasonNumber: seasonNumber))
    }

    private func send(_ action: EpisodesAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let episodes = state.episodes

        VStack {
            if state.isLoading && episodes.isEmpty {
                ProgressView().accessibilityIdentifier("episodes.loading")
            }

            if state.error != nil && episodes.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    if let error = state.error { Text(error.message) }
                    Button("Retry") { send(EpisodesActionOnRetry.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("episodes.retry")
                }
            }

            if !episodes.isEmpty {
                List {
                    ForEach(episodes, id: \.id) { episode in
                        EpisodeRow(
                            episode: episode,
                            isExpanded: state.expandedEpisodeId?.int32Value == episode.id,
                            onOpen: { send(EpisodesActionOnEpisodeClick(episodeNumber: episode.episodeNumber)) },
                            onToggle: { send(EpisodesActionOnEpisodeToggle(episodeId: episode.id)) }
                        )
                    }
                }
                .listStyle(.plain)
                .accessibilityIdentifier("episodes.list")
            }
        }
        .navigationTitle(state.seasonDetails?.name ?? String(localized: "Episodes"))
        .navigationBarTitleDisplayMode(.inline)
        .errorAlert(
            episodes.isEmpty ? nil : state.error,
            onDismiss: { send(EpisodesActionOnDismissError.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToEpisodeDetails(let details): navToEpisodeDetails(details.episodeNumber)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }
}

private struct EpisodeRow: View {
    let episode: Episode
    let isExpanded: Bool
    let onOpen: () -> Void
    let onToggle: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: AppDimensions.medium) {
            Button(action: onOpen) {
                HStack(alignment: .top, spacing: AppDimensions.medium) {
                    AppImage(url: BASE_BACKDROP_IMAGE_URL + (episode.stillPath ?? ""), placeholder: "moviePoster")
                        .modifier(PlainPosterImageModifier(aspectRatio: 1.7))
                        .frame(width: 130)

                    VStack(alignment: .leading, spacing: AppDimensions.small) {
                        Text("\(episode.episodeNumber). \(episode.name)")
                            .font(.headline)
                            .lineLimit(2)
                        Text(episode.duration)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        RateView(stars: episode.voteAverage, spacing: 2).frame(width: 90)
                        if !episode.overview.isEmpty {
                            Text(episode.overview)
                                .font(.caption)
                                .lineLimit(isExpanded ? Int.max : 2)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .simultaneousGesture(LongPressGesture().onEnded { _ in onToggle() })
            .accessibilityIdentifier("episode.row.\(episode.episodeNumber)")

            Button(action: onToggle) {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Overview")
            .accessibilityIdentifier("episode.toggle.\(episode.episodeNumber)")
        }
        .animation(.default, value: isExpanded)
    }
}
