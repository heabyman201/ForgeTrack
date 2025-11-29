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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
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
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType.Companion.ItemStart
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.wearable.Node
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
        WorkoutDataSync.init(this)
        setContent {
            WorkoutAppWear()
        }
    }
    override fun onDestroy() {
        WorkoutDataSync.shutdown(this)
        super.onDestroy()
    }
}

object WorkoutDataSync { // Removed Capability Listener (simplified for reliability)
    private const val TAG = "WorkoutDataSync"
    private const val WORKOUT_STATE_PATH = "/workout_state"
    private const val HR_PATH = "/hr"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Helper to get all connected nodes (Phone, etc.)
    private suspend fun getConnectedNodes(context: Context): List<Node> {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun init(context: Context) {
        // No complex init needed for this direct broadcast approach
    }

    fun shutdown(context: Context) {
        // Cleanup if needed
    }

    // Now uses MessageClient for Instant updates
    fun sendHeartRate(context: Context, bpm: Int) {
        val payload = bpm.toString().toByteArray(Charsets.UTF_8)
        scope.launch {
            val nodes = getConnectedNodes(context)
            nodes.forEach { node ->
                try {
                    Wearable.getMessageClient(context).sendMessage(node.id, HR_PATH, payload)
                        .await()
                    Log.d(TAG, "Sent HR $bpm to ${node.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send HR to ${node.displayName}", e)
                }
            }
        }
    }

    fun sendWorkoutState(context: Context) {
        val payload = ConnectedWorkout.toSyncString().toByteArray(Charsets.UTF_8)
        scope.launch {
            val nodes = getConnectedNodes(context)
            nodes.forEach { node ->
                try {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, WORKOUT_STATE_PATH, payload).await()
                    Log.d(TAG, "Sent State to ${node.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send State to ${node.displayName}", e)
                }
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

val extendedWorkoutPresets = listOf(
    "Bench Press", "Squat", "Deadlift", "Overhead Press",
    "Pull Up", "Push Up", "Bicep Curl", "Tricep Extension",
    "Leg Press", "Lat Pulldown", "Running (Treadmill)", "Stationary Bike",
    "Plank", "Crunches", "Dumbbell Row", "Barbell Row",
    "Incline Bench Press", "Dips", "Chin Up", "Romanian Deadlift",
    "Lateral Raise", "Face Pull", "Hammer Curl", "Skullcrushers",
    "Walking Lunge", "Leg Extension", "Leg Curl", "Calf Raise",
    "Seated Cable Row", "Dumbbell Press", "Hack Squat", "Front Squat",
    "Preacher Curl", "Cable Fly", "Russian Twist", "Hanging Leg Raise",
    "Jump Rope", "Rowing Machine", "Elliptical", "Cycling (Outdoor)"
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
            anchorType = ItemStart,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 30.dp, bottom = 30.dp)
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Select Workout",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center
                    )
                }
            }
            items(extendedWorkoutPresets) { workout ->
                Chip(
                    onClick = { onWorkoutSelected(workout) },
                    label = {
                        Text(
                            text = workout,
                            maxLines = 1,
                            style = MaterialTheme.typography.body2
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(0.9f)
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
    val isCardio = workoutName().contains("Running") || workoutName().contains("Bike") || workoutName().contains("Cycling")
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
            anchorType = ItemStart,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 60.dp)
        ) {
            item {
                ListHeader {
                    Text(
                        text = workoutName(),
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            if (!isCardio) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CompactChip(
                            onClick = { onGoalTypeChange("Reps") },
                            label = { Text("Reps") },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (goalType() == "Reps") MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                                contentColor = if (goalType() == "Reps") MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        CompactChip(
                            onClick = { onGoalTypeChange("Time") },
                            label = { Text("Time") },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (goalType() == "Time") MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                                contentColor = if (goalType() == "Time") MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                            )
                        )
                    }
                }
            }

            when (goalType()) {
                "Reps" -> {
                    item {
                        Stepper(
                            value = goalSets(),
                            onValueChange = {
                                onGoalSetsChange(it)
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            valueProgression = 1..50,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SETS", style = MaterialTheme.typography.caption2, color = Color.Gray)
                                Text(goalSets().toString(), style = MaterialTheme.typography.title2)
                            }
                        }
                    }
                    item {
                        Stepper(
                            value = goalReps(),
                            onValueChange = {
                                onGoalRepsChange(it)
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            valueProgression = 1..100,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REPS", style = MaterialTheme.typography.caption2, color = Color.Gray)
                                Text(goalReps().toString(), style = MaterialTheme.typography.title2)
                            }
                        }
                    }
                    item {
                        Stepper(
                            value = currentWeight(),
                            onValueChange = {
                                onCurrentWeightChange(it)
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            valueRange = 0f..500f,
                            steps = 199,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WEIGHT", style = MaterialTheme.typography.caption2, color = Color.Gray)
                                Text("${String.format("%.1f", currentWeight())} kg", style = MaterialTheme.typography.body1)
                            }
                        }
                    }
                }
                "Time" -> {
                    item {
                        val minutes = (goalTime() / 60000).toInt()
                        Stepper(
                            value = minutes,
                            onValueChange = {
                                onGoalTimeChange(it * 60000L)
                                WorkoutDataSync.sendWorkoutState(context)
                            },
                            valueProgression = 1..180,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MINUTES", style = MaterialTheme.typography.caption2, color = Color.Gray)
                                Text(minutes.toString(), style = MaterialTheme.typography.title2)
                            }
                        }
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
                            valueRange = 1f..500f,
                            steps = 499,
                            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
                            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("KM", style = MaterialTheme.typography.caption2, color = Color.Gray)
                                Text(String.format("%.1f", goalDistance()), style = MaterialTheme.typography.title2)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                ) {
                    Text("START", fontWeight = FontWeight.Bold)
                }
            }
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
    var heartRate by remember { mutableIntStateOf(0) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val hrSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }

    DisposableEffect(isPaused, hrSensor) {
        if (!isPaused && hrSensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.values.isNotEmpty()) heartRate = event.values[0].toInt().coerceAtLeast(0)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(heartRate) {
        if (heartRate > 0) WorkoutDataSync.sendHeartRate(context, heartRate)
    }

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
        if (workoutState.goalType.value == "Distance") {
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
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 290f,
            endAngle = 250f,
            strokeWidth = 8.dp,
            trackColor = MaterialTheme.colors.surface
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = workoutState.workout.value.uppercase(),
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.secondary,
                maxLines = 1
            )

            AnimatedContent(targetState = workoutState.goalType.value, label = "workout_metric") { type ->
                when (type) {
                    "Reps" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${workoutState.currentReps.intValue}",
                                    style = MaterialTheme.typography.display3,
                                    color = MaterialTheme.colors.primary
                                )
                                Text(
                                    text = "/${workoutState.goalReps.intValue}",
                                    style = MaterialTheme.typography.title3,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = "SET ${workoutState.currentSets.intValue + 1} OF ${workoutState.goalSets.intValue}",
                                style = MaterialTheme.typography.caption1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "Time" -> {
                        val time = workoutState.currentTime.longValue
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.display2,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    "Distance" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.2f", workoutState.currentDistance.doubleValue),
                                style = MaterialTheme.typography.display3,
                                color = MaterialTheme.colors.primary
                            )
                            Text("KILOMETERS", style = MaterialTheme.typography.caption2)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                    contentDescription = "Heart Rate",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("$heartRate", style = MaterialTheme.typography.title3)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (workoutState.goalType.value == "Reps") {
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
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text("+1", style = MaterialTheme.typography.title2, fontWeight = FontWeight.Black)
                    }
                } else {
                    CompactChip(
                        onClick = { isPaused = !isPaused },
                        label = { Text(if (isPaused) "RESUME" else "PAUSE") },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }

                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "End"
                    )
                }
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
        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            trackColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
            indicatorColor = MaterialTheme.colors.secondary
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("REST", style = MaterialTheme.typography.title3, color = MaterialTheme.colors.secondary)
            Text(
                "${TimeUnit.MILLISECONDS.toSeconds(remainingTime) + 1}",
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text("SECONDS", style = MaterialTheme.typography.caption2, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            CompactChip(onClick = onFinishRest, label = { Text("SKIP") })
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
