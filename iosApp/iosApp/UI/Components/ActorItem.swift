import Shared
import SwiftUI

extension PersonUi: @retroactive Identifiable {}

struct ActorItem: View {
    let person: PersonUi
    let onClick: () -> Void

    var body: some View {
        VStack {
            Button {
                onClick()
            } label: {
                AppImage(
                    url: BASE_POSTER_IMAGE_URL + (person.profilePath ?? ""),
                    placeholder: "personPoster"
                )
            }
            .buttonStyle(.plain)
            .modifier(PosterImageModifier())
            .accessibilityIdentifier("person.\(person.id)")

            Text(person.name).lineLimit(1)
        }
    }
}
