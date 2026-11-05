package com.enmanuelbergling.corntime.feature.movies.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.RoundedUserImage
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.theme.Gold
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.feature.movies.presentation.R

object RateSheetTags {
    const val STAR = "rate_sheet_star_"
    const val SUBMIT = "rate_sheet_submit"
    const val DELETE = "rate_sheet_delete"
}

/** Body of the rating bottom sheet: five tappable stars (halves by tapping the same star twice) and submit. */
@Composable
fun RateSheetContent(
    user: UserDetails,
    onSubmit: (stars: Float) -> Unit,
    modifier: Modifier = Modifier,
    initialRating: Float? = null,
    onDelete: (() -> Unit)? = null,
) {
    var rating by remember { mutableFloatStateOf(initialRating ?: 5f) }
    var review by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.medium),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(DesignR.string.leave_a_review),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimen.small))
            Text(
                text = stringResource(DesignR.string.leave_a_review_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }

        InteractiveRatingBar(rating = rating, onRatingChange = { rating = it })

        // kept from Pop-Corns' sheet; TMDB has no endpoint to publish a written review, so the text is not sent
        TextField(
            value = review,
            onValueChange = { review = it },
            minLines = 3,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = CardDefaults.shape,
            placeholder = { Text(stringResource(DesignR.string.leave_a_review)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                RoundedUserImage(user.avatarPath, Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(MaterialTheme.dimen.mediumSmall))
                Text(text = user.name, style = MaterialTheme.typography.labelLarge)
            }

            Button(onClick = { onSubmit(rating) }, modifier = Modifier.testTag(RateSheetTags.SUBMIT)) {
                Text(text = stringResource(DesignR.string.accept))
            }
        }

        if (onDelete != null) {
            TextButton(onClick = onDelete, modifier = Modifier.testTag(RateSheetTags.DELETE)) {
                Text(stringResource(R.string.remove_rating), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun InteractiveRatingBar(rating: Float, onRatingChange: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small)) {
        repeat(5) { index ->
            val starIndex = index + 1
            Card(
                onClick = {
                    // tapping the selected star again takes half a star off
                    if (starIndex.toFloat() == rating) onRatingChange(rating - .5f)
                    else onRatingChange(starIndex.toFloat())
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${RateSheetTags.STAR}$starIndex"),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = MaterialTheme.dimen.small)
                            .size(32.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = MaterialTheme.dimen.small)
                            .size(32.dp)
                            .drawWithContent {
                                val right = if (starIndex.toFloat() <= rating) size.width
                                else size.width.times(rating - (starIndex - 1)).coerceAtLeast(0f)
                                clipRect(right = right) { this@drawWithContent.drawContent() }
                            },
                        tint = Gold,
                    )
                }
            }
        }
    }
}
