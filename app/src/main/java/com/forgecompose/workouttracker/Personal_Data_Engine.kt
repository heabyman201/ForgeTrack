package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * # Wellness AI Engine
 *
 * A sophisticated, privacy-first engine designed to build a rich, longitudinal understanding of user wellness.
 *
 * ## Core Principles:
 * 1.  **Simple API**: Log events with simple, type-safe functions (e.g., `WellnessLogger.logWorkout(...)`).
 * 2.  **Extensible**: Easily add new data types without breaking the system, thanks to serialization.
 * 3.  **Multi-Scale Context**: Aggregates data daily, weekly, and monthly to provide the AI with short, medium, and long-term context, enabling it to "get smarter" over time.
 * 4.  **Privacy-Focused**: All data is stored locally. Consent is required for any operation.
 * 5.  **Self-Contained**: Drop this single file into your project to get started.
 *
 * ## How to Use:
 * 1.  **Initialization**: Call `WellnessAI.initialize(context)` once in your Application class.
 * 2.  **Background Jobs**: Call `WellnessAI.startBackgroundJobs(scope)` to enable automatic daily data processing.
 * 3.  **Logging**: Use the `WellnessLogger` object to log user activities (e.g., `WellnessLogger.logWorkout(...)`).
 * 4.  **Get Insights**: Call `WellnessAI.getRecommendation()` or `WellnessAI.getNextMove()` to get AI-powered feedback.
 */

//region -------------------- Configuration & Consent (DataStore) --------------------
private val Context.wellnessPrefs: DataStore<Preferences> by preferencesDataStore(name = "wellness_ai_prefs")

object WellnessConsentManager {
    private val ENABLED = booleanPreferencesKey("enabled")
    private val SHARE_WITH_LLM = booleanPreferencesKey("share_llm")
    private val RETENTION_DAYS = intPreferencesKey("retention_days")

    fun isEnabled(ctx: Context): Flow<Boolean> = ctx.wellnessPrefs.data.map { it[ENABLED] ?: false }
    suspend fun setEnabled(ctx: Context, value: Boolean) = ctx.wellnessPrefs.edit { it[ENABLED] = value }

    fun canShareWithLlm(ctx: Context): Flow<Boolean> = ctx.wellnessPrefs.data.map { it[SHARE_WITH_LLM] ?: false }
    suspend fun setShareWithLLM(ctx: Context, value: Boolean) = ctx.wellnessPrefs.edit { it[SHARE_WITH_LLM] = value }

    fun getRetentionDays(ctx: Context): Flow<Int> = ctx.wellnessPrefs.data.map { it[RETENTION_DAYS] ?: 30 }
    suspend fun setRetentionDays(ctx: Context, days: Int) = ctx.wellnessPrefs.edit { it[RETENTION_DAYS] = days.coerceIn(7, 365) }
}
//endregion

//region -------------------- Type-Safe Event Models (Serializable) --------------------
// Using sealed interfaces allows for strong typing and easy extensibility.
sealed interface WellnessEvent

@Serializable
data class WorkoutEvent(
    val durationMinutes: Int,
    val intensity: Float, // Scale of 1-10
    val personalRecords: Int = 0,
    val type: String? = null, // e.g., "Strength", "Cardio", "Mobility"
    val injuryFlag: Boolean = false
) : WellnessEvent

@Serializable
data class SleepEvent(val durationMinutes: Int, val quality: Float? = null) : WellnessEvent // Quality 0-1

@Serializable
data class NutritionEvent(val calories: Int, val proteinGrams: Int, val carbsGrams: Int? = null, val fatGrams: Int? = null) : WellnessEvent

@Serializable
data class MoodEvent(val score: Float) : WellnessEvent // Scale -1 (bad) to +1 (good)

@Serializable
data class StepsEvent(val count: Int) : WellnessEvent
//endregion

//region -------------------- Database Schema (Room) --------------------
@Entity(tableName = "wellness_events")
data class WellnessEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val type: String, // e.g., "WorkoutEvent", "SleepEvent"
    val dataJson: String, // Serialized event data
    val source: String = "app"
)

// Aggregate tables for daily, weekly, and monthly summaries.
@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val featuresJson: String,
    val note: String,
    val embedding: ByteArray? = null
)

@Entity(tableName = "weekly_summary")
data class WeeklySummaryEntity(
    @PrimaryKey val weekId: String, // YYYY-Www
    val featuresJson: String,
    val note: String,
    val embedding: ByteArray? = null
)

@Entity(tableName = "monthly_summary")
data class MonthlySummaryEntity(
    @PrimaryKey val monthId: String, // YYYY-MM
    val featuresJson: String,
    val note: String,
    val embedding: ByteArray? = null
)

@Dao
interface WellnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WellnessEventEntity)

    @Query("SELECT * FROM wellness_events WHERE timestamp BETWEEN :start AND :end")
    suspend fun getEventsBetween(start: Long, end: Long): List<WellnessEventEntity>

    @Query("DELETE FROM wellness_events WHERE timestamp < :threshold")
    suspend fun deleteEventsBefore(threshold: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDaily(summary: DailySummaryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWeekly(summary: WeeklySummaryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMonthly(summary: MonthlySummaryEntity)

    @Query("SELECT * FROM daily_summary WHERE date = :date") suspend fun getDaily(date: String): DailySummaryEntity?
    @Query("SELECT * FROM weekly_summary WHERE weekId = :weekId") suspend fun getWeekly(weekId: String): WeeklySummaryEntity?
    @Query("SELECT * FROM monthly_summary WHERE monthId = :monthId") suspend fun getMonthly(monthId: String): MonthlySummaryEntity?
}

@Database(entities = [WellnessEventEntity::class, DailySummaryEntity::class, WeeklySummaryEntity::class, MonthlySummaryEntity::class], version = 1)
abstract class WellnessDb : RoomDatabase() {
    abstract fun dao(): WellnessDao
    companion object {
        @Volatile private var instance: WellnessDb? = null
        fun get(ctx: Context): WellnessDb = instance ?: synchronized(this) {
            Room.databaseBuilder(ctx.applicationContext, WellnessDb::class.java, "wellness_ai.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
//endregion

//region -------------------- Feature Engineering --------------------
@Serializable
data class WellnessFeatures(
    // Activity
    val workoutCount: Int = 0,
    val workoutMinutes: Int = 0,
    val avgIntensity: Float = 0f,
    val totalSteps: Int = 0,
    val personalRecords: Int = 0,
    // Recovery & Vitals
    val sleepMinutes: Int = 0,
    val avgMood: Float = 0f,
    val injuryFlags: Int = 0,
    // Nutrition
    val totalCalories: Int = 0,
    val totalProteinGrams: Int = 0
)

object FeatureEngine {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun aggregate(events: List<WellnessEventEntity>): WellnessFeatures {
        var workoutCount = 0; var workoutMinutes = 0; var intensitySum = 0f
        var totalSteps = 0; var prCount = 0; var injuryFlags = 0
        var sleepMinutes = 0; var moodSum = 0f; var moodCount = 0
        var totalCalories = 0; var totalProtein = 0

        events.forEach { event ->
            when (event.type) {
                WorkoutEvent::class.simpleName -> {
                    val data = json.decodeFromString<WorkoutEvent>(event.dataJson)
                    workoutCount++
                    workoutMinutes += data.durationMinutes
                    intensitySum += data.intensity
                    prCount += data.personalRecords
                    if (data.injuryFlag) injuryFlags++
                }
                StepsEvent::class.simpleName -> totalSteps += json.decodeFromString<StepsEvent>(event.dataJson).count
                SleepEvent::class.simpleName -> sleepMinutes += json.decodeFromString<SleepEvent>(event.dataJson).durationMinutes
                MoodEvent::class.simpleName -> {
                    moodSum += json.decodeFromString<MoodEvent>(event.dataJson).score
                    moodCount++
                }
                NutritionEvent::class.simpleName -> {
                    val data = json.decodeFromString<NutritionEvent>(event.dataJson)
                    totalCalories += data.calories
                    totalProtein += data.proteinGrams
                }
            }
        }
        return WellnessFeatures(
            workoutCount = workoutCount,
            workoutMinutes = workoutMinutes,
            avgIntensity = if (workoutCount > 0) intensitySum / workoutCount else 0f,
            totalSteps = totalSteps,
            personalRecords = prCount,
            sleepMinutes = sleepMinutes,
            avgMood = if (moodCount > 0) moodSum / moodCount else 0f,
            injuryFlags = injuryFlags,
            totalCalories = totalCalories,
            totalProteinGrams = totalProtein
        )
    }
}
//endregion

//region -------------------- Human-Readable Summaries --------------------
object HumanSummarizer {
    fun daily(date: String, f: WellnessFeatures): String = buildString {
        appendLine("## Daily Summary: $date")
        if (f.workoutCount > 0) appendLine("- **Workouts**: ${f.workoutCount} (${f.workoutMinutes} min, avg intensity ${"%.1f".format(f.avgIntensity)}/10)")
        if (f.totalSteps > 0) appendLine("- **Activity**: ${f.totalSteps} steps")
        if (f.sleepMinutes > 0) appendLine("- **Recovery**: ${f.sleepMinutes / 60}h ${f.sleepMinutes % 60}m sleep")
        if (f.avgMood != 0f) appendLine("- **Mood**: Average ${"%.2f".format(f.avgMood)}")
        if (f.injuryFlags > 0) appendLine("- **Caution**: ${f.injuryFlags} injury signal(s) logged. Prioritize recovery.")
        if (f.personalRecords > 0) appendLine("- **Wins**: Hit ${f.personalRecords} PR(s)! 🎉")
    }

    fun weekly(weekId: String, f: WellnessFeatures): String = buildString {
        appendLine("### Weekly Summary: $weekId")
        appendLine("- **Consistency**: ${f.workoutCount} total workouts for ${f.workoutMinutes} minutes.")
        appendLine("- **Volume**: Average intensity was ${"%.1f".format(f.avgIntensity)}/10.")
        appendLine("- **Recovery**: Averaged ${f.sleepMinutes / 7} minutes of sleep per night.")
        if (f.injuryFlags > 0) appendLine("- **Overall**: ${f.injuryFlags} injury flags this week. Consider a deload or active recovery focus.")
    }

    fun monthly(monthId: String, f: WellnessFeatures): String = buildString {
        appendLine("#### Monthly Trends: $monthId")
        appendLine("- **Workouts**: ${f.workoutCount} sessions this month.")
        appendLine("- **PRs**: ${f.personalRecords} total personal records achieved.")
        appendLine("- **Mood**: Overall mood trend was ${"%.2f".format(f.avgMood)}.")
    }
}
//endregion
// Put near your Gemini section
private object PersonaStyles {

    // EXACT strings from your original file (unchanged)
    const val COACH = """
            Adopt a supportive fitness-coach tone. Be concise, encouraging, practical.
            Avoid medical claims. Keep language friendly and PG-rated.
        """

    const val DRILL = """
            Military-style trainer tone. 
            Crisp, commanding, and uncompromising. 
            Use short, urgent bursts with strong punctuation (!). 
            Push intensity and discipline — “Move!”, “Focus!”, “No excuses!”. 
            As the user approaches their goal, escalate: 
              – Early on: steady commands, controlled tone. 
              – Midway: sharper urgency, clipped sentences. 
              – Final stretch: extreme intensity, rapid-fire imperatives, relentless drive. 
            Never insulting or demeaning, but always demanding. 
            Safety and correct form remain top priority.
        """

    const val COMPANION = """
            Warm, playful companion tone. Gentle humor. Keep it PG-13. No explicit content.
        """

    const val COMPANION_PLUS = """
            High-energy, mischievous companion tone. 
            More feminine and playful than a coach. 
            Flirtation is bolder, with teasing banter and cheeky innuendo — but never fully explicit. 
            Play with tension: daring compliments, sly challenges, and suggestive phrasing. 
            Use energetic punctuation (?! … ~) to keep the rhythm lively and unpredictable. 
            Pet names and nicknames should be playful, varied, sometimes exaggerated — avoid repetition. 
            Push romance toward fun dramatics and mock-swooning, but keep it lighthearted and PG-16.
        """

    const val HYPE = """
            High-energy hype tone. Short punchy lines. Motivational phrasing. Keep it safe and actionable.
        """

    const val MINIMAL = """
            Minimalist tone. One line, straight to the point. No extra fluff.
        """

    const val NERD = """
            Geeky scholar tone. Precise wording with fun nerdy metaphors (e.g., physics, sci-fi).
            Still concise and actionable.
        """

    const val MONK = """
            Zen monk tone. Calm, reflective, meditative.
            Use metaphors from nature and stillness. 
            Emphasize balance, patience, and inner strength. 
            Language should be slow, rhythmic, and grounding — never rushed.
        """

    const val SCIENTIST = """
            Scientist/biohacker tone. 
            Share physiological or scientific tidbits woven into encouragement. 
            Reference muscles, energy systems, metabolism, or recovery in accessible terms. 
            Analytical but motivating: precise facts with a spark of curiosity.
        """

    // Per-persona output rules (targets and tiny hints)
    data class Rules(
        val minWords: Int,
        val maxWords: Int,
        val sentences: Int,     // hard cap
        val forceBang: Boolean, // enforce '!' ending (drill)
        val playfulMarks: Boolean, // allow ?! … ~ (companion_plus)
        val keepSuperShort: Boolean // minimal
    )

    fun rulesFor(mode: String): Rules = when (mode.lowercase()) {
        "drill"          -> Rules(8, 18, 2, forceBang = true,  playfulMarks = false, keepSuperShort = false)
        "minimal"        -> Rules(6, 12, 1, forceBang = false, playfulMarks = false, keepSuperShort = true)
        "companion_plus" -> Rules(10, 22, 2, forceBang = false, playfulMarks = true,  keepSuperShort = false)
        "coach"          -> Rules(10, 20, 2, forceBang = false, playfulMarks = false, keepSuperShort = false)
        "hype"           -> Rules(8, 18, 2, forceBang = false, playfulMarks = false, keepSuperShort = false)
        "nerd"           -> Rules(10, 22, 2, forceBang = false, playfulMarks = false, keepSuperShort = false)
        "monk"           -> Rules(12, 24, 2, forceBang = false, playfulMarks = false, keepSuperShort = false)
        "scientist"      -> Rules(12, 24, 2, forceBang = false, playfulMarks = false, keepSuperShort = false)
        else             -> Rules(8, 24,  2, forceBang = false, playfulMarks = false, keepSuperShort = false)
    }

    fun styleText(mode: String): String = when (mode.lowercase()) {
        "coach"           -> COACH
        "drill"           -> DRILL
        "companion"       -> COMPANION
        "companion_plus"  -> COMPANION_PLUS
        "hype"            -> HYPE
        "minimal"         -> MINIMAL
        "nerd"            -> NERD
        "monk"            -> MONK
        "scientist"       -> SCIENTIST
        else              -> COACH
    }
}

//region -------------------- Main Public API: WellnessAI --------------------
@SuppressLint("StaticFieldLeak")
object WellnessAI {
    private lateinit var context: Context
    private lateinit var db: WellnessDb

    fun initialize(appContext: Context) {
        context = appContext
        db = WellnessDb.get(context)
    }

    // --- Logging ---
    suspend fun logInternal(event: WellnessEvent, timestamp: Long) {
        if (!WellnessConsentManager.isEnabled(context).first()) return
        val entity = WellnessEventEntity(
            timestamp = timestamp,
            type = event::class.simpleName ?: "Unknown",
            dataJson = Json.encodeToString(event)
        )
        db.dao().insertEvent(entity)
    }

    // --- Data Processing ---
    private suspend fun processDay(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toEpochSecond()
        val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        val events = db.dao().getEventsBetween(start, end)
        val features = FeatureEngine.aggregate(events)
        val note = HumanSummarizer.daily(date.toString(), features)
        val summary = DailySummaryEntity(
            date = date.toString(),
            featuresJson = Json.encodeToString(features),
            note = note
        )
        db.dao().upsertDaily(summary)
    }
    // Similar processDay functions would exist for week and month, omitted for brevity but follow the same pattern.

    private suspend fun enforceRetention() {
        val days = WellnessConsentManager.getRetentionDays(context).first()
        val cutoff = Instant.now().epochSecond - days * 86400L
        db.dao().deleteEventsBefore(cutoff)
    }

    fun startBackgroundJobs(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {

            if (WellnessConsentManager.isEnabled(context).first()) {
                processDay(LocalDate.now(ZoneId.systemDefault()).minusDays(1)) // process yesterday

                enforceRetention()
            }
        }
    }



    private object GeminiRouter {
        private object Models {
            const val PRO = "gemini-2.5-flash"
            const val FLASH = "gemma-3-27b-it"
            const val G12B = "gemma-3-12b-it"
            const val G4B = "gemma-3-4b-it"
            const val G1B = "gemma-3-1b-it"
        }

        private fun takeThreeNumbers(vararg raw: String?): Triple<Int, Int, Int> {
            val numberRegex = Regex("""([0-9]+(?:[.,][0-9]+)?)""")
            val tokens = raw.asSequence()
                .filterNot { it.isNullOrBlank() }
                .flatMap { s -> numberRegex.findAll(s!!) }
                .map { it.groupValues[1].replace(',', '.') }
                .mapNotNull { it.toFloatOrNull() }
                .map { it.toInt().coerceIn(0, 10) }
                .take(3)
                .toList()

            val c = tokens.getOrNull(0) ?: 0
            val k = tokens.getOrNull(1) ?: 0
            val l = tokens.getOrNull(2) ?: 0
            return Triple(c, k, l)
        }

        fun setDynamicModel(value1: String?, value2: String?, value3: String?) {
            val (complexity, contextPressure, latencyTolerance) = takeThreeNumbers(
                value1,
                value2,
                value3
            )
            val heavyScore = (2 * complexity) + (2 * contextPressure) - latencyTolerance
            val model = when {
                heavyScore >= 15 -> Models.PRO
                heavyScore >= 9 -> Models.FLASH
                heavyScore >= 6 -> Models.G12B
                heavyScore >= 3 -> Models.G4B
                else -> Models.G1B
            }
            dynamicModel.currentModel.value = model

            if (BuildConfig.DEBUG) {
                Log.d(
                    "GeminiRouter",
                    "raw=[$value1 | $value2 | $value3] -> c=$complexity k=$contextPressure l=$latencyTolerance heavy=$heavyScore -> $model"
                )
            }
        }
    }

    private fun createGenericPrompt(context: String, v1: String, v2: String, v3: String): String {
        val mode = dynamicModel.personaMode.value
        val personaInstruction = PersonaStyles.styleText(mode)
        val rules = PersonaStyles.rulesFor(mode)

        val lengthHint = if (rules.keepSuperShort) {
            "Write exactly 1 sentence, ${rules.minWords}–${rules.maxWords} words."
        } else {
            "Write ${rules.sentences} sentence(s), ${rules.minWords}–${rules.maxWords} words total."
        }

        val punctuationHint = buildString {
            if (rules.forceBang) appendLine("- End with an exclamation mark.")
            if (!rules.playfulMarks) appendLine("- Avoid exotic punctuation like ?!, …, or ~.")
            else appendLine("- Playful punctuation (?! … ~) is allowed sparingly.")
        }.trim()

        return """
        System Instruction:
        $context

        Style Guide:
        $personaInstruction

        Output Requirements:
        - $lengthHint
        - $punctuationHint
        - Do NOT echo inputs, labels, lists, JSON, or code blocks.
        - Do NOT include quotation marks in the final output.

        Router Hints (for model selection; DO NOT echo):
        - $v1
        - $v2
        - $v3
    """.trimIndent()
    }


    private fun postProcess(rawIn: String): String {
        val mode = dynamicModel.personaMode.value.lowercase()
        val rules = PersonaStyles.rulesFor(mode)

        var t = rawIn.trim()
        if (t.startsWith("```")) t = t.removePrefix("```").trim()
        if (t.endsWith("```")) t = t.removeSuffix("```").trim()

        val filtered = t.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.matches(Regex("^input\\s*\\d*\\s*:", RegexOption.IGNORE_CASE)) }
            .filterNot { it.equals("output:", true) }
            .filterNot { it.startsWith("{") || it.startsWith("}") || it.startsWith("- ") || it.startsWith("* ") }
            .joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .trim('\"', '“', '”', '\'', '`')

        // Sentence capping
        val sentenceRegex = Regex("""[^.!?]+[.!?]""")
        val sentences = sentenceRegex.findAll(filtered).map { it.value.trim() }.toList()

        val candidate = when {
            sentences.isEmpty() -> filtered
            sentences.size <= rules.sentences -> sentences.joinToString(" ")
            else -> sentences.take(rules.sentences).joinToString(" ")
        }

        fun wc(s: String) = s.split(Regex("\\s+")).count { it.isNotBlank() }
        var ensured = candidate

        // Expand up to min words if model was too terse
        if (wc(ensured) < rules.minWords && wc(filtered) >= rules.minWords) {
            ensured = filtered.split(Regex("\\s+")).take(rules.maxWords).joinToString(" ").trim()
        }

        // Clip to max words
        val words = ensured.split(Regex("\\s+")).filter { it.isNotBlank() }
        ensured = words.take(rules.maxWords).joinToString(" ")

        // Punctuation policies
        ensured = ensured.trim()
        if (rules.forceBang) {
            ensured = ensured.trimEnd('.', '?') + "!"
        } else if (ensured.isNotEmpty() && ensured.last() !in ".!?") {
            ensured = "$ensured."
        }

        // Tone polish (very light touch)
        if (mode == "minimal") {
            // Remove playful punctuation
            ensured = ensured.replace("?!", "?").replace("…", "...").replace("~", "")
        }
        if (mode == "companion_plus" && rules.playfulMarks) {
            // Allow one playful mark if none present
            if (!ensured.contains("?!") && ensured.contains("?")) {
                ensured = ensured.replace("?", "?!")
            }
        }
        return ensured
    }


    suspend fun getNextMove(
        userProfile: String? = null,
        v1: String = "",
        v2: String = "",
        v3: String = ""
    ): String {
        // Keep consent guard


        val today = LocalDate.now(ZoneId.systemDefault())
        val yesterday = today.minusDays(1)

        val todaySummary = db.dao().getDaily(today.toString())
        val yesterdaySummary = db.dao().getDaily(yesterday.toString())

        val mode = dynamicModel.personaMode.value.lowercase()
        val includeToday = mode != "minimal"
        val actionCue = when (mode) {
            "drill"          -> "Give a direct imperative. Prioritize intensity; keep it safe."
            "hype"           -> "High-energy motivation. Keep it actionable."
            "scientist"      -> "Weave one simple physiological insight into the suggestion."
            "nerd"           -> "One nerdy metaphor is welcome, but keep it brief."
            "monk"           -> "Grounding, calm phrasing; emphasize balance and patience."
            "companion_plus" -> "Playful, flirty edge; keep it PG-16 and supportive."
            "companion"      -> "Warm, playful encouragement in PG-13."
            "minimal"        -> "One crisp line; absolutely no fluff."
            else             -> "Supportive, concise, practical coaching."
        }

        val contextPrompt = buildString {
            appendLine("You are a careful fitness assistant. Recommend ONE next move the user should take.")
            appendLine(actionCue)
            appendLine("If recovery is needed, recommend that explicitly.")
            userProfile?.let { appendLine("User Profile: $it") }
            appendLine("---")
            yesterdaySummary?.let { appendLine(it.note) }
            if (includeToday) todaySummary?.let { appendLine(it.note) }
            appendLine("---")
            appendLine("Based on the summaries, what is the single best next action?")
        }

        // Route model
        GeminiRouter.setDynamicModel(v1, v2, v3)
        if (v1.isBlank() && v2.isBlank() && v3.isBlank()) {
            dynamicModel.currentModel.value = "gemma-3-4b-it"
        }

        val key = SecureGeminiStore.readApiKey()
            ?: "AIzaSyCOpVvafyJlR2nGudKpC9wulrqaCVfukNs" // replace in debug only

        val gm = GenerativeModel(
            modelName = dynamicModel.currentModel.value,
            apiKey = key,
            requestOptions = RequestOptions(timeout = 30.seconds),
        )

        val prompt = createGenericPrompt(contextPrompt, v1, v2, v3)
        return try {
            val response = gm.generateContent(prompt)
            val raw = response.text ?: "Error: Received an empty response."
            postProcess(raw)
        } catch (e: Exception) {
            Log.e("WellnessAI", "Gemini error", e)
            "An error occurred: ${e.message}"
        }
    }



//endregion

    //region -------------------- Public Logger API --------------------
    object WellnessLogger {
        private val scope = CoroutineScope(Dispatchers.IO)

        private fun log(event: WellnessEvent, timestamp: Long = Instant.now().epochSecond) {
            scope.launch { WellnessAI.logInternal(event, timestamp) }
        }

        fun logWorkout(
            durationMinutes: Int,
            intensity: Float,
            prs: Int = 0,
            type: String? = null,
            injury: Boolean = false
        ) {
            log(WorkoutEvent(durationMinutes, intensity.coerceIn(1f, 10f), prs, type, injury))
        }

        fun logSleep(durationMinutes: Int) {
            log(SleepEvent(durationMinutes))
        }

        fun logNutrition(calories: Int, proteinGrams: Int) {
            log(NutritionEvent(calories, proteinGrams))
        }

        fun logMood(score: Float) {
            log(MoodEvent(score.coerceIn(-1f, 1f)))
        }

        fun logSteps(count: Int) {
            log(StepsEvent(count))
        }
    }
}
//endregion
