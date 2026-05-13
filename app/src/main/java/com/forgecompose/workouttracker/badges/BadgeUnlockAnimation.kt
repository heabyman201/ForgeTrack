package com.forgecompose.workouttracker.badges

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random




// Heartbeat waveform: two full lub-dub cycles, then stop. No looping.
// Per cycle: short softer "lub" → 90ms gap → stronger "dub" → ~620ms rest.
private const val HEARTBEAT_CYCLES = 2
private val HEARTBEAT_TIMINGS = longArrayOf(0, 55, 90, 95, 620, 55, 90, 95, 620)
private val HEARTBEAT_AMPLITUDES = intArrayOf(0, 170, 0, 255, 0, 170, 0, 255, 0)

@Suppress("MissingPermission")
private fun getSystemVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

@Suppress("MissingPermission")
private fun startHeartbeatVibration(vibrator: Vibrator?) {
    vibrator ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (!vibrator.hasVibrator()) return
    try {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(HEARTBEAT_TIMINGS, HEARTBEAT_AMPLITUDES, -1)
        } else {
            VibrationEffect.createWaveform(HEARTBEAT_TIMINGS, -1)
        }
        vibrator.vibrate(effect)
    } catch (_: SecurityException) {
        // VIBRATE permission denied at runtime — fall back silently.
    }
}

private data class FireworkBurst(
    val cxFrac: Float,
    val cyFrac: Float,
    val color: Color,
    val secondaryColor: Color,
    val streakCount: Int,
    val launchDelayMs: Long,
    val maxRadiusFrac: Float = 0.24f,
)

@Composable
fun BadgeUnlockAnimation(
    badgeTitle: String,
    onAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current

    val animationTime = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0f) }
    val iconRotation = remember { Animatable(-30f) }
    val lightBurst = remember { Animatable(0f) }
    val ornamentProgress = remember { Animatable(0f) }

    val prShockwave = remember { Animatable(0f) }
    val prFlash = remember { Animatable(0f) }
    val prPulse = remember { Animatable(1f) }
    val prTextAlpha = remember { Animatable(0f) }
    val prTextScale = remember { Animatable(0.9f) }

    val raysGlowBurst = remember { Animatable(0f) }
    val crownEntranceGlow = remember { Animatable(0f) }
    val crownShine = remember { Animatable(-0.3f) }
    val textBlur = remember { Animatable(34f) }
    val heartbeatScale = remember { Animatable(1f) }
    val heartbeatRingLub = remember { Animatable(1f) }
    val heartbeatRingDub = remember { Animatable(1f) }

    val vibrator = remember(context) { getSystemVibrator(context) }

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    val fireworks = remember {
        listOf(
            FireworkBurst(0.20f, 0.28f, Color(0xFFFFD54F), Color.White,           22, 340),
            FireworkBurst(0.82f, 0.30f, Color(0xFFFF7043), Color(0xFFFFE0B2),     20, 600),
            FireworkBurst(0.14f, 0.72f, Color(0xFF4DD0E1), Color.White,           24, 860),
            FireworkBurst(0.88f, 0.74f, Color(0xFFFFEE58), Color.White,           22, 1120),
            FireworkBurst(0.50f, 0.16f, Color(0xFFCE93D8), Color.White,           26, 1380),
            FireworkBurst(0.30f, 0.86f, Color(0xFFA5D6A7), Color.White,           22, 1640),
            FireworkBurst(0.72f, 0.88f, Color(0xFFFFAB91), Color.White,           24, 1900),
            FireworkBurst(0.50f, 0.42f, Color(0xFFFFD700), Color.White,           32, 2180, 0.34f),
        )
    }

    LaunchedEffect(Unit) {
        particles = generateParticles(
            listOf(Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFE082), Color(0xFFFFF8E1), Color(0xFFFFB300), Color.White),
            count = 1200
        )

        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope {
            launch {
                animationTime.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3750, easing = LinearEasing)
                )
            }
            launch {
                lightBurst.animateTo(1f, tween(80, easing = FastOutSlowInEasing))
                lightBurst.animateTo(0f, tween(600))
            }

            // Background flash/shockwave/pulse — the inciting "boom"
            launch {
                delay(40)
                prFlash.snapTo(1f)
                prFlash.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                delay(60)
                prShockwave.snapTo(0f)
                prShockwave.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                delay(70)
                repeat(3) {
                    prPulse.animateTo(1.15f, tween(110, easing = FastOutSlowInEasing))
                    prPulse.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                }
            }
            launch {
                delay(80)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(80)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // Cause → effect → confirmation cascade.
            // Rays fire FIRST — staggered one-by-one waterfall.
            launch {
                delay(90)
                ornamentProgress.snapTo(0f)
                ornamentProgress.animateTo(1f, tween(durationMillis = 720, easing = FastOutSlowInEasing))
            }
            launch {
                delay(90)
                raysGlowBurst.animateTo(1f, tween(155, easing = FastOutSlowInEasing))
                raysGlowBurst.animateTo(0f, tween(1150, easing = FastOutSlowInEasing))
            }
            launch {
                delay(90)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // Crown stamps in ~60ms after the rays — overshoot spring + entrance glow halo.
            launch {
                delay(150)
                iconScale.snapTo(0f)
                iconScale.animateTo(1f, spring(dampingRatio = 0.22f, stiffness = 155f))
            }
            launch {
                delay(150)
                iconRotation.snapTo(-50f)
                iconRotation.animateTo(0f, spring(dampingRatio = 0.30f, stiffness = 115f))
            }
            launch {
                delay(150)
                crownEntranceGlow.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
                crownEntranceGlow.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                delay(150)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            // Text begins to materialize ~60ms after the stamp — alpha + blur dissolve.
            launch {
                delay(210)
                textAlpha.animateTo(1f, tween(1250, easing = FastOutSlowInEasing))
            }
            launch {
                delay(210)
                textScale.snapTo(0.55f)
                textScale.animateTo(1f, spring(dampingRatio = 0.36f, stiffness = 250f))
            }
            launch {
                delay(210)
                textBlur.snapTo(34f)
                textBlur.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
            }
            launch {
                delay(270)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(380)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(420)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            // Badge title (subtitle) — appears after the main text begins.
            launch {
                delay(800)
                prTextAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                delay(800)
                prTextScale.snapTo(0.85f)
                prTextScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
            }

            // Crown shine — diagonal highlight sweeps across the crown twice.
            launch {
                delay(420)
                crownShine.snapTo(-0.3f)
                crownShine.animateTo(1.3f, tween(820, easing = FastOutSlowInEasing))
                delay(380)
                crownShine.snapTo(-0.3f)
                crownShine.animateTo(1.3f, tween(720, easing = FastOutSlowInEasing))
            }
        }
        onAnimationFinished()
    }

    // Heartbeat pulse — kicks in after the text has resolved, plays exactly two
    // lub-dub cycles synchronized with a custom waveform vibration, then stops.
    DisposableEffect(vibrator) {
        onDispose { vibrator?.cancel() }
    }
    LaunchedEffect(Unit) {
        try {
            delay(1700)
            startHeartbeatVibration(vibrator)
            repeat(HEARTBEAT_CYCLES) {
                launch {
                    heartbeatRingLub.snapTo(0f)
                    heartbeatRingLub.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
                }
                heartbeatScale.animateTo(1.06f, tween(55, easing = FastOutSlowInEasing))
                heartbeatScale.animateTo(1.0f, tween(90, easing = FastOutSlowInEasing))
                launch {
                    heartbeatRingDub.snapTo(0f)
                    heartbeatRingDub.animateTo(1f, tween(durationMillis = 820, easing = FastOutSlowInEasing))
                }
                heartbeatScale.animateTo(1.14f, tween(95, easing = FastOutSlowInEasing))
                heartbeatScale.animateTo(1.0f, tween(620, easing = FastOutSlowInEasing))
            }
        } finally {
            vibrator?.cancel()
        }
    }

    val mainText = "ACHIEVEMENT UNLOCKED"
    val styledText = remember(mainText, theme.primary, theme.secondary) {
        val gradient = Brush.verticalGradient(
            colors = listOf(theme.primary, theme.secondary, theme.primary)
        )
        buildAnnotatedString {
            withStyle(SpanStyle(brush = gradient)) { append(mainText) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
//            .graphicsLayer { zIndex = 9999f },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = animationTime.value * 2.2f

            if (lightBurst.value > 0f) {
                drawCircle(
                    color = theme.primary.copy(alpha = lightBurst.value * 0.6f),
                    radius = size.maxDimension * lightBurst.value,
                    center = center
                )
            }

            if (prFlash.value > 0f) {
                drawRect(
                    color = theme.secondary.copy(alpha = 0.25f * prFlash.value),
                    size = size
                )
            }

            if (prShockwave.value > 0f) {
                val t = prShockwave.value
                val r = size.maxDimension * (0.1f + 1.1f * t)
                val a = (1f - t).coerceIn(0f, 1f)
                drawCircle(
                    color = theme.primary.copy(alpha = 0.7f * a),
                    radius = r,
                    center = center,
                    style = Stroke(width = (12.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                )
            }

            // Fireworks bursts — each fires at its own delay, lives ~1.3s, then fades.
            val globalMs = animationTime.value * 3750f
            val burstDurMs = 1350f
            fireworks.forEach { burst ->
                val tBurst = ((globalMs - burst.launchDelayMs) / burstDurMs).coerceIn(0f, 1f)
                if (tBurst > 0f && tBurst < 1f) {
                    val cx = size.width * burst.cxFrac
                    val cy = size.height * burst.cyFrac
                    val maxR = size.minDimension * burst.maxRadiusFrac
                    val gravity = 90f * density

                    val flashAlpha = (1f - tBurst * 5f).coerceIn(0f, 1f)
                    if (flashAlpha > 0f) {
                        val flashR = maxR * 0.7f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = flashAlpha * 0.95f),
                                    burst.color.copy(alpha = flashAlpha * 0.55f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = flashR
                            ),
                            radius = flashR,
                            center = Offset(cx, cy)
                        )
                    }

                    val tailAlpha = ((1f - tBurst) * 1.25f).coerceIn(0f, 1f)
                    val streaks = burst.streakCount
                    val angleOffset = burst.launchDelayMs * 0.0017
                    for (i in 0 until streaks) {
                        val angle = (2 * PI / streaks) * i + angleOffset
                        val rOuter = maxR * tBurst
                        val rInner = maxR * tBurst * 0.62f
                        val dropOuter = gravity * tBurst * tBurst
                        val dropInner = gravity * (tBurst * 0.62f) * (tBurst * 0.62f)
                        val ox = cx + cos(angle).toFloat() * rOuter
                        val oy = cy + sin(angle).toFloat() * rOuter + dropOuter
                        val ix = cx + cos(angle).toFloat() * rInner
                        val iy = cy + sin(angle).toFloat() * rInner + dropInner

                        drawLine(
                            color = burst.color.copy(alpha = tailAlpha * 0.85f),
                            start = Offset(ix, iy),
                            end = Offset(ox, oy),
                            strokeWidth = (3.dp.toPx() * (1f - tBurst * 0.6f)).coerceAtLeast(1f)
                        )
                        drawCircle(
                            color = burst.secondaryColor.copy(alpha = tailAlpha),
                            radius = (1.5.dp.toPx() + 4.dp.toPx() * (1f - tBurst)).coerceAtLeast(0.5f),
                            center = Offset(ox, oy)
                        )
                    }
                }
            }

            // Heartbeat pulse rings — emanate from the crown's screen position.
            val heartCenter = Offset(size.width / 2f, size.height / 2f - 80.dp.toPx())
            val heartColor = Color(0xFFFFD700)
            if (heartbeatRingLub.value > 0f && heartbeatRingLub.value < 1f) {
                val t1 = heartbeatRingLub.value
                val baseR = 90.dp.toPx()
                val grow = 140.dp.toPx()
                drawCircle(
                    color = heartColor.copy(alpha = ((1f - t1) * 0.55f).coerceAtLeast(0f)),
                    radius = baseR + grow * t1,
                    center = heartCenter,
                    style = Stroke(width = (3.dp.toPx() * (1f - t1 * 0.7f)).coerceAtLeast(0.5f))
                )
            }
            if (heartbeatRingDub.value > 0f && heartbeatRingDub.value < 1f) {
                val t2 = heartbeatRingDub.value
                val baseR = 100.dp.toPx()
                val grow = 230.dp.toPx()
                drawCircle(
                    color = heartColor.copy(alpha = ((1f - t2) * 0.75f).coerceAtLeast(0f)),
                    radius = baseR + grow * t2,
                    center = heartCenter,
                    style = Stroke(width = (5.dp.toPx() * (1f - t2 * 0.6f)).coerceAtLeast(0.5f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            heartColor.copy(alpha = ((1f - t2) * 0.22f).coerceAtLeast(0f)),
                            Color.Transparent
                        ),
                        center = heartCenter,
                        radius = baseR + grow * t2
                    ),
                    radius = baseR + grow * t2,
                    center = heartCenter
                )
            }

            if (time > 0f) {
                particles.forEach { particle ->
                    val gravity = 1800f * density
                    val x = center.x + (particle.velocity.x * density * time)
                    val y = center.y + (particle.velocity.y * density * time) + (0.5f * gravity * time * time)
                    val particleAlpha = (1f - (time / 2.5f)).coerceIn(0f, 1f)

                    if (particleAlpha > 0f) {
                        rotate(degrees = particle.rotationSpeed * time * 120f, pivot = Offset(x, y)) {
                            when (particle.type) {
                                ParticleType.CIRCLE -> drawCircle(
                                    color = particle.color,
                                    center = Offset(x, y),
                                    radius = particle.size * density * particleAlpha,
                                    alpha = particleAlpha
                                )
                                ParticleType.SQUARE -> drawRect(
                                    color = particle.color,
                                    topLeft = Offset(x - particle.size, y - particle.size),
                                    size = Size(particle.size * 2, particle.size * 2),
                                    alpha = particleAlpha
                                )
                                ParticleType.SHARD -> drawLine(
                                    color = particle.color,
                                    start = Offset(x, y),
                                    end = Offset(x + particle.velocity.x * 0.06f, y + particle.velocity.y * 0.06f),
                                    strokeWidth = particle.size * density * 0.6f,
                                    alpha = particleAlpha
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = (-80).dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        val pulse = prPulse.value
                        val heart = heartbeatScale.value
                        scaleX = iconScale.value * pulse * heart
                        scaleY = iconScale.value * pulse * heart
                        rotationZ = iconRotation.value
                    }
            ) {
                val w = size.width
                val h = size.height
                val centerX = w / 2
                val centerY = h / 2

                if (ornamentProgress.value > 0f) {
                    val ornamentCount = 16
                    val maxRadius = w * 0.5f
                    val baseRadius = w * 0.3f
                    val burstGlow = raysGlowBurst.value
                    val rayAccentColor = Color(0xFFFFD700)

                    // Massive radial halo on initial burst — blooms from center and fades
                    if (burstGlow > 0f) {
                        val glowRadius = w * (0.45f + 1.15f * burstGlow)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    rayAccentColor.copy(alpha = burstGlow * 0.72f),
                                    rayAccentColor.copy(alpha = burstGlow * 0.28f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = Offset(centerX, centerY)
                        )
                    }

                    rotate(degrees = animationTime.value * 20f) {
                        for (i in 0 until ornamentCount) {
                            val angle = (2 * PI / ornamentCount) * i
                            // Per-ray staggered progress — rays cascade out one by one
                            val rawRayProgress = ornamentProgress.value * ornamentCount.toFloat() - i
                            val rayProgress = rawRayProgress.coerceIn(0f, 1f)
                            if (rayProgress <= 0f) continue

                            val currentRadius = baseRadius + (maxRadius - baseRadius) * rayProgress

                            val startX = centerX + cos(angle).toFloat() * baseRadius
                            val startY = centerY + sin(angle).toFloat() * baseRadius
                            val endX = centerX + cos(angle).toFloat() * currentRadius
                            val endY = centerY + sin(angle).toFloat() * currentRadius

                            val spikePath = Path().apply {
                                moveTo(startX, startY)
                                lineTo(endX, endY)
                                lineTo(
                                    centerX + cos(angle + 0.1).toFloat() * (baseRadius + 10f),
                                    centerY + sin(angle + 0.1).toFloat() * (baseRadius + 10f)
                                )
                                close()
                            }

                            val spikeBrush = Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))

                            drawPath(path = spikePath, brush = spikeBrush, alpha = rayProgress)

                            // Tip orb — massively inflated on burst then settles to a small dot
                            val tipBaseRadius = 3.dp.toPx() * rayProgress
                            val tipBurstRadius = tipBaseRadius + 18.dp.toPx() * burstGlow * rayProgress

                            if (burstGlow > 0f) {
                                drawCircle(
                                    color = rayAccentColor.copy(alpha = burstGlow * 0.45f * rayProgress),
                                    radius = tipBurstRadius * 2.4f,
                                    center = Offset(endX, endY)
                                )
                            }
                            drawCircle(
                                color = rayAccentColor.copy(alpha = (rayProgress * (0.9f + 0.1f * burstGlow)).coerceIn(0f, 1f)),
                                radius = tipBurstRadius.coerceAtLeast(tipBaseRadius),
                                center = Offset(endX, endY)
                            )
                        }
                    }
                }

                // Crown entrance glow halo — flares on crown appear, decays to nothing
                if (crownEntranceGlow.value > 0f) {
                    val g = crownEntranceGlow.value
                    val crownGlowColor = Color(0xFFFFD700)
                    val glowRadius = w * (0.52f + 0.9f * g)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                crownGlowColor.copy(alpha = g * 0.62f),
                                crownGlowColor.copy(alpha = g * 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                val crownPath = Path().apply {
                    val cw = w * 0.6f
                    val ch = h * 0.6f
                    val ox = (w - cw) / 2
                    val oy = (h - ch) / 2 + (h * 0.1f)

                    moveTo(ox + cw * 0.2f, oy + ch * 0.7f)
                    lineTo(ox + cw * 0.8f, oy + ch * 0.7f)
                    lineTo(ox + cw * 0.9f, oy + ch * 0.3f)
                    lineTo(ox + cw * 0.65f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.5f, oy + ch * 0.15f)
                    lineTo(ox + cw * 0.35f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.1f, oy + ch * 0.3f)
                    close()
                }

                val crownBrush = Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))

                drawPath(path = crownPath, brush = crownBrush)

                // Diagonal shine sweep across the crown — clipped to the crown shape.
                val shineT = crownShine.value
                if (shineT > -0.2f && shineT < 1.2f) {
                    val bandX = -w * 0.4f + w * 1.8f * shineT
                    val bandHalfWidth = w * 0.18f
                    clipPath(crownPath) {
                        translate(left = bandX, top = 0f) {
                            rotate(degrees = 18f, pivot = Offset(0f, h / 2f)) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.0f),
                                            Color.White.copy(alpha = 0.95f),
                                            Color.White.copy(alpha = 0.0f),
                                            Color.Transparent
                                        ),
                                        startX = -bandHalfWidth,
                                        endX = bandHalfWidth
                                    ),
                                    topLeft = Offset(-bandHalfWidth, -h * 0.3f),
                                    size = Size(bandHalfWidth * 2f, h * 1.6f)
                                )
                            }
                        }
                    }
                }

                drawPath(
                    path = crownPath,
                    style = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Round),
                    color = Color(0xFFFFF8E1)
                )
            }
        }

        Text(
            text = styledText,
            modifier = Modifier
                .offset(y = 60.dp)
                .blur(radius = textBlur.value.dp)
                .graphicsLayer {
                    val pulse = prPulse.value
                    scaleX = textScale.value * pulse
                    scaleY = textScale.value * pulse
                    alpha = textAlpha.value
                },
            textAlign = TextAlign.Center,
            lineHeight = 55.sp,
            style = TextStyle(
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
            )
        )

        Text(
            text = badgeTitle,
            modifier = Modifier
                .offset(y = 160.dp)
                .graphicsLayer {
                    alpha = prTextAlpha.value
                    scaleX = prTextScale.value * prPulse.value
                    scaleY = prTextScale.value * prPulse.value
                },
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = Color(0xFFFFD700)
        )
    }
}

private fun generateParticles(
    colors: List<Color>,
    count: Int = 800
): List<Particle> {
    val rng = Random(System.currentTimeMillis())
    return List(count) {
        val angle = rng.nextDouble(0.0, 2 * PI)
        val speed = rng.nextFloat() * 1600f + 700f

        val vx = cos(angle).toFloat() * speed * rng.nextFloat()
        val vy = sin(angle).toFloat() * speed * rng.nextFloat() - 1200f

        Particle(
            color = colors.random(rng),
            velocity = Offset(vx, vy),
            size = rng.nextFloat() * 10f + 4f,
            rotationSpeed = (rng.nextFloat() - 0.5f) * 15f,
            type = ParticleType.entries.toTypedArray().random(rng)
        )
    }
}
