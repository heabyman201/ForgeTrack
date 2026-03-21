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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthConnectViewModel(
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _availability = MutableStateFlow(HealthConnectAvailability.NOT_SUPPORTED)
    val availability: StateFlow<HealthConnectAvailability> = _availability.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    val permissions: Set<String> = HealthConnectManager.REQUIRED_PERMISSIONS

    init {
        checkAvailabilityAndPermissions()
    }

    fun checkAvailabilityAndPermissions() {
        viewModelScope.launch {
            _availability.value = healthConnectManager.checkAvailability()
            if (_availability.value == HealthConnectAvailability.INSTALLED) {
                _permissionsGranted.value = healthConnectManager.hasAllPermissions()
            }
        }
    }

    fun revokePermissions() {
        viewModelScope.launch {
            healthConnectManager.revokeAllPermissions()
            checkAvailabilityAndPermissions()
        }
    }
}