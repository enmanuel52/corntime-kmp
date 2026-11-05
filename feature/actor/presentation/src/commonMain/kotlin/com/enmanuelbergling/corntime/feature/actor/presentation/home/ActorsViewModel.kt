package com.enmanuelbergling.corntime.feature.actor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.ActorRemoteDataSource
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** The popular actors list, loaded page by page. */
@KoinViewModel
class ActorsViewModel(dataSource: ActorRemoteDataSource) : ViewModel() {

    private val paginator = Paginator { page -> dataSource.getPopularActors(page) }

    private val _state = MutableStateFlow(ActorsState())
    val state: StateFlow<ActorsState> = _state.asStateFlow()

    private val _events = Channel<ActorsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: ActorsAction) {
        when (action) {
            ActorsAction.OnLoadNextPage -> loadNextPage()
            is ActorsAction.OnActorClick -> viewModelScope.launch {
                // the list shows the original name, and the shared-element transition is keyed by what is shown
                _events.send(ActorsEvent.NavigateToDetails(action.actor.id, action.actor.originalName, action.actor.profilePath))
            }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }
}
