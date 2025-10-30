package com.forgecompose.workouttracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HrViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = HrRepository(app)
    val hr = repo.hr
    override fun onCleared() { super.onCleared(); repo.stop() }
    fun start() { repo.start() }
}
class HrRepository(private val context: Context) : DataClient.OnDataChangedListener {
    private val dataClient = Wearable.getDataClient(context)
    private val _hr = MutableStateFlow(0)
    val hr: StateFlow<Int> = _hr.asStateFlow()
    fun start() { dataClient.addListener(this) }
    fun stop() { dataClient.removeListener(this) }
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { e ->
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == "/hr") {
                val map = DataMapItem.fromDataItem(e.dataItem).dataMap
                _hr.value = map.getInt("bpm", 0)
            }
        }
    }
}
