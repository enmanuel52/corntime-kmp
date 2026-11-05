package com.enmanuelbergling.corntime.feature.actor.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.enmanuelbergling.corntime.feature.actor.presentation.details.ActorDetailsRoot
import com.enmanuelbergling.corntime.feature.actor.presentation.home.ActorsRoot
import kotlinx.serialization.Serializable

@Serializable
data object ActorsKey : NavKey

/** [imagePath] and [name] are only what the screen shows while its own request is running (and for the shared-element transition). */
@Serializable
data class ActorDetailsKey(val id: Int, val imagePath: String? = null, val name: String = "") : NavKey

fun EntryProviderScope<NavKey>.actorEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onMovie: (movieId: Int) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    entry<ActorsKey> {
        ActorsRoot(
            onDetails = { onNavigate(ActorDetailsKey(it.id, it.imagePath, it.name)) },
            onOpenDrawer = onOpenDrawer,
        )
    }

    entry<ActorDetailsKey> { key ->
        ActorDetailsRoot(
            id = key.id,
            imagePath = key.imagePath,
            name = key.name,
            onMovie = onMovie,
            onBack = onBack,
        )
    }
}
