package com.forgecompose.workouttracker.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forgecompose.workouttracker.ColorSchemeAppTheme
import com.forgecompose.workouttracker.ui.theme.ForgeShape
import com.forgecompose.workouttracker.ui.theme.darkenedBy
import com.forgecompose.workouttracker.ui.theme.forgeHairline
import com.forgecompose.workouttracker.ui.theme.forgeTopHighlight
import com.forgecompose.workouttracker.ui.theme.lightenedBy

/* ───────────────────────── Backdrop blur ─────────────────────────────────── *
 *
 * Real frosted glass needs the content BEHIND the card, not the card's own fill —
 * blurring a smooth gradient changes nothing. So a screen marks its animated
 * background as the backdrop "source": it records itself into a [GraphicsLayer]
 * once per frame. Every [forgeFrostedSurface] then samples that layer at its own
 * on-screen position and draws it back, blurred and clipped to the card — true
 * see-through frost over the waves/orbs.
 */

/** Shared handle between the backdrop source and the frosted cards that sample it. */
class ForgeBackdrop internal constructor() {
    /** The recorded backdrop content. Null until the source has drawn once. */
    var layer: GraphicsLayer? by mutableStateOf(null)
        internal set

    /** Where the backdrop source sits, so cards can offset into it. */
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
        internal set
}

/** Provided down the tree by a screen that hosts a [forgeBackdropSource]. */
val LocalForgeBackdrop = staticCompositionLocalOf<ForgeBackdrop?> { null }

@Composable
fun rememberForgeBackdrop(): ForgeBackdrop = remember { ForgeBackdrop() }

/**
 * Marks this composable's drawing as the blur source for every [forgeFrostedSurface]
 * beneath [LocalForgeBackdrop]. Put it on the animated background that should show
 * through the cards. The content still draws normally (unblurred) — the blur only
 * happens where cards sample it.
 *
 *     val backdrop = rememberForgeBackdrop()
 *     CompositionLocalProvider(LocalForgeBackdrop provides backdrop) {
 *         AnimatedBackdrop(Modifier.matchParentSize().forgeBackdropSource(backdrop))
 *         // ...cards using ForgeCard / forgeFrostedSurface...
 *     }
 */
@Composable
fun Modifier.forgeBackdropSource(backdrop: ForgeBackdrop): Modifier {
    val layer = rememberGraphicsLayer()
    SideEffect { backdrop.layer = layer }
    return this
        .onGloballyPositioned { backdrop.coordinates = it }
        .drawWithContent {
            // Record the backdrop into the shared layer, then draw it to screen as-is.
            layer.record { this@drawWithContent.drawContent() }
            drawContent()
        }
}

/* ───────────────────────── Frosted surface ───────────────────────────────── */

/**
 * ForgeTrack frosted-glass surface — the single reusable "blur method".
 *
 * When a [LocalForgeBackdrop] is present it samples that backdrop and paints it
 * **blurred** behind a translucent, theme-tinted glass. With no backdrop it falls
 * back to an opaque themed surface (a soft bloom of the theme's secondary/tertiary)
 * so cards everywhere still look right. Crisp content drawn on top stays sharp —
 * pair with [forgeFrostedEdge] for the hairline + catch-light.
 *
 * @param theme       active colour scheme; every colour derives from it.
 * @param shape       clip shape (defaults to the shared card radius).
 * @param blurRadius  backdrop blur strength. 0.dp disables the blur.
 * @param glow        when true, emits a faint primary-colour bloom from the top-left.
 */
@Composable
fun Modifier.forgeFrostedSurface(
    theme: ColorSchemeAppTheme,
    shape: Shape = RoundedCornerShape(ForgeShape.card),
    blurRadius: Dp = 30.dp,
    glow: Boolean = false,
): Modifier {
    val backdrop = LocalForgeBackdrop.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val bg = theme.background
    val secondary = theme.secondary
    val tertiary = theme.tertiary
    val accent = theme.primary

    // One light source, top-left: lighter at the origin, darker toward bottom-right —
    // tinted with the theme's own secondary/tertiary so cards carry the theme.
    val light = secondary.lightenedBy(0.12f)
    val base = bg.lightenedBy(0.04f)
    val dark = tertiary.darkenedBy(0.12f)

    return this
        .clip(shape)
        .then(
            if (backdrop != null) Modifier.onGloballyPositioned { coordinates = it }
            else Modifier
        )
        .drawWithCache {
            val r = ForgeShape.card.toPx()
            val blurPx = blurRadius.toPx()

            // Opaque fill (used when there's no backdrop to blur).
            val opaqueFill = Brush.linearGradient(
                colors = listOf(light, base, dark),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            // Translucent glass tint (used over the blurred backdrop). Lighter than
            // before so more of the blurred backdrop reads through — cleaner, airier glass.
            val glassFill = Brush.linearGradient(
                colors = listOf(
                    light.copy(alpha = 0.26f),
                    base.copy(alpha = 0.34f),
                    dark.copy(alpha = 0.40f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            // Soft, wide bloom of secondary from the top-left light source.
            val frost = Brush.radialGradient(
                colors = listOf(
                    secondary.lightenedBy(0.10f).copy(alpha = 0.20f),
                    secondary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.15f, size.height * 0.08f),
                radius = size.maxDimension * 1.15f
            )
            // A gentle wash of tertiary from the opposite corner for depth — kept subtle
            // so the card never muddies up toward the bottom-right.
            val depth = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height),
                radius = size.maxDimension * 0.9f
            )

            onDrawBehind {
                val layer = backdrop?.layer
                val source = backdrop?.coordinates
                val me = coordinates

                if (layer != null && source != null && me != null) {
                    // Blur the recorded backdrop and stamp the region behind this card.
                    layer.renderEffect =
                        if (blurPx > 0f) BlurEffect(blurPx, blurPx, TileMode.Decal) else null
                    val offset = source.localPositionOf(me, Offset.Zero)
                    translate(left = -offset.x, top = -offset.y) {
                        drawLayer(layer)
                    }
                    // Theme-tinted glass over the blur so the card still reads as the theme.
                    drawRoundRect(brush = glassFill, cornerRadius = CornerRadius(r))
                } else {
                    drawRoundRect(brush = opaqueFill, cornerRadius = CornerRadius(r))
                }

                drawRoundRect(brush = depth, cornerRadius = CornerRadius(r))
                drawRoundRect(brush = frost, cornerRadius = CornerRadius(r))
                if (glow) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.18f, 0f),
                            radius = size.maxDimension * 0.95f
                        ),
                        cornerRadius = CornerRadius(r)
                    )
                }
            }
        }
}

/**
 * The crisp edge treatment that sits on TOP of [forgeFrostedSurface] — a faint
 * catch-light along the top edge and a 1dp hairline border. Kept separate so it
 * never gets caught in the surface blur and the card outline stays sharp.
 */
fun Modifier.forgeFrostedEdge(theme: ColorSchemeAppTheme): Modifier {
    val bg = theme.background
    val hairline = forgeHairline(bg)
    val topHighlight = forgeTopHighlight(bg)
    return this.drawWithCache {
        val r = ForgeShape.card.toPx()
        val hairlineWidth = 1.dp.toPx()
        onDrawBehind {
            // Catch-light along the top edge.
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, topHighlight, Color.Transparent)
                ),
                start = Offset(r * 0.55f, hairlineWidth),
                end = Offset(size.width - r * 0.55f, hairlineWidth),
                strokeWidth = hairlineWidth
            )
            // Crisp hairline border.
            drawRoundRect(
                color = hairline,
                style = Stroke(width = hairlineWidth),
                cornerRadius = CornerRadius(r)
            )
        }
    }
}
