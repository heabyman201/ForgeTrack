package com.forgecompose.workouttracker.workout

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.Normalizer

object WorkoutConfigSync {
    private const val TAG = "WorkoutConfigSync"
    private const val WORKOUT_CONFIG_PATH = "/workout_config"
    private const val WORKOUT_CONFIG_SUBCOLLECTION = "workout_sync"
    private const val WORKOUT_CONFIG_DOC = "latest"
    private const val WORKOUT_CONFIG_CACHE_PREFS = "workout_config_sync_cache"
    private const val KEY_LAST_PAYLOAD_JSON = "lastPayloadJson"

    suspend fun sendToWatch(
        context: Context,
        workoutName: String,
        goalType: String,
        goalSets: Int,
        goalReps: Int,
        goalTimeMs: Long,
        goalDistance: Double,
        currentWeight: Double
    ) = withContext(Dispatchers.IO) {
        val canonicalWorkoutName = toCanonicalWorkoutName(workoutName)
        val uid = FirebaseGoogleAuth.ensureSignedIn(context)
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "No authenticated Firebase user; skipping Firestore config sync")
        }

        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "schemaVersion" to 1,
            "ownerUid" to uid,
            "workoutName" to canonicalWorkoutName,
            "goalType" to goalType,
            "goalSets" to goalSets,
            "goalReps" to goalReps,
            "goalTime" to goalTimeMs,
            "goalDistance" to goalDistance,
            "currentWeight" to currentWeight,
            "ts" to now
        )
        if (!uid.isNullOrBlank()) {
            runCatching {
                Firebase.firestore
                    .collection("users")
                    .document(uid)
                    .collection(WORKOUT_CONFIG_SUBCOLLECTION)
                    .document(WORKOUT_CONFIG_DOC)
                    .set(data, SetOptions.merge())
                    .await()
            }.onFailure {
                Log.w(TAG, "Failed to upsert config to Firestore", it)
            }
        }

        val payloadJson = JSONObject()
            .put("schemaVersion", 1)
            .put("ownerUid", uid ?: "")
            .put("workoutName", canonicalWorkoutName)
            .put("goalType", goalType)
            .put("goalSets", goalSets)
            .put("goalReps", goalReps)
            .put("goalTime", goalTimeMs)
            .put("goalDistance", goalDistance)
            .put("currentWeight", currentWeight)
            .put("ts", now)
            .toString()
        cachePayload(context, payloadJson)

        sendPayloadToWatchNodes(
            context = context,
            payload = payloadJson.toByteArray(Charsets.UTF_8)
        )
    }

    suspend fun resendLastToWatch(
        context: Context,
        targetNodeId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val payloadJson = context.applicationContext
            .getSharedPreferences(WORKOUT_CONFIG_CACHE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_PAYLOAD_JSON, null)
            .orEmpty()
            .trim()
        if (payloadJson.isBlank()) {
            Log.w(TAG, "No cached workout config payload to resend.")
            return@withContext false
        }
        sendPayloadToWatchNodes(
            context = context,
            payload = payloadJson.toByteArray(Charsets.UTF_8),
            targetNodeId = targetNodeId
        )
    }

    private fun cachePayload(context: Context, payloadJson: String) {
        context.applicationContext
            .getSharedPreferences(WORKOUT_CONFIG_CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_PAYLOAD_JSON, payloadJson)
            .apply()
    }

    private suspend fun sendPayloadToWatchNodes(
        context: Context,
        payload: ByteArray,
        targetNodeId: String? = null
    ): Boolean {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            Log.w(TAG, "No connected watch nodes. Workout config not sent.")
            return false
        }

        val targetNodes = if (targetNodeId.isNullOrBlank()) {
            nodes
        } else {
            nodes.filter { it.id == targetNodeId }
        }
        if (targetNodes.isEmpty()) {
            Log.w(TAG, "Target watch node not connected. targetNodeId=$targetNodeId")
            return false
        }

        var delivered = false
        targetNodes.forEach { node ->
            runCatching {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, WORKOUT_CONFIG_PATH, payload)
                    .await()
                delivered = true
            }.onFailure {
                Log.e(TAG, "Failed to send config to node=${node.displayName}", it)
            }
        }
        return delivered
    }

    private fun toCanonicalWorkoutName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        val key = nameKey(clean)
        return workoutPresets.firstOrNull { nameKey(it.name) == key }?.name ?: clean
    }

    private fun nameKey(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .replace(Regex("[\\u2010-\\u2015]"), "-")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.lowercase()
    }
}
