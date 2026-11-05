import SwiftUI

/// A cast, crew or guest star member. It takes plain values so every TV screen can use it, whatever
/// shared type (`PersonUi`, `EpisodePersonUi`) describes the person there.
struct PersonCard: View {
    let id: Int32
    let name: String
    let profilePath: String?
    let onClick: () -> Void

    var body: some View {
        VStack {
            Button {
                onClick()
            } label: {
                AppImage(
                    url: BASE_POSTER_IMAGE_URL + (profilePath ?? ""),
                    placeholder: "personPoster"
                )
            }
            .buttonStyle(.plain)
            .modifier(PosterImageModifier())
            .accessibilityIdentifier("person.\(id)")

            Text(name).lineLimit(1)
        }
    }
}
