package com.forgecompose.app_wear.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.currentMode
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
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
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalDistance
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalReps
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalSets
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalTime
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.GoalType
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.workout
import com.forgecompose.app_wear.presentation.theme.hasHeartRatePermission
import com.forgecompose.app_wear.presentation.theme.requiredSensorPermissions
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme
import com.google.android.gms.wearable.Wearable
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType

class WearWorkoutActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedWorkoutConfig()
        val autoStartWorkout = applyIncomingWorkoutIntent(intent)
        requestWorkoutConfigFromPhone()

        val application = application as WearApplication
        val factory = WorkoutListViewModelFactory(application.repository)
        FirebaseApp.initializeApp(this)
        if (!autoStartWorkout) {
            syncWorkoutConfigFromFirestoreIfNewer()
        }
        setContent {
            val workoutListViewModel: WorkoutListViewModel = viewModel(factory = factory)
            WearWorkoutApp(
                viewModel = workoutListViewModel,
                autoStartWorkout = autoStartWorkout
            )
        }
    }

    private fun requestWorkoutConfigFromPhone() {
        lifecycleScope.launch {
            repeat(3) { attempt ->
                val nodes = runCatching {
                    Wearable.getNodeClient(this@WearWorkoutActivity).connectedNodes.await()
                }.getOrDefault(emptyList())
                if (nodes.isNotEmpty()) {
                    val requestPayload = ByteArray(0)
                    nodes.forEach { node ->
                        runCatching {
                            Wearable.getMessageClient(this@WearWorkoutActivity)
                                .sendMessage(node.id, "/workout_config_request", requestPayload)
                                .await()
                        }
                    }
                    return@launch
                }
                if (attempt < 2) delay(1000)
            }
        }
    }

    private fun applyIncomingWorkoutIntent(intent: Intent?): Boolean {
        val extras = intent?.extras ?: return false
        if (!extras.getBoolean("startWorkoutImmediately", false)) return false

        val incomingWorkout = canonicalWorkoutName(
            extras.getString("workoutName").orEmpty().ifBlank { workout.value }
        )
        val incomingGoalType = extras.getString("goalType").orEmpty().ifBlank { GoalType }
        val incomingGoalSets = extras.getInt("goalSets", GoalSets.intValue).coerceAtLeast(0)
        val incomingGoalReps = extras.getInt("goalReps", GoalReps.intValue).coerceAtLeast(0)
        val incomingGoalTime = extras.getLong("goalTime", GoalTime.value).coerceAtLeast(0L)
        val incomingGoalDistance = extras.getDouble("goalDistance", GoalDistance.value).coerceAtLeast(0.0)
        val incomingWeight = extras.getDouble("currentWeight", CurrentWeight.value).coerceAtLeast(0.0)

        ConnectedWorkoutWear.applyExternalConfig(
            workoutName = incomingWorkout,
            goalType = incomingGoalType,
            goalSets = incomingGoalSets,
            goalReps = incomingGoalReps,
            goalTime = incomingGoalTime,
            goalDistance = incomingGoalDistance,
            currentWeight = incomingWeight,
            ts = System.currentTimeMillis()
        )
        CurrentReps.intValue = 0
        CurrentSets.intValue = 0
        CurrentTime.value = 0L
        currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
        WorkoutDataSync.sendWorkoutState(this, ConnectedWorkoutWear.toSyncJson())
        return true
    }

    private fun applyPersistedWorkoutConfig() {
        val prefs = getSharedPreferences("wear_workout_sync", MODE_PRIVATE)
        val workoutName = prefs.getString("workoutName", null)
        val goalType = prefs.getString("goalType", null)
        val goalSets = prefs.getInt("goalSets", Int.MIN_VALUE)
        val goalReps = prefs.getInt("goalReps", Int.MIN_VALUE)
        val goalTime = prefs.getLong("goalTime", Long.MIN_VALUE)
        val goalDistance = prefs.getFloat("goalDistance", Float.NaN)
        val currentWeight = prefs.getFloat("currentWeight", Float.NaN)
        val ts = prefs.getLong(KEY_LAST_UPDATED_TS, 0L)
        val resolvedTs = if (ts > 0L) ts else if (!workoutName.isNullOrBlank()) System.currentTimeMillis() else 0L

        ConnectedWorkoutWear.applyExternalConfig(
            workoutName = canonicalWorkoutName(workoutName.orEmpty()),
            goalType = goalType.orEmpty().ifBlank { GoalType },
            goalSets = if (goalSets != Int.MIN_VALUE) goalSets else GoalSets.intValue,
            goalReps = if (goalReps != Int.MIN_VALUE) goalReps else GoalReps.intValue,
            goalTime = if (goalTime != Long.MIN_VALUE) goalTime else GoalTime.value,
            goalDistance = if (!goalDistance.isNaN()) goalDistance.toDouble() else GoalDistance.value,
            currentWeight = if (!currentWeight.isNaN()) currentWeight.toDouble() else CurrentWeight.value,
            ts = resolvedTs
        )
    }

    private fun syncWorkoutConfigFromFirestoreIfNewer() {
        lifecycleScope.launch {
            val doc = runCatching {
                val prefs = getSharedPreferences("wear_workout_sync", MODE_PRIVATE)
                val ownerUid = prefs.getString(KEY_OWNER_UID, null).orEmpty().trim()
                if (ownerUid.isBlank()) return@runCatching null
                Firebase.firestore
                    .collection("users")
                    .document(ownerUid)
                    .collection(WORKOUT_CONFIG_SUBCOLLECTION)
                    .document(WORKOUT_CONFIG_DOC)
                    .get()
                    .await()
            }.getOrNull() ?: return@launch
            if (doc == null) return@launch
            if (!doc.exists()) return@launch

            val remoteTs = doc.getLong("ts") ?: return@launch
            val prefs = getSharedPreferences("wear_workout_sync", MODE_PRIVATE)
            val localTs = prefs.getLong(KEY_LAST_UPDATED_TS, 0L)
            if (remoteTs <= localTs) return@launch

            val workoutName = canonicalWorkoutName(doc.getString("workoutName").orEmpty())
            val goalType = doc.getString("goalType").orEmpty().ifBlank { GoalType }
            val goalSets = (doc.getLong("goalSets") ?: GoalSets.intValue.toLong()).toInt().coerceAtLeast(0)
            val goalReps = (doc.getLong("goalReps") ?: GoalReps.intValue.toLong()).toInt().coerceAtLeast(0)
            val goalTime = (doc.getLong("goalTime") ?: GoalTime.value).coerceAtLeast(0L)
            val goalDistance = (doc.getDouble("goalDistance") ?: GoalDistance.value).coerceAtLeast(0.0)
            val currentWeight = (doc.getDouble("currentWeight") ?: CurrentWeight.value).coerceAtLeast(0.0)
            val ownerUid = doc.getString("ownerUid").orEmpty()

            ConnectedWorkoutWear.applyExternalConfig(
                workoutName = workoutName,
                goalType = goalType,
                goalSets = goalSets,
                goalReps = goalReps,
                goalTime = goalTime,
                goalDistance = goalDistance,
                currentWeight = currentWeight,
                ts = remoteTs
            )

            prefs.edit()
                .putString("workoutName", ConnectedWorkoutWear.workout.value)
                .putString("goalType", ConnectedWorkoutWear.GoalType)
                .putInt("goalSets", ConnectedWorkoutWear.GoalSets.intValue)
                .putInt("goalReps", ConnectedWorkoutWear.GoalReps.intValue)
                .putLong("goalTime", ConnectedWorkoutWear.GoalTime.value)
                .putFloat("goalDistance", ConnectedWorkoutWear.GoalDistance.value.toFloat())
                .putFloat("currentWeight", ConnectedWorkoutWear.CurrentWeight.value.toFloat())
                .putString(KEY_OWNER_UID, ownerUid.ifBlank { null })
                .putLong(KEY_LAST_UPDATED_TS, remoteTs)
                .apply()
        }
    }

    companion object {
        private const val WORKOUT_CONFIG_SUBCOLLECTION = "workout_sync"
        private const val WORKOUT_CONFIG_DOC = "latest"
        private const val KEY_OWNER_UID = "ownerUid"
        private const val KEY_LAST_UPDATED_TS = "lastUpdatedTs"
    }

    private fun canonicalWorkoutName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        val key = workoutNameKey(clean)
        return workoutPresets.firstOrNull { workoutNameKey(it.name) == key }?.name ?: clean
    }

    private fun workoutNameKey(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .replace(Regex("[\\u2010-\\u2015]"), "-")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.lowercase()
    }
}

@Composable
fun WearWorkoutApp(
    viewModel: WorkoutListViewModel,
    autoStartWorkout: Boolean = false,
) {
    val navController = rememberSwipeDismissableNavController()
    // Service Management
    val mode by currentMode
    val startDestination = if (autoStartWorkout) "ActiveWorkout" else "GoalSetup"

    WorkoutTrackerTheme {
        Scaffold(
            timeText = { TimeText() },
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable("GoalSetup") {
                    WearGoalScreen(navController)
                }
                composable("WeightPicker") {
                    WearWeightPickerScreen(navController)
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
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var selectedType by remember { mutableStateOf(GoalType) }
    var hasManualEdits by remember { mutableStateOf(false) }
    var syncTimeoutReached by remember { mutableStateOf(false) }
    val hasSyncedConfig by remember {
        derivedStateOf { ConnectedWorkoutWear.lastConfigTs.longValue > 0L }
    }

    // Local state
    var setCounter by remember { mutableIntStateOf(GoalSets.intValue) }
    var repCounter by remember { mutableIntStateOf(GoalReps.intValue) }
    var minutesCounter by remember { mutableIntStateOf((GoalTime.value / 60000).toInt().coerceAtLeast(1)) }

    // Weight state tracking
    var weightCounter by remember { mutableDoubleStateOf(CurrentWeight.value) }
    val currentWeightGlobal = CurrentWeight.value

    // Optimized params
    val autoCenteringParams = remember { AutoCenteringParams(itemIndex = 1) }
    val scalingParams = remember { ScalingLazyColumnDefaults.scalingParams() }

    // Keep local wheel/chip values aligned with synced phone config updates.
    LaunchedEffect(
        GoalType,
        GoalSets.intValue,
        GoalReps.intValue,
        GoalTime.value
    ) {
        if (!hasManualEdits) {
            selectedType = GoalType
            setCounter = GoalSets.intValue
            repCounter = GoalReps.intValue
            minutesCounter = (GoalTime.value / 60000).toInt().coerceAtLeast(1)
        }
    }

    // Update weight Counter when coming back from Picker
    LaunchedEffect(currentWeightGlobal) {
        if (weightCounter != currentWeightGlobal) {
            weightCounter = currentWeightGlobal
            hasManualEdits = true
        }
    }

    LaunchedEffect(hasSyncedConfig) {
        if (!hasSyncedConfig) {
            delay(8_000)
            if (!hasSyncedConfig) {
                syncTimeoutReached = true
            }
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
                        text = "Setup",
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                ToggleChip(
                    checked = selectedType == "Reps",
                    onCheckedChange = {
                        selectedType = if (it) "Reps" else "Time"
                        hasManualEdits = true
                    },
                    label = { Text("Mode: $selectedType") },
                    toggleControl = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Mode",
                            tint = MaterialTheme.colors.primary
                        )
                    },
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedEndBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f),
                        checkedStartBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f),
                        uncheckedEndBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f),
                        uncheckedStartBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha=0.15f), RoundedCornerShape(16.dp))
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
                            onClick = {
                                if (setCounter > 1) {
                                    setCounter--
                                    hasManualEdits = true
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Sets")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sets", style = MaterialTheme.typography.caption2, color = Color.Gray)
                            Text("$setCounter", style = MaterialTheme.typography.title3, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (setCounter < 10) {
                                    setCounter++
                                    hasManualEdits = true
                                }
                            },
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
                            onClick = {
                                if (repCounter > 1) {
                                    repCounter--
                                    hasManualEdits = true
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Reps")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reps", style = MaterialTheme.typography.caption2, color = Color.Gray)
                            Text("$repCounter", style = MaterialTheme.typography.title3, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (repCounter < 50) {
                                    repCounter++
                                    hasManualEdits = true
                                }
                            },
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
                            onClick = {
                                if (minutesCounter > 1) {
                                    minutesCounter--
                                    hasManualEdits = true
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Decrease Time")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mins", style = MaterialTheme.typography.caption2, color = Color.Gray)
                            Text("$minutesCounter", style = MaterialTheme.typography.title3, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (minutesCounter < 120) {
                                    minutesCounter++
                                    hasManualEdits = true
                                }
                            },
                            colors = ButtonDefaults.secondaryButtonColors(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, "Increase Time")
                        }
                    }
                }
            }

            // Weight Control (Navigates to Picker)
            item {
                Card(
                    onClick = { navController.navigate("WeightPicker") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, MaterialTheme.colors.primary.copy(alpha=0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f),
                        endBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weight", style = MaterialTheme.typography.body2, color = Color.White)
                        Text(
                            text = "${((weightCounter * 10).roundToInt() / 10.0)} kg",
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val canStartWithConfig = hasSyncedConfig || hasManualEdits || syncTimeoutReached
            val hasValidGoal = if (selectedType == "Reps") {
                setCounter > 0 && repCounter > 0
            } else {
                minutesCounter > 0
            }
            item {
                Card(
                    onClick = {
                        GoalType = selectedType
                        if (selectedType == "Reps") {
                            GoalSets.intValue = setCounter
                            GoalReps.intValue = repCounter
                            GoalTime.value = 0L
                        } else {
                            GoalSets.intValue = 0
                            GoalReps.intValue = 0
                            GoalTime.value = minutesCounter * 60 * 1000L
                        }
                        CurrentWeight.value = weightCounter

                        CurrentReps.intValue = 0
                        CurrentSets.intValue = 0
                        CurrentTime.value = 0L

                        currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
                        WorkoutDataSync.sendWorkoutState(context, ConnectedWorkoutWear.toSyncJson())
                        navController.navigate("ActiveWorkout")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.15f),
                        endBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START", style = MaterialTheme.typography.button, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!canStartWithConfig) {
                item {
                    Text(
                        text = "Waiting for phone sync...",
                        style = MaterialTheme.typography.caption2,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (syncTimeoutReached && !hasSyncedConfig) {
                item {
                    Text(
                        text = "Sync timed out. You can start manually.",
                        style = MaterialTheme.typography.caption2,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// WEIGHT PICKER SCREEN
// --------------------------------------------------------------------------------
@Composable
fun WearWeightPickerScreen(navController: NavHostController) {
    val initialWeight = ConnectedWorkoutWear.CurrentWeight.value
    val items = 1000 // 0.0 to 499.5
    val pickerState = androidx.wear.compose.material.rememberPickerState(
        initialNumberOfOptions = items,
        initiallySelectedOption = (initialWeight * 2).roundToInt().coerceIn(0, items - 1)
    )

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Select Weight",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.primary
        )
        
        androidx.wear.compose.material.Picker(
            state = pickerState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        if (it.verticalScrollPixels > 0) {
                            pickerState.scrollToOption(pickerState.selectedOption + 1)
                        } else {
                            pickerState.scrollToOption(pickerState.selectedOption - 1)
                        }
                    }
                    true
                },
            contentDescription = "Weight"
        ) { index ->
            val weight = index * 0.5
            Text(
                text = "${weight} kg",
                style = MaterialTheme.typography.display2.copy(
                    fontWeight = if (pickerState.selectedOption == index) FontWeight.Bold else FontWeight.Normal,
                    fontFeatureSettings = "tnum"
                ),
                color = if (pickerState.selectedOption == index) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
        
        Button(
            onClick = {
                ConnectedWorkoutWear.CurrentWeight.value = pickerState.selectedOption * 0.5
                navController.popBackStack()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(44.dp),
            colors = ButtonDefaults.primaryButtonColors()
        ) {
            Icon(Icons.Default.Check, "Confirm", modifier = Modifier.size(24.dp))
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
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val bpm by HrMonitorRuntime.bpm.collectAsState()
    val requiredPermissions = remember { requiredSensorPermissions() }
    var hasBodySensorsPermission by remember {
        mutableStateOf(hasHeartRatePermission(context))
    }
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    fun startHeartRateMonitor() {
        val serviceIntent = Intent(context, HrMonitorService::class.java).apply {
            action = HrMonitorService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = hasHeartRatePermission(result)
        hasBodySensorsPermission = granted
        permissionRequestAttempted = true
        if (granted) {
            permanentlyDenied = false
            startHeartRateMonitor()
        } else {
            permanentlyDenied = requiredPermissions.any { permission ->
                activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                } == true
            }
        }
    }
    LaunchedEffect(Unit) {
        if (hasBodySensorsPermission) {
            startHeartRateMonitor()
        } else if (!permissionRequestAttempted) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    // Timer Logic - Using primitives
    val startAt = rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var accMs by rememberSaveable { mutableLongStateOf(0L) }

    // Update workout clock once per second to lower recomposition and CPU wakeups.
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            val now = SystemClock.elapsedRealtime()
            CurrentTime.value = accMs + (now - startAt.longValue)
            delay(1000)
        }
    }

    val progress by remember {
        derivedStateOf {
            if (GoalType == "Reps") {
                if(GoalSets.intValue > 0) CurrentSets.intValue.toFloat() / GoalSets.intValue.toFloat() else 0f
            } else {
                if(GoalTime.value > 0) CurrentTime.value.toFloat() / GoalTime.value.toFloat() else 0f
            }
        }
    }

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
            if (!hasBodySensorsPermission) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Heart sensor permission required",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Card(
                        onClick = {
                            if (permanentlyDenied) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            } else {
                                permissionLauncher.launch(requiredPermissions.toTypedArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, MaterialTheme.colors.primary.copy(alpha=0.5f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.3f),
                            endBackgroundColor = MaterialTheme.colors.surface.copy(alpha=0.3f)
                        ),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            if (permanentlyDenied) "Open Settings" else "Grant Sensors",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        )
                    }
                }
            }
            item {
                Text(
                    text = if (isPaused) "PAUSED" else "ACTIVE",
                    color = if (isPaused) Color.Yellow else MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.caption2,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.4f),
                        endBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatMs(CurrentTime.value),
                            style = MaterialTheme.typography.display1.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFDC143C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if ((bpm ?: 0) > 0) "${bpm ?: 0} BPM" else "-- BPM",
                                style = MaterialTheme.typography.caption1,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (GoalType == "Reps") {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Set ${CurrentSets.intValue} / ${GoalSets.intValue}",
                            style = MaterialTheme.typography.title3,
                            color = Color.White
                        )
                        Text(
                            text = "Reps: ${CurrentReps.intValue}",
                            style = MaterialTheme.typography.caption1,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    Card(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, MaterialTheme.colors.secondary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.15f),
                            endBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.05f)
                        ),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            "FINISH SET",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.button,
                            color = Color.White
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Goal: ${formatMs(GoalTime.value)}",
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.8f)
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
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Icon(
                            imageVector = if(isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    CompactButton(
                        onClick = { showExitDialog = true },
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = Color.White)
                    }
                }
            }
        }

        // Dialog Logic (unchanged logic, just placement)
        if (showExitDialog) {
            Dialog(showDialog = showExitDialog, onDismissRequest = { showExitDialog = false }) {
                Alert(
                    title = { Text("Finish?", color = Color.White) },
                    negativeButton = {
                        Button(onClick = { showExitDialog = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                            Icon(Icons.Default.Clear, "No", tint = Color.White)
                        }
                    },
                    positiveButton = {
                        Button(onClick = {
                            scope.launch {
                                currentMode.value = ConnectedWorkoutWear.WorkoutMode.INACTIVE
                                WorkoutDataSync.sendWorkoutState(context, ConnectedWorkoutWear.toSyncJson(modeOverride = "COMPLETED"))
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
                                    notes = "Watch Workout",
                                    goalWeight = CurrentWeight.value,
                                    goalReps = GoalReps.intValue,
                                    goalSets = GoalSets.intValue
                                )
                                showExitDialog = false
                                (context as? android.app.Activity)?.finish()
                            }
                        }, colors = ButtonDefaults.primaryButtonColors()) {
                            Icon(Icons.Default.Check, "Yes", tint = Color.Black)
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
            Text("RESTING", style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.secondary)
            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.display1.copy(fontFeatureSettings = "tnum"),
                fontSize = 50.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    currentMode.value = ConnectedWorkoutWear.WorkoutMode.ACTIVE
                    navController.popBackStack()
                },
                colors = ButtonDefaults.secondaryButtonColors(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.ArrowRight, "Skip", tint = Color.White)
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
    var lastConfigTs = mutableLongStateOf(0L)

    var workout = mutableStateOf("")
    var GoalReps = mutableIntStateOf(0)
    var GoalSets = mutableIntStateOf(0)
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

    fun applyExternalConfig(
        workoutName: String,
        goalType: String,
        goalSets: Int,
        goalReps: Int,
        goalTime: Long,
        goalDistance: Double,
        currentWeight: Double,
        ts: Long
    ) {
        val cleanWorkout = workoutName.trim()
        val resolvedGoalType = goalType.ifBlank { GoalType }
        val safeGoalSets = goalSets.coerceAtLeast(0)
        val safeGoalReps = goalReps.coerceAtLeast(0)
        val safeGoalTime = goalTime.coerceAtLeast(0L)
        val safeGoalDistance = goalDistance.coerceAtLeast(0.0)
        val safeCurrentWeight = currentWeight.coerceAtLeast(0.0)

        val looksEmpty =
            cleanWorkout.isBlank() &&
                safeGoalSets == 0 &&
                safeGoalReps == 0 &&
                safeGoalTime == 0L &&
                safeGoalDistance == 0.0 &&
                safeCurrentWeight == 0.0
        if (looksEmpty) return

        if (cleanWorkout.isNotBlank()) {
            workout.value = cleanWorkout
        }
        GoalType = resolvedGoalType

        GoalSets.intValue = when {
            safeGoalSets > 0 -> safeGoalSets
            resolvedGoalType.equals("Time", ignoreCase = true) ||
                resolvedGoalType.equals("Distance", ignoreCase = true) -> 0
            else -> GoalSets.intValue
        }

        GoalReps.intValue = when {
            safeGoalReps > 0 -> safeGoalReps
            !resolvedGoalType.equals("Reps", ignoreCase = true) -> 0
            else -> GoalReps.intValue
        }

        GoalTime.value = when {
            safeGoalTime > 0L -> safeGoalTime
            resolvedGoalType.equals("Time", ignoreCase = true) -> 0L
            else -> GoalTime.value
        }

        GoalDistance.value = when {
            safeGoalDistance > 0.0 -> safeGoalDistance
            resolvedGoalType.equals("Distance", ignoreCase = true) -> 0.0
            else -> GoalDistance.value
        }

        if (safeCurrentWeight > 0.0 || CurrentWeight.value <= 0.0) {
            CurrentWeight.value = safeCurrentWeight
        }
        lastConfigTs.longValue = maxOf(lastConfigTs.longValue, ts)
    }

    fun toSyncJson(modeOverride: String? = null): String {
        return JSONObject()
            .put("schemaVersion", 1)
            .put("mode", modeOverride ?: currentMode.value.name)
            .put("workout", workout.value)
            .put("goalWeight", CurrentWeight.value)
            .put("goalReps", GoalReps.intValue)
            .put("goalSets", GoalSets.intValue)
            .put("currentWeight", CurrentWeight.value)
            .put("currentReps", CurrentReps.intValue)
            .put("currentSets", CurrentSets.intValue)
            .put("currentTime", CurrentTime.value)
            .put("goalTime", GoalTime.value)
            .put("goalDistance", GoalDistance.value)
            .put("currentDistance", currentDistance.value)
            .put("goalType", GoalType)
            .put("ts", System.currentTimeMillis())
            .toString()
    }
}
