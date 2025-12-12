package com.forgecompose.app_wear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun WearWorkoutSelector(
    navController: NavController,
    onWorkoutSelected: (String) -> Unit
) {
    val listState = rememberScalingLazyListState()

    // --- State ---
    var selectedCategory by remember { mutableStateOf("All") }
    // Define categories outside composition or remember them to avoid allocation
    val baseCategories = remember { listOf("All", "Bodyweight", "Dumbbell", "Barbell", "Machines") }

    // --- Filtering Logic ---
    // Ensure workoutPresets is a stable list. If it's static, sort it once globally if possible.
    val filteredWorkouts by remember(selectedCategory) {
        derivedStateOf {
            val base = if (selectedCategory == "All") {
                workoutPresets
            } else {
                workoutPresets.filter { it.category.contains(selectedCategory, ignoreCase = true) }
            }
            base.sortedBy { it.name }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
        scalingParams = ScalingLazyColumnDefaults.scalingParams()
    ) {
        // 1. Header
        item {
            Text(
                text = "Select Workout",
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }


        item {

            CompactChip(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = {
                    val currentIndex = baseCategories.indexOf(selectedCategory)
                    val nextIndex = (currentIndex + 1) % baseCategories.size
                    selectedCategory = baseCategories[nextIndex]
                },
                label = {
                    Text("Filter: $selectedCategory", style = MaterialTheme.typography.caption2)
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }


        items(filteredWorkouts, key = { it.name }) { preset ->
            Chip(
                modifier = Modifier
                    .fillMaxWidth()

                    .padding(vertical = 2.dp),
                colors = ChipDefaults.secondaryChipColors(),
                onClick = {
                    if (ConnectedWorkoutWear.currentMode.value == ConnectedWorkoutWear.WorkoutMode.INACTIVE) {
                        ConnectedWorkoutWear.workout.value = preset.name
                        onWorkoutSelected(preset.name)
                    }
                },
                label = {
                    Text(
                        text = preset.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                secondaryLabel = {
                    Text(
                        text = preset.category,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            )
        }

        // 4. "Phone" Hint
        item {
            Text(
                text = "More options on phone",
                style = MaterialTheme.typography.caption2,
                color = Color.Gray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}