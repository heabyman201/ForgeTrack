package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentTime
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.GoalDistance
import com.forgecompose.workouttracker.ConnectedWorkout.GoalReps
import com.forgecompose.workouttracker.ConnectedWorkout.GoalSets
import com.forgecompose.workouttracker.ConnectedWorkout.GoalTime
import com.forgecompose.workouttracker.ConnectedWorkout.GoalType
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.GoalSelectionScreen.blurScreen
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.TextStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(navController: NavController, viewModel: WorkoutListViewModel) {
    val workoutState by remember { workout }
    var selectedGoalType by remember(workoutState) {
        mutableStateOf(ConnectedWorkout.workoutGoalTypeMap[workoutState] ?: "Time")
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "buttonScale")
    GoalType = selectedGoalType
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val last by remember(workout.value) {
        PresetStateRepo.observe(ctx, workout.value)
    }.collectAsState(initial = null)

    var showRestTimeDialog by remember { mutableStateOf(false) }

    if (showRestTimeDialog) {
        RestTimeSelectorDialog(
            onDismissRequest = { showRestTimeDialog = false;
                               blurScreen.value = false},
            onConfirm = { newRestTime ->
                ConnectedWorkout.restTime.longValue = newRestTime
                showRestTimeDialog = false
                blurScreen.value = false
            },
            initialRestTimeInMillis = 60000L
        )
    }


    LaunchedEffect(last) {
        if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE && last != null) {
            last?.weightKg?.let { CurrentWeight.value = it.toDouble() }
            last?.goalSets?.let { GoalSets.intValue = it }
            last?.goalTimeMillis?.let { GoalTime.value = it }
        }
    }

    var animationClock by remember { mutableStateOf(0f) }

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

    val period1 = 8f
    val period2 = 6f
    val period3 = 16f

    val intensePulse = 0.65f + 0.35f * sin(animationClock * 2 * PI.toFloat() / period1)
    val glowIntensity = 0.7f + 0.3f * sin(animationClock * 2 * PI.toFloat() / period2)
    val gradientOffset = 0.5f + 0.5f * sin(animationClock * 2 * PI.toFloat() / period3)

    var repsPerSet by remember {
        mutableIntStateOf(GoalReps.intValue.takeIf { it > 0 } ?: 10)
    }
    if (GoalSets.intValue <= 0 && GoalReps.intValue <= 0) {
        repsPerSet = 0
    } else {
        repsPerSet = GoalReps.intValue.takeIf { it > 0 } ?: 10
    }
    val totalGoalReps by derivedStateOf {
        val mode = ConnectedWorkout.currentMode.value
        if (mode == WorkoutMode.INACTIVE && GoalReps.intValue == 0) {
            0
        } else {
            repsPerSet * GoalSets.intValue
        }
    }


    LaunchedEffect(Unit) {
        if (ConnectedWorkout.currentMode.value == WorkoutMode.ACTIVE){
            navController.navigate("WorkoutScreen")
        } else if (ConnectedWorkout.currentMode.value == WorkoutMode.RESTING){
            navController.navigate("RestScreen")
        }
    }
val blurAnimation by animateDpAsState(
    if (blurScreen.value) intensity.value else 0.dp,
    animationSpec = tween(650),
    label = "blur"
)
    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(750, easing = LinearEasing),
        label = "introFade"
    )
    LaunchedEffect(Unit) { showIntro = false }
    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )
    LaunchedEffect(Unit) {
        GoalReps.intValue = 0
        GoalSets.intValue = 0
        GoalTime.value = 0
        GoalDistance.value = 0.0
        CurrentReps.intValue = 0
        CurrentSets.intValue = 0
        CurrentTime.value = 0
        repsPerSet = 0
    }
    WorkoutTrackerTheme {
        val aggressiveGradientBrush = remember(gradientOffset, glowIntensity, intensePulse) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A0808),
                    Color(0xFF4A1515).copy(alpha = 0.9f + gradientOffset * 0.1f),
                    Color(0xFF650000).copy(alpha = 0.8f + glowIntensity * 0.2f),
                    Color(0xFF8B0000).copy(alpha = 0.7f + intensePulse * 0.3f),
                    Color(0xFF0D0404)
                ),
                radius = 1000f + (gradientOffset * 600f),
                center = Offset(
                    0.5f + sin(gradientOffset * PI.toFloat() * 2f) * 0.3f,
                    0.4f + cos(gradientOffset * PI.toFloat() * 1.5f) * 0.2f
                )
            )
        }
        val secondaryGradientBrush = remember(intensePulse, glowIntensity) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8B0000).copy(alpha = 0.3f + intensePulse * 0.4f),
                    Color.Transparent,
                    Color(0xFF4A1515).copy(alpha = 0.2f + glowIntensity * 0.3f),
                    Color.Transparent
                )
            )
        }
        val animatedContainerColor = remember(intensePulse) {
            Color(0xFF0D0404).copy(alpha = 0.8f + intensePulse * 0.1f)
        }
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurAnim)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(brush = aggressiveGradientBrush)
                        drawRect(brush = secondaryGradientBrush)
                        drawRect(color = animatedContainerColor)
                        if (introProgress < 1f) {
                            drawRect(brush = introBrush, alpha = 1f - introProgress)
                        }
                    }
                }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize().blur(
                    blurAnimation
                ),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = workout.value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = {
                                activity?.finish()
                                    context.startActivity(intent

                                    ) }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showRestTimeDialog = true;
                            blurScreen.value = true
                            }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Rest Time Settings",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp))

                    Text(
                        text = if (
                            ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE
                        )"Set Your Goal" else workout.value,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,

                        )
                    Spacer(modifier = Modifier.height(48.dp))

                    GoalSelector(
                        selectedType = selectedGoalType,
                        onTypeSelected = { newType ->
                            selectedGoalType = newType
                            ConnectedWorkout.workoutGoalTypeMap[workout.value] = newType
                        },
                        navController = navController
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    AnimatedContent(
                        targetState = selectedGoalType,
                        label = "GoalTypeAnimation",
                        transitionSpec = {
                            fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 2 }) togetherWith
                                    fadeOut(tween(400)) + slideOutVertically(tween(400), targetOffsetY = { -it / 2 })
                        }
                    ) { targetType ->
                        val animatedDividerModifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .drawBehind {
                                val animatedColor = Color(0xFFFF4444).copy(alpha = 0.2f + glowIntensity * 0.2f)
                                drawLine(
                                    color = animatedColor,
                                    start = Offset(0f, center.y),
                                    end = Offset(size.width, center.y),
                                    strokeWidth = size.height
                                )
                            }
                        if (targetType == "Reps") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WeightSelector()
                                Box(modifier = animatedDividerModifier)
                                RepSelector()
                                Box(modifier = animatedDividerModifier)
                                SetSelector()
                            }
                        } else if (targetType == "Distance") {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box(modifier = animatedDividerModifier)
                                DistanceSelector(
                                    label = "Distance",
                                    value = GoalDistance.value,
                                    onValueChange = { GoalDistance.value = it }
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                WeightSelector()
                                Box(modifier = animatedDividerModifier)
                                TimerSelector(navController)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val animatedButtonContainerColor = remember(intensePulse) {
                        Color(0xFF4A1515).copy(alpha = 0.4f + intensePulse * 0.2f)
                    }
                    val animatedBorderBrush = remember(intensePulse, glowIntensity) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B0000).copy(alpha = 0.8f + intensePulse * 0.2f),
                                Color(0xFFFF8800).copy(alpha = 0.6f + glowIntensity * 0.3f),
                                Color(0xFF650000).copy(alpha = 0.7f)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            when (selectedGoalType) {
                                "Reps" -> if (repsPerSet != 0 && GoalSets.intValue != 0) {
                                    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                                        GoalReps.intValue = totalGoalReps
                                    }
                                    scope.launch {
                                        val roundedWeight = (CurrentWeight.value * 10).roundToInt() / 10f
                                        PresetStateRepo.upsert(
                                            context = ctx,
                                            presetName = workout.value,
                                            weightKg =  roundedWeight,
                                            goalReps = null,
                                            goalSets = GoalSets.intValue,
                                            goalTimeMillis = null
                                        )
                                    }
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                                "Time" -> if (GoalTime.value != 0L) {
                                    scope.launch {
                                        val roundedWeight = (CurrentWeight.value * 10).roundToInt() / 10f
                                        PresetStateRepo.upsert(
                                            context = ctx,
                                            presetName = workout.value,
                                            weightKg = roundedWeight,
                                            goalReps = null,
                                            goalSets = null,
                                            goalTimeMillis = GoalTime.value
                                        )
                                    }
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                                "Distance" -> if (GoalDistance.value != 0.0) {
                                    scope.launch {
                                        PresetStateRepo.upsert(
                                            context = ctx,
                                            presetName = workout.value,
                                            weightKg = CurrentWeight.value.toFloat(),
                                            goalReps = null,
                                            goalSets = null,
                                            goalTimeMillis = null
                                        )
                                    }
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .drawBehind {
                                drawRoundRect(
                                    color = animatedButtonContainerColor,
                                    cornerRadius = CornerRadius(size.height / 2f)
                                )
                                drawRoundRect(
                                    brush = animatedBorderBrush,
                                    cornerRadius = CornerRadius(size.height / 2f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        interactionSource = interactionSource
                    ) {
                        Text(
                            text = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) "Start Workout" else "Resume Workout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,

                            )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun getWrappedValue(value: Int, delta: Int, range: IntRange): Int {
    if (range.isEmpty()) return 0
    val rangeSize = range.last - range.first + 1
    val relativeValue = value - range.first
    val newRelativeValue = relativeValue + delta
    val mod = ((newRelativeValue % rangeSize) + rangeSize) % rangeSize
    return mod + range.first
}

@Composable
fun DraggableTimePicker(
    modifier: Modifier = Modifier,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(value) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetY.value + dragAmount
                        scope.launch { offsetY.snapTo(newOffset) }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragEnd = {
                        val steps = (offsetY.value / itemHeightPx).roundToInt()
                        val target = steps * itemHeightPx
                        scope.launch {
                            offsetY.animateTo(target, animationSpec = spring())
                            val newValue = getWrappedValue(value, -steps, range)
                            if (newValue != value) onValueChange(newValue)
                            offsetY.snapTo(0f)
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetY.animateTo(0f, animationSpec = spring()) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val displayRange = -20..20
        for (i in displayRange) {
            val displayValue = getWrappedValue(value, i, range)
            val verticalOffset = (i * itemHeightPx) + offsetY.value
            val distanceRatio = verticalOffset / itemHeightPx
            val scale = 1f - (abs(distanceRatio) * 0.15f).coerceAtMost(0.4f)
            val alpha = 1f - (abs(distanceRatio) * 0.5f).coerceAtMost(1f)
            val rotationX = -20f * distanceRatio.coerceIn(-2f, 2f)

            Text(
                text = String.format("%02d", displayValue),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        translationY = verticalOffset
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationX = rotationX
                    }
            )
        }
    }
}

@Composable
fun RestTimeSelectorDialog(
    initialRestTimeInMillis: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialHours = (initialRestTimeInMillis / (1000 * 60 * 60)) % 24
    val initialMinutes = (initialRestTimeInMillis / (1000 * 60)) % 60
    val initialSeconds = (initialRestTimeInMillis / 1000) % 60

    var hours by remember { mutableIntStateOf(initialHours.toInt()) }
    var minutes by remember { mutableIntStateOf(initialMinutes.toInt()) }
    var seconds by remember { mutableIntStateOf(initialSeconds.toInt()) }

    val animatedBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B0000).copy(alpha = 0.8f),
                Color(0xFFFF8800).copy(alpha = 0.6f),
                Color(0xFF650000).copy(alpha = 0.7f)
            )
        )
    }
val dialogAlpha by remember {
    mutableStateOf(if (intensity.value == 0.dp) 1f else 0.79f)
}
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D0404).copy(alpha = dialogAlpha)
            ),
            modifier = Modifier.border(1.5.dp, animatedBorderBrush, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Set Rest Time",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DraggableTimePicker(
                        modifier = Modifier.height(120.dp),
                        value = hours,
                        range = 0..23,
                        onValueChange = { hours = it }
                    )
                    Text(":", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    DraggableTimePicker(
                        modifier = Modifier.height(120.dp),
                        value = minutes,
                        range = 0..59,
                        onValueChange = { minutes = it }
                    )
                    Text(":", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    DraggableTimePicker(
                        modifier = Modifier.height(120.dp),
                        value = seconds,
                        range = 0..59,
                        onValueChange = { seconds = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val totalMillis = (hours * 3600 + minutes * 60 + seconds) * 1000L
                            onConfirm(totalMillis)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF650000).copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

object GoalSelectionScreen{
    var blurScreen = mutableStateOf(false)
}