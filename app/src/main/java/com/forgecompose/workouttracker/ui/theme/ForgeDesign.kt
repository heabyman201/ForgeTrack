package com.forgecompose.workouttracker.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forgecompose.workouttracker.ColorSchemeAppTheme

/**
 * ForgeTrack design language — the single source of truth for the redesign.
 *
 * Every surface in the app should agree with every other one:
 *  - one spacing scale   ([ForgeSpacing])
 *  - one card language    (see ForgeCard, built on [ForgeShape] + the light-source helpers below)
 *  - one motion physics  ([ForgeMotion] — springs, never linear tweens)
 *  - one colour-meaning rule ([forgeValueColor], [ForgeAttention])
 *
 * It is theme-agnostic: every value derives from the active [ColorSchemeAppTheme]
 * (primary / secondary / tertiary / background) so all 34 themes — including the
 * light ones (Noir, Ink, OldPaper…) — stay crisp.
 */

/** One spacing scale, enforced between every section so the vertical rhythm feels deliberate. */
object ForgeSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}

/** Shared corner language. One radius for cards, fully-round for chips/pills. */
object ForgeShape {
    val card: Dp = 24.dp
    val chip: Dp = 999.dp
    /** Inner elements (bars, mini-tiles) sit one notch tighter than the card. */
    val inner: Dp = 14.dp
}

/**
 * One motion physics. Spring on everything — never a linear tween.
 *  - [smooth]  settle: presses, fades, layout.
 *  - [bouncy]  arrivals: card entrances, launch pops.
 *  - [snappy]  small, decisive moves: thumbs, toggles.
 */
object ForgeMotion {
    fun <T> smooth(): AnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    fun <T> bouncy(): AnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)

    fun <T> snappy(): AnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /** Delay between staggered card arrivals on load (30–50ms band). */
    const val StaggerStepMs: Int = 45
}

/** OpenType tabular figures, so digits (reps, weight, %) never jitter as they change. */
const val ForgeTabularFigures: String = "tnum"

/** Apply tabular figures to any [TextStyle] used for numbers. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = ForgeTabularFigures)

/**
 * The one colour-meaning rule:
 *  - crimson (theme.primary) = the primary value / brand.
 *  - orange ([ForgeAttention]) = high intensity / attention.
 * Apply everywhere without exception.
 */
val ForgeAttention: Color = Color(0xFFFF7A1A)

/**
 * Value→colour ramp for charts & gauges. Low values read as calm brand crimson,
 * high values climb toward orange attention — the ForgeTrack analogue of Samsung
 * Health's value-mapped stress timeline, but on our crimson identity (no blue/teal).
 *
 * @param fraction 0f..1f position along the scale.
 */
fun forgeValueColor(theme: ColorSchemeAppTheme, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    val low = theme.primary
    val mid = lerp(theme.primary, ForgeAttention, 0.55f)
    val high = ForgeAttention
    return if (f < 0.5f) lerp(low, mid, f / 0.5f) else lerp(mid, high, (f - 0.5f) / 0.5f)
}

/** Lighten a colour toward white (the single light source: top-left). */
fun Color.lightenedBy(t: Float): Color = lerp(this, Color.White, t.coerceIn(0f, 1f))

/** Darken a colour toward black (away from the light source: bottom-right). */
fun Color.darkenedBy(t: Float): Color = lerp(this, Color.Black, t.coerceIn(0f, 1f))

/** True when a background is light enough that white hairlines would vanish. */
fun Color.isLightSurface(): Boolean = luminance() > 0.6f

/**
 * 1dp hairline border at ~10% — white on dark themes, black on light ones,
 * so card edges stay crisp under every theme.
 */
fun forgeHairline(background: Color): Color =
    if (background.isLightSurface()) Color.Black.copy(alpha = 0.12f)
    else Color.White.copy(alpha = 0.10f)

/** The faint lighter line along the top edge of every card (the catch-light). */
fun forgeTopHighlight(background: Color): Color =
    if (background.isLightSurface()) Color.White.copy(alpha = 0.55f)
    else Color.White.copy(alpha = 0.16f)
