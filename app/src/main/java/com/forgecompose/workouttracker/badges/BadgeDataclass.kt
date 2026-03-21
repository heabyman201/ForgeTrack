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



enum class BadgeCategory {
    TOTAL_WORKOUTS,
    MUSCLE
}

// You can hook this to your existing Muscle enum instead
enum class MuscleId {
    CHEST, BACK, SHOULDERS, LEGS, ARMS, CORE
}

data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: BadgeCategory,
    val target: Int,
    val muscleId: MuscleId? = null,
    val icon: Int? = null // drawable resource for the future
)


data class BadgeProgress(
    val definition: BadgeDefinition,
    val progress: Int = 0,
    val unlockedAtEpochMillis: Long? = null,
    val hasShownUnlockAnimation: Boolean = false
) {
    val isUnlocked: Boolean get() = progress >= definition.target

    val progressFraction: Float
        get() = (progress.coerceAtMost(definition.target) / definition.target.toFloat())
}
