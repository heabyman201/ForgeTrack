// app-wear/src/main/java/com/example/app_wear/passive/PassiveHrRegistrar.kt
package com.forgecompose.app_wear.passive

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import com.forgecompose.app_wear.presentation.PassiveDataReceiver
import kotlinx.coroutines.guava.await

suspend fun registerPassiveHr(context: Context) {
    val client = HealthServices.getClient(context).passiveMonitoringClient
    val cfg = PassiveListenerConfig.Builder()
        .setDataTypes(setOf(DataType.HEART_RATE_BPM))
        .build()

    runCatching {

        client.setPassiveListenerServiceAsync(PassiveDataReceiver::class.java, cfg).await()
    }.onSuccess {
        Log.d("PassiveHR", "Passive listener registered")
    }.onFailure { e ->
        Log.e("PassiveHR", "Passive registration failed", e)
    }
}

suspend fun unregisterPassiveHr(context: Context) {
    val client = HealthServices.getClient(context).passiveMonitoringClient
    runCatching { client.clearPassiveListenerServiceAsync().await() }
        .onSuccess { Log.d("PassiveHR", "Passive listener unregistered") }
        .onFailure { e -> Log.e("PassiveHR", "Passive unregister failed", e) }
}
