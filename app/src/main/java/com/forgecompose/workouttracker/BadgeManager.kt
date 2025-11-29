package com.forgecompose.workouttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BadgeManager(
    private val badgeStorage: BadgeStorage
) {

    suspend fun onWorkoutLogged(
        totalWorkoutsSoFar: Int,
        musclesTrained: Set<MuscleId>
    ) {
        val previous = badgeStorage.getAllBadgeProgress()
        val updated = updateBadgesForLoggedWorkout(
            totalWorkoutsSoFar = totalWorkoutsSoFar,
            musclesTrainedInThisWorkout = musclesTrained,
            previousProgress = previous
        )
        badgeStorage.saveAllBadgeProgress(updated)
    }
}

interface BadgeStorage {
    suspend fun getAllBadgeProgress(): List<BadgeProgress>
    suspend fun saveAllBadgeProgress(badges: List<BadgeProgress>)
}

fun updateBadgesForLoggedWorkout(
    totalWorkoutsSoFar: Int,
    musclesTrainedInThisWorkout: Set<MuscleId>,
    previousProgress: List<BadgeProgress>,
    nowEpochMillis: Long = System.currentTimeMillis()
): List<BadgeProgress> {
    // Ensure we always have a BadgeProgress for every definition
    val progressById = previousProgress.associateBy { it.definition.id }.toMutableMap()

    fun ensureProgress(def: BadgeDefinition): BadgeProgress {
        return progressById[def.id] ?: BadgeProgress(definition = def)
    }

    val allDefinitions = BadgeCatalog.allBadges

    for (def in allDefinitions) {
        var badgeProgress = ensureProgress(def)

        val newRawProgress = when (def.category) {
            BadgeCategory.TOTAL_WORKOUTS -> totalWorkoutsSoFar
            BadgeCategory.MUSCLE -> {
                // +1 per workout that hits that muscle
                if (def.muscleId != null && musclesTrainedInThisWorkout.contains(def.muscleId)) {
                    badgeProgress.progress + 1
                } else {
                    badgeProgress.progress
                }
            }
        }

        val wasUnlocked = badgeProgress.isUnlocked
        val updated = badgeProgress.copy(
            progress = newRawProgress,
            unlockedAtEpochMillis = when {
                !wasUnlocked && newRawProgress >= def.target ->
                    nowEpochMillis
                else -> badgeProgress.unlockedAtEpochMillis
            }
        )

        progressById[def.id] = updated
    }

    return progressById.values.toList()
}
class BadgeViewModelFactory(
    private val badgeStorage: BadgeStorage
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BadgeViewModel::class.java)) {
            return BadgeViewModel(badgeStorage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
class InMemoryBadgeStorage : BadgeStorage {
    private var cache: List<BadgeProgress> = emptyList()

    override suspend fun getAllBadgeProgress(): List<BadgeProgress> = cache

    override suspend fun saveAllBadgeProgress(badges: List<BadgeProgress>) {
        cache = badges
    }
}
