package com.enmanuelbergling.corntime.feature.tvshows.presentation.details

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ElevatedCard
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
import com.enmanuelbergling.corntime.core.designsystem.components.OnceLottieAnimation
import com.enmanuelbergling.corntime.core.designsystem.components.RatingStars
import com.enmanuelbergling.corntime.core.designsystem.components.common.ActorCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.ActorsRowPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.theme.Heart
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.core.domain.model.tv.Season
import com.enmanuelbergling.corntime.feature.tvshows.presentation.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object TvShowDetailsTags {
    const val FAVORITE = "tv_show_details_favorite"
    const val WATCHLIST = "tv_show_details_watchlist"
    const val FAVORITE_LOADING = "tv_show_details_favorite_loading"
    const val WATCHLIST_LOADING = "tv_show_details_watchlist_loading"
    const val CONTENT = "tv_show_details_content"
    const val SEASONS = "tv_show_details_seasons"
    const val SEE_ALL_SEASONS = "tv_show_details_see_all_seasons"
}

@Composable
fun TvShowDetailsRoot(
    tvShowId: Int,
    onSeeAllSeasons: () -> Unit,
    onSeason: (seasonNumber: Int) -> Unit,
    onActor: (actorId: Int, name: String, imagePath: String?) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<TvShowDetailsViewModel> { parametersOf(tvShowId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TvShowDetailsEvent.NavigateBack -> onBack()
            TvShowDetailsEvent.NavigateToSeasons -> onSeeAllSeasons()
            is TvShowDetailsEvent.NavigateToEpisodes -> onSeason(event.seasonNumber)
            is TvShowDetailsEvent.NavigateToActor -> onActor(event.actorId, event.name, event.profilePath)
        }
    }

    TvShowDetailsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TvShowDetailsScreen(
    state: TvShowDetailsState,
    onAction: (TvShowDetailsAction) -> Unit,
) {
    val details = state.details
    val errorMessage = state.error?.messageResource

    if (errorMessage != null && details != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(TvShowDetailsAction.OnDismissError) },
            message = stringResource(errorMessage),
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = { Text(text = stringResource(DesignR.string.details)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(TvShowDetailsAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
                actions = {
                    if (state.isFavoriteLoading) {
                        LoadingIndicator(
                            modifier = Modifier
                                .testTag(TvShowDetailsTags.FAVORITE_LOADING)
                                .padding(horizontal = MaterialTheme.dimen.small)
                                .size(MaterialTheme.dimen.veryLarge),
                            color = Heart,
                            polygons = List(2) { MaterialShapes.Triangle },
                        )
                    } else {
                        state.accountStates?.let {
                            IconButton(
                                onClick = { onAction(TvShowDetailsAction.OnFavoriteClick) },
                                modifier = Modifier.testTag(TvShowDetailsTags.FAVORITE),
                            ) {
                                if (it.favorite) {
                                    OnceLottieAnimation(
                                        resId = DesignR.raw.add_to_favorite,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(3f),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(DesignR.drawable.heart_outline),
                                        contentDescription = stringResource(DesignR.string.favorites),
                                    )
                                }
                            }
                        }
                    }

                    if (state.isWatchlistLoading) {
                        LoadingIndicator(
                            modifier = Modifier
                                .testTag(TvShowDetailsTags.WATCHLIST_LOADING)
                                .padding(horizontal = MaterialTheme.dimen.small)
                                .size(MaterialTheme.dimen.veryLarge),
                            polygons = List(2) { MaterialShapes.Pill },
                        )
                    } else {
                        state.accountStates?.let {
                            IconButton(
                                onClick = { onAction(TvShowDetailsAction.OnWatchlistClick) },
                                modifier = Modifier.testTag(TvShowDetailsTags.WATCHLIST),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (it.watchlist) DesignR.drawable.bookmark_solid
                                        else DesignR.drawable.bookmark_outline
                                    ),
                                    contentDescription = stringResource(DesignR.string.watchlist),
                                    tint = if (it.watchlist) MaterialTheme.colorScheme.primary
                                    else LocalContentColor.current,
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { paddingValues ->
        if (details == null) {
            if (state.isLoading) {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            } else if (errorMessage != null) {
                ErrorScreen(
                    errorMessage = stringResource(errorMessage),
                    onRetry = { onAction(TvShowDetailsAction.OnRetry) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier
                    .testTag(TvShowDetailsTags.CONTENT)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                detailsImage(backdropUrl = ImageUrls.BACKDROP + details.backdropPath.orEmpty())

                information(
                    title = details.name,
                    year = details.firstAirYear,
                    rating = details.voteAverage.toFloat().div(2),
                    genres = details.formattedGenres,
                    seasonCount = details.numberOfSeasons,
                )

                overview(details.overview)

                seasonsSection(
                    seasons = details.seasons,
                    onSeason = { onAction(TvShowDetailsAction.OnSeasonClick(it)) },
                    onSeeAll = { onAction(TvShowDetailsAction.OnSeeAllSeasons) },
                )

                persons(
                    title = DesignR.string.cast,
                    persons = state.cast.distinct(),
                    isLoading = state.isLoading && state.credits == null,
                    onPerson = { onAction(TvShowDetailsAction.OnPersonClick(it)) },
                )

                persons(
                    title = DesignR.string.crew,
                    persons = state.crew.distinct(),
                    isLoading = state.isLoading && state.credits == null,
                    onPerson = { onAction(TvShowDetailsAction.OnPersonClick(it)) },
                )
            }
        }
    }
}

private fun LazyListScope.persons(
    @StringRes title: Int,
    persons: List<PersonUi>,
    isLoading: Boolean = false,
    onPerson: (personId: Int) -> Unit,
) {
    if (persons.isEmpty() && !isLoading) return

    item {
        Column(Modifier.padding(all = MaterialTheme.dimen.small)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = MaterialTheme.dimen.small),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

            if (isLoading && persons.isEmpty()) {
                ActorsRowPlaceholder()
            } else {
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
    genres: String,
    seasonCount: Int,
) {
    item {
        Column(Modifier.padding(MaterialTheme.dimen.small)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimen.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$title ($year)",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )

                RatingStars(value = rating)
            }

            Text(
                text = "$genres - ${stringResource(R.string.season_count, seasonCount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private fun LazyListScope.detailsImage(backdropUrl: String?) {
    item {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            placeholder = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
            error = painterResource(id = DesignR.drawable.pop_corn_and_cinema_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth(),
        )
    }
}

private fun LazyListScope.seasonsSection(
    seasons: List<Season>,
    onSeason: (seasonNumber: Int) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (seasons.isEmpty()) return

    item {
        Column(Modifier.padding(all = MaterialTheme.dimen.small)) {
            Text(
                text = stringResource(R.string.seasons),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = MaterialTheme.dimen.small),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier.testTag(TvShowDetailsTags.SEASONS),
            ) {
                items(seasons, key = { it.id }) { season ->
                    SeasonCard(
                        season = season,
                        modifier = Modifier.width(80.dp),
                        onClick = { onSeason(season.seasonNumber) },
                    )
                }
                item {
                    SeeAllCard(
                        modifier = Modifier
                            .width(80.dp)
                            .testTag(TvShowDetailsTags.SEE_ALL_SEASONS),
                        onClick = onSeeAll,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonCard(
    season: Season,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier) {
        ElevatedCard(onClick = onClick, shape = MaterialTheme.shapes.extraLarge) {
            AsyncImage(
                model = ImageUrls.POSTER + season.posterPath.orEmpty(),
                contentDescription = "season poster",
                error = painterResource(id = DesignR.drawable.pop_corn_and_cinema_poster),
                placeholder = painterResource(id = DesignR.drawable.pop_corn_and_cinema_poster),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(.8f),
                contentScale = ContentScale.Crop,
            )
        }

        Text(
            text = season.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.episode_count, season.episodeCount),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeeAllCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ElevatedCard(onClick = onClick, shape = MaterialTheme.shapes.extraLarge) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(.8f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.see_all),
                )
            }
        }

        Text(
            text = stringResource(R.string.see_all),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
