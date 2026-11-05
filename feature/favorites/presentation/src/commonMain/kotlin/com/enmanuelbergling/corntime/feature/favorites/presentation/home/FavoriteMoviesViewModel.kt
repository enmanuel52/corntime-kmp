package com.enmanuelbergling.corntime.feature.favorites.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
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
 * The account's favorite movies. Removing one hides it at once with an undo window; if the screen is
 * left before the change reached TMDB, the rest goes to the background scheduler.
 */
@KoinViewModel
class FavoriteMoviesViewModel(
    private val userRemote: UserRemoteDataSource,
    private val syncScheduler: MovieSyncScheduler,
) : ViewModel() {

    private val paginator = Paginator { page -> userRemote.getAccountFavoriteMovies(page) }

    private val removals = RemovalTracker(viewModelScope) { removal ->
        userRemote.removeMovieFromFavorites(removal.id).map { }
    }

    private val _state = MutableStateFlow(FavoriteMoviesState())
    val state: StateFlow<FavoriteMoviesState> = _state.asStateFlow()

    private val _events = Channel<FavoriteMoviesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        removals.state.onEach { removalState -> _state.update { it.copy(removals = removalState) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: FavoriteMoviesAction) {
        when (action) {
            FavoriteMoviesAction.OnLoadNextPage -> loadNextPage()
            is FavoriteMoviesAction.OnMovieClick -> send(FavoriteMoviesEvent.NavigateToDetails(action.movieId))
            FavoriteMoviesAction.OnBack -> send(FavoriteMoviesEvent.NavigateBack)
            is FavoriteMoviesAction.OnRemove -> removals.remove(action.movieId, RemovalKind.Remove)
            is FavoriteMoviesAction.OnUndoRemove -> removals.undo(action.movieId)
            FavoriteMoviesAction.OnRetryRemoval -> removals.retry()
            FavoriteMoviesAction.OnDismissRemovalError -> removals.dismissFailure()
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: FavoriteMoviesEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    override fun onCleared() {
        val unfinished = removals.unfinished()
        if (unfinished.isNotEmpty()) syncScheduler.deleteFromFavorite(unfinished.map { it.id })
    }
}
