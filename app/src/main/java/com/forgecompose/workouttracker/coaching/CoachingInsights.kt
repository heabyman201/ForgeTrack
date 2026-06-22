package com.forgecompose.workouttracker.coaching

import android.content.Context
import com.forgecompose.workouttracker.ai.WorkoutEngine
import com.forgecompose.workouttracker.ai.WorkoutRecommendation
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Turns the persisted coaching state — the most-recent [WeeklyPlan], the user's
 * [CoachingGoal]s, and the live [MuscleSignal]s — into short, human-readable lines
 * that other surfaces (the home advice card in particular) can display.
 *
 * This is the bridge between [CoachingEngine] (which *builds* the plan) and the rest
 * of the app: once the user has generated a routine or set a goal on the Coaching
 * screen, [buildCoachingInsights] exposes "what's on today", "what the goal is",
 * "how each muscle is tracking", and "the actual exercises in the chosen plan".
 */
data class CoachingInsights(
    val hasPlan: Boolean,
    val hasGoal: Boolean,
    val title: String,
    val scheduleLines: List<String>,
    val goalLines: List<String>,
    val progressLines: List<String>,
    val exerciseLines: List<String>,
    /** A compact, ready-to-rotate selection drawn from every section above. */
    val messages: List<String>
) {
    val isEmpty: Boolean get() = messages.isEmpty()

    /** A one-line summary handy for enriching an LLM prompt. */
    val promptSummary: String
        get() = (scheduleLines + goalLines + progressLines).joinToString(" ")

    companion object {
        val EMPTY = CoachingInsights(
            hasPlan = false,
            hasGoal = false,
            title = "",
            scheduleLines = emptyList(),
            goalLines = emptyList(),
            progressLines = emptyList(),
            exerciseLines = emptyList(),
            messages = emptyList()
        )
    }
}

/** Returns the current weekday using the same "Monday".."Sunday" names the plan uses. */
fun todayDayName(today: LocalDate = LocalDate.now()): String = when (today.dayOfWeek) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

fun buildCoachingInsights(
    plan: WeeklyPlan?,
    goals: List<CoachingGoal>,
    signals: List<MuscleSignal>,
    today: String = todayDayName(),
    recommendation: WorkoutRecommendation? = null
): CoachingInsights {
    val activeGoal = goals.firstOrNull { it.statusEnum == GoalStatus.ACTIVE }
    if (plan == null && activeGoal == null) return CoachingInsights.EMPTY

    val signalByMuscle = signals.associateBy { it.muscle }

    // ---- Schedule + today's exercises -------------------------------------
    val scheduleLines = mutableListOf<String>()
    val exerciseLines = mutableListOf<String>()
    if (plan != null) {
        val todayDay = plan.days.firstOrNull { it.dayName.equals(today, ignoreCase = true) }
        when {
            todayDay == null -> Unit
            todayDay.isRestDay -> scheduleLines += "Today ($today): Rest & recovery"
            else -> {
                val mins = if (todayDay.estimatedMinutes > 0) " · ~${todayDay.estimatedMinutes}m" else ""
                scheduleLines += "Today ($today): ${todayDay.title}$mins"
                todayDay.exercises.take(4).forEach { ex ->
                    exerciseLines += "${ex.name}: ${ex.sets}×${ex.reps} @ RPE ${ex.rpe}"
                }
                if (todayDay.exercises.size > 4) {
                    exerciseLines += "+${todayDay.exercises.size - 4} more in today's session"
                }
            }
        }
        val trainingDayNames = plan.days.filter { !it.isRestDay }.map { it.dayName.take(3) }
        if (trainingDayNames.isNotEmpty()) {
            scheduleLines += "Schedule: ${plan.trainingDays} days/wk — ${trainingDayNames.joinToString(", ")}"
        }
    }

    // ---- Goal -------------------------------------------------------------
    val goalLines = mutableListOf<String>()
    if (activeGoal != null) {
        goalLines += "Goal: ${activeGoal.title} — ${activeGoal.progressPct}% done"
        if (activeGoal.focusMuscles.isNotEmpty()) {
            goalLines += "Targeting: ${activeGoal.focusMuscles.joinToString(", ")} (${activeGoal.targetWeeks}-wk)"
        }
    }

    // ---- Per-muscle progress ---------------------------------------------
    // Prefer the muscles the user actually cares about: goal focus, then plan
    // focus, then whatever is furthest from its weekly target.
    val focusMuscles = when {
        activeGoal != null && activeGoal.focusMuscles.isNotEmpty() -> activeGoal.focusMuscles
        plan != null && plan.focusMuscles.isNotEmpty() -> plan.focusMuscles
        else -> signals.sortedBy { it.targetCompletion }.take(4).map { it.muscle }
    }
    val targets = plan?.weeklySetTargets ?: emptyMap()
    val progressLines = mutableListOf<String>()
    focusMuscles.distinct().take(4).forEach { m ->
        val sig = signalByMuscle[m]
        when {
            sig != null -> {
                val pct = (sig.targetCompletion * 100f).roundToInt()
                val target = targets[m]
                val detail = if (target != null) {
                    "${sig.weeklyProgress.roundToInt()}/$target sets"
                } else {
                    "${sig.weeklyProgress.roundToInt()} sets"
                }
                progressLines += "$m: $detail ($pct%)"
            }
            targets[m] != null -> progressLines += "$m: 0/${targets[m]} sets (0%)"
        }
    }

    val recLine = recommendation?.let {
        "Model read: ${it.intensityTier} ${it.exerciseCategory} day suits you"
    }

    // ---- Compact rotation set --------------------------------------------
    val messages = buildList {
        scheduleLines.firstOrNull()?.let { add(it) }
        goalLines.firstOrNull()?.let { add(it) }
        addAll(progressLines.take(2))
        addAll(exerciseLines.take(2))
        recLine?.let { add(it) }
    }.filter { it.isNotBlank() }.distinct()

    val title = when {
        plan != null -> plan.title
        activeGoal != null -> "Goal: ${activeGoal.title}"
        else -> "Your plan"
    }

    return CoachingInsights(
        hasPlan = plan != null,
        hasGoal = activeGoal != null,
        title = title,
        scheduleLines = scheduleLines,
        goalLines = goalLines,
        progressLines = progressLines,
        exerciseLines = exerciseLines,
        messages = messages
    )
}

/**
 * Best-effort bridge from the coaching muscle signals to the on-device TFLite
 * [WorkoutEngine]. Returns null if the model/asset isn't bundled or anything throws,
 * so the coaching UI degrades gracefully when the model is unavailable.
 *
 * The 8-element volume vector mirrors the major movement groups the model was
 * trained on; muscles missing from the signal set contribute 0 volume.
 */
fun recommendationFromSignals(
    context: Context,
    signals: List<MuscleSignal>,
    recovery: RecoverySnapshot,
    trainingWeek: Float
): WorkoutRecommendation? = runCatching {
    if (signals.isEmpty()) return null
    val engine = WorkoutEngine(context)
    try {
        val volumeOrder = listOf(
            "Pecs", "Lats", "Delts", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes"
        )
        val byMuscle = signals.associateBy { it.muscle }
        val weeklyVolume = FloatArray(volumeOrder.size) { i ->
            byMuscle[volumeOrder[i]]?.weeklyProgress ?: 0f
        }
        val sleep = recovery.sleepHours ?: 7.5f
        engine.recommend(
            sleepHours = sleep,
            daysSinceLast = 1f,
            weeklyVolume = weeklyVolume,
            trainingWeek = trainingWeek.coerceAtLeast(1f),
            avgSleep7d = sleep
        )
    } finally {
        engine.close()
    }
}.getOrNull()
