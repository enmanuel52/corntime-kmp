package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EpisodesViewModel(
    @InjectedParam private val tvShowId: Int,
    @InjectedParam private val seasonNumber: Int,
    private val dataSource: TvRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(EpisodesState(tvShowId = tvShowId, seasonNumber = seasonNumber))
    val state: StateFlow<EpisodesState> = _state.asStateFlow()

    private val _events = Channel<EpisodesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: EpisodesAction) {
        when (action) {
            EpisodesAction.OnBack -> send(EpisodesEvent.NavigateBack)
            EpisodesAction.OnRetry -> load()
            EpisodesAction.OnDismissError -> _state.update { it.copy(error = null) }
            is EpisodesAction.OnEpisodeClick -> send(EpisodesEvent.NavigateToEpisodeDetails(action.episodeNumber))
            is EpisodesAction.OnEpisodeToggle -> _state.update {
                it.copy(expandedEpisodeId = action.episodeId.takeUnless { id -> id == it.expandedEpisodeId })
            }
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = dataSource.getSeasonDetails(tvShowId, seasonNumber)) {
                is Result.Success -> _state.update { it.copy(seasonDetails = result.data, isLoading = false) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiError()) }
            }
        }
    }

    private fun send(event: EpisodesEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
