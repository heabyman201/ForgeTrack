package com.forgecompose.app_wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutAppWear()
        }
    }
}

object WorkoutDataSync {
    private const val TAG = "WorkoutDataSync"
    private const val WORKOUT_STATE_PATH = "/workout_state"
    private const val WEAR_CAPABILITY = "wear_app"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun sendWorkoutState(context: Context) {
        val workoutState = ConnectedWorkout.toSyncString()
        val messageClient = Wearable.getMessageClient(context)
        val capabilityClient = Wearable.getCapabilityClient(context)

        scope.launch {
            try {
                val nodes = capabilityClient.getCapability(WEAR_CAPABILITY, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE).await().nodes
                nodes.firstOrNull()?.let { node ->
                    messageClient.sendMessage(node.id, WORKOUT_STATE_PATH, workoutState.toByteArray(Charsets.UTF_8))
                        .addOnSuccessListener { Log.d(TAG, "State sent successfully") }
                        .addOnFailureListener { e -> Log.e(TAG, "Failed to send state", e) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting capable nodes", e)
            }
        }
    }
}

object ConnectedWorkout {
    enum class WorkoutMode { INACTIVE, ACTIVE, RESTING, COMPLETED }
    var currentMode = mutableStateOf(WorkoutMode.INACTIVE)
    var restTimeRemaining = mutableLongStateOf(45000L)

    var workout = mutableStateOf("Select Workout")
    var goalReps = mutableIntStateOf(10)
    var goalSets = mutableIntStateOf(3)
    var currentReps = mutableIntStateOf(0)
    var currentSets = mutableIntStateOf(0)
    var currentTime = mutableLongStateOf(0L)
    var goalTime = mutableLongStateOf(300000L)
    var goalDistance = mutableDoubleStateOf(1.0)
    var currentDistance = mutableDoubleStateOf(0.0)
    var currentWeight = mutableDoubleStateOf(10.0)
    var goalType = mutableStateOf("Reps")

    fun reset() {
        currentMode.value = WorkoutMode.INACTIVE
        workout.value = "Select Workout"
        goalReps.intValue = 10
        goalSets.intValue = 3
        currentReps.intValue = 0
        currentSets.intValue = 0
        currentTime.longValue = 0L
        goalTime.longValue = 300000L
        goalDistance.doubleValue = 1.0
        currentDistance.doubleValue = 0.0
        currentWeight.doubleValue = 10.0
        goalType.value = "Reps"
        restTimeRemaining.longValue = 45000L
    }

    fun toSyncString(): String {
        return "mode=${currentMode.value.name}," +
                "workout=${workout.value}," +
                "goalReps=${goalReps.intValue}," +
                "goalSets=${goalSets.intValue}," +
                "currentReps=${currentReps.intValue}," +
                "currentSets=${currentSets.intValue}," +
                "currentTime=${currentTime.longValue}," +
                "goalTime=${goalTime.longValue}," +
                "goalDistance=${goalDistance.doubleValue}," +
                "currentDistance=${currentDistance.doubleValue}," +
                "currentWeight=${currentWeight.doubleValue}," +
                "goalType=${goalType.value}"
    }
}

val workoutPresets = listOf(
    "Bench Press", "Squat", "Deadlift", "Overhead Press",
    "Pull Up", "Push Up", "Bicep Curl", "Tricep Extension",
    "Leg Press", "Lat Pulldown", "Running (Treadmill)", "Stationary Bike",
    "Plank", "Crunches", "Dumbbell Row", "Barbell Row"
)

private val wearColorPalette = Colors(
    primary = Color(0xFFE57373),
    primaryVariant = Color(0xFF4A0000),
    secondary = Color(0xFFCF6679),
    background = Color.Black,
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun WorkoutAppWear() {
    val navController = rememberSwipeDismissableNavController()
    val workoutState = remember { ConnectedWorkout }
    val context = LocalContext.current

    MaterialTheme(colors = wearColorPalette) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "selector"
        ) {
            composable("selector") {
                WorkoutSelectorScreen(onWorkoutSelected = {
                    workoutState.workout.value = it
                    WorkoutDataSync.sendWorkoutState(context)
                    navController.navigate("goal_setter")
                })
            }
            composable("goal_setter") {
                GoalSetterScreen(
                    onStart = {
                        workoutState.currentMode.value = ConnectedWorkout.WorkoutMode.ACTIVE
                        WorkoutDataSync.sendWorkoutState(context)
                        navController.navigate("workout")
                    },
                    workoutName = { workoutState.workout.value },
                    goalType = { workoutState.goalType.value },
                    onGoalTypeChange = { workoutState.goalType.value = it },
                    goalSets = { workoutState.goalSets.intValue },
                    onGoalSetsChange = { workoutState.goalSets.intValue = it },
                    goalReps = { workoutState.goalReps.intValue },
                    onGoalRepsChange = { workoutState.goalReps.intValue = it },
                    currentWeight = { workoutState.currentWeight.doubleValue.toFloat() },
                    onCurrentWeightChange = { workoutState.currentWeight.doubleValue = it.toDouble() },
                    goalTime = { workoutState.goalTime.longValue },
                    onGoalTimeChange = { workoutState.goalTime.longValue = it },
                    goalDistance = { workoutState.goalDistance.doubleValue },
                    onGoalDistanceChange = { workoutState.goalDistance.doubleValue = it }
                )
            }
            composable("workout") {
                WorkoutScreen(
                    onFinish = {
                        workoutState.currentMode.value = ConnectedWorkout.WorkoutMode.COMPLETED
                        WorkoutDataSync.sendWorkoutState(context)
                        workoutState.reset()
                        navController.navigate("selector") { popUpTo("selector") { inclusive = true } }
                    },
                    onRest = {
                        workoutState.currentMode.value = ConnectedWorkout.WorkoutMode.RESTING
                        WorkoutDataSync.sendWorkoutState(context)
                        navController.navigate("rest")
                    },
                    workoutState = workoutState
                )
            }
            composable("rest") {
                RestScreen(
                    onFinishRest = {
                        workoutState.currentMode.value = ConnectedWorkout.WorkoutMode.ACTIVE
                        WorkoutDataSync.sendWorkoutState(context)
                        navController.popBackStack()
                    },
                    restTimeProvider = { workoutState.restTimeRemaining.longValue }
                )
            }
        }
    }
}

@Composable
fun WorkoutSelectorScreen(onWorkoutSelected: (String) -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            item {
                Text(
                    text = "Choose Workout",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center
                )
            }
            items(workoutPresets) { workout ->
                Chip(
                    onClick = { onWorkoutSelected(workout) },
                    label = { Text(workout) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}

@Composable
fun GoalSetterScreen(
    onStart: () -> Unit,
    workoutName: () -> String,
    goalType: () -> String,
    onGoalTypeChange: (String) -> Unit,
    goalSets: () -> Int,
    onGoalSetsChange: (Int) -> Unit,
    goalReps: () -> Int,
    onGoalRepsChange: (Int) -> Unit,
    currentWeight: () -> Float,
    onCurrentWeightChange: (Float) -> Unit,
    goalTime: () -> Long,
    onGoalTimeChange: (Long) -> Unit,
    goalDistance: () -> Double,
    onGoalDistanceChange: (Double) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val isCardio = workoutName().contains("Running") || workoutName().contains("Bike")
    val context = LocalContext.current

    LaunchedEffect(isCardio) {
        onGoalTypeChange(if (isCardio) "Distance" else "Reps")
        WorkoutDataSync.sendWorkoutState(context)
    }

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = workoutName(),
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (!isCardio) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CompactChip(
                            onClick = {
                                onGoalTypeChange("Reps")
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            label = { Text("Reps") },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (goalType() == "Reps") MaterialTheme.colors.primary else Color.DarkGray
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        CompactChip(
                            onClick = {
                                onGoalTypeChange("Time")
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            label = { Text("Time") },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (goalType() == "Time") MaterialTheme.colors.primary else Color.DarkGray
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            when (goalType()) {
                "Reps" -> {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Stepper(
                                value = goalSets(), onValueChange = {
                                    onGoalSetsChange(it)
                                    WorkoutDataSync.sendWorkoutState(context)
                                }, valueProgression = 1..50,
                                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") }
                            ) { Text("Sets: ${goalSets()}") }
                            Stepper(
                                value = goalReps(), onValueChange = {
                                    onGoalRepsChange(it)
                                    WorkoutDataSync.sendWorkoutState(context)
                                }, valueProgression = 1..100,
                                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") }
                            ) { Text("Reps: ${goalReps()}") }
                            Stepper(
                                value = currentWeight(), onValueChange = {
                                    onCurrentWeightChange(it)
                                    WorkoutDataSync.sendWorkoutState(context)
                                }, valueRange = 0f..500f, steps = 7,
                                increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                                decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") }
                            ) { Text("Weight: ${String.format("%.1f", currentWeight())} kg") }
                        }
                    }
                }
                "Time" -> {
                    item {
                        val minutes = (goalTime() / 60000).toInt()
                        Stepper(
                            value = minutes, onValueChange = {
                                onGoalTimeChange(it * 60000L)
                                WorkoutDataSync.sendWorkoutState(context)
                            }, valueProgression = 1..120,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") }
                        ) { Text("Time: $minutes min") }
                    }
                }
                "Distance" -> {
                    item {
                        Stepper(
                            value = (goalDistance() * 10).toFloat(),
                            onValueChange = {
                                onGoalDistanceChange(it.toDouble() / 10.0)
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            valueRange = 1f..500f, steps = 9,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") }
                        ) { Text("Dist: ${String.format("%.1f", goalDistance())} km") }
                    }
                }
            }

            item { Button(onClick = onStart, modifier = Modifier.padding(top = 16.dp)) { Text("Start") } }
        }
    }
}


@Composable
fun WorkoutScreen(onFinish: () -> Unit, onRest: () -> Unit, workoutState: ConnectedWorkout) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var isPaused by remember { mutableStateOf(false) }

    val startAt = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var accMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            val now = SystemClock.elapsedRealtime()
            startAt.longValue = now - accMs
            while (true) {
                delay(1000)
                if (!isPaused) {
                    accMs = SystemClock.elapsedRealtime() - startAt.longValue
                    workoutState.currentTime.longValue = accMs
                    WorkoutDataSync.sendWorkoutState(context)
                }
            }
        } else {
            accMs = workoutState.currentTime.longValue
        }
    }

    LaunchedEffect(workoutState.goalType.value) {
        if(workoutState.goalType.value == "Distance"){
            continuousStepDetectionAndDistanceCalculation(
                context = context,
                onStepDetected = {},
                onUpdate = { distanceKm ->
                    workoutState.currentDistance.doubleValue = distanceKm
                    WorkoutDataSync.sendWorkoutState(context)
                }
            )
        }
    }

    val progress by remember {
        derivedStateOf {
            when (workoutState.goalType.value) {
                "Reps" -> {
                    val totalGoal = (workoutState.goalSets.intValue * workoutState.goalReps.intValue).toFloat()
                    val totalCurrent = (workoutState.currentSets.intValue * workoutState.goalReps.intValue + workoutState.currentReps.intValue).toFloat()
                    if (totalGoal > 0) (totalCurrent / totalGoal).coerceIn(0f, 1f) else 0f
                }
                "Time" -> {
                    if (workoutState.goalTime.longValue > 0) {
                        (workoutState.currentTime.longValue.toFloat() / workoutState.goalTime.longValue.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                }
                "Distance" -> {
                    if (workoutState.goalDistance.doubleValue > 0) {
                        (workoutState.currentDistance.doubleValue / workoutState.goalDistance.doubleValue).toFloat().coerceIn(0f, 1f)
                    } else 0f
                }
                else -> 0f
            }
        }
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "progress")

    LaunchedEffect(progress) {
        if (progress >= 1f) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(500)
            onFinish()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = animatedProgress, modifier = Modifier.fillMaxSize(),
            startAngle = 290f, endAngle = 250f, strokeWidth = 6.dp
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text( text = workoutState.workout.value, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1, modifier = Modifier.padding(horizontal = 12.dp)
            )

            AnimatedContent(targetState = workoutState.goalType.value, label="workout_metric") { type ->
                when(type) {
                    "Reps" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${workoutState.currentReps.intValue}/${workoutState.goalReps.intValue}",
                                style = MaterialTheme.typography.display1, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Set: ${workoutState.currentSets.intValue + 1}/${workoutState.goalSets.intValue}",
                                style = MaterialTheme.typography.title3
                            )
                        }
                    }
                    "Time" -> {
                        val time = workoutState.currentTime.longValue
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60
                        Text( String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.display1, fontWeight = FontWeight.Bold
                        )
                    }
                    "Distance" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                String.format("%.2f", workoutState.currentDistance.doubleValue),
                                style = MaterialTheme.typography.display1, fontWeight = FontWeight.Bold
                            )
                            Text("km", style = MaterialTheme.typography.title3)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(workoutState.goalType.value == "Reps") {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            workoutState.currentReps.intValue += 1
                            if (workoutState.currentReps.intValue >= workoutState.goalReps.intValue) {
                                workoutState.currentSets.intValue += 1
                                workoutState.currentReps.intValue = 0
                                if (workoutState.currentSets.intValue < workoutState.goalSets.intValue) {
                                    onRest()
                                } else {
                                    onFinish()
                                }
                            }
                            WorkoutDataSync.sendWorkoutState(context)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                    ) { Text("REP", fontWeight = FontWeight.Bold) }
                } else {
                    Button(onClick = { isPaused = !isPaused }) { Text(if (isPaused) "Resume" else "Pause") }
                }

                CompactChip( onClick = onFinish, label = { Text("End") },
                    colors = ChipDefaults.chipColors( backgroundColor = Color.DarkGray )
                )
            }
        }
    }
}


@Composable
fun RestScreen(onFinishRest: () -> Unit, restTimeProvider: () -> Long) {
    val totalRestTime = restTimeProvider()
    var remainingTime by remember { mutableLongStateOf(totalRestTime) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val startTime = SystemClock.elapsedRealtime()
        while (remainingTime > 0) {
            delay(200)
            val elapsed = SystemClock.elapsedRealtime() - startTime
            remainingTime = (totalRestTime - elapsed).coerceAtLeast(0)
            WorkoutDataSync.sendWorkoutState(context)
        }
        onFinishRest()
    }

    val progress = 1f - (remainingTime.toFloat() / totalRestTime.toFloat())
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "rest_progress", animationSpec = tween(200))

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = animatedProgress, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("REST", style = MaterialTheme.typography.title1)
            Text(
                "${TimeUnit.MILLISECONDS.toSeconds(remainingTime) + 1}s",
                style = MaterialTheme.typography.display1, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onFinishRest) { Text("Skip") }
        }
    }
}

suspend fun continuousStepDetectionAndDistanceCalculation(
    context: Context,
    onStepDetected: () -> Unit,
    onUpdate: (distanceKm: Double) -> Unit
) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) ?: return

    val sensorThread = HandlerThread("StepDetectorThread").apply { start() }
    val sensorHandler = Handler(sensorThread.looper)
    var distanceMeters = 0.0

    val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return
            if (event.values.isNotEmpty() && event.values[0] == 1.0f) {
                distanceMeters += 0.762
                Handler(Looper.getMainLooper()).post {
                    onStepDetected()
                    onUpdate(distanceMeters / 1000.0)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    try {
        val registered = sensorManager.registerListener(
            stepListener, stepDetector,
            SensorManager.SENSOR_DELAY_GAME,
            sensorHandler
        )
        if (registered) {
            awaitCancellation()
        }
    } finally {
        sensorManager.unregisterListener(stepListener)
        sensorThread.quitSafely()
    }
}

