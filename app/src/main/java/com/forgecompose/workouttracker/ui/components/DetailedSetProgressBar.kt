package com.forgecompose.workouttracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.forgecompose.workouttracker.ColorSchemeAppTheme

import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DetailedSetsProgressBar(
    currentSet: Int,
    goalSets: Int,
    modifier: Modifier = Modifier,
    theme: ColorSchemeAppTheme
) {
    if (goalSets <= 0) return

    val target = remember(currentSet, goalSets) {
        when {
            goalSets <= 1 -> if (currentSet >= 1) 1f else 0f
            else -> ((currentSet - 1).toFloat() / (goalSets - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    val progressState = animateFloatAsState(
        targetValue = target,
        label = "SetProgressBarProgress",
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

    var animationClock by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (true) {
            val currentTime = withFrameNanos { it }
            if (lastFrameTime != 0L) {
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                animationClock += deltaTime
            }
            lastFrameTime = currentTime
            delay(42)
        }
    }

    // Bursts to 1f on each set completion then fades out
    val celebrationGlow = remember { Animatable(0f) }
    LaunchedEffect(currentSet) {
        if (currentSet > 1) {
            celebrationGlow.snapTo(1f)
            celebrationGlow.animateTo(0f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
    }

    val accent = theme.secondary
    val accentBright = theme.primary.copy(alpha = 1.0f)

    val isLinear = goalSets <= 8

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isLinear) 44.dp else 220.dp)
            .drawWithCache {
                onDrawBehind {
                    val progress = progressState.value
                    val shimmer = (animationClock / 1.8f) % 1.4f - 0.2f
                    val pulse = 1.05f + 0.15f * sin(animationClock * 2 * PI.toFloat())
                    val celebration = celebrationGlow.value

                    if (isLinear) {
                        drawLinearProgress(
                            goalSets, currentSet, progress,
                            shimmer, pulse, celebration, accent, accentBright
                        )
                    } else {
                        drawCircularProgress(
                            goalSets, currentSet, progress,
                            shimmer, pulse, celebration, accent
                        )
                    }
                }
            }
    )
}

public fun DrawScope.drawLinearProgress(
    goalSets: Int, currentSet: Int, progress: Float,
    shimmer: Float, pulse: Float, celebration: Float,
    accent: Color, accentBright: Color
) {
    val y = size.height / 2f
    val base = 6.dp.toPx()
    val prog = 9.dp.toPx()
    val dotR = 8.dp.toPx()
    val w = size.width - (dotR * 2)
    val startPad = dotR

    // Color and stroke width brighten as completion approaches
    val completionBrightness = progress.coerceIn(0f, 1f)
    val progressColor = lerp(accent, accentBright, completionBrightness * 0.6f)
    val progressTip = lerp(progressColor, Color.White, completionBrightness * 0.25f)

    drawLine(
        color = Color.White.copy(0.1f),
        start = Offset(startPad, y),
        end = Offset(startPad + w, y),
        strokeWidth = base,
        cap = StrokeCap.Round
    )

    if (progress > 0f) {
        // Soft glow bloom behind the progress line, grows with completion and celebration
        val glowAlpha = 0.12f + completionBrightness * 0.2f + celebration * 0.28f
        val glowWidth = prog * (1.8f + completionBrightness * 0.6f + celebration * 1.4f)
        drawLine(
            brush = Brush.horizontalGradient(listOf(progressColor.copy(glowAlpha), progressTip.copy(glowAlpha))),
            start = Offset(startPad, y),
            end = Offset(startPad + w * progress, y),
            strokeWidth = glowWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.horizontalGradient(listOf(progressColor, progressTip)),
            start = Offset(startPad, y),
            end = Offset(startPad + w * progress, y),
            strokeWidth = prog,
            cap = StrokeCap.Round
        )
    }

    val shWidth = w * 0.4f
    val shStart = (w + shWidth) * shimmer - shWidth + startPad
    drawLine(
        brush = Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(0.18f), Color.Transparent),
            start = Offset(shStart, y),
            end = Offset(shStart + shWidth, y)
        ),
        start = Offset(startPad, y),
        end = Offset(startPad + w, y),
        strokeWidth = base,
        cap = StrokeCap.Round
    )

    for (i in 1..goalSets) {
        val x = if (goalSets > 1) startPad + (w * ((i - 1).toFloat() / (goalSets - 1))) else size.width / 2f
        val completed = i < currentSet
        val isCurrent = i == currentSet

        when {
            completed -> drawCircle(color = progressColor, radius = dotR, center = Offset(x, y))
            isCurrent -> {
                val r = dotR * pulse
                // Celebration burst: two expanding transparent rings
                if (celebration > 0f) {
                    drawCircle(
                        color = progressTip.copy(alpha = celebration * 0.2f),
                        radius = r * (3.0f + celebration * 2.5f),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = celebration * 0.35f),
                        radius = r * (1.9f + celebration * 1.4f),
                        center = Offset(x, y)
                    )
                }
                drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(x, y))
                drawCircle(
                    color = Color.White,
                    radius = r * (1f + celebration * 0.5f),
                    center = Offset(x, y)
                )
            }
            else -> drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(x, y))
        }
    }
}

public fun DrawScope.drawCircularProgress(
    goalSets: Int, currentSet: Int, progress: Float,
    shimmer: Float, pulse: Float, celebration: Float, accent: Color
) {
    val strokeBase = 8.dp.toPx()
    val strokeProg = 10.dp.toPx()
    val dotR = 6.dp.toPx()
    val pad = 16.dp.toPx()
    val radius = (min(size.width, size.height) / 2f) - (strokeProg + pad)
    val arcCenter = center
    val startAngle = -90f
    val sweep = 360f

    // Arc color and glow width brighten toward completion
    val completionBrightness = progress.coerceIn(0f, 1f)
    val progressColor = lerp(accent, Color.White, completionBrightness * 0.35f)

    drawCircle(
        color = Color.White.copy(0.1f),
        radius = radius,
        center = arcCenter,
        style = Stroke(width = strokeBase, cap = StrokeCap.Round)
    )

    if (progress > 0f) {
        val glowAlpha = 0.18f + completionBrightness * 0.22f + celebration * 0.35f
        val glowWidth = strokeProg * (1.6f + completionBrightness * 0.7f + celebration * 1.3f)
        drawArc(
            color = progressColor.copy(alpha = glowAlpha),
            startAngle = startAngle,
            sweepAngle = sweep * progress,
            useCenter = false,
            topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = glowWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = progressColor,
            startAngle = startAngle,
            sweepAngle = sweep * progress,
            useCenter = false,
            topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeProg, cap = StrokeCap.Round)
        )
    }

    for (i in 1..goalSets) {
        val t = if (goalSets > 1) (i - 1).toFloat() / (goalSets - 1).toFloat() else 0.5f
        val ang = Math.toRadians((startAngle + t * sweep).toDouble())
        val cx = (arcCenter.x + cos(ang).toFloat() * radius)
        val cy = (arcCenter.y + sin(ang).toFloat() * radius)
        val completed = i < currentSet
        val isCurrent = i == currentSet

        when {
            completed -> drawCircle(color = progressColor, radius = dotR, center = Offset(cx, cy))
            isCurrent -> {
                val r = dotR * pulse
                if (celebration > 0f) {
                    drawCircle(
                        color = progressColor.copy(alpha = celebration * 0.2f),
                        radius = r * (3.0f + celebration * 2.5f),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = celebration * 0.35f),
                        radius = r * (1.9f + celebration * 1.4f),
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(color = Color.White, radius = r * (1f + celebration * 0.5f), center = Offset(cx, cy))
                drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(cx, cy))
            }
            else -> drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(cx, cy))
        }
    }
}
