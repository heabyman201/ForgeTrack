@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.forgecompose.workouttracker.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-element transition plumbing for ForgeTrack.
 *
 * Rather than thread [SharedTransitionScope] and [AnimatedVisibilityScope] through
 * every screen signature, we publish them via CompositionLocals: the NavHost is
 * wrapped in a single [androidx.compose.animation.SharedTransitionLayout], and each
 * relevant destination provides its own [AnimatedVisibilityScope]. Any composable can
 * then opt a node into the morph with [forgeSharedBounds] — when either scope is
 * absent the modifier is a no-op, so it's always safe to call.
 */

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Spring morph for shared bounds — matches the one motion physics (never linear). */
private val ForgeMorphBounds = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Tag a node as one end of a shared-element morph keyed by [key]. The matching node on
 * the other screen (same key) morphs into this one with a spring, instead of a hard cut.
 */
@Composable
fun Modifier.forgeSharedBounds(key: Any): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedScope.current ?: return this
    return with(sharedScope) {
        this@forgeSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = ForgeMorphBounds,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        )
    }
}
