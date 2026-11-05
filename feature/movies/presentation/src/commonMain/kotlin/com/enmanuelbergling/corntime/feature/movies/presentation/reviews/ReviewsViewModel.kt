package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBreakdownSimulator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** Reviews of a movie with its rating breakdown, and the logged-in user's own rating. */
@KoinViewModel
class ReviewsViewModel(
    @InjectedParam private val movieId: Int,
    @InjectedParam averageRating: Double,
    @InjectedParam totalReviews: Int,
    private val movieRemote: MovieRemoteDataSource,
    private val userRemote: UserRemoteDataSource,
    private val userPreferences: UserPreferenceDataSource,
) : ViewModel() {

    private val paginator = Paginator { page -> movieRemote.getMovieReviews(movieId, page) }

    private val _state = MutableStateFlow(
        ReviewsState(
            movieId = movieId,
            averageRating = averageRating.toFloat(),
            totalReviews = totalReviews,
            breakdown = RatingBreakdownSimulator.get(averageRating, totalReviews),
        ),
    )
    val state: StateFlow<ReviewsState> = _state.asStateFlow()

    private val _events = Channel<ReviewsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        loadNextPage()
        loadAccount()
    }

    fun onAction(action: ReviewsAction) {
        when (action) {
            ReviewsAction.OnBack -> send(ReviewsEvent.NavigateBack)
            ReviewsAction.OnLoadNextPage -> loadNextPage()
            ReviewsAction.OnToggleRateSheet -> _state.update {
                // there is nothing to rate with while logged out
                if (it.canRate) it.copy(isRateSheetOpen = !it.isRateSheetOpen) else it
            }
            is ReviewsAction.OnSubmitRating -> rate(action.stars)
            ReviewsAction.OnDeleteRating -> removeRating()
            is ReviewsAction.OnToggleReview -> _state.update { state ->
                state.copy(expandedReviewId = action.reviewId.takeIf { it != state.expandedReviewId })
            }
            ReviewsAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    /** The account states fail with unauthorized while logged out; rating is then simply unavailable. */
    private fun loadAccount() {
        viewModelScope.launch {
            val user = userPreferences.getCurrentUser().firstOrNull()
            _state.update { it.copy(user = user) }
        }
        viewModelScope.launch {
            val result = movieRemote.getMovieAccountStates(movieId)
            if (result is Result.Success) _state.update { it.copy(accountStates = result.data) }
        }
    }

    private fun rate(stars: Float) {
        val states = _state.value.accountStates ?: return
        if (_state.value.isRating) return
        _state.update { it.copy(isRating = true) }
        viewModelScope.launch {
            finishRating(userRemote.addMovieRating(movieId, stars)) { states.copy(rated = stars) }
        }
    }

    private fun removeRating() {
        val states = _state.value.accountStates ?: return
        if (_state.value.isRating) return
        _state.update { it.copy(isRating = true) }
        viewModelScope.launch {
            finishRating(userRemote.deleteMovieRating(movieId)) { states.copy(rated = null) }
        }
    }

    private fun finishRating(
        result: NetworkResult<WatchResponse>,
        success: () -> MovieAccountStates,
    ) {
        _state.update { state ->
            when (result) {
                is Result.Success -> state.copy(
                    isRating = false,
                    isRateSheetOpen = false,
                    accountStates = success(),
                )
                is Result.Failure -> state.copy(isRating = false, error = result.error.toUiError())
            }
        }
    }

    private fun send(event: ReviewsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
