package com.enmanuelbergling.corntime.feature.watchlists.presentation.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler
import com.enmanuelbergling.corntime.core.domain.util.map
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalTracker
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.MoveTvShowToFavoritesUseCase
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
 * The account's TV show watchlist. Removing a TV show, or moving it to favorites, hides it at once with an
 * undo window; what has not reached TMDB when the screen is left goes to the background scheduler.
 */
@KoinViewModel
class WatchlistTvShowsViewModel(
    private val dataSource: TvRemoteDataSource,
    private val moveToFavorites: MoveTvShowToFavoritesUseCase,
    private val syncScheduler: TvShowSyncScheduler,
) : ViewModel() {

    private val paginator = Paginator { page -> dataSource.getAccountWatchlistTv(page) }

    private val removals = RemovalTracker(viewModelScope) { removal ->
        when (removal.kind) {
            RemovalKind.Remove -> dataSource.removeTvFromAccountWatchlist(removal.id).map { }
            RemovalKind.MoveToFavorites -> moveToFavorites(removal.id)
        }
    }

    private var wasShown = false

    private val _state = MutableStateFlow(WatchlistTvShowsState())
    val state: StateFlow<WatchlistTvShowsState> = _state.asStateFlow()

    private val _events = Channel<WatchlistTvShowsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        removals.state.onEach { removalState -> _state.update { it.copy(removals = removalState) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: WatchlistTvShowsAction) {
        when (action) {
            WatchlistTvShowsAction.OnLoadNextPage -> loadNextPage()
            WatchlistTvShowsAction.OnScreenShown -> onScreenShown()
            is WatchlistTvShowsAction.OnTvShowClick -> send(WatchlistTvShowsEvent.NavigateToDetails(action.tvShowId))
            is WatchlistTvShowsAction.OnRemove -> removals.remove(action.tvShowId, RemovalKind.Remove)
            is WatchlistTvShowsAction.OnMoveToFavorites -> removals.remove(action.tvShowId, RemovalKind.MoveToFavorites)
            is WatchlistTvShowsAction.OnUndo -> removals.undo(action.tvShowId)
            WatchlistTvShowsAction.OnRetryRemoval -> removals.retry()
            WatchlistTvShowsAction.OnDismissRemovalError -> removals.dismissFailure()
        }
    }

    private fun onScreenShown() {
        if (wasShown) viewModelScope.launch { paginator.refresh() }
        wasShown = true
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: WatchlistTvShowsEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    override fun onCleared() {
        val unfinished = removals.unfinished()
        if (unfinished.isEmpty()) return
        syncScheduler.watchlist(
            deletedTvShowIds = unfinished.filter { it.kind == RemovalKind.Remove }.map { it.id },
            favoriteTvShowIds = unfinished.filter { it.kind == RemovalKind.MoveToFavorites }.map { it.id },
        )
    }
}
