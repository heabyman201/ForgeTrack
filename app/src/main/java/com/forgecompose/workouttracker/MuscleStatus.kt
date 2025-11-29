package com.forgecompose.workouttracker

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord as HcHeartRateRecord
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

// ==========================================
// EXTENSIONS & UTILS
// ==========================================

fun Modifier.glow(
    color: Color,
    radius: Dp = 12.dp,
    alpha: Float = 0.6f,
    offsetY: Dp = 0.dp
): Modifier {
    return this.shadow(
        elevation = radius,
        shape = RoundedCornerShape(32.dp),
        spotColor = color.copy(alpha = alpha),
        ambientColor = color.copy(alpha = alpha)
    )
}


// ==========================================
// SURVEY PARSING logic
// ==========================================

data class SurveyInsights(
    // Core Training Variables
    val goal: String = "General Health",
    val experience: String = "Just starting",
    val daysAvailable: String = "3 days",
    val equipmentAccess: String = "Full Commercial Gym",
    val sessionLength: String = "45-60 mins",

    // Physical & Health Stats
    val focusMuscles: List<MuscleGroups> = emptyList(),
    val injuries: List<String> = emptyList(),
    val recoverySelfAssessment: String = "Average",
    val stimulantUsage: String = "Coffee / Natural only",
    val sleepQuality: String = "7-8 hours",
    val dietStyle: String = "No restrictions",
    val waterIntake: String = "1-2 Liters",
    val stressLevel: String = "Moderate",

    // Preferences & Lifestyle
    val cardioPreference: String = "No Cardio",
    val preferredTrainingTime: String = "Evening",
    val motivationSource: String = "Visual changes",
    val trackingStyle: String = "Mental Notes",
    val gripStrengthNeedsHelp: Boolean = false,
    val mobilityFrequency: String = "Rarely",
    val usesCreatine: Boolean = false
)

// Parse the raw "Tape" string from SurveyTape into structured data
fun parseSurveyTape(tape: String): SurveyInsights {
    if (tape.isBlank()) return SurveyInsights()

    val lastAnswers = mutableMapOf<String, String>()

    // Regex to find "ID: [id] | A: [answer]"
    // Tape format: [TIMESTAMP] ID: questionId | A: Answer text
    tape.lineSequence().forEach { line ->
        val idStart = line.indexOf("ID: ")
        val answerStart = line.indexOf("| A: ")

        if (idStart != -1 && answerStart != -1) {
            val id = line.substring(idStart + 4, answerStart).trim()
            val answer = line.substring(answerStart + 5).trim()
            lastAnswers[id] = answer
        }
    }




    val focusRaw = lastAnswers["focus_area"] ?: ""
    val focusList = when {
        focusRaw.contains("Chest", ignoreCase = true) -> listOf(MuscleGroups.Pecs, MuscleGroups.Triceps)
        focusRaw.contains("Back", ignoreCase = true) -> listOf(MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Biceps)
        focusRaw.contains("Legs", ignoreCase = true) -> listOf(MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes)
        focusRaw.contains("Arms", ignoreCase = true) -> listOf(MuscleGroups.Biceps, MuscleGroups.Triceps, MuscleGroups.Forearms)
        else -> emptyList()
    }


    val gripRaw = lastAnswers["grip_strength"] ?: ""
    val needsStraps = gripRaw.contains("Yes") || gripRaw.contains("weak point")


    val suppsRaw = lastAnswers["supplements"] ?: ""
    val takesCreatine = suppsRaw.contains("Yes")

    return SurveyInsights(

        goal = lastAnswers["goal"] ?: "General Health",
        experience = lastAnswers["exp"] ?: "Just starting",
        daysAvailable = lastAnswers["days"] ?: "3 days",
        equipmentAccess = lastAnswers["equip"] ?: "Full Commercial Gym",
        sessionLength = lastAnswers["session_len"] ?: "45-60 mins",

        // Physical Mappings
        focusMuscles = focusList,
        injuries = (lastAnswers["limitations"] ?: "").split(",").filter { it.isNotBlank() && !it.contains("No injuries") },
        recoverySelfAssessment = lastAnswers["recovery_rate"] ?: "Average",
        stimulantUsage = lastAnswers["stimulants"] ?: "Coffee / Natural only",
        sleepQuality = lastAnswers["sleep_quality"] ?: "7-8 hours",
        dietStyle = lastAnswers["diet_style"] ?: "No restrictions",
        waterIntake = lastAnswers["water_intake"] ?: "1-2 Liters",
        stressLevel = lastAnswers["stress_levels"] ?: "Moderate",

        // Preference Mappings
        cardioPreference = lastAnswers["cardio_pref"] ?: "No Cardio",
        preferredTrainingTime = lastAnswers["training_time"] ?: "Evening",
        motivationSource = lastAnswers["motivation"] ?: "Visual changes",
        trackingStyle = lastAnswers["tracking_style"] ?: "Mental Notes",
        gripStrengthNeedsHelp = needsStraps,
        mobilityFrequency = lastAnswers["mobility"] ?: "Rarely",
        usesCreatine = takesCreatine
    )
}

// ==========================================
// USER PROFILE & CONFIGURATION
// ==========================================

enum class MuscleGroups(val recoverySpeed: Float, val cnsImpact: Float) {
    Pecs(1.0f, 0.7f), Delts(1.2f, 0.4f), Biceps(1.3f, 0.3f), Triceps(1.2f, 0.4f),
    Lats(1.0f, 0.7f), Traps(1.3f, 0.5f), Abs(1.5f, 0.3f), Forearms(1.6f, 0.2f),
    Quads(0.9f, 0.9f), Hamstrings(0.8f, 0.8f), Glutes(0.9f, 0.8f), Calves(1.5f, 0.3f),
    LowerBack(0.7f, 1.0f), UpperBack(1.1f, 0.6f)
}

enum class LoadBand { NotTrained, SlightlyTrained, Building, OnTrack, Recovering, Overreached, DeloadRecommended }

data class UserProfile(
    val name: String,
    val weightKg: Int,
    val heightCm: Int,
    val age: Int,
    val experience: String,
    val preferredStyle: String,
    val importantMuscles: List<MuscleGroups>
)

fun buildUserProfile(prefs: UserPreferencesManager): UserProfile {
    fun toIntSafe(s: String?, f: Int) = s?.toIntOrNull() ?: f
    fun toListOfMuscles(s: List<String>?): List<MuscleGroups> =
        s?.flatMap { it.split(",", ";") }?.mapNotNull { t ->
            MuscleGroups.entries.firstOrNull { it.name.equals(t.trim(), true) }
        } ?: emptyList()
    return UserProfile(
        name = prefs.getName(),
        weightKg = toIntSafe(prefs.getWeight(), 75),
        heightCm = toIntSafe(prefs.getHeight(), 175),
        age = toIntSafe(prefs.getAge(), 25),
        experience = prefs.getExperience().lowercase(Locale.US),
        preferredStyle = prefs.getPreferredStyle().lowercase(Locale.US),
        importantMuscles = toListOfMuscles(prefs.getImportantMuscles())
    )
}

// ==========================================
// RECOVERY LOGIC
// ==========================================

data class RecoveryFactors(
    val sleepHours: Float?,
    val restingHeartRate: Long?,
    val sleepSpo2: Double?,
    val proteinGrams: Double?,
    val bodyFatPercentage: Double?,
    val dailyCalories: Double?,
    val age: Int,
    val survey: SurveyInsights // Added Survey Context
) {
    val recoveryEfficacy: Float by lazy {
        var score = 1.0f

        // 1. Sleep
        val s = sleepHours ?: 7.5f
        score *= when {
            s >= 8.5f -> 1.15f; s >= 7.5f -> 1.05f; s >= 6.5f -> 1.0f
            s >= 5.5f -> 0.85f; s >= 4.0f -> 0.60f; else -> 0.40f
        }

        // 2. RHR
        val rhr = restingHeartRate ?: 60L
        score *= when {
            rhr < 50 -> 1.10f; rhr < 60 -> 1.05f; rhr < 70 -> 1.00f
            rhr < 80 -> 0.90f; rhr < 90 -> 0.80f; else -> 0.70f
        }

        // 3. Nutrition
        if (proteinGrams != null) {
            score *= when {
                proteinGrams > 180.0 -> 1.10f; proteinGrams > 140.0 -> 1.05f
                proteinGrams > 100.0 -> 1.00f; proteinGrams > 60.0 -> 0.85f; else -> 0.70f
            }
        }

        // 4. Survey: Self-Assessed Recovery Rate
        score *= when {
            survey.recoverySelfAssessment.contains("Fast") -> 1.15f
            survey.recoverySelfAssessment.contains("Slow") -> 0.85f
            else -> 1.0f
        }

        score *= when (survey.stressLevel) {
            "Low / Relaxed" -> 1.10f
            "Moderate" -> 1.0f
            "High" -> 0.90f
            else -> 0.75f
        }
        // 5. Survey: Stimulants
        // High stimulants might mask fatigue, but biologically they don't increase tissue repair speed.
        // However, if they are "Stimulant Free", we might slightly boost base score as sleep quality is likely better.
        if (survey.stimulantUsage.contains("Free")) score *= 1.05f
        score *= when {
            survey.waterIntake.contains("4+") || survey.waterIntake.contains("3-4") -> 1.05f
            survey.waterIntake.contains("1-2") -> 1.0f
            else -> 0.90f
        }

        score.coerceIn(0.5f, 1.5f)
    }

    val sleepMultiplier: Float get() = if ((sleepHours ?: 7.5f) >= 7.5f) 1.1f else 0.9f
    val rhrMultiplier: Float get() = if ((restingHeartRate ?: 65) < 60) 1.1f else 0.9f
    val spo2Multiplier: Float get() = if ((sleepSpo2 ?: 96.0) > 95.0) 1.1f else 0.9f
    val proteinMultiplier: Float get() = if ((proteinGrams ?: 100.0) > 140) 1.1f else 0.9f
    val caloriesMultiplier: Float get() = if ((dailyCalories ?: 2000.0) > 2500) 1.05f else 0.95f

    // New: Subjective Multiplier for UI display
    val subjectiveMultiplier: Float get() = when {
        survey.recoverySelfAssessment.contains("Fast") -> 1.15f
        survey.recoverySelfAssessment.contains("Slow") -> 0.85f
        else -> 1.0f
    }
}

// ==========================================
// WORKOUT PARSING & VOLUME
// ==========================================

data class WorkoutSummary(val date: Instant, val name: String, val exercises: List<String> = emptyList())

data class MuscleLoad(
    val group: MuscleGroups,
    val weeklyProgress: Float,
    val weeklyTarget: Float,
    val band: LoadBand,
    val score: Float,
    val lastTrainedAgo: String? = null
)

private fun friendlyAgo(now: Instant, then: Instant): String {
    val mins = ChronoUnit.MINUTES.between(then, now)
    if (mins < 60) return "${max(0L, mins)}m ago"
    val hours = ChronoUnit.HOURS.between(then, now)
    if (hours < 24) return "${hours}h ago"
    val days = ChronoUnit.DAYS.between(then, now)
    return if (days == 1L) "1d ago" else if (days < 7) "${days}d ago" else "${days / 7}w ago"
}

private val setsRx = Regex("""(\d+)\s*sets?""", RegexOption.IGNORE_CASE)
private val heavyRx = Regex("""(1rm|pr|max|heavy|failure|amrap)""", RegexOption.IGNORE_CASE)
private val lightRx = Regex("""(warmup|light|deload|rehab)""", RegexOption.IGNORE_CASE)

private fun extractEffectiveSets(text: String): Float {
    val s = setsRx.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
    val effectiveVolume = if (s > 5) 5f + ln(s - 4f) else s
    var mult = 1.0f
    if (heavyRx.containsMatchIn(text)) mult *= 1.25f
    if (lightRx.containsMatchIn(text)) mult *= 0.5f
    return effectiveVolume * mult
}

private val muscleMappings = listOf(
    Regex("bench|press|push-up|dip|fly") to mapOf(MuscleGroups.Pecs to 1.0f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    Regex("overhead|military|arnold|lateral|face pull") to mapOf(MuscleGroups.Delts to 1.0f, MuscleGroups.Triceps to 0.3f, MuscleGroups.Traps to 0.4f),
    Regex("pull-up|chin-up|pulldown|row") to mapOf(MuscleGroups.Lats to 0.9f, MuscleGroups.UpperBack to 0.8f, MuscleGroups.Biceps to 0.6f),
    Regex("squat|lunge|step|press") to mapOf(MuscleGroups.Quads to 1.0f, MuscleGroups.Glutes to 0.7f, MuscleGroups.LowerBack to 0.4f),
    Regex("deadlift|rdl|hinge|clean") to mapOf(MuscleGroups.Hamstrings to 0.9f, MuscleGroups.Glutes to 0.8f, MuscleGroups.LowerBack to 1.0f, MuscleGroups.Traps to 0.6f),
    Regex("curl") to mapOf(MuscleGroups.Biceps to 1.0f, MuscleGroups.Forearms to 0.3f),
    Regex("extension|skull|kickback|pushdown") to mapOf(MuscleGroups.Triceps to 1.0f),
    Regex("raise|calf") to mapOf(MuscleGroups.Calves to 1.0f),
    Regex("plank|crunch|sit-up|leg raise") to mapOf(MuscleGroups.Abs to 1.0f)
)

private fun getAccumulatedStimulus(recent: List<WorkoutSummary>, now: Instant, days: Long): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(days))
    val accumulator = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.forEach { workout ->
        val tokens = if (workout.exercises.isNotEmpty()) workout.exercises else listOf(workout.name)
        tokens.forEach { t ->
            val rawSets = extractEffectiveSets(t)
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(t.lowercase())) {
                    impacts.forEach { (muscle, ratio) -> accumulator[muscle] = accumulator.getValue(muscle) + (rawSets * ratio) }
                }
            }
        }
    }
    return accumulator
}

// UPDATED: Takes SurveyInsights into account
private fun calculateBaseTarget(profile: UserProfile, survey: SurveyInsights, muscle: MuscleGroups): Float {
    var target = if (profile.experience.contains("year") && (profile.experience.filter { it.isDigit() }.toIntOrNull() ?: 0) > 3) 16f else 14f

    // Goal Adjustment
    target *= when {
        survey.goal.contains("Strength") -> 0.85f // Strength focuses on intensity, slightly lower volume
        survey.goal.contains("Endurance") -> 1.2f // Endurance tolerates higher set counts
        else -> 1.0f // Hypertrophy/General
    }

    // Weight Adjustment
    if (profile.weightKg > 90) target *= 0.9f

    // Muscle Specific Baselines
    target *= when(muscle) {
        MuscleGroups.LowerBack -> 0.6f; MuscleGroups.Hamstrings -> 0.8f
        MuscleGroups.Delts, MuscleGroups.Calves, MuscleGroups.Abs -> 1.3f; else -> 1.0f
    }

    // Preference & Focus Area Boosts
    if (profile.importantMuscles.contains(muscle) || survey.focusMuscles.contains(muscle)) {
        target *= 1.25f
    }

    // Calisthenics boost
    if (profile.preferredStyle == "calisthenics") target *= 1.15f

    // Injury Limitations (Simple string matching on survey answers)
    val injuryMap = mapOf(
        "Shoulder" to listOf(MuscleGroups.Delts, MuscleGroups.Pecs),
        "Knee" to listOf(MuscleGroups.Quads, MuscleGroups.Calves),
        "Back" to listOf(MuscleGroups.LowerBack, MuscleGroups.Glutes)
    )

    survey.injuries.forEach { injury ->
        injuryMap.entries.forEach { (key, affected) ->
            if (injury.contains(key) && affected.contains(muscle)) {
                target *= 0.6f // Significantly reduce target volume if injured
            }
        }
    }

    return target
}

private fun calculateFatigueState(muscle: MuscleGroups, lastTrained: Instant?, now: Instant, acuteVolume: Float, recoveryEfficacy: Float): Float {
    if (lastTrained == null) return 0f
    val hoursSince = ChronoUnit.HOURS.between(lastTrained, now).coerceAtLeast(0)
    val baseHalfLife = (24f / muscle.recoverySpeed)
    val adjustedHalfLife = baseHalfLife / recoveryEfficacy
    val initialFatigue = acuteVolume * 10f * (0.8f + (muscle.cnsImpact * 0.4f))
    return (initialFatigue * exp(-(ln(2.0) / adjustedHalfLife) * hoursSince)).toFloat()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoadsStepwise(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    surveyTapeContent: String, // New Input
    onStep: (List<MuscleLoad>, RecoveryFactors, Int, Int) -> Unit,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    caloriesBurned: List<TotalCaloriesBurnedRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> {
    val survey = parseSurveyTape(surveyTapeContent)
    val (loads, factors) = deriveMuscleLoads(now, recent, profile, survey, sleepSessions, oxygenSaturations, nutrition, bodyFat, caloriesBurned, readHeartRate)
    val accumulated = mutableListOf<MuscleLoad>()
    loads.forEachIndexed { index, load ->
        accumulated.add(load)
        onStep(accumulated.toList(), factors, index + 1, loads.size)
        delay(20)
    }
    return loads to factors
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    survey: SurveyInsights,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    caloriesBurned: List<TotalCaloriesBurnedRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> = withContext(Dispatchers.Default) {

    val avgSleep = sleepSessions
        .filter { it.endTime.isAfter(now.minus(Duration.ofHours(24))) }
        .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        .toFloat()
        .div(60f)
        .takeIf { it > 0f }

    val recentSpo2 = oxygenSaturations.map { it.percentage.value }.average().takeIf { !it.isNaN() }
    val avgProtein = if (nutrition.isNotEmpty()) nutrition.filter { it.startTime.isAfter(now.minus(Duration.ofHours(48))) }.sumOf { it.protein?.inGrams ?: 0.0 } / 2.0 else null
    val currentBfp = bodyFat.maxByOrNull { it.time }?.percentage?.value

    val avgCalories = if (caloriesBurned.isNotEmpty()) {
        val daysFound = caloriesBurned.map { it.startTime.truncatedTo(ChronoUnit.DAYS) }.distinct().size.coerceAtLeast(1)
        caloriesBurned.sumOf { it.energy.inKilocalories } / daysFound
    } else null

    val hrRecords = readHeartRate(now.minus(Duration.ofHours(24)), now)
    val restingHr = if (hrRecords.isNotEmpty()) hrRecords.flatMap { it.samples }.map { it.beatsPerMinute }.average().toLong() else null

    val recoveryFactors = RecoveryFactors(
        sleepHours = avgSleep, restingHeartRate = restingHr, sleepSpo2 = recentSpo2,
        proteinGrams = avgProtein, bodyFatPercentage = currentBfp, dailyCalories = avgCalories,
        age = profile.age, survey = survey
    )

    val weeklyVolume = getAccumulatedStimulus(recent, now, 7)
    val acute24h = getAccumulatedStimulus(recent, now, 1)
    val lastTrainedMap = mutableMapOf<MuscleGroups, Instant>()
    recent.sortedBy { it.date }.forEach { w ->
        val tokens = w.exercises.ifEmpty { listOf(w.name) }
        tokens.forEach { t ->
            muscleMappings.forEach { (rx, map) ->
                if (rx.containsMatchIn(t.lowercase())) map.keys.forEach { m -> lastTrainedMap[m] = w.date }
            }
        }
    }

    val loads = MuscleGroups.entries.map { muscle ->
        val baseTarget = calculateBaseTarget(profile, survey, muscle)
        val effectiveTarget = baseTarget * recoveryFactors.recoveryEfficacy
        val currentVol = weeklyVolume[muscle] ?: 0f
        val lastDate = lastTrainedMap[muscle]
        val fatigueScore = calculateFatigueState(muscle, lastDate, now, acute24h[muscle] ?: 0f, recoveryFactors.recoveryEfficacy)

        val band = when {
            lastDate == null || ChronoUnit.DAYS.between(lastDate, now) > 14 -> LoadBand.NotTrained
            fatigueScore > 40f -> LoadBand.Overreached
            fatigueScore > 20f -> LoadBand.Recovering
            currentVol > effectiveTarget * 1.3f -> LoadBand.DeloadRecommended
            currentVol >= effectiveTarget * 0.8f -> LoadBand.OnTrack
            currentVol >= effectiveTarget * 0.3f -> LoadBand.Building
            else -> LoadBand.SlightlyTrained
        }
        val progressPct = (currentVol / effectiveTarget).coerceIn(0f, 1.5f)
        MuscleLoad(muscle, currentVol, effectiveTarget, band, progressPct, lastDate?.let { friendlyAgo(now, it) })
    }
    loads to recoveryFactors
}
// ==========================================
// INSIGHTS & MISSIONS
// ==========================================

data class Mission(
    val group: MuscleGroups, val setsGoalToday: Int, val setsDoneToday: Int,
    val priorityScore: Float, val reason: String, val exercises: List<String>, val lifestyleHints: List<String>
)

data class MuscleInsight(
    val growthPct: Float, val growthLabel: String, val fatigue: Float,
    val readinessText: String, val suggestedRpeMin: Int, val suggestedRpeMax: Int, val recommendation: String
)

private data class LifestyleSignals(
    val sleepOk: Boolean, val rhrOk: Boolean, val spo2Ok: Boolean, val proteinOk: Boolean, val proteinTargetG: Int
)

private fun lifestyleSignalsFrom(profile: UserProfile, factors: RecoveryFactors): LifestyleSignals {
    val protein = factors.proteinGrams ?: 0.0
    val protTarget = (profile.weightKg * 1.8).toInt()
    return LifestyleSignals(
        sleepOk = (factors.sleepHours ?: 7.5f) >= 7.0f,
        rhrOk = (factors.restingHeartRate ?: 60L) < 75L,
        spo2Ok = (factors.sleepSpo2 ?: 97.0) >= 95.0,
        proteinOk = protein >= protTarget * 0.85,
        proteinTargetG = protTarget
    )
}

private fun recentExercisesByMuscle(recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, List<String>> {
    val map = mutableMapOf<MuscleGroups, MutableMap<String, Int>>()
    recent.forEach { w ->
        val tokens = w.exercises.ifEmpty { listOf(w.name) }
        tokens.forEach { t ->
            muscleMappings.forEach { (rx, impacts) ->
                if (rx.containsMatchIn(t.lowercase())) impacts.keys.forEach { m ->
                    val clean = t.replace(Regex("""\d+x\d+.*"""), "").trim()
                    if (clean.length > 3) map.getOrPut(m) { mutableMapOf() }[clean] = (map.getOrPut(m) { mutableMapOf() }[clean] ?: 0) + 1
                }
            }
        }
    }
    return map.mapValues { (_, counts) -> counts.entries.sortedByDescending { it.value }.map { it.key }.take(4) }
}

private fun chooseExercisesFor(m: MuscleGroups, recentMap: Map<MuscleGroups, List<String>>, count: Int): List<String> {
    val hist = recentMap[m] ?: emptyList()
    if (hist.size >= count) return hist
    val defaults = when(m) {
        MuscleGroups.Pecs -> listOf("Bench Press", "Incline DB Press", "Cable Fly")
        MuscleGroups.Delts -> listOf("Overhead Press", "Lateral Raise", "Face Pull")
        MuscleGroups.Biceps -> listOf("Barbell Curl", "Hammer Curl")
        MuscleGroups.Triceps -> listOf("Tricep Pushdown", "Skullcrushers")
        MuscleGroups.Lats -> listOf("Pull Up", "Lat Pulldown")
        MuscleGroups.Traps -> listOf("Shrugs", "Rack Pulls")
        MuscleGroups.Abs -> listOf("Hanging Leg Raise", "Plank")
        MuscleGroups.Forearms -> listOf("Wrist Curl", "Reverse Curl")
        MuscleGroups.Quads -> listOf("Squat", "Leg Extension")
        MuscleGroups.Hamstrings -> listOf("RDL", "Leg Curl")
        MuscleGroups.Glutes -> listOf("Hip Thrust", "Kickback")
        MuscleGroups.Calves -> listOf("Calf Raise")
        MuscleGroups.LowerBack -> listOf("Back Extension")
        MuscleGroups.UpperBack -> listOf("Rear Delt Fly", "Row")
    }
    return (hist + defaults).distinct().take(count)
}

private fun twoWindowGrowth(now: Instant, recent: List<WorkoutSummary>): Pair<Map<MuscleGroups, Float>, Map<MuscleGroups, Float>> {
    val t0 = now; val t1 = now.minus(Duration.ofDays(14)); val t2 = now.minus(Duration.ofDays(28))
    val current = getAccumulatedStimulus(recent.filter { it.date.isAfter(t1) }, t0, 14)
    val prev = getAccumulatedStimulus(recent.filter { it.date.isAfter(t2) && it.date.isBefore(t1) }, t1, 14)
    return prev to current
}

private fun insightForMuscle(
    m: MuscleGroups, now: Instant, lastTrained: Map<MuscleGroups, Instant>,
    prev14: Map<MuscleGroups, Float>, cur14: Map<MuscleGroups, Float>, acute24: Map<MuscleGroups, Float>,
    recovery: RecoveryFactors
): MuscleInsight {
    val currentVol = cur14[m] ?: 0f
    val prevVol = prev14[m] ?: 0f
    val delta = if (prevVol == 0f) 100f else ((currentVol - prevVol) / prevVol) * 100f
    val fatigueScore = calculateFatigueState(m, lastTrained[m], now, acute24[m] ?: 0f, recovery.recoveryEfficacy)
    val fatigueNorm = (fatigueScore / 50f).coerceIn(0f, 1f)
    val readiness = if (fatigueNorm > 0.8f) "Highly Fatigued" else if (fatigueNorm > 0.4f) "Recovering" else "Fresh & Ready"
    val rec = if (fatigueNorm > 0.6f) "Focus on active recovery." else if (delta < -10f) "Volume dropping. Increase sets." else "System primed for overload."
    return MuscleInsight(
        delta, if (delta > 15f) "rising" else if (delta < -15f) "falling" else "stable",
        fatigueNorm, readiness, if (fatigueNorm > 0.5f) 5 else 7, if (fatigueNorm > 0.5f) 7 else 9, rec
    )
}

private enum class BodyCategory { All, UpperBody, Arms, Core, LowerBody }
private fun categoryOf(name: String): BodyCategory = when (name.lowercase()) {
    "pecs", "delts", "lats", "traps", "upperback", "upper back" -> BodyCategory.UpperBody
    "biceps", "triceps", "forearms" -> BodyCategory.Arms
    "abs", "lowerback", "lower back" -> BodyCategory.Core
    "quads", "hamstrings", "glutes", "calves" -> BodyCategory.LowerBody
    else -> BodyCategory.UpperBody
}

private fun planDailyMissions(
    now: Instant, profile: UserProfile, loads: List<MuscleLoad>, weekly: Map<MuscleGroups, Float>,
    daily: Map<MuscleGroups, Float>, lastTrained: Map<MuscleGroups, Instant>,
    recentExerciseMap: Map<MuscleGroups, List<String>>, lifestyle: LifestyleSignals
): List<Mission> {
    val priorityBoost = profile.importantMuscles.associateWith { 1.15f }
    return loads.mapNotNull { ml ->
        val wk = weekly[ml.group] ?: 0f
        val day = daily[ml.group] ?: 0f
        val pct = if (ml.weeklyTarget <= 0f) 0f else wk / ml.weeklyTarget
        val hrs = lastTrained[ml.group]?.let { ChronoUnit.HOURS.between(it, now).coerceAtLeast(0) } ?: Long.MAX_VALUE

        if (ml.band == LoadBand.DeloadRecommended) return@mapNotNull null
        if (ml.band == LoadBand.Recovering && hrs < 6) return@mapNotNull null

        val boost = priorityBoost[ml.group] ?: 1f
        val fatiguePenalty = (if (lifestyle.sleepOk) 0f else 0.12f) + (if (lifestyle.proteinOk) 0f else 0.08f)
        val basePerDay = (ml.weeklyTarget / 7f)
        val bump = if (pct < 0.5f) 0.55f else 0.08f
        val goal = (basePerDay * (1f + bump) * boost * (1f - fatiguePenalty)).coerceIn(2f, 10f).toInt()

        val score = (when(ml.band) { LoadBand.NotTrained -> 1f; LoadBand.Building -> 0.8f; else -> 0.5f } + (1f-pct)) * (1f - fatiguePenalty)

        Mission(ml.group, goal, day.toInt(), score, "Maintain volume", chooseExercisesFor(ml.group, recentExerciseMap, 3), emptyList())
    }.sortedByDescending { it.priorityScore }.take(4)
}

@Composable
private fun CategoryFilterBar(selected: BodyCategory, onSelect: (BodyCategory) -> Unit) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val items = listOf(
        BodyCategory.All to "All", BodyCategory.UpperBody to "Upper", BodyCategory.Arms to "Arms",
        BodyCategory.Core to "Core", BodyCategory.LowerBody to "Lower"
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (cat, label) ->
            val isSelected = selected == cat
            val pillRadius = 32.dp
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(pillRadius))
                    .clickable { onSelect(cat) }
                    .background(
                        if (isSelected) Brush.radialGradient(listOf(theme.secondary, theme.tertiary))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(1.dp, if (isSelected) theme.primary.copy(0.6f) else Color(0xFF525252), RoundedCornerShape(pillRadius))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if(isSelected) Modifier.glow(theme.primary, radius = 8.dp) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSelected) theme.primary else Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MuscleStatusSection(
    recentWorkouts: List<WorkoutSummary>,
    advicePayload: String?,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier,
    onOpenWeeklySummary: () -> Unit = {}
) {
    val context = LocalContext.current

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors


    LaunchedEffect(Unit) { SurveyTape.init(context) }

    val healthConnectManager = remember { HealthConnectManager(context) }
    var loads by remember { mutableStateOf<List<MuscleLoad>>(emptyList()) }
    var recoveryFactors by remember { mutableStateOf<RecoveryFactors?>(null) }
    var computing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val prefsManager = remember { UserPreferencesManager(context) }
    val profile = remember { buildUserProfile(prefsManager) }
    val recomputeKey = remember(recentWorkouts, advicePayload, profile) {
        "${recentWorkouts.size}-${profile.name}".hashCode()
    }

    LaunchedEffect(recomputeKey) {
        computing = true
        progress = 0f
        val now = Instant.ofEpochMilli(nowEpochMillis)
        val start = now.minus(30, ChronoUnit.DAYS)


        val sleep = healthConnectManager.readSleepSessions(start, now)
        val spo2 = healthConnectManager.readOxygenSaturation(start, now)
        val nutrition = healthConnectManager.readNutrition(now.minus(2, ChronoUnit.DAYS), now)
        val bodyfat = healthConnectManager.readBodyFat(start, now)
        val calories = healthConnectManager.readTotalCalories(now.minus(7, ChronoUnit.DAYS), now)


        val surveyContent = SurveyTape.readTape()

        val (finalLoads, factors) = deriveMuscleLoadsStepwise(
            now = now,
            recent = recentWorkouts,
            profile = profile,
            surveyTapeContent = surveyContent,
            onStep = { partial, fac, done, total ->
                loads = partial
                recoveryFactors = fac
                progress = done.toFloat() / total.toFloat()
            },
            sleepSessions = sleep,
            oxygenSaturations = spo2,
            nutrition = nutrition,
            bodyFat = bodyfat,
            caloriesBurned = calories,
            readHeartRate = { s, e -> healthConnectManager.readHeartRateRecords(s, e) }
        )
        loads = finalLoads
        recoveryFactors = factors
        computing = false
    }

    var selected by remember { mutableStateOf(BodyCategory.All) }
    var showHealth by remember { mutableStateOf(false) }
    var selectedMuscle by remember { mutableStateOf<MuscleLoad?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    if (computing) {
        LoadingScreen(progress = progress, modifier = modifier.fillMaxWidth())
        return
    }

    val now = Instant.ofEpochMilli(nowEpochMillis)
    val weekSets = remember { getAccumulatedStimulus(recentWorkouts, now, 7) }
    val daySets = remember { getAccumulatedStimulus(recentWorkouts, now, 1) }
    val lastTrained = remember(recentWorkouts) {
        val m = mutableMapOf<MuscleGroups, Instant>()
        recentWorkouts.forEach { w -> w.exercises.forEach { e -> muscleMappings.forEach { (r, mp) -> if(r.containsMatchIn(e.lowercase())) mp.keys.forEach { m[it] = w.date } } } }
        m
    }
    val recentMap = remember { recentExercisesByMuscle(recentWorkouts, profile) }
    val lifestyle = remember(recoveryFactors) { recoveryFactors?.let { lifestyleSignalsFrom(profile, it) } }

    val missions = remember(loads, lifestyle) {
        if (lifestyle == null) emptyList() else planDailyMissions(now, profile, loads, weekSets, daySets, lastTrained, recentMap, lifestyle)
    }

    val filtered = remember(loads, selected) { if (selected == BodyCategory.All) loads else loads.filter { categoryOf(it.group.name) == selected } }
    val sortedLoads = remember(filtered) { filtered.sortedByDescending { it.score } }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = onOpenWeeklySummary,
                modifier = Modifier.weight(1f).height(50.dp).glow(theme.tertiary, radius = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.tertiary,
                    contentColor = theme.primary
                ),
                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.FitnessCenter, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Weekly", style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalButton(
                onClick = { if (recoveryFactors != null) { showHealth = true; haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                modifier = Modifier.weight(1f).height(50.dp).glow(theme.tertiary, radius = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.tertiary,
                    contentColor = theme.primary
                ),
                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.MonitorHeart, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recovery", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (recoveryFactors != null) RecoveryFactorRow(factors = recoveryFactors!!, onClick = { showHealth = true })
        OverviewRow(loads = filtered)
        CategoryFilterBar(selected = selected, onSelect = { selected = it; haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap) })

        FlowRow(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortedLoads.forEach { item ->
                Box(modifier = Modifier.weight(1f, true).widthIn(min = 150.dp).clickable { selectedMuscle = item }) {
                    MuscleCircleTile(load = item)
                }
            }
            if (sortedLoads.size % 2 != 0) Spacer(Modifier.weight(1f).widthIn(min = 150.dp))
        }
    }

    if (showHealth && recoveryFactors != null) RecoveryDetailSheet(state = sheetState, onDismiss = { showHealth = false }, factors = recoveryFactors!!)
    selectedMuscle?.let { ml ->
        val (prev14, cur14) = remember { twoWindowGrowth(now, recentWorkouts) }
        val acute24 = remember { getAccumulatedStimulus(recentWorkouts, now, 1) }
        val insight = insightForMuscle(ml.group, now, lastTrained, prev14, cur14, acute24, recoveryFactors!!)
        MuscleDetailSheet(sheetState, { selectedMuscle = null }, ml, insight, chooseExercisesFor(ml.group, recentMap, 3))
    }
}

@Composable
private fun RecoveryFactorRow(factors: RecoveryFactors, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(2.dp))
        if (factors.sleepHours != null) FactorPill((factors.sleepMultiplier - 1f) * 100f, "Sleep", Icons.Outlined.NightlightRound)
        if (factors.restingHeartRate != null) FactorPill((factors.rhrMultiplier - 1f) * 100f, "RHR", Icons.Outlined.FavoriteBorder)
        if (factors.dailyCalories != null) FactorPill((factors.caloriesMultiplier - 1f) * 100f, "Burn", Icons.Outlined.LocalFireDepartment)

        // Subjective Survey Feedback
        FactorPill((factors.subjectiveMultiplier - 1f) * 100f, "Self-Check", Icons.Outlined.AutoAwesome)

        if (factors.proteinGrams != null) FactorPill((factors.proteinMultiplier - 1f) * 100f, "Protein", Icons.Outlined.Restaurant)
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun FactorPill(value: Float, label: String, icon: ImageVector) {
    val isPositive = value >= -0.1f
    val baseColor = if (isPositive) Color(0xFF00E676) else Color(0xFFFF3B30)
    Surface(
        color = baseColor.copy(0.1f), shape = RoundedCornerShape(32.dp), border = BorderStroke(1.dp, baseColor.copy(0.2f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Icon(icon, null, tint = baseColor, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.9f))
            Text(String.format(Locale.US, "%+.0f%%", value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = baseColor)
        }
    }
}

@Composable
private fun LoadingScreen(progress: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val animatedProgress by animateFloatAsState(progress, tween(400))
    Column(
        modifier = modifier.padding(16.dp).height(180.dp).clip(RoundedCornerShape(32.dp))
            .background(Brush.radialGradient(listOf(theme.secondary, theme.background), radius = 500f)).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Analyzing Biometrics", style = MaterialTheme.typography.titleMedium, color = theme.primary, fontWeight = FontWeight.SemiBold)
            Text("Syncing Health Connect & Survey Data", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.1f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(theme.primary, theme.primary.copy(alpha = 0.6f))))
                .glow(theme.primary, radius = 10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewRow(loads: List<MuscleLoad>) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val counts = remember(loads) {
        mapOf("Building" to (loads.count { it.band == LoadBand.Building } to Color(0xFF42A5F5)), "On Track" to (loads.count { it.band == LoadBand.OnTrack } to Color(0xFF00E676)),
            "Recovery" to (loads.count { it.band == LoadBand.Recovering } to Color(0xFFAB47BC)), "Warning" to (loads.count { it.band == LoadBand.Overreached } to Color(0xFFFFCA28)))
    }
    Surface(
        color = theme.tertiary,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, theme.secondary.copy(0.3f))
    ) {
        FlowRow(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.forEach { (label, data) ->
                if (data.first > 0) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(data.second.copy(0.12f)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(data.second).glow(data.second, 4.dp))
                        Text("$label ${data.first}", style = MaterialTheme.typography.labelSmall, color = data.second.copy(0.9f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleCircleTile(load: MuscleLoad) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val color = when (load.band) { LoadBand.Building -> Color(0xFF42A5F5); LoadBand.OnTrack -> Color(0xFF00E676); LoadBand.Recovering -> Color(0xFFAB47BC); else -> Color(0xFF9E9E9E) }
    val pct = if (load.weeklyTarget > 0f) ((load.weeklyProgress / load.weeklyTarget) * 100f) else 0f
    val animatedPct by animateFloatAsState(pct.coerceIn(0f, 100f), tween(1000, easing = FastOutSlowInEasing))

    Surface(
        color = theme.tertiary,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(color.copy(0.2f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    drawCircle(color = Color.White.copy(0.05f), style = Stroke(width = 6.dp.toPx()))
                    drawArc(brush = Brush.sweepGradient(listOf(color.copy(0.2f), color)), startAngle = -90f, sweepAngle = (animatedPct / 100f) * 360f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("${animatedPct.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(load.group.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Surface(color = color.copy(0.1f), shape = RoundedCornerShape(32.dp)) {
                    Text(load.band.name, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryDetailSheet(state: SheetState, onDismiss: () -> Unit, factors: RecoveryFactors) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = theme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.secondary) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Health Information", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)

            HealthMetricRow(Icons.Outlined.NightlightRound, "Sleep", factors.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "—", "Last Night", ((factors.sleepMultiplier - 0.85f) / 0.4f).coerceIn(0f, 1f))
            HealthMetricRow(Icons.Outlined.MonitorHeart, "RHR", factors.restingHeartRate?.toString() ?: "—", "BPM", if (factors.restingHeartRate == null) 0.5f else (1f - ((factors.restingHeartRate - 40f) / 60f).coerceIn(0f, 1f)))
            HealthMetricRow(Icons.Outlined.LocalFireDepartment, "Avg Burn", factors.dailyCalories?.let { "%.0f".format(it) } ?: "—", "kCal/day", 0.7f)

            // Added Subjective Row
            HealthMetricRow(
                Icons.Outlined.AutoAwesome,
                "Self-Check",
                factors.survey.recoverySelfAssessment,
                "Survey Response",
                ((factors.subjectiveMultiplier - 0.85f) / 0.3f).coerceIn(0f, 1f)
            )

            HealthMetricRow(Icons.Outlined.Restaurant, "Protein", factors.proteinGrams?.let { "%.0f g".format(it) } ?: "—", "Last 24h", if (factors.proteinGrams == null) 0.5f else (factors.proteinGrams.toFloat() / 180f).coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun HealthMetricRow(icon: ImageVector, label: String, value: String, subValue: String, pct: Float) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(theme.secondary.copy(alpha = 0.2f)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(theme.tertiary).glow(theme.tertiary, 6.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = theme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.titleSmall, color = theme.primary, fontWeight = FontWeight.Bold)
            }
            Text(subValue, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).glow(theme.primary, 4.dp), color = theme.primary, trackColor = theme.tertiary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDetailSheet(state: SheetState, onDismiss: () -> Unit, load: MuscleLoad, insight: MuscleInsight, exercises: List<String>) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val color = when (load.band) { LoadBand.Building -> Color(0xFF42A5F5); LoadBand.OnTrack -> Color(0xFF00E676); else -> Color(0xFF9E9E9E) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = theme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.secondary) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(load.group.name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                    Text(insight.readinessText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(color.copy(0.15f)).padding(horizontal = 12.dp, vertical = 6.dp).glow(color, 8.dp)) {
                    Text("${(load.weeklyProgress / load.weeklyTarget * 100f).toInt()}% Vol", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Surface(color = theme.secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(32.dp), border = BorderStroke(1.dp, color.copy(0.3f))) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = color, modifier = Modifier.size(20.dp))
                    Text(insight.recommendation, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f))
                }
            }
            if (exercises.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercises.forEach { ex ->
                        Surface(color = theme.tertiary, shape = RoundedCornerShape(32.dp)) {
                            Text(ex, style = MaterialTheme.typography.labelMedium, color = theme.primary.copy(alpha = 0.8f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}