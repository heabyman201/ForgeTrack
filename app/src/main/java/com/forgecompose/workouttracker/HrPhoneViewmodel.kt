package com.forgecompose.workouttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HrPhoneViewModel : ViewModel() {
    private val _bpm = MutableStateFlow<Int?>(null)
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()

    init {
        viewModelScope.launch {
            HrUpdateBus.events
                .map { it.first } // bpm
                .distinctUntilChanged()
                .collect { _bpm.value = it }
        }
    }
}