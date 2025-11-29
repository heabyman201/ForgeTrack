package com.forgecompose.workouttracker

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HeartbeatEcgCenterStrip(
    bpm: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    lineThickness: Dp = 2.dp,
    crimson: Color = Color(0xFFDC143C),
    label: Boolean = true
) {
    val density = LocalDensity.current
    val linePx = with(density) { lineThickness.toPx() }

    // FIX: Handle 0 as "detecting" (default to 72 for animation) instead of clamping 0 to 36
    val animationBpm = if (bpm == null || bpm == 0) 72 else bpm.coerceIn(36, 200)

    val cyclesPerSecond = (animationBpm / 60f).coerceIn(0.6f, 3.0f)
    val ampFactor = (animationBpm / 72f).coerceIn(0.8f, 1.25f)

    val infinite = rememberInfiniteTransition(label = "ecg_center_phase")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = (1000f / cyclesPerSecond).toInt(), easing = LinearEasing)
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val y = size.height * 0.5f
                // Base line
                drawLine(
                    color = crimson.copy(alpha = 0.3f), // Dimmer base line looks cleaner
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = linePx
                )

                // "Scanner" line
                val p = phase % 1f
                val x = size.width * p
                val tickH = size.height * 0.3f * ampFactor
                val fade = 0.45f * (0.5f + 0.5f * kotlin.math.cos((p * 2 * Math.PI).toFloat()))

                drawLine(
                    color = crimson.copy(alpha = fade + 0.3f), // Add base alpha
                    start = Offset(x, y - tickH),
                    end = Offset(x, y + tickH),
                    strokeWidth = linePx * 1.5f // Slightly thicker scanner
                )
            }
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CenterPulseHeart(bpm = animationBpm, color = crimson)
            Spacer(Modifier.width(10.dp))
            if (label) {
                // FIX: Use monospaced digits so text doesn't jitter when numbers change (e.g. 111 vs 100)
                Text(
                    text = if (bpm == null || bpm == 0) "--" else "$bpm bpm",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFeatureSettings = "tnum" // Tabular (monospaced) numbers
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CenterPulseHeart(bpm: Int, color: Color) {
    val beatMs = (60_000f / bpm).toInt().coerceAtLeast(220)
    val trans = rememberInfiniteTransition(label = "heart_phase")
    val phase by trans.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(durationMillis = beatMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )

    val s = 1f + 0.12f * kotlin.math.sin(phase)
    val glowAlpha = 0.2f * (0.5f * (1f + kotlin.math.cos(phase)))

    // FIX: Using a fixed size Box and scaling via graphicsLayer to prevent layout thrashing
    Box(
        modifier = Modifier.size(42.dp), // Fixed container size
        contentAlignment = Alignment.Center
    ) {
        // Glow Effect
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = s * 1.2f
                    scaleY = s * 1.2f
                    alpha = glowAlpha
                }
                .clip(RoundedCornerShape(50))
                .background(color)
        )

        // Heart Icon
        Box(
            modifier = Modifier
                .size(32.dp) // Base size of the heart
                .graphicsLayer {
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

