package com.enmanuelbergling.corntime.feature.actor.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.domain.model.actor.ActorDetails
import com.enmanuelbergling.corntime.core.domain.model.actor.KnownMovie
import com.enmanuelbergling.corntime.core.designsystem.R
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.ErrorScreen
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingScreen
import com.enmanuelbergling.corntime.core.designsystem.components.RatingStars
import com.enmanuelbergling.corntime.core.designsystem.components.UserImage
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCard
import com.enmanuelbergling.corntime.core.designsystem.components.common.MovieCardPlaceholder
import com.enmanuelbergling.corntime.core.designsystem.core.sharedBoundsIfAvailable
import com.enmanuelbergling.corntime.core.designsystem.core.sharedElementIfAvailable
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.core.shimmerIf
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import com.valentinilk.shimmer.shimmer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActorDetailsRoot(
    id: Int,
    imagePath: String?,
    name: String,
    onMovie: (movieId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<ActorDetailsViewModel> { parametersOf(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ActorDetailsEvent.NavigateBack -> onBack()
            is ActorDetailsEvent.NavigateToMovie -> onMovie(event.movieId)
        }
    }

    ActorDetailsScreen(
        imagePath = imagePath,
        name = name,
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailsScreen(
    imagePath: String?,
    name: String,
    state: ActorDetailsState,
    onAction: (ActorDetailsAction) -> Unit,
) {

    if (state.error != null && state.details != null) {
        DefaultErrorDialog(
            onDismissDialog = { onAction(ActorDetailsAction.OnDismissError) },
            message = stringResource(state.error.messageResource)
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = { Text(text = stringResource(R.string.details)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onAction(ActorDetailsAction.OnBack) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = stringResource(R.string.back_icon)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    ) { paddingValues ->

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(120.dp),
            verticalItemSpacing = MaterialTheme.dimen.small,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
            modifier = Modifier
                .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                .fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                DetailsHeader(
                    avatarPath = imagePath,
                    name = name,
                    popularity = state.details?.popularity ?: 5.0
                )
            }


            if (state.details == null) item(span = StaggeredGridItemSpan.FullLine) {
                if (state.isLoading) {
                    LoadingScreen()
                } else if (state.error != null) {
                    ErrorScreen(
                        errorMessage = stringResource(state.error.messageResource),
                        onRetry = { onAction(ActorDetailsAction.OnRetry) },
                    )
                }
            } else {
                if (state.details.biography.isNotBlank()) about(biography = state.details.biography)

                knownMovies(
                    knownMovies = state.knownMovies,
                    onMovie = { onAction(ActorDetailsAction.OnMovieClick(it)) }
                )
            }
        }
    }
}

private fun LazyStaggeredGridScope.knownMovies(
    knownMovies: List<KnownMovie>,
    onMovie: (movieId: Int) -> Unit,
) {
    item(span = StaggeredGridItemSpan.FullLine) {
        Text(
            text = stringResource(com.enmanuelbergling.corntime.feature.actor.presentation.R.string.known_movies),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }

    items(knownMovies) { movie ->
        MovieCard(
            title = movie.title,
            imageUrl = movie.posterPath.orEmpty(),
            rating = movie.voteAverage
        ) { onMovie(movie.id) }
    }
    if (knownMovies.isEmpty()) {
        items(5) {
            MovieCardPlaceholder(
                modifier = Modifier
                    .shimmer()
            )
        }
    }
}

private fun LazyStaggeredGridScope.about(
    biography: String,
) {
    item(span = StaggeredGridItemSpan.FullLine) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.mediumSmall)
        ) {
            Text(
                text = stringResource(R.string.about),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var isBiographyExpanded by remember {
                mutableStateOf(false)
            }

            Text(
                text = biography,
                maxLines = if (isBiographyExpanded) Int.MAX_VALUE else 3,
                modifier = Modifier
                    .clickable { isBiographyExpanded = !isBiographyExpanded }
                    .animateContentSize(
                        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                    ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * @param popularity in percent max 100
 * */
@Composable
private fun DetailsHeader(
    avatarPath: String?,
    name: String,
    popularity: Double?,
) {
    Row(
        Modifier
            .heightIn(max = 180.dp)
            .fillMaxWidth()
    ) {
        UserImage(
            avatarPath,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxHeight()
                .padding(all = MaterialTheme.dimen.verySmall)
                .sharedElementIfAvailable(avatarPath.orEmpty())
                .clip(MaterialTheme.shapes.medium)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                MaterialTheme.dimen.mediumSmall,
                Alignment.CenterVertically
            ),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.sharedBoundsIfAvailable(name)
            )

            RatingStars(
                value = popularity?.div(100)?.times(5)?.toFloat() ?: 0f,
                size = 30.dp, spaceBetween = 3.dp,
                modifier = Modifier.shimmerIf { popularity == null }
            )
        }
    }
}

@Preview
@Composable
private fun ActorDetailsScreenPreview() {
    val sampleActorDetails = ActorDetails(
        adult = false,
        alsoKnownAs = listOf("Avicii"),
        biography = "Tim Bergling, known professionally as Avicii, was a Swedish DJ, remixer, record producer, musician, and songwriter. He is widely considered one of the most influential DJs and music producers of all time.",
        birthday = "1989-09-08",
        deathday = "2018-04-20",
        gender = 2,
        homepage = "http://avicii.com",
        id = 1,
        imdbId = "nm4812328",
        knownForDepartment = "Music",
        name = "Tim Bergling",
        placeOfBirth = "Stockholm, Sweden",
        popularity = 90.0,
        profilePath = "/tim_path.jpg"
    )

    val sampleKnownMovies = listOf(
        KnownMovie(id = 1, posterPath = "/path1.jpg", title = "Movie 1", voteAverage = 8.5),
        KnownMovie(id = 2, posterPath = "/path2.jpg", title = "Movie 2", voteAverage = 7.0),
        KnownMovie(id = 3, posterPath = "/path3.jpg", title = "Movie 3", voteAverage = 9.0),
    )

    val state = ActorDetailsState(
        actorId = 1,
        details = sampleActorDetails,
        loadedMovies = sampleKnownMovies,
        isLoading = false,
        error = null
    )

    CornTimeTheme {
        AnimatedVisibility(visible = true) {
            ActorDetailsScreen(
                imagePath = "/path.jpg",
                name = "Tim Bergling",
                state = state,
                onAction = {}
            )
        }
    }
}

@Preview
@Composable
private fun ActorDetailsLoadingPreview() {
    val state = ActorDetailsState(
        actorId = 1,
        details = null,
        loadedMovies = listOf(),
        isLoading = true,
        error = null
    )

    CornTimeTheme {
        AnimatedVisibility(visible = true) {
            ActorDetailsScreen(
                imagePath = "/path.jpg",
                name = "Tim Bergling",
                state = state,
                onAction = {}
            )
        }
    }
}

@Preview
@Composable
private fun ActorDetailsErrorPreview() {
    val state = ActorDetailsState(
        actorId = 1,
        details = null,
        loadedMovies = listOf(),
        isLoading = false,
        error = UiError.Server
    )

    CornTimeTheme {
        AnimatedVisibility(visible = true) {
            ActorDetailsScreen(
                imagePath = "/path.jpg",
                name = "Tim Bergling",
                state = state,
                onAction = {}
            )
        }
    }
}
