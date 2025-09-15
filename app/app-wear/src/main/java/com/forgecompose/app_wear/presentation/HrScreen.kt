// app-wear/src/main/java/com/example/app_wear/ui/HrScreenPro.kt
package com.forgecompose.app_wear.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import com.forgecompose.app_wear.presentation.CrimsonWearTheme
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

suspend fun isWatchConnected(context: Context): Boolean {
    val nodes = Wearable.getNodeClient(context).connectedNodes.await()
    return nodes.isNotEmpty()
}

@Composable
fun HrScreenPro(
    bpm: Int?,
    inExercise: Boolean,
    // extra info from your ViewModel / repo:
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

    LaunchedEffect(Unit) {
        while (connected == null || connected == false) {
            connected = isWatchConnected(ctx)
            delay(5000)
        }
    }

    CrimsonWearTheme {
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
                Text(
                    text = when (connected) {
                        true -> "Watch connected"
                        false -> "No watch connected"
                        null -> "Checking..."
                    }
                )
                // Middle: Gauge + BPM + sparkline
                CenterCard(
                    bpm = bpm,
                    avg = avgBpm,
                    min = minBpm,
                    max = maxBpm
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
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CenterCard(bpm: Int?, avg: Int?, min: Int?, max: Int?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()

            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BPM big + pulsing heart + zone chip
            BigBpmHeader(bpm)

            // Mini stats row
            StatsRow(avg = avg, min = min, max = max)

            // Tiny sparkline at the bottom
            EcgSparkline(bpm = bpm)
        }
    }
}

@Composable
private fun BigBpmHeader(bpm: Int?) {
    val zone = zoneFor(bpm)
    val zoneColor by animateColorAsState(zone.color, label = "zoneColor")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        PulseHeart(bpm = bpm, color = zoneColor)
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = bpm?.toString() ?: "—",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "bpm",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
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
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(v?.toString() ?: "—", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EcgSparkline(bpm: Int?) {
    val cyclesPerSecond = ((bpm ?: 72) / 60f).coerceIn(0.6f, 3f)
    val anim = rememberInfiniteTransition(label = "ecgPhase")
    val phase by anim.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = (1000f / cyclesPerSecond).toInt().coerceAtLeast(120), easing = LinearEasing)
        ),
        label = "phase"
    )

    // Crimson accents
    val crimson = MaterialTheme.colorScheme.primary                  // #B71C1C
    val crimsonLight = MaterialTheme.colorScheme.secondary           // #EF5350
    val crimsonDark = MaterialTheme.colorScheme.error                // #7F0000 (mapped)
    val lineBrush = Brush.horizontalGradient(listOf(crimsonDark, crimson, crimsonLight))

    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
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
                color = crimson.copy(alpha = 0.18f),
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
                color = crimson.copy(alpha = 0.25f),
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
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onStart()
            },
            enabled = !inExercise
        ) { Text("Start") }

        Button(
            onClick = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onStop()
            },
            enabled = inExercise
        ) { Text("Stop") }
    }
}

@Composable
private fun PulseHeart(bpm: Int?, color: Color) {
    // Pulse period tracks BPM (fallback 72)
    val b = (bpm ?: 72).coerceIn(36, 200)
    val beatMs = (60_000f / b).roundToInt().coerceAtLeast(220)
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
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
    )

    val size = (34 * scale).dp
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Rounded.Favorite,
            contentDescription = null,
            tint = color
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
