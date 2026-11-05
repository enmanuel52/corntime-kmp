import Shared
import SwiftUI

struct MovieDetailsScreen: View {
    @State private var model: ScreenModel<MovieDetailsViewModel, MovieDetailsState>
    let navToActorDetails: (Int32, String) -> Void

    init(movieId: Int32, navToActorDetails: @escaping (Int32, String) -> Void) {
        self.navToActorDetails = navToActorDetails
        _model = State(initialValue: ScreenModels.movieDetails(movieId: movieId))
    }

    private func send(_ action: MovieDetailsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        VStack {
            if state.isLoading && state.details == nil {
                ProgressView("loading movies").accessibilityIdentifier("details.loading")
            }

            ScrollView {
                if let details = state.details {
                    DetailsImage(url: details.backdropPath)

                    Information(
                        title: details.title,
                        year: details.releaseYear,
                        rating: details.voteAverage,
                        genres: details.formattedGenres,
                        duration: details.duration
                    ).padding(.horizontal, AppDimensions.medium)

                    Overview(text: details.overview)
                        .padding(.horizontal, AppDimensions.medium)
                }

                if state.credits != nil {
                    PersonRow(label: "Cast", persons: state.cast) {
                        send(MovieDetailsActionOnPersonClick(personId: $0.id))
                    }

                    PersonRow(label: "Crew", persons: state.crew) {
                        send(MovieDetailsActionOnPersonClick(personId: $0.id))
                    }
                }
            }
            .accessibilityIdentifier("details.content")
        }
        .navigationBarTitle("Details", displayMode: .inline)
        .toolbar {
            if state.canManageAccount {
                if state.isFavoriteLoading {
                    ProgressView()
                } else {
                    Button {
                        send(MovieDetailsActionOnFavoriteClick.shared)
                    } label: {
                        Image(systemName: state.isFavorite ? "heart.fill" : "heart")
                    }
                    .accessibilityIdentifier("details.favorite")
                }

                if state.isWatchlistLoading {
                    ProgressView()
                } else {
                    Button {
                        send(MovieDetailsActionOnWatchlistClick.shared)
                    } label: {
                        Image(systemName: state.isInWatchlist ? "bookmark.fill" : "bookmark")
                    }
                    .accessibilityIdentifier("details.watchlist")
                }
            }
        }
        .errorAlert(
            state.details == nil ? nil : state.error,
            onDismiss: { send(MovieDetailsActionOnDismissError.shared) }
        )
        .overlay {
            if state.details == nil, let error = state.error {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(MovieDetailsActionOnRetry.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("details.retry")
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
}

private struct PersonRow: View {
    let label: String
    let persons: [PersonUi]
    let onClick: (PersonUi) -> Void

    var body: some View {
        Section {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: AppDimensions.medium) {
                    ForEach(persons) { person in
                        ActorItem(person: person) { onClick(person) }
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

private struct Overview: View {
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
            SectionLabel(label: "Overview")
        }
    }
}

private struct Information: View {
    let title: String
    let year: String
    let rating: Double
    let genres: String
    let duration: String

    var body: some View {
        VStack(alignment: .leading) {
            Text(title + "(\(year))")
                .font(.title2)
                .fontWeight(.bold)
                .frame(maxWidth: .infinity)
                .lineLimit(1)

            HStack {
                Text(genres + " - " + duration)
                    .font(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(1)

                RateView(stars: rating, spacing: 2)
                    .frame(width: 120)
            }
        }
    }
}

#Preview {
    Information(
        title: "This is some title, let's see how it goes",
        year: "2023",
        rating: 4.5,
        genres: "Action/ Comedy",
        duration: "1h 30m"
    )
}
