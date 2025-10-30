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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HeartbeatEcgCenterStrip(
    bpm: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    lineThickness: Dp = 3.dp,
    crimson: Color = Color(0xFFDC143C),
    crimsonLight: Color = MaterialTheme.colorScheme.secondary,
    crimsonDark: Color = MaterialTheme.colorScheme.error,
    label: Boolean = true
) {
    val density = LocalDensity.current
    val linePx = with(density) { lineThickness.toPx() }
    val b = (bpm ?: 72).coerceIn(36, 200)
    val cyclesPerSecond = (b / 60f).coerceIn(0.6f, 3.0f)
    val infinite = rememberInfiniteTransition(label = "ecg_center_phase")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = (1000f / cyclesPerSecond).toInt().coerceAtLeast(120),
                easing = LinearEasing
            )
        ),
        label = "phase"
    )
    val ampFactor = (b / 72f).coerceIn(0.8f, 1.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {

        Row(
            modifier = Modifier
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CenterPulseHeart(bpm = bpm, color = crimson)
            Spacer(Modifier.width(10.dp))
            if (label) {
                Text(
                    text = bpm?.let { "$it bpm" } ?: "— bpm",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

private fun Path.addEcgCycleRight(startX: Float, baseline: Float, amp: Float, cycleW: Float) {
    val pFlat1 = 0.18f
    val pRise = 0.10f
    val pSpike = 0.10f
    val pDrop = 0.20f
    val pRecov = 0.42f
    val total = pFlat1 + pRise + pSpike + pDrop + pRecov
    val sFlat1 = cycleW * pFlat1 / total
    val sRise = cycleW * pRise / total
    val sSpike = cycleW * pSpike / total
    val sDrop = cycleW * pDrop / total
    val sRecov = cycleW * pRecov / total
    var x = startX
    lineTo(x + sFlat1, baseline); x += sFlat1
    cubicTo(
        x + sRise * 0.3f, baseline,
        x + sRise * 0.7f, baseline - amp * 0.25f,
        x + sRise, baseline - amp * 0.25f
    ); x += sRise
    lineTo(x + sSpike * 0.18f, baseline + amp * 0.35f)
    lineTo(x + sSpike * 0.28f, baseline - amp)
    lineTo(x + sSpike * 0.50f, baseline + amp * 0.55f)
    lineTo(x + sSpike, baseline); x += sSpike
    cubicTo(
        x + sDrop * 0.25f, baseline,
        x + sDrop * 0.55f, baseline + amp * 0.18f,
        x + sDrop, baseline + amp * 0.10f
    ); x += sDrop
    cubicTo(
        x + sRecov * 0.35f, baseline + amp * 0.10f,
        x + sRecov * 0.75f, baseline - amp * 0.12f,
        x + sRecov, baseline
    )
}

private fun Path.addEcgCycleLeft(startX: Float, baseline: Float, amp: Float, cycleW: Float) {
    val pFlat1 = 0.18f
    val pRise = 0.10f
    val pSpike = 0.10f
    val pDrop = 0.20f
    val pRecov = 0.42f
    val total = pFlat1 + pRise + pSpike + pDrop + pRecov
    val sFlat1 = cycleW * pFlat1 / total
    val sRise = cycleW * pRise / total
    val sSpike = cycleW * pSpike / total
    val sDrop = cycleW * pDrop / total
    val sRecov = cycleW * pRecov / total
    var x = startX
    lineTo(-(x + sFlat1), baseline); x += sFlat1
    cubicTo(
        -(x + sRise * 0.3f), baseline,
        -(x + sRise * 0.7f), baseline - amp * 0.25f,
        -(x + sRise), baseline - amp * 0.25f
    ); x += sRise
    lineTo(-(x + sSpike * 0.18f), baseline + amp * 0.35f)
    lineTo(-(x + sSpike * 0.28f), baseline - amp)
    lineTo(-(x + sSpike * 0.50f), baseline + amp * 0.55f)
    lineTo(-(x + sSpike), baseline); x += sSpike
    cubicTo(
        -(x + sDrop * 0.25f), baseline,
        -(x + sDrop * 0.55f), baseline + amp * 0.18f,
        -(x + sDrop), baseline + amp * 0.10f
    ); x += sDrop
    cubicTo(
        -(x + sRecov * 0.35f), baseline + amp * 0.10f,
        -(x + sRecov * 0.75f), baseline - amp * 0.12f,
        -(x + sRecov), baseline
    )
}

@Composable
private fun CenterPulseHeart(bpm: Int?, color: Color) {
    val b = (bpm ?: 72).coerceIn(36, 200)
    val beatMs = (60_000f / b).toInt().coerceAtLeast(220)
    val pulse by rememberInfiniteTransition(label = "heart_pulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = beatMs
                1.12f at (beatMs * 0.12f).toInt()
                0.92f at (beatMs * 0.40f).toInt()
                1.05f at (beatMs * 0.70f).toInt()
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val size = (28 * pulse).dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.24f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Rounded.Favorite,
            contentDescription = null,
            tint = color
        )
    }
}