import SwiftUI

/// Wide backdrop card, the TV counterpart of `MovieMainCard`.
struct TvShowMainCard: View {
    let name: String
    let backdropPath: String?
    let voteAverage: Double
    let onItem: () -> Void

    var body: some View {
        Button {
            onItem()
        } label: {
            VStack(spacing: AppDimensions.medium) {
                DetailsImage(url: backdropPath ?? "")
                    .modifier(PosterImageModifier(aspectRatio: 1.7))

                HStack {
                    Text(name)
                        .font(.title2)
                        .lineLimit(1)

                    RateView(
                        stars: voteAverage,
                        spacing: 1
                    )
                    .frame(width: 80)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("tv.main.\(name)")
    }
}
