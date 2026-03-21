package com.forgecompose.app_wear.presentation

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme

suspend fun isWatchConnected(context: Context): Boolean {
    val nodes = Wearable.getNodeClient(context).connectedNodes.await()
    return nodes.isNotEmpty()
}

@Composable
fun HrScreenPro(
    bpm: Int?,
    inExercise: Boolean,

    avgBpm: Int? = null,
    minBpm: Int? = null,
    maxBpm: Int? = null,
    sensorAvailable: Boolean = true,
    connectedToPhone: Boolean = true,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val ctx = LocalContext.current
    var connected by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(inExercise) {
        while (isActive) {
            connected = isWatchConnected(ctx)
            delay(if (inExercise) 30000L else 60000L)
        }
    }

    WorkoutTrackerTheme {
        // Subtle dark gradient background
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surface,
                        1f to MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                .padding(10.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status row: connection + sensor state
                TopStatusRow(connectedToPhone, sensorAvailable)
                androidx.wear.compose.material.Text(
                    text = when (connected) {
                        true -> "Watch connected"
                        false -> "No watch connected"
                        null -> "Checking..."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    style = androidx.wear.compose.material.MaterialTheme.typography.caption2
                )
                // Middle: Gauge + BPM + sparkline
                CenterCard(
                    bpm = bpm,
                    avg = avgBpm,
                    min = minBpm,
                    max = maxBpm,
                    inExercise = inExercise
                )

                // Bottom controls
                BottomControls(
                    inExercise = inExercise,
                    onStart = onStart,
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun TopStatusRow(connected: Boolean, sensorAvailable: Boolean) {
    val dotConnected = if (connected) Color(0xFF3CE28F) else Color(0xFFFFB04A)
    val dotSensor    = if (sensorAvailable) Color(0xFF3CE28F) else Color(0xFFFF7D7D)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(
            label = if (connected) "Phone linked" else "Phone not linked",
            dot = dotConnected
        )
        StatusPill(
            label = if (sensorAvailable) "Sensor OK" else "Adjust strap",
            dot = dotSensor
        )
    }
}

@Composable
private fun StatusPill(label: String, dot: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(6.dp))
        androidx.wear.compose.material.Text(label, style = androidx.wear.compose.material.MaterialTheme.typography.caption2, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun CenterCard(bpm: Int?, avg: Int?, min: Int?, max: Int?, inExercise: Boolean) {
    androidx.wear.compose.material.Card(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        backgroundPainter = androidx.wear.compose.material.CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            endBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BPM big + pulsing heart + zone chip
            BigBpmHeader(bpm, inExercise)

            // Mini stats row
            StatsRow(avg = avg, min = min, max = max)

            // Tiny sparkline at the bottom
            EcgSparkline(bpm = bpm, animate = inExercise && bpm != null)
        }
    }
}

@Composable
private fun BigBpmHeader(bpm: Int?, inExercise: Boolean) {
    val zone = zoneFor(bpm)
    val zoneColor by animateColorAsState(zone.color, label = "zoneColor")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        PulseHeart(bpm = bpm, color = zoneColor, animate = inExercise && bpm != null)
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.wear.compose.material.Text(
                text = bpm?.toString() ?: "—",
                color = Color.White,
                style = androidx.wear.compose.material.MaterialTheme.typography.title1.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold
            )
            androidx.wear.compose.material.Text(
                text = "bpm",
                color = Color.White.copy(alpha = 0.7f),
                style = androidx.wear.compose.material.MaterialTheme.typography.caption2
            )
            Spacer(Modifier.height(4.dp))
            ZoneChip(zone.label, zoneColor)
        }
    }
}

@Composable
private fun ZoneChip(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        androidx.wear.compose.material.Text(text, color = color, style = androidx.wear.compose.material.MaterialTheme.typography.caption2)
    }
}

@Composable
private fun StatsRow(avg: Int?, min: Int?, max: Int?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Avg", avg)
        StatItem("Min", min)
        StatItem("Max", max)
    }
}

@Composable
private fun StatItem(label: String, v: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.wear.compose.material.Text(label, color = Color.White.copy(alpha = 0.6f), style = androidx.wear.compose.material.MaterialTheme.typography.caption2)
        androidx.wear.compose.material.Text(v?.toString() ?: "—", color = Color.White, fontWeight = FontWeight.SemiBold, style = androidx.wear.compose.material.MaterialTheme.typography.body2.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun EcgSparkline(bpm: Int?, animate: Boolean) {
    val cyclesPerSecond = ((bpm ?: 72) / 60f).coerceIn(0.6f, 3f)
    val phase = if (animate) {
        val anim = rememberInfiniteTransition(label = "ecgPhase")
        anim.animateFloat(
            0f, 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = (1000f / cyclesPerSecond).toInt().coerceAtLeast(120), easing = LinearEasing)
            ),
            label = "phase"
        ).value
    } else {
        0f
    }

    // Crimson accents
    val crimson = Color(0xFFDC143C)                  // #DC143C
    val crimsonLight = Color(0xFFFF4D6D)           // Light Crimson
    val crimsonDark = Color(0xFF8B0000)                // Dark Crimson
    val lineBrush = Brush.horizontalGradient(listOf(crimsonDark, crimson, crimsonLight))

    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            val w = size.width
            val h = size.height
            val base = h * 0.5f
            val amp = h * 0.35f
            val cycleW = maxOf(w * 0.22f, 120f)
            val count = ceil(w / cycleW + 2).toInt()
            val shift = -phase * cycleW

            // baseline (dim crimson)
            drawLine(
                color = crimson.copy(alpha = 0.3f),
                start = Offset(0f, base),
                end = Offset(w, base),
                strokeWidth = 2f
            )

            val path = Path().apply {
                moveTo(shift - cycleW, base)
                repeat(count) { i ->
                    val x = shift + i * cycleW
                    addCycle(this, x, base, amp, cycleW)
                }
            }

            // 1) Soft glow underlay
            drawPath(
                path = path,
                color = crimson.copy(alpha = 0.4f),
                style = Stroke(width = 8f) // fat + translucent
            )
            // 2) Crisp colored line on top
            drawPath(
                path = path,
                brush = lineBrush,
                style = Stroke(width = 3.5f)
            )
        }
    }
}


private fun addCycle(path: Path, startX: Float, base: Float, amp: Float, w: Float) {
    val s1 = w * 0.18f; val s2 = w * 0.10f; val s3 = w * 0.10f; val s4 = w * 0.20f; val s5 = w * 0.42f
    var x = startX
    path.lineTo(x + s1, base); x += s1
    path.cubicTo(x + s2*0.3f, base, x + s2*0.7f, base - amp*0.25f, x + s2, base - amp*0.25f); x += s2
    path.lineTo(x + s3*0.18f, base + amp*0.35f)
    path.lineTo(x + s3*0.28f, base - amp)
    path.lineTo(x + s3*0.50f, base + amp*0.55f)
    path.lineTo(x + s3, base); x += s3
    path.cubicTo(x + s4*0.25f, base, x + s4*0.55f, base + amp*0.18f, x + s4, base + amp*0.10f); x += s4
    path.cubicTo(x + s5*0.35f, base + amp*0.10f, x + s5*0.75f, base - amp*0.12f, x + s5, base)
}

@Composable
private fun BottomControls(inExercise: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onStart()
            },
            enabled = !inExercise,
            colors = androidx.wear.compose.material.ButtonDefaults.primaryButtonColors(
                backgroundColor = androidx.wear.compose.material.MaterialTheme.colors.primary.copy(alpha = 0.8f)
            )
        ) { androidx.wear.compose.material.Text("Start", style = androidx.wear.compose.material.MaterialTheme.typography.button, color = Color.White) }

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onStop()
            },
            enabled = inExercise,
            colors = androidx.wear.compose.material.ButtonDefaults.secondaryButtonColors(
                backgroundColor = androidx.wear.compose.material.MaterialTheme.colors.surface.copy(alpha = 0.6f)
            )
        ) { androidx.wear.compose.material.Text("Stop", style = androidx.wear.compose.material.MaterialTheme.typography.button, color = Color.White) }
    }
}

@Composable
private fun PulseHeart(bpm: Int?, color: Color, animate: Boolean) {
    // Pulse period tracks BPM (fallback 72)
    val b = (bpm ?: 72).coerceIn(36, 200)
    val beatMs = (60_000f / b).roundToInt().coerceAtLeast(220)
    val scale = if (animate) {
        val infinite = rememberInfiniteTransition(label = "pulse")
        infinite.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = beatMs
                    1.1f at (beatMs * 0.12f).roundToInt()
                    0.9f at (beatMs * 0.40f).roundToInt()
                    1.05f at (beatMs * 0.70f).roundToInt()
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseScale"
        ).value
    } else {
        1f
    }

    val size = (34 * scale).dp
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

/* ----------------- Zone mapping ----------------- */



private data class Zone(val label: String, val color: Color)

private fun zoneFor(bpm: Int?): Zone {
    val v = bpm ?: return Zone("No signal", Color(0xFFFFB04A))
    return when (v) {
        in 0..99 -> Zone("Warm-up", Color(0xFF3CE28F))
        in 100..129 -> Zone("Fat-burn", Color(0xFF8DE1FF))
        in 130..159 -> Zone("Cardio", Color(0xFF9C86FF))
        else -> Zone("Peak", Color(0xFFFF7DA6))
    }
}