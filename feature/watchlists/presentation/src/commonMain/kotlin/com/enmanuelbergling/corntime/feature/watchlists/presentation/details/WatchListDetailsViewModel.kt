package com.enmanuelbergling.corntime.feature.watchlists.presentation.details

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
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** The movies of one list the user created. Removals work like on the watchlist, against this list. */
@KoinViewModel
class WatchListDetailsViewModel(
    @InjectedParam private val listId: Int,
    private val userRemote: UserRemoteDataSource,
    private val moveToFavorites: MoveMovieToFavoritesUseCase,
    private val syncScheduler: MovieSyncScheduler,
) : ViewModel() {

    private val paginator = Paginator { page -> userRemote.getWatchListMovies(listId, page) }

    private val removals = RemovalTracker(viewModelScope) { removal ->
        when (removal.kind) {
            RemovalKind.Remove -> userRemote.deleteMovieFromList(removal.id, listId).map { }
            RemovalKind.MoveToFavorites -> moveToFavorites(removal.id, listId)
        }
    }

    private val _state = MutableStateFlow(WatchListDetailsState(listId))
    val state: StateFlow<WatchListDetailsState> = _state.asStateFlow()

    private val _events = Channel<WatchListDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        removals.state.onEach { removalState -> _state.update { it.copy(removals = removalState) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: WatchListDetailsAction) {
        when (action) {
            WatchListDetailsAction.OnLoadNextPage -> loadNextPage()
            is WatchListDetailsAction.OnMovieClick -> send(WatchListDetailsEvent.NavigateToDetails(action.movieId))
            WatchListDetailsAction.OnBack -> send(WatchListDetailsEvent.NavigateBack)
            is WatchListDetailsAction.OnRemove -> removals.remove(action.movieId, RemovalKind.Remove)
            is WatchListDetailsAction.OnMoveToFavorites -> removals.remove(action.movieId, RemovalKind.MoveToFavorites)
            is WatchListDetailsAction.OnUndo -> removals.undo(action.movieId)
            WatchListDetailsAction.OnRetryRemoval -> removals.retry()
            WatchListDetailsAction.OnDismissRemovalError -> removals.dismissFailure()
            WatchListDetailsAction.OnAddShortcut -> send(WatchListDetailsEvent.AddShortcut(listId))
            WatchListDetailsAction.OnRemoveShortcut -> send(WatchListDetailsEvent.RemoveShortcut(listId))
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: WatchListDetailsEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    override fun onCleared() {
        val unfinished = removals.unfinished()
        if (unfinished.isEmpty()) return
        syncScheduler.deleteFromList(
            listId = listId,
            deletedMovieIds = unfinished.filter { it.kind == RemovalKind.Remove }.map { it.id },
            favoriteMovieIds = unfinished.filter { it.kind == RemovalKind.MoveToFavorites }.map { it.id },
        )
    }
}
