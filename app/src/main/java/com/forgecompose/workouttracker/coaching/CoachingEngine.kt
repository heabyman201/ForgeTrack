package com.forgecompose.workouttracker.coaching

import android.content.Context
import com.forgecompose.workouttracker.BuildConfig
import com.forgecompose.workouttracker.ai.AiModelProvider
import com.forgecompose.workouttracker.ai.EdgeModelManager
import com.forgecompose.workouttracker.ai.PersonaPrefs
import com.forgecompose.workouttracker.ai.buildLocalLlmOpenAiBaseUrl
import com.forgecompose.workouttracker.muscle.MuscleGroups
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.roundToInt

/**
 * The coaching "brain".
 *
 * [generatePlan] builds a rich prompt from the user's free-text request plus the
 * live muscle-status signals, routes it to whichever LLM provider the user has
 * selected (on-device MediaPipe, a local-network OpenAI-compatible server, or the
 * Google AI Studio / Gemini API), and parses the JSON response into a [WeeklyPlan].
 *
 * If no model is configured/reachable, or the model returns something unparseable,
 * it transparently falls back to [buildHeuristicPlan] — a deterministic planner
 * driven by the same muscle signals — so the feature always produces a usable week.
 */
object CoachingEngine {

    private const val GOOGLE_AI_STUDIO_MODEL = "gemini-3.1-flash-lite-preview"
    private const val GOOGLE_AI_STUDIO_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$GOOGLE_AI_STUDIO_MODEL:generateContent"

    private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    suspend fun generatePlan(
        context: Context,
        provider: AiModelProvider,
        request: PlanRequest,
        onStage: (String) -> Unit
    ): WeeklyPlan = withContext(Dispatchers.IO) {
        onStage("Reading muscle signals")
        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(request)

        onStage(
            when (provider) {
                AiModelProvider.GOOGLE_AI_STUDIO -> "Asking Gemini"
                AiModelProvider.LOCAL_NETWORK -> "Asking local model"
                AiModelProvider.EDGE_ON_DEVICE -> "Running on-device model"
            }
        )

        val raw = runCatching {
            when (provider) {
                AiModelProvider.GOOGLE_AI_STUDIO -> callGoogleAiStudio(systemPrompt, userPrompt)
                AiModelProvider.LOCAL_NETWORK -> callLocalLlm(systemPrompt, userPrompt)
                AiModelProvider.EDGE_ON_DEVICE -> callEdge(context, "$systemPrompt\n\n$userPrompt")
            }
        }.getOrNull().orEmpty()

        if (raw.isNotBlank()) {
            onStage("Building your week")
            val parsed = runCatching { parsePlan(raw, request, provider.displayLabel()) }.getOrNull()
            if (parsed != null && parsed.days.isNotEmpty()) return@withContext parsed
        }

        onStage("Composing plan")
        buildHeuristicPlan(request)
    }

    private fun AiModelProvider.displayLabel(): String = when (this) {
        AiModelProvider.GOOGLE_AI_STUDIO -> "Gemini API"
        AiModelProvider.LOCAL_NETWORK -> "Local Network"
        AiModelProvider.EDGE_ON_DEVICE -> "On-Device"
    }

    /** Whether the selected provider currently has everything it needs to run. */
    fun providerReady(provider: AiModelProvider): Boolean = when (provider) {
        AiModelProvider.GOOGLE_AI_STUDIO -> BuildConfig.GOOGLE_AI_STUDIO_API_KEY.isNotBlank()
        AiModelProvider.LOCAL_NETWORK -> {
            val cfg = PersonaPrefs.readLocalLlmConfig()
            buildLocalLlmOpenAiBaseUrl(cfg.ipAddress, cfg.port) != null && cfg.selectedModel.isNotBlank()
        }
        AiModelProvider.EDGE_ON_DEVICE -> EdgeModelManager.getModelPath()?.let { File(it).exists() } == true
    }

    // ---------------------------------------------------------------------
    // Prompt building
    // ---------------------------------------------------------------------

    private fun buildSystemPrompt(): String = """
        You are ForgeTrack's elite strength & hypertrophy coach. You design a single
        7-day training plan tailored to the athlete's live muscle-recovery data, goals,
        equipment, and available days. Respect recovery: never prescribe heavy work for
        muscles flagged Overreached, DeloadRecommended, or with high injury risk — give
        them light/technique work or rest. Prioritise under-trained muscles and the
        athlete's stated focus. Balance push/pull/legs and place rest days sensibly.

        Respond with STRICT, MINIFIED JSON ONLY — no markdown, no commentary. Schema:
        {
          "title": string,
          "summary": string,
          "focusMuscles": [string],
          "coachNotes": [string],
          "weeklySetTargets": { "MuscleName": int },
          "days": [
            {
              "dayName": "Monday".."Sunday",
              "title": string,
              "isRestDay": boolean,
              "focusMuscles": [string],
              "estimatedMinutes": int,
              "intensityNote": string,
              "exercises": [
                { "name": string, "targetMuscle": string, "sets": int,
                  "reps": string, "restSeconds": int, "rpe": string, "notes": string }
              ]
            }
          ]
        }
        Always return exactly 7 day objects (Monday..Sunday). Use these muscle names:
        Pecs, Delts, Biceps, Triceps, Lats, Traps, Abs, Forearms, Quads, Hamstrings,
        Glutes, Calves, LowerBack, UpperBack.
    """.trimIndent()

    private fun buildUserPrompt(r: PlanRequest): String = buildString {
        appendLine("ATHLETE REQUEST: \"${r.userPrompt.ifBlank { "Build me an effective, balanced week." }}\"")
        if (r.goalTitle.isNotBlank()) appendLine("ACTIVE GOAL: ${r.goalTitle}")
        appendLine()
        appendLine("PROFILE: experience=${r.experience}, style=${r.preferredStyle}, equipment=${r.equipment}")
        appendLine("AVAILABILITY: ${r.daysAvailable} training days/week, ~${r.sessionMinutes} min/session")
        if (r.importantMuscles.isNotEmpty()) appendLine("PRIORITY MUSCLES: ${r.importantMuscles.joinToString()}")
        appendLine()
        appendLine("RECOVERY: efficacy=${"%.2f".format(r.recovery.recoveryEfficacy)}" +
            (r.recovery.sleepHours?.let { ", sleep=${"%.1f".format(it)}h" } ?: "") +
            (r.recovery.restingHeartRate?.let { ", restingHR=${it}bpm" } ?: "") +
            (r.recovery.proteinGrams?.let { ", protein=${it.roundToInt()}g" } ?: "") +
            (if (r.recovery.environmentStress) ", environment stress high" else ""))
        appendLine()
        appendLine("LIVE MUSCLE STATUS (name | readiness | weeklyVolume/target | injuryRisk | lastTrained):")
        r.signals.sortedByDescending { priorityScore(it, r.importantMuscles) }.forEach { s ->
            val pct = (s.targetCompletion * 100f).roundToInt()
            appendLine(
                "- ${s.muscle} | ${s.band} | ${"%.0f".format(s.weeklyProgress)}/${"%.0f".format(s.weeklyTarget)} sets (${pct}%) " +
                    "| risk ${s.injuryRiskPct}% | ${s.lastTrainedAgo ?: "never"}"
            )
        }
        appendLine()
        append("Design the 7-day JSON plan now.")
    }

    // ---------------------------------------------------------------------
    // LLM transports
    // ---------------------------------------------------------------------

    private fun callGoogleAiStudio(systemPrompt: String, userPrompt: String): String {
        val key = BuildConfig.GOOGLE_AI_STUDIO_API_KEY.trim()
        if (key.isBlank()) return ""
        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemPrompt) }) })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", userPrompt) }) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 4096)
                put("responseMimeType", "application/json")
            })
        }.toString()

        val connection = (URL("$GOOGLE_AI_STUDIO_ENDPOINT?key=$key").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 240000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || body.isBlank()) return ""
            JSONObject(body).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?.let { parts ->
                    buildString {
                        for (i in 0 until parts.length()) {
                            val t = parts.optJSONObject(i)?.optString("text").orEmpty()
                            if (t.isNotBlank()) append(t)
                        }
                    }
                }.orEmpty().trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun callLocalLlm(systemPrompt: String, userPrompt: String): String {
        val cfg = PersonaPrefs.readLocalLlmConfig()
        val baseUrl = buildLocalLlmOpenAiBaseUrl(cfg.ipAddress, cfg.port) ?: return ""
        if (cfg.selectedModel.isBlank()) return ""
        val payload = JSONObject().apply {
            put("model", cfg.selectedModel)
            put("temperature", 0.7)
            put("max_tokens", 4096)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
            })
        }.toString()

        val connection = (URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 240000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299 || body.isBlank()) return ""
            JSONObject(body).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun callEdge(context: Context, prompt: String): String {
        val path = EdgeModelManager.getModelPath() ?: return ""
        if (!File(path).exists()) return ""
        return runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(2048)
                .build()
            val inference = LlmInference.createFromOptions(context, options)
            try {
                inference.generateResponse(prompt).orEmpty()
            } finally {
                inference.close()
            }
        }.getOrDefault("")
    }

    // ---------------------------------------------------------------------
    // Response parsing (tolerant — accepts ints or strings for reps/rpe)
    // ---------------------------------------------------------------------

    private fun parsePlan(raw: String, request: PlanRequest, sourceLabel: String): WeeklyPlan? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val root = JSONObject(raw.substring(start, end + 1))

        val days = root.optJSONArray("days") ?: return null
        val planDays = (0 until days.length()).mapNotNull { i ->
            val d = days.optJSONObject(i) ?: return@mapNotNull null
            val exArr = d.optJSONArray("exercises")
            val exercises = if (exArr == null) emptyList() else (0 until exArr.length()).mapNotNull { j ->
                val e = exArr.optJSONObject(j) ?: return@mapNotNull null
                val name = e.optString("name").trim()
                if (name.isBlank()) return@mapNotNull null
                PlanExercise(
                    name = name,
                    targetMuscle = e.optString("targetMuscle").trim(),
                    sets = e.optInt("sets", 3).coerceIn(1, 8),
                    reps = e.optString("reps", "8-12").trim().ifBlank { "8-12" },
                    restSeconds = e.optInt("restSeconds", 90).coerceIn(15, 600),
                    rpe = e.optString("rpe", "7-8").trim().ifBlank { "7-8" },
                    notes = e.optString("notes").trim()
                )
            }
            PlanDay(
                dayName = d.optString("dayName").trim().ifBlank { DAY_NAMES.getOrElse(i) { "Day ${i + 1}" } },
                title = d.optString("title").trim().ifBlank { if (exercises.isEmpty()) "Rest" else "Training" },
                isRestDay = d.optBoolean("isRestDay", exercises.isEmpty()),
                focusMuscles = d.optJSONArray("focusMuscles").toStringList(),
                estimatedMinutes = d.optInt("estimatedMinutes", exercises.size * 12).coerceIn(0, 240),
                intensityNote = d.optString("intensityNote").trim(),
                exercises = exercises
            )
        }
        if (planDays.isEmpty()) return null

        val targets = mutableMapOf<String, Int>()
        root.optJSONObject("weeklySetTargets")?.let { obj ->
            obj.keys().forEach { k -> targets[k] = obj.optInt(k, 0) }
        }

        return WeeklyPlan(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            title = root.optString("title").trim().ifBlank { "Your Custom Week" },
            summary = root.optString("summary").trim().ifBlank { "A plan tuned to your current recovery." },
            source = sourceLabel,
            userPrompt = request.userPrompt,
            focusMuscles = root.optJSONArray("focusMuscles").toStringList()
                .ifEmpty { request.importantMuscles },
            days = planDays,
            coachNotes = root.optJSONArray("coachNotes").toStringList(),
            weeklySetTargets = if (targets.isEmpty()) heuristicTargets(request) else targets
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).trim().ifBlank { null } }
    }

    // ---------------------------------------------------------------------
    // Deterministic, signal-driven fallback planner
    // ---------------------------------------------------------------------

    /**
     * Builds a complete week from the muscle signals alone. Used when no LLM is
     * available, and as a guaranteed safety net. Prioritises under-trained / stated
     * focus muscles, backs off muscles that are overreached or at injury risk, and
     * spreads training and rest days across the week.
     */
    fun buildHeuristicPlan(r: PlanRequest): WeeklyPlan {
        val trainingDays = r.daysAvailable.coerceIn(2, 6)
        val signalByMuscle = r.signals.associateBy { it.muscle }
        val template = splitTemplate(trainingDays)
        val slots = (r.sessionMinutes / 12).coerceIn(3, 7)
        val weekSeed = (System.currentTimeMillis() / (1000L * 60 * 60 * 24 * 7)).toInt()

        val trainingDayIndices = spreadDays(trainingDays)
        val builtDays = ArrayList<PlanDay>(7)
        var templateIdx = 0

        for (dayIdx in 0 until 7) {
            val dayName = DAY_NAMES[dayIdx]
            if (dayIdx in trainingDayIndices && templateIdx < template.size) {
                val (label, muscles) = template[templateIdx]
                templateIdx++
                builtDays.add(buildTrainingDay(dayName, label, muscles, signalByMuscle, slots, r, weekSeed + dayIdx))
            } else {
                builtDays.add(restDay(dayName, signalByMuscle))
            }
        }

        val focus = r.signals
            .sortedByDescending { priorityScore(it, r.importantMuscles) }
            .take(4)
            .map { it.muscle }

        return WeeklyPlan(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            title = heuristicTitle(r),
            summary = heuristicSummary(r, trainingDays),
            source = "Heuristic Coach",
            userPrompt = r.userPrompt,
            focusMuscles = focus,
            days = builtDays,
            coachNotes = heuristicNotes(r),
            weeklySetTargets = heuristicTargets(r)
        )
    }

    private fun buildTrainingDay(
        dayName: String,
        label: String,
        muscles: List<MuscleGroups>,
        signals: Map<String, MuscleSignal>,
        slots: Int,
        r: PlanRequest,
        seed: Int
    ): PlanDay {
        // Order this day's muscles by priority and drop any that should be fully rested.
        val ranked = muscles
            .map { it to (signals[it.name]) }
            .sortedByDescending { (m, s) -> s?.let { priorityScore(it, r.importantMuscles) } ?: priorityBaseline(m, r.importantMuscles) }

        val trainable = ranked.filter { (_, s) ->
            val band = s?.band ?: "OnTrack"
            val risk = s?.injuryRiskPct ?: 0
            !(band == "DeloadRecommended" || risk > 75)
        }
        val pool = if (trainable.isEmpty()) ranked else trainable

        // Distribute exercise slots — top-priority muscle gets the extra slot(s).
        val exercises = ArrayList<PlanExercise>(slots)
        val usedNames = HashSet<String>()
        var slotsLeft = slots
        var round = 0
        while (slotsLeft > 0 && round < 4) {
            for ((m, s) in pool) {
                if (slotsLeft <= 0) break
                // Higher-priority muscles get an extra exercise on the first pass.
                val ex = pickExercise(m, usedNames, seed + round) ?: continue
                usedNames.add(ex)
                exercises.add(prescribe(ex, m, s, r))
                slotsLeft--
            }
            round++
        }

        val estMinutes = exercises.sumOf { it.sets * (it.restSeconds + 35) } / 60
        val avgRisk = pool.mapNotNull { it.second?.injuryRiskPct }.maxOrNull() ?: 0
        val intensity = when {
            r.recovery.recoveryEfficacy < 0.8f || avgRisk > 55 -> "Auto-regulate: stay 1-2 reps shy of failure today."
            r.recovery.recoveryEfficacy > 1.1f -> "Recovery is high — push the top sets."
            else -> "Controlled tempo, leave 1 rep in reserve on the last set."
        }

        return PlanDay(
            dayName = dayName,
            title = label,
            isRestDay = false,
            focusMuscles = pool.map { it.first.name }.distinct(),
            estimatedMinutes = estMinutes.coerceIn(20, 120),
            intensityNote = intensity,
            exercises = exercises
        )
    }

    private fun prescribe(name: String, muscle: MuscleGroups, s: MuscleSignal?, r: PlanRequest): PlanExercise {
        val band = s?.band ?: "OnTrack"
        val risk = s?.injuryRiskPct ?: 0
        val compound = name in COMPOUND_LIFTS

        val (sets, reps, rpe) = when {
            band == "DeloadRecommended" || risk > 75 -> Triple(2, "12-15", "5-6")
            band == "Overreached" || band == "Recovering" || risk > 50 -> Triple(3, "10-12", "6-7")
            band == "NotTrained" || band == "SlightlyTrained" || band == "Building" ->
                if (compound) Triple(4, "6-10", "7-9") else Triple(4, "10-12", "7-9")
            else -> if (compound) Triple(3, "6-8", "7-8") else Triple(3, "10-12", "7-8")
        }
        // Down-shift on poor recovery.
        val finalSets = if (r.recovery.recoveryEfficacy < 0.8f) (sets - 1).coerceAtLeast(2) else sets
        val rest = if (compound) 150 else 75

        val note = buildString {
            s?.lastTrainedAgo?.let { append("Last trained $it. ") }
            when {
                risk > 75 -> append("High injury risk — technique focus only.")
                band == "NotTrained" || band == "SlightlyTrained" -> append("Under-trained — make this a priority.")
                band == "OnTrack" -> append("On track — progressive overload.")
            }
        }.trim()

        return PlanExercise(
            name = name,
            targetMuscle = muscle.name,
            sets = finalSets,
            reps = reps,
            restSeconds = rest,
            rpe = rpe,
            notes = note
        )
    }

    private fun pickExercise(muscle: MuscleGroups, used: Set<String>, seed: Int): String? {
        val catalog = EXERCISE_CATALOG[muscle] ?: return null
        val available = catalog.filter { it !in used }
        if (available.isEmpty()) return null
        return available[(seed % available.size + available.size) % available.size]
    }

    private fun restDay(dayName: String, signals: Map<String, MuscleSignal>): PlanDay {
        val needRecovery = signals.values.any {
            it.band == "Overreached" || it.band == "DeloadRecommended" || it.injuryRiskPct > 60
        }
        val note = if (needRecovery)
            "Full rest or light mobility — several muscles are still recovering."
        else
            "Active recovery: 20-30 min easy cardio, stretching, or a walk."
        return PlanDay(
            dayName = dayName,
            title = "Rest & Recovery",
            isRestDay = true,
            focusMuscles = emptyList(),
            estimatedMinutes = 0,
            intensityNote = note,
            exercises = emptyList()
        )
    }

    // ---------------------------------------------------------------------
    // Scoring & copy
    // ---------------------------------------------------------------------

    private fun priorityScore(s: MuscleSignal, important: List<String>): Float {
        var score = 0f
        score += when (s.band) {
            "NotTrained" -> 5f
            "SlightlyTrained" -> 4f
            "Building" -> 3f
            "OnTrack" -> 1.5f
            "Recovering" -> 0.5f
            "Overreached" -> -2f
            "DeloadRecommended" -> -4f
            else -> 1f
        }
        score += (1f - s.targetCompletion.coerceIn(0f, 1f)) * 3f       // less done -> higher priority
        score -= (s.injuryRiskPct / 100f) * 4f                          // risk lowers priority
        if (s.muscle in important) score += 2.5f
        return score
    }

    private fun priorityBaseline(m: MuscleGroups, important: List<String>): Float =
        if (m.name in important) 2.5f else 1f

    private fun heuristicTargets(r: PlanRequest): Map<String, Int> =
        r.signals.associate { it.muscle to it.weeklyTarget.roundToInt().coerceIn(4, 22) }

    private fun heuristicTitle(r: PlanRequest): String = when {
        r.preferredStyle.contains("strength", true) -> "${r.daysAvailable}-Day Strength Block"
        r.preferredStyle.contains("cardio", true) -> "${r.daysAvailable}-Day Conditioning Week"
        else -> "${r.daysAvailable}-Day Hypertrophy Week"
    }

    private fun heuristicSummary(r: PlanRequest, days: Int): String {
        val focus = r.signals.sortedByDescending { priorityScore(it, r.importantMuscles) }.take(2).joinToString(" & ") { it.muscle }
        return "A $days-day plan tuned to your recovery (efficacy ${"%.0f".format(r.recovery.recoveryEfficacy * 100)}%), " +
            "prioritising $focus while protecting fatigued muscles."
    }

    private fun heuristicNotes(r: PlanRequest): List<String> {
        val notes = mutableListOf<String>()
        if (r.recovery.recoveryEfficacy < 0.8f)
            notes.add("Recovery is below baseline — volume has been trimmed. Prioritise sleep and protein.")
        r.recovery.sleepHours?.let { if (it < 6.5f) notes.add("Sleep is short (${"%.1f".format(it)}h). Aim for 7-9h to support these sessions.") }
        val atRisk = r.signals.filter { it.injuryRiskPct > 60 }.map { it.muscle }
        if (atRisk.isNotEmpty()) notes.add("Elevated injury risk on ${atRisk.joinToString()} — kept light this week.")
        val neglected = r.signals.filter { it.band == "NotTrained" || it.band == "SlightlyTrained" }.map { it.muscle }
        if (neglected.isNotEmpty()) notes.add("Bringing up under-trained ${neglected.take(3).joinToString()}.")
        notes.add("Log every set in ForgeTrack — next week's plan adapts to what you actually do.")
        return notes
    }

    // ---------------------------------------------------------------------
    // Splits, day spreads, exercise catalog
    // ---------------------------------------------------------------------

    private fun spreadDays(trainingDays: Int): Set<Int> = when (trainingDays) {
        2 -> setOf(0, 3)
        3 -> setOf(0, 2, 4)
        4 -> setOf(0, 1, 3, 4)
        5 -> setOf(0, 1, 2, 4, 5)
        6 -> setOf(0, 1, 2, 3, 4, 5)
        else -> setOf(0, 2, 4)
    }

    private fun splitTemplate(trainingDays: Int): List<Pair<String, List<MuscleGroups>>> {
        val push = listOf(MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Triceps)
        val pull = listOf(MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Biceps, MuscleGroups.Traps)
        val legs = listOf(MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves)
        val upper = listOf(MuscleGroups.Pecs, MuscleGroups.Lats, MuscleGroups.Delts, MuscleGroups.Triceps, MuscleGroups.Biceps, MuscleGroups.UpperBack)
        val lower = listOf(MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves, MuscleGroups.Abs)
        val fullBody = listOf(MuscleGroups.Quads, MuscleGroups.Pecs, MuscleGroups.Lats, MuscleGroups.Delts, MuscleGroups.Hamstrings, MuscleGroups.Abs)

        return when (trainingDays) {
            2 -> listOf("Upper Body" to upper, "Lower Body" to lower)
            3 -> listOf("Push — Chest, Shoulders & Triceps" to push, "Pull — Back & Biceps" to pull, "Legs" to legs)
            4 -> listOf(
                "Upper A — Push Emphasis" to (push + MuscleGroups.UpperBack),
                "Lower A — Quad Emphasis" to listOf(MuscleGroups.Quads, MuscleGroups.Calves, MuscleGroups.Abs),
                "Upper B — Pull Emphasis" to pull,
                "Lower B — Posterior Chain" to listOf(MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.LowerBack, MuscleGroups.Calves)
            )
            5 -> listOf(
                "Push — Chest, Shoulders & Triceps" to push,
                "Pull — Back & Biceps" to pull,
                "Legs" to legs,
                "Upper — Hypertrophy" to upper,
                "Lower & Core" to (lower + MuscleGroups.LowerBack)
            )
            6 -> listOf(
                "Push A" to push, "Pull A" to pull, "Legs A" to legs,
                "Push B" to push, "Pull B" to pull, "Legs B" to legs
            )
            else -> List(trainingDays.coerceAtLeast(1)) { "Full Body" to fullBody }
        }
    }

    /** Exercises that warrant longer rest and lower-rep prescriptions. */
    private val COMPOUND_LIFTS = setOf(
        "Barbell Bench Press", "Incline Barbell Press", "Overhead Press", "Pull Up", "Lat Pulldown",
        "Barbell Row", "T-Bar Row", "Back Squat", "Front Squat", "Leg Press", "Romanian Deadlift",
        "Deadlift", "Hip Thrust", "Bulgarian Split Squat", "Close-Grip Bench Press", "Dips",
        "Rack Pull", "Seated Cable Row", "Good Morning"
    )

    private val EXERCISE_CATALOG: Map<MuscleGroups, List<String>> = mapOf(
        MuscleGroups.Pecs to listOf("Barbell Bench Press", "Incline Barbell Press", "Incline DB Press", "Cable Fly", "Machine Chest Press", "Dips"),
        MuscleGroups.Delts to listOf("Overhead Press", "Lateral Raise", "Arnold Press", "Cable Lateral Raise", "Face Pull", "Rear Delt Fly"),
        MuscleGroups.Biceps to listOf("Barbell Curl", "Incline DB Curl", "Hammer Curl", "Cable Curl", "Preacher Curl"),
        MuscleGroups.Triceps to listOf("Close-Grip Bench Press", "Tricep Pushdown", "Overhead Extension", "Skullcrushers", "Dips"),
        MuscleGroups.Lats to listOf("Pull Up", "Lat Pulldown", "Single-Arm DB Row", "Straight-Arm Pulldown", "Chest-Supported Row"),
        MuscleGroups.Traps to listOf("Barbell Shrug", "Rack Pull", "Upright Row", "DB Shrug"),
        MuscleGroups.Abs to listOf("Hanging Leg Raise", "Cable Crunch", "Plank", "Ab Wheel Rollout", "Decline Sit-Up"),
        MuscleGroups.Forearms to listOf("Wrist Curl", "Reverse Curl", "Farmer Carry", "Plate Pinch"),
        MuscleGroups.Quads to listOf("Back Squat", "Front Squat", "Leg Press", "Leg Extension", "Bulgarian Split Squat", "Hack Squat"),
        MuscleGroups.Hamstrings to listOf("Romanian Deadlift", "Lying Leg Curl", "Seated Leg Curl", "Good Morning", "Nordic Curl"),
        MuscleGroups.Glutes to listOf("Hip Thrust", "Romanian Deadlift", "Glute Kickback", "Cable Pull-Through", "Walking Lunge"),
        MuscleGroups.Calves to listOf("Standing Calf Raise", "Seated Calf Raise", "Leg Press Calf Raise"),
        MuscleGroups.LowerBack to listOf("Back Extension", "Deadlift", "Good Morning", "Bird Dog"),
        MuscleGroups.UpperBack to listOf("Barbell Row", "Seated Cable Row", "T-Bar Row", "Rear Delt Fly", "Reverse Pec Deck")
    )
}
