package com.forgecompose.app_wear.presentation

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.workout
import com.forgecompose.app_wear.presentation.theme.hasHeartRatePermission
import com.forgecompose.app_wear.presentation.theme.requiredSensorPermissions
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme


@Composable
fun WearApp(
    mainViewModel: MainScreenViewModel
) {
    val navController = rememberSwipeDismissableNavController()
    val context  = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    WorkoutTrackerTheme {
        Scaffold(
            timeText = { if (currentRoute != "HomeScreen") TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "HomeScreen"
            ) {
                composable("HomeScreen") {
                    WearHomeScreen(
                        mainViewModel = mainViewModel,
                        onNavigateToWorkout = { workoutId ->
                            // Navigate to detail screen
                        },
                        onQuickStart = {
                           navController.navigate("WorkoutSelector")
                        },
                        onHrMonitor = {
                           navController.navigate("HrMonitor")
                        }
                    )
                }
                composable("WorkoutSelector") {
                    WearWorkoutSelector(
                        navController = navController,
                        onWorkoutSelected = { workoutName ->
                            val intent = Intent(context, WearWorkoutActivity::class.java)
                            intent.putExtra("workoutName", workoutName)
                            workout.value = workoutName
                            context.startActivity(intent)
                        }
                    )
                }
                composable("HrMonitor") {
                    WearHrMonitorScreen()
                }
            }
        }
    }
}

@Composable
fun WearHomeScreen(
    mainViewModel: MainScreenViewModel,
    onNavigateToWorkout: (Long) -> Unit,
    onQuickStart: () -> Unit,
    onHrMonitor: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val latestName by mainViewModel.latestWorkoutName.collectAsStateWithLifecycle()

    val presets = remember {
        listOf("Push-ups", "Pull-ups", "Bodyweight Squats", "Barbell Curls", "Deadlifts")
    }
    val context = LocalContext.current

    val scalingParams = remember {
        ScalingLazyColumnDefaults.scalingParams(
            edgeScale = 0.5f,
            edgeAlpha = 0.5f
        )
    }
    val autoCenteringParams = remember { AutoCenteringParams(itemIndex = 1) }

    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val backgroundColor = MaterialTheme.colors.background

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            scalingParams = scalingParams,
            autoCentering = autoCenteringParams
        ) {
            // 1. Header
            item {
                ListHeader {
                    Text(
                        text = "Dashboard",
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        style = MaterialTheme.typography.title2,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Last Workout Summary
            item {
                val lastWorkoutTitle = latestName?.name ?: "No Recent Data"
                val lastWorkoutTime = formatTimeForWear(latestName?.durationMillis?.toLong() ?: 0)

                Card(
                    onClick = { /* Navigate to history details */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = primaryColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = backgroundColor.copy(alpha = 0.5f),
                        endBackgroundColor = backgroundColor.copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Last Session", style = MaterialTheme.typography.caption1, color = Color.White.copy(alpha = 0.7f))
                            Text(text = lastWorkoutTime, style = MaterialTheme.typography.caption1, color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastWorkoutTitle,
                            style = MaterialTheme.typography.title3,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Quick Start Action
            item {
                Card(
                    onClick = onQuickStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = primaryColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = primaryColor.copy(alpha = 0.15f),
                        endBackgroundColor = primaryColor.copy(alpha = 0.05f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "Start", tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Quick Start", style = MaterialTheme.typography.button, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    onClick = onHrMonitor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDC143C).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFFDC143C).copy(alpha = 0.1f),
                        endBackgroundColor = Color(0xFFDC143C).copy(alpha = 0.05f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Rounded.Favorite, contentDescription = "Heart Rate", tint = Color(0xFFDC143C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "HR Monitor", style = MaterialTheme.typography.button, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 4. Divider / Section
            item {
                ListHeader {
                    Text("Suggested", color = Color.White.copy(alpha = 0.8f))
                }
            }

            // 5. Preset List
            items(presets, key = { it }) { workoutName ->
                Card(
                    onClick = {
                        val intent = Intent(context, WearWorkoutActivity::class.java)
                        workout.value = workoutName
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = backgroundColor.copy(alpha = 0.4f),
                        endBackgroundColor = backgroundColor.copy(alpha = 0.4f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = workoutName,
                            style = MaterialTheme.typography.body2,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Select",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper to format time efficiently
fun formatTimeForWear(ms: Long): String {
    if (ms == 0L) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun WearHrMonitorScreen() {
    val context = LocalContext.current
    val bpm by HrMonitorRuntime.bpm.collectAsStateWithLifecycle()
    val running by HrMonitorRuntime.running.collectAsStateWithLifecycle()
    val err by HrMonitorRuntime.error.collectAsStateWithLifecycle()
    val activity = context as? android.app.Activity

    val requiredPermissions = remember { requiredSensorPermissions() }
    fun hasRequiredPermissions(): Boolean = hasHeartRatePermission(context)
    fun startService(force: Boolean = false) {
        val intent = Intent(context, HrMonitorService::class.java).apply {
            action = if (force) HrMonitorService.ACTION_FORCE_START else HrMonitorService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService() {
        val intent = Intent(context, HrMonitorService::class.java).apply {
            action = HrMonitorService.ACTION_STOP
        }
        context.startService(intent)
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = hasHeartRatePermission(result)
        permanentlyDenied = !granted && requiredPermissions.any { perm ->
            activity?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, perm) } == true
        }
        if (granted) startService()
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 1)
        ) {
            item {
                ListHeader {
                    Text("HR Monitor", textAlign = TextAlign.Center, color = Color.White)
                }
            }
            item {
                Card(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDC143C).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFFDC143C).copy(alpha = 0.1f),
                        endBackgroundColor = Color(0xFFDC143C).copy(alpha = 0.05f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFDC143C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if ((bpm ?: 0) > 0) "${bpm ?: 0} BPM" else "-- BPM",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            style = MaterialTheme.typography.title2.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                Card(
                    onClick = {
                        if (running) {
                            stopService()
                        } else if (hasRequiredPermissions()) {
                            startService()
                        } else if (permanentlyDenied) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        } else {
                            permissionLauncher.launch(requiredPermissions.toTypedArray())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
                        endBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (running) Color.Yellow else MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (running) "Stop Sensor" else "Start Sensor",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            item {
                Card(
                    onClick = {
                        startService(force = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.2f),
                        endBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.2f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Force Start",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            if (!running && err != null) {
                item {
                    Text(
                        text = err ?: "",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption2
                    )
                }
            }
        }
    }
}
