package com.forgecompose.workouttracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forgecompose.workouttracker.AppearanceOptionsAppTheme
import com.forgecompose.workouttracker.AppearanceOptionsManagerAppTheme
import com.forgecompose.workouttracker.ui.theme.ForgeMotion
import com.forgecompose.workouttracker.ui.theme.ForgeShape
import com.forgecompose.workouttracker.ui.theme.ForgeSpacing
import com.forgecompose.workouttracker.ui.theme.darkenedBy

/**
 * Elevation tiers for [ForgeCard]. Hierarchy is *felt* — the AI hero sits above the
 * list cards, which sit above flat inline tiles.
 */
enum class ForgeElevation(val dp: Dp) {
    Hero(20.dp),
    Standard(7.dp),
    Flat(0.dp)
}

/**
 * The one card used everywhere in ForgeTrack — AI hero, workout rows, Recent
 * Highlights, settings sections, dialogs. Every instance shares:
 *
 *  - 24dp corners ([ForgeShape.card]).
 *  - a subtle gradient fill flowing the SAME direction on every card:
 *    top-left lighter → bottom-right darker (one light source).
 *  - a 1dp hairline border (~10%) so edges stay crisp on any theme.
 *  - a faint lighter catch-light along the top edge.
 *  - spring press feedback: ripple + a slight ~0.98 depress.
 *
 * @param elevation hierarchy tier (Hero > Standard > Flat).
 * @param glow when true, the card emits a faint crimson light (CTAs / the AI hero).
 * @param onClick optional — supplies ripple + press depress when set.
 */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    elevation: ForgeElevation = ForgeElevation.Standard,
    glow: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(ForgeSpacing.md),
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val appearance by AppearanceOptionsManagerAppTheme.flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearance.colors

    val shape = remember { RoundedCornerShape(ForgeShape.card) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = ForgeMotion.smooth(),
        label = "forgeCardScale"
    )

    val bg = theme.background
    val accent = theme.primary
    // Theme-tinted shadow instead of pure black — picks up the theme on every cast.
    val shadowTint = bg.darkenedBy(0.87f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                clip = false,
                // Glowing cards emit crimson; ordinary cards cast a soft, diffuse
                // theme-tinted shadow — low-alpha so it reads as depth, not a dark halo.
                ambientColor = if (glow) accent.copy(alpha = 0.55f) else shadowTint.copy(alpha = 0.32f),
                spotColor = if (glow) accent.copy(alpha = 0.45f) else shadowTint.copy(alpha = 0.40f)
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(color = accent),
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        // 1. Blurred, theme-tinted frosted surface (the reusable "blur method").
        Box(
            Modifier
                .matchParentSize()
                .forgeFrostedSurface(theme = theme, shape = shape, glow = glow)
        )
        // 2. Crisp edge details on top — never caught in the surface blur.
        Box(
            Modifier
                .matchParentSize()
                .forgeFrostedEdge(theme = theme)
        )
        // 3. Content defines the card's size; the two layers above match it.
        Box(Modifier.padding(contentPadding), content = content)
    }
}
