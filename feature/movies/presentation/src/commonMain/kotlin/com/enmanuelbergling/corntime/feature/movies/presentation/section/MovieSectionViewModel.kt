package com.enmanuelbergling.corntime.feature.movies.presentation.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
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
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** The paged "see all" list of one home section. Replaces the four Pop-Corns per-section ViewModels. */
@KoinViewModel
class MovieSectionViewModel(
    @InjectedParam section: MovieSection,
    dataSource: MovieRemoteDataSource,
) : ViewModel() {

    private val paginator = Paginator { page ->
        when (section) {
            MovieSection.Upcoming -> dataSource.getUpcomingMovies(page)
            MovieSection.TopRated -> dataSource.getTopRatedMovies(page)
            MovieSection.NowPlaying -> dataSource.getNowPlayingMovies(page)
            MovieSection.Popular -> dataSource.getPopularMovies(page)
        }
    }

    private val _state = MutableStateFlow(MovieSectionState(section))
    val state: StateFlow<MovieSectionState> = _state.asStateFlow()

    private val _events = Channel<MovieSectionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: MovieSectionAction) {
        when (action) {
            MovieSectionAction.OnLoadNextPage -> loadNextPage()
            is MovieSectionAction.OnMovieClick -> send(MovieSectionEvent.NavigateToDetails(action.movieId))
            MovieSectionAction.OnBack -> send(MovieSectionEvent.NavigateBack)
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: MovieSectionEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
