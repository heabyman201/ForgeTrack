package com.forgecompose.workouttracker

import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant

class DataLayerListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @OptIn(ExperimentalTime::class)
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/workout_state") {
            val data = String(messageEvent.data, Charsets.UTF_8)
            val workoutData = parseWorkoutData(data)

            if (workoutData["mode"] == "COMPLETED") {
                val application = application as MyApplication
                val repository = application.workoutRepository
                val factory = WorkoutListViewModelFactory(repository)
                val viewModel = factory.create(WorkoutListViewModel::class.java)

                val workoutName = workoutData["workout"] ?: "Unknown Workout"
                val duration = workoutData["currentTime"]?.toLongOrNull() ?: 0L
                val weight = workoutData["currentWeight"]?.toDoubleOrNull() ?: 0.0
                val sets = workoutData["currentSets"]?.toIntOrNull() ?: 0
                val goalReps = workoutData["goalReps"]?.toIntOrNull() ?: 0
                val totalReps = sets * goalReps
                val distance = workoutData["currentDistance"]?.toDoubleOrNull() ?: 0.0

                serviceScope.launch {
                    Log.d("DataLayerListenerService", "Received completed workout: $workoutName. Saving via ViewModel...")
                    viewModel.addSampleWorkout(
                        name = workoutName,
                        status = WorkoutStatus.COMPLETED,
                        durationMillis = duration,
                        weight = weight,
                        sets = sets,
                        reps = totalReps,
                        distance = distance,
                        notes = "Synced from Wear OS"
                    )

                    val healthConnectManager = HealthConnectManager(applicationContext)
                    if (healthConnectManager.hasAllPermissions()) {
                        val endInstant = Clock.System.now().toJavaInstant()
                        val cardioExerciseNames = listOf(
                            "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                            "Rowing Machine", "Stationary Bike", "Swimming"
                        )
                        val workoutDetails = WorkoutDetails(
                            title = workoutName,
                            startTime = endInstant.minusMillis(duration),
                            endTime = endInstant,
                            exerciseType = if (workoutName in cardioExerciseNames) {
                                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                            } else {
                                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                            }
                        )
                        healthConnectManager.writeWorkout(workoutDetails)
                        Log.d("DataLayerListenerService", "Workout data also written to Health Connect.")
                    }
                }
            }
        }
    }

    private fun parseWorkoutData(data: String): Map<String, String> {
        return data.split(',').associate {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                "" to ""
            }
        }.filterKeys { it.isNotEmpty() }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

