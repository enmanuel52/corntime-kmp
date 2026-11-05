package com.enmanuelbergling.corntime.feature.watchlists.presentation.created

import com.enmanuelbergling.corntime.core.domain.model.user.WatchList
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ListFieldError

data class CreateListForm(
    val name: String = "",
    val description: String = "",
    val nameError: ListFieldError? = null,
    val descriptionError: ListFieldError? = null,
    val isVisible: Boolean = false,
)

data class CreatedWatchListsState(
    val paging: PagingState<WatchList> = PagingState(),
    val form: CreateListForm = CreateListForm(),
    /** A create or delete request is running. */
    val isWorking: Boolean = false,
    val error: UiError? = null,
) {
    /** The loaded lists with a concrete element type (generic `items` is untyped in Swift). */
    val lists: List<WatchList> get() = paging.items
}

sealed interface CreatedWatchListsAction {
    /**
     * The screen became visible. The first time nothing happens (the first page is already loading); when
     * it comes back (for example from a movie's details) the list reloads, since it may have changed there.
     */
    data object OnScreenShown : CreatedWatchListsAction
    data object OnLoadNextPage : CreatedWatchListsAction
    data class OnListClick(val list: WatchList) : CreatedWatchListsAction
    data object OnToggleForm : CreatedWatchListsAction
    data class OnNameChange(val value: String) : CreatedWatchListsAction
    data class OnDescriptionChange(val value: String) : CreatedWatchListsAction
    data object OnSubmitForm : CreatedWatchListsAction
    data class OnDeleteList(val listId: Int) : CreatedWatchListsAction
    data object OnDismissError : CreatedWatchListsAction
}

sealed interface CreatedWatchListsEvent {
    data class NavigateToDetails(val listId: Int, val name: String) : CreatedWatchListsEvent

    /** The app can drop anything tied to the list, such as its Android shortcut. */
    data class ListDeleted(val listId: Int) : CreatedWatchListsEvent
}
