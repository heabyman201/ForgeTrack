package com.forgecompose.workouttracker.health

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class HrViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = HrRepository(app)
    val hr = repo.hr

    // Ensure we stop listening when the ViewModel is cleared to save battery
    override fun onCleared() {
        super.onCleared()
        repo.stop()
    }

    // Call this when the UI becomes active
    fun start() {
        repo.start()
    }

    fun stop() {
        repo.stop()
    }

    fun setWorkoutHrRecording(active: Boolean) {
        viewModelScope.launch {
            repo.sendHrRecordingCommand(active)
        }
    }
}

class HrRepository(
    private val context: Context,
    private val messageClientProvider: (Context) -> MessageClient = { Wearable.getMessageClient(it) },
    private val connectedNodesProvider: suspend (Context) -> List<Node> = {
        Wearable.getNodeClient(it).connectedNodes.await()
    }
) : MessageClient.OnMessageReceivedListener {
    private val messageClient = messageClientProvider(context)
    private val dataClient = Wearable.getDataClient(context)
    private val _hr = MutableStateFlow(0)
    val hr: StateFlow<Int> = _hr.asStateFlow()
    private val hrControlPath = "/hr_control"
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hrBusJob: Job? = null

    fun start() {
        messageClient.addListener(this)
        if (hrBusJob?.isActive == true) return
        hrBusJob = repoScope.launch {
            HrUpdateBus.events.collectLatest { (bpm, _) ->
                _hr.value = bpm
            }
        }
    }

    fun stop() {
        messageClient.removeListener(this)
        hrBusJob?.cancel()
        hrBusJob = null
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Match the path used in the Wear app
        if (messageEvent.path == "/hr") {
            // In the Wear fix, we sent the BPM as a String converted to bytes.
            // We must convert it back exactly the same way.
            try {
                val bpmString = String(messageEvent.data, Charsets.UTF_8)
                val bpm = bpmString.toIntOrNull() ?: 0
                _hr.value = bpm
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendHrRecordingCommand(active: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val cmd = if (active) "start" else "stop"
        val payload = cmd.toByteArray(Charsets.UTF_8)
        runCatching {
            var sent = false
            repeat(if (active) 5 else 1) { attempt ->
                val nodes = connectedNodesProvider(context)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, hrControlPath, payload).await()
                    sent = true
                }
                if (sent) return@repeat
                if (active && attempt < 4) delay(750)
            }
            // Fallback path to avoid depending only on MessageClient.
            val req = PutDataMapRequest.create("/hr_control_state").apply {
                dataMap.putBoolean("active", active)
                dataMap.putLong("ts", System.currentTimeMillis())
                setUrgent()
            }.asPutDataRequest()
            dataClient.putDataItem(req).await()
            Unit

        }
    }
}
