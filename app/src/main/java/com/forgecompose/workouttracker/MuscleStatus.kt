package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord as HcHeartRateRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

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

data class SurveyInsights(
    val goal: String = "General Health",
    val experience: String = "Just starting",
    val daysAvailable: String = "3 days",
    val equipmentAccess: String = "Full Commercial Gym",
    val sessionLength: String = "45-60 mins",
    val focusMuscles: List<MuscleGroups> = emptyList(),
    val injuries: List<String> = emptyList(),
    val recoverySelfAssessment: String = "Average",
    val stimulantUsage: String = "Coffee / Natural only",
    val sleepQuality: String = "7-8 hours",
    val dietStyle: String = "No restrictions",
    val waterIntake: String = "1-2 Liters",
    val stressLevel: String = "Moderate",
    val cardioPreference: String = "No Cardio",
    val preferredTrainingTime: String = "Evening",
    val motivationSource: String = "Visual changes",
    val trackingStyle: String = "Mental Notes",
    val gripStrengthNeedsHelp: Boolean = false,
    val mobilityFrequency: String = "Rarely",
    val usesCreatine: Boolean = false
)
data class SprintGoal(
    val focusMuscles: List<MuscleGroups> = emptyList(),
    val durationWeeks: Int = 4,
    val isActive: Boolean = false
)

class SprintGoalPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("forge_sprint_goal", Context.MODE_PRIVATE)

    fun save(goal: SprintGoal) {
        prefs.edit()
            .putBoolean("is_active", goal.isActive)
            .putInt("duration", goal.durationWeeks)
            .putString("muscles", goal.focusMuscles.joinToString(",") { it.name })
            .apply()
    }

    fun load(): SprintGoal {
        val active = prefs.getBoolean("is_active", false)
        val duration = prefs.getInt("duration", 4)
        val musclesStr = prefs.getString("muscles", "") ?: ""
        val muscles = musclesStr.split(",").mapNotNull { name ->
            MuscleGroups.entries.find { it.name == name }
        }
        return SprintGoal(muscles, duration, active)
    }
}
fun parseSurveyTape(tape: String): SurveyInsights {
    if (tape.isBlank()) return SurveyInsights()

    val lastAnswers = mutableMapOf<String, String>()
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
        focusMuscles = focusList,
        injuries = (lastAnswers["limitations"] ?: "").split(",").filter { it.isNotBlank() && !it.contains("No injuries") },
        recoverySelfAssessment = lastAnswers["recovery_rate"] ?: "Average",
        stimulantUsage = lastAnswers["stimulants"] ?: "Coffee / Natural only",
        sleepQuality = lastAnswers["sleep_quality"] ?: "7-8 hours",
        dietStyle = lastAnswers["diet_style"] ?: "No restrictions",
        waterIntake = lastAnswers["water_intake"] ?: "1-2 Liters",
        stressLevel = lastAnswers["stress_levels"] ?: "Moderate",
        cardioPreference = lastAnswers["cardio_pref"] ?: "No Cardio",
        preferredTrainingTime = lastAnswers["training_time"] ?: "Evening",
        motivationSource = lastAnswers["motivation"] ?: "Visual changes",
        trackingStyle = lastAnswers["tracking_style"] ?: "Mental Notes",
        gripStrengthNeedsHelp = needsStraps,
        mobilityFrequency = lastAnswers["mobility"] ?: "Rarely",
        usesCreatine = takesCreatine
    )
}

enum class MuscleGroups(val sizeModifier: Float, val cnsImpact: Float, val localRecoverySpeed: Float) {
    Pecs(1.0f, 0.6f, 1.0f), Delts(0.7f, 0.3f, 1.3f), Biceps(0.5f, 0.2f, 1.5f), Triceps(0.6f, 0.3f, 1.3f),
    Lats(1.2f, 0.7f, 0.9f), Traps(0.8f, 0.5f, 1.4f), Abs(0.4f, 0.2f, 1.8f), Forearms(0.3f, 0.1f, 1.9f),
    Quads(1.4f, 0.9f, 0.8f), Hamstrings(1.3f, 0.8f, 0.8f), Glutes(1.5f, 0.8f, 0.9f), Calves(0.6f, 0.2f, 1.6f),
    LowerBack(1.0f, 0.9f, 0.6f), UpperBack(0.9f, 0.5f, 1.1f)
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
) {
    val bmr: Double get() = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + 5.0
}

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

data class RecoveryFactors(
    val sleepHours: Float?,
    val restingHeartRate: Long?,
    val sleepSpo2: Double?,
    val proteinGrams: Double?,
    val bodyFatPercentage: Double?,
    val dailyCalories: Double?,
    val profile: UserProfile,
    val survey: SurveyInsights
) {
    private fun getAgeAdjustedRHR(age: Int): Double = 55.0 + (age - 20).coerceAtLeast(0) * 0.3

    private fun sigmoid(x: Double, target: Double, steepness: Double = 1.0): Double {
        return 1.0 / (1.0 + exp(-steepness * (x - target)))
    }

    val recoveryEfficacy: Float by lazy {
        var baseScore = 1.0

        val s = sleepHours?.toDouble() ?: 7.5
        val sleepScore = when {
            s < 5.0 -> 0.6
            s < 6.5 -> 0.8 + ((s - 5.0) / 1.5) * 0.2
            s < 8.5 -> 1.0 + ((s - 6.5) / 2.0) * 0.1
            else -> 1.1
        }
        baseScore *= sleepScore

        val proteinTarget = if (survey.dietStyle.contains("Cut", true)) 2.2 * profile.weightKg else 1.8 * profile.weightKg
        val p = proteinGrams ?: (proteinTarget * 0.9)
        val pRatio = (p / proteinTarget).coerceIn(0.5, 1.5)

        val calsTarget = profile.bmr * when(survey.dietStyle) { "Bulking" -> 1.5; "Cutting" -> 1.2; else -> 1.35 }
        val c = dailyCalories ?: calsTarget
        val cRatio = (c / calsTarget).coerceIn(0.6, 1.4)

        val nutritionMod = if (cRatio < 0.9) (0.85 * pRatio.pow(0.5)) else (1.0 + (cRatio - 1.0) * 0.2) * pRatio.pow(0.3)
        baseScore *= nutritionMod

        val rhrTarget = getAgeAdjustedRHR(profile.age)
        val rhr = restingHeartRate?.toDouble() ?: rhrTarget
        val rhrStress = if (rhr > rhrTarget + 10) 0.85 else if (rhr > rhrTarget + 5) 0.95 else 1.05

        val spo2 = sleepSpo2 ?: 98.0
        val spo2Mod = if (spo2 < 93.0) 0.9 else 1.0

        val reportedStress = when(survey.stressLevel) {
            "High" -> 0.85
            "Moderate" -> 0.98
            "Low / Relaxed" -> 1.05
            else -> 1.0
        }

        baseScore *= (rhrStress * spo2Mod * reportedStress)

        val ageDampener = if (profile.age > 35) 1.0 - ((profile.age - 35) * 0.008) else 1.0
        baseScore *= ageDampener

        if (survey.usesCreatine) baseScore *= 1.05

        baseScore.toFloat().coerceIn(0.4f, 1.6f)
    }

    val sleepMultiplier: Float get() = (sleepHours?.let { if(it >= 7.5) 1.1f else if(it >= 6) 1.0f else 0.8f } ?: 1.0f)
    val rhrMultiplier: Float get() {
        val target = getAgeAdjustedRHR(profile.age)
        val current = restingHeartRate?.toDouble() ?: target
        return if(current < target) 1.1f else if(current > target + 8) 0.8f else 1.0f
    }
    val proteinMultiplier: Float get() {
        val target = 2.0 * profile.weightKg
        val current = proteinGrams ?: (target * 0.8)
        return (current / target).toFloat().coerceIn(0.8f, 1.2f)
    }
    val caloriesMultiplier: Float get() {
        val target = profile.bmr * 1.4
        return ((dailyCalories ?: target) / target).toFloat().coerceIn(0.8f, 1.2f)
    }
    val subjectiveMultiplier: Float get() = when {
        survey.recoverySelfAssessment.contains("Fast") -> 1.1f
        survey.recoverySelfAssessment.contains("Slow") -> 0.9f
        else -> 1.0f
    } * (if(survey.stressLevel == "High") 0.9f else 1.0f)
}

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
private val heavyRx = Regex("""(1rm|pr|max|heavy|failure|amrap|rpe\s*(9|10))""", RegexOption.IGNORE_CASE)
private val lightRx = Regex("""(warmup|light|deload|rehab|rpe\s*[1-5])""", RegexOption.IGNORE_CASE)

private fun extractEffectiveSets(text: String): Float {
    val s = setsRx.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
    var fatigueCost = s
    val effectiveStimulus = if (s > 8) 8f + (s - 8f) * 0.5f else s

    var intensityMult = 1.0f
    if (heavyRx.containsMatchIn(text)) intensityMult = 1.3f
    if (lightRx.containsMatchIn(text)) intensityMult = 0.6f

    return effectiveStimulus * intensityMult
}

private val muscleMappings = listOf(
    Regex("bench|press|push-up|dip|fly") to mapOf(MuscleGroups.Pecs to 1.0f, MuscleGroups.Triceps to 0.5f, MuscleGroups.Delts to 0.4f),
    Regex("overhead|military|arnold|lateral|face pull") to mapOf(MuscleGroups.Delts to 1.0f, MuscleGroups.Triceps to 0.2f, MuscleGroups.Traps to 0.3f),
    Regex("pull-up|chin-up|pulldown|row") to mapOf(MuscleGroups.Lats to 0.9f, MuscleGroups.UpperBack to 0.8f, MuscleGroups.Biceps to 0.5f),
    Regex("squat|lunge|step|press") to mapOf(MuscleGroups.Quads to 1.0f, MuscleGroups.Glutes to 0.6f, MuscleGroups.LowerBack to 0.3f, MuscleGroups.Calves to 0.2f),
    Regex("deadlift|rdl|hinge|clean") to mapOf(MuscleGroups.Hamstrings to 0.9f, MuscleGroups.Glutes to 0.8f, MuscleGroups.LowerBack to 1.0f, MuscleGroups.Traps to 0.6f, MuscleGroups.Forearms to 0.4f),
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

private fun calculateBaseTarget(profile: UserProfile, survey: SurveyInsights, muscle: MuscleGroups, goal: SprintGoal): Float {
    var target = when {
        profile.experience.contains("advanced") -> 18f
        profile.experience.contains("intermediate") || profile.experience.contains("year") -> 14f
        else -> 10f
    }

    val daysAvailable = survey.daysAvailable.filter { it.isDigit() }.toIntOrNull() ?: 3
    val maxRecoverablePerSession = 8f
    val maxTheoreticalWeekly = daysAvailable * maxRecoverablePerSession * 1.5
    target = target.coerceAtMost(maxTheoreticalWeekly.toFloat())

    if (profile.preferredStyle == "calisthenics") target *= 1.2f
    if (survey.goal.contains("Strength")) target *= 0.8f

    if (profile.age > 40) {
        val decay = (profile.age - 40) * 0.2f
        target = (target - decay).coerceAtLeast(6f)
    }

    target *= when(muscle) {
        MuscleGroups.LowerBack -> 0.5f
        MuscleGroups.Hamstrings -> 0.8f
        MuscleGroups.Delts, MuscleGroups.Calves, MuscleGroups.Abs -> 1.4f
        MuscleGroups.Quads -> 1.1f
        else -> 1.0f
    }

    if (profile.importantMuscles.contains(muscle) || survey.focusMuscles.contains(muscle)) {
        target *= 1.2f
    }

    if (goal.isActive && goal.focusMuscles.contains(muscle)) {
        target *= 1.35f
    }

    val injuryMap = mapOf(
        "Shoulder" to listOf(MuscleGroups.Delts, MuscleGroups.Pecs),
        "Knee" to listOf(MuscleGroups.Quads, MuscleGroups.Calves),
        "Back" to listOf(MuscleGroups.LowerBack, MuscleGroups.Glutes),
        "Elbow" to listOf(MuscleGroups.Triceps, MuscleGroups.Biceps, MuscleGroups.Forearms)
    )

    survey.injuries.forEach { injury ->
        injuryMap.entries.forEach { (key, affected) ->
            if (injury.contains(key, true) && affected.contains(muscle)) {
                target *= 0.5f
            }
        }
    }

    return target
}
private fun calculateFatigueState(
    muscle: MuscleGroups,
    lastTrained: Instant?,
    now: Instant,
    acuteVolume: Float,
    recoveryEfficacy: Float,
    systemicFatigue: Float
): Float {
    if (lastTrained == null) return 0f

    val hoursSince = ChronoUnit.HOURS.between(lastTrained, now).coerceAtLeast(0)
    val baseHalfLife = 24f * muscle.localRecoverySpeed * (1f + (muscle.sizeModifier * 0.3f))
    val effectiveHalfLife = baseHalfLife * (1.0f + (systemicFatigue * 0.5f)) / recoveryEfficacy
    val initialFatigue = acuteVolume * 10f * (0.9f + (muscle.cnsImpact * 0.5f))
    val residual = (initialFatigue * exp(-(ln(2.0) / effectiveHalfLife) * hoursSince)).toFloat()

    val domsCurve = if(hoursSince < 72) {
        val peakTime = 24f + (muscle.sizeModifier * 10f)
        val sigma = 12f
        val domsHeight = initialFatigue * 0.4f * (1f / recoveryEfficacy)
        domsHeight * exp(-((hoursSince - peakTime).pow(2)) / (2 * sigma.pow(2)))
    } else 0.0

    return residual + domsCurve.toFloat()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoadsStepwise(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    surveyTapeContent: String,
    sprintGoal: SprintGoal,
    onStep: (List<MuscleLoad>, RecoveryFactors, Int, Int) -> Unit,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    caloriesBurned: List<TotalCaloriesBurnedRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> {
    val survey = parseSurveyTape(surveyTapeContent)
    val (loads, factors) = deriveMuscleLoads(now, recent, profile, survey, sprintGoal, sleepSessions, oxygenSaturations, nutrition, bodyFat, caloriesBurned, readHeartRate)
    val accumulated = mutableListOf<MuscleLoad>()
    loads.forEachIndexed { index, load ->
        accumulated.add(load)
        onStep(accumulated.toList(), factors, index + 1, loads.size)
        delay(10)
    }
    return loads to factors
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    survey: SurveyInsights,
    sprintGoal: SprintGoal,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    caloriesBurned: List<TotalCaloriesBurnedRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> = withContext(Dispatchers.Default) {

    val lastNightSleepDuration = sleepSessions
        .filter { it.endTime.isAfter(now.minus(Duration.ofHours(24))) }
        .maxByOrNull { Duration.between(it.startTime, it.endTime).toMinutes() }
        ?.let { Duration.between(it.startTime, it.endTime).toMinutes() / 60f }

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
        sleepHours = lastNightSleepDuration,
        restingHeartRate = restingHr,
        sleepSpo2 = recentSpo2,
        proteinGrams = avgProtein,
        bodyFatPercentage = currentBfp,
        dailyCalories = avgCalories,
        profile = profile,
        survey = survey
    )

    val weeklyVolume = getAccumulatedStimulus(recent, now, 7)
    val acute24h = getAccumulatedStimulus(recent, now, 1)

    val totalSystemicLoad = MuscleGroups.entries.sumOf { ((acute24h[it] ?: 0f) * it.sizeModifier).toDouble() }.toFloat()
    val systemicFatigueNorm = (totalSystemicLoad / 30f).coerceIn(0f, 1f)

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
        val baseTarget = calculateBaseTarget(profile, survey, muscle, sprintGoal)
        val effectiveTarget = baseTarget * recoveryFactors.recoveryEfficacy
        val currentVol = weeklyVolume[muscle] ?: 0f
        val lastDate = lastTrainedMap[muscle]

        val fatigueScore = calculateFatigueState(
            muscle, lastDate, now,
            acute24h[muscle] ?: 0f,
            recoveryFactors.recoveryEfficacy,
            systemicFatigueNorm
        )

        val band = when {
            lastDate == null || ChronoUnit.DAYS.between(lastDate, now) > 14 -> LoadBand.NotTrained
            fatigueScore > 45f -> LoadBand.Overreached
            fatigueScore > 20f -> LoadBand.Recovering
            currentVol > effectiveTarget * 1.4f -> LoadBand.DeloadRecommended
            currentVol >= effectiveTarget * 0.85f -> LoadBand.OnTrack
            currentVol >= effectiveTarget * 0.3f -> LoadBand.Building
            else -> LoadBand.SlightlyTrained
        }
        val progressPct = (currentVol / effectiveTarget).coerceIn(0f, 1.5f)
        MuscleLoad(muscle, currentVol, effectiveTarget, band, progressPct, lastDate?.let { friendlyAgo(now, it) })
    }
    loads to recoveryFactors
}

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
        sleepOk = (factors.sleepHours ?: 7.5f) >= 6.5f,
        rhrOk = (factors.restingHeartRate ?: 60L) < (55 + (profile.age - 20) * 0.5 + 10),
        spo2Ok = (factors.sleepSpo2 ?: 97.0) >= 94.0,
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

    val systemicLoad = acute24.values.sum()
    val sysNorm = (systemicLoad / 40f).coerceIn(0f, 1f)

    val fatigueScore = calculateFatigueState(m, lastTrained[m], now, acute24[m] ?: 0f, recovery.recoveryEfficacy, sysNorm)
    val fatigueNorm = (fatigueScore / 50f).coerceIn(0f, 1f)

    val readiness = when {
        fatigueNorm > 0.8f -> "Deeply Fatigued"
        fatigueNorm > 0.5f -> "Recovering"
        fatigueNorm > 0.2f -> "Ready"
        else -> "Fully Rested"
    }

    val rec = when {
        fatigueNorm > 0.6f -> "High CNS load detected. Active recovery only."
        fatigueNorm > 0.4f -> "Trainable, but keep RPE < 7."
        delta < -15f -> "Volume dropping. Increase frequency or sets."
        delta > 20f -> "Great progress. Ensure calories match output."
        else -> "System primed for progressive overload."
    }

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
    val priorityBoost = profile.importantMuscles.associateWith { 1.25f }
    return loads.mapNotNull { ml ->
        val wk = weekly[ml.group] ?: 0f
        val day = daily[ml.group] ?: 0f
        val pct = if (ml.weeklyTarget <= 0f) 0f else wk / ml.weeklyTarget
        val hrs = lastTrained[ml.group]?.let { ChronoUnit.HOURS.between(it, now).coerceAtLeast(0) } ?: Long.MAX_VALUE

        if (ml.band == LoadBand.DeloadRecommended || ml.band == LoadBand.Overreached) return@mapNotNull null
        if (ml.band == LoadBand.Recovering && hrs < 24) return@mapNotNull null

        val boost = priorityBoost[ml.group] ?: 1f
        val fatiguePenalty = (if (lifestyle.sleepOk) 0f else 0.15f) + (if (lifestyle.proteinOk) 0f else 0.1f)

        val idealFreq = if(ml.group.sizeModifier > 1.2f) 2.0f else 3.0f
        val basePerSession = (ml.weeklyTarget / idealFreq)

        val urgency = if (pct < 0.5f) 1.2f else 0.8f
        val goal = (basePerSession * urgency * boost * (1f - fatiguePenalty)).coerceIn(2f, 8f).toInt()

        val gapScore = (1f - pct).coerceAtLeast(0f) * 10f
        val freshnessScore = (hrs / 96f).coerceAtMost(1f) * 5f
        val priorityScore = gapScore + freshnessScore + (if (boost > 1f) 5f else 0f)

        Mission(ml.group, goal, day.toInt(), priorityScore, "Maintain volume", chooseExercisesFor(ml.group, recentExerciseMap, 3), emptyList())
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
                        if (isSelected) Brush.radialGradient(listOf(theme.background, theme.tertiary.copy(alpha = 0.45f)))
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

    val goalPrefs = remember { SprintGoalPreferences(context) }
    var sprintGoal by remember { mutableStateOf(goalPrefs.load()) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var showHealth by remember { mutableStateOf(false) }

    val recomputeKey = remember(recentWorkouts, advicePayload, profile, sprintGoal) {
        "${recentWorkouts.size}-${profile.name}-${sprintGoal.focusMuscles.joinToString()}-${sprintGoal.isActive}".hashCode()
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
            sprintGoal = sprintGoal,
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
    var selectedMuscle by remember { mutableStateOf<MuscleLoad?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    if (computing) {
        LoadingScreen(progress = progress, modifier = modifier.fillMaxWidth())
        return
    }

    val now = Instant.ofEpochMilli(nowEpochMillis)
    val filtered = remember(loads, selected) { if (selected == BodyCategory.All) loads else loads.filter { categoryOf(it.group.name) == selected } }
    val sortedLoads = remember(filtered) { filtered.sortedByDescending { it.score } }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonModifier = Modifier
                .weight(1f)
                .height(45.dp)


            @Composable
            fun ButtonContent(icon: ImageVector, label: String) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenWeeklySummary()
                },
                modifier = buttonModifier.glow(theme.tertiary, radius = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.background,
                    contentColor = theme.primary
                ),
                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 4.dp) // Minimize padding to prevent early wrapping
            ) {
                ButtonContent(Icons.Outlined.FitnessCenter, "Weekly")
            }

            FilledTonalButton(
                onClick = { if (recoveryFactors != null) showHealth = true },
                modifier = buttonModifier.glow(theme.tertiary, radius = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.background,
                    contentColor = theme.primary
                ),
                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                ButtonContent(Icons.Outlined.MonitorHeart, "Recovery")
            }

            FilledTonalButton(
                onClick = { showGoalSheet = true },
                modifier = buttonModifier.glow(
                    if (sprintGoal.isActive) theme.primary else theme.tertiary,
                    radius = 8.dp
                ),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (sprintGoal.isActive) theme.background.copy(alpha = 0.2f) else theme.tertiary,
                    contentColor = theme.primary
                ),
                border = BorderStroke(
                    1.dp,
                    if (sprintGoal.isActive) theme.primary else theme.secondary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                ButtonContent(
                    Icons.Outlined.AutoAwesome,
                    if (sprintGoal.isActive) "Active" else "Goal"
                )
            }
        }

        if (recoveryFactors != null) RecoveryFactorRow(factors = recoveryFactors!!, onClick = { showHealth = true })
        OverviewRow(loads = filtered)
        CategoryFilterBar(selected = selected, onSelect = { selected = it; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) })

        FlowRow(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortedLoads.forEach { item ->
                Box(
                    modifier = Modifier.weight(1f, true).widthIn(min = 150.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedMuscle = item
                    }
                ) {
                    MuscleCircleTile(load = item)
                }
            }
            if (sortedLoads.size % 2 != 0) Spacer(Modifier.weight(1f).widthIn(min = 150.dp))
        }

        StimulantTrackerSection()
    }

    if (showHealth && recoveryFactors != null) RecoveryDetailSheet(state = sheetState, onDismiss = { showHealth = false }, factors = recoveryFactors!!)

    if (showGoalSheet) {
        GoalConfigurationSheet(
            current = sprintGoal,
            onSave = {
                goalPrefs.save(it)
                sprintGoal = it
                showGoalSheet = false
            },
            onDismiss = { showGoalSheet = false }
        )
    }

    selectedMuscle?.let { ml ->
        val (prev14, cur14) = remember { twoWindowGrowth(now, recentWorkouts) }
        val acute24 = remember { getAccumulatedStimulus(recentWorkouts, now, 1) }
        val lastTrainedMap = remember(recentWorkouts) {
            val m = mutableMapOf<MuscleGroups, Instant>()
            recentWorkouts.forEach { w -> w.exercises.forEach { e -> muscleMappings.forEach { (r, mp) -> if(r.containsMatchIn(e.lowercase())) mp.keys.forEach { m[it] = w.date } } } }
            m
        }
        val recentMap = remember { recentExercisesByMuscle(recentWorkouts, profile) }
        val insight = insightForMuscle(ml.group, now, lastTrainedMap, prev14, cur14, acute24, recoveryFactors!!)
        MuscleDetailSheet(sheetState, { selectedMuscle = null }, ml, insight, chooseExercisesFor(ml.group, recentMap, 3))
    }
}
@Composable
private fun RecoveryFactorRow(factors: RecoveryFactors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(2.dp))
        if (factors.sleepHours != null) FactorPill((factors.sleepMultiplier - 1f) * 100f, "Sleep", Icons.Outlined.NightlightRound)
        if (factors.restingHeartRate != null) FactorPill((factors.rhrMultiplier - 1f) * 100f, "RHR", Icons.Outlined.FavoriteBorder)
        if (factors.dailyCalories != null) FactorPill((factors.caloriesMultiplier - 1f) * 100f, "Burn", Icons.Outlined.LocalFireDepartment)
        FactorPill((factors.subjectiveMultiplier - 1f) * 100f, "Self-Check", Icons.Outlined.AutoAwesome)
        if (factors.proteinGrams != null) FactorPill((factors.proteinMultiplier - 1f) * 100f, "Protein", Icons.Outlined.Restaurant)
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun FactorPill(value: Float, label: String, icon: ImageVector) {
    val isPositive = value >= -0.1f
    val baseColor = if (isPositive) Color(0xFF00E676) else Color(0xFFFF3B30)

    val transition = rememberInfiniteTransition(label = "pillPulse")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Surface(
        color = baseColor.copy(if (!isPositive) alphaAnim else 0.1f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, baseColor.copy(0.2f))
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

    Box(
        modifier = modifier
            .padding(16.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(theme.secondary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = theme.primary,
                trackColor = theme.secondary.copy(alpha = 0.3f),
            )
            Text(
                "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.primary
            )
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
        color = theme.background,
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
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors


    val color = remember(load.band) {
        when (load.band) {
            LoadBand.Building -> Color(0xFF42A5F5)
            LoadBand.OnTrack -> Color(0xFF00E676)
            LoadBand.Recovering -> Color(0xFFAB47BC)
            else -> Color(0xFF9E9E9E)
        }
    }

    val pct = remember(load.weeklyProgress, load.weeklyTarget) {
        if (load.weeklyTarget > 0f)
            (load.weeklyProgress / load.weeklyTarget).coerceIn(0f, 1f)
        else 0f
    }

    Surface(
        color = theme.background,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(listOf(color.copy(0.2f), Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 6.dp.toPx()


                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                            radius = size.minDimension / 1.2f
                        )
                    )

                    drawCircle(
                        color = Color.White.copy(0.05f),
                        style = Stroke(width = strokeWidth)
                    )


                    val sweep = pct * 360f

                    rotate(degrees = -90f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to color.copy(alpha = 0.3f),
                                pct to color,
                                pct + 0.01f to Color.Transparent, // Hard stop to avoid bleed
                                1.0f to Color.Transparent
                            ),
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Text(
                    text = "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = load.group.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = load.band.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
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
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Recovery & Health Stats", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)

            HealthMetricRow(Icons.Outlined.NightlightRound, "Sleep Duration", factors.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "—", "Tracked via Health Connect", ((factors.sleepMultiplier - 0.8f) / 0.3f).coerceIn(0f, 1f))
            HealthMetricRow(Icons.Outlined.MonitorHeart, "Resting HR", factors.restingHeartRate?.toString() ?: "—", "Tracked via Health Connect", if (factors.restingHeartRate == null) 0.5f else (1f - ((factors.restingHeartRate - 40f) / 60f).coerceIn(0f, 1f)))
            HealthMetricRow(Icons.Outlined.Restaurant, "Protein Intake", factors.proteinGrams?.let { "%.0f g".format(it) } ?: "—", "Tracked via Health Connect", if (factors.proteinGrams == null) 0.5f else (factors.proteinGrams.toFloat() / 180f).coerceIn(0f, 1f))

            val hydrationScore = if(factors.survey.waterIntake.contains("3-4")) 1f else if(factors.survey.waterIntake.contains("1-2")) 0.6f else 0.3f
            HealthMetricRow(Icons.Outlined.LocalFireDepartment, "Hydration", factors.survey.waterIntake, "Survey Response", hydrationScore)

            val stressScore = when(factors.survey.stressLevel) { "Low / Relaxed" -> 1f; "Moderate" -> 0.6f; else -> 0.3f }
            HealthMetricRow(Icons.Outlined.AutoAwesome, "Stress Level", factors.survey.stressLevel, "Survey Response", stressScore)

            val qualityScore = if(factors.survey.sleepQuality.contains("Good") || factors.survey.sleepQuality.contains("7-8")) 1f else 0.4f
            HealthMetricRow(Icons.Outlined.NightlightRound, "Sleep Quality", factors.survey.sleepQuality, "Self-Reported", qualityScore)

            Spacer(Modifier.height(12.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GoalConfigurationSheet(
    current: SprintGoal,
    onSave: (SprintGoal) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMuscles by remember { mutableStateOf(current.focusMuscles) }
    var duration by remember { mutableStateOf(current.durationWeeks) }
    var active by remember { mutableStateOf(current.isActive) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.background
    ) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Custom Sprint Goal", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Switch(
                    checked = active,
                    onCheckedChange = { active = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primary, checkedTrackColor = theme.secondary)
                )
            }

            Text("Select Focus Muscles (Max 6)", style = MaterialTheme.typography.labelLarge, color = theme.primary)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleGroups.entries.forEach { muscle ->
                    val isSelected = selectedMuscles.contains(muscle)
                    Surface(
                        onClick = {
                            if (isSelected) selectedMuscles = selectedMuscles - muscle
                            else if (selectedMuscles.size < 6) selectedMuscles = selectedMuscles + muscle
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) theme.primary else theme.secondary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if(isSelected) theme.primary else theme.secondary.copy(0.5f))
                    ) {
                        Text(
                            muscle.name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (isSelected) theme.background else Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text("Duration: $duration Weeks", style = MaterialTheme.typography.labelLarge, color = theme.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 4, 6, 8, 12).forEach { weeks ->
                    val isSelected = duration == weeks
                    Surface(
                        onClick = { duration = weeks },
                        shape = CircleShape,
                        color = if (isSelected) theme.tertiary else Color.Transparent,
                        border = BorderStroke(1.dp, if(isSelected) theme.primary else theme.secondary)
                    ) {
                        Text(
                            "$weeks w",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) theme.primary else Color.White.copy(0.7f)
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = { onSave(SprintGoal(selectedMuscles, duration, active)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.primary, contentColor = theme.background)
            ) {
                Text("Apply Goal")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
@Composable
private fun HealthMetricRow(icon: ImageVector, label: String, value: String, subValue: String, pct: Float) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val animatedPct by animateFloatAsState(pct, tween(1000, easing = LinearEasing), label = "bar")

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
            LinearProgressIndicator(progress = { animatedPct }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).glow(theme.primary, 4.dp), color = theme.primary, trackColor = theme.tertiary)
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

class StimulantManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("forge_stimulants", Context.MODE_PRIVATE)
    private val CAFFEINE_LIMIT_BASE = 400
    private val PSEUDO_LIMIT_BASE = 240

    fun add(type: String, amount: Int) {
        val today = LocalDate.now().toEpochDay()
        val lastDay = prefs.getLong("${type}_last_day", 0)
        var current = prefs.getInt("${type}_today", 0)
        var avg = prefs.getFloat("${type}_avg", 0f)

        // Update Frequency
        val freqKey = "${type}_freq_$amount"
        val currentFreq = prefs.getInt(freqKey, 0)

        val editor = prefs.edit()
        editor.putInt(freqKey, currentFreq + 1)

        if (today != lastDay) {
            if (lastDay != 0L) {
                avg = if (avg == 0f) current.toFloat() else (avg * 0.7f) + (current * 0.3f)
            }
            current = 0
            editor.putLong("${type}_last_day", today).putFloat("${type}_avg", avg)
        }

        editor.putInt("${type}_today", current + amount).apply()
    }

    fun get(type: String): Triple<Int, Int, Float> {
        val today = LocalDate.now().toEpochDay()
        val lastDay = prefs.getLong("${type}_last_day", 0)
        val avg = prefs.getFloat("${type}_avg", 0f)

        if (today != lastDay) {
            return Triple(0, getLimit(type, avg), avg)
        }
        val current = prefs.getInt("${type}_today", 0)
        return Triple(current, getLimit(type, avg), avg)
    }

    fun getMostUsed(type: String): List<Int> {
        return prefs.all
            .filter { it.key.startsWith("${type}_freq_") }
            .mapNotNull { entry ->
                val dose = entry.key.removePrefix("${type}_freq_").toIntOrNull()
                val count = entry.value as? Int ?: 0
                if (dose != null) dose to count else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
            .ifEmpty { if (type == "caffeine") listOf(50, 100, 200) else listOf(30, 60, 120) }
    }

    private fun getLimit(type: String, avg: Float): Int {
        val base = if (type == "caffeine") CAFFEINE_LIMIT_BASE else PSEUDO_LIMIT_BASE
        return max(base, (avg * 1.1f).toInt())
    }
}

@Composable
fun StimulantTrackerSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    val manager = remember { StimulantManager(context) }

    var caffeineState by remember { mutableStateOf(manager.get("caffeine")) }
    var pseudoState by remember { mutableStateOf(manager.get("pseudo")) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(theme.primary.copy(0.2f), Color.Transparent)),
                RoundedCornerShape(28.dp)
            ),
        color = theme.background.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "STIMULANT LOG",
                style = MaterialTheme.typography.labelLarge,
                color = theme.primary.copy(alpha = 0.8f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Black
            )

            StimulantRow(
                label = "Caffeine",
                icon = Icons.Outlined.Bolt,
                current = caffeineState.first,
                limit = caffeineState.second,
                theme = theme,
                onAdd = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    manager.add("caffeine", it)
                    caffeineState = manager.get("caffeine")
                },
                mostUsed = manager.getMostUsed("caffeine")
            )

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.primary.copy(0.1f)))

            StimulantRow(
                label = "Pseudoephedrine",
                icon = Icons.Outlined.Healing,
                current = pseudoState.first,
                limit = pseudoState.second,
                theme = theme,
                onAdd = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    manager.add("pseudo", it)
                    pseudoState = manager.get("pseudo")
                },
                mostUsed = manager.getMostUsed("pseudo")
            )
        }
    }
}

@Composable
fun StimulantRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    current: Int,
    limit: Int,
    theme: ColorSchemeAppTheme,
    onAdd: (Int) -> Unit,
    mostUsed: List<Int>
) {
    var customDose by remember { mutableStateOf("") }
    val progress = (current.toFloat() / limit.toFloat()).coerceIn(0f, 1.2f)
    val overLimit = current > limit

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, null, tint = theme.primary, modifier = Modifier.size(18.dp))
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = if (overLimit) "LIMIT EXCEEDED" else "DAILY ALLOWANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overLimit) Color.Red else Color.White.copy(0.4f)
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = if (overLimit) Color.Red else theme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    ) {
                        append("$current")
                    }
                    withStyle(SpanStyle(color = Color.White.copy(0.4f), fontSize = 14.sp)) {
                        append(" / $limit mg")
                    }
                }
            )
        }

        // Custom Progress Bar matching MuscleCircleTile logic
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val trackColor = Color.White.copy(0.05f)
            val barBrush = Brush.horizontalGradient(
                listOf(theme.primary.copy(0.6f), theme.primary)
            )

            drawRoundRect(color = trackColor, size = size, cornerRadius = CornerRadius(4.dp.toPx()))
            drawRoundRect(
                brush = barBrush,
                size = Size(width = size.width * progress.coerceAtMost(1f), height = size.height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            if (progress > 1f) {
                drawRoundRect(
                    color = Color.Red.copy(0.5f),
                    size = Size(width = size.width * (progress - 1f).coerceAtMost(1f), height = size.height),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customDose,
                onValueChange = { if (it.all { c -> c.isDigit() }) customDose = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Custom mg", color = Color.White.copy(0.3f), style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
//                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.secondary.copy(0.5f),
                    focusedContainerColor = theme.secondary.copy(0.2f),
                    unfocusedContainerColor = theme.secondary.copy(0.1f),
                    cursorColor = theme.primary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )

            Surface(
                onClick = {
                    customDose.toIntOrNull()?.let { onAdd(it); customDose = "" }
                },
                shape = RoundedCornerShape(16.dp),
                color = theme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = theme.background, modifier = Modifier.size(24.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mostUsed.take(3).forEach { dose ->
                Surface(
                    onClick = { onAdd(dose) },
                    shape = RoundedCornerShape(12.dp),
                    color = theme.secondary.copy(0.3f),
                    border = BorderStroke(1.dp, theme.primary.copy(0.15f))
                ) {
                    Text(
                        text = "+${dose}mg",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(0.8f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
