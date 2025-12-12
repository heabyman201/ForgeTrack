package com.forgecompose.app_wear.presentation

import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.currentMode
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.CurrentReps
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.CurrentSets
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.CurrentTime
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.CurrentWeight
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalReps
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalSets
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalTime
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalType
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.workout
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
class WearWorkoutActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = application as WearApplication
        // Assuming you have the factory setup in your app instance
        val factory = WorkoutListViewModelFactory(application.repository)
        FirebaseApp.initializeApp(this)
        setContent {
            val workoutListViewModel: WorkoutListViewModel = viewModel(factory = factory)
            WearWorkoutApp(workoutListViewModel)
        }
    }
}

@Composable
fun WearWorkoutApp(
    viewModel: WorkoutListViewModel,
) {
    val navController = rememberSwipeDismissableNavController()
    // Service Management
    val mode by currentMode

    WorkoutTrackerTheme {
        Scaffold(
            timeText = { TimeText() },
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "GoalSetup"
            ) {
                composable("GoalSetup") {
                    WearGoalScreen(navController)
                }
                composable("ActiveWorkout") {
                    WearActiveWorkoutScreen(navController, viewModel)
                }
                composable("RestScreen") {
                    WearRestScreen(navController)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// SCREEN 1: GOAL SETUP
// --------------------------------------------------------------------------------

@Composable
fun WearGoalScreen(navController: NavHostController) {
    val listState = rememberScalingLazyListState()
    var selectedType by remember { mutableStateOf("Reps") }

    // Local state
    var setCounter by remember { mutableIntStateOf(GoalSets.intValue) }
    var repCounter by remember { mutableIntStateOf(GoalReps.intValue) }
    var minutesCounter by remember { mutableIntStateOf((GoalTime.value / 60000).toInt().coerceAtLeast(1)) }

    // Weight state
    var weightCounter by remember { mutableDoubleStateOf(CurrentWeight.value) }
    var weightDragAccumulator by remember { mutableFloatStateOf(0f) }

    // Focus state
    val focusRequester = remember { FocusRequester() }
    var isWeightFocused by remember { mutableStateOf(false) }

    // Optimized params
    val autoCenteringParams = remember { AutoCenteringParams(itemIndex = 1) }
    val scalingParams = remember { ScalingLazyColumnDefaults.scalingParams() }

    LaunchedEffect(isWeightFocused) {
        if (isWeightFocused) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = autoCenteringParams,
            scalingParams = scalingParams
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Setup ${workout.value}",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            item {
                ToggleChip(
                    checked = selectedType == "Reps",
                    onCheckedChange = { selectedType = if (it) "Reps" else "Time" },
                    label = { Text("Mode: $selectedType") },
                    toggleControl = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Mode"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedType == "Reps") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (setCounter > 1) setCounter-- },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Sets")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sets", style = MaterialTheme.typography.caption2)
                            Text("$setCounter", style = MaterialTheme.typography.title3)
                        }

                        Button(
                            onClick = { if (setCounter < 10) setCounter++ },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, "Increase Sets")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (repCounter > 1) repCounter-- },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Reps")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reps", style = MaterialTheme.typography.caption2)
                            Text("$repCounter", style = MaterialTheme.typography.title3)
                        }

                        Button(
                            onClick = { if (repCounter < 50) repCounter++ },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, "Increase Reps")
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (minutesCounter > 1) minutesCounter-- },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Time")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mins", style = MaterialTheme.typography.caption2)
                            Text("$minutesCounter", style = MaterialTheme.typography.title3)
                        }

                        Button(
                            onClick = { if (minutesCounter < 120) minutesCounter++ },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, "Increase Time")
                        }
                    }
                }
            }

            // Weight Control (Swipeable + Digital Bezel)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isWeightFocused) 2.dp else 0.dp,
                            color = if (isWeightFocused) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { isWeightFocused = !isWeightFocused }
                        .onRotaryScrollEvent {
                            if (isWeightFocused) {
                                weightDragAccumulator += it.verticalScrollPixels
                                val threshold = 40f
                                if (abs(weightDragAccumulator) >= threshold) {
                                    val steps = (weightDragAccumulator / threshold).toInt()
                                    val change = steps * 0.25
                                    val newValue = (weightCounter + change).coerceIn(0.0, 500.0)
                                    if (newValue != weightCounter) {
                                        weightCounter = newValue
                                        weightDragAccumulator -= steps * threshold
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .focusRequester(focusRequester)
                        .focusable()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (!isWeightFocused) isWeightFocused = true

                                weightDragAccumulator += dragAmount
                                val threshold = 20f
                                if (abs(weightDragAccumulator) >= threshold) {
                                    val steps = (weightDragAccumulator / threshold).toInt()
                                    val change = steps * 0.25
                                    val newValue = (weightCounter + change).coerceIn(0.0, 500.0)
                                    if (newValue != weightCounter) {
                                        weightCounter = newValue
                                        weightDragAccumulator -= steps * threshold
                                    }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Weight", style = MaterialTheme.typography.caption2)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = if (isWeightFocused) 1f else 0.5f)
                        )

                        Text(
                            text = "${((weightCounter * 10).roundToInt() / 10.0)}", // Optimized formatting
                            style = MaterialTheme.typography.title3,
                            color = if (isWeightFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = if (isWeightFocused) 1f else 0.5f)
                        )
                    }

                    Text(
                        text = if (isWeightFocused) "Use Bezel to Adjust" else "Tap to use Bezel",
                        style = MaterialTheme.typography.caption2,
                        color = if (isWeightFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        GoalType = selectedType
                        GoalSets.intValue = setCounter
                        GoalReps.intValue = repCounter
                        GoalTime.value = minutesCounter * 60 * 1000L
                        CurrentWeight.value = weightCounter

                        CurrentReps.intValue = 0
                        CurrentSets.intValue = 0
                        CurrentTime.value = 0L

                        currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
                        navController.navigate("ActiveWorkout")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("START")
                }
            }
        }
    }
}
// --------------------------------------------------------------------------------
// SCREEN 2: ACTIVE WORKOUT
// --------------------------------------------------------------------------------
@Composable
fun WearActiveWorkoutScreen(
    navController: NavHostController,
    viewModel: WorkoutListViewModel
) {
    val listState = rememberScalingLazyListState()
    var isPaused by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Timer Logic - Using primitives
    val startAt = rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var accMs by rememberSaveable { mutableLongStateOf(0L) }

    // Optimization: Only run loop if not paused
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            val now = SystemClock.elapsedRealtime()
            CurrentTime.value = accMs + (now - startAt.longValue)
            delay(500)
        }
    }

    // Optimization: Calculate progress in derivedState to avoid full recomposition
    val progress by remember {
        derivedStateOf {
            if (GoalType == "Reps") {
                if(GoalSets.intValue > 0) CurrentSets.intValue.toFloat() / GoalSets.intValue.toFloat() else 0f
            } else {
                if(GoalTime.value > 0) CurrentTime.value.toFloat() / GoalTime.value.toFloat() else 0f
            }
        }
    }

    // Simplified Layout: Remove Box overlap, put indicator in background if supported or just separate
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
            startAngle = 295f,
            endAngle = 245f,
            strokeWidth = 4.dp,
            indicatorColor = MaterialTheme.colors.secondary
        )

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            autoCentering = AutoCenteringParams(itemIndex = 1)
        ) {
            item {
                Text(
                    text = if (isPaused) "PAUSED" else "ACTIVE",
                    color = if (isPaused) Color.Yellow else MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.caption2
                )
            }

            item {
                // Only this Text recomposes on timer tick
                Text(
                    text = formatMs(CurrentTime.value),
                    style = MaterialTheme.typography.display1,
                    fontWeight = FontWeight.Bold
                )
            }

            if (GoalType == "Reps") {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Set ${CurrentSets.intValue} / ${GoalSets.intValue}",
                            style = MaterialTheme.typography.title3
                        )
                        Text(
                            text = "Reps: ${CurrentReps.intValue}",
                            style = MaterialTheme.typography.body1,
                            color = Color.Gray
                        )
                    }
                }

                item {
                    Chip(
                        onClick = {
                            CurrentSets.intValue += 1
                            CurrentReps.intValue += GoalReps.intValue
                            if (CurrentSets.intValue >= GoalSets.intValue) {
                                showExitDialog = true
                            } else {
                                currentMode.value = ConnectedWorkoutWear.WorkoutMode.RESTING
                                navController.navigate("RestScreen")
                            }
                        },
                        label = { Text("FINISH SET", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            } else {
                item {
                    Text(
                        text = "Goal: ${formatMs(GoalTime.value)}",
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CompactButton(
                        onClick = {
                            if (!isPaused) {
                                accMs += SystemClock.elapsedRealtime() - startAt.longValue
                                isPaused = true
                            } else {
                                startAt.longValue = SystemClock.elapsedRealtime()
                                isPaused = false
                            }
                        },
                        colors = ButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            imageVector = if(isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause"
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    CompactButton(
                        onClick = { showExitDialog = true },
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Icon(Icons.Default.Stop, "Stop")
                    }
                }
            }
        }

        // Dialog Logic (unchanged logic, just placement)
        if (showExitDialog) {
            Dialog(showDialog = showExitDialog, onDismissRequest = { showExitDialog = false }) {
                Alert(
                    title = { Text("Finish?") },
                    negativeButton = {
                        Button(onClick = { showExitDialog = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                            Icon(Icons.Default.Clear, "No")
                        }
                    },
                    positiveButton = {
                        Button(onClick = {
                            scope.launch {
                                viewModel.addSampleWorkout(
                                    name = workout.value,
                                    status = WorkoutStatus.COMPLETED,
                                    durationMillis = CurrentTime.value,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue,
                                    distance = 0.0,
                                    notes = "Watch Workout"
                                )
                                viewModel.saveToFirestore(
                                    name = workout.value,
                                    status = WorkoutStatus.COMPLETED,
                                    durationMillis = CurrentTime.value,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue,
                                    distance = 0.0,
                                    notes = "Watch Workout"
                                )
                                currentMode.value = ConnectedWorkoutWear.WorkoutMode.INACTIVE
                                showExitDialog = false
                                (context as? android.app.Activity)?.finish()
                            }
                        }, colors = ButtonDefaults.primaryButtonColors()) {
                            Icon(Icons.Default.Check, "Yes")
                        }
                    }
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------
// SCREEN 3: REST SCREEN (OPTIMIZED)
// --------------------------------------------------------------------------------
@Composable
fun WearRestScreen(navController: NavHostController) {
    // Use Long primitive
    var timeLeft by remember { mutableLongStateOf(60L) }

    LaunchedEffect(Unit) {
        val endTime = SystemClock.elapsedRealtime() + 60000L
        while(timeLeft > 0) {
            val remaining = (endTime - SystemClock.elapsedRealtime()) / 1000
            timeLeft = remaining.coerceAtLeast(0)
            delay(1000)
        }
        currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
        navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = timeLeft / 60f,
            modifier = Modifier.fillMaxSize(),
            indicatorColor = MaterialTheme.colors.secondary
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RESTING", style = MaterialTheme.typography.caption2, color = Color.Gray)
            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.display1,
                fontSize = 50.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            CompactButton(
                onClick = {
                    currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
                    navController.popBackStack()
                }
            ) {
                Icon(Icons.Default.ArrowRight, "Skip")
            }
        }
    }
}

// --------------------------------------------------------------------------------
// UTILS
// --------------------------------------------------------------------------------
fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    val s = if (seconds < 10) "0$seconds" else "$seconds"
    val m = if (minutes < 10) "0$minutes" else "$minutes"

    return if (hours > 0) {
        "$hours:$m:$s"
    } else {
        "$m:$s"
    }
}

object ConnectedWorkoutWear{
    enum class WorkoutMode { INACTIVE, ACTIVE, RESTING }
    var currentMode = mutableStateOf(WorkoutMode.INACTIVE)
    var restTimeRemaining = mutableLongStateOf(0L)

    var workout = mutableStateOf("")
    var GoalReps = mutableIntStateOf(30)
    var GoalSets = mutableIntStateOf(3)
    var CurrentReps = mutableIntStateOf(0)
    var CurrentSets = mutableIntStateOf(0)
    var CurrentTime = mutableStateOf(0L)
    var GoalTime = mutableStateOf(0L)
    var GoalDistance = mutableStateOf(0.0)
    var currentDistance = mutableStateOf(0.0)
    var CurrentWeight = mutableStateOf(0.0)
    var GoalType = "Reps"
    val workoutGoalTypeMap = mutableMapOf<String, String>()
    var selectedWorkout: MutableState<String> = mutableStateOf("")
    var restTime = mutableLongStateOf(60000L)
    var interHour = mutableIntStateOf(0)
    var healthConnectEnabled = mutableStateOf(false)
    var interMinute = mutableIntStateOf(0)
    var interSecond = mutableIntStateOf(0)
}