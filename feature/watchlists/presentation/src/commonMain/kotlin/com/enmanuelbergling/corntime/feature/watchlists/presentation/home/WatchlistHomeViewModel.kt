package com.enmanuelbergling.corntime.feature.watchlists.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
import com.enmanuelbergling.corntime.core.domain.util.map
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalTracker
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.MoveMovieToFavoritesUseCase
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
 * The account's movie watchlist. Removing a movie, or moving it to favorites, hides it at once with an
 * undo window; what has not reached TMDB when the screen is left goes to the background scheduler.
 */
@KoinViewModel
class WatchlistHomeViewModel(
    private val userRemote: UserRemoteDataSource,
    private val moveToFavorites: MoveMovieToFavoritesUseCase,
    private val syncScheduler: MovieSyncScheduler,
) : ViewModel() {

    private val paginator = Paginator { page -> userRemote.getAccountWatchlistMovies(page) }

    private val removals = RemovalTracker(viewModelScope) { removal ->
        when (removal.kind) {
            RemovalKind.Remove -> userRemote.removeMovieFromAccountWatchlist(removal.id).map { }
            RemovalKind.MoveToFavorites -> moveToFavorites(removal.id)
        }
    }

    private var wasShown = false

    private val _state = MutableStateFlow(WatchlistHomeState())
    val state: StateFlow<WatchlistHomeState> = _state.asStateFlow()

    private val _events = Channel<WatchlistHomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        removals.state.onEach { removalState -> _state.update { it.copy(removals = removalState) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: WatchlistHomeAction) {
        when (action) {
            WatchlistHomeAction.OnLoadNextPage -> loadNextPage()
            WatchlistHomeAction.OnScreenShown -> onScreenShown()
            is WatchlistHomeAction.OnMovieClick -> send(WatchlistHomeEvent.NavigateToDetails(action.movieId))
            WatchlistHomeAction.OnListsClick -> send(WatchlistHomeEvent.NavigateToLists)
            is WatchlistHomeAction.OnRemove -> removals.remove(action.movieId, RemovalKind.Remove)
            is WatchlistHomeAction.OnMoveToFavorites -> removals.remove(action.movieId, RemovalKind.MoveToFavorites)
            is WatchlistHomeAction.OnUndo -> removals.undo(action.movieId)
            WatchlistHomeAction.OnRetryRemoval -> removals.retry()
            WatchlistHomeAction.OnDismissRemovalError -> removals.dismissFailure()
        }
    }

    private fun onScreenShown() {
        if (wasShown) viewModelScope.launch { paginator.refresh() }
        wasShown = true
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: WatchlistHomeEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    override fun onCleared() {
        val unfinished = removals.unfinished()
        if (unfinished.isEmpty()) return
        syncScheduler.watchlist(
            deletedMovieIds = unfinished.filter { it.kind == RemovalKind.Remove }.map { it.id },
            favoriteMovieIds = unfinished.filter { it.kind == RemovalKind.MoveToFavorites }.map { it.id },
        )
    }
}
