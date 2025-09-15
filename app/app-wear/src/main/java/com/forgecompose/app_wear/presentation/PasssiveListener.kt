package com.forgecompose.app_wear.presentation

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType

// app-wear/passive/PassiveListener.kt
// PassiveListener.kt
class PassiveListener : PassiveListenerService() {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val bpm = dataPoints.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value
        Log.d("PassiveHR", "HR update: $bpm bpm")
    }
}
