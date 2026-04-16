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

    val theme = appearanceOptions.selectedTheme.colors
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 42) else emptyList()
    }
    val smallOrbs = remember(showSmallOrbs) {
        if (showSmallOrbs) generateSmallBackdropOrbs(count = 5) else emptyList()
    }

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

        drawBackdropOrbs(
            w = w,
            h = h,
            baseColor = theme.primary,
            accentColor = theme.secondary,
            highlightColor = theme.tertiary
        )

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

    val theme = appearanceOptions.selectedTheme.colors
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 56) else emptyList()
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

        drawBackdropOrbs(
            w = w,
            h = h,
            baseColor = theme.primary.compositeOver(darkBlueBase),
            accentColor = theme.secondary.compositeOver(darkTealBase),
            highlightColor = theme.tertiary.compositeOver(darkBlueBase)
        )

        drawNightStars(
            stars = nightStars,
            w = w,
            h = h,
            topBias = 0.52f
        )

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
sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}
