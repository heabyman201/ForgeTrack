package com.forgecompose.workouttracker



import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun HeartbeatEcgCenterStrip(
    bpm: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    lineThickness: Dp = 3.dp,
    // Crimson vibe pulls from theme; override if you want a custom red
    crimson: Color = MaterialTheme.colorScheme.primary,
    crimsonLight: Color = MaterialTheme.colorScheme.secondary, // lighter red
    crimsonDark: Color = MaterialTheme.colorScheme.error,      // deepest red
    label: Boolean = true
) {
    val density = LocalDensity.current
    val linePx = with(density) { lineThickness.toPx() }

    // Animation speed: 1 beat ≈ 1 cycle
    val cyclesPerSecond = ((bpm ?: 72) / 60f).coerceIn(0.6f, 3.0f)
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

    // Gentle amplitude scaling with BPM
    val ampFactor = ((bpm ?: 72) / 72f).coerceIn(0.8f, 1.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height) // borderless: no clip/background container
    ) {
        // ECG canvas (borderless; draws glow + line)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val w = size.width
            val h = size.height
            val halfW = w / 2f
            val base = h * 0.5f
            val amp = (h * 0.32f * ampFactor).coerceIn(h * 0.18f, h * 0.42f)

            // One cycle width + how many to fill half the screen
            val cycleW = max(w * 0.18f, 120f)
            val cyclesToDraw = ceil((halfW / cycleW) + 2).toInt()

            // Shift right-going path by phase
            val offsetX = -phase * cycleW

            // Brush + glow
            val lineBrush = Brush.horizontalGradient(listOf(crimsonDark, crimson, crimsonLight))
            val baselineAlpha = 0.18f

            // Baseline across full width (dim crimson)
            drawLine(
                color = crimson.copy(alpha = baselineAlpha),
                start = Offset(0f, base),
                end = Offset(w, base),
                strokeWidth = linePx
            )

            // Build a path that starts at center and goes rightwards
            fun buildRightPath(): Path {
                return Path().apply {
                    // Start at center baseline, then tile cycles to the right
                    moveTo(0f, base)
                    for (i in 0 until cyclesToDraw) {
                        val startX = offsetX + i * cycleW
                        // Only draw if that segment will be visible in the right half
                        if (startX + cycleW >= 0f) {
                            addEcgCycleFromCenter(
                                startX = startX,
                                baseline = base,
                                amp = amp,
                                cycleW = cycleW
                            )
                        }
                    }
                }
            }

            // Draw right side (positive X) from center
            withTransform({
                translate(left = halfW, top = 0f) // center is (0, base) now
            }) {
                val pathRight = buildRightPath()
                // Soft glow underlay
                drawPath(
                    path = pathRight,
                    color = crimson.copy(alpha = 0.25f),
                    style = Stroke(width = linePx * 2.6f)
                )
                // Crisp line
                drawPath(
                    path = pathRight,
                    brush = lineBrush,
                    style = Stroke(width = linePx)
                )
            }

            // Draw left side by mirroring X (scale -1 on X around center)
            withTransform({
                translate(left = halfW, top = 0f)
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
            }) {
                val pathLeft = buildRightPath()
                drawPath(
                    path = pathLeft,
                    color = crimson.copy(alpha = 0.25f),
                    style = Stroke(width = linePx * 2.6f)
                )
                drawPath(
                    path = pathLeft,
                    brush = lineBrush,
                    style = Stroke(width = linePx)
                )
            }
        }

        // Center stack: heart icon + BPM label (no borders)
        Row(
            modifier = Modifier
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing heart
            CenterPulseHeart(
                bpm = bpm,
                color = crimson
            )
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

/**
 * Add one ECG-ish cycle that starts from the current point and proceeds *rightward*.
 * This is similar to your earlier cycle but assumes we've translated to the center already.
 */
private fun Path.addEcgCycleFromCenter(startX: Float, baseline: Float, amp: Float, cycleW: Float) {
    val pFlat1 = 0.18f
    val pRise  = 0.10f
    val pSpike = 0.10f
    val pDrop  = 0.20f
    val pRecov = 0.42f
    val total = pFlat1 + pRise + pSpike + pDrop + pRecov
    val sFlat1 = cycleW * pFlat1 / total
    val sRise  = cycleW * pRise  / total
    val sSpike = cycleW * pSpike / total
    val sDrop  = cycleW * pDrop  / total
    val sRecov = cycleW * pRecov / total

    var x = startX
    // Flat
    lineTo(x + sFlat1, baseline); x += sFlat1
    // Gentle rise (P)
    cubicTo(
        x + sRise * 0.3f, baseline,
        x + sRise * 0.7f, baseline - amp * 0.25f,
        x + sRise,        baseline - amp * 0.25f
    ); x += sRise
    // Sharp QRS
    lineTo(x + sSpike * 0.18f, baseline + amp * 0.35f)
    lineTo(x + sSpike * 0.28f, baseline - amp)
    lineTo(x + sSpike * 0.50f, baseline + amp * 0.55f)
    lineTo(x + sSpike,         baseline); x += sSpike
    // Drop smoothing + recovery (T)
    cubicTo(
        x + sDrop * 0.25f, baseline,
        x + sDrop * 0.55f, baseline + amp * 0.18f,
        x + sDrop,         baseline + amp * 0.10f
    ); x += sDrop
    cubicTo(
        x + sRecov * 0.35f, baseline + amp * 0.10f,
        x + sRecov * 0.75f, baseline - amp * 0.12f,
        x + sRecov,         baseline
    )
}

/** Center pulsing heart for the crimson vibe */
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
        // Use Material icons if you have them; otherwise swap to your vector
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Rounded.Favorite,
            contentDescription = null,
            tint = color
        )
    }
}


/* ------------------------- Example Usage ------------------------- */

