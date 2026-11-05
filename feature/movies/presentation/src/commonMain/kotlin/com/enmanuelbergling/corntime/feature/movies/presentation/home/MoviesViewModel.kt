package com.enmanuelbergling.corntime.feature.movies.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.domain.util.errorOrNull
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.feature.movies.domain.home.MoviesHomeChain
import com.enmanuelbergling.corntime.feature.movies.domain.home.MoviesHomeRequest
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
class MoviesViewModel(
    private val homeChain: MoviesHomeChain,
    private val syncUserDetails: SyncUserDetailsUseCase,
    userPreferences: UserPreferenceDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(MoviesState())
    val state: StateFlow<MoviesState> = _state.asStateFlow()

    private val _events = Channel<MoviesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        userPreferences.getCurrentUser()
            .onEach { user -> _state.update { it.copy(user = user) } }
            .launchIn(viewModelScope)
        viewModelScope.launch { syncUserDetails() }
        load()
    }

    fun onAction(action: MoviesAction) {
        when (action) {
            MoviesAction.OnRetry -> load()
            MoviesAction.OnDismissError -> _state.update { it.copy(error = null) }
            is MoviesAction.OnMovieClick -> send(MoviesEvent.NavigateToDetails(action.movieId))
            is MoviesAction.OnSectionClick -> send(MoviesEvent.NavigateToSection(action.section))
        }
    }

    private fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val request = MoviesHomeRequest(_state.value.sections)
            val failure = homeChain.create().invokeForResult(request).errorOrNull()
            // keep whatever loaded before a failing handler
            _state.update {
                it.copy(sections = request.sections.toMap(), isLoading = false, error = failure?.toUiError())
            }
        }
    }

    private fun send(event: MoviesEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
