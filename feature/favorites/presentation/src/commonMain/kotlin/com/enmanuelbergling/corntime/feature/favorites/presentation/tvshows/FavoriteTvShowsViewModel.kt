package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler
import com.enmanuelbergling.corntime.core.domain.util.map
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalTracker
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

/**
 * The account's favorite TV shows. Removing one hides it at once with an undo window; if the screen is
 * left before the change reached TMDB, the rest goes to the background scheduler.
 */
@KoinViewModel
class FavoriteTvShowsViewModel(
    private val dataSource: TvRemoteDataSource,
    private val syncScheduler: TvShowSyncScheduler,
) : ViewModel() {

    private val paginator = Paginator { page -> dataSource.getAccountFavoriteTv(page) }

    private val removals = RemovalTracker(viewModelScope) { removal ->
        dataSource.removeTvFromFavorites(removal.id).map { }
    }

    private var wasShown = false

    private val _state = MutableStateFlow(FavoriteTvShowsState())
    val state: StateFlow<FavoriteTvShowsState> = _state.asStateFlow()

    private val _events = Channel<FavoriteTvShowsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        removals.state.onEach { removalState -> _state.update { it.copy(removals = removalState) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: FavoriteTvShowsAction) {
        when (action) {
            FavoriteTvShowsAction.OnLoadNextPage -> loadNextPage()
            FavoriteTvShowsAction.OnScreenShown -> onScreenShown()
            is FavoriteTvShowsAction.OnTvShowClick -> send(FavoriteTvShowsEvent.NavigateToDetails(action.tvShowId))
            FavoriteTvShowsAction.OnBack -> send(FavoriteTvShowsEvent.NavigateBack)
            is FavoriteTvShowsAction.OnRemove -> removals.remove(action.tvShowId, RemovalKind.Remove)
            is FavoriteTvShowsAction.OnUndoRemove -> removals.undo(action.tvShowId)
            FavoriteTvShowsAction.OnRetryRemoval -> removals.retry()
            FavoriteTvShowsAction.OnDismissRemovalError -> removals.dismissFailure()
        }
    }

    private fun onScreenShown() {
        if (wasShown) viewModelScope.launch { paginator.refresh() }
        wasShown = true
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: FavoriteTvShowsEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    override fun onCleared() {
        val unfinished = removals.unfinished()
        if (unfinished.isNotEmpty()) syncScheduler.deleteFromFavorite(unfinished.map { it.id })
    }
}
