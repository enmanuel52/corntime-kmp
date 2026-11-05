import Shared
import SwiftUI

struct TvShowDetailsScreen: View {
    @State private var model: ScreenModel<TvShowDetailsViewModel, TvShowDetailsState>
    let navToSeasons: () -> Void
    let navToEpisodes: (Int32) -> Void
    let navToActorDetails: (Int32, String) -> Void

    init(
        tvShowId: Int32,
        navToSeasons: @escaping () -> Void,
        navToEpisodes: @escaping (Int32) -> Void,
        navToActorDetails: @escaping (Int32, String) -> Void
    ) {
        self.navToSeasons = navToSeasons
        self.navToEpisodes = navToEpisodes
        self.navToActorDetails = navToActorDetails
        _model = State(initialValue: ScreenModels.tvShowDetails(tvShowId: tvShowId))
    }

    private func send(_ action: TvShowDetailsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        VStack {
            if state.isLoading && state.details == nil {
                ProgressView("Loading TV show").accessibilityIdentifier("tvdetails.loading")
            }

            ScrollView {
                if let details = state.details {
                    DetailsImage(url: details.backdropPath ?? "")

                    TvShowInformation(
                        name: details.name,
                        year: details.firstAirYear,
                        rating: details.voteAverage,
                        genres: details.formattedGenres,
                        status: details.status
                    ).padding(.horizontal, AppDimensions.medium)

                    if !details.tagline.isEmpty {
                        Text(details.tagline)
                            .font(.callout)
                            .italic()
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, AppDimensions.medium)
                    }

                    TvShowOverview(text: details.overview)
                        .padding(.horizontal, AppDimensions.medium)

                    if !details.seasons.isEmpty {
                        SeasonsStrip(seasons: details.seasons, onSeeAll: { send(TvShowDetailsActionOnSeeAllSeasons.shared) }) {
                            send(TvShowDetailsActionOnSeasonClick(seasonNumber: $0))
                        }
                    }
                }

                if state.details != nil {
                    TvShowPersonRow(
                        label: String(localized: "Cast"),
                        persons: state.cast.map { PersonEntry(id: $0.id, name: $0.name, profilePath: $0.profilePath) }
                    ) { send(TvShowDetailsActionOnPersonClick(personId: $0)) }

                    TvShowPersonRow(
                        label: String(localized: "Crew"),
                        persons: state.crew.map { PersonEntry(id: $0.id, name: $0.name, profilePath: $0.profilePath) }
                    ) { send(TvShowDetailsActionOnPersonClick(personId: $0)) }
                }
            }
            .accessibilityIdentifier("tvdetails.content")
        }
        .navigationBarTitle("Details", displayMode: .inline)
        .toolbar {
            if state.canManageAccount {
                if state.isFavoriteLoading {
                    ProgressView()
                } else {
                    Button {
                        send(TvShowDetailsActionOnFavoriteClick.shared)
                    } label: {
                        Image(systemName: state.isFavorite ? "heart.fill" : "heart")
                    }
                    .accessibilityIdentifier("tvdetails.favorite")
                }

                if state.isWatchlistLoading {
                    ProgressView()
                } else {
                    Button {
                        send(TvShowDetailsActionOnWatchlistClick.shared)
                    } label: {
                        Image(systemName: state.isInWatchlist ? "bookmark.fill" : "bookmark")
                    }
                    .accessibilityIdentifier("tvdetails.watchlist")
                }
            }
        }
        .errorAlert(
            state.details == nil ? nil : state.error,
            onDismiss: { send(TvShowDetailsActionOnDismissError.shared) }
        )
        .overlay {
            if state.details == nil, let error = state.error {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(TvShowDetailsActionOnRetry.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("tvdetails.retry")
                }
            }
        }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToSeasons: navToSeasons()
                case .navigateToEpisodes(let episodes): navToEpisodes(episodes.seasonNumber)
                case .navigateToActor(let actor): navToActorDetails(actor.actorId, actor.name)
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }
}

/// What the cast and crew rows need of a person, whichever shared type it came from.
struct PersonEntry: Identifiable {
    let id: Int32
    let name: String
    let profilePath: String?
}

struct TvShowPersonRow: View {
    let label: String
    let persons: [PersonEntry]
    let onClick: (Int32) -> Void

    var body: some View {
        if !persons.isEmpty {
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: AppDimensions.medium) {
                        ForEach(persons) { person in
                            PersonCard(id: person.id, name: person.name, profilePath: person.profilePath) {
                                onClick(person.id)
                            }
                            .frame(width: 160)
                        }
                    }
                }
                .safeAreaPadding(AppDimensions.large)
            } header: {
                SectionLabel(label: label)
            }
        }
    }
}

private struct SeasonsStrip: View {
    let seasons: [Season]
    let onSeeAll: () -> Void
    let onSeason: (Int32) -> Void

    var body: some View {
        Section {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: AppDimensions.medium) {
                    ForEach(seasons, id: \.id) { season in
                        Button { onSeason(season.seasonNumber) } label: {
                            VStack {
                                AppImage(url: BASE_POSTER_IMAGE_URL + (season.posterPath ?? ""), placeholder: "moviePoster")
                                    .modifier(PosterImageModifier())

                                Text(season.name).font(.subheadline).lineLimit(1)
                                Text("\(season.episodeCount) episodes")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                        .frame(width: 140)
                        .accessibilityIdentifier("season.card.\(season.seasonNumber)")
                    }
                }
            }
            .safeAreaPadding(.horizontal, AppDimensions.large)
        } header: {
            SectionLabelRow(label: String(localized: "Seasons"), onSeeAll: onSeeAll)
        }
    }
}

private struct TvShowOverview: View {
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

private struct TvShowInformation: View {
    let name: String
    let year: String
    let rating: Double
    let genres: String
    let status: String

    var body: some View {
        VStack(alignment: .leading) {
            Text(name + "(\(year))")
                .font(.title2)
                .fontWeight(.bold)
                .frame(maxWidth: .infinity)
                .lineLimit(1)

            HStack {
                Text(genres + " - " + status)
                    .font(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(1)

                RateView(stars: rating, spacing: 2)
                    .frame(width: 120)
            }
        }
    }
}
