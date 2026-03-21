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

import android.content.Context
import android.content.SharedPreferences
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
    suspend fun getPendingUnlockNotifications(): List<String>
    suspend fun clearPendingUnlockNotifications()
    suspend fun addPendingUnlockNotification(badgeId: String)
}

fun updateBadgesForLoggedWorkout(
    totalWorkoutsSoFar: Int,
    musclesTrainedInThisWorkout: Set<MuscleId>,
    previousProgress: List<BadgeProgress>,
    nowEpochMillis: Long = System.currentTimeMillis()
): List<BadgeProgress> {
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
    private val pending = mutableListOf<String>()

    override suspend fun getAllBadgeProgress(): List<BadgeProgress> = cache

    override suspend fun saveAllBadgeProgress(badges: List<BadgeProgress>) {
        cache = badges
    }

    override suspend fun getPendingUnlockNotifications(): List<String> = pending.toList()

    override suspend fun clearPendingUnlockNotifications() {
        pending.clear()
    }

    override suspend fun addPendingUnlockNotification(badgeId: String) {
        pending.add(badgeId)
    }
}

class PersistentBadgeStorage(context: Context) : BadgeStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("badge_progress_prefs", Context.MODE_PRIVATE)

    override suspend fun getAllBadgeProgress(): List<BadgeProgress> {
        val allDefinitions = BadgeCatalog.allBadges
        return allDefinitions.map { def ->
            val progress = prefs.getInt("progress_${def.id}", 0)
            val unlockedAt = prefs.getLong("unlocked_at_${def.id}", -1L)
            val shown = prefs.getBoolean("shown_${def.id}", false)
            BadgeProgress(
                definition = def,
                progress = progress,
                unlockedAtEpochMillis = if (unlockedAt == -1L) null else unlockedAt,
                hasShownUnlockAnimation = shown
            )
        }
    }

    override suspend fun saveAllBadgeProgress(badges: List<BadgeProgress>) {
        prefs.edit().apply {
            badges.forEach { bp ->
                putInt("progress_${bp.definition.id}", bp.progress)
                if (bp.unlockedAtEpochMillis != null) {
                    putLong("unlocked_at_${bp.definition.id}", bp.unlockedAtEpochMillis)
                }
                putBoolean("shown_${bp.definition.id}", bp.hasShownUnlockAnimation)
            }
            apply()
        }
    }

    override suspend fun getPendingUnlockNotifications(): List<String> {
        return prefs.getStringSet("pending_unlocks", emptySet())?.toList() ?: emptyList()
    }

    override suspend fun clearPendingUnlockNotifications() {
        prefs.edit().remove("pending_unlocks").commit()
    }

    override suspend fun addPendingUnlockNotification(badgeId: String) {
        val current = prefs.getStringSet("pending_unlocks", emptySet()) ?: emptySet()
        val next = current + badgeId
        prefs.edit().putStringSet("pending_unlocks", next).commit()
    }
}
