package com.enmanuelbergling.corntime.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.enmanuelbergling.corntime.core.presentation.image.ImageUrls
import com.enmanuelbergling.corntime.core.designsystem.R
import com.enmanuelbergling.corntime.core.designsystem.theme.DimensionTokens

/**
 * Crete a [VectorPainter] with the given [ImageVector] and [Color]*/
@Composable
fun rememberVectorPainter(image: ImageVector, color: Color): VectorPainter =
    androidx.compose.ui.graphics.vector.rememberVectorPainter(
        defaultWidth = image.defaultWidth,
        defaultHeight = image.defaultHeight,
        viewportWidth = image.viewportWidth,
        viewportHeight = image.viewportHeight,
        name = image.name,
        tintColor = color,
        tintBlendMode = image.tintBlendMode,
        autoMirror = image.autoMirror,
        content = { _, _ -> RenderVectorGroup(group = image.root) }
    )

@Composable
fun UserImage(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = "${ImageUrls.ORIGINAL}$avatarPath",
        contentDescription = "user image",
        placeholder = painterResource(
            id = R.drawable.mr_bean
        ),
        error = painterResource(
            id = R.drawable.mr_bean
        ),
        modifier = modifier,
        contentScale = contentScale
    )
}
@Composable
fun RoundedUserImage(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = "${ImageUrls.ORIGINAL}$avatarPath",
        contentDescription = "user image",
        placeholder = painterResource(
            id = R.drawable.mr_bean
        ),
        error = painterResource(
            id = R.drawable.mr_bean
        ),
        modifier = modifier
            .size(DimensionTokens.ProfileImageSize)
            .clip(CircleShape),
        contentScale = contentScale
    )
}