package com.forgecompose.workouttracker

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ai.RepPredictor

class RepViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private var predictor: RepPredictor? = null

    private fun getOrCreatePredictor(): RepPredictor {
        val existing = predictor
        if (existing != null) return existing
        return RepPredictor(app).also { predictor = it }
    }

    fun getPrediction(
        exerciseIndex: Int, // 0: Squat, 1: Curl, 2: BW Squat, 3: Crunch, 4: Pull-up, 5: Push-up
        weight: Float,
        sets: Int,
        durationSec: Int,
        onResult: (String) -> Unit
    ) {
        try {
            val prediction = getOrCreatePredictor().predict(
                exerciseIndex = exerciseIndex,
                weight = weight,
                sets = sets,
                durationSec = durationSec
            )
            val finalResult = Math.round(prediction).coerceAtLeast(0)
            onResult(finalResult.toString())
        } catch (e: Exception) {
            onResult("Err")
        }
    }

    fun releasePredictor() {
        predictor?.close()
        predictor = null
    }

    override fun onCleared() {
        releasePredictor()
        super.onCleared()
    }
}

@Composable
fun RepPredictorScreen(navController: NavController, viewModel: RepViewModel = viewModel()) {
    var weight by remember { mutableStateOf("48") }
    var sets by remember { mutableStateOf("3") }
    var duration by remember { mutableStateOf("60") }
    var result by remember { mutableStateOf("0") }
    val exercises = listOf("Back Squat", "Curls", "Bodyweight Squat", "Crunches", "Pull-ups", "Push-ups")
    var selectedIndex by remember { mutableStateOf(5) }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.releasePredictor()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Predict Reps", style = MaterialTheme.typography.headlineLarge)

        Text("Exercise: ${exercises[selectedIndex]}")

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (KG)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = sets,
            onValueChange = { sets = it },
            label = { Text("Sets") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it },
            label = { Text("Duration (Seconds)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.getPrediction(
                    selectedIndex,
                    weight.toFloatOrNull() ?: 0f,
                    sets.toIntOrNull() ?: 0,
                    duration.toIntOrNull() ?: 0
                ) {
                    result = it
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Text("Estimated Reps: $result", style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}
