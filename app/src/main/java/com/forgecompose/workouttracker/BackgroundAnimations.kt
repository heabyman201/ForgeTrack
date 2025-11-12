package com.forgecompose.workouttracker

import android.graphics.RenderEffect
import android.graphics.Shader
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

//    LaunchedEffect(enableAnimation, slowCycleMinutes) {
//        if (!enableAnimation) return@LaunchedEffect
//        val cycleSec1 = (slowCycleMinutes.coerceAtLeast(1f) * 60f)
//        val cycleSec2 = cycleSec1 * 1.6180339887f
//        var last = 0L
//        while (true) {
//            withFrameNanos { now ->
//                if (last == 0L) { last = now; return@withFrameNanos }
//                val dt = (now - last) / 1_000_000_000f
//                last = now
//                p1.floatValue = (p1.floatValue + dt / cycleSec1) % 2f
//                p2.floatValue = (p2.floatValue + dt / cycleSec2) % 2f
//            }
//        }
//    }

    fun tri(t: Float): Float {
        val x = (t % 2f + 2f) % 2f
        return 1f - kotlin.math.abs(x - 1f)
    }
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)

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

        val bg = Brush.radialGradient(
            colors = listOf(
                Color(0xFF652121).copy(alpha = 0.78f - 0.06f * s1),
                Color(0xFF2B0E0E),
                Color(0xFF120707)
            ),
            center = Offset(w * (0.30f + 0.14f * s1), h * (0.24f + 0.12f * s2)),
            radius = max(w, h) * (0.72f + 0.06f * s1)
        )
        drawRect(bg)



        if (introAlpha > 0f) {
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

//    LaunchedEffect(enableAnimation, slowCycleMinutes) {
//        if (!enableAnimation) return@LaunchedEffect
//        val cycleSec1 = (slowCycleMinutes.coerceAtLeast(1f) * 60f)
//        val cycleSec2 = cycleSec1 * 1.6180339887f
//        var last = 0L
//        while (true) {
//            withFrameNanos { now ->
//                if (last == 0L) { last = now; return@withFrameNanos }
//                val dt = (now - last) / 1_000_000_000f
//                last = now
//                p1.floatValue = (p1.floatValue + dt / cycleSec1) % 2f
//                p2.floatValue = (p2.floatValue + dt / cycleSec2) % 2f
//            }
//        }
//    }

    fun tri(t: Float): Float {
        val x = (t % 2f + 2f) % 2f
        return 1f - kotlin.math.abs(x - 1f)
    }
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)

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

        val bg = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1284B2).copy(alpha = 0.78f - 0.06f * s1),
                Color(0xFF0E272B),
                Color(0xFF071012)
            ),
            center = Offset(w * (0.30f + 0.14f * s1), h * (0.24f + 0.12f * s2)),
            radius = max(w, h) * (0.72f + 0.06f * s1)
        )
        drawRect(bg)



        if (introAlpha > 0f) {
            drawRect(introBrush, alpha = introAlpha.coerceIn(0f, 1f))
        }
    }
}
sealed interface BackdropMode {
    data class PreBaked(val frames: Int = 24) : BackdropMode
    data object Live : BackdropMode
}