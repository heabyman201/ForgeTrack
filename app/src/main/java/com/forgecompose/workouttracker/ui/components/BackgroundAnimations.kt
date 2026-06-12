package com.forgecompose.workouttracker.ui.components

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import java.time.LocalTime
import kotlin.random.Random

@Composable
fun AnimatedBackdrop(
    modifier: Modifier = Modifier,
    introBrush: Brush,
    introAlpha: Float,
    enableWaves: Boolean,
    enableAnimation: Boolean,
    showSmallOrbs: Boolean = false,
    slowCycleMinutes: Float = 8f
) {
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager
        .flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

    val theme = appearanceOptions.colors
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 42) else emptyList()
    }
    val smallOrbs = remember(showSmallOrbs) {
        if (showSmallOrbs) generateSmallBackdropOrbs(count = 5) else emptyList()
    }
    val grain = remember(performanceOptions.navEffects) {
        if (performanceOptions.navEffects) generateGrain(count = 150) else emptyList()
    }
    val currentHour = remember { LocalTime.now().hour }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height


        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    theme.background.copy(alpha = 1.0f),
                    theme.background.copy(alpha = 0.96f),
                    theme.secondary.copy(alpha = 0.18f).compositeOver(theme.background)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Diagonal sheen for a directional sense of light across the flat base
        drawDirectionalSheen(w = w, h = h, theme = theme)

        drawTimeBasedSkyOverlay(hour = currentHour, w = w, h = h)

        // Asymmetric corner depth behind the main orbs
        drawCornerGlows(w = w, h = h, theme = theme)

        drawBackdropOrbs(
            w = w,
            h = h,
            baseColor = theme.primary,
            accentColor = theme.secondary,
            highlightColor = theme.tertiary
        )

        // Soft mid-ground aurora band drifting across the middle
        drawAuroraBand(w = w, h = h, theme = theme)

        drawSmallBackdropOrbs(
            orbs = smallOrbs,
            w = w,
            h = h,
            theme = theme
        )

        drawNightStars(
            stars = nightStars,
            w = w,
            h = h,
            topBias = 0.58f
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    theme.primary.copy(alpha = 0.06f)
                ),
                startY = h * 0.35f,
                endY = h
            )
        )

        // Fine grain to dither the gradients and kill colour banding
        drawGrainOverlay(grain = grain, w = w, h = h)

        // 5. Very subtle Vignette to keep focus in the center
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                center = center,
                radius = max(w, h)
            )
        )

        // Intro layer
        if (introAlpha > 0f && performanceOptions.navEffects) {
            drawRect(introBrush, alpha = introAlpha.coerceIn(0f, 1f))
        }
    }
}
@Composable
fun AnimatedBackdropBlue(
    modifier: Modifier = Modifier,
    introBrush: Brush,
    introAlpha: Float,
    enableWaves: Boolean,
    enableAnimation: Boolean,
    slowCycleMinutes: Float = 8f
) {
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

    val theme = appearanceOptions.colors
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 56) else emptyList()
    }
    val grain = remember(performanceOptions.navEffects) {
        if (performanceOptions.navEffects) generateGrain(count = 150) else emptyList()
    }

    val darkBlueBase = Color(0xFF094F6E)
    val darkTealBase = Color(0xFF061418)
    val darkBlackBase = Color(0xFF020506)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height


        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    darkBlackBase,
                    darkTealBase,
                    darkBlueBase.copy(alpha = 0.92f)
                )
            )
        )

        drawDirectionalSheen(w = w, h = h, theme = theme)

        drawCornerGlows(w = w, h = h, theme = theme)

        drawBackdropOrbs(
            w = w,
            h = h,
            baseColor = theme.primary.compositeOver(darkBlueBase),
            accentColor = theme.secondary.compositeOver(darkTealBase),
            highlightColor = theme.tertiary.compositeOver(darkBlueBase)
        )

        drawAuroraBand(w = w, h = h, theme = theme)

        drawNightStars(
            stars = nightStars,
            w = w,
            h = h,
            topBias = 0.52f
        )

        drawGrainOverlay(grain = grain, w = w, h = h)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.White.copy(alpha = 0.07f)
                ),
                startY = h * 0.45f,
                endY = h
            )
        )


        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                center = center,
                radius = max(w, h)
            )
        )

        // Intro layer
        if (introAlpha > 0f && performanceOptions.navEffects) {
            drawRect(introBrush, alpha = introAlpha.coerceIn(0f, 1f))
        }
    }
}

private data class NightStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

private data class SmallBackdropOrb(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

private data class GrainDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val color: Color
)

private fun isNightTime(): Boolean {
    val hour = LocalTime.now().hour
    return hour >= 19 || hour < 6
}

private fun generateNightStars(count: Int): List<NightStar> {
    val random = Random(LocalTime.now().toSecondOfDay())
    return List(count) {
        val x = random.nextFloat()
        val y = (random.nextFloat() * random.nextFloat()).coerceAtMost(1f)
        val biasedY = (y * 0.58f).coerceIn(0f, 1f)
        NightStar(
            x = x,
            y = biasedY,
            radius = 0.75f + random.nextFloat() * 1.8f,
            alpha = 0.35f + random.nextFloat() * 0.5f
        )
    }
}

private fun generateSmallBackdropOrbs(count: Int): List<SmallBackdropOrb> {
    val random = Random(LocalTime.now().toSecondOfDay() * 31L)
    return List(count) {
        val x = random.nextFloat()
        val y = random.nextFloat()
        SmallBackdropOrb(
            x = x,
            y = y,
            radius = 100f + random.nextFloat() * 160f,
            alpha = 0.06f + random.nextFloat() * 0.10f
        )
    }
}

private fun generateGrain(count: Int): List<GrainDot> {
    val random = Random(LocalTime.now().toSecondOfDay() * 17L + 7L)
    return List(count) {
        // Mix of cool-white and warm specks so the texture isn't monochrome.
        val warm = random.nextFloat() < 0.25f
        GrainDot(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = 0.5f + random.nextFloat() * 0.9f,
            alpha = 0.015f + random.nextFloat() * 0.04f,
            color = if (warm) Color(0xFFFFE7C2) else Color(0xFFE8F1FF)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmallBackdropOrbs(
    orbs: List<SmallBackdropOrb>,
    w: Float,
    h: Float,
    theme: ColorSchemeAppTheme
) {
    if (orbs.isEmpty()) return

    orbs.forEachIndexed { index, orb ->
        val tint = when (index % 4) {
            0 -> theme.primary
            1 -> theme.secondary
            2 -> theme.tertiary
            else -> theme.background
        }
        val center = Offset(orb.x * w, orb.y * h)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = orb.alpha),
                    tint.copy(alpha = orb.alpha * 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = orb.radius
            ),
            center = center,
            radius = orb.radius
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNightStars(
    stars: List<NightStar>,
    w: Float,
    h: Float,
    topBias: Float
) {
    if (stars.isEmpty()) return

    stars.forEachIndexed { index, star ->
        val x = star.x * w
        val y = (star.y * h * topBias).coerceAtMost(h * 0.72f)
        val starColor = if (index % 7 == 0) {
            Color(0xFFEAF4FF)
        } else {
            Color.White
        }

        drawCircle(
            color = starColor.copy(alpha = star.alpha),
            radius = star.radius,
            center = Offset(x, y)
        )

        if (star.radius > 1.6f) {
            drawCircle(
                color = starColor.copy(alpha = star.alpha * 0.18f),
                radius = star.radius * 2.6f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Diagonal linear sheen that gives the flat base a sense of light direction.
 * Primary bleeds in from the top-left, tertiary anchors the bottom-right.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDirectionalSheen(
    w: Float,
    h: Float,
    theme: ColorSchemeAppTheme
) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                theme.primary.copy(alpha = 0.06f),
                Color.Transparent,
                Color.Transparent,
                theme.tertiary.copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )
}

/**
 * Two opposing corner glows that break the symmetry of the centered orbs and
 * add depth in the corners the radial vignette would otherwise flatten.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerGlows(
    w: Float,
    h: Float,
    theme: ColorSchemeAppTheme
) {
    val topLeft = Offset(w * 0.05f, h * 0.02f)
    val topLeftRadius = max(w, h) * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primary.copy(alpha = 0.12f),
                theme.primary.copy(alpha = 0.04f),
                Color.Transparent
            ),
            center = topLeft,
            radius = topLeftRadius
        ),
        center = topLeft,
        radius = topLeftRadius
    )

    val bottomRight = Offset(w * 0.96f, h * 0.94f)
    val bottomRightRadius = max(w, h) * 0.82f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.tertiary.copy(alpha = 0.16f),
                theme.tertiary.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = bottomRight,
            radius = bottomRightRadius
        ),
        center = bottomRight,
        radius = bottomRightRadius
    )
}

/**
 * A soft, slightly angled colour band drifting across the middle of the screen,
 * like a faint aurora. Adds a mid-ground layer between the orbs and the stars.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAuroraBand(
    w: Float,
    h: Float,
    theme: ColorSchemeAppTheme
) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                theme.secondary.copy(alpha = 0.10f),
                theme.primary.copy(alpha = 0.07f),
                theme.secondary.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(0f, h * 0.30f),
            end = Offset(w, h * 0.72f)
        )
    )
}

/**
 * Scatters very faint dots across the whole surface. This dithers the large
 * gradients so they read as textured rather than flat, banded fills.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrainOverlay(
    grain: List<GrainDot>,
    w: Float,
    h: Float
) {
    if (grain.isEmpty()) return

    grain.forEach { dot ->
        drawCircle(
            color = dot.color.copy(alpha = dot.alpha),
            radius = dot.radius,
            center = Offset(dot.x * w, dot.y * h)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdropOrbs(
    w: Float,
    h: Float,
    baseColor: Color,
    accentColor: Color,
    highlightColor: Color
) {
    val orbs = listOf(
        Triple(Offset(w * -0.10f, h * 0.10f), w * 1.10f, baseColor.copy(alpha = 0.22f)),
        Triple(Offset(w * 0.90f, h * 0.85f), w * 1.05f, accentColor.copy(alpha = 0.20f)),
        Triple(Offset(w * 0.50f, h * 0.45f), w * 1.20f, highlightColor.copy(alpha = 0.18f))
    )

    orbs.forEach { (center, radius, tint) ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tint, Color.Transparent),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
    }
}
private enum class TimeOfDay { NIGHT, DAWN, DAY, SUNSET }

private fun getTimeOfDay(hour: Int): TimeOfDay = when {
    hour >= 19 || hour < 5 -> TimeOfDay.NIGHT
    hour < 8               -> TimeOfDay.DAWN
    hour < 17              -> TimeOfDay.DAY
    else                   -> TimeOfDay.SUNSET
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeBasedSkyOverlay(
    hour: Int,
    w: Float,
    h: Float
) {
    val topFade = h * 0.44f

    when (getTimeOfDay(hour)) {
        TimeOfDay.NIGHT -> {
            // Deep navy ceiling that fades into the scene
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF010210).copy(alpha = 0.80f),
                        Color(0xFF040820).copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = topFade
                )
            )
        }

        TimeOfDay.DAWN -> {
            // Warm orange-pink horizon bleeding up
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6835).copy(alpha = 0.36f),
                        Color(0xFFFFB09A).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = topFade
                )
            )
            // Rising-sun glow just below the top edge
            val sunCenter = Offset(w * 0.50f, h * 0.32f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE566).copy(alpha = 0.58f),
                        Color(0xFFFF9A6C).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = w * 0.40f
                ),
                center = sunCenter,
                radius = w * 0.40f
            )
        }

        TimeOfDay.DAY -> {
            // Pale golden wash at the top to evoke sunlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE566).copy(alpha = 0.20f),
                        Color(0xFFFFF5CC).copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = topFade
                )
            )
            // High, broad sun halo near the top centre
            val sunCenter = Offset(w * 0.50f, h * 0.03f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFEE88).copy(alpha = 0.30f),
                        Color(0xFFFFDD44).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = w * 0.65f
                ),
                center = sunCenter,
                radius = w * 0.65f
            )
        }

        TimeOfDay.SUNSET -> {
            // Purple-to-deep-magenta sky at the top
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E0D50).copy(alpha = 0.62f),
                        Color(0xFF8B1A5A).copy(alpha = 0.32f),
                        Color(0xFFFF5F35).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = topFade
                )
            )
            // Sun disc sitting on the lower edge of the top gradient
            val sunCenter = Offset(w * 0.50f, h * 0.27f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD060).copy(alpha = 0.72f),
                        Color(0xFFFF8C42).copy(alpha = 0.34f),
                        Color(0xFFFF3E6A).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = w * 0.46f
                ),
                center = sunCenter,
                radius = w * 0.46f
            )
        }
    }
}

sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}
