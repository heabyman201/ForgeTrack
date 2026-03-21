package com.forgecompose.app_wear.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
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

    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val backgroundColor = MaterialTheme.colors.background

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
                style = androidx.wear.compose.material.MaterialTheme.typography.title2.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
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
                    Text("Filter: $selectedCategory", style = MaterialTheme.typography.caption2, color = Color.White)
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = secondaryColor.copy(alpha = 0.25f),
                    contentColor = Color.White
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Filter",
                        tint = primaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            )
        }

        items(filteredWorkouts, key = { it.name }) { preset ->
            Card(
                onClick = {
                    if (ConnectedWorkoutWear.currentMode.value == ConnectedWorkoutWear.WorkoutMode.INACTIVE) {
                        ConnectedWorkoutWear.workout.value = preset.name
                        onWorkoutSelected(preset.name)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = backgroundColor.copy(alpha = 0.4f),
                    endBackgroundColor = backgroundColor.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.title3,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.category,
                            style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Select",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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