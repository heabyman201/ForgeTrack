// app-wear/src/main/java/com/example/app_wear/passive/PassiveDataReceiver.kt
package com.forgecompose.app_wear.presentation

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.UserActivityInfo
import kotlinx.coroutines.*

class PassiveDataReceiver : PassiveListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sync: WearHrSync

    override fun onCreate() {
        super.onCreate()
        sync = WearHrSync(this)
    }

    // NOTE: Do NOT try to override onRegistered/onRegistrationFailed here — they don't exist on the service API.
    // If you need registration success/failure, handle it from the register call's Future/Task (see below).

    override fun onNewDataPointsReceived(container: DataPointContainer) {
        val bpm = container.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.toInt()
        if (bpm != null && bpm > 0) {
            scope.launch {
                runCatching { sync.sendBpm(bpm) }
                    .onSuccess { Log.d("PassiveHR", "Forwarded passive bpm=$bpm") }
                    .onFailure { Log.e("PassiveHR", "sendBpm failed", it) }
            }
        }
    }

    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        // optional
    }

    fun onExerciseInfoReceived(info: ExerciseInfo) {
        // optional
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
