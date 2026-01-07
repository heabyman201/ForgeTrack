package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.compositeOver
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
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(navController: NavController, viewModel: WorkoutListViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(ctx)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val workoutState by remember { workout }
    var selectedGoalType by remember(workoutState) {
        mutableStateOf(ConnectedWorkout.workoutGoalTypeMap[workoutState] ?: "Time")
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "buttonScale")

    LaunchedEffect(selectedGoalType) {
        GoalType = selectedGoalType
    }
    val last by remember(workout.value) {
        PresetStateRepo.observe(ctx, workout.value)
    }.collectAsState(initial = null)

    LaunchedEffect(last) {
        if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE && last != null) {
            last?.weightKg?.let { CurrentWeight.value = it.toDouble() }
            last?.goalSets?.let { GoalSets.intValue = it }
            last?.goalTimeMillis?.let { GoalTime.value = it }

        }
    }

    var showRestTimeDialog by remember { mutableStateOf(false) }

    if (showRestTimeDialog) {
        RestTimeSelectorDialog(
            onDismissRequest = {
                showRestTimeDialog = false
                blurScreen.value = false
            },
            onConfirm = { newRestTime ->
                ConnectedWorkout.restTime.longValue = newRestTime
                showRestTimeDialog = false
                blurScreen.value = false
            },
            initialRestTimeInMillis = 60000L
        )
    }

    var animationClock by remember { mutableFloatStateOf(0f) }
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

    val repsPerSet by remember {
        derivedStateOf {
            if (GoalSets.intValue <= 0 && GoalReps.intValue <= 0) 0
            else GoalReps.intValue.takeIf { it > 0 } ?: 10
        }
    }

    val totalGoalReps by remember {
        derivedStateOf {
            val mode = ConnectedWorkout.currentMode.value
            if (mode == WorkoutMode.INACTIVE && GoalReps.intValue == 0) 0
            else repsPerSet * GoalSets.intValue
        }
    }

    val textBrush = remember(theme) {
        Brush.horizontalGradient(colors = listOf(theme.primary, Color.White.copy(alpha = 0.9f)))
    }
    val introColors = remember(theme) {
        listOf(theme.secondary.copy(alpha = 0.8f), theme.tertiary, theme.background, theme.background)
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(colors = introColors, start = Offset.Zero, end = Offset(Float.POSITIVE_INFINITY, 0f))
    }

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(750, easing = LinearEasing),
        label = "introFade"
    )

    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )

    val blurAnimation by animateDpAsState(
        if (blurScreen.value) intensity.value else 0.dp,
        animationSpec = tween(650),
        label = "blurInner"
    )

    LaunchedEffect(Unit) {
        showIntro = false
        if (ConnectedWorkout.currentMode.value == WorkoutMode.ACTIVE) navController.navigate("WorkoutScreen")
        else if (ConnectedWorkout.currentMode.value == WorkoutMode.RESTING) navController.navigate("RestScreen")

        GoalReps.intValue = 0; GoalSets.intValue = 0; GoalTime.value = 0
        GoalDistance.value = 0.0; CurrentReps.intValue = 0; CurrentSets.intValue = 0; CurrentTime.value = 0
    }


    WorkoutTrackerTheme {
        val performanceOptions by PerformanceOptionsManager.flow(ctx).collectAsState(initial = PerformanceOptions.Defaults)
        val movingEnabled = performanceOptions.movingGradientAndParticles
        val scrollState = rememberScrollState()
        val activity = ctx as? Activity

        Box(modifier = Modifier.fillMaxSize().blur(blurAnim)) {
            AnimatedBackdrop(
                modifier = Modifier.fillMaxSize(),
                introBrush = introBrush,
                introAlpha = 1f - introProgress,
                enableWaves = movingEnabled,
                enableAnimation = movingEnabled
            )

            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize().blur(blurAnimation),
                topBar = {
                    TopAppBar(
                        title = { Text(workout.value, fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = { activity?.finish(); ctx.startActivity(Intent(ctx, MainActivity::class.java)) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showRestTimeDialog = true; blurScreen.value = true }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp))
                    Text(
                        text = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) "Set Your Goal" else workout.value,
                        style = MaterialTheme.typography.displaySmall.copy(brush = textBrush),
                        fontWeight = FontWeight.Bold
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

                    AnimatedContent(targetState = selectedGoalType, label = "GoalTypeAnimation") { targetType ->
                        val animatedDividerModifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .drawWithCache {
                                onDrawBehind {
                                    val glow = 0.7f + 0.3f * sin(animationClock * 2 * PI.toFloat() / 6f)
                                    drawLine(
                                        color = theme.primary.copy(alpha = 0.2f + glow * 0.2f),
                                        start = Offset(0f, center.y),
                                        end = Offset(size.width, center.y),
                                        strokeWidth = size.height
                                    )
                                }
                            }

                        Column(verticalArrangement = Arrangement.spacedBy(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (targetType) {
                                "Reps" -> {
                                    WeightSelector()
                                    Box(modifier = animatedDividerModifier)
                                    RepSelector()
                                    Box(modifier = animatedDividerModifier)
                                    SetSelector()
                                }
                                "Distance" -> {
                                    Box(modifier = animatedDividerModifier)
                                    DistanceSelector(label = "Distance", value = GoalDistance.value, onValueChange = { GoalDistance.value = it })
                                }
                                else -> {
                                    WeightSelector()
                                    Box(modifier = animatedDividerModifier)
                                    TimerSelector(navController)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            when (selectedGoalType) {
                                "Reps" -> if (repsPerSet != 0 && GoalSets.intValue != 0) {
                                    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) GoalReps.intValue = totalGoalReps
                                    scope.launch {
                                        val roundedWeight = CurrentWeight.value.roundToInt()
                                        PresetStateRepo.upsert(
                                            context = ctx,
                                            presetName = workout.value,
                                            weightKg = roundedWeight.toFloat(),
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
                                        val roundedWeight = CurrentWeight.value.roundToInt()
                                        PresetStateRepo.upsert(
                                            context = ctx,
                                            presetName = workout.value,
                                            weightKg = roundedWeight.toFloat(),
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
                                        PresetStateRepo.upsert(ctx, workout.value, CurrentWeight.value.toFloat(), null, null, null)
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
                            .drawWithCache {
                                onDrawBehind {
                                    val intensePulse = 0.65f + 0.35f * sin(animationClock * 2 * PI.toFloat() / 8f)
                                    val glowIntensity = 0.7f + 0.3f * sin(animationClock * 2 * PI.toFloat() / 6f)

                                    val containerColor = theme.secondary.copy(alpha = 0.4f + intensePulse * 0.2f)
                                    val borderBrush = Brush.linearGradient(
                                        colors = listOf(
                                            theme.primary.copy(alpha = 0.8f + intensePulse * 0.2f),
                                            theme.primary.copy(alpha = 0.6f + glowIntensity * 0.3f).compositeOver(Color.White),
                                            theme.secondary.copy(alpha = 0.7f)
                                        )
                                    )

                                    drawRoundRect(color = containerColor, cornerRadius = CornerRadius(size.height / 2f))
                                    drawRoundRect(brush = borderBrush, cornerRadius = CornerRadius(size.height / 2f), style = Stroke(width = 2.dp.toPx()))
                                }
                            },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                    ) {
                        Text(
                            text = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) "Start Workout" else "Resume Workout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
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
    val context = LocalContext.current

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val initialHours = (initialRestTimeInMillis / (1000 * 60 * 60)) % 24
    val initialMinutes = (initialRestTimeInMillis / (1000 * 60)) % 60
    val initialSeconds = (initialRestTimeInMillis / 1000) % 60

    var hours by remember { mutableIntStateOf(initialHours.toInt()) }
    var minutes by remember { mutableIntStateOf(initialMinutes.toInt()) }
    var seconds by remember { mutableIntStateOf(initialSeconds.toInt()) }

    val animatedBorderBrush = remember(theme) {
        Brush.linearGradient(
            colors = listOf(
                theme.primary.copy(alpha = 0.8f),
                theme.secondary.copy(alpha = 0.6f),
                theme.tertiary.copy(alpha = 0.7f)
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
                containerColor = theme.background.copy(alpha = dialogAlpha)
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
                            containerColor = theme.secondary.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

object GoalSelectionScreen {
    var blurScreen = mutableStateOf(false)
}