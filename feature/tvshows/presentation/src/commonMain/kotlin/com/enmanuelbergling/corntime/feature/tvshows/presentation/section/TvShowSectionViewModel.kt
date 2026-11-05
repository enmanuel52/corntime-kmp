package com.enmanuelbergling.corntime.feature.tvshows.presentation.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
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
class TvShowSectionViewModel(
    @InjectedParam section: TvShowsSection,
    dataSource: TvRemoteDataSource,
) : ViewModel() {

    private val paginator = Paginator { page ->
        when (section) {
            TvShowsSection.Popular -> dataSource.getPopularTv(page)
            TvShowsSection.TopRated -> dataSource.getTopRatedTv(page)
            TvShowsSection.OnTheAir -> dataSource.getOnTheAirTv(page)
            TvShowsSection.AiringToday -> dataSource.getAiringTodayTv(page)
        }
    }

    private val _state = MutableStateFlow(TvShowSectionState(section))
    val state: StateFlow<TvShowSectionState> = _state.asStateFlow()

    private val _events = Channel<TvShowSectionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state
            .onEach { paging -> _state.update { it.copy(paging = paging) } }
            .launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: TvShowSectionAction) {
        when (action) {
            TvShowSectionAction.OnLoadNextPage -> loadNextPage()
            is TvShowSectionAction.OnTvShowClick -> send(TvShowSectionEvent.NavigateToDetails(action.tvShowId))
            TvShowSectionAction.OnBack -> send(TvShowSectionEvent.NavigateBack)
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun send(event: TvShowSectionEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
