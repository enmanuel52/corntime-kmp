package com.enmanuelbergling.corntime.feature.tvshows.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.domain.util.errorOrNull
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.tvshows.domain.home.TvShowsHomeChain
import com.enmanuelbergling.corntime.feature.tvshows.domain.home.TvShowsHomeRequest
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

@KoinViewModel
class TvShowsViewModel(
    private val homeChain: TvShowsHomeChain,
    private val syncUserDetails: SyncUserDetailsUseCase,
    userPreferences: UserPreferenceDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(TvShowsState())
    val state: StateFlow<TvShowsState> = _state.asStateFlow()

    private val _events = Channel<TvShowsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        userPreferences.getCurrentUser()
            .onEach { user -> _state.update { it.copy(user = user) } }
            .launchIn(viewModelScope)
        viewModelScope.launch { syncUserDetails() }
        load()
    }

    fun onAction(action: TvShowsAction) {
        when (action) {
            TvShowsAction.OnRetry -> load()
            TvShowsAction.OnDismissError -> _state.update { it.copy(error = null) }
            is TvShowsAction.OnTvShowClick -> send(TvShowsEvent.NavigateToDetails(action.tvShowId))
            is TvShowsAction.OnSectionClick -> send(TvShowsEvent.NavigateToSection(action.section))
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val request = TvShowsHomeRequest(_state.value.sections)
            val failure = homeChain.create().invokeForResult(request).errorOrNull()
            // keep whatever loaded before a failing handler
            _state.update {
                it.copy(sections = request.sections.toMap(), isLoading = false, error = failure?.toUiError())
            }
        }
    }

    private fun send(event: TvShowsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
