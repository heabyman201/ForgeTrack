package com.forgecompose.app_wear.presentation

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.forgecompose.app_wear.presentation.ConnectedWorkoutWear.workout
import com.forgecompose.app_wear.presentation.theme.WorkoutTrackerTheme


@Composable
fun WearApp(
    mainViewModel: MainScreenViewModel
) {
    val navController = rememberSwipeDismissableNavController()
    val context  = LocalContext.current

    WorkoutTrackerTheme {
        Scaffold(
            timeText = { TimeText() },
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
            }
        }
    }
}

@Composable
fun WearHomeScreen(
    mainViewModel: MainScreenViewModel,
    onNavigateToWorkout: (Long) -> Unit,
    onQuickStart: () -> Unit
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
                    Text(text = "Dashboard", textAlign = TextAlign.Center)
                }
            }

            // 2. Last Workout Summary
            item {
                val lastWorkoutTitle = latestName?.name ?: "No Recent Data"
                val lastWorkoutTime = formatTimeForWear(latestName?.durationMillis?.toLong() ?: 0)

                TitleCard(
                    onClick = { /* Navigate to history details */ },
                    title = { Text(text = "Last Session") },
                    time = { Text(text = lastWorkoutTime) },
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    Text(
                        text = lastWorkoutTitle,
                        style = MaterialTheme.typography.body1
                    )
                }
            }

            // 3. Quick Start Action
            item {
                Chip(
                    label = { Text("Quick Start") },
                    onClick = onQuickStart,
                    icon = {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow, contentDescription = "Start")
                    },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillParentMaxWidth()
                )
            }

            // 4. Divider / Section
            item {
                ListHeader {
                    Text("Suggested")
                }
            }

            // 5. Preset List
            items(presets, key = { it }) { workoutName ->
                Chip(
                    label = { Text(workoutName) },
                    onClick = {
                        val intent = Intent(context, WearWorkoutActivity::class.java)
                        workout.value = workoutName
                        context.startActivity(intent)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillParentMaxWidth()
                )
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