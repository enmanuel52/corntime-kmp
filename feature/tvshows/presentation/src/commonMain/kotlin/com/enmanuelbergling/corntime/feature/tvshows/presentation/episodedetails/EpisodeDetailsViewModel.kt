package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails

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
class EpisodeDetailsViewModel(
    @InjectedParam private val tvShowId: Int,
    @InjectedParam private val seasonNumber: Int,
    @InjectedParam private val episodeNumber: Int,
    private val dataSource: TvRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EpisodeDetailsState(tvShowId = tvShowId, seasonNumber = seasonNumber, episodeNumber = episodeNumber),
    )
    val state: StateFlow<EpisodeDetailsState> = _state.asStateFlow()

    private val _events = Channel<EpisodeDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: EpisodeDetailsAction) {
        when (action) {
            EpisodeDetailsAction.OnBack -> send(EpisodeDetailsEvent.NavigateBack)
            EpisodeDetailsAction.OnRetry -> load()
            EpisodeDetailsAction.OnDismissError -> _state.update { it.copy(error = null) }
            is EpisodeDetailsAction.OnPersonClick -> openPerson(action.personId)
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = dataSource.getEpisodeDetails(tvShowId, seasonNumber, episodeNumber)) {
                is Result.Success -> _state.update { it.copy(details = result.data, isLoading = false) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiError()) }
            }
        }
    }

    private fun openPerson(personId: Int) {
        val state = _state.value
        val person = (state.guestStars + state.crew).firstOrNull { it.id == personId }
        send(EpisodeDetailsEvent.NavigateToActor(personId, person?.name.orEmpty(), person?.profilePath))
    }

    private fun send(event: EpisodeDetailsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
