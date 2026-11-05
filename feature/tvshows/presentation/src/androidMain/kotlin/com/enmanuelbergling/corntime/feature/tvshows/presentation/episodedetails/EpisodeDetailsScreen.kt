package com.enmanuelbergling.corntime.feature.tvshows.presentation.episodedetails

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.enmanuelbergling.corntime.core.designsystem.components.common.ActorCard
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object EpisodeDetailsTags {
    const val CONTENT = "episode_details_content"
}

@Composable
fun EpisodeDetailsRoot(
    tvShowId: Int,
    seasonNumber: Int,
    episodeNumber: Int,
    onActor: (actorId: Int, name: String, imagePath: String?) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<EpisodeDetailsViewModel> {
        parametersOf(tvShowId, seasonNumber, episodeNumber)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EpisodeDetailsEvent.NavigateBack -> onBack()
            is EpisodeDetailsEvent.NavigateToActor -> onActor(event.actorId, event.name, event.profilePath)
        }
    }

    EpisodeDetailsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailsScreen(
    state: EpisodeDetailsState,
    onAction: (EpisodeDetailsAction) -> Unit,
) {
    val details = state.details
    val errorMessage = state.error?.messageResource

    if (errorMessage != null && details != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(EpisodeDetailsAction.OnDismissError) },
            message = stringResource(errorMessage),
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = { Text(text = stringResource(R.string.episode_details)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(EpisodeDetailsAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        if (details == null) {
            if (errorMessage != null) {
                ErrorScreen(
                    errorMessage = stringResource(errorMessage),
                    onRetry = { onAction(EpisodeDetailsAction.OnRetry) },
                    modifier = Modifier.padding(paddingValues),
                )
            } else if (state.isLoading) {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier
                    .testTag(EpisodeDetailsTags.CONTENT)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                stillImage(ImageUrls.BACKDROP + details.stillPath.orEmpty())

                information(
                    title = details.name,
                    year = details.airYear,
                    rating = details.voteAverage.toFloat().div(2),
                    duration = details.duration,
                )

                overview(details.overview)

                persons(
                    title = DesignR.string.cast,
                    persons = state.guestStars.distinct(),
                    onPerson = { onAction(EpisodeDetailsAction.OnPersonClick(it)) },
                )

                persons(
                    title = DesignR.string.crew,
                    persons = state.crew.distinct(),
                    onPerson = { onAction(EpisodeDetailsAction.OnPersonClick(it)) },
                )
            }
        }
    }
}

private fun LazyListScope.persons(
    @StringRes title: Int,
    persons: List<EpisodePersonUi>,
    onPerson: (personId: Int) -> Unit,
) {
    if (persons.isEmpty()) return

    item {
        Column(Modifier.padding(all = MaterialTheme.dimen.small)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = MaterialTheme.dimen.small),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small)) {
                items(persons) { person ->
                    ActorCard(
                        imageUrl = person.profilePath,
                        name = person.name,
                        modifier = Modifier.width(110.dp),
                    ) {
                        onPerson(person.id)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.overview(overview: String) {
    if (overview.isBlank()) return

    item {
        var expanded by remember { mutableStateOf(false) }

        Column(Modifier.padding(all = MaterialTheme.dimen.small)) {
            Text(
                text = stringResource(R.string.overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = overview,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .animateContentSize(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun LazyListScope.information(
    title: String,
    year: String,
    rating: Float,
    duration: String,
) {
    item {
        Column(Modifier.padding(MaterialTheme.dimen.small)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(MaterialTheme.dimen.small))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.duration_and_year, duration, year),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                )
                RatingStars(value = rating)
            }
        }
    }
}

private fun LazyListScope.stillImage(backdropUrl: String?) {
    item {
        AsyncImage(
            model = backdropUrl,
            contentDescription = stringResource(DesignR.string.poster_image),
            placeholder = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
            error = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth(),
        )
    }
}
