import SwiftUI

internal struct PlainPosterImageModifier: ViewModifier {
    var aspectRatio: CGFloat = 0.65

    func body(content: Content) -> some View {
        content
            .aspectRatio(self.aspectRatio, contentMode: .fit)
            .cornerRadius(12)
    }
}

/// A fixed height should be given.
struct MovieItemRow: View {
    let title: String
    let backdropPath: String?
    let voteAverage: Double

    var body: some View {
        HStack(spacing: AppDimensions.medium) {
            AppImage(
                url: BASE_BACKDROP_IMAGE_URL + (backdropPath ?? ""),
                placeholder: "moviePoster"
            )
            .frame(maxHeight: .infinity)
            .modifier(PlainPosterImageModifier(aspectRatio: 1.4))

            VStack(spacing: AppDimensions.medium) {
                Text(title).font(.title2).lineLimit(2)

                RateView(stars: voteAverage).frame(width: 120)
            }.frame(maxWidth: .infinity)
        }
    }
}

#Preview {
    MovieItemRow(
        title: "hello this is a long long text",
        backdropPath: "/rvRGFevvZK48onX0PYI1eQLbuJd.jpg",
        voteAverage: 4.3
    ).frame(height: 100)
}
