package com.forgecompose.workouttracker.coaching

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Lightweight persistence for the coaching system. Plans and goals are stored as
 * JSON in a private [SharedPreferences] file. This is intentionally separate from
 * the encrypted AI-secrets store ([com.forgecompose.workouttracker.ai.PersonaPrefs])
 * because none of this data is sensitive and it benefits from easy JSON round-trips.
 */
object CoachingStore {
    private const val FILE = "forge_coaching"
    private const val K_PLAN = "current_plan"
    private const val K_HISTORY = "plan_history"
    private const val K_GOALS = "goals"
    private const val HISTORY_LIMIT = 12

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- Current plan ----

    fun saveCurrentPlan(context: Context, plan: WeeklyPlan) {
        runCatching {
            prefs(context).edit().putString(K_PLAN, json.encodeToString(WeeklyPlan.serializer(), plan)).apply()
        }
        appendToHistory(context, plan)
    }

    fun loadCurrentPlan(context: Context): WeeklyPlan? = runCatching {
        prefs(context).getString(K_PLAN, null)?.let { json.decodeFromString(WeeklyPlan.serializer(), it) }
    }.getOrNull()

    fun clearCurrentPlan(context: Context) {
        runCatching { prefs(context).edit().remove(K_PLAN).apply() }
    }

    // ---- Plan history ----

    private fun appendToHistory(context: Context, plan: WeeklyPlan) {
        runCatching {
            val existing = loadHistory(context).filter { it.id != plan.id }
            val updated = (listOf(plan) + existing).take(HISTORY_LIMIT)
            prefs(context).edit()
                .putString(K_HISTORY, json.encodeToString(ListSerializer(WeeklyPlan.serializer()), updated))
                .apply()
        }
    }

    fun loadHistory(context: Context): List<WeeklyPlan> = runCatching {
        prefs(context).getString(K_HISTORY, null)
            ?.let { json.decodeFromString(ListSerializer(WeeklyPlan.serializer()), it) }
            ?: emptyList()
    }.getOrDefault(emptyList())

    // ---- Goals ----

    fun saveGoals(context: Context, goals: List<CoachingGoal>) {
        runCatching {
            prefs(context).edit()
                .putString(K_GOALS, json.encodeToString(ListSerializer(CoachingGoal.serializer()), goals))
                .apply()
        }
    }

    fun loadGoals(context: Context): List<CoachingGoal> = runCatching {
        prefs(context).getString(K_GOALS, null)
            ?.let { json.decodeFromString(ListSerializer(CoachingGoal.serializer()), it) }
            ?: emptyList()
    }.getOrDefault(emptyList())
}
