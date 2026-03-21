package com.forgecompose.app_wear.presentation

import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject
import java.text.Normalizer

private object WearHrRecorder {
    fun start(context: android.content.Context) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, HrMonitorService::class.java).apply {
            action = HrMonitorService.ACTION_START
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }.onFailure {
            Log.e("WearHrControlSvc", "Failed to start HR monitor service", it)
        }
    }

    fun stop(context: android.content.Context) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, HrMonitorService::class.java).apply {
            action = HrMonitorService.ACTION_STOP
        }
        runCatching { appContext.startService(intent) }
            .onFailure { Log.e("WearHrControlSvc", "Failed to stop HR monitor service", it) }
    }

    fun shutdown() = Unit
}

class WearHrControlListenerService : WearableListenerService() {
    private val prefs by lazy {
        applicationContext.getSharedPreferences("wear_workout_sync", MODE_PRIVATE)
    }
    private val latestTs: Long
        get() = prefs.getLong(KEY_LAST_UPDATED_TS, 0L)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/hr_control" -> {
                val command = runCatching { String(messageEvent.data, Charsets.UTF_8) }.getOrDefault("")
                when (command.lowercase()) {
                    "start" -> WearHrRecorder.start(applicationContext)
                    "stop" -> WearHrRecorder.stop(applicationContext)
                }
            }
            "/workout_config" -> {
                val payload = runCatching { String(messageEvent.data, Charsets.UTF_8) }.getOrDefault("")
                runCatching {
                    val json = JSONObject(payload)
                    val incomingTs = json.optLong("ts", System.currentTimeMillis())
                    if (incomingTs < latestTs) {
                        Log.w("WearHrControlSvc", "Ignored stale workout config (ts=$incomingTs)")
                        return@runCatching
                    }
                    val ownerUid = json.optString("ownerUid", "").trim()
                    val workoutName = canonicalWorkoutName(json.optString("workoutName", ""))
                    val goalType = json.optString("goalType", "Reps")
                    val goalSets = json.optInt("goalSets", ConnectedWorkoutWear.GoalSets.intValue)
                    val goalReps = json.optInt("goalReps", ConnectedWorkoutWear.GoalReps.intValue)
                    val goalTime = json.optLong("goalTime", ConnectedWorkoutWear.GoalTime.value)
                    val goalDistance = json.optDouble("goalDistance", ConnectedWorkoutWear.GoalDistance.value)
                    val currentWeight = json.optDouble("currentWeight", ConnectedWorkoutWear.CurrentWeight.value)

                    ConnectedWorkoutWear.applyExternalConfig(
                        workoutName = workoutName,
                        goalType = goalType,
                        goalSets = goalSets,
                        goalReps = goalReps,
                        goalTime = goalTime,
                        goalDistance = goalDistance,
                        currentWeight = currentWeight,
                        ts = incomingTs
                    )

                    prefs.edit()
                        .putString("workoutName", ConnectedWorkoutWear.workout.value)
                        .putString("goalType", ConnectedWorkoutWear.GoalType)
                        .putInt("goalSets", ConnectedWorkoutWear.GoalSets.intValue)
                        .putInt("goalReps", ConnectedWorkoutWear.GoalReps.intValue)
                        .putLong("goalTime", ConnectedWorkoutWear.GoalTime.value)
                        .putFloat("goalDistance", ConnectedWorkoutWear.GoalDistance.value.toFloat())
                        .putFloat("currentWeight", ConnectedWorkoutWear.CurrentWeight.value.toFloat())
                        .putString(KEY_OWNER_UID, ownerUid.ifBlank { null })
                        .putLong(KEY_LAST_UPDATED_TS, incomingTs)
                        .apply()
                    Log.d("WearHrControlSvc", "Applied workout config from phone for ${ConnectedWorkoutWear.workout.value}")
                }.onFailure {
                    Log.e("WearHrControlSvc", "Failed parsing /workout_config", it)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path
            if (path == "/hr_control_state") {
                val map = runCatching { DataMapItem.fromDataItem(event.dataItem).dataMap }.getOrNull() ?: return@forEach
                val active = map.getBoolean("active")
                if (active) {
                    WearHrRecorder.start(applicationContext)
                } else {
                    WearHrRecorder.stop(applicationContext)
                }
                return@forEach
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WearHrRecorder.shutdown()
    }

    companion object {
        private const val KEY_OWNER_UID = "ownerUid"
        private const val KEY_LAST_UPDATED_TS = "lastUpdatedTs"
    }

    private fun canonicalWorkoutName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        val key = workoutNameKey(clean)
        return workoutPresets.firstOrNull { workoutNameKey(it.name) == key }?.name ?: clean
    }

    private fun workoutNameKey(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .replace(Regex("[\\u2010-\\u2015]"), "-")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.lowercase()
    }
}
