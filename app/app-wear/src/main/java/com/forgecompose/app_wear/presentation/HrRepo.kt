package com.forgecompose.app_wear.presentation

import android.content.Context
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
// <-- for ListenableFuture.await()
import kotlin.math.roundToInt

class HrRepository(private val context: Context) {

    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    /** True if the current device supports emitting HEART_RATE_BPM for a generic workout. */
    fun supportsHr(): Flow<Boolean> = flow {
        val caps = exerciseClient.getCapabilitiesAsync().await()
        val generic = caps.getExerciseTypeCapabilities(ExerciseType.WORKOUT)
        emit(DataType.HEART_RATE_BPM in generic.supportedDataTypes)
    }

    /** Starts a minimal exercise requesting heart-rate updates. */
    suspend fun startHrExercise(): Boolean {
        val config = ExerciseConfig.Builder(ExerciseType.WORKOUT)
            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
            .build()

        exerciseClient.startExerciseAsync(config).await()
        return true
    }

    /** Ends the current exercise (if any) without throwing. */
    suspend fun endExercise() {
        runCatching { exerciseClient.endExerciseAsync().await() }
    }

    /** Stream the latest BPM as an Int (null when temporarily unavailable). */
    fun heartRateStream(): Flow<Int?> = callbackFlow {
        val exec = androidx.core.content.ContextCompat.getMainExecutor(context)

        val callback = object : ExerciseUpdateCallback {
            override fun onRegistered() = Unit
            override fun onRegistrationFailed(throwable: Throwable) {
                trySend(null)
                close(throwable)
            }

            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val bpm = update.latestMetrics
                    .getData(DataType.HEART_RATE_BPM)
                    .lastOrNull()
                    ?.value
                    ?.roundToInt()
                trySend(bpm)
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
            fun onExerciseEnded(exerciseEndReason: ExerciseEndReason) = Unit
            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                if (dataType == DataType.HEART_RATE_BPM) {
                    trySend(null)
                }
            }
        }

        exerciseClient.setUpdateCallback(exec, callback)

        awaitClose {
            // launch a coroutine in the channel's scope to safely call suspend function
            launch {
                runCatching { exerciseClient.clearUpdateCallbackAsync(callback).await() }
            }
        }
    }.distinctUntilChanged()


}
