package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED,
    UPDATE_REQUIRED
}

class HealthConnectManager(private val context: Context) {

    val healthConnectClient: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    companion object {
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),

        )
    }

    fun checkAvailability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NOT_SUPPORTED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            else -> HealthConnectAvailability.NOT_INSTALLED
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(REQUIRED_PERMISSIONS)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking permissions", e)
            false
        }
    }

    suspend fun revokeAllPermissions() {
        try {
            healthConnectClient.permissionController.revokeAllPermissions()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error revoking permissions", e)
        }
    }

    suspend fun writeWorkout(workout: WorkoutDetails) {
        try {
            val records = listOf(workout.toExerciseSessionRecord())
            healthConnectClient.insertRecords(records)
            Log.i("HealthConnect", "Workout record inserted successfully.")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error inserting workout record", e)
        }
    }
}

data class WorkoutDetails(
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
)

@SuppressLint("RestrictedApi")
fun WorkoutDetails.toExerciseSessionRecord(): ExerciseSessionRecord {
    val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(this.startTime)

    return ExerciseSessionRecord(
        title = this.title,
        startTime = this.startTime,
        endTime = this.endTime,
        startZoneOffset = zoneOffset,
        endZoneOffset = zoneOffset,
        exerciseType = this.exerciseType,
        metadata = Metadata.manualEntry()
    )
}