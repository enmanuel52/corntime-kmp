package com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons

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
class SeasonsViewModel(
    @InjectedParam private val tvShowId: Int,
    private val dataSource: TvRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SeasonsState(tvShowId = tvShowId))
    val state: StateFlow<SeasonsState> = _state.asStateFlow()

    private val _events = Channel<SeasonsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: SeasonsAction) {
        when (action) {
            SeasonsAction.OnBack -> send(SeasonsEvent.NavigateBack)
            SeasonsAction.OnRetry -> load()
            SeasonsAction.OnDismissError -> _state.update { it.copy(error = null) }
            is SeasonsAction.OnSeasonClick -> send(SeasonsEvent.NavigateToEpisodes(action.seasonNumber))
            is SeasonsAction.OnSeasonToggle -> _state.update {
                it.copy(expandedSeasonId = action.seasonId.takeUnless { id -> id == it.expandedSeasonId })
            }
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = dataSource.getTvDetails(tvShowId)) {
                is Result.Success -> _state.update { it.copy(details = result.data, isLoading = false) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiError()) }
            }
        }
    }

    private fun send(event: SeasonsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
