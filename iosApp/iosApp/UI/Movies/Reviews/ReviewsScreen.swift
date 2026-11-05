import Shared
import SwiftUI

struct ReviewsScreen: View {
    @State private var model: ScreenModel<ReviewsViewModel, ReviewsState>

    init(movieId: Int32, averageRating: Double, totalReviews: Int32) {
        _model = State(
            initialValue: ScreenModels.reviews(
                movieId: movieId,
                averageRating: averageRating,
                totalReviews: totalReviews
            )
        )
    }

    private func send(_ action: ReviewsAction) {
        model.viewModel.onAction(action: action)
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        List {
            Section {
                VStack(spacing: AppDimensions.medium) {
                    RatingSummary(
                        averageRating: state.averageRating,
                        totalReviews: state.totalReviews,
                        breakdown: state.breakdown
                    )
                    ratingChip(state)
                }
                .listRowSeparator(.hidden)
            }

            ForEach(state.reviews, id: \.id) { review in
                ReviewRow(review: review, isExpanded: state.expandedReviewId == review.id)
                    .onTapGesture { send(ReviewsActionOnToggleReview(reviewId: review.id)) }
                    .accessibilityIdentifier("review.\(review.id)")
                    .onAppear {
                        if review.id == state.reviews.last?.id {
                            send(ReviewsActionOnLoadNextPage.shared)
                        }
                    }
            }

            if paging.isLoading {
                ProgressView().frame(maxWidth: .infinity).listRowSeparator(.hidden)
            }

            if paging.isEmpty {
                Text("No reviews yet")
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier("reviews.empty")
            }

            if let error = paging.uiError {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(ReviewsActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("reviews.retry")
                }
                .frame(maxWidth: .infinity)
            }
        }
        .listStyle(.plain)
        .accessibilityIdentifier("reviews.list")
        .navigationTitle("Ratings & Reviews")
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if state.isRating { ProgressView().controlSize(.large) }
        }
        .sheet(
            isPresented: Binding(
                get: { state.isRateSheetOpen && state.user != nil },
                set: { if !$0 && state.isRateSheetOpen { send(ReviewsActionOnToggleRateSheet.shared) } }
            )
        ) {
            if let user = state.user {
                RateSheet(
                    user: user,
                    initialRating: state.rated?.floatValue,
                    onSubmit: { send(ReviewsActionOnSubmitRating(stars: $0)) },
                    onDelete: state.rated == nil ? nil : { send(ReviewsActionOnDeleteRating.shared) }
                )
                .presentationDetents([.medium])
            }
        }
        .errorAlert(state.error, onDismiss: { send(ReviewsActionOnDismissError.shared) })
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateBack: break  // the system back button pops the stack
                }
            }
        }
    }

    @ViewBuilder
    private func ratingChip(_ state: ReviewsState) -> some View {
        if let rated = state.rated {
            Button {
                send(ReviewsActionOnToggleRateSheet.shared)
            } label: {
                HStack {
                    Image(systemName: "checkmark")
                    Text("You have rated this movie")
                    RateView(stars: CGFloat(rated.floatValue), spacing: 1).frame(width: 80)
                }
            }
            .buttonStyle(.bordered)
            .tint(.yellow)
            .accessibilityIdentifier("reviews.rated")
        } else if state.canRate {
            Button {
                send(ReviewsActionOnToggleRateSheet.shared)
            } label: {
                Label("Rate this movie", systemImage: "star")
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier("reviews.rate")
        }
    }
}

private struct ReviewRow: View {
    let review: Review
    let isExpanded: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: AppDimensions.medium) {
            HStack(spacing: AppDimensions.medium) {
                AppImage(url: reviewAvatarUrl(review.authorDetails.avatarPath), placeholder: "moviePoster")
                    .scaledToFill()
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())

                Text(review.author).font(.headline)

                Spacer()

                if let rating = review.rating {
                    RateView(stars: CGFloat(rating.floatValue), spacing: 1).frame(width: 90)
                }
            }

            Text(review.content)
                .font(.subheadline)
                .lineLimit(isExpanded ? nil : 4)

            Text(review.elapsedSincePosted().text)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, AppDimensions.medium)
        .contentShape(Rectangle())
    }
}
