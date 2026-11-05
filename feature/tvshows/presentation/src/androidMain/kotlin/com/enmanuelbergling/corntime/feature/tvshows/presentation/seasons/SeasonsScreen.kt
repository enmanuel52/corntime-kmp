package com.enmanuelbergling.corntime.feature.tvshows.presentation.seasons

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.tv.Season
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object SeasonsTags {
    const val LIST = "seasons_list"
}

@Composable
fun SeasonsRoot(
    tvShowId: Int,
    onSeason: (seasonNumber: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<SeasonsViewModel> { parametersOf(tvShowId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SeasonsEvent.NavigateBack -> onBack()
            is SeasonsEvent.NavigateToEpisodes -> onSeason(event.seasonNumber)
        }
    }

    SeasonsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonsScreen(
    state: SeasonsState,
    onAction: (SeasonsAction) -> Unit,
) {
    val errorMessage = state.error?.messageResource

    if (errorMessage != null && state.details != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(SeasonsAction.OnDismissError) },
            message = stringResource(errorMessage),
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = { Text(text = state.details?.name ?: stringResource(R.string.seasons)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SeasonsAction.OnBack) }) {
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
            state.details == null && errorMessage != null -> ErrorScreen(
                errorMessage = stringResource(errorMessage),
                onRetry = { onAction(SeasonsAction.OnRetry) },
                modifier = Modifier.padding(paddingValues),
            )

            state.details == null && state.isLoading -> LoadingScreen(Modifier.padding(paddingValues))

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier
                    .testTag(SeasonsTags.LIST)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.dimen.small)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                items(state.seasons, key = { it.id }) { season ->
                    SeasonRow(
                        season = season,
                        expanded = state.expandedSeasonId == season.id,
                        onLongClick = { onAction(SeasonsAction.OnSeasonToggle(season.id)) },
                        onOpen = { onAction(SeasonsAction.OnSeasonClick(season.seasonNumber)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeasonRow(
    season: Season,
    expanded: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .combinedClickable(onLongClick = onLongClick, onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimen.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageUrls.POSTER + season.posterPath.orEmpty(),
                contentDescription = "season poster",
                placeholder = painterResource(DesignR.drawable.pop_corn_and_cinema_poster),
                error = painterResource(DesignR.drawable.pop_corn_and_cinema_poster),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.dimen.small,
                        vertical = MaterialTheme.dimen.verySmall,
                    )
                    .height(90.dp)
                    .aspectRatio(0.65f)
                    .clip(CardDefaults.elevatedShape),
            )

            Spacer(Modifier.width(MaterialTheme.dimen.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.episode_count, season.episodeCount),
                    style = MaterialTheme.typography.bodySmall,
                )

                AnimatedVisibility(expanded && season.overview.isNotBlank()) {
                    Text(
                        text = season.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = MaterialTheme.dimen.small),
                    )
                }
            }
        }
    }
}
