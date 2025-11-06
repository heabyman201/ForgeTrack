package com.forgecompose.workouttracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.GoalReps
import com.forgecompose.workouttracker.ConnectedWorkout.GoalSets
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.restTimeRemaining
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RestScreen(
    navController: NavController,
    vm: HrViewModel = viewModel()
) {
    val haptics = LocalHapticFeedback.current
    val bpm by vm.hr.collectAsState()
    val initialTotal = rememberSaveable { 60000L }
    PreventBackGesture()
    var remaining by restTimeRemaining

    LaunchedEffect(Unit) {
        remaining = initialTotal
        while (remaining > 0) {
            delay(1000)
            remaining -= 1000
        }
        if (remaining <= 0) {
            ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
            navController.navigate("WorkoutScreen") { popUpTo("WorkoutScreen") { inclusive = true } }
        }
    }

    val progress = (1f - (remaining.toFloat() / initialTotal.toFloat())).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "p"
    )

    val seconds = (remaining / 1000) % 60
    val minutes = (remaining / (1000 * 60)) % 60
    val hours = (remaining / (1000 * 60 * 60))

    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else      -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(if (showIntro) 0f else 1f, tween(2000, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

    var animationClock by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (isActive) {
            val currentTime = withFrameNanos { it }
            if (lastFrameTime != 0L) {
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                animationClock += deltaTime
            }
            lastFrameTime = currentTime
            delay(42)
        }
    }

    val gradientOffset = 0.5f + 0.5f * sin(animationClock * 2f * PI.toFloat() / 22f)
    val glow = 0.525f + 0.175f * sin(animationClock * 2f * PI.toFloat() / 16f)

    val currentSetCount = CurrentSets.intValue.coerceAtLeast(1)

    LaunchedEffect(currentSetCount) {
        val repsPerSet = (GoalReps.intValue / GoalSets.intValue.coerceAtLeast(1)).toString()
        val weightPerSet = String.format("%.1f", CurrentWeight.value)
        while (WorkoutLog.sets.size < currentSetCount) {
            WorkoutLog.sets.add(
                SetRecord(
                    reps = mutableStateOf(repsPerSet),
                    weight = mutableStateOf(weightPerSet)
                )
            )
        }
    }

    val totalReps by remember {
        derivedStateOf {
            WorkoutLog.sets.sumOf { it.reps.value.toIntOrNull() ?: 0 }
        }
    }

    LaunchedEffect(totalReps) {
        CurrentReps.intValue = totalReps
    }

    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )
    WorkoutTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(
                    blurAnim
                )
                .drawWithContent {
                    val bg = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3636A4),
                            Color(0xFF2E3786).copy(alpha = 0.90f + gradientOffset * 0.02f),
                            Color(0xFF000000)
                        ),
                        radius = 1600f + gradientOffset * 600f,
                        center = Offset(size.width * 0.45f, size.height * 0.82f)
                    )
                    drawRect(bg)
                    if (introProgress < 1f) drawRect(
                        brush = Brush.radialGradient(introColors, radius = 1200f, center = Offset(size.width * 0.4f, size.height * 0.28f)),
                        alpha = 1f - introProgress
                    )
                    drawContent()
                }
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))

                val ringSize = 300.dp

                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f
                        val stroke = 18f
                        val radius = min(w, h) / 2f - stroke

                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF0B1117), Color(0xFF0E1620)),
                                center = center,
                                radius = radius * 1.2f
                            ),
                            radius = radius,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        val sweep = 360f * animatedProgress
                        val arcRect = Rect(
                            Offset(cx - radius, cy - radius),
                            Size(radius * 2, radius * 2)
                        )
                        val arcBrush = Brush.sweepGradient(
                            0f to Color(0xFF4CA3FF),
                            0.35f to Color(0xFF66D4FF),
                            0.7f to Color(0xFF9BE7FF),
                            1f to Color(0xFF4CA3FF),
                            center = center
                        )
                        drawArc(
                            brush = arcBrush,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                            topLeft = arcRect.topLeft,
                            size = arcRect.size
                        )

                        drawArc(
                            color = Color(0xFF7BD1FF).copy(alpha = 0.18f + 0.12f * glow),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke * 1.6f, cap = StrokeCap.Round),
                            topLeft = arcRect.topLeft,
                            size = arcRect.size
                        )

                        if (animatedProgress > 0f) {
                            val capAngle = Math.toRadians((sweep - 90).toDouble()).toFloat()
                            val px = cx + cos(capAngle) * radius
                            val py = cy + sin(capAngle) * radius
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFFB2EBFF), Color.Transparent),
                                    center = Offset(px, py),
                                    radius = 26f
                                ),
                                radius = 26f * (0.7f + 0.3f * glow),
                                center = Offset(px, py),
                                alpha = 0.8f
                            )
                            drawCircle(
                                color = Color(0xFFCCF4FF),
                                radius = 6f,
                                center = Offset(px, py)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "REST",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF9BE7FF).copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format("%02d:%02d:%02d", hours, minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                var isExpanded by remember { mutableStateOf(false) }
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "expand_icon")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 32.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.LightGray.copy(alpha = 0.02f)
                    ) {}

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .background(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded;
                                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)}
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Workout Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.rotate(rotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    itemsIndexed(WorkoutLog.sets) { index, record ->
                                        EditableSetRow(
                                            setNumber = index + 1,
                                            record = record
                                        )
                                        if (index < WorkoutLog.sets.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                thickness = (0.5).dp,
                                                color = Color.White.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                Spacer(Modifier.weight(1f))

                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "btnScale")
                val elevation by animateDpAsState(if (pressed) 2.dp else 8.dp, label = "btnElev")

                val backdrop = rememberLayerBackdrop()
                val uiSensor = rememberUISensor()
                val progressAnimation = remember { Animatable(0f) }
                val isPressed by interactionSource.collectIsPressedAsState()
                LaunchedEffect(isPressed) {
                    val spec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)
                    progressAnimation.animateTo(if (isPressed) 1f else 0f, spec)
                }

                val buttonShape = RoundedCornerShape(32.dp)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .clip(buttonShape)
                        .graphicsLayer {
                            val liquidScale = lerp(1f, 1.1f, progressAnimation.value)
                            scaleX = scale * liquidScale
                            scaleY = scale * liquidScale
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { buttonShape },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                refraction(height = 24f.dp.toPx(), amount = 48f.dp.toPx(), hasDepthEffect = true)
                            },

                            highlight = { Highlight(style = HighlightStyle.Default(angle = uiSensor.gravityAngle)) }
                        ),



                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                            navController.navigate("WorkoutScreen") {
                                popUpTo("WorkoutScreen") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        interactionSource = interactionSource
                    ) {
                        Text("Skip Rest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}