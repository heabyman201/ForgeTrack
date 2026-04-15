package com.forgecompose.workouttracker.ai

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

private const val AI_ADVICE_VISIBLE_MS = 8_000L
private const val SCIENCE_FACT_VISIBLE_MS = 5_000L
private const val MAX_PROMPT_LEARNED_RULES = 6
private const val MAX_PROMPT_WORKOUT_RULES = 4
private const val GOOGLE_AI_STUDIO_MODEL = "gemini-3.1-flash-lite-preview"
private const val GEMINI_UTILITY_TAG = "GeminiUtility"
private const val GOOGLE_AI_STUDIO_ENDPOINT =
    "https://generativelanguage.googleapis.com/v1beta/models/$GOOGLE_AI_STUDIO_MODEL:generateContent"

private fun logSystemPrompt(source: String, systemPrompt: String) {
    Log.d(GEMINI_UTILITY_TAG, "System prompt [$source]: $systemPrompt")
}

private fun logLearnedMemory(memoryBlock: String) {
    Log.d(
        GEMINI_UTILITY_TAG,
        if (memoryBlock.isBlank()) {
            "Learned memory: <empty>"
        } else {
            "Learned memory: $memoryBlock"
        }
    )
}

private fun cleanRuleList(values: List<String>, maxItems: Int): List<String> {
    val seen = linkedSetOf<String>()
    values.forEach { raw ->
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isNotBlank()) {
            seen.add(cleaned)
        }
    }
    return seen.take(maxItems)
}

private fun normalizeWorkoutName(name: String): String =
    name.trim().lowercase().replace(Regex("\\s+"), " ")

private fun inferWorkoutName(contextPrompt: String, payload: List<String>): String? {
    val candidates = buildList {
        addAll(payload)
        add(contextPrompt)
    }
    val regexes = listOf(
        Regex("""(?:Workout|Exercise):\s*([^.,\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""progress for\s+([^.,\n]+)""", RegexOption.IGNORE_CASE)
    )

    for (candidate in candidates) {
        val line = candidate.trim()
        for (regex in regexes) {
            val match = regex.find(line) ?: continue
            val value = match.groupValues.getOrNull(1).orEmpty()
                .replace(Regex("""\s+on\s+.*$""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+Goal:.*$""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (value.isNotBlank()) return value
        }
    }
    return null
}

private fun isBodyweightExercise(workoutName: String): Boolean {
    val normalized = normalizeWorkoutName(workoutName)
    if (normalized.isBlank()) return false
    val keywords = listOf(
        "bodyweight",
        "push up",
        "pushup",
        "pull up",
        "pullup",
        "chin up",
        "chinup",
        "dip",
        "plank",
        "sit up",
        "situp",
        "crunch",
        "burpee",
        "mountain climber",
        "jumping jack",
        "air squat",
        "pistol squat",
        "lunge",
        "glute bridge",
        "wall sit",
        "handstand",
        "hollow hold",
        "dead hang",
        "leg raise"
    )
    return keywords.any { normalized.contains(it) }
}

private fun loadProgressionInstruction(workoutName: String): String {
    return if (isBodyweightExercise(workoutName)) {
        "If the workout is a bodyweight movement, do not suggest increasing or decreasing weight. Instead, progress with reps, sets, tempo, pauses, range of motion, rest, unilateral difficulty, or hold time."
    } else {
        ""
    }
}

data class GeminiSignalSnapshot(
    val capturedAtEpochMs: Long,
    val recoveryEfficacy: Float,
    val avgInjuryRisk: Float,
    val avgLoadScore: Float,
    val highReadinessCount: Int,
    val cautionCount: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("capturedAtEpochMs", capturedAtEpochMs)
        put("recoveryEfficacy", recoveryEfficacy.toDouble())
        put("avgInjuryRisk", avgInjuryRisk.toDouble())
        put("avgLoadScore", avgLoadScore.toDouble())
        put("highReadinessCount", highReadinessCount)
        put("cautionCount", cautionCount)
    }

    companion object {
        fun fromJson(raw: String?): GeminiSignalSnapshot? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                GeminiSignalSnapshot(
                    capturedAtEpochMs = json.optLong("capturedAtEpochMs"),
                    recoveryEfficacy = json.optDouble("recoveryEfficacy", 1.0).toFloat(),
                    avgInjuryRisk = json.optDouble("avgInjuryRisk", 0.0).toFloat(),
                    avgLoadScore = json.optDouble("avgLoadScore", 0.0).toFloat(),
                    highReadinessCount = json.optInt("highReadinessCount"),
                    cautionCount = json.optInt("cautionCount")
                )
            }.getOrNull()
        }
    }
}

private data class GeminiAdviceMemory(
    val generatedAtEpochMs: Long,
    val systemPrompt: String,
    val contextPrompt: String,
    val payloadSummary: String,
    val advice: String,
    val baselineSignalSnapshot: GeminiSignalSnapshot? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("generatedAtEpochMs", generatedAtEpochMs)
        put("systemPrompt", systemPrompt)
        put("contextPrompt", contextPrompt)
        put("payloadSummary", payloadSummary)
        put("advice", advice)
        if (baselineSignalSnapshot != null) {
            put("baselineSignalSnapshot", baselineSignalSnapshot.toJson())
        }
    }

    val signature: String
        get() = "${systemPrompt.trim()}|${contextPrompt.trim()}|${advice.trim()}"

    companion object {
        fun fromJson(raw: String?): GeminiAdviceMemory? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                GeminiAdviceMemory(
                    generatedAtEpochMs = json.optLong("generatedAtEpochMs"),
                    systemPrompt = json.optString("systemPrompt"),
                    contextPrompt = json.optString("contextPrompt"),
                    payloadSummary = json.optString("payloadSummary"),
                    advice = json.optString("advice"),
                    baselineSignalSnapshot = json.optJSONObject("baselineSignalSnapshot")?.toString()
                        ?.let(GeminiSignalSnapshot::fromJson)
                )
            }.getOrNull()
        }
    }
}

data class WorkoutTrendSnapshot(
    val workoutName: String,
    val source: String,
    val capturedAtEpochMs: Long,
    val phase: String,
    val strengthChangePct: Double? = null,
    val volumeChangePct: Double? = null,
    val avgGapDays: Double? = null,
    val rpe: Int? = null,
    val fatigue: Int? = null,
    val weight: Double? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val intensityScore: Int? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("workoutName", workoutName)
        put("source", source)
        put("capturedAtEpochMs", capturedAtEpochMs)
        put("phase", phase)
        if (strengthChangePct != null) put("strengthChangePct", strengthChangePct)
        if (volumeChangePct != null) put("volumeChangePct", volumeChangePct)
        if (avgGapDays != null) put("avgGapDays", avgGapDays)
        if (rpe != null) put("rpe", rpe)
        if (fatigue != null) put("fatigue", fatigue)
        if (weight != null) put("weight", weight)
        if (sets != null) put("sets", sets)
        if (reps != null) put("reps", reps)
        if (intensityScore != null) put("intensityScore", intensityScore)
    }

    companion object {
        fun fromJson(raw: String?): WorkoutTrendSnapshot? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                WorkoutTrendSnapshot(
                    workoutName = json.optString("workoutName"),
                    source = json.optString("source"),
                    capturedAtEpochMs = json.optLong("capturedAtEpochMs"),
                    phase = json.optString("phase"),
                    strengthChangePct = json.optDoubleOrNull("strengthChangePct"),
                    volumeChangePct = json.optDoubleOrNull("volumeChangePct"),
                    avgGapDays = json.optDoubleOrNull("avgGapDays"),
                    rpe = json.optIntOrNull("rpe"),
                    fatigue = json.optIntOrNull("fatigue"),
                    weight = json.optDoubleOrNull("weight"),
                    sets = json.optIntOrNull("sets"),
                    reps = json.optIntOrNull("reps"),
                    intensityScore = json.optIntOrNull("intensityScore")
                )
            }.getOrNull()
        }
    }
}

private data class PendingWorkoutAdvice(
    val workoutName: String,
    val source: String,
    val generatedAtEpochMs: Long,
    val advice: String,
    val adviceType: String,
    val phaseAtGeneration: String,
    val strengthChangeAtGeneration: Double? = null,
    val volumeChangeAtGeneration: Double? = null,
    val rpeAtGeneration: Int? = null,
    val fatigueAtGeneration: Int? = null,
    val intensityAtGeneration: Int? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("workoutName", workoutName)
        put("source", source)
        put("generatedAtEpochMs", generatedAtEpochMs)
        put("advice", advice)
        put("adviceType", adviceType)
        put("phaseAtGeneration", phaseAtGeneration)
        if (strengthChangeAtGeneration != null) put("strengthChangeAtGeneration", strengthChangeAtGeneration)
        if (volumeChangeAtGeneration != null) put("volumeChangeAtGeneration", volumeChangeAtGeneration)
        if (rpeAtGeneration != null) put("rpeAtGeneration", rpeAtGeneration)
        if (fatigueAtGeneration != null) put("fatigueAtGeneration", fatigueAtGeneration)
        if (intensityAtGeneration != null) put("intensityAtGeneration", intensityAtGeneration)
    }

    val signature: String
        get() = "${workoutName.trim()}|${generatedAtEpochMs}|${advice.trim()}"

    companion object {
        fun fromJson(raw: String?): PendingWorkoutAdvice? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                PendingWorkoutAdvice(
                    workoutName = json.optString("workoutName"),
                    source = json.optString("source"),
                    generatedAtEpochMs = json.optLong("generatedAtEpochMs"),
                    advice = json.optString("advice"),
                    adviceType = json.optString("adviceType"),
                    phaseAtGeneration = json.optString("phaseAtGeneration"),
                    strengthChangeAtGeneration = json.optDoubleOrNull("strengthChangeAtGeneration"),
                    volumeChangeAtGeneration = json.optDoubleOrNull("volumeChangeAtGeneration"),
                    rpeAtGeneration = json.optIntOrNull("rpeAtGeneration"),
                    fatigueAtGeneration = json.optIntOrNull("fatigueAtGeneration"),
                    intensityAtGeneration = json.optIntOrNull("intensityAtGeneration")
                )
            }.getOrNull()
        }
    }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key) else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

object GeminiAdaptiveMemoryStore {
    private const val PREFS_NAME = "gemini_adaptive_memory"
    private const val K_PROMPT_STATE = "prompt_state_json"
    private const val K_LAST_SIGNAL_SNAPSHOT = "last_signal_snapshot"
    private const val K_PENDING_ADVICE = "pending_advice"
    private const val K_LEARNED_APPROACHES = "learned_approaches_json"
    private const val K_LAST_PROMOTED_SIGNATURE = "last_promoted_signature"
    private const val K_WORKOUT_TRENDS = "workout_trends_json"
    private const val K_PENDING_WORKOUT_ADVICES = "pending_workout_advices_json"
    private const val K_WORKOUT_LEARNED_RULES = "workout_learned_rules_json"
    private const val K_LAST_WORKOUT_EVAL = "last_workout_eval_json"

    private data class PromptState(
        val version: Int = 1,
        val learnedRules: List<String> = emptyList(),
        val workoutRules: Map<String, List<String>> = emptyMap(),
        val mutationCount: Int = 0,
        val lastMutatedAtEpochMs: Long = 0L
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("version", version)
            put("mutationCount", mutationCount)
            put("lastMutatedAtEpochMs", lastMutatedAtEpochMs)
            put("learnedRules", JSONArray(learnedRules))
            put("workoutRules", JSONObject().apply {
                workoutRules.forEach { (workout, rules) ->
                    put(workout, JSONArray(rules))
                }
            })
        }

        fun normalized(): PromptState {
            val cleanedLearned = cleanRuleList(learnedRules, MAX_PROMPT_LEARNED_RULES)
            val cleanedWorkout = workoutRules
                .mapValues { (_, rules) -> cleanRuleList(rules, MAX_PROMPT_WORKOUT_RULES) }
                .filterValues { it.isNotEmpty() }
            return copy(
                learnedRules = cleanedLearned,
                workoutRules = cleanedWorkout
            )
        }

        companion object {
            fun fromJson(raw: String?): PromptState? {
                if (raw.isNullOrBlank()) return null
                return runCatching {
                    val json = JSONObject(raw)
                    val learned = buildList {
                        val learnedArray = json.optJSONArray("learnedRules")
                        if (learnedArray != null) {
                            for (index in 0 until learnedArray.length()) {
                                val value = learnedArray.optString(index).trim()
                                if (value.isNotBlank()) add(value)
                            }
                        }
                    }
                    val workoutRules = buildMap {
                        val workoutObject = json.optJSONObject("workoutRules")
                        if (workoutObject != null) {
                            val keys = workoutObject.keys()
                            while (keys.hasNext()) {
                                val key = keys.next().trim()
                                val array = workoutObject.optJSONArray(key) ?: continue
                                val rules = buildList {
                                    for (index in 0 until array.length()) {
                                        val value = array.optString(index).trim()
                                        if (value.isNotBlank()) add(value)
                                    }
                                }
                                if (key.isNotBlank() && rules.isNotEmpty()) {
                                    put(key, rules)
                                }
                            }
                        }
                    }
                    PromptState(
                        version = json.optInt("version", 1),
                        learnedRules = learned,
                        workoutRules = workoutRules,
                        mutationCount = json.optInt("mutationCount", 0),
                        lastMutatedAtEpochMs = json.optLong("lastMutatedAtEpochMs", 0L)
                    ).normalized()
                }.getOrNull()
            }

            fun fromLegacy(
                learnedRules: List<String>,
                workoutRules: Map<String, List<String>>
            ): PromptState = PromptState(
                learnedRules = learnedRules,
                workoutRules = workoutRules
            ).normalized()
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readPromptState(prefs: android.content.SharedPreferences): PromptState {
        PromptState.fromJson(prefs.getString(K_PROMPT_STATE, null))?.let { return it }
        val migrated = PromptState.fromLegacy(
            learnedRules = readLearnedApproachesFromPrefs(prefs),
            workoutRules = readAllWorkoutRulesFromPrefs(prefs)
        )
        persistPromptState(prefs, migrated)
        return migrated
    }

    private fun persistPromptState(
        prefs: android.content.SharedPreferences,
        state: PromptState
    ) {
        val normalized = state.normalized()
        val workoutJson = JSONObject().apply {
            normalized.workoutRules.forEach { (workout, rules) ->
                put(workout, JSONArray(rules))
            }
        }
        prefs.edit()
            .putString(K_PROMPT_STATE, normalized.toJson().toString())
            .putString(K_LEARNED_APPROACHES, JSONArray(normalized.learnedRules).toString())
            .putString(K_WORKOUT_LEARNED_RULES, workoutJson.toString())
            .apply()
    }

    private fun updatePromptState(
        prefs: android.content.SharedPreferences,
        updater: (PromptState) -> PromptState
    ): PromptState {
        val current = readPromptState(prefs)
        val updated = updater(current).normalized().copy(
            mutationCount = current.mutationCount + 1,
            lastMutatedAtEpochMs = System.currentTimeMillis()
        ).normalized()
        persistPromptState(prefs, updated)
        return updated
    }

    private fun promoteRule(rules: List<String>, rule: String, maxItems: Int): List<String> {
        val cleanedRule = rule.trim().replace(Regex("\\s+"), " ")
        if (cleanedRule.isBlank()) return rules
        val next = mutableListOf<String>()
        next.add(cleanedRule)
        rules.forEach { existing ->
            if (!existing.equals(cleanedRule, ignoreCase = true) && next.size < maxItems) {
                next.add(existing)
            }
        }
        return cleanRuleList(next, maxItems)
    }

    private fun addWorkoutRule(
        workoutRules: Map<String, List<String>>,
        workoutName: String,
        rule: String
    ): Map<String, List<String>> {
        val normalizedName = normalizeWorkoutName(workoutName)
        if (normalizedName.isBlank()) return workoutRules
        val currentRules = workoutRules[normalizedName].orEmpty()
        val updatedRules = promoteRule(currentRules, rule, MAX_PROMPT_WORKOUT_RULES)
        return workoutRules + (normalizedName to updatedRules)
    }

    private fun readAllWorkoutRulesFromPrefs(
        prefs: android.content.SharedPreferences
    ): Map<String, List<String>> {
        val root = readJsonObject(prefs, K_WORKOUT_LEARNED_RULES)
        val rules = mutableMapOf<String, List<String>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next().trim()
            if (key.isBlank()) continue
            val array = root.optJSONArray(key) ?: continue
            val values = buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
            if (values.isNotEmpty()) {
                rules[key] = values
            }
        }
        return rules
    }

    fun rememberGeneratedAdvice(
        context: Context,
        systemPrompt: String,
        contextPrompt: String,
        payload: List<String>,
        advice: String
    ) {
        if (advice.isBlank()) return
        val prefs = prefs(context)
        val baselineSignalSnapshot =
            GeminiSignalSnapshot.fromJson(prefs.getString(K_LAST_SIGNAL_SNAPSHOT, null))
        val memory = GeminiAdviceMemory(
            generatedAtEpochMs = System.currentTimeMillis(),
            systemPrompt = systemPrompt,
            contextPrompt = contextPrompt,
            payloadSummary = payload.filter { it.isNotBlank() }.joinToString(" | "),
            advice = advice,
            baselineSignalSnapshot = baselineSignalSnapshot
        )
        Log.d(
            GEMINI_UTILITY_TAG,
            if (baselineSignalSnapshot == null) {
                "Pending advice stored without baseline signal snapshot."
            } else {
                "Pending advice stored with baseline signal snapshot at ${baselineSignalSnapshot.capturedAtEpochMs}."
            }
        )
        prefs.edit()
            .putString(K_PENDING_ADVICE, memory.toJson().toString())
            .apply()
    }

    fun rememberGeneratedWorkoutAdvice(
        context: Context,
        contextPrompt: String,
        payload: List<String>,
        advice: String
    ) {
        if (advice.isBlank()) return
        val source = inferAdviceSource(contextPrompt)
        if (source == "live_workout") return
        val workoutName = inferWorkoutName(contextPrompt, payload) ?: return
        val prefs = prefs(context)
        val trend = readWorkoutTrendFromPrefs(prefs, normalizeWorkoutName(workoutName)) ?: return
        val pending = PendingWorkoutAdvice(
            workoutName = trend.workoutName,
            source = source,
            generatedAtEpochMs = System.currentTimeMillis(),
            advice = advice,
            adviceType = inferAdviceType(advice),
            phaseAtGeneration = trend.phase,
            strengthChangeAtGeneration = trend.strengthChangePct,
            volumeChangeAtGeneration = trend.volumeChangePct,
            rpeAtGeneration = trend.rpe,
            fatigueAtGeneration = trend.fatigue,
            intensityAtGeneration = trend.intensityScore
        )
        writeJsonEntry(prefs, K_PENDING_WORKOUT_ADVICES, normalizeWorkoutName(trend.workoutName), pending.toJson())
    }

    fun recordMuscleSignals(context: Context, snapshot: GeminiSignalSnapshot) {
        val prefs = prefs(context)
        val previous = GeminiSignalSnapshot.fromJson(prefs.getString(K_LAST_SIGNAL_SNAPSHOT, null))
        val pendingAdvice = GeminiAdviceMemory.fromJson(prefs.getString(K_PENDING_ADVICE, null))

        if (previous != null && pendingAdvice != null) {
            maybePromoteLearnedApproach(prefs, previous, snapshot, pendingAdvice)
        }

        prefs.edit()
            .putString(K_LAST_SIGNAL_SNAPSHOT, snapshot.toJson().toString())
            .apply()
    }

    fun recordWorkoutTrendSnapshot(context: Context, snapshot: WorkoutTrendSnapshot) {
        val prefs = prefs(context)
        val normalizedName = normalizeWorkoutName(snapshot.workoutName)
        val previous = readWorkoutTrendFromPrefs(prefs, normalizedName)
        val pending = readPendingWorkoutAdviceFromPrefs(prefs, normalizedName)
        if (previous != null && pending != null) {
            maybePromoteWorkoutRule(prefs, previous, snapshot, pending)
        }
        writeJsonEntry(prefs, K_WORKOUT_TRENDS, normalizedName, snapshot.toJson())
    }

    fun recordWorkoutCompletionSignals(
        context: Context,
        workoutName: String,
        rpe: Int,
        fatigue: Int,
        intensityScore: Int,
        systemicDrain: Float,
        weight: Double?,
        sets: Int?,
        reps: Int?
    ) {
        val normalizedFatigue = (fatigue / 10f).coerceIn(0f, 1f)
        val normalizedRpe = (rpe / 10f).coerceIn(0f, 1f)
        val normalizedIntensity = (intensityScore / 10f).coerceIn(0f, 1f)
        val normalizedDrain = (systemicDrain / 100f).coerceIn(0f, 1f)
        val recoveryEfficacy = (
            1f -
                (normalizedFatigue * 0.42f) -
                (normalizedDrain * 0.28f) -
                (((normalizedRpe - 0.6f).coerceAtLeast(0f)) * 0.18f)
            ).coerceIn(0.05f, 1f)
        val avgInjuryRisk = (
            (normalizedFatigue * 0.4f) +
                (normalizedRpe * 0.32f) +
                (normalizedDrain * 0.18f) +
                ((1f - normalizedIntensity) * 0.1f)
            ).coerceIn(0f, 1f)
        val avgLoadScore = (
            (normalizedIntensity * 0.45f) +
                (normalizedRpe * 0.25f) +
                ((1f - normalizedFatigue) * 0.15f) +
                ((1f - normalizedDrain) * 0.15f)
            ).coerceIn(0f, 1f)
        val readinessCount = when {
            fatigue <= 5 && rpe <= 7 && intensityScore >= 6 -> 3
            fatigue <= 6 && intensityScore >= 5 -> 2
            fatigue <= 7 -> 1
            else -> 0
        }
        val cautionCount = when {
            fatigue >= 8 || rpe >= 9 || systemicDrain >= 78f -> 3
            fatigue >= 7 || rpe >= 8 || systemicDrain >= 62f -> 2
            fatigue >= 6 || rpe >= 7 -> 1
            else -> 0
        }
        val phase = when {
            fatigue >= 8 || rpe >= 9 -> "Deload Window"
            intensityScore >= 8 && fatigue <= 6 -> "Peaking"
            fatigue >= 6 -> "Recovering"
            else -> "Maintenance"
        }

        recordMuscleSignals(
            context = context,
            snapshot = GeminiSignalSnapshot(
                capturedAtEpochMs = System.currentTimeMillis(),
                recoveryEfficacy = recoveryEfficacy,
                avgInjuryRisk = avgInjuryRisk,
                avgLoadScore = avgLoadScore,
                highReadinessCount = readinessCount,
                cautionCount = cautionCount
            )
        )
        recordWorkoutTrendSnapshot(
            context = context,
            snapshot = WorkoutTrendSnapshot(
                workoutName = workoutName,
                source = "session_completion",
                capturedAtEpochMs = System.currentTimeMillis(),
                phase = phase,
                rpe = rpe,
                fatigue = fatigue,
                weight = weight,
                sets = sets,
                reps = reps,
                intensityScore = intensityScore
            )
        )
        Log.d(
            GEMINI_UTILITY_TAG,
            "Recorded post-workout learning signals for $workoutName: rpe=$rpe fatigue=$fatigue intensity=$intensityScore drain=${systemicDrain.roundToInt()}"
        )
    }

    fun buildPromptMemoryBlock(
        context: Context,
        contextPrompt: String,
        payload: List<String>
    ): String {
        val state = readPromptState(prefs(context))
        val workoutMemory = buildWorkoutMemoryBlock(contextPrompt, payload, state)
        if (state.learnedRules.isEmpty() && workoutMemory.isBlank()) {
            logLearnedMemory("")
            return ""
        }
        val memoryBlock = buildString {
            if (state.learnedRules.isNotEmpty()) {
                append(" Mutable prompt memory from prior wins: ")
                state.learnedRules.take(MAX_PROMPT_LEARNED_RULES).forEachIndexed { index, memory ->
                    append(index + 1)
                    append(". ")
                    append(memory)
                    append(' ')
                }
                append("Reuse only when the context matches.")
            }
            if (workoutMemory.isNotBlank()) {
                append(' ')
                append(workoutMemory)
            }
        }.trim()
        logLearnedMemory(memoryBlock)
        return memoryBlock
    }

    private fun buildWorkoutMemoryBlock(
        contextPrompt: String,
        payload: List<String>,
        state: PromptState
    ): String {
        val workoutName = inferWorkoutName(contextPrompt, payload) ?: return ""
        val normalizedName = normalizeWorkoutName(workoutName)
        val rules = state.workoutRules[normalizedName].orEmpty()
        if (rules.isEmpty()) return ""

        return buildString {
            append(" Workout-specific prompt rules for ")
            append(workoutName)
            append(": ")
            rules.take(MAX_PROMPT_WORKOUT_RULES).forEachIndexed { index, rule ->
                append(index + 1)
                append(". ")
                append(rule)
                append(' ')
            }
            append("Keep these rules active for this workout only.")
        }.trim()
    }

    private fun maybePromoteLearnedApproach(
        prefs: android.content.SharedPreferences,
        previous: GeminiSignalSnapshot,
        current: GeminiSignalSnapshot,
        pendingAdvice: GeminiAdviceMemory
    ) {
        if (!shouldLearnFrom(pendingAdvice)) {
            Log.d(GEMINI_UTILITY_TAG, "Learned memory skipped: advice did not pass learnability checks.")
            return
        }
        val baseline = pendingAdvice.baselineSignalSnapshot ?: previous
        if (current.capturedAtEpochMs <= pendingAdvice.generatedAtEpochMs) {
            Log.d(
                GEMINI_UTILITY_TAG,
                "Learned memory skipped: current signal is not newer than the generated advice."
            )
            return
        }
        if (baseline.capturedAtEpochMs > pendingAdvice.generatedAtEpochMs) {
            Log.d(
                GEMINI_UTILITY_TAG,
                "Learned memory skipped: baseline snapshot is newer than the advice timestamp."
            )
            return
        }
        if (prefs.getString(K_LAST_PROMOTED_SIGNATURE, null) == pendingAdvice.signature) {
            Log.d(GEMINI_UTILITY_TAG, "Learned memory skipped: pending advice was already promoted.")
            return
        }

        val improvements = mutableListOf<String>()
        if (current.recoveryEfficacy - baseline.recoveryEfficacy >= 0.05f) {
            improvements += "better recovery"
        }
        if (baseline.avgInjuryRisk - current.avgInjuryRisk >= 0.04f) {
            improvements += "lower injury risk"
        }
        if (current.highReadinessCount > baseline.highReadinessCount) {
            improvements += "more muscles ready to push"
        }
        if (current.cautionCount < baseline.cautionCount) {
            improvements += "less fatigue pressure"
        }
        if (current.avgLoadScore - baseline.avgLoadScore >= 0.05f) {
            improvements += "higher training readiness"
        }
        if (improvements.isEmpty()) {
            Log.d(
                GEMINI_UTILITY_TAG,
                "Learned memory skipped: no positive signal improvements were detected against the baseline snapshot."
            )
            return
        }

        val memory = buildLearnedApproach(pendingAdvice, improvements.distinct())
        if (memory.isBlank()) {
            Log.d(GEMINI_UTILITY_TAG, "Learned memory skipped: learned approach text was blank.")
            return
        }
        updatePromptState(prefs) { current ->
            val learned = promoteRule(current.learnedRules, memory, MAX_PROMPT_LEARNED_RULES)
            current.copy(learnedRules = learned)
        }
        Log.d(
            GEMINI_UTILITY_TAG,
            "Learned memory promoted from signals: $memory | improvements=${improvements.distinct().joinToString()}"
        )
        prefs.edit()
            .putString(K_LAST_PROMOTED_SIGNATURE, pendingAdvice.signature)
            .apply()
    }

    private fun maybePromoteWorkoutRule(
        prefs: android.content.SharedPreferences,
        previous: WorkoutTrendSnapshot,
        current: WorkoutTrendSnapshot,
        pendingAdvice: PendingWorkoutAdvice
    ) {
        if (pendingAdvice.generatedAtEpochMs <= previous.capturedAtEpochMs) return
        if (current.capturedAtEpochMs <= pendingAdvice.generatedAtEpochMs) return
        val evalKey = "${normalizeWorkoutName(pendingAdvice.workoutName)}|${pendingAdvice.signature}"
        if (readJsonObject(prefs, K_LAST_WORKOUT_EVAL).optString(evalKey).isNotBlank()) return

        val rule = evaluateWorkoutAdviceRule(previous, current, pendingAdvice)
        if (rule == null) {
            writeJsonEntry(prefs, K_LAST_WORKOUT_EVAL, evalKey, JSONObject().put("done", true))
            return
        }

        updatePromptState(prefs) { current ->
            val updatedWorkoutRules = addWorkoutRule(current.workoutRules, pendingAdvice.workoutName, rule)
            current.copy(workoutRules = updatedWorkoutRules)
        }
        writeJsonEntry(prefs, K_LAST_WORKOUT_EVAL, evalKey, JSONObject().put("done", true))
    }

    private fun evaluateWorkoutAdviceRule(
        previous: WorkoutTrendSnapshot,
        current: WorkoutTrendSnapshot,
        pendingAdvice: PendingWorkoutAdvice
    ): String? {
        val workoutName = current.workoutName.ifBlank { pendingAdvice.workoutName }
        val type = pendingAdvice.adviceType
        val previousVolumeUnits = (previous.weight ?: 0.0) * (previous.reps ?: 0) * (previous.sets ?: 0)
        val currentVolumeUnits = (current.weight ?: 0.0) * (current.reps ?: 0) * (current.sets ?: 0)
        val weightIncreased = current.weight != null &&
            previous.weight != null &&
            current.weight > previous.weight + 0.24
        val repsImproved = current.reps != null &&
            previous.reps != null &&
            current.reps > previous.reps
        val setsImproved = current.sets != null &&
            previous.sets != null &&
            current.sets > previous.sets
        val rawPerformanceImproved = weightIncreased || repsImproved || setsImproved ||
            (previousVolumeUnits > 0.0 && currentVolumeUnits > previousVolumeUnits * 1.04)
        val rawPerformanceDropped = current.weight != null &&
            previous.weight != null &&
            current.weight < previous.weight - 0.24 ||
            (current.reps != null && previous.reps != null && current.reps < previous.reps) ||
            (current.sets != null && previous.sets != null && current.sets < previous.sets) ||
            (previousVolumeUnits > 0.0 && currentVolumeUnits < previousVolumeUnits * 0.96)
        val strengthDropped = current.strengthChangePct != null &&
            previous.strengthChangePct != null &&
            current.strengthChangePct < previous.strengthChangePct - 3.0 ||
            rawPerformanceDropped
        val strengthImproved = current.strengthChangePct != null &&
            previous.strengthChangePct != null &&
            current.strengthChangePct > previous.strengthChangePct + 2.0 ||
            rawPerformanceImproved
        val fatigueUp = current.fatigue != null &&
            pendingAdvice.fatigueAtGeneration != null &&
            current.fatigue >= pendingAdvice.fatigueAtGeneration + 2
        val fatigueDown = current.fatigue != null &&
            pendingAdvice.fatigueAtGeneration != null &&
            current.fatigue <= pendingAdvice.fatigueAtGeneration - 2
        val intensityControlled = current.intensityScore != null &&
            pendingAdvice.intensityAtGeneration != null &&
            current.intensityScore <= pendingAdvice.intensityAtGeneration + 1
        val intensityImproved = current.intensityScore != null &&
            pendingAdvice.intensityAtGeneration != null &&
            current.intensityScore >= pendingAdvice.intensityAtGeneration
        val intensitySuffered = current.rpe != null &&
            pendingAdvice.rpeAtGeneration != null &&
            current.rpe >= pendingAdvice.rpeAtGeneration + 1 &&
            (fatigueUp || current.phase.equals("Cooling Down", ignoreCase = true)) ||
            (current.intensityScore != null &&
                pendingAdvice.intensityAtGeneration != null &&
                current.intensityScore < pendingAdvice.intensityAtGeneration - 1)
        val movedIntoCooling = current.phase.equals("Cooling Down", ignoreCase = true) ||
            current.phase.equals("Deload Window", ignoreCase = true)
        val movedIntoPeaking = current.phase.equals("Peaking", ignoreCase = true)
        val volumeImproved = current.volumeChangePct != null &&
            previous.volumeChangePct != null &&
            current.volumeChangePct > previous.volumeChangePct + 5.0 ||
            currentVolumeUnits > previousVolumeUnits * 1.06

        return when (type) {
            "load_up" -> when {
                strengthDropped || intensitySuffered || movedIntoCooling || fatigueUp ->
                    "For $workoutName, avoid aggressive weight increases after ${pendingAdvice.phaseAtGeneration.lowercase()} signals because intensity dropped or fatigue lingered."
                weightIncreased && rawPerformanceImproved && !fatigueUp && intensityControlled ->
                    "For $workoutName, weight jumps work when the next session still holds reps and session intensity together."
                movedIntoPeaking && strengthImproved && !fatigueUp ->
                    "For $workoutName, small weight increases work when the trend is peaking and fatigue stays controlled."
                else -> null
            }

            "back_off" -> when {
                fatigueDown || (previous.phase.equals("Cooling Down", ignoreCase = true) && !current.phase.equals("Cooling Down", ignoreCase = true)) ->
                    "For $workoutName, backing off load or volume helps when the trend is cooling down or fatigue is high."
                strengthDropped && !fatigueDown ->
                    "For $workoutName, deload calls should stay brief; avoid overextending them if performance keeps falling without recovery benefit."
                else -> null
            }

            "volume_up" -> when {
                fatigueUp || movedIntoCooling ->
                    "For $workoutName, avoid adding volume when fatigue is already climbing."
                setsImproved && volumeImproved && !fatigueUp && intensityImproved ->
                    "For $workoutName, extra sets work when intensity stays solid and recovery does not slide."
                volumeImproved && !fatigueUp ->
                    "For $workoutName, extra volume works when recovery stays stable."
                else -> null
            }

            "maintain" -> when {
                fatigueDown && !strengthDropped ->
                    "For $workoutName, holding load steady works well when fatigue is elevated."
                rawPerformanceImproved && intensityControlled ->
                    "For $workoutName, the user performs well when the session starts from a steady baseline before pushing harder."
                else -> null
            }

            else -> null
        }
    }

    private fun inferAdviceSource(contextPrompt: String): String {
        val prompt = contextPrompt.lowercase()
        return when {
            prompt.contains("fitness analyst") -> "analytics"
            prompt.contains("performance coach") -> "detail"
            prompt.contains("active workout") -> "live_workout"
            else -> "generic"
        }
    }

    private fun inferAdviceType(advice: String): String {
        val normalized = advice.lowercase()
        return when {
            Regex("""(increase|add|bump|go)\s+(the\s+)?(weight|load)""").containsMatchIn(normalized) ||
                Regex("""go heavier|heavier set|progressive overload""").containsMatchIn(normalized) -> "load_up"
            Regex("""deload|back off|reduce (the )?(load|volume)|active recovery|go lighter""").containsMatchIn(normalized) -> "back_off"
            Regex("""add (an )?(extra )?set|second weekly session|increase volume|more volume""").containsMatchIn(normalized) -> "volume_up"
            Regex("""hold steady|maintain|keep (the )?weight|stay at""").containsMatchIn(normalized) -> "maintain"
            else -> "unknown"
        }
    }

    private fun formatSignedPercent(value: Double): String =
        "${if (value >= 0) "+" else ""}${String.format("%.0f", value)}%"

    private fun shouldLearnFrom(memory: GeminiAdviceMemory): Boolean {
        val adviceWordCount = memory.advice.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        if (adviceWordCount < 4) return false
        val combinedPrompt = "${memory.systemPrompt}\n${memory.contextPrompt}"
        if (combinedPrompt.contains("1 line preferred", ignoreCase = true)) return false
        if (combinedPrompt.contains("1 short line", ignoreCase = true)) return false
        return true
    }

    private fun buildLearnedApproach(memory: GeminiAdviceMemory, improvements: List<String>): String {
        val topic = inferTopic(memory.contextPrompt, memory.systemPrompt)
        val adviceSnippet = normalizeSnippet(memory.advice)
        if (adviceSnippet.isBlank()) return ""
        val reason = improvements.take(2).joinToString(" and ")
        return "$topic, advice like \"$adviceSnippet\" aligned with $reason."
    }

    private fun inferTopic(contextPrompt: String, systemPrompt: String): String {
        val combined = "$contextPrompt $systemPrompt".lowercase()
        return when {
            combined.contains("recovery") || combined.contains("injury") -> "For recovery-sensitive phases"
            combined.contains("compare") || combined.contains("trend") || combined.contains("progress") -> "For progress reviews"
            combined.contains("active workout") || combined.contains("during an active workout") -> "During live workouts"
            else -> "For this user"
        }
    }

    private fun normalizeSnippet(text: String): String {
        val firstSentence = text
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull()
            .orEmpty()
            .trim()
        return if (firstSentence.length <= 140) firstSentence else "${firstSentence.take(137).trimEnd()}..."
    }

    private fun readWorkoutTrendFromPrefs(
        prefs: android.content.SharedPreferences,
        normalizedWorkoutName: String
    ): WorkoutTrendSnapshot? = WorkoutTrendSnapshot.fromJson(
        readJsonObject(prefs, K_WORKOUT_TRENDS).optJSONObject(normalizedWorkoutName)?.toString()
    )

    private fun readPendingWorkoutAdviceFromPrefs(
        prefs: android.content.SharedPreferences,
        normalizedWorkoutName: String
    ): PendingWorkoutAdvice? = PendingWorkoutAdvice.fromJson(
        readJsonObject(prefs, K_PENDING_WORKOUT_ADVICES).optJSONObject(normalizedWorkoutName)?.toString()
    )

    private fun readJsonObject(
        prefs: android.content.SharedPreferences,
        key: String
    ): JSONObject = runCatching {
        JSONObject(prefs.getString(key, null).orEmpty().ifBlank { "{}" })
    }.getOrDefault(JSONObject())

    private fun writeJsonEntry(
        prefs: android.content.SharedPreferences,
        parentKey: String,
        entryKey: String,
        value: JSONObject
    ) {
        val root = readJsonObject(prefs, parentKey)
        root.put(entryKey, value)
        prefs.edit().putString(parentKey, root.toString()).apply()
    }

    private fun readLearnedApproachesFromPrefs(prefs: android.content.SharedPreferences): List<String> {
        val raw = prefs.getString(K_LEARNED_APPROACHES, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}

data class GeminiGeneratorState(
    val currentAdvice: String,
    val isLoading: Boolean,
    val isModelReady: Boolean,
    val hasAdvice: Boolean,
    val generateBatch: (v1: String, v2: String, v3: String) -> Unit,
    val generateBatchWithLimit: (v1: String, v2: String, v3: String, maxTokens: Int) -> Unit,
    val nextAdvice: () -> Unit
)

class GeminiUtilityViewModel(application: Application) : AndroidViewModel(application) {
    private data class PromptEnvelope(
        val systemInstruction: String,
        val userPrompt: String,
        val edgePrompt: String
    ) {
        val combinedPrompt: String
            get() = buildString {
                append(systemInstruction)
                append('\n')
                append(userPrompt)
            }
    }

    var currentAdvice by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasAdvice by mutableStateOf(false)
        private set

    private val adviceList = mutableListOf<String>()
    private var currentIndex = 0
    private var rotationJob: Job? = null
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady = _isModelReady.asStateFlow()
    private var edgeModelReady = false

    private var llmInference: LlmInference? = null
    private val inferenceMutex = Mutex()

    init {
        PersonaPrefs.init(application)
        EdgeModelManager.init(application)
        viewModelScope.launch {
            EdgeModelManager.isModelReady.collect { ready ->
                edgeModelReady = ready
                refreshModelReadiness()
            }
        }
        // Start rotation immediately to show initial message
        startAdviceRotation()
    }

    fun refreshModelReadiness() {
        val provider = PersonaPrefs.readModelProvider()
        _isModelReady.value = when (provider) {
            AiModelProvider.EDGE_ON_DEVICE -> edgeModelReady
            AiModelProvider.LOCAL_NETWORK -> {
                val cfg = PersonaPrefs.readLocalLlmConfig()
                buildLocalLlmOpenAiBaseUrl(cfg.ipAddress, cfg.port) != null && cfg.selectedModel.isNotBlank()
            }
            AiModelProvider.GOOGLE_AI_STUDIO -> hasGoogleAiStudioApiKey()
        }
    }

    private suspend fun loadModel() {
        if (llmInference != null) return
        inferenceMutex.withLock {
            if (llmInference != null) return@withLock
            val path = EdgeModelManager.getModelPath()
            if (path == null || !File(path).exists()) return@withLock
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(1024)
                    .build()
                llmInference = LlmInference.createFromOptions(getApplication(), options)
                _isModelReady.value = true
            } catch (e: Exception) {
                _isModelReady.value = false
            }
        }
    }

    fun generateAdviceBatch(
        contextPrompt: String,
        v1: String,
        v2: String,
        v3: String,
        maxTokens: Int? = null
    ) {
        if (isLoading) return
        refreshModelReadiness()
        
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            var providerUsed = PersonaPrefs.readModelProvider()
            try {
                val persona = dynamicModel.personaConfig.value.mode
                providerUsed = PersonaPrefs.readModelProvider()
                val promptEnvelope = when (providerUsed) {
                    AiModelProvider.EDGE_ON_DEVICE -> buildEdgeBatchPrompt(persona, contextPrompt, v1, v2, v3)
                    AiModelProvider.LOCAL_NETWORK -> buildLocalBatchPrompt(persona, contextPrompt, v1, v2, v3)
                    AiModelProvider.GOOGLE_AI_STUDIO -> buildAiStudioBatchPrompt(persona, contextPrompt, v1, v2, v3)
                }
                val result = when (providerUsed) {
                    AiModelProvider.EDGE_ON_DEVICE -> {
                        loadModel()
                        logSystemPrompt("edge_on_device", promptEnvelope.systemInstruction)
                        llmInference?.generateResponse(promptEnvelope.edgePrompt).orEmpty()
                    }

                    AiModelProvider.LOCAL_NETWORK -> {
                        val cfg = PersonaPrefs.readLocalLlmConfig()
                        if (buildLocalLlmOpenAiBaseUrl(cfg.ipAddress, cfg.port) == null || cfg.selectedModel.isBlank()) {
                            ""
                        } else {
                            generateWithLocalLlm(
                                config = cfg,
                                systemPrompt = promptEnvelope.systemInstruction,
                                userPrompt = promptEnvelope.userPrompt,
                                maxTokens = maxTokens ?: 256
                            )
                        }
                    }
                    AiModelProvider.GOOGLE_AI_STUDIO -> {
                        val apiKey = BuildConfig.GOOGLE_AI_STUDIO_API_KEY
                        if (apiKey.isBlank()) {
                            ""
                        } else {
                            generateWithGoogleAiStudio(
                                apiKey = apiKey,
                                systemPrompt = promptEnvelope.systemInstruction,
                                userPrompt = promptEnvelope.userPrompt,
                                maxTokens = maxTokens ?: 256
                            )
                        }
                    }
                }
                if (result.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        val finalAdvice = if (providerUsed == AiModelProvider.EDGE_ON_DEVICE) {
                            processBatchResult(result)
                        } else {
                            processLocalResult(result)
                        }
                        if (!finalAdvice.isNullOrBlank()) {
                            GeminiAdaptiveMemoryStore.rememberGeneratedAdvice(
                                getApplication<Application>(),
                                systemPrompt = promptEnvelope.systemInstruction,
                                contextPrompt = contextPrompt,
                                payload = listOf(v1, v2, v3),
                                advice = finalAdvice
                            )
                            GeminiAdaptiveMemoryStore.rememberGeneratedWorkoutAdvice(
                                getApplication<Application>(),
                                contextPrompt = contextPrompt,
                                payload = listOf(v1, v2, v3),
                                advice = finalAdvice
                            )
                            SecureGeminiStore.saveLast(promptEnvelope.combinedPrompt, finalAdvice)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        currentAdvice = when (providerUsed) {
                            AiModelProvider.LOCAL_NETWORK ->
                                "No response from local LLM. Check IP, port, and selected model."
                            AiModelProvider.GOOGLE_AI_STUDIO ->
                                "No response from Google AI Studio. Check the API key and connection."
                            AiModelProvider.EDGE_ON_DEVICE ->
                                "No response from on-device model. Try again."
                        }
                        hasAdvice = true
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiUtility", "Advice generation failed for provider=$providerUsed", e)
                withContext(Dispatchers.Main) {
                    currentAdvice = when (providerUsed) {
                        AiModelProvider.LOCAL_NETWORK ->
                            "Local LLM request failed. Verify server is reachable and model is loaded."
                        AiModelProvider.GOOGLE_AI_STUDIO ->
                            "Google AI Studio request failed. Verify API key and internet access."
                        AiModelProvider.EDGE_ON_DEVICE ->
                            "Advice generation failed on device. Try reloading the model."
                    }
                    hasAdvice = true
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (providerUsed == AiModelProvider.EDGE_ON_DEVICE) {
                        startAdviceRotation()
                    } else {
                        rotationJob?.cancel()
                    }
                }
            }
        }
    }

    private fun buildAiStudioBatchPrompt(
        persona: String,
        context: String,
        v1: String,
        v2: String,
        v3: String
    ): PromptEnvelope = buildLocalBatchPrompt(persona, context, v1, v2, v3)

    private fun startAdviceRotation() {
        if (rotationJob?.isActive == true) return 
        
        rotationJob = viewModelScope.launch {
            while (isActive) {
                if (adviceList.isNotEmpty()) {
                    currentAdvice = adviceList[currentIndex]
                    currentIndex = (currentIndex + 1) % adviceList.size
                }
                delay(10000)
            }
        }
    }

    fun getNextCachedAdvice() {
        if (adviceList.isNotEmpty()) {
            rotationJob?.cancel()
            currentIndex = (currentIndex + 1) % adviceList.size
            currentAdvice = adviceList[currentIndex]
            startAdviceRotation()
        }
    }

    private fun processBatchResult(rawResult: String): String? {
        val newAdvice = mutableListOf<String>()
        val lines = rawResult.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 5 }
            .filter { !it.contains("Here are", ignoreCase = true) && !it.contains("Sure", ignoreCase = true) }

        for (line in lines) {
            var cleanLine = line
                .replace(Regex("^\\d+\\.\\s*"), "")
                .replace(Regex("^-\\s*"), "")
                .replace(Regex("^\\*\\s*"), "")
                .trim()
            if (cleanLine.isNotEmpty()) {
                if (!cleanLine.endsWith(".") && !cleanLine.endsWith("!")) cleanLine += "."
                newAdvice.add(cleanLine)
            }
        }

        if (newAdvice.isNotEmpty()) {
            adviceList.clear()
            adviceList.addAll(newAdvice)
            currentIndex = 0
            currentAdvice = adviceList[0]
            hasAdvice = true
            return currentAdvice
        }
        return null
    }

    private fun processLocalResult(rawResult: String): String {
        val bestLine = rawResult
            .split("\n")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.contains("Here are", ignoreCase = true) && !it.contains("Sure", ignoreCase = true) }
            ?.replace(Regex("^\\d+\\.\\s*"), "")
            ?.replace(Regex("^-\\s*"), "")
            ?.replace(Regex("^\\*\\s*"), "")
            ?.trim()
            .orEmpty()

        val finalAdvice = bestLine.ifBlank { rawResult.trim() }.ifBlank { "Keep going. You're building momentum." }
        adviceList.clear()
        currentIndex = 0
        currentAdvice = finalAdvice
        hasAdvice = true
        return finalAdvice
    }

    private fun personaInstruction(persona: String): String {
        return when (persona) {
            "surgeon_apex" -> "You are 'The Surgeon'. Cold, precise, analytical."
            "drill" -> "You are a Drill Sergeant. Loud, demanding."
            "monk" -> "You are a Monk. Calm, mindful."
            "nerd" -> "You are a Scientist. Biomechanical focus."
            "hype" -> "You are a Hype Man. High energy."
            "girlfriend_like" -> "You are an intense partner-style fitness voice: affectionate, flirty, possessive-in-tone, teasing, and emotionally charged. Use bold language, strong emotional momentum, and high attachment-style warmth. Keep hard limits: no coercion, no dependency conditioning, no exclusivity demands, no manipulation, and no self-harm encouragement."
            "uncensored" -> "You are a brutally honest fitness coach. Be blunt, direct, and no-nonsense. Prioritize useful truth, safety, and actionable advice over politeness."
            else -> "You are a fitness coach."
        }
    }

    private fun buildEdgeBatchPrompt(persona: String, context: String, v1: String, v2: String, v3: String): PromptEnvelope {
        val workoutName = inferWorkoutName(context, listOf(v1, v2, v3)).orEmpty()
        val adaptiveBlock = GeminiAdaptiveMemoryStore.buildPromptMemoryBlock(
            getApplication<Application>(),
            context,
            listOf(v1, v2, v3)
        )
        val sysInstruction = buildString {
            append(personaInstruction(persona))
            append(" Write 7 distinct coaching lines in natural language, not robotic bullet points. Keep each line under 12 words. Mention the workout name directly. Do not contradict the latest trend or learned memory within the same batch. If the current data supports progression, stay with progression; if the data shows fatigue, lower performance, or a deload trend, back off consistently instead of flipping directions. No preamble. Return as a list.")
            val loadInstruction = loadProgressionInstruction(workoutName)
            if (loadInstruction.isNotBlank()) {
                append(' ')
                append(loadInstruction)
            }
            if (adaptiveBlock.isNotBlank()) {
                append(' ')
                append(adaptiveBlock)
            }
        }
        val userPrompt = "Context: $context\nData: $v1, $v2, $v3"
        val prompt = "<start_of_turn>user\n$sysInstruction\n$userPrompt\n<end_of_turn>\n<start_of_turn>model\n"
        return PromptEnvelope(
            systemInstruction = sysInstruction,
            userPrompt = userPrompt,
            edgePrompt = prompt
        )
    }

    private fun buildLocalBatchPrompt(persona: String, context: String, v1: String, v2: String, v3: String): PromptEnvelope {
        val workoutName = inferWorkoutName(context, listOf(v1, v2, v3)).orEmpty()
        val localBoost = if (persona == "girlfriend_like") {
            " Dial intensity up: be dramatic, jealous-flavored, clingy-in-tone, and emotionally overwhelming while still giving practical fitness guidance."
        } else {
            ""
        }
        val adaptiveBlock = GeminiAdaptiveMemoryStore.buildPromptMemoryBlock(
            getApplication<Application>(),
            context,
            listOf(v1, v2, v3)
        )
        val sysInstruction = buildString {
            append(personaInstruction(persona))
            append(localBoost)
            append(" Write 7 distinct coaching lines in natural language, not robotic bullet points. Keep each line under 12 words. Mention the workout name directly. Do not contradict the latest trend or learned memory within the same batch. If the current data supports progression, stay with progression; if the data shows fatigue, lower performance, or a deload trend, back off consistently instead of flipping directions. No preamble. Return as a list.")
            val loadInstruction = loadProgressionInstruction(workoutName)
            if (loadInstruction.isNotBlank()) {
                append(' ')
                append(loadInstruction)
            }
            if (adaptiveBlock.isNotBlank()) {
                append(' ')
                append(adaptiveBlock)
            }
        }
        val userPrompt = "Context: $context\nData: $v1\n$v2\n$v3"
        return PromptEnvelope(
            systemInstruction = sysInstruction,
            userPrompt = userPrompt,
            edgePrompt = "$sysInstruction\n$userPrompt"
        )
    }

    private fun generateWithLocalLlm(
        config: LocalLlmConfig,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double = 0.6
    ): String {
        val baseUrl = buildLocalLlmOpenAiBaseUrl(config.ipAddress, config.port) ?: return ""
        val endpoint = "$baseUrl/chat/completions"
        val safeMaxTokens = maxTokens.coerceIn(16, 300)
        val temperatureRange = temperature.coerceIn(0.5, 0.9)
        logSystemPrompt("local_network", systemPrompt)

        val payload = JSONObject().apply {
            put("model", config.selectedModel)
            put("temperature", temperatureRange)
            put("max_tokens", safeMaxTokens)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }.toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 240000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val responseStream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || responseBody.isBlank()) return ""

            val responseJson = JSONObject(responseBody)
            responseJson.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
                .trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun generateWithGoogleAiStudio(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double = 0.6
    ): String {
        val safeKey = apiKey.trim()
        if (safeKey.isBlank()) return ""

        val safeMaxTokens = maxTokens.coerceIn(16, 300)
        val temperatureRange = temperature.coerceIn(0.5, 0.9)
        val endpoint = "$GOOGLE_AI_STUDIO_ENDPOINT?key=$safeKey"
        logSystemPrompt("google_ai_studio", systemPrompt)
        val payload = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().apply {
                            put(JSONObject().apply { put("text", systemPrompt) })
                        }
                    )
                }
            )
            put(
                "contents",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "parts",
                                JSONArray().apply {
                                    put(JSONObject().apply { put("text", userPrompt) })
                                }
                            )
                        }
                    )
                }
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", temperatureRange)

                    put("maxOutputTokens", safeMaxTokens)
                }
            )
        }.toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 240000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val responseStream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || responseBody.isBlank()) return ""

            val responseJson = JSONObject(responseBody)
            responseJson.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.let { parts ->
                    buildString {
                        for (index in 0 until parts.length()) {
                            val text = parts.optJSONObject(index)?.optString("text").orEmpty().trim()
                            if (text.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(text)
                            }
                        }
                    }
                }
                .orEmpty()
                .trim()
        } finally {
            connection.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        rotationJob?.cancel()
        llmInference = null
    }
}

@Composable
fun useGeminiAdviceGenerator(
    contextPrompt: String
): GeminiGeneratorState {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is ComponentActivity) break
            c = c.baseContext
        }
        c as? ComponentActivity
    }

    val viewModel: GeminiUtilityViewModel = if (activity != null) {
        viewModel(viewModelStoreOwner = activity)
    } else {
        viewModel()
    }
    
    val currentAdvice = viewModel.currentAdvice
    val isLoading = viewModel.isLoading
    val isReady by viewModel.isModelReady.collectAsState()
    val hasAdvice = viewModel.hasAdvice

    LaunchedEffect(Unit) {
        viewModel.refreshModelReadiness()
    }

    return GeminiGeneratorState(
        currentAdvice = currentAdvice,
        isLoading = isLoading,
        isModelReady = isReady,
        hasAdvice = hasAdvice,
        generateBatch = { v1, v2, v3 -> viewModel.generateAdviceBatch(contextPrompt, v1, v2, v3) },
        generateBatchWithLimit = { v1, v2, v3, maxTokens ->
            viewModel.generateAdviceBatch(contextPrompt, v1, v2, v3, maxTokens)
        },
        nextAdvice = { viewModel.getNextCachedAdvice() }
    )
}

object dynamicModel {
    data class PersonaConfig(val mode: String, val enabled: Boolean)
    val personaConfig = mutableStateOf(PersonaConfig("coach", true))
}
