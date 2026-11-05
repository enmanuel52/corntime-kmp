package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingScreen
import com.enmanuelbergling.corntime.core.designsystem.components.RatingStars
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.tv.Episode
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object EpisodesTags {
    const val LIST = "episodes_list"
}

@Composable
fun EpisodesRoot(
    tvShowId: Int,
    seasonNumber: Int,
    onEpisode: (episodeNumber: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<EpisodesViewModel> { parametersOf(tvShowId, seasonNumber) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EpisodesEvent.NavigateBack -> onBack()
            is EpisodesEvent.NavigateToEpisodeDetails -> onEpisode(event.episodeNumber)
        }
    }

    EpisodesScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesScreen(
    state: EpisodesState,
    onAction: (EpisodesAction) -> Unit,
) {
    val errorMessage = state.error?.messageResource

    if (errorMessage != null && state.seasonDetails != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(EpisodesAction.OnDismissError) },
            message = stringResource(errorMessage),
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = { Text(text = state.seasonDetails?.name ?: stringResource(R.string.episodes)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(EpisodesAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        when {
            state.seasonDetails == null && errorMessage != null -> ErrorScreen(
                errorMessage = stringResource(errorMessage),
                onRetry = { onAction(EpisodesAction.OnRetry) },
                modifier = Modifier.padding(paddingValues),
            )

            state.seasonDetails == null && state.isLoading -> LoadingScreen(Modifier.padding(paddingValues))

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier
                    .testTag(EpisodesTags.LIST)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.dimen.small)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                items(state.episodes, key = { it.id }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        expanded = state.expandedEpisodeId == episode.id,
                        onLongClick = { onAction(EpisodesAction.OnEpisodeToggle(episode.id)) },
                        onOpen = { onAction(EpisodesAction.OnEpisodeClick(episode.episodeNumber)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRow(
    episode: Episode,
    expanded: Boolean,
    onLongClick: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .combinedClickable(onLongClick = onLongClick, onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(all = MaterialTheme.dimen.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageUrls.BACKDROP + episode.stillPath.orEmpty(),
                    contentDescription = "episode still",
                    placeholder = painterResource(DesignR.drawable.pop_corn_and_cinema_backdrop),
                    error = painterResource(DesignR.drawable.pop_corn_and_cinema_backdrop),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(70.dp)
                        .aspectRatio(1.5f)
                        .clip(CardDefaults.shape),
                )

                Spacer(Modifier.width(MaterialTheme.dimen.small))

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.verySmall),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "${episode.episodeNumber}. ${episode.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingStars(
                            value = episode.voteAverage.div(2).toFloat(),
                            size = 14.dp,
                            spaceBetween = 1.dp,
                            surfaceVariant = LocalContentColor.current,
                        )

                        Spacer(Modifier.width(MaterialTheme.dimen.small))

                        Text(
                            text = episode.duration,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Light,
                        )
                    }
                }
            }

            AnimatedVisibility(expanded && episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MaterialTheme.dimen.verySmall),
                )
            }
        }
    }
}
