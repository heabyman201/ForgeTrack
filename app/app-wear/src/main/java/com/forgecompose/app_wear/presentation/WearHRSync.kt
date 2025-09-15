package com.forgecompose.app_wear.presentation

// app-wear/src/main/java/com/example/app_wear/sync/WearHrSync.kt


import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext


class WearHrSync(private val context: Context) {
    private val dataClient by lazy { Wearable.getDataClient(context) }

    /**
     * Send a BPM update to the phone. Throttle on your side before calling if needed.
     */
    suspend fun sendBpm(bpm: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = PutDataMapRequest.create("/hr").apply {
                dataMap.putInt("bpm", bpm)
                dataMap.putLong("ts", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(req)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}