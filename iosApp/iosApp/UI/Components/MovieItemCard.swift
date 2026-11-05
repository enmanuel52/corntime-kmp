import SwiftUI

/// Poster card. `voteAverage` is already on a 0...5 star scale (the shared mapper halves TMDB's 0...10).
struct MovieItemCard: View {
    let title: String
    let posterUrl: String
    let voteAverage: Double
    let onItem: () -> Void

    var body: some View {
        VStack(alignment: .center) {
            Button {
                onItem()
            } label: {
                AppImage(url: BASE_POSTER_IMAGE_URL + posterUrl, placeholder: "moviePoster")
            }
            .buttonStyle(.plain)
            .modifier(PosterImageModifier())
            .accessibilityIdentifier("movie.poster.\(title)")

            Text(title).font(.title3).lineLimit(1)

            RateView(stars: voteAverage).frame(width: 120)
        }
    }
}

internal struct PosterImageModifier: ViewModifier {
    var aspectRatio: CGFloat = 0.65

    func body(content: Content) -> some View {
        content
            .aspectRatio(self.aspectRatio, contentMode: .fit)
            .cornerRadius(28)
            .shadow(radius: 8, x: 0, y: 8)
    }
}

#Preview {
    MovieItemCard(
        title: "hello",
        posterUrl: "/54roTwbX9fltg85zjsmrooXAs12.jpg",
        voteAverage: 4.0,
        onItem: {}
    ).frame(width: 200)
}
