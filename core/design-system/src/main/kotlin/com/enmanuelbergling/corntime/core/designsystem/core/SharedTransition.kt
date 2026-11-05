@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.enmanuelbergling.corntime.core.designsystem.core

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Provided by the app's navigation host; null in previews and screen tests. */
val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> = compositionLocalOf { null }

/** The destination's animated scope, provided per navigation entry; null in previews and screen tests. */
val LocalAnimatedContentScope: ProvidableCompositionLocal<AnimatedContentScope?> = compositionLocalOf { null }

val BoundsTransition = BoundsTransform { _, _ ->
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
}

/** Shared element between destinations when a transition scope is available, otherwise a no-op. */
@Composable
fun Modifier.sharedElementIfAvailable(key: Any): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalAnimatedContentScope.current ?: return this
    return with(sharedScope) {
        this@sharedElementIfAvailable.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = BoundsTransition,
        )
    }
}

/** Shared bounds between destinations when a transition scope is available, otherwise a no-op. */
@Composable
fun Modifier.sharedBoundsIfAvailable(key: Any): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalAnimatedContentScope.current ?: return this
    return with(sharedScope) {
        this@sharedBoundsIfAvailable.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = BoundsTransition,
        )
    }
}
