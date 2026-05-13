package com.forgecompose.workouttracker.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forgecompose.workouttracker.AppearanceOptionsAppTheme
import com.forgecompose.workouttracker.AppearanceOptionsManagerAppTheme
import com.forgecompose.workouttracker.ConnectedWorkout
import com.forgecompose.workouttracker.Particle
import com.forgecompose.workouttracker.ParticleType
import com.forgecompose.workouttracker.WorkoutCompletionSummary
import com.forgecompose.workouttracker.WorkoutCompletionSummaryScreen

import com.forgecompose.workouttracker.drawWorkoutBackdropOrbs
import com.forgecompose.workouttracker.generateParticles

import com.forgecompose.workouttracker.generateWorkoutBackdropOrbs

import com.forgecompose.workouttracker.workout.PrFlags
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
val goldenParticleColors = listOf(
    Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFE082),
    Color(0xFFFFF8E1), Color(0xFFFFB300), Color.White
)

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
        // repeat = -1 → play the pattern once (which already contains HEARTBEAT_CYCLES cycles), then stop.
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
fun GoalCompletionAnimation(
    onAnimationFinished: () -> Unit,
    onFinishAnimation: () -> Unit,
    summary: WorkoutCompletionSummary,
    prFlags: PrFlags,
    estimatedRpe: Int
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

    val sets = remember { ConnectedWorkout.CurrentSets.intValue.coerceAtLeast(1) }
    val reps = remember { ConnectedWorkout.CurrentReps.intValue.coerceAtLeast(1) }

    val prShockwave = remember { Animatable(0f) }
    val prFlash = remember { Animatable(0f) }
    val prPulse = remember { Animatable(1f) }
    val prTextAlpha = remember { Animatable(0f) }
    val prTextScale = remember { Animatable(0.9f) }
    val raysGlowBurst = remember { Animatable(0f) }
    val crownEntranceGlow = remember { Animatable(0f) }
    val crownShine = remember { Animatable(-0.3f) }
    val fireworkTime = remember { Animatable(0f) }
    val textBlur = remember { Animatable(34f) }
    val heartbeatScale = remember { Animatable(1f) }
    val heartbeatRingLub = remember { Animatable(1f) }
    val heartbeatRingDub = remember { Animatable(1f) }

    val vibrator = remember(context) { getSystemVibrator(context) }
    var showFinishButton by remember { mutableStateOf(false) }
    var showSummaryScreen by remember { mutableStateOf(false) }

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
        particles = if (prFlags.any) {
            generateParticles(goldenParticleColors, count = 1100)
        } else {
            generateParticles(
                listOf(
                    theme.primary, theme.secondary,
                    theme.primary.copy(alpha = 0.7f), theme.secondary.copy(alpha = 0.6f),
                    Color.White, Color.White.copy(alpha = 0.8f)
                ),
                count = 600
            )
        }

        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope {
            launch {
                animationTime.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3750, easing = LinearEasing)
                )
                onAnimationFinished()
                showFinishButton = true
            }
            launch {
                lightBurst.animateTo(1f, tween(80, easing = FastOutSlowInEasing))
                lightBurst.animateTo(0f, tween(600))
            }

            if (prFlags.any) {
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
                    delay(700)
                    prTextAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(700)
                    prTextScale.snapTo(0.75f)
                    prTextScale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 380f))
                }
                launch {
                    delay(80)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(80)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            // Cause → effect → confirmation cascade.
            // Rays fire FIRST — they're the kinetic event that summons the crown.
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

            // Crown stamps in ~60ms after the rays — the "effect".
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

            // Text begins to materialize ~60ms after the stamp — the "confirmation".
            // Its long blur dissolve means it's still the last thing to settle.
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

            // Crown shine — diagonal highlight sweeps across the crown twice,
            // landing after the stamp has visually settled.
            launch {
                delay(420)
                crownShine.snapTo(-0.3f)
                crownShine.animateTo(1.3f, tween(820, easing = FastOutSlowInEasing))
                delay(380)
                crownShine.snapTo(-0.3f)
                crownShine.animateTo(1.3f, tween(720, easing = FastOutSlowInEasing))
            }

            // Fireworks: drive a single global time and let each burst clip to its own window.
            launch {
                fireworkTime.animateTo(1f, tween(durationMillis = 3500, easing = LinearEasing))
            }
            fireworks.forEach { burst ->
                launch {
                    delay(burst.launchDelayMs)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    // Heartbeat pulse — kicks in once the crown has stamped and starts to "live".
    // Cancels itself if the user advances to the summary screen, and always
    // cancels the underlying vibration when the composable leaves composition.
    DisposableEffect(vibrator) {
        onDispose { vibrator?.cancel() }
    }
    LaunchedEffect(showSummaryScreen) {
        if (showSummaryScreen) {
            vibrator?.cancel()
            return@LaunchedEffect
        }
        try {
            // Wait for "WORKOUT COMPLETE" to actually appear (text alpha + blur resolve around ~1.6s).
            delay(1700)
            startHeartbeatVibration(vibrator)
            repeat(HEARTBEAT_CYCLES) {
                // LUB — quick, smaller bump + small expanding ring
                launch {
                    heartbeatRingLub.snapTo(0f)
                    heartbeatRingLub.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
                }
                heartbeatScale.animateTo(1.06f, tween(55, easing = FastOutSlowInEasing))
                heartbeatScale.animateTo(1.0f, tween(90, easing = FastOutSlowInEasing))
                // DUB — stronger, bigger ring
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

    val mainText = "WORKOUT\nCOMPLETE"

    val prLabel = remember(prFlags) {
        when {
            prFlags.strengthPr && prFlags.volumePr -> "DOUBLE PR"
            prFlags.strengthPr -> "NEW STRENGTH PR"
            prFlags.volumePr -> "NEW VOLUME PR"
            prFlags.repsPr && prFlags.setsPr -> "RECORDS SHATTERED"
            prFlags.repsPr -> "REP RECORD"
            prFlags.setsPr -> "SET RECORD"
            else -> "NEW PR"
        }
    }

    val backdropOrbs = remember {
        generateWorkoutBackdropOrbs(count = 8)
    }

    val finishButtonAlpha by animateFloatAsState(
        targetValue = if (showFinishButton) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "goal_completion_finish_button_alpha"
    )

    AnimatedContent(
        targetState = showSummaryScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(animationSpec = tween(320)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320))) togetherWith
                    (fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 1.02f, animationSpec = tween(220)))
        },
        label = "goal_completion_stage"
    ) { isSummaryVisible ->
        if (isSummaryVisible) {
            WorkoutCompletionSummaryScreen(
                summary = summary,
                estimatedRpe = estimatedRpe,
                primaryColor = theme.primary,
                secondaryColor = theme.secondary,
                backgroundColor = theme.background,
                onContinue = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFinishAnimation()
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawWorkoutBackdropOrbs(backdropOrbs, size, theme)
                    val time = animationTime.value * 2.5f

                    if (lightBurst.value > 0f) {
                        drawCircle(
                            color = theme.primary.copy(alpha = lightBurst.value * 0.5f),
                            radius = size.maxDimension * lightBurst.value,
                            center = center
                        )
                    }

                    if (prFlags.any && prFlash.value > 0f) {
                        drawRect(
                            color = theme.secondary.copy(alpha = 0.18f * prFlash.value),
                            size = size
                        )
                    }

                    if (prFlags.any && prShockwave.value > 0f) {
                        val t = prShockwave.value
                        val r = size.maxDimension * (0.15f + 0.95f * t)
                        val a = (1f - t).coerceIn(0f, 1f)
                        drawCircle(
                            color = theme.primary.copy(alpha = 0.55f * a),
                            radius = r,
                            center = center,
                            style = Stroke(width = (10.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                        )
                    }

                    // Fireworks bursts — each fires at its own delay, lives ~1.3s,
                    // then fades. Drawn behind the confetti so the streamers pop on top.
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
                    val heartCenter = Offset(size.width / 2f, size.height / 2f - 60.dp.toPx())
                    val heartColor = if (prFlags.any) Color(0xFFFFD700) else theme.primary
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
                        // Soft inner glow on the dub for extra "thump" presence
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
                            val gravity = 2000f * density
                            val x = center.x + (particle.velocity.x * density * time)
                            val y = center.y + (particle.velocity.y * density * time) + (0.5f * gravity * time * time)
                            val particleAlpha = (1f - (time / 2.0f)).coerceIn(0f, 1f)

                            if (particleAlpha > 0f) {
                                rotate(degrees = particle.rotationSpeed * time * 100f, pivot = Offset(x, y)) {
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
                                            end = Offset(x + particle.velocity.x * 0.05f, y + particle.velocity.y * 0.05f),
                                            strokeWidth = particle.size * density * 0.5f,
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-60).dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
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
                            val ornamentCount = 8 + (sets * 2).coerceAtMost(24)
                            val maxRadius = (w * 0.4f) + (reps * 2f * density).coerceAtMost(w * 0.3f)
                            val baseRadius = w * 0.25f
                            val burstGlow = raysGlowBurst.value
                            val rayAccentColor = if (prFlags.any) Color(0xFFFFD700) else theme.primary

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

                                    val spikeBrush = if (prFlags.any) {
                                        Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))
                                    } else {
                                        Brush.linearGradient(listOf(theme.primary, theme.secondary))
                                    }

                                    drawPath(path = spikePath, brush = spikeBrush, alpha = rayProgress)

                                    // Tip orb — massively inflated on burst then settles to a small dot
                                    val tipBaseRadius = 3.dp.toPx() * rayProgress
                                    val tipBurstRadius = tipBaseRadius + 18.dp.toPx() * burstGlow * rayProgress

                                    if (burstGlow > 0f) {
                                        // Outer soft glow halo per tip during burst
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
                            val crownGlowColor = if (prFlags.any) Color(0xFFFFD700) else theme.primary
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

                        val crownBrush = if (prFlags.any) {
                            Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))
                        } else {
                            Brush.linearGradient(listOf(theme.primary, theme.secondary, theme.tertiary))
                        }

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
                            style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round),
                            color = if (prFlags.any) Color(0xFFFFF8E1) else theme.primary.copy(alpha = 0.65f)
                        )
                    }
                }

                Text(
                    text = mainText,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 60.dp)
                        .blur(radius = textBlur.value.dp)
                        .graphicsLayer {
                            val pulse = prPulse.value
                            scaleX = textScale.value * pulse
                            scaleY = textScale.value * pulse
                            alpha = textAlpha.value
                        },
                    textAlign = TextAlign.Center,
                    lineHeight = 50.sp,
                    color = theme.primary,
                    style = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                    )
                )

                if (prFlags.any) {
                    Text(
                        text = prLabel,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 140.dp)
                            .graphicsLayer {
                                alpha = prTextAlpha.value
                                scaleX = prTextScale.value * prPulse.value
                                scaleY = prTextScale.value * prPulse.value
                            },
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        ),
                        color = Color(0xFFFFD700)
                    )
                }

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSummaryScreen = true
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primary,
                        contentColor = theme.background
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth()
                        .height(58.dp)
                        .graphicsLayer {
                            alpha = finishButtonAlpha
                            translationY = (1f - finishButtonAlpha) * 48f
                        },
                    enabled = showFinishButton
                ) {
                    Text(
                        text = "View Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}