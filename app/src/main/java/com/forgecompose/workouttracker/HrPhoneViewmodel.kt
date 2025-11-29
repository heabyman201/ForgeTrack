package com.forgecompose.workouttracker


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}

class HrRepository(private val context: Context) : MessageClient.OnMessageReceivedListener {
    private val messageClient = Wearable.getMessageClient(context)
    private val _hr = MutableStateFlow(0)
    val hr: StateFlow<Int> = _hr.asStateFlow()

    fun start() {
        // Register specifically for messages
        messageClient.addListener(this)
    }

    fun stop() {
        messageClient.removeListener(this)
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
}