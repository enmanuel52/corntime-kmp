import Shared
import SwiftUI

/// Average, vote count and the per-star breakdown. [Rated] / [Rate] chips are added by the reviews screen.
struct RatingSummary: View {
    let averageRating: Float
    let totalReviews: Int32
    let breakdown: [RatingBucket]

    var body: some View {
        HStack(spacing: AppDimensions.xLarge) {
            VStack(spacing: AppDimensions.medium) {
                Text(String(format: "%.1f", averageRating))
                    .font(.system(size: 56, weight: .light))
                    .accessibilityIdentifier("rating.average")
                RateView(stars: CGFloat(averageRating), spacing: 2)
                    .frame(width: 100)
                Text("(\(Int(totalReviews)) reviews)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)

            VStack(spacing: AppDimensions.small) {
                ForEach(breakdown, id: \.stars) { bucket in
                    HStack(spacing: AppDimensions.medium) {
                        Text("\(bucket.stars)").font(.caption)
                        ProgressView(value: Double(bucket.fraction))
                            .tint(.yellow)
                    }
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.vertical, AppDimensions.medium)
    }
}

#Preview {
    RatingSummary(
        averageRating: 4.2,
        totalReviews: 120,
        breakdown: [
            RatingBucket(stars: 5, count: 60, fraction: 0.5),
            RatingBucket(stars: 4, count: 30, fraction: 0.25),
            RatingBucket(stars: 3, count: 18, fraction: 0.15),
            RatingBucket(stars: 2, count: 6, fraction: 0.05),
            RatingBucket(stars: 1, count: 6, fraction: 0.05),
        ]
    )
    .padding()
}
