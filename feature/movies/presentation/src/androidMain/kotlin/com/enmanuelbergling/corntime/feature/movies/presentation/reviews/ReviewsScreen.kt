package com.enmanuelbergling.corntime.feature.movies.presentation.reviews

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.LinearLoading
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingDialog
import com.enmanuelbergling.corntime.core.designsystem.components.RatingStars
import com.enmanuelbergling.corntime.core.designsystem.components.RoundedUserImage
import com.enmanuelbergling.corntime.core.designsystem.core.OnLoadMore
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.error.toUiError
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.movies.presentation.R
import com.enmanuelbergling.corntime.feature.movies.presentation.components.RateSheetContent
import com.enmanuelbergling.corntime.feature.movies.presentation.components.RatingSummaryHeader
import com.enmanuelbergling.corntime.feature.movies.presentation.components.asText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object ReviewsTags {
    const val LIST = "reviews_list"
    const val REVIEW = "reviews_review_"
    const val EMPTY = "reviews_empty"
    const val RETRY = "reviews_retry"
    const val SHEET = "reviews_rate_sheet"
    const val LOADING = "reviews_rating_loading"
}

@Composable
fun ReviewsRoot(
    movieId: Int,
    averageRating: Double,
    totalReviews: Int,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<ReviewsViewModel> { parametersOf(movieId, averageRating, totalReviews) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ReviewsEvent.NavigateBack -> onBack()
        }
    }

    ReviewsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    state: ReviewsState,
    onAction: (ReviewsAction) -> Unit,
) {
    if (state.isRating) LoadingDialog()

    state.error?.let {
        DefaultErrorDialog(
            onDismissDialog = { onAction(ReviewsAction.OnDismissError) },
            message = stringResource(it.messageResource),
        )
    }

    val paging = state.paging
    val lazyListState = rememberLazyListState()
    lazyListState.OnLoadMore(itemCount = paging.items.size + 1, enabled = paging.error == null && !paging.endReached) {
        onAction(ReviewsAction.OnLoadNextPage)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(DesignR.string.rate_and_reviews)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(ReviewsAction.OnBack) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(DesignR.string.back_icon),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .testTag(ReviewsTags.LIST)
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            item {
                RatingSummaryHeader(
                    averageRating = state.averageRating,
                    totalReviews = state.totalReviews,
                    breakdown = state.breakdown,
                    rated = state.rated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = MaterialTheme.dimen.medium),
                    onRate = if (state.canRate) ({ onAction(ReviewsAction.OnToggleRateSheet) }) else null,
                )
            }

            items(paging.items, key = { it.id }) { review ->
                ReviewItem(
                    review = review,
                    isExpanded = state.expandedReviewId == review.id,
                    modifier = Modifier.testTag("${ReviewsTags.REVIEW}${review.id}"),
                ) { onAction(ReviewsAction.OnToggleReview(review.id)) }
            }

            if (paging.isLoading) item { LinearLoading() }

            if (paging.isEmpty) item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.dimen.large)
                        .testTag(ReviewsTags.EMPTY),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.no_reviews_yet)) }
            }

            paging.error?.let { error ->
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.dimen.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(error.toUiError().messageResource))
                        OutlinedButton(
                            onClick = { onAction(ReviewsAction.OnLoadNextPage) },
                            modifier = Modifier.testTag(ReviewsTags.RETRY),
                        ) { Text(stringResource(DesignR.string.retry)) }
                    }
                }
            }
        }
    }

    val user = state.user
    if (state.isRateSheetOpen && user != null) {
        ModalBottomSheet(onDismissRequest = { onAction(ReviewsAction.OnToggleRateSheet) }) {
            RateSheetContent(
                user = user,
                initialRating = state.rated,
                onSubmit = { onAction(ReviewsAction.OnSubmitRating(it)) },
                onDelete = state.rated?.let { { onAction(ReviewsAction.OnDeleteRating) } },
                modifier = Modifier
                    .testTag(ReviewsTags.SHEET)
                    .imePadding()
                    .padding(MaterialTheme.dimen.medium),
            )
        }
    }
}

@Composable
fun ReviewItem(
    review: Review,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimen.medium)
            .padding(top = MaterialTheme.dimen.small),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
        ) {
            RoundedUserImage(
                avatarPath = review.authorDetails.avatarPath,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimen.small))
            Text(
                text = review.author,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            review.rating?.let { RatingStars(value = it, size = 18.dp) }
        }

        Text(
            text = review.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            modifier = Modifier.animateContentSize(
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            ),
        )

        Text(
            text = review.elapsedSincePosted().asText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}
