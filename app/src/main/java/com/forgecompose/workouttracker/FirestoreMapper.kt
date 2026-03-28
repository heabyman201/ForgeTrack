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

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toWorkout(): Workout {
    val data = this.data ?: return Workout(
        name = "Error",
        date = 0L,
        startTime = 0L,
        endTime = null,
        durationMillis = null,
        status = WorkoutStatus.COMPLETED,
        weight = null,
        sets = null,
        reps = null,
        distance = null,
        notes = null,
        rpe = null,
        tempo = null,
        restPeriodSeconds = null,
        heartRateAvg = null,
        heartRateMax = null,
        heartRateTimeline = null,
        caloriesBurned = null,
        equipmentUsed = null,
        weatherConditions = null,
        fatigueLevel = null,
        sleepQualityScore = null,
        // Default new fields to null for error case
        trainingEnvironment = null,
        sessionRpe = null,
        systemicDrainScore = null,
        intensityScore = null,
        timingFatigueScore = null
    )

    val tempId = this.id.hashCode()
    val timestamp = data["date"] as? Timestamp
    val dateMillis = timestamp?.toDate()?.time ?: System.currentTimeMillis()

    val statusString = data["status"] as? String ?: "COMPLETED"
    val statusEnum = try {
        WorkoutStatus.valueOf(statusString)
    } catch (e: Exception) {
        WorkoutStatus.COMPLETED
    }

    return Workout(
        id = tempId,
        name = data["name"] as? String ?: "Unknown",
        date = dateMillis,
        startTime = (data["startTime"] as? Number)?.toLong() ?: dateMillis,
        endTime = (data["endTime"] as? Number)?.toLong(),
        durationMillis = (data["durationMillis"] as? Number)?.toLong(),
        status = statusEnum,
        weight = (data["weight"] as? Number)?.toDouble(),
        sets = (data["sets"] as? Number)?.toInt(),
        reps = (data["reps"] as? Number)?.toInt(),
        distance = (data["distance"] as? Number)?.toDouble(),
        notes = data["notes"] as? String,
        rpe = (data["rpe"] as? Number)?.toInt(),
        tempo = data["tempo"] as? String,
        restPeriodSeconds = (data["restPeriodSeconds"] as? Number)?.toInt(),
        heartRateAvg = (data["heartRateAvg"] as? Number)?.toInt(),
        heartRateMax = (data["heartRateMax"] as? Number)?.toInt(),
        heartRateTimeline = data["heartRateTimeline"] as? String,
        caloriesBurned = (data["caloriesBurned"] as? Number)?.toInt(),
        equipmentUsed = data["equipmentUsed"] as? String,
        weatherConditions = data["weatherConditions"] as? String,
        fatigueLevel = (data["fatigueLevel"] as? Number)?.toInt(),
        sleepQualityScore = (data["sleepQualityScore"] as? Number)?.toInt(),

        // --- NEW SMART ALGO FIELDS ---
        trainingEnvironment = data["trainingEnvironment"] as? String,
        sessionRpe = (data["sessionRpe"] as? Number)?.toInt(),
        systemicDrainScore = (data["systemicDrainScore"] as? Number)?.toFloat(),
        intensityScore = (data["intensityScore"] as? Number)?.toInt(),
        timingFatigueScore = (data["timingFatigueScore"] as? Number)?.toInt()
    )
}
