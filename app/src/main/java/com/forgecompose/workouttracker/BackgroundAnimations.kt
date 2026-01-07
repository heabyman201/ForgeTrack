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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager
        .flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

    val theme = appearanceOptions.selectedTheme.colors
    val density = LocalDensity.current

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


        drawRect(theme.background)


        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.secondary.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.1f, h * 0.1f),
                radius = w * 1.2f
            ),
            center = Offset(w * 0.1f, h * 0.1f),
            radius = w * 1.2f
        )


        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.tertiary.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(w * 0.9f, h * 0.8f),
                radius = w * 1.5f
            ),
            center = Offset(w * 0.9f, h * 0.8f),
            radius = w * 1.5f
        )


        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    theme.primary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(w, 0f),
                end = Offset(w * 0.4f, h * 0.5f)
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
                listOf(darkTealBase, darkBlackBase)
            )
        )


        val coreColor = theme.primary.copy(alpha = 0.20f).compositeOver(darkBlueBase)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w * 0.35f, h * 0.30f),
                radius = w * 1.1f
            ),
            center = Offset(w * 0.35f, h * 0.30f),
            radius = w * 1.1f
        )


        val secondaryNode = theme.secondary.copy(alpha = 0.12f).compositeOver(darkTealBase)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryNode.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.75f),
                radius = w * 1.3f
            ),
            center = Offset(w * 0.85f, h * 0.75f),
            radius = w * 1.3f
        )


        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(theme.primary.copy(alpha = 0.05f), Color.Transparent),
                start = Offset(w * 0.2f, 0f),
                end = Offset(w * 0.8f, h * 0.4f)
            )
        )


        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
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
sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}