package com.enmanuelbergling.corntime.feature.watchlists.presentation.created

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.CtiContentDialog
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.DeleteMovieConfirmationDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingScreen
import com.enmanuelbergling.corntime.core.designsystem.components.NewerDragListItem
import com.enmanuelbergling.corntime.core.designsystem.components.common.WatchListCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.WatchListCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.designsystem.design.CtiTextField
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ListFieldError
import com.enmanuelbergling.corntime.feature.watchlists.presentation.R
import org.koin.compose.viewmodel.koinViewModel

private const val NO_LIST = -1

object CreatedListsTags {
    const val ADD = "lists add"
    const val LIST = "lists list"
    const val DELETE = "lists delete"
    const val SUBMIT = "lists create submit"
}

@Composable
fun CreatedWatchListsRoot(
    onDetails: (listId: Int, listName: String) -> Unit,
    onListDeleted: (listId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<CreatedWatchListsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(CreatedWatchListsAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CreatedWatchListsEvent.NavigateToDetails -> onDetails(event.listId, event.name)
            is CreatedWatchListsEvent.ListDeleted -> onListDeleted(event.listId)
        }
    }

    CreatedWatchListsScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatedWatchListsScreen(
    state: CreatedWatchListsState,
    onAction: (CreatedWatchListsAction) -> Unit,
    onBack: () -> Unit,
) {
    val paging = state.paging
    var pickedList by rememberSaveable { mutableIntStateOf(NO_LIST) }

    state.error?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(CreatedWatchListsAction.OnDismissError) },
            message = stringResource(error.messageResource),
        )
    }

    if (pickedList != NO_LIST) {
        DeleteMovieConfirmationDialog(
            title = stringResource(R.string.delete_list_title),
            onDismiss = { pickedList = NO_LIST },
            onDelete = {
                val listId = pickedList
                pickedList = NO_LIST
                onAction(CreatedWatchListsAction.OnDeleteList(listId))
            },
        )
    }

    if (state.form.isVisible) CreateListDialog(state.form, onAction)

    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.my_lists)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(CreatedWatchListsAction.OnToggleForm) },
                        modifier = Modifier.testTag(CreatedListsTags.ADD),
                    ) {
                        Icon(
                            painter = painterResource(DesignR.drawable.add),
                            contentDescription = stringResource(DesignR.string.add_icon),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        val error = paging.error
        when {
            state.isWorking && state.lists.isEmpty() -> LoadingScreen(Modifier.padding(paddingValues))

            error != null && state.lists.isEmpty() -> ErrorScreen(
                errorMessage = stringResource(error.toUiError().messageResource),
                onRetry = { onAction(CreatedWatchListsAction.OnLoadNextPage) },
                modifier = Modifier.padding(paddingValues),
            )

            paging.isEmpty -> Box(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.no_lists_yet)) }

            else -> {
                val listState = rememberLazyListState()
                listState.OnLoadMore(itemCount = paging.items.size, enabled = error == null) {
                    onAction(CreatedWatchListsAction.OnLoadNextPage)
                }
                val revealed = with(LocalDensity.current) { (-80).dp.toPx() }

                LazyColumn(
                    modifier = Modifier
                        .testTag(CreatedListsTags.LIST)
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(horizontal = MaterialTheme.dimen.verySmall)
                        .shimmerIf { paging.isLoadingFirstPage },
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                ) {
                    items(state.lists, key = { list -> list.id }) { list ->
                        NewerDragListItem(
                            bottomContentWidth = revealed,
                            bottomContent = {
                                Box(Modifier.align(Alignment.CenterEnd)) {
                                    IconButton(
                                        onClick = { pickedList = list.id },
                                        modifier = Modifier
                                            .testTag(CreatedListsTags.DELETE)
                                            .padding(horizontal = MaterialTheme.dimen.small),
                                    ) {
                                        Icon(
                                            painter = painterResource(DesignR.drawable.trash),
                                            contentDescription = stringResource(DesignR.string.delete_icon),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CardDefaults.elevatedShape,
                            ),
                        ) {
                            WatchListCard(
                                name = list.name,
                                description = list.description,
                                modifier = Modifier.fillMaxWidth(),
                            ) { onAction(CreatedWatchListsAction.OnListClick(list)) }
                        }
                    }

                    if (paging.isLoadingFirstPage) items(12) { WatchListCardPlaceholder() }
                    if (error != null && state.lists.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { onAction(CreatedWatchListsAction.OnLoadNextPage) },
                                    shape = RoundedCornerShape(50),
                                ) { Text(stringResource(DesignR.string.retry)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateListDialog(
    form: CreateListForm,
    onAction: (CreatedWatchListsAction) -> Unit,
) {
    CtiContentDialog(
        onDismiss = { onAction(CreatedWatchListsAction.OnToggleForm) },
        title = { Text(stringResource(R.string.new_list)) },
        confirmButton = {
            TextButton(
                onClick = { onAction(CreatedWatchListsAction.OnSubmitForm) },
                modifier = Modifier.testTag(CreatedListsTags.SUBMIT),
            ) { Text(text = stringResource(R.string.create)) }
        },
        verticalSpacing = MaterialTheme.dimen.medium,
    ) {
        createListFields(form, onAction)
    }
}

/** The two inputs of the "new list" dialog, apart from the dialog window so they can be tested alone. */
internal fun LazyListScope.createListFields(
    form: CreateListForm,
    onAction: (CreatedWatchListsAction) -> Unit,
) {
    item {
        CtiTextField(
            text = form.name,
            onTextChange = { onAction(CreatedWatchListsAction.OnNameChange(it)) },
            hint = stringResource(R.string.list_name_hint),
            leadingIcon = { Icon(painter = painterResource(DesignR.drawable.info_circle), null) },
            errorText = form.nameError?.let { stringResource(it.messageResource) },
        )
    }

    item {
        CtiTextField(
            text = form.description,
            onTextChange = { onAction(CreatedWatchListsAction.OnDescriptionChange(it)) },
            hint = stringResource(R.string.list_description_hint),
            leadingIcon = { Icon(painter = painterResource(DesignR.drawable.info_circle), null) },
            errorText = form.descriptionError?.let { stringResource(it.messageResource) },
        )
    }
}

private val ListFieldError.messageResource: Int
    get() = when (this) {
        ListFieldError.NameRequired -> R.string.list_name_required
        ListFieldError.DescriptionRequired -> R.string.list_description_required
    }
