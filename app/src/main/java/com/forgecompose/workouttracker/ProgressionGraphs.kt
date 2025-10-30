package com.forgecompose.workouttracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseWeightProgressionGraph(
    exerciseName: String,
    workouts: List<Workout>
) {
    // 1. Filter and sort the data for the specific exercise
    val exerciseData = remember(workouts, exerciseName) {
        workouts
            .filter { it.name == exerciseName && (it.weight ?: 0.0) > 0.0 }
            .sortedBy { it.date }
    }

    // 2. Determine the min/max values for scaling the graph
    val maxWeight = remember(exerciseData) { exerciseData.maxOfOrNull { it.weight!! } ?: 0.0 }
    val minWeight = remember(exerciseData) { exerciseData.minOfOrNull { it.weight!! } ?: 0.0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Progression for $exerciseName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (exerciseData.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Not enough data to show a progression.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LineChart(
                    data = exerciseData,
                    maxWeight = maxWeight,
                    minWeight = minWeight
                )
            }
        }
    }
}
@Composable
fun ExerciseSetProgressionGraph(
    exerciseName: String,
    workouts: List<Workout>
) {
    val exerciseData = remember(workouts, exerciseName) {
        workouts
            .filter { it.name == exerciseName && (it.sets ?: 0) > 0 }
            .sortedBy { it.date }
    }

    val maxSets = remember(exerciseData) { exerciseData.maxOfOrNull { it.sets ?: 0 } ?: 0 }
    val minSets = remember(exerciseData) { exerciseData.minOfOrNull { it.sets ?: 0 } ?: 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Progression for $exerciseName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (exerciseData.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Not enough data to show a progression.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LineChartSets(
                    data = exerciseData,
                    maxValue = maxSets.toDouble(),
                    minValue = minSets.toDouble()
                )
            }
        }
    }
}

@Composable
fun ExerciseRepProgressionGraph(
    exerciseName: String,
    workouts: List<Workout>
) {
    val exerciseData = remember(workouts, exerciseName) {
        workouts
            .filter { it.name == exerciseName && (it.reps ?: 0) > 0 }
            .sortedBy { it.date }
    }

    val maxReps = remember(exerciseData) { exerciseData.maxOfOrNull { it.reps ?: 0 } ?: 0 }
    val minReps = remember(exerciseData) { exerciseData.minOfOrNull { it.reps ?: 0 } ?: 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Progression for $exerciseName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (exerciseData.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Not enough data to show a progression.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LineChartReps(
                    data = exerciseData,
                    maxValue = maxReps.toDouble(),
                    minValue = minReps.toDouble()
                )
            }
        }
    }
}