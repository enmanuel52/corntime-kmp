package com.enmanuelbergling.corntime.feature.actor.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.util.errorOrNull
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.actor.domain.details.ActorDetailsChain
import com.enmanuelbergling.corntime.feature.actor.domain.details.ActorDetailsChainRequest
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
class ActorDetailsViewModel(
    @InjectedParam private val actorId: Int,
    private val detailsChain: ActorDetailsChain,
) : ViewModel() {

    private val _state = MutableStateFlow(ActorDetailsState(actorId = actorId))
    val state: StateFlow<ActorDetailsState> = _state.asStateFlow()

    private val _events = Channel<ActorDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ActorDetailsAction) {
        when (action) {
            ActorDetailsAction.OnBack -> send(ActorDetailsEvent.NavigateBack)
            ActorDetailsAction.OnRetry -> load()
            ActorDetailsAction.OnDismissError -> _state.update { it.copy(error = null) }
            is ActorDetailsAction.OnMovieClick -> send(ActorDetailsEvent.NavigateToMovie(action.movieId))
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val current = _state.value
            val request = ActorDetailsChainRequest(actorId, current.details, current.loadedMovies)
            val failure = detailsChain.create().invokeForResult(request).errorOrNull()

            // keep whatever loaded before a failure, so a retry only fetches the rest
            _state.update {
                it.copy(
                    details = request.details,
                    loadedMovies = request.knownMovies,
                    isLoading = false,
                    error = failure?.toUiError(),
                )
            }
        }
    }

    private fun send(event: ActorDetailsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
