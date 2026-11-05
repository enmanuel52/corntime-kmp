package com.enmanuelbergling.corntime.feature.movies.presentation.details

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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object MovieDetailsTags {
    const val FAVORITE = "movie_details_favorite"
    const val WATCHLIST = "movie_details_watchlist"
    const val FAVORITE_LOADING = "movie_details_favorite_loading"
    const val WATCHLIST_LOADING = "movie_details_watchlist_loading"
    const val CONTENT = "movie_details_content"
}

@Composable
fun MovieDetailsRoot(
    movieId: Int,
    onActor: (actorId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<MovieDetailsViewModel> { parametersOf(movieId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            MovieDetailsEvent.NavigateBack -> onBack()
            is MovieDetailsEvent.NavigateToActor -> onActor(event.actorId)
        }
    }

    MovieDetailsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MovieDetailsScreen(
    state: MovieDetailsState,
    onAction: (MovieDetailsAction) -> Unit,
) {
    val details = state.details
    val errorMessage = state.error?.messageResource

    if (errorMessage != null && details != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(MovieDetailsAction.OnDismissError) },
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
                    IconButton(onClick = { onAction(MovieDetailsAction.OnBack) }) {
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
                                .testTag(MovieDetailsTags.FAVORITE_LOADING)
                                .padding(horizontal = MaterialTheme.dimen.small)
                                .size(MaterialTheme.dimen.veryLarge),
                            color = Heart,
                            polygons = List(2) { MaterialShapes.Triangle },
                        )
                    } else {
                        state.accountStates?.let {
                            IconButton(
                                onClick = { onAction(MovieDetailsAction.OnFavoriteClick) },
                                modifier = Modifier.testTag(MovieDetailsTags.FAVORITE),
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
                                .testTag(MovieDetailsTags.WATCHLIST_LOADING)
                                .padding(horizontal = MaterialTheme.dimen.small)
                                .size(MaterialTheme.dimen.veryLarge),
                            polygons = List(2) { MaterialShapes.Pill },
                        )
                    } else {
                        state.accountStates?.let {
                            IconButton(
                                onClick = { onAction(MovieDetailsAction.OnWatchlistClick) },
                                modifier = Modifier.testTag(MovieDetailsTags.WATCHLIST),
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
                    onRetry = { onAction(MovieDetailsAction.OnRetry) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
                modifier = Modifier
                    .testTag(MovieDetailsTags.CONTENT)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                detailsImage(backdropUrl = ImageUrls.BACKDROP + details.backdropPath)

                information(
                    title = details.title,
                    year = details.releaseYear,
                    rating = null,
                    genres = details.formattedGenres,
                    duration = details.duration,
                )

                overview(details.overview)

                persons(
                    title = DesignR.string.cast,
                    persons = state.cast.distinct(),
                    isLoading = state.isLoading && state.credits == null,
                    onPerson = { onAction(MovieDetailsAction.OnPersonClick(it)) },
                )

                persons(
                    title = DesignR.string.crew,
                    persons = state.crew.distinct(),
                    isLoading = state.isLoading && state.credits == null,
                    onPerson = { onAction(MovieDetailsAction.OnPersonClick(it)) },
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
    item {
        var expanded by remember { mutableStateOf(false) }

        Column(Modifier.padding(all = MaterialTheme.dimen.small)) {
            Text(
                text = "Overview",
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
    rating: Float?,
    genres: String,
    duration: String,
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

                rating?.let { RatingStars(value = it) }
            }

            Text(
                text = "$genres - $duration",
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
