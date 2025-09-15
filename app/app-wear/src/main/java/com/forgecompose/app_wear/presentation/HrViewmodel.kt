package com.forgecompose.app_wear.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    init {
        // forward BPM to phone, throttled
        forwardBpmToPhone()
    }

    fun start() {
        if (_inExercise.value) return
        viewModelScope.launch {
            runCatching { repo.startHrExercise() }
                .onSuccess {
                    _inExercise.value = true
                    repo.heartRateStream().collect { _bpm.value = it }
                }
                .onFailure { _inExercise.value = false }
        }
    }

    fun stop() {
        if (!_inExercise.value) return
        viewModelScope.launch {
            repo.endExercise()
            _inExercise.value = false
        }
    }

    @OptIn(FlowPreview::class)
    private fun forwardBpmToPhone() {
        _bpm
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(800) // ~1 update per ~0.8s; tweak as you like
            .onEach { viewModelScope.launch { runCatching { sync.sendBpm(it) } } }
            .launchIn(viewModelScope)
    }
}

