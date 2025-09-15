package com.forgecompose.workouttracker

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow



import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class HrDataListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { ev ->
            if (ev.type == DataEvent.TYPE_CHANGED && ev.dataItem.uri.path == "/hr") {
                val map = DataMapItem.fromDataItem(ev.dataItem).dataMap
                val bpm = map.getInt("bpm")
                val ts  = map.getLong("ts")
                Log.d("WearHR", "HR from watch: $bpm @ $ts")

                // fan-out to in-app stream (and/or persist)
                HrUpdateBus.emit(bpm, ts)
            }
        }
    }
}
// app/src/main/java/com/example/app/wear/HrUpdateBus.kt


object HrUpdateBus {
    private val _events = MutableSharedFlow<Pair<Int, Long>>(replay = 1)
    val events: SharedFlow<Pair<Int, Long>> = _events

    fun emit(bpm: Int, ts: Long) {
        _events.tryEmit(bpm to ts)   // non-suspending
    }
}

