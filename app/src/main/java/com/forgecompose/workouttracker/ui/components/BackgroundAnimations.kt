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

import android.graphics.RenderEffect
import android.graphics.RenderEffect.createBlurEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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
    val density = LocalDensity.current
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 42) else emptyList()
    }
    val smallOrbs = remember(showSmallOrbs) {
        if (showSmallOrbs) generateSmallBackdropOrbs(count = 18) else emptyList()
    }

    val blurRadiusPx = remember(performanceOptions.blurEnabled) {
        if (performanceOptions.blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            with(density) { 60.dp.toPx() } // Increased blur for smoother blending
        } else 0f
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (blurRadiusPx > 0f) {
                    renderEffect = RenderEffect
                        .createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.DECAL)
                        .asComposeRenderEffect()
                }
            }
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
    val density = LocalDensity.current
    val isNight = remember { isNightTime() }
    val nightStars = remember(isNight) {
        if (isNight) generateNightStars(count = 56) else emptyList()
    }
    val blurRadiusPx = with(density) { if (performanceOptions.blurEnabled) 64.dp.toPx() else 0f }


    val darkBlueBase = Color(0xFF094F6E)
    val darkTealBase = Color(0xFF061418)
    val darkBlackBase = Color(0xFF020506)

    Canvas(modifier = modifier
        .fillMaxSize()
        .graphicsLayer {
            if (blurRadiusPx > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                renderEffect = RenderEffect.createBlurEffect(
                    blurRadiusPx, blurRadiusPx, Shader.TileMode.DECAL
                ).asComposeRenderEffect()
            }
        }
    ) {
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
            radius = 18f + random.nextFloat() * 44f,
            alpha = 0.06f + random.nextFloat() * 0.12f
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
        Triple(Offset(w * -0.08f, h * 0.08f), w * 0.85f, baseColor.copy(alpha = 0.22f)),
        Triple(Offset(w * 0.82f, h * 0.18f), w * 0.58f, accentColor.copy(alpha = 0.20f)),
        Triple(Offset(w * 0.18f, h * 0.84f), w * 0.72f, highlightColor.copy(alpha = 0.24f)),
        Triple(Offset(w * 1.04f, h * 0.92f), w * 0.92f, baseColor.copy(alpha = 0.16f))
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

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(w * 0.55f, h * 0.72f),
            radius = w * 0.40f
        ),
        center = Offset(w * 0.55f, h * 0.72f),
        radius = w * 0.40f
    )
}
sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}
