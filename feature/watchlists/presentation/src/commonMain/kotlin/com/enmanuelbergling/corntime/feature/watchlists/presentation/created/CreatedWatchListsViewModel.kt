package com.enmanuelbergling.corntime.feature.watchlists.presentation.created

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.util.onFailure
import com.enmanuelbergling.corntime.core.domain.util.onSuccess
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.paging.Paginator
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ValidateListFormUseCase
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

/** The lists the user created: browse, create and delete. */
@KoinViewModel
class CreatedWatchListsViewModel(
    private val userRemote: UserRemoteDataSource,
    private val validateListForm: ValidateListFormUseCase,
) : ViewModel() {

    private val paginator = Paginator { page -> userRemote.getWatchLists(page) }

    private val _state = MutableStateFlow(CreatedWatchListsState())
    val state: StateFlow<CreatedWatchListsState> = _state.asStateFlow()

    private val _events = Channel<CreatedWatchListsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        paginator.state.onEach { paging -> _state.update { it.copy(paging = paging) } }.launchIn(viewModelScope)
        loadNextPage()
    }

    fun onAction(action: CreatedWatchListsAction) {
        when (action) {
            CreatedWatchListsAction.OnLoadNextPage -> loadNextPage()
            is CreatedWatchListsAction.OnListClick ->
                send(CreatedWatchListsEvent.NavigateToDetails(action.list.id, action.list.name))

            CreatedWatchListsAction.OnToggleForm -> _state.update {
                // closing the form discards what was typed
                it.copy(form = if (it.form.isVisible) CreateListForm() else it.form.copy(isVisible = true))
            }

            is CreatedWatchListsAction.OnNameChange ->
                _state.update { it.copy(form = it.form.copy(name = action.value, nameError = null)) }

            is CreatedWatchListsAction.OnDescriptionChange ->
                _state.update { it.copy(form = it.form.copy(description = action.value, descriptionError = null)) }

            CreatedWatchListsAction.OnSubmitForm -> submit()
            is CreatedWatchListsAction.OnDeleteList -> delete(action.listId)
            CreatedWatchListsAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadNextPage() {
        viewModelScope.launch { paginator.loadNext() }
    }

    private fun submit() {
        val form = _state.value.form
        if (_state.value.isWorking) return

        val validation = validateListForm(form.name, form.description)
        _state.update {
            it.copy(form = it.form.copy(nameError = validation.nameError, descriptionError = validation.descriptionError))
        }
        if (!validation.isValid) return

        _state.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            userRemote.createWatchList(CreateListPost(form.name.trim(), form.description.trim()))
                .onSuccess {
                    _state.update { it.copy(isWorking = false, form = CreateListForm()) }
                    paginator.refresh()
                }
                .onFailure { error -> _state.update { it.copy(isWorking = false, error = error.toUiError()) } }
        }
    }

    private fun delete(listId: Int) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            userRemote.deleteList(listId)
                .onSuccess {
                    _state.update { it.copy(isWorking = false) }
                    _events.send(CreatedWatchListsEvent.ListDeleted(listId))
                    paginator.refresh()
                }
                .onFailure { error -> _state.update { it.copy(isWorking = false, error = error.toUiError()) } }
        }
    }

    private fun send(event: CreatedWatchListsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
