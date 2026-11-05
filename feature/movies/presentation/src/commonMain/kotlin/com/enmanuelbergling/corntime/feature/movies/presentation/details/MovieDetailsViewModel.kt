package com.enmanuelbergling.corntime.feature.movies.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieAccountStates
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.domain.util.errorOrNull
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.movies.domain.details.MovieDetailsChain
import com.enmanuelbergling.corntime.feature.movies.domain.details.MovieDetailsChainRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MovieDetailsViewModel(
    @InjectedParam private val movieId: Int,
    private val detailsChain: MovieDetailsChain,
    private val userRemote: UserRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(MovieDetailsState(movieId = movieId))
    val state: StateFlow<MovieDetailsState> = _state.asStateFlow()

    private val _events = Channel<MovieDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: MovieDetailsAction) {
        when (action) {
            MovieDetailsAction.OnBack -> send(MovieDetailsEvent.NavigateBack)
            MovieDetailsAction.OnRetry -> load()
            MovieDetailsAction.OnDismissError -> _state.update { it.copy(error = null) }
            MovieDetailsAction.OnWatchlistClick -> toggleWatchlist()
            MovieDetailsAction.OnFavoriteClick -> toggleFavorite()
            is MovieDetailsAction.OnPersonClick -> openPerson(action.personId)
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val current = _state.value
            val request = MovieDetailsChainRequest(
                movieId = movieId,
                details = current.details,
                credits = current.credits,
                accountStates = current.accountStates,
            )
            val failure = detailsChain.create().invokeForResult(request).errorOrNull()

            // Logged out: only the last step (account states) fails with Unauthorized. Details and
            // credits are loaded, so this is expected and not an error. A 401 earlier still is.
            val loggedOut = failure == DataError.Network.Unauthorized &&
                request.details != null && request.credits != null

            _state.update {
                it.copy(
                    details = request.details,
                    credits = request.credits,
                    accountStates = request.accountStates,
                    isLoading = false,
                    error = if (loggedOut) null else failure?.toUiError(),
                )
            }
        }
    }

    private fun toggleWatchlist() {
        val states = _state.value.accountStates ?: return
        if (_state.value.isWatchlistLoading) return
        _state.update { it.copy(isWatchlistLoading = true) }
        viewModelScope.launch {
            val result = if (states.watchlist) userRemote.removeMovieFromAccountWatchlist(movieId)
            else userRemote.addMovieToAccountWatchlist(movieId)
            applyToggle(
                result = result,
                update = { it.copy(watchlist = !states.watchlist) },
                finish = { copy(isWatchlistLoading = false) },
            )
        }
    }

    private fun toggleFavorite() {
        val states = _state.value.accountStates ?: return
        if (_state.value.isFavoriteLoading) return
        _state.update { it.copy(isFavoriteLoading = true) }
        viewModelScope.launch {
            val result = if (states.favorite) userRemote.removeMovieFromFavorites(movieId)
            else userRemote.addMovieToFavorites(movieId)
            applyToggle(
                result = result,
                update = { it.copy(favorite = !states.favorite) },
                finish = { copy(isFavoriteLoading = false) },
            )
        }
    }

    /** Applies a successful toggle to the account states, surfaces a failure, and ends only that toggle's loading. */
    private fun applyToggle(
        result: NetworkResult<WatchResponse>,
        update: (MovieAccountStates) -> MovieAccountStates,
        finish: MovieDetailsState.() -> MovieDetailsState,
    ) {
        _state.update { state ->
            val next = when (result) {
                is Result.Success -> state.copy(accountStates = state.accountStates?.let(update))
                is Result.Failure -> state.copy(error = result.error.toUiError())
            }
            next.finish()
        }
    }

    private fun openPerson(personId: Int) {
        val state = _state.value
        val person = (state.cast + state.crew).firstOrNull { it.id == personId }
        send(MovieDetailsEvent.NavigateToActor(personId, person?.name.orEmpty(), person?.profilePath))
    }

    private fun send(event: MovieDetailsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
