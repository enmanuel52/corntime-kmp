import SwiftUI

/// Poster card of a TV show. `voteAverage` is already on a 0...5 star scale (the shared mapper halves TMDB's 0...10).
struct TvShowItemCard: View {
    let name: String
    let posterPath: String?
    let voteAverage: Double
    let onItem: () -> Void

    var body: some View {
        VStack(alignment: .center) {
            Button {
                onItem()
            } label: {
                AppImage(url: BASE_POSTER_IMAGE_URL + (posterPath ?? ""), placeholder: "moviePoster")
            }
            .buttonStyle(.plain)
            .modifier(PosterImageModifier())
            .accessibilityIdentifier("tv.poster.\(name)")

            Text(name).font(.title3).lineLimit(1)

            RateView(stars: voteAverage).frame(width: 120)
        }
    }
}
