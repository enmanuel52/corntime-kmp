import Shared
import SwiftUI

/// Bottom sheet to rate a movie: five tappable stars (tap the selected star again to take half a star off).
struct RateSheet: View {
    let user: UserDetails
    let initialRating: Float?
    let onSubmit: (Float) -> Void
    let onDelete: (() -> Void)?

    @State private var rating: Float

    init(
        user: UserDetails,
        initialRating: Float?,
        onSubmit: @escaping (Float) -> Void,
        onDelete: (() -> Void)?
    ) {
        self.user = user
        self.initialRating = initialRating
        self.onSubmit = onSubmit
        self.onDelete = onDelete
        _rating = State(initialValue: initialRating ?? 5)
    }

    var body: some View {
        VStack(spacing: AppDimensions.large) {
            VStack(spacing: AppDimensions.medium) {
                Text("Leave a review").font(.title2).fontWeight(.bold)
                Text("What are your thoughts in this movie?")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            HStack(spacing: AppDimensions.medium) {
                ForEach(1...5, id: \.self) { star in
                    Button {
                        rating = Float(star) == rating ? rating - 0.5 : Float(star)
                    } label: {
                        Image(systemName: symbol(for: star))
                            .resizable()
                            .scaledToFit()
                            .foregroundStyle(.yellow)
                            .frame(maxWidth: .infinity, minHeight: 32)
                    }
                    .buttonStyle(.bordered)
                    .accessibilityIdentifier("rate.star.\(star)")
                }
            }

            HStack {
                AppImage(url: avatarUrl, placeholder: "moviePoster")
                    .scaledToFill()
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())
                Text(user.name).font(.headline)
                Spacer()
                Button("Accept") { onSubmit(rating) }
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier("rate.submit")
            }

            if let onDelete {
                Button("Remove my rating", role: .destructive, action: onDelete)
                    .accessibilityIdentifier("rate.delete")
            }
        }
        .padding(AppDimensions.large)
        .accessibilityIdentifier("rate.sheet")
    }

    private func symbol(for star: Int) -> String {
        let value = Float(star)
        if value <= rating { return "star.fill" }
        if value - 0.5 <= rating { return "star.leadinghalf.filled" }
        return "star"
    }

    private var avatarUrl: String {
        reviewAvatarUrl(user.avatarPath)
    }
}

/// TMDB gives either a path on its image host, or a full address behind a leading slash.
func reviewAvatarUrl(_ path: String?) -> String {
    guard let path, !path.isEmpty else { return "" }
    if path.hasPrefix("/http") { return String(path.dropFirst()) }
    return BASE_IMAGE_URL + path
}
