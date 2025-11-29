package com.forgecompose.workouttracker

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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun AnimatedBackdrop(
    modifier: Modifier = Modifier,
    introBrush: Brush,
    introAlpha: Float,
    enableWaves: Boolean,
    enableAnimation: Boolean,
    slowCycleMinutes: Float = 8f
) {
    val p1 = remember { mutableFloatStateOf(0.5f) }
    val p2 = remember { mutableFloatStateOf(0.2f) }

    fun tri(t: Float): Float {
        val x = (t % 2f + 2f) % 2f
        return 1f - kotlin.math.abs(x - 1f)
    }

    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager
        .flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

    val currentThemeColors = appearanceOptions.selectedTheme.colors

    val density = LocalDensity.current
    val navEffects = performanceOptions.navEffects

    val blurRadiusPx = remember(performanceOptions.blurEnabled) {
        if (
            performanceOptions.blurEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            with(density) { 40.dp.toPx() }
        } else {
            0f
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                renderEffect =
                    if (blurRadiusPx > 0f) {
                        createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                    } else {
                        null
                    }
            }
    ) {
        val w = size.width
        val h = size.height
        val s1 = 0.4f
        val s2 = 0.3f

        val bg = Brush.radialGradient(
            colors = listOf(
                currentThemeColors.secondary.copy(alpha = 0.78f - 0.06f * s1),
                currentThemeColors.tertiary,
                currentThemeColors.background
            ),
            center = Offset(w * (0.30f + 0.14f * s1), h * (0.24f + 0.12f * s2)),
            radius = max(w, h) * (0.72f + 0.06f * s1)
        )
        drawRect(bg)

        if (introAlpha > 0f && navEffects) {
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
    val p1 = remember { mutableFloatStateOf(0.5f) }
    val p2 = remember { mutableFloatStateOf(0.2f) }

    fun tri(t: Float): Float {
        val x = (t % 2f + 2f) % 2f
        return 1f - kotlin.math.abs(x - 1f)
    }
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val blurEnabled = performanceOptions.blurEnabled
    val blurCanva = remember { if (blurEnabled) 64.dp else 0.dp }

    Canvas(modifier = modifier.fillMaxSize()
        .graphicsLayer{
            renderEffect = RenderEffect.createBlurEffect(
                blurCanva.value,blurCanva.value, Shader.TileMode.DECAL
            ).asComposeRenderEffect()

        }) {
        val w = size.width
        val h = size.height
        val s1 =  0.4f
        val s2 = 0.3f

        val darkBlueBase = Color(0xFF094F6E)
        val darkTealBase = Color(0xFF061418)
        val darkBlackBase = Color(0xFF020506)

        val mixedCenter = theme.primary.copy(alpha = 0.15f).compositeOver(darkBlueBase)
        val mixedMiddle = theme.secondary.copy(alpha = 0.10f).compositeOver(darkTealBase)
        val mixedOuter = theme.background.copy(alpha = 0.60f).compositeOver(darkBlackBase)

        val bg = Brush.radialGradient(
            colors = listOf(
                mixedCenter.copy(alpha = 0.60f - 0.06f * s1),
                mixedMiddle,
                mixedOuter
            ),
            center = Offset(w * (0.30f + 0.14f * s1), h * (0.24f + 0.12f * s2)),
            radius = max(w, h) * (0.72f + 0.06f * s1)
        )
        drawRect(bg)

        if (introAlpha > 0f && performanceOptions.navEffects) {
            drawRect(introBrush, alpha = introAlpha.coerceIn(0f, 1f))
        }
    }
}
sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}