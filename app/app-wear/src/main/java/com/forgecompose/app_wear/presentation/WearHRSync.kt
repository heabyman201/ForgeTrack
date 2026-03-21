package com.forgecompose.app_wear.presentation

// app-wear/src/main/java/com/example/app_wear/sync/WearHrSync.kt


import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await


class WearHrSync(private val context: Context) {
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private var cachedNodes: List<Node> = emptyList()
    private var lastNodeFetchMs: Long = 0L

    /**
     * Send a BPM update to the phone. Throttle on your side before calling if needed.
     */
    suspend fun sendBpm(bpm: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!shouldSend(bpm)) return@withContext Result.success(Unit)
        try {
            sendBpmDataItem(bpm)
            sendBpmMessageToNodes(bpm)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendBpmDataItem(bpm: Int) {
        val req = PutDataMapRequest.create("/hr").apply {
            dataMap.putInt("bpm", bpm)
            dataMap.putLong("ts", System.currentTimeMillis())
            setUrgent()
        }.asPutDataRequest()
        dataClient.putDataItem(req).await()
    }

    private suspend fun sendBpmMessageToNodes(bpm: Int) {
        val payload = bpm.toString().toByteArray(Charsets.UTF_8)
        val nodes = getConnectedNodesCached()
        if (nodes.isEmpty()) return
        val preferred = nodes.firstOrNull { it.isNearby } ?: nodes.first()
        messageClient.sendMessage(preferred.id, "/hr", payload).await()
    }

    private suspend fun getConnectedNodesCached(): List<Node> {
        val now = System.currentTimeMillis()
        if (cachedNodes.isNotEmpty() && (now - lastNodeFetchMs) < NODE_CACHE_MS) {
            return cachedNodes
        }
        val nodes = nodeClient.connectedNodes.await()
        cachedNodes = nodes
        lastNodeFetchMs = now
        return nodes
    }

    private fun shouldSend(bpm: Int): Boolean {
        val now = System.currentTimeMillis()
        synchronized(sendLock) {
            val shouldSend =
                lastSentBpm == null ||
                    kotlin.math.abs(bpm - (lastSentBpm ?: bpm)) >= MIN_BPM_DELTA ||
                    (now - lastSentAtMs) >= MIN_SEND_INTERVAL_MS
            if (shouldSend) {
                lastSentBpm = bpm
                lastSentAtMs = now
            }
            return shouldSend
        }
    }

    companion object {
        private const val MIN_SEND_INTERVAL_MS = 2000L
        private const val MIN_BPM_DELTA = 3
        private const val NODE_CACHE_MS = 60000L
        private val sendLock = Any()
        @Volatile private var lastSentBpm: Int? = null
        @Volatile private var lastSentAtMs: Long = 0L
    }
}
