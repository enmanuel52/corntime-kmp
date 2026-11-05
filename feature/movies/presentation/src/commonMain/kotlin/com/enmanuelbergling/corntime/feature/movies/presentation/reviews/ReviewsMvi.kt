package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBucket

data class ReviewsState(
    val movieId: Int,
    /** On the 0..5 star scale. */
    val averageRating: Float,
    val totalReviews: Int,
    val paging: PagingState<Review> = PagingState(),
    /** Null while logged out, or when the request failed: rating is unavailable. */
    val accountStates: MovieAccountStates? = null,
    val user: UserDetails? = null,
    val isRateSheetOpen: Boolean = false,
    /** A rating is being sent or removed. */
    val isRating: Boolean = false,
    /** The review shown in full; the others are collapsed. */
    val expandedReviewId: String? = null,
    val error: UiError? = null,
    /** Share of voters per star, 5 down to 1; empty without votes. Simulated once, as it is costly. */
    val breakdown: List<RatingBucket> = emptyList(),
) {
    /** The loaded reviews with a concrete element type (generic `items` is untyped in Swift). */
    val reviews: List<Review> get() = paging.items

    /** The user's own rating on the 0..5 scale; null if they have not rated this movie. */
    val rated: Float? get() = accountStates?.rated

    /** Rating needs a logged-in user whose account states and profile are known. */
    val canRate: Boolean get() = accountStates != null && user != null
}

sealed interface ReviewsAction {
    data object OnBack : ReviewsAction
    data object OnLoadNextPage : ReviewsAction
    data object OnToggleRateSheet : ReviewsAction
    data class OnSubmitRating(val stars: Float) : ReviewsAction
    data object OnDeleteRating : ReviewsAction
    data class OnToggleReview(val reviewId: String) : ReviewsAction
    data object OnDismissError : ReviewsAction
}

sealed interface ReviewsEvent {
    data object NavigateBack : ReviewsEvent
}
