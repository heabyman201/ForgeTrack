package com.forgecompose.workouttracker


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        }
    }
    fun onWorkoutLogged(totalWorkoutsSoFar: Int) {
        viewModelScope.launch {
            val previous = badgeStorage.getAllBadgeProgress()

            // Only update TOTAL_WORKOUTS badges for now
            val updated = previous.map { bp ->
                if (bp.definition.category == BadgeCategory.TOTAL_WORKOUTS) {
                    val newProgress = totalWorkoutsSoFar

                    val wasUnlocked = bp.isUnlocked
                    val isNowUnlocked = newProgress >= bp.definition.target

                    bp.copy(
                        progress = newProgress,
                        unlockedAtEpochMillis = when {
                            !wasUnlocked && isNowUnlocked ->
                                System.currentTimeMillis()
                            else -> bp.unlockedAtEpochMillis
                        }
                    )
                } else {
                    // muscle badges untouched for now
                    bp
                }
            }

            badgeStorage.saveAllBadgeProgress(updated)
            _badges.value = updated.toUi()
        }
    }
    fun syncTotalWorkouts(totalWorkouts: Int) {
        viewModelScope.launch {
            val previous = badgeStorage.getAllBadgeProgress()

            val updated = previous.map { bp ->
                if (bp.definition.category == BadgeCategory.TOTAL_WORKOUTS) {
                    val newProgress = totalWorkouts
                    val wasUnlocked = bp.isUnlocked
                    val isNowUnlocked = newProgress >= bp.definition.target

                    bp.copy(
                        progress = newProgress,
                        unlockedAtEpochMillis = when {
                            !wasUnlocked && isNowUnlocked ->
                                System.currentTimeMillis()
                            else -> bp.unlockedAtEpochMillis
                        }
                    )
                } else {
                    bp
                }
            }

            badgeStorage.saveAllBadgeProgress(updated)
            _badges.value = updated.toUi()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val progress = badgeStorage.getAllBadgeProgress()
            _badges.value = progress.toUi()
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
