package com.forgecompose.workouttracker.badges

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(Unit) {
        particles = generateParticles(ParticlePalette.GOLDEN, count = 1200)

        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope {
            launch {
                animationTime.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
                )
            }
            launch {
                lightBurst.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
                lightBurst.animateTo(0f, tween(600))
            }

            launch {
                delay(60)
                prFlash.snapTo(1f)
                prFlash.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
            }
            launch {
                delay(80)
                prShockwave.snapTo(0f)
                prShockwave.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                delay(90)
                repeat(3) {
                    prPulse.animateTo(1.15f, tween(150, easing = FastOutSlowInEasing))
                    prPulse.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }
            }
            launch {
                delay(200)
                prTextAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                prTextScale.snapTo(0.85f)
                prTextScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
            }

            launch {
                delay(100)
                iconScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 180f))
            }
            launch {
                delay(100)
                iconRotation.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 130f))
            }
            launch {
                delay(150)
                ornamentProgress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 90f))
            }
            launch {
                delay(250)
                textAlpha.animateTo(1f, tween(400))
            }
            launch {
                delay(250)
                textScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 250f))
            }
            launch {
                delay(100)
                repeat(5) {
                    delay(200)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
        onAnimationFinished()
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
                        scaleX = iconScale.value * pulse
                        scaleY = iconScale.value * pulse
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

                    rotate(degrees = animationTime.value * 30f) {
                        for (i in 0 until ornamentCount) {
                            val angle = (2 * PI / ornamentCount) * i
                            val currentRadius = baseRadius + (maxRadius - baseRadius) * ornamentProgress.value

                            val startX = centerX + cos(angle).toFloat() * baseRadius
                            val startY = centerY + sin(angle).toFloat() * baseRadius
                            val endX = centerX + cos(angle).toFloat() * currentRadius
                            val endY = centerY + sin(angle).toFloat() * currentRadius

                            val spikePath = Path().apply {
                                moveTo(startX, startY)
                                lineTo(endX, endY)
                                lineTo(
                                    centerX + cos(angle + 0.15).toFloat() * (baseRadius + 15f),
                                    centerY + sin(angle + 0.15).toFloat() * (baseRadius + 15f)
                                )
                                close()
                            }

                            val spikeBrush = Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))

                            drawPath(path = spikePath, brush = spikeBrush)

                            drawCircle(
                                color = Color(0xFFFFD700),
                                radius = 4.dp.toPx() * ornamentProgress.value,
                                center = Offset(endX, endY),
                                alpha = ornamentProgress.value
                            )
                        }
                    }
                }

                // Trophy/Badge icon path
                val crownPath = Path().apply {
                    val cw = w * 0.7f
                    val ch = h * 0.7f
                    val ox = (w - cw) / 2
                    val oy = (h - ch) / 2 + (h * 0.05f)

                    moveTo(ox + cw * 0.2f, oy + ch * 0.8f)
                    lineTo(ox + cw * 0.8f, oy + ch * 0.8f)
                    lineTo(ox + cw * 0.9f, oy + ch * 0.2f)
                    lineTo(ox + cw * 0.65f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.5f, oy + ch * 0.1f)
                    lineTo(ox + cw * 0.35f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.1f, oy + ch * 0.2f)
                    close()
                }

                val crownBrush = Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))

                drawPath(path = crownPath, brush = crownBrush)

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
    palette: ParticlePalette,
    count: Int = 800
): List<Particle> {
    val rng = Random(System.currentTimeMillis())

    val colors = when (palette) {
        ParticlePalette.CRIMSON -> listOf(
            Color(0xFFDC143C),
            Color(0xFFD50000),
            Color(0xFFFF1744),
            Color(0xFFB71C1C),
            Color(0xFFFF8A80),
            Color.White
        )
        ParticlePalette.GOLDEN -> listOf(
            Color(0xFFFFD700),
            Color(0xFFFFC107),
            Color(0xFFFFE082),
            Color(0xFFFFF8E1),
            Color(0xFFFFB300),
            Color.White
        )
    }

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
