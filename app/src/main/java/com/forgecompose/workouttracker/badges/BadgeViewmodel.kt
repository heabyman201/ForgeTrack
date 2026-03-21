package com.forgecompose.workouttracker.badges

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class BadgeUiState(
    val id: String,
    val title: String,
    val description: String,
    val progressFraction: Float,
    val isUnlocked: Boolean
)

class BadgeViewModel(
    private val badgeStorage: BadgeStorage
) : ViewModel() {

    private val _badges = MutableStateFlow<List<BadgeUiState>>(emptyList())
    val badges: StateFlow<List<BadgeUiState>> = _badges

    private val _newUnlockEvent = MutableSharedFlow<BadgeUiState>()
    val newUnlockEvent: SharedFlow<BadgeUiState> = _newUnlockEvent.asSharedFlow()

    private var isCheckingPending = false

    init {
        viewModelScope.launch {
            // 1) Load from storage
            val stored = badgeStorage.getAllBadgeProgress()

            // 2) If nothing is stored yet, create default badge progress
            val initialProgress: List<BadgeProgress> =
                if (stored.isEmpty()) {
                    val defaults = BadgeCatalog.allBadges.map { def ->
                        BadgeProgress(
                            definition = def,
                            progress = 0,
                            unlockedAtEpochMillis = null
                        )
                    }
                    badgeStorage.saveAllBadgeProgress(defaults)
                    defaults
                } else {
                    stored
                }

            // 3) Push to UI
            _badges.value = initialProgress.toUi()
            
            checkPendingNotifications()
        }
    }

    fun checkPendingNotifications() {
        if (isCheckingPending) return
        isCheckingPending = true
        viewModelScope.launch {
            try {
                val pendingIds = badgeStorage.getPendingUnlockNotifications()
                val allProgress = badgeStorage.getAllBadgeProgress()
                if (pendingIds.isNotEmpty()) {
                    pendingIds.forEach { id ->
                        val bp = allProgress.find { it.definition.id == id }
                        if (bp != null && !bp.hasShownUnlockAnimation) {
                            _newUnlockEvent.emit(BadgeUiState(
                                id = bp.definition.id,
                                title = bp.definition.title,
                                description = bp.definition.description,
                                progressFraction = 1f,
                                isUnlocked = true
                            ))
                        }
                    }
                    badgeStorage.clearPendingUnlockNotifications()
                }
            } finally {
                isCheckingPending = false
            }
        }
    }
    fun onWorkoutLogged(totalWorkoutsSoFar: Int) {
        viewModelScope.launch {
            val previous = badgeStorage.getAllBadgeProgress()
            val newlyUnlocked = mutableListOf<BadgeUiState>()

            // Only update TOTAL_WORKOUTS badges for now
            val updated = previous.map { bp ->
                if (bp.definition.category == BadgeCategory.TOTAL_WORKOUTS) {
                    val newProgress = totalWorkoutsSoFar

                    val wasUnlocked = bp.isUnlocked
                    val isNowUnlocked = newProgress >= bp.definition.target

                    val updatedBp = bp.copy(
                        progress = newProgress,
                        unlockedAtEpochMillis = when {
                            !wasUnlocked && isNowUnlocked ->
                                System.currentTimeMillis()
                            else -> bp.unlockedAtEpochMillis
                        }
                    )
                    if (!wasUnlocked && isNowUnlocked && !bp.hasShownUnlockAnimation) {
                        newlyUnlocked.add(BadgeUiState(
                            id = updatedBp.definition.id,
                            title = updatedBp.definition.title,
                            description = updatedBp.definition.description,
                            progressFraction = 1f,
                            isUnlocked = true
                        ))
                        badgeStorage.addPendingUnlockNotification(updatedBp.definition.id)
                    }
                    updatedBp
                } else {
                    // muscle badges untouched for now
                    bp
                }
            }

            badgeStorage.saveAllBadgeProgress(updated)
            _badges.value = updated.toUi()

            newlyUnlocked.forEach {
                _newUnlockEvent.emit(it)
            }
        }
    }
    fun syncTotalWorkouts(totalWorkouts: Int) {
        viewModelScope.launch {
            val previous = badgeStorage.getAllBadgeProgress()
            val newlyUnlocked = mutableListOf<BadgeUiState>()

            val updated = previous.map { bp ->
                if (bp.definition.category == BadgeCategory.TOTAL_WORKOUTS) {
                    val newProgress = totalWorkouts
                    val wasUnlocked = bp.isUnlocked
                    val isNowUnlocked = newProgress >= bp.definition.target

                    val updatedBp = bp.copy(
                        progress = newProgress,
                        unlockedAtEpochMillis = when {
                            !wasUnlocked && isNowUnlocked ->
                                System.currentTimeMillis()
                            else -> bp.unlockedAtEpochMillis
                        }
                    )
                    if (!wasUnlocked && isNowUnlocked && !bp.hasShownUnlockAnimation) {
                        newlyUnlocked.add(BadgeUiState(
                            id = updatedBp.definition.id,
                            title = updatedBp.definition.title,
                            description = updatedBp.definition.description,
                            progressFraction = 1f,
                            isUnlocked = true
                        ))
                        badgeStorage.addPendingUnlockNotification(updatedBp.definition.id)
                    }
                    updatedBp
                } else {
                    bp
                }
            }

            badgeStorage.saveAllBadgeProgress(updated)
            _badges.value = updated.toUi()

            newlyUnlocked.forEach {
                _newUnlockEvent.emit(it)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val progress = badgeStorage.getAllBadgeProgress()
            _badges.value = progress.toUi()
        }
    }

    fun onNotificationShown(badgeId: String) {
        viewModelScope.launch {
            val previous = badgeStorage.getAllBadgeProgress()
            val updated = previous.map { bp ->
                if (bp.definition.id == badgeId) {
                    bp.copy(hasShownUnlockAnimation = true)
                } else {
                    bp
                }
            }
            badgeStorage.saveAllBadgeProgress(updated)
            _badges.value = updated.toUi()
        }
    }

    private fun List<BadgeProgress>.toUi(): List<BadgeUiState> {
        return this
            .sortedWith(
                compareByDescending<BadgeProgress> { it.isUnlocked }
                    .thenByDescending { it.progressFraction }
            )
            .map { bp ->
                BadgeUiState(
                    id = bp.definition.id,
                    title = bp.definition.title,
                    description = bp.definition.description,
                    progressFraction = bp.progressFraction,
                    isUnlocked = bp.isUnlocked
                )
            }
    }
}
