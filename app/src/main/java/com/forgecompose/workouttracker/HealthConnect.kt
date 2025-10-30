package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class)
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
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(REQUIRED_PERMISSIONS)
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
            Log.d("HealthConnect", "Workout record inserted successfully.")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error inserting workout record", e)
        }
    }

    suspend fun readHeartRateRecords(start: Instant, end: Instant): List<HeartRateRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading heart rate", e)
            emptyList()
        }
    }

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading sleep sessions", e)
            emptyList()
        }
    }

    suspend fun readRestingHeartRate(start: Instant, end: Instant): List<RestingHeartRateRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading resting HR", e)
            emptyList()
        }
    }

    suspend fun readBodyFat(start: Instant, end: Instant): List<BodyFatRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading body fat", e)
            emptyList()
        }
    }

    suspend fun readNutrition(start: Instant, end: Instant): List<NutritionRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading nutrition", e)
            emptyList()
        }
    }

    suspend fun readTotalCalories(start: Instant, end: Instant): List<TotalCaloriesBurnedRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading calories", e)
            emptyList()
        }
    }

    suspend fun readOxygenSaturation(start: Instant, end: Instant): List<OxygenSaturationRecord> {
        return try {
            val req = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            healthConnectClient.readRecords(req).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading SpO2", e)
            emptyList()
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