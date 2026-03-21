package com.forgecompose.app_wear.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

// app-wear/presentation/HrViewModel.kt
// app-wear/src/main/java/com/example/app_wear/presentation/HrViewModel.kt


class HrViewModel(
    private val repo: HrRepository,
    private val appContext: Context
) : ViewModel() {

    private val sync = WearHrSync(appContext)

    private val _bpm = MutableStateFlow<Int?>(null)
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()

    private val _inExercise = MutableStateFlow(false)
    val inExercise: StateFlow<Boolean> = _inExercise.asStateFlow()
    private var streamJob: Job? = null

    init {
        // forward BPM to phone, throttled
        forwardBpmToPhone()
    }

    fun start() {
        if (_inExercise.value) return
        viewModelScope.launch {
            val started = runCatching { repo.startHrExercise() }.getOrDefault(false)
            if (!started) {
                _inExercise.value = false
                return@launch
            }
            _inExercise.value = true
            streamJob?.cancel()
            streamJob = launch {
                runCatching {
                    repo.heartRateStream()
                        .filterNotNull()
                        .conflate()
                        .sample(1500)
                        .distinctUntilChanged()
                        .collect { value -> _bpm.value = value }
                }.onFailure {
                    _inExercise.value = false
                }
            }
        }
    }

    fun stop() {
        if (!_inExercise.value) return
        viewModelScope.launch {
            streamJob?.cancel()
            streamJob = null
            repo.endExercise()
            _inExercise.value = false
            _bpm.value = null
        }
    }

    @OptIn(FlowPreview::class)
    private fun forwardBpmToPhone() {
        _bpm
            .filterNotNull()
            .conflate()
            .distinctUntilChanged()
            .debounce(2000)
            .onEach { runCatching { sync.sendBpm(it) } }
            .launchIn(viewModelScope)
    }
}

