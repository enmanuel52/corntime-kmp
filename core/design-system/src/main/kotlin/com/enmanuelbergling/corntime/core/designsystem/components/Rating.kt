package com.enmanuelbergling.corntime.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.enmanuelbergling.corntime.core.designsystem.theme.Gold

@Composable
fun RatingStars(
    value: Float,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    spaceBetween: Dp = 2.dp,
    surfaceVariant: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
            repeat(5) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = surfaceVariant,
                    modifier = Modifier.size(size)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
            repeat(5) { index ->
                val starIndex = index + 1
                if (starIndex.toFloat() <= value) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(size)
                    )
                } else if (starIndex.toFloat() < value + 1) {
                    val partialFactor = value - (starIndex - 1)
                    Box(modifier = Modifier.size(size)) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier
                                .size(size)
                                .clip(FractionalRectangleShape(partialFactor))
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(size))
                }
            }
        }
    }
}

/**
 * Interactive 5-star rating input with half-star support (values 0.5f..5f).
 * Tapping the left half of a star selects `index - 0.5f`, the right half selects `index`.
 */
@Composable
fun SelectableRatingStars(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    spaceBetween: Dp = 4.dp,
    surfaceVariant: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spaceBetween)) {
        repeat(5) { index ->
            val starIndex = index + 1
            val fillFraction = (value - index).coerceIn(0f, 1f)

            Box(modifier = Modifier.size(size)) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = surfaceVariant,
                    modifier = Modifier.size(size)
                )

                if (fillFraction > 0f) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier
                            .size(size)
                            .clip(FractionalRectangleShape(fillFraction))
                    )
                }

                Row(Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onValueChange(starIndex - 0.5f) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onValueChange(starIndex.toFloat()) }
                    )
                }
            }
        }
    }
}

private class FractionalRectangleShape(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(Rect(0f, 0f, size.width * fraction, size.height))
    }
}
