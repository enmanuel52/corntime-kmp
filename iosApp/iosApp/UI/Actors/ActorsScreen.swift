import Shared
import SwiftUI

private let columns = [
    GridItem(.adaptive(minimum: 110), spacing: AppDimensions.medium),
]

struct ActorsScreen: View {
    @State private var model = ScreenModels.actors()
    let navToActorDetails: (Int32, String) -> Void

    private func send(_ action: ActorsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging
        let actors = state.actors

        ScrollView(showsIndicators: false) {
            if paging.isLoadingFirstPage {
                ProgressView().accessibilityIdentifier("actors.loading")
            } else if paging.isEmpty {
                Text("No actors to show").accessibilityIdentifier("actors.empty")
            }

            if let error = paging.uiError, actors.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(ActorsActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("actors.retry")
                }
            }

            if !actors.isEmpty {
                LazyVGrid(columns: columns, spacing: AppDimensions.medium) {
                    ForEach(actors, id: \.id) { actor in
                        ActorGridItem(actor: actor) { send(ActorsActionOnActorClick(actor: actor)) }
                            .onAppear {
                                if actor.id == actors.last?.id { send(ActorsActionOnLoadNextPage.shared) }
                            }
                    }
                }
                .accessibilityIdentifier("actors.grid")

                if paging.isLoadingMore {
                    ProgressView().padding()
                }
                if paging.uiError != nil {
                    Button("Retry") { send(ActorsActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .padding()
                        .accessibilityIdentifier("actors.retry")
                }
            }
        }
        .contentMargins(.horizontal, AppDimensions.medium)
        .scrollBounceBehavior(.basedOnSize)
        .navigationTitle("Actors")
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToActorDetails(details.actorId, details.name)
                }
            }
        }
    }
}

private struct ActorGridItem: View {
    let actor: Actor
    let onClick: () -> Void

    var body: some View {
        VStack {
            Button(action: onClick) {
                AppImage(
                    url: BASE_POSTER_IMAGE_URL + (actor.profilePath ?? ""),
                    placeholder: "personPoster"
                )
            }
            .buttonStyle(.plain)
            .modifier(PosterImageModifier())
            .accessibilityIdentifier("actor.poster.\(actor.originalName)")

            Text(actor.originalName).lineLimit(2).font(.subheadline)
        }
    }
}
