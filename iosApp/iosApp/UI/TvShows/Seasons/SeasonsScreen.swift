import Shared
import SwiftUI

struct SeasonsScreen: View {
    @State private var model: ScreenModel<SeasonsViewModel, SeasonsState>
    let navToEpisodes: (Int32) -> Void

    init(tvShowId: Int32, navToEpisodes: @escaping (Int32) -> Void) {
        self.navToEpisodes = navToEpisodes
        _model = State(initialValue: ScreenModels.seasons(tvShowId: tvShowId))
    }

    private func send(_ action: SeasonsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let seasons = state.seasons

        VStack {
            if state.isLoading && seasons.isEmpty {
                ProgressView().accessibilityIdentifier("seasons.loading")
            }

            if state.error != nil && seasons.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    if let error = state.error { Text(error.message) }
                    Button("Retry") { send(SeasonsActionOnRetry.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("seasons.retry")
                }
            }

            if !seasons.isEmpty {
                List {
                    ForEach(seasons, id: \.id) { season in
                        SeasonRow(
                            season: season,
                            isExpanded: state.expandedSeasonId?.int32Value == season.id,
                            onOpen: { send(SeasonsActionOnSeasonClick(seasonNumber: season.seasonNumber)) },
                            onToggle: { send(SeasonsActionOnSeasonToggle(seasonId: season.id)) }
                        )
                    }
                }
                .listStyle(.plain)
                .accessibilityIdentifier("seasons.list")
            }
        }
        .navigationTitle("Seasons")
        .errorAlert(
            seasons.isEmpty ? nil : state.error,
            onDismiss: { send(SeasonsActionOnDismissError.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToEpisodes(let episodes): navToEpisodes(episodes.seasonNumber)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }
}

/// Tapping a season (or long-pressing it) behaves differently: tap opens its episodes, a long press or the
/// chevron opens the overview in place.
private struct SeasonRow: View {
    let season: Season
    let isExpanded: Bool
    let onOpen: () -> Void
    let onToggle: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: AppDimensions.medium) {
            Button(action: onOpen) {
                HStack(alignment: .top, spacing: AppDimensions.medium) {
                    AppImage(url: BASE_POSTER_IMAGE_URL + (season.posterPath ?? ""), placeholder: "moviePoster")
                        .modifier(PlainPosterImageModifier())
                        .frame(width: 80)

                    VStack(alignment: .leading, spacing: AppDimensions.small) {
                        Text(season.name).font(.headline)
                        Text("\(season.episodeCount) episodes")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        if let airDate = season.airDate, !airDate.isEmpty {
                            Text(airDate).font(.caption).foregroundStyle(.secondary)
                        }
                        RateView(stars: season.voteAverage, spacing: 2).frame(width: 90)
                        if !season.overview.isEmpty {
                            Text(season.overview)
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
            .accessibilityIdentifier("season.row.\(season.seasonNumber)")

            Button(action: onToggle) {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Overview")
            .accessibilityIdentifier("season.toggle.\(season.seasonNumber)")
        }
        .animation(.default, value: isExpanded)
    }
}
