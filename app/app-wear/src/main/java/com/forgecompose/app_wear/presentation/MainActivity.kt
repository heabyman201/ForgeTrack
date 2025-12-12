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
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.firebase.FirebaseApp
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
        FirebaseApp.initializeApp(this)
        val app = application as WearApplication
        val repository = app.repository
        // viewModel (WorkoutListViewModel) removed as it's not used in the optimized WearApp
        val mainViewModel = MainScreenViewModel(repository)
        WorkoutDataSync.init(this)
        setContent {
            WorkoutTrackerTheme {
                WearApp(
                    mainViewModel = mainViewModel
                )
            }
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
