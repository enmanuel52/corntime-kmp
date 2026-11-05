package com.enmanuelbergling.corntime.feature.actor.presentation.home

import com.enmanuelbergling.corntime.core.domain.model.actor.Actor
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState

data class ActorsState(
    val paging: PagingState<Actor> = PagingState(),
) {
    /** The loaded actors with a concrete element type (generic `items` is untyped in Swift). */
    val actors: List<Actor> get() = paging.items
}

sealed interface ActorsAction {
    /** Triggered when the last loaded item becomes visible; also retries after a failed page. */
    data object OnLoadNextPage : ActorsAction
    data class OnActorClick(val actor: Actor) : ActorsAction
}

sealed interface ActorsEvent {
    /** Carries what the details screen shows right away, before its own request finishes. */
    data class NavigateToDetails(val actorId: Int, val name: String, val profilePath: String?) : ActorsEvent
}
