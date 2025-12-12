package com.forgecompose.workouttracker

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toWorkout(): Workout {
    val data = this.data ?: return Workout(name = "Error", date = 0L, startTime = 0L, status = WorkoutStatus.COMPLETED, durationMillis = null, weight = null, sets = null, reps = null, distance = null, notes = null,
        endTime = null)


    val tempId = this.id.hashCode()

    val timestamp = data["date"] as? Timestamp
    val dateMillis = timestamp?.toDate()?.time ?: System.currentTimeMillis()

    // Safely parse the status Enum
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
        startTime = dateMillis,
        endTime = null,
        durationMillis = (data["durationMillis"] as? Number)?.toLong(),
        status = statusEnum,
        weight = (data["weight"] as? Number)?.toDouble(),
        sets = (data["sets"] as? Number)?.toInt(),
        reps = (data["reps"] as? Number)?.toInt(),
        distance = (data["distance"] as? Number)?.toDouble(),
        notes = data["notes"] as? String
    )
}