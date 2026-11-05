package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieFilter
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

internal val FILTER_DEBOUNCE = 500.milliseconds

/** TMDB's `sort_by` value; every criteria sorts descending. */
internal val SortCriteria.apiValue: String
    get() = when (this) {
        SortCriteria.Popularity -> "popularity.desc"
        SortCriteria.VoteAverage -> "vote_average.desc"
        SortCriteria.VoteCount -> "vote_count.desc"
        SortCriteria.Revenue -> "revenue.desc"
    }

/** Discovers movies by genres and sort order. Replaces Pop-Corns' `MoviesFilterVM` and its Paging 3 flow. */
@KoinViewModel
class MoviesFilterViewModel(
    private val dataSource: MovieRemoteDataSource,
) : ViewModel() {

    /** The filter the paginator currently belongs to, so a page is never requested for a half-debounced one. */
    private var activeFilter = MovieFilter()

    private val paginator = Paginator { page ->
        dataSource.getMoviesByGenre(
            genres = activeFilter.genres.joinToString(",") { it.id.toString() },
            sortBy = activeFilter.sortBy.apiValue,
            page = page,
        )
    }

    private val _state = MutableStateFlow(MoviesFilterState())
    val state: StateFlow<MoviesFilterState> = _state.asStateFlow()

    private val _events = Channel<MoviesFilterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        loadGenres()
        loadNextPage()
        observeFilter()
    }

    fun onAction(action: MoviesFilterAction) {
        when (action) {
            MoviesFilterAction.OnBack -> send(MoviesFilterEvent.NavigateBack)
            is MoviesFilterAction.OnToggleGenre -> updateFilter { filter ->
                val genre = action.genre
                filter.copy(genres = if (genre in filter.genres) filter.genres - genre else filter.genres + genre)
            }
            is MoviesFilterAction.OnPickSortCriteria -> updateFilter { it.copy(sortBy = action.criteria) }
            MoviesFilterAction.OnClearFilter -> updateFilter { MovieFilter() }
            MoviesFilterAction.OnLoadNextPage -> loadNextPage()
            MoviesFilterAction.OnRetryGenres -> loadGenres()
            MoviesFilterAction.OnDismissError -> _state.update { it.copy(genresError = null) }
            is MoviesFilterAction.OnMovieClick -> send(MoviesFilterEvent.NavigateToDetails(action.movieId))
        }
    }

    private fun updateFilter(transform: (MovieFilter) -> MovieFilter) {
        _state.update { it.copy(filter = transform(it.filter)) }
    }

    /** The first page for the default filter loads right away; later changes wait for a pause. */
    @OptIn(FlowPreview::class)
    private fun observeFilter() {
        viewModelScope.launch {
            _state
                .map { it.filter }
                .distinctUntilChanged()
                .drop(1)
                .debounce(FILTER_DEBOUNCE)
                .collectLatest { filter ->
                    // toggled back to what is already shown within the pause: nothing to reload
                    if (filter == activeFilter) return@collectLatest
                    activeFilter = filter
                    paginator.refresh()
                }
        }
    }

    private fun loadGenres() {
        if (_state.value.isLoadingGenres) return
        _state.update { it.copy(isLoadingGenres = true, genresError = null) }
        viewModelScope.launch {
            when (val result = dataSource.getMovieGenres()) {
                is Result.Success -> _state.update { it.copy(genres = result.data, isLoadingGenres = false) }
                is Result.Failure -> _state.update {
                    it.copy(isLoadingGenres = false, genresError = result.error.toUiError())
                }
            }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: MoviesFilterEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
