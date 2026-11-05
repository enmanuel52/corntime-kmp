package com.enmanuelbergling.corntime.feature.movies.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

internal val SEARCH_DEBOUNCE = 500.milliseconds

@KoinViewModel
class MovieSearchViewModel(
    private val dataSource: MovieRemoteDataSource,
) : ViewModel() {

    /**
     * The query the paginator currently belongs to. Pages are always requested for this one, never for
     * what was typed since: otherwise page 2 of "abc" could be fetched with "abcd" while it is debounced.
     */
    private var activeQuery = ""

    private val paginator = Paginator { page -> dataSource.searchMovie(activeQuery, page) }

    private val typedQuery = MutableStateFlow("")

    private val _state = MutableStateFlow(MovieSearchState())
    val state: StateFlow<MovieSearchState> = _state.asStateFlow()

    private val _events = Channel<MovieSearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        observeQuery()
    }

    fun onAction(action: MovieSearchAction) {
        when (action) {
            is MovieSearchAction.OnQueryChange -> setQuery(action.query)
            MovieSearchAction.OnClear -> setQuery("")
            MovieSearchAction.OnLoadNextPage -> if (activeQuery.isNotBlank()) {
                viewModelScope.launch { paginator.loadNext() }
            }
            is MovieSearchAction.OnMovieClick ->
                viewModelScope.launch { _events.send(MovieSearchEvent.NavigateToDetails(action.movieId)) }
        }
    }

    private fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        typedQuery.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            typedQuery
                .map { it.trim() }
                // clearing is immediate, typing waits for a pause
                .debounce { if (it.isBlank()) 0.milliseconds else SEARCH_DEBOUNCE }
                .distinctUntilChanged()
                .collectLatest { query ->
                    activeQuery = query
                    paginator.reset()
                    if (query.isNotBlank()) paginator.loadNext()
                }
        }
    }
}
