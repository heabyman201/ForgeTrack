package com.forgecompose.workouttracker.coaching

import kotlinx.serialization.Serializable

/**
 * Domain models for the LLM coaching system.
 *
 * Everything here is [Serializable] so a generated plan, the goal list, and the
 * generation history can be persisted as JSON through [CoachingStore] and round
 * tripped to/from the LLM. Muscle groups are stored as their enum *name* (String)
 * so these models stay decoupled from the muscle analysis layer's enums.
 */

/** A single prescribed exercise inside a [PlanDay]. */
@Serializable
data class PlanExercise(
    val name: String,
    val targetMuscle: String,
    val sets: Int,
    val reps: String,            // e.g. "8-12" or "5"
    val restSeconds: Int,
    val rpe: String,             // e.g. "7-8"
    val notes: String = ""
)

/** One day of the weekly plan. May be a rest/recovery day. */
@Serializable
data class PlanDay(
    val dayName: String,                 // "Monday" ... "Sunday"
    val title: String,                   // "Push — Chest & Triceps"
    val isRestDay: Boolean = false,
    val focusMuscles: List<String> = emptyList(),
    val estimatedMinutes: Int = 0,
    val intensityNote: String = "",
    val exercises: List<PlanExercise> = emptyList()
)

/** A full custom week produced by the coach. */
@Serializable
data class WeeklyPlan(
    val id: String,
    val createdAt: Long,
    val title: String,
    val summary: String,
    val source: String,                  // "Gemini API" | "Local Network" | "On-Device" | "Heuristic Coach"
    val userPrompt: String = "",
    val focusMuscles: List<String> = emptyList(),
    val days: List<PlanDay> = emptyList(),
    val coachNotes: List<String> = emptyList(),
    val weeklySetTargets: Map<String, Int> = emptyMap()  // muscle name -> target weekly sets
) {
    val trainingDays: Int get() = days.count { !it.isRestDay }
    val totalExercises: Int get() = days.sumOf { it.exercises.size }
    val totalSets: Int get() = days.sumOf { d -> d.exercises.sumOf { it.sets } }
    val estimatedWeeklyMinutes: Int get() = days.sumOf { it.estimatedMinutes }
}

enum class GoalStatus { ACTIVE, ACHIEVED, PAUSED }

/** A long-running user goal tracked on the Goals & Progress page. */
@Serializable
data class CoachingGoal(
    val id: String,
    val title: String,
    val description: String = "",
    val focusMuscles: List<String> = emptyList(),
    val targetWeeks: Int = 8,
    val createdAt: Long,
    val progressPct: Int = 0,
    val status: String = "ACTIVE"        // GoalStatus name
) {
    val statusEnum: GoalStatus
        get() = runCatching { GoalStatus.valueOf(status) }.getOrDefault(GoalStatus.ACTIVE)
}

/**
 * A serialisable snapshot of one muscle's computed status. Built from the muscle
 * analysis engine's `MuscleLoad`, it is both fed to the LLM (as prompt context)
 * and used by the deterministic heuristic planner.
 */
@Serializable
data class MuscleSignal(
    val muscle: String,
    val band: String,
    val weeklyProgress: Float,
    val weeklyTarget: Float,
    val injuryRiskPct: Int,
    val adaptationScore: Float,
    val consistencyScore: Float,
    val developmentScore: Float,
    val lastTrainedAgo: String? = null
) {
    /** 0f (no work done) .. 1f+ (target met/exceeded). */
    val targetCompletion: Float
        get() = if (weeklyTarget <= 0f) 0f else (weeklyProgress / weeklyTarget)
}

/** Whole-body recovery context derived from `RecoveryFactors`. */
@Serializable
data class RecoverySnapshot(
    val recoveryEfficacy: Float = 1f,
    val sleepHours: Float? = null,
    val restingHeartRate: Long? = null,
    val proteinGrams: Double? = null,
    val environmentStress: Boolean = false
)

/** Everything the engine needs to build a week. */
data class PlanRequest(
    val userPrompt: String,
    val goalTitle: String,
    val signals: List<MuscleSignal>,
    val recovery: RecoverySnapshot,
    val experience: String,
    val preferredStyle: String,
    val importantMuscles: List<String>,
    val daysAvailable: Int,
    val sessionMinutes: Int,
    val equipment: String
)

/** UI state for plan generation. */
sealed interface PlanGenState {
    data object Idle : PlanGenState
    data class Loading(val stage: String) : PlanGenState
    data class Success(val plan: WeeklyPlan) : PlanGenState
    data class Error(val message: String) : PlanGenState
}
