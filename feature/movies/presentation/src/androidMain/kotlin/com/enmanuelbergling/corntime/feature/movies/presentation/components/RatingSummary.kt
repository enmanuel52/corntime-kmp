package com.enmanuelbergling.corntime.feature.movies.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.RatingStars
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.theme.Gold
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBucket
import com.enmanuelbergling.corntime.feature.movies.presentation.R
import kotlin.math.roundToInt

object RatingSummaryTags {
    const val AVERAGE = "rating_summary_average"
    const val RATE = "rating_summary_rate"
    const val RATED = "rating_summary_rated"
}

/**
 * Average, vote count and the per-star breakdown. When the user can rate, a chip below either invites
 * them to rate ([rated] is null) or shows their rating; both open the rating sheet through [onRate].
 */
@Composable
fun RatingSummaryHeader(
    averageRating: Float,
    totalReviews: Int,
    breakdown: List<RatingBucket>,
    modifier: Modifier = Modifier,
    rated: Float? = null,
    onRate: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = averageRating.roundedToOneDecimal(),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.testTag(RatingSummaryTags.AVERAGE),
                )
                RatingStars(value = averageRating, size = 18.dp)
                Spacer(modifier = Modifier.height(MaterialTheme.dimen.small))
                Text(
                    text = pluralStringResource(R.plurals.reviews_count, totalReviews, totalReviews),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            HorizontalBarChart(breakdown, Modifier.weight(1.5f))
        }

        when {
            rated != null -> AssistChip(
                onClick = { onRate?.invoke() },
                label = { Text(stringResource(DesignR.string.movie_rated)) },
                leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null) },
                trailingIcon = { RatingStars(rated) },
                colors = AssistChipDefaults.assistChipColors(
                    leadingIconContentColor = Gold,
                    containerColor = Gold.copy(alpha = .3f),
                ),
                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Gold),
                modifier = Modifier.testTag(RatingSummaryTags.RATED),
            )

            onRate != null -> AssistChip(
                onClick = onRate,
                label = { Text(stringResource(DesignR.string.rate_this_movie)) },
                leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null) },
                modifier = Modifier.testTag(RatingSummaryTags.RATE),
            )
        }
    }
}

private fun Float.roundedToOneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Composable
private fun HorizontalBarChart(breakdown: List<RatingBucket>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.verySmall)) {
        breakdown.forEachIndexed { index, bucket ->
            val progress = remember { Animatable(0f) }
            LaunchedEffect(bucket.fraction) {
                progress.animateTo(bucket.fraction, tween(delayMillis = index * 150))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small),
            ) {
                Text(text = bucket.stars.toString(), style = MaterialTheme.typography.labelMedium)
                ProgressBar(progress.value, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimen.small - 2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(MaterialTheme.dimen.small)
                .background(Gold, CircleShape),
        )
    }
}
