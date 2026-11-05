import Shared
import SwiftUI

struct EpisodeDetailsScreen: View {
    @State private var model: ScreenModel<EpisodeDetailsViewModel, EpisodeDetailsState>
    let navToActorDetails: (Int32, String) -> Void

    init(
        tvShowId: Int32,
        seasonNumber: Int32,
        episodeNumber: Int32,
        navToActorDetails: @escaping (Int32, String) -> Void
    ) {
        self.navToActorDetails = navToActorDetails
        _model = State(
            initialValue: ScreenModels.episodeDetails(
                tvShowId: tvShowId,
                seasonNumber: seasonNumber,
                episodeNumber: episodeNumber
            )
        )
    }

    private func send(_ action: EpisodeDetailsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        VStack {
            if state.isLoading && state.details == nil {
                ProgressView().accessibilityIdentifier("episodedetails.loading")
            }

            ScrollView {
                if let details = state.details {
                    DetailsImage(url: details.stillPath ?? "")

                    VStack(alignment: .leading, spacing: AppDimensions.small) {
                        Text(details.name)
                            .font(.title2)
                            .fontWeight(.bold)
                            .accessibilityIdentifier("episodedetails.name")

                        HStack {
                            Text(episodeSubtitle(details))
                                .font(.body)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .lineLimit(1)

                            RateView(stars: details.voteAverage, spacing: 2)
                                .frame(width: 120)
                        }
                    }
                    .padding(.horizontal, AppDimensions.medium)

                    if !details.overview.isEmpty {
                        EpisodeOverview(text: details.overview)
                            .padding(.horizontal, AppDimensions.medium)
                    }

                    TvShowPersonRow(
                        label: String(localized: "Guest stars"),
                        persons: state.guestStars.map { PersonEntry(id: $0.id, name: $0.name, profilePath: $0.profilePath) }
                    ) { send(EpisodeDetailsActionOnPersonClick(personId: $0)) }

                    TvShowPersonRow(
                        label: String(localized: "Crew"),
                        persons: state.crew.map { PersonEntry(id: $0.id, name: $0.name, profilePath: $0.profilePath) }
                    ) { send(EpisodeDetailsActionOnPersonClick(personId: $0)) }
                }
            }
            .accessibilityIdentifier("episodedetails.content")
        }
        .navigationBarTitle("Episode details", displayMode: .inline)
        .errorAlert(
            state.details == nil ? nil : state.error,
            onDismiss: { send(EpisodeDetailsActionOnDismissError.shared) }
        )
        .overlay {
            if state.details == nil, let error = state.error {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(EpisodeDetailsActionOnRetry.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("episodedetails.retry")
                }
            }
        }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToActor(let actor): navToActorDetails(actor.actorId, actor.name)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }

    private func episodeSubtitle(_ details: EpisodeDetails) -> String {
        [
            "S\(details.seasonNumber) E\(details.episodeNumber)",
            details.airYear.isEmpty ? nil : details.airYear,
            details.duration,
        ]
        .compactMap { $0 }
        .joined(separator: " - ")
    }
}

private struct EpisodeOverview: View {
    let text: String
    @State var isOverviewExpanded = false

    var body: some View {
        Section {
            Button {
                withAnimation { isOverviewExpanded.toggle() }
            } label: {
                Text(text)
                    .font(.caption)
                    .lineLimit(isOverviewExpanded ? Int.max : 3)
            }
            .buttonStyle(.plain)
        } header: {
            SectionLabel(label: String(localized: "Overview"))
        }
    }
}
