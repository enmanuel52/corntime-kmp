@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.enmanuelbergling.corntime.feature.tvshows.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import com.enmanuelbergling.corntime.core.domain.model.TvShowsSection
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import com.enmanuelbergling.corntime.feature.tvshows.presentation.details.TvShowDetailsRoot
import com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails.EpisodeDetailsRoot
import com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes.EpisodesRoot
import com.enmanuelbergling.corntime.feature.tvshows.presentation.home.TvShowsRoot
import com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons.SeasonsRoot
import com.enmanuelbergling.corntime.feature.tvshows.presentation.section.TvShowSectionRoot
import kotlinx.serialization.Serializable

@Serializable
data object TvShowsHomeKey : NavKey

@Serializable
data class TvShowSectionKey(val section: TvShowsSection) : NavKey

@Serializable
data class TvShowDetailsKey(val tvShowId: Int) : NavKey

@Serializable
data class SeasonsKey(val tvShowId: Int) : NavKey

@Serializable
data class EpisodesKey(val tvShowId: Int, val seasonNumber: Int) : NavKey

@Serializable
data class EpisodeDetailsKey(val tvShowId: Int, val seasonNumber: Int, val episodeNumber: Int) : NavKey

/**
 * Scene strategy of the seasons -> episodes -> episode flow: on wide windows the seasons and episodes
 * lists sit beside the episode details, on phones every screen is a normal stack entry. The app hands it
 * to its `NavDisplay`.
 */
@Composable
fun rememberTvShowsSceneStrategy(): SceneStrategy<NavKey> = rememberListDetailSceneStrategy<NavKey>()

/**
 * Destinations of the TV shows feature. Cross-feature destinations (actor, drawer, favorites) are
 * callbacks so the feature never depends on another one; the app wires them.
 *
 * The seasons, episodes and episode details entries carry list/detail pane metadata (see
 * [rememberTvShowsSceneStrategy]), and [backStack] lets that flow replace its own panes: choosing another
 * season swaps the episodes pane instead of stacking one more.
 */
fun EntryProviderScope<NavKey>.tvShowsEntries(
    backStack: MutableList<NavKey>,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onActor: (actorId: Int, name: String, imagePath: String?) -> Unit = { _, _, _ -> },
    onOpenDrawer: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
) {
    entry<TvShowsHomeKey> {
        TvShowsRoot(
            onDetails = { onNavigate(TvShowDetailsKey(it)) },
            onSection = { onNavigate(TvShowSectionKey(it)) },
            onOpenDrawer = onOpenDrawer,
            onFavorites = onFavorites,
        )
    }

    entry<TvShowSectionKey> { key ->
        TvShowSectionRoot(
            section = key.section,
            onTvShow = { onNavigate(TvShowDetailsKey(it)) },
            onBack = onBack,
        )
    }

    entry<TvShowDetailsKey> { key ->
        TvShowDetailsRoot(
            tvShowId = key.tvShowId,
            onSeeAllSeasons = { onNavigate(SeasonsKey(key.tvShowId)) },
            onSeason = { seasonNumber ->
                onNavigate(SeasonsKey(key.tvShowId))
                onNavigate(EpisodesKey(key.tvShowId, seasonNumber))
            },
            onActor = onActor,
            onBack = onBack,
        )
    }

    entry<SeasonsKey>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                // read while composing, so it follows the stack
                val hasEpisodes = backStack.any { it is EpisodesKey }
                PanePlaceholder(
                    stringResource(if (hasEpisodes) R.string.episode_details else R.string.episodes),
                )
            },
        ),
    ) { key ->
        SeasonsRoot(
            tvShowId = key.tvShowId,
            onSeason = { seasonNumber ->
                backStack.removeAll { it is EpisodesKey || it is EpisodeDetailsKey }
                onNavigate(EpisodesKey(key.tvShowId, seasonNumber))
            },
            onBack = onBack,
        )
    }

    entry<EpisodesKey>(metadata = ListDetailSceneStrategy.extraPane()) { key ->
        EpisodesRoot(
            tvShowId = key.tvShowId,
            seasonNumber = key.seasonNumber,
            onEpisode = { episodeNumber ->
                backStack.removeAll { it is EpisodeDetailsKey }
                onNavigate(EpisodeDetailsKey(key.tvShowId, key.seasonNumber, episodeNumber))
            },
            // back from the episodes list returns to the seasons, dropping whatever sat beside it
            onBack = { backStack.removeAll { it is EpisodesKey || it is EpisodeDetailsKey } },
        )
    }

    entry<EpisodeDetailsKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        EpisodeDetailsRoot(
            tvShowId = key.tvShowId,
            seasonNumber = key.seasonNumber,
            episodeNumber = key.episodeNumber,
            onActor = onActor,
            onBack = onBack,
        )
    }
}

@Composable
private fun PanePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
