package com.enmanuelbergling.corntime.core.designsystem.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind

/**
 * Shows "removed - undo" for the latest removal still inside its undo window. The ViewModel owns the
 * timing, so the snackbar stays until the removal is confirmed (or taken back) and then goes away.
 */
@Composable
fun UndoSnackbarEffect(
    undoable: PendingRemoval?,
    hostState: SnackbarHostState,
    removedMessage: String,
    movedToFavoritesMessage: String,
    undoLabel: String,
    onUndo: (id: Int) -> Unit,
) {
    val latestOnUndo by rememberUpdatedState(onUndo)

    LaunchedEffect(undoable) {
        if (undoable == null) {
            hostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val message = when (undoable.kind) {
            RemovalKind.Remove -> removedMessage
            RemovalKind.MoveToFavorites -> movedToFavoritesMessage
        }
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) latestOnUndo(undoable.id)
    }
}
