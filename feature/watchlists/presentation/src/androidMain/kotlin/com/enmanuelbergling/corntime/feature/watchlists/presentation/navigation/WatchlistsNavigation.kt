package com.enmanuelbergling.corntime.feature.watchlists.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.feature.watchlists.presentation.created.CreatedWatchListsRoot
import com.enmanuelbergling.corntime.feature.watchlists.presentation.details.ListShortcuts
import com.enmanuelbergling.corntime.feature.watchlists.presentation.details.WatchListDetailsRoot
import com.enmanuelbergling.corntime.feature.watchlists.presentation.home.WatchlistHomeRoot
import kotlinx.serialization.Serializable

@Serializable
data object WatchlistKey : NavKey

@Serializable
data object CreatedListsKey : NavKey

@Serializable
data class ListDetailsKey(val listId: Int, val listName: String) : NavKey

fun EntryProviderScope<NavKey>.watchlistEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onMovieDetails: (movieId: Int) -> Unit,
    onTvShowDetails: (tvShowId: Int) -> Unit,
    shortcuts: ListShortcuts = ListShortcuts.None,
    onListDeleted: (listId: Int) -> Unit = {},
) {
    entry<WatchlistKey> {
        WatchlistHomeRoot(
            onMovieDetails = onMovieDetails,
            onTvShowDetails = onTvShowDetails,
            onNavigateToLists = { onNavigate(CreatedListsKey) },
            onBack = onBack,
        )
    }

    entry<CreatedListsKey> {
        CreatedWatchListsRoot(
            onDetails = { id, name -> onNavigate(ListDetailsKey(id, name)) },
            onListDeleted = onListDeleted,
            onBack = onBack,
        )
    }

    entry<ListDetailsKey> { key ->
        WatchListDetailsRoot(
            listId = key.listId,
            listName = key.listName,
            shortcuts = shortcuts,
            onMovieDetails = onMovieDetails,
            onBack = onBack,
        )
    }
}
