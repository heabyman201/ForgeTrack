package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant

class DataLayerListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @OptIn(ExperimentalTime::class)
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/workout_config_request") {
            serviceScope.launch {
                runCatching {
                    val deliveredFromCache = WorkoutConfigSync.resendLastToWatch(
                        context = applicationContext,
                        targetNodeId = messageEvent.sourceNodeId
                    )
                    if (!deliveredFromCache) {
                        val hasMeaningfulInMemoryConfig =
                            ConnectedWorkout.workout.value.isNotBlank() &&
                                (ConnectedWorkout.GoalSets.intValue > 0 ||
                                    ConnectedWorkout.GoalReps.intValue > 0 ||
                                    ConnectedWorkout.GoalTime.value > 0L ||
                                    ConnectedWorkout.GoalDistance.value > 0.0 ||
                                    ConnectedWorkout.CurrentWeight.value > 0.0)
                        if (hasMeaningfulInMemoryConfig) {
                            WorkoutConfigSync.sendToWatch(
                                context = applicationContext,
                                workoutName = ConnectedWorkout.workout.value,
                                goalType = ConnectedWorkout.GoalType,
                                goalSets = ConnectedWorkout.GoalSets.intValue,
                                goalReps = ConnectedWorkout.GoalReps.intValue,
                                goalTimeMs = ConnectedWorkout.GoalTime.value,
                                goalDistance = ConnectedWorkout.GoalDistance.value,
                                currentWeight = ConnectedWorkout.CurrentWeight.value
                            )
                        } else {
                            Log.w(
                                "DataLayerListenerService",
                                "No cached or meaningful in-memory workout config to send."
                            )
                        }
                    }
                }.onFailure {
                    Log.w("DataLayerListenerService", "Failed to respond to /workout_config_request", it)
                }
            }
            return
        }

        if (messageEvent.path == "/workout_state") {
            val data = String(messageEvent.data, Charsets.UTF_8)
            val workoutData = parseWorkoutData(data)
            Log.d("DataLayerListenerService", "Workout payload: $workoutData")

            if (workoutData["mode"]?.uppercase() == "COMPLETED") {
                val application = application as MyApplication
                val repository = application.workoutRepository

                val workoutName = workoutData["workout"] ?: "Unknown Workout"
                val duration = workoutData["currentTime"]?.toLongOrNull() ?: 0L
                val weight = workoutData["currentWeight"]?.toDoubleOrNull()
                    ?: workoutData["goalWeight"]?.toDoubleOrNull()
                    ?: 0.0
                val sets = workoutData["currentSets"]?.toIntOrNull() ?: 0
                val goalReps = workoutData["goalReps"]?.toIntOrNull() ?: 0
                val totalReps = workoutData["currentReps"]?.toIntOrNull() ?: (sets * goalReps)
                val distance = workoutData["currentDistance"]?.toDoubleOrNull() ?: 0.0
                val goalSets = workoutData["goalSets"]?.toIntOrNull() ?: 0
                val goalWeight = workoutData["goalWeight"]?.toDoubleOrNull() ?: weight
                val syncedNotes =
                    "Synced from Wear OS | goals(weight=$goalWeight, reps=$goalReps, sets=$goalSets)"

                serviceScope.launch {
                    Log.d("DataLayerListenerService", "Received completed workout: $workoutName. Saving to local repository...")
                    val currentTime = System.currentTimeMillis()
                    val newWorkout = Workout(
                        name = workoutName,
                        date = currentTime,
                        startTime = currentTime,
                        endTime = null,
                        durationMillis = duration,
                        status = WorkoutStatus.COMPLETED,
                        weight = weight,
                        sets = sets,
                        reps = totalReps,
                        distance = distance,
                        notes = syncedNotes,
                        rpe = null,
                        tempo = null,
                        restPeriodSeconds = null,
                        heartRateAvg = null,
                        heartRateMax = null,
                        caloriesBurned = null,
                        equipmentUsed = null,
                        weatherConditions = null,
                        fatigueLevel = null,
                        sleepQualityScore = null,
                        trainingEnvironment = null,
                        sessionRpe = null,
                        systemicDrainScore = null
                    )
                    repository.insertWorkout(newWorkout)
                    Log.d("DataLayerListenerService", "Saved completed workout to local repository")

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
        val trimmed = data.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val json = JSONObject(trimmed)
                json.keys().asSequence().associateWith { key -> json.optString(key, "") }
                    .filterKeys { it.isNotEmpty() }
            } catch (_: Exception) {
                parseLegacyPayload(data)
            }
        }
        return parseLegacyPayload(data)
    }

    private fun parseLegacyPayload(data: String): Map<String, String> {
        return data.split(',')
            .mapNotNull { token ->
                val parts = token.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
            .filterKeys { it.isNotEmpty() }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

