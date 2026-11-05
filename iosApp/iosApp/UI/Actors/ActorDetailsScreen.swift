import Shared
import SwiftUI

struct ActorDetailsScreen: View {
    @State private var model: ScreenModel<ActorDetailsViewModel, ActorDetailsState>
    /// Shown as the title until the details arrive.
    let name: String
    let navToMovieDetails: (Int32) -> Void

    init(actorId: Int32, name: String, navToMovieDetails: @escaping (Int32) -> Void) {
        self.name = name
        self.navToMovieDetails = navToMovieDetails
        _model = State(initialValue: ScreenModels.actorDetails(actorId: actorId))
    }

    private func send(_ action: ActorDetailsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state

        VStack {
            if state.isLoading && state.details == nil {
                ProgressView("Loading").accessibilityIdentifier("actor.loading")
            }

            List {
                if let details = state.details {
                    ActorBio(profilePath: details.profilePath, biography: details.biography)
                        .frame(maxHeight: 250)
                        .listRowSeparator(.hidden)
                        .accessibilityIdentifier("actor.bio")
                }

                if state.loadedMovies != nil {
                    Section("Known movies") {
                        ForEach(state.knownMovies, id: \.id) { movie in
                            Button {
                                send(ActorDetailsActionOnMovieClick(movieId: movie.id))
                            } label: {
                                MovieItemRow(
                                    title: movie.title,
                                    backdropPath: movie.posterPath,
                                    voteAverage: movie.voteAverage
                                )
                                .frame(height: 90)
                            }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("actor.movie.\(movie.title)")
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
        .navigationBarTitle(state.details?.name ?? name)
        .errorAlert(
            state.error,
            onDismiss: { send(ActorDetailsActionOnDismissError.shared) },
            onRetry: { send(ActorDetailsActionOnRetry.shared) }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateBack: break
                case .navigateToMovie(let movie): navToMovieDetails(movie.movieId)
                }
            }
        }
    }
}

private struct ActorBio: View {
    let profilePath: String
    let biography: String

    var body: some View {
        HStack(alignment: .center, spacing: AppDimensions.large) {
            if !profilePath.isEmpty {
                AppImage(
                    url: BASE_POSTER_IMAGE_URL + profilePath,
                    placeholder: "personPoster"
                )
                .modifier(PosterImageModifier())
            }

            Text(biography.isEmpty ? String(localized: "No info") : biography)
                .font(.caption)
                .frame(maxWidth: .infinity)
        }
    }
}
