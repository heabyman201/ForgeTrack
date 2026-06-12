package com.forgecompose.workouttracker.muscle

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
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.health.connect.client.records.HeartRateRecord as HcHeartRateRecord

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

// Tanaka et al. (2001): HRmax = 208 - 0.7 * age. More accurate than 220 - age.
private fun tanakaMaxHr(age: Int): Double = 208.0 - 0.7 * age

// Katch-McArdle: BMR = 370 + 21.6 * lean body mass (kg). More accurate than Mifflin-St Jeor
// when body composition is known because it removes adipose tissue (low metabolic activity).
private fun katchMcArdleBmr(weightKg: Int, bodyFatPct: Double?): Double {
    if (bodyFatPct == null || bodyFatPct <= 0.0 || bodyFatPct >= 60.0) return Double.NaN
    val leanMass = weightKg * (1.0 - bodyFatPct / 100.0)
    return 370.0 + 21.6 * leanMass
}

// Karvonen target HR = ((HRmax - RHR) * intensity) + RHR. We invert: given current HR, what is %HRR?
private fun heartRateReservePct(observedHr: Double, restingHr: Double, maxHr: Double): Double {
    val reserve = (maxHr - restingHr).coerceAtLeast(1.0)
    return ((observedHr - restingHr) / reserve).coerceIn(0.0, 1.2)
}

// Parses a water intake survey answer (e.g. "1-2 Liters", "3 Liters") into liters/day.
private fun parseWaterIntakeLiters(raw: String): Double {
    val nums = Regex("""\d+(?:\.\d+)?""").findAll(raw).map { it.value.toDouble() }.toList()
    if (nums.isEmpty()) return 1.5
    return nums.average()
}

// Physical Activity Level (FAO/WHO/UN consensus): sedentary 1.2, light 1.375, moderate 1.55,
// very active 1.725, extra active 1.9. Derived from session count + cardio preference.
private fun physicalActivityLevel(daysPerWeek: Int, cardioPref: String): Double {
    val base = when {
        daysPerWeek <= 1 -> 1.2
        daysPerWeek == 2 -> 1.375
        daysPerWeek in 3..4 -> 1.55
        daysPerWeek in 5..6 -> 1.725
        else -> 1.9
    }
    val cardioBoost = when {
        cardioPref.contains("HIIT", true) || cardioPref.contains("Daily", true) -> 0.075
        cardioPref.contains("Running", true) || cardioPref.contains("Cycling", true) -> 0.05
        cardioPref.contains("LISS", true) || cardioPref.contains("Walk", true) -> 0.025
        else -> 0.0
    }
    return (base + cardioBoost).coerceIn(1.2, 2.0)
}

// Renaissance Periodization style volume landmarks (sets/week). Per muscle:
// MV = maintenance, MEV = minimum effective, MAV = max adaptive (centered target), MRV = max recoverable.
private data class VolumeLandmarks(val mv: Float, val mev: Float, val mav: Float, val mrv: Float)
private fun volumeLandmarksFor(muscle: MuscleGroups): VolumeLandmarks = when (muscle) {
    MuscleGroups.Pecs       -> VolumeLandmarks(8f, 10f, 16f, 22f)
    MuscleGroups.Delts      -> VolumeLandmarks(8f, 12f, 18f, 26f)
    MuscleGroups.Biceps     -> VolumeLandmarks(6f, 8f, 14f, 20f)
    MuscleGroups.Triceps    -> VolumeLandmarks(6f, 8f, 14f, 20f)
    MuscleGroups.Lats       -> VolumeLandmarks(8f, 10f, 16f, 22f)
    MuscleGroups.Traps      -> VolumeLandmarks(4f, 8f, 14f, 20f)
    MuscleGroups.Abs        -> VolumeLandmarks(0f, 8f, 16f, 25f)
    MuscleGroups.Forearms   -> VolumeLandmarks(2f, 4f, 10f, 16f)
    MuscleGroups.Quads      -> VolumeLandmarks(8f, 10f, 16f, 20f)
    MuscleGroups.Hamstrings -> VolumeLandmarks(4f, 6f, 12f, 18f)
    MuscleGroups.Glutes     -> VolumeLandmarks(0f, 4f, 12f, 16f)
    MuscleGroups.Calves     -> VolumeLandmarks(6f, 8f, 14f, 22f)
    MuscleGroups.LowerBack  -> VolumeLandmarks(2f, 4f, 8f, 12f)
    MuscleGroups.UpperBack  -> VolumeLandmarks(8f, 10f, 18f, 25f)
}

// Epley estimated 1RM: 1RM = w * (1 + reps/30). Brzycki agrees within ~3% for reps <= 10.
private fun estimated1RM(weight: Double, reps: Int): Double {
    if (weight <= 0 || reps <= 0) return 0.0
    return weight * (1.0 + reps / 30.0)
}

// Banister TRIMP with sex-neutral weighting: TRIMP = duration * HRR_ratio * 0.64 * exp(1.92 * HRR_ratio)
private fun banisterTrimp(durationMin: Double, hrrRatio: Double): Double {
    val r = hrrRatio.coerceIn(0.0, 1.2)
    return durationMin * r * 0.64 * exp(1.92 * r)
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
    val survey: SurveyInsights,
    val recentEnvironmentStress: Boolean = false
) {
    // Adult RHR baseline: ~70 bpm at age 20, drifts up ~0.3 bpm/year, capped near 80.
    // For aerobically fit individuals subtract via fitness signals later.
    private fun getAgeAdjustedRHR(age: Int): Double =
        (60.0 + (age - 20).coerceAtLeast(0) * 0.25).coerceAtMost(82.0)

    // Best-available BMR: Katch-McArdle when body composition is known, else Mifflin-St Jeor.
    val effectiveBmr: Double by lazy {
        val km = katchMcArdleBmr(profile.weightKg, bodyFatPercentage)
        if (km.isFinite()) km else profile.bmr
    }

    // FFM (kg) used for protein target when BFP is known.
    private val freeFatMass: Double by lazy {
        if (bodyFatPercentage != null && bodyFatPercentage > 0.0 && bodyFatPercentage < 60.0)
            profile.weightKg * (1.0 - bodyFatPercentage / 100.0)
        else
            profile.weightKg * 0.82  // assume ~18% bf default
    }

    val recoveryEfficacy: Float by lazy {
        var baseScore = 1.0

        // ---- Sleep: smooth bell curve around 7.5h. Below 6h debt accumulates non-linearly.
        // Based on Walker (2017) cognitive/recovery curves; <5h ~ 60% capacity, 7.5h optimal.
        val s = sleepHours?.toDouble() ?: 7.5
        val sleepScore = run {
            // Asymmetric: steep penalty below 7h, plateau above 8h. Penalize > 10h slightly.
            val raw = when {
                s < 5.0 -> 0.55 + (s - 4.0) * 0.07          // 0.55 at 5h floor, 0.62 below
                s < 7.0 -> 0.62 + (s - 5.0) * 0.18          // 0.62 at 5h → 0.98 at 7h
                s <= 9.0 -> 1.05 + (1.0 - abs(s - 8.0)) * 0.05  // small bonus for 7-9h
                s <= 10.0 -> 1.05
                else -> (1.05 - (s - 10.0) * 0.05).coerceAtLeast(0.85)
            }
            // Subjective sleep quality from survey nudges +/- 5%.
            val qualityAdj = when {
                survey.sleepQuality.contains("9", true) || survey.sleepQuality.contains("Excellent", true) -> 1.05
                survey.sleepQuality.contains("5", true) || survey.sleepQuality.contains("Poor", true) -> 0.95
                else -> 1.0
            }
            (raw * qualityAdj).coerceIn(0.5, 1.15)
        }
        baseScore *= sleepScore

        // ---- Protein vs FFM-aware target. Cutting needs 2.0-2.4 g/kg FFM for muscle retention
        // (Helms et al 2014); maintenance/bulking 1.6-2.0 g/kg total weight (Morton et al 2018).
        val proteinTargetG = if (survey.dietStyle.contains("Cut", true))
            2.3 * freeFatMass
        else
            1.8 * profile.weightKg
        val p = proteinGrams ?: (proteinTargetG * 0.9)
        val pRatio = (p / proteinTargetG).coerceIn(0.5, 1.5)

        // ---- TDEE via Mifflin/Katch * activity-adjusted PAL.
        val daysPerWeek = survey.daysAvailable.filter { it.isDigit() }.toIntOrNull() ?: 3
        val pal = physicalActivityLevel(daysPerWeek, survey.cardioPreference)
        val phaseMult = when {
            survey.goal.contains("Bulk", true) || survey.dietStyle.contains("Bulk", true) -> 1.12
            survey.goal.contains("Cut", true) || survey.dietStyle.contains("Cut", true) -> 0.82
            else -> 1.0
        }
        val calsTarget = effectiveBmr * pal * phaseMult
        val c = dailyCalories ?: calsTarget
        val cRatio = (c / calsTarget).coerceIn(0.6, 1.5)

        // Combined nutrition modifier: protein dominates for repair, calories gate output.
        // Diminishing return on excess kcal (>110% of target gives only 1.5% per 10%).
        val nutritionMod = if (cRatio < 0.85)
            (0.78 + (cRatio - 0.6) * 0.5) * pRatio.pow(0.5)
        else
            (1.0 + (cRatio.coerceAtMost(1.1) - 1.0) * 0.15) * pRatio.pow(0.35)
        baseScore *= nutritionMod

        // ---- Hydration via 35 ml/kg target (EFSA / ACSM guidance).
        val hydrationTargetL = profile.weightKg * 0.035
        val hydrationActualL = parseWaterIntakeLiters(survey.waterIntake)
        val hydrationRatio = (hydrationActualL / hydrationTargetL).coerceIn(0.4, 1.4)
        val hydrationMod = if (hydrationRatio < 0.85) 0.92 + (hydrationRatio - 0.4) * 0.18 else 1.02
        baseScore *= hydrationMod

        // ---- Cardiovascular stress: HR reserve interpretation (Karvonen).
        // Elevated RHR vs personal baseline indicates incomplete autonomic recovery (HRV proxy).
        val hrMax = tanakaMaxHr(profile.age)
        val rhrTarget = getAgeAdjustedRHR(profile.age)
        val rhr = restingHeartRate?.toDouble() ?: rhrTarget
        // Compute deviation as fraction of HR reserve (more individualized than fixed bpm).
        val reserve = (hrMax - rhrTarget).coerceAtLeast(40.0)
        val rhrDeviationPct = ((rhr - rhrTarget) / reserve).coerceIn(-0.15, 0.4)
        val rhrStress = (1.05 - rhrDeviationPct * 0.6).coerceIn(0.78, 1.08)

        // SpO2: <90% sleep desat is clinically meaningful; 90-93 mild, 94-97 normal, >=98 optimal.
        val spo2 = sleepSpo2 ?: 97.5
        val spo2Mod = when {
            spo2 < 90.0 -> 0.82
            spo2 < 94.0 -> 0.93
            spo2 < 97.0 -> 0.99
            else -> 1.02
        }

        val reportedStress = when(survey.stressLevel) {
            "High" -> 0.85
            "Moderate" -> 0.98
            "Low / Relaxed" -> 1.05
            else -> 1.0
        }

        baseScore *= (rhrStress * spo2Mod * reportedStress)

        if (recentEnvironmentStress) baseScore *= 0.92

        // ---- Age-related recovery decline: ~0.5-1% per year past 30 (Pollock et al).
        val ageDampener = when {
            profile.age <= 30 -> 1.0
            profile.age <= 50 -> 1.0 - ((profile.age - 30) * 0.006)
            else -> 0.88 - ((profile.age - 50) * 0.009)
        }.coerceAtLeast(0.7)
        baseScore *= ageDampener

        // Creatine: ~3-5% recovery/work-capacity benefit per Kreider meta-analysis.
        if (survey.usesCreatine) baseScore *= 1.04

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

data class WorkoutSummary(
    val date: Instant,
    val name: String,
    val exercises: List<String> = emptyList(),
    val environment: String? = null,
    val sessionRpe: Int? = null,
    val fatigueLevel: Int? = null,
    val restPeriodSeconds: Int? = null,
    val durationMinutes: Float? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Double? = null,
    val distance: Double? = null,
    val heartRateAvg: Int? = null,
    val heartRateMax: Int? = null,
    val systemicDrainScore: Float? = null
)

data class MuscleLoad(
    val group: MuscleGroups,
    val weeklyProgress: Float,
    val weeklyTarget: Float,
    val band: LoadBand,
    val score: Float,
    val lastTrainedAgo: String? = null,
    val injuryRisk: Float = 0f,
    val adaptationScore: Float = 0f,
    val consistencyScore: Float = 0f,
    val developmentScore: Float = 0f
)

private fun buildGeminiSignalSnapshot(
    loads: List<MuscleLoad>,
    recoveryFactors: RecoveryFactors
): GeminiSignalSnapshot {
    val cautionBands = setOf(LoadBand.Recovering, LoadBand.Overreached, LoadBand.DeloadRecommended)
    val readinessBands = setOf(LoadBand.Building, LoadBand.OnTrack)
    val avgInjuryRisk = loads.map { it.injuryRisk }.average().toFloat()
    val avgScore = loads.map { it.score }.average().toFloat()

    return GeminiSignalSnapshot(
        capturedAtEpochMs = System.currentTimeMillis(),
        recoveryEfficacy = recoveryFactors.recoveryEfficacy,
        avgInjuryRisk = avgInjuryRisk,
        avgLoadScore = avgScore,
        highReadinessCount = loads.count { it.band in readinessBands },
        cautionCount = loads.count { it.band in cautionBands }
    )
}

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
    Regex("plank|crunch|sit-up|leg raise") to mapOf(MuscleGroups.Abs to 1.0f),
    // Imported Health Connect labels and common cardio activity names.
    Regex("strength training|weight training|resistance training|weightlifting|gym workout") to mapOf(
        MuscleGroups.Quads to 0.5f,
        MuscleGroups.Hamstrings to 0.4f,
        MuscleGroups.Glutes to 0.4f,
        MuscleGroups.Lats to 0.35f,
        MuscleGroups.UpperBack to 0.35f,
        MuscleGroups.Pecs to 0.35f,
        MuscleGroups.Delts to 0.3f,
        MuscleGroups.Triceps to 0.25f,
        MuscleGroups.Biceps to 0.25f,
        MuscleGroups.Abs to 0.2f
    ),
    Regex("running|jog|treadmill|sprint") to mapOf(
        MuscleGroups.Quads to 0.7f,
        MuscleGroups.Hamstrings to 0.55f,
        MuscleGroups.Glutes to 0.55f,
        MuscleGroups.Calves to 0.6f
    ),
    Regex("walking|walk|hiking|hike|stairs|stair") to mapOf(
        MuscleGroups.Quads to 0.45f,
        MuscleGroups.Glutes to 0.4f,
        MuscleGroups.Calves to 0.35f
    ),
    Regex("cycling|biking|bike|spinning") to mapOf(
        MuscleGroups.Quads to 0.7f,
        MuscleGroups.Hamstrings to 0.45f,
        MuscleGroups.Glutes to 0.45f,
        MuscleGroups.Calves to 0.25f
    ),
    Regex("cardio|aerobic|elliptical") to mapOf(
        MuscleGroups.Quads to 0.4f,
        MuscleGroups.Hamstrings to 0.3f,
        MuscleGroups.Glutes to 0.3f,
        MuscleGroups.Calves to 0.25f
    ),
    Regex("rowing|erg") to mapOf(
        MuscleGroups.UpperBack to 0.65f,
        MuscleGroups.Lats to 0.55f,
        MuscleGroups.Biceps to 0.3f,
        MuscleGroups.Quads to 0.35f
    ),
    Regex("swimming|swim") to mapOf(
        MuscleGroups.Delts to 0.55f,
        MuscleGroups.Lats to 0.5f,
        MuscleGroups.UpperBack to 0.45f,
        MuscleGroups.Triceps to 0.35f
    ),
    Regex("hiit|crossfit|functional|circuit|bootcamp") to mapOf(
        MuscleGroups.Quads to 0.45f,
        MuscleGroups.Hamstrings to 0.35f,
        MuscleGroups.Glutes to 0.35f,
        MuscleGroups.Pecs to 0.3f,
        MuscleGroups.Lats to 0.3f,
        MuscleGroups.Delts to 0.3f,
        MuscleGroups.Abs to 0.25f
    ),
    Regex("\\bworkout\\b|\\btraining session\\b") to mapOf(
        MuscleGroups.Quads to 0.25f,
        MuscleGroups.Hamstrings to 0.2f,
        MuscleGroups.Glutes to 0.2f,
        MuscleGroups.Pecs to 0.2f,
        MuscleGroups.Lats to 0.2f,
        MuscleGroups.Delts to 0.2f,
        MuscleGroups.Abs to 0.15f
    )
)

private data class SessionDerivedLoad(
    val volumeUnits: Float,
    val loadUnits: Float,
    val hrStrain: Float,
    val fatigueNorm: Float
)

private fun workoutTokens(workout: WorkoutSummary): List<String> {
    return if (workout.exercises.isNotEmpty()) workout.exercises else listOf(workout.name)
}

private fun estimateSessionDerivedLoad(workout: WorkoutSummary): SessionDerivedLoad {
    val sets = (workout.sets ?: 0).coerceAtLeast(0)
    val reps = (workout.reps ?: 0).coerceAtLeast(0)
    val weight = (workout.weight ?: 0.0).coerceAtLeast(0.0)
    val distance = (workout.distance ?: 0.0).coerceAtLeast(0.0)
    val durationMinutes = (workout.durationMinutes ?: 0f).coerceAtLeast(0f)

    // Tonnage in metric tonnes; %1RM intensity scales effective stimulus per Helms/Schoenfeld.
    // Below ~30% 1RM, hypertrophy stimulus drops sharply (Lasevicius 2018, Schoenfeld 2017).
    val effectiveReps = when {
        sets > 0 && reps > 0 -> (reps / sets).coerceAtLeast(1)
        reps > 0 -> reps
        else -> 0
    }
    val oneRm = estimated1RM(weight, effectiveReps)
    val intensityFraction = if (oneRm > 0.0 && weight > 0.0) (weight / oneRm).coerceIn(0.0, 1.0) else 0.0
    val tonnageRaw = ((sets * reps).toDouble() * weight / 1000.0).toFloat()
    // Effective volume curve: stimulus(intensity) = clamp(0.4 + 1.2*x - 0.6*x^2, 0.4, 1.0)
    // ~1.0 at 60-80% 1RM (hypertrophy zone), tapers slightly above (more CNS, less per rep).
    val intensityCurve = if (intensityFraction > 0.0) {
        (0.4 + 1.2 * intensityFraction - 0.6 * intensityFraction * intensityFraction)
            .coerceIn(0.4, 1.05).toFloat()
    } else 1.0f
    val tonnageUnits = tonnageRaw * intensityCurve

    // Endurance: ~0.9 kcal/kg/km running, normalized to 75kg ⇒ ~70kcal/km, scale by 2.5 to align units.
    // Distance-based stimulus.
    val enduranceUnits = (distance * 2.5).toFloat()
    val durationUnits = durationMinutes / 30f
    val fallbackUnits = if (tonnageUnits == 0f && enduranceUnits == 0f) {
        (((workout.sessionRpe ?: workout.fatigueLevel ?: 5).toFloat() / 10f) *
            (durationMinutes / 35f).coerceAtLeast(0.75f))
    } else {
        0f
    }
    val volumeUnits = (tonnageUnits + enduranceUnits + durationUnits + fallbackUnits)
        .coerceAtLeast(0.2f)

    // HR strain: prefer Banister TRIMP via heart-rate reserve. Falls back to linear.
    val hrAvg = (workout.heartRateAvg ?: 0).toFloat()
    val hrMax = (workout.heartRateMax ?: 0).toFloat()
    val hrStrain = run {
        val avg = hrAvg.toDouble()
        val peak = hrMax.toDouble()
        val rest = 60.0  // population RHR (no per-user RHR plumbed here)
        val hrCeiling = if (peak > 0.0) peak else 195.0
        val hrrAvg = if (avg > 0.0) heartRateReservePct(avg, rest, hrCeiling) else 0.0
        val hrrPeak = if (peak > 0.0) heartRateReservePct(peak, rest, hrCeiling) else 0.0
        if (avg > 0.0 && durationMinutes > 0f) {
            val trimp = banisterTrimp(durationMinutes.toDouble(), hrrAvg)
            // Normalize TRIMP so an hour at 70% HRR ≈ 1.0 strain.
            (trimp / 90.0).toFloat().coerceIn(0f, 1.3f)
        } else if (hrrAvg > 0.0 || hrrPeak > 0.0) {
            ((hrrAvg * 0.65 + hrrPeak * 0.35).toFloat()).coerceIn(0f, 1.3f)
        } else 0f
    }

    val rpeNorm = (((workout.sessionRpe ?: workout.fatigueLevel ?: 6) - 4).toFloat() / 6f)
        .coerceIn(0f, 1f)
    val fatigueNorm = (((workout.fatigueLevel ?: workout.sessionRpe ?: 6) - 3).toFloat() / 7f)
        .coerceIn(0f, 1f)
    val systemicNorm = ((workout.systemicDrainScore ?: 0f) / 100f).coerceIn(0f, 1f)

    // sRPE method (Foster 2001): session load ≈ duration * RPE. Combined with volume + HR strain.
    val sRpeLoad = if (durationMinutes > 0f) (durationMinutes / 10f) * (rpeNorm + 0.5f) else 0f

    val loadUnits = (
        volumeUnits * (1f + (rpeNorm * 0.45f) + (hrStrain * 0.35f)) +
            (sRpeLoad * 0.4f) +
            (systemicNorm * 4.5f) +
            (fatigueNorm * 2.5f)
        ).coerceIn(0.2f, 120f)

    return SessionDerivedLoad(
        volumeUnits = volumeUnits,
        loadUnits = loadUnits,
        hrStrain = hrStrain,
        fatigueNorm = fatigueNorm
    )
}

private fun getAccumulatedStimulus(recent: List<WorkoutSummary>, now: Instant, days: Long): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(days))
    val accumulator = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.forEach { workout ->
        val sessionLoad = estimateSessionDerivedLoad(workout)
        val tokens = workoutTokens(workout)
        val explicitSetEstimate = (workout.sets ?: 1).coerceAtLeast(1).toFloat()
        val repsPerSet = run {
            val setsValue = workout.sets
            val repsValue = workout.reps
            if (setsValue != null && repsValue != null && setsValue > 0 && repsValue > 0) {
                repsValue.toFloat() / setsValue.toFloat()
            } else {
                null
            }
        }
        val repsMultiplier = when {
            repsPerSet == null -> 1f
            repsPerSet >= 18f -> 1.12f
            repsPerSet >= 12f -> 1.06f
            repsPerSet <= 5f -> 0.95f
            else -> 1f
        }
        val densityMultiplier = when {
            workout.restPeriodSeconds == null -> 1f
            workout.restPeriodSeconds <= 75 -> 1.08f
            workout.restPeriodSeconds <= 120 -> 1.03f
            else -> 0.96f
        }
        val sessionScale =
            (sessionLoad.volumeUnits / 8f).coerceIn(0.75f, 1.45f) *
                (0.9f + ((sessionLoad.loadUnits / 14f).coerceIn(0f, 1.2f) * 0.25f)) *
                (0.92f + (sessionLoad.hrStrain * 0.22f)) *
                densityMultiplier

        tokens.forEach { t ->
            val rawSets = max(extractEffectiveSets(t), explicitSetEstimate)
            val normalizedStimulus = rawSets * repsMultiplier * sessionScale
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(t.lowercase())) {
                    impacts.forEach { (muscle, ratio) ->
                        accumulator[muscle] = accumulator.getValue(muscle) + (normalizedStimulus * ratio)
                    }
                }
            }
        }
    }
    return accumulator
}

private fun getAccumulatedLoad(recent: List<WorkoutSummary>, now: Instant, days: Long): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(days))
    val accumulator = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.forEach { workout ->
        val sessionLoad = estimateSessionDerivedLoad(workout)
        val tokens = workoutTokens(workout)
        val tokenCount = tokens.size.coerceAtLeast(1).toFloat()
        tokens.forEach { t ->
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(t.lowercase())) {
                    val loadShare = sessionLoad.loadUnits / tokenCount
                    impacts.forEach { (muscle, ratio) ->
                        accumulator[muscle] = accumulator.getValue(muscle) + (loadShare * ratio)
                    }
                }
            }
        }
    }
    return accumulator
}

// Williams et al. (2017): EWMA ACWR is more sensitive than rolling average to acute spikes
// because recent training is weighted exponentially. lambda = 2/(N+1) gives a half-life ~ 0.7N.
// Returns weekly-equivalent EWMA load by muscle.
private fun getEwmaStimulus(
    recent: List<WorkoutSummary>,
    now: Instant,
    halfLifeDays: Float
): Map<MuscleGroups, Float> {
    if (recent.isEmpty()) return emptyMap()
    val k = ln(2.0) / halfLifeDays.toDouble()
    val accumulator = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    val window = halfLifeDays * 4f  // ignore samples beyond 4 half-lives (<6.25% weight)
    val start = now.minus(Duration.ofDays(window.toLong().coerceAtLeast(7L)))
    recent.filter { it.date.isAfter(start) && !it.date.isAfter(now) }.forEach { workout ->
        val daysAgo = ChronoUnit.HOURS.between(workout.date, now).coerceAtLeast(0L) / 24.0
        val weight = exp(-k * daysAgo).toFloat()
        val sessionLoad = estimateSessionDerivedLoad(workout)
        val tokens = workoutTokens(workout)
        val tokenCount = tokens.size.coerceAtLeast(1).toFloat()
        tokens.forEach { t ->
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(t.lowercase())) {
                    val share = (sessionLoad.loadUnits / tokenCount) * weight
                    impacts.forEach { (muscle, ratio) ->
                        accumulator[muscle] = accumulator.getValue(muscle) + (share * ratio)
                    }
                }
            }
        }
    }
    // Normalize EWMA sum to weekly-equivalent so it lines up with rolling 7-day metrics.
    val normFactor = 7f / halfLifeDays  // sum of weights ≈ halfLifeDays / ln(2); scale to weekly view
    return accumulator.mapValues { (_, v) -> v * normFactor }
}

private fun getMuscleFrequencyPerWeek(recent: List<WorkoutSummary>, now: Instant, days: Long): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(days))
    val sessionsPerMuscle = mutableMapOf<MuscleGroups, Int>().withDefault { 0 }

    recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.forEach { workout ->
        val hits = mutableSetOf<MuscleGroups>()
        workoutTokens(workout).forEach { token ->
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(token.lowercase())) {
                    hits.addAll(impacts.keys)
                }
            }
        }
        hits.forEach { muscle ->
            sessionsPerMuscle[muscle] = sessionsPerMuscle.getValue(muscle) + 1
        }
    }

    val weeks = (days.toFloat() / 7f).coerceAtLeast(1f)
    return MuscleGroups.entries.associateWith { muscle ->
        (sessionsPerMuscle[muscle] ?: 0).toFloat() / weeks
    }
}

private fun averageHrStrainByMuscle(recent: List<WorkoutSummary>, now: Instant, days: Long): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(days))
    val sums = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    val counts = mutableMapOf<MuscleGroups, Int>().withDefault { 0 }

    recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.forEach { workout ->
        val sessionLoad = estimateSessionDerivedLoad(workout)
        val hits = mutableSetOf<MuscleGroups>()
        workoutTokens(workout).forEach { token ->
            muscleMappings.forEach { (regex, impacts) ->
                if (regex.containsMatchIn(token.lowercase())) {
                    hits.addAll(impacts.keys)
                }
            }
        }
        hits.forEach { muscle ->
            sums[muscle] = sums.getValue(muscle) + sessionLoad.hrStrain
            counts[muscle] = counts.getValue(muscle) + 1
        }
    }

    return MuscleGroups.entries.associateWith { muscle ->
        val count = counts[muscle] ?: 0
        if (count == 0) 0f else sums.getValue(muscle) / count
    }
}

private fun frequencyQualityMultiplier(muscle: MuscleGroups, frequencyPerWeek: Float): Float {
    val optimal = if (muscle.sizeModifier > 1.2f) 2.0f else 2.6f
    return when {
        frequencyPerWeek < 0.8f -> 0.72f + (frequencyPerWeek * 0.22f)
        frequencyPerWeek <= optimal -> 0.9f + ((frequencyPerWeek - 0.8f) / (optimal - 0.8f).coerceAtLeast(0.5f)) * 0.18f
        frequencyPerWeek <= 4.0f -> 1.08f - ((frequencyPerWeek - optimal) / (4.0f - optimal).coerceAtLeast(0.5f)) * 0.12f
        else -> 0.96f - ((frequencyPerWeek - 4.0f) * 0.08f)
    }.coerceIn(0.65f, 1.1f)
}

private data class MuscleHistoryProfile(
    val exposureCount: Int = 0,
    val distinctExercises: Int = 0,
    val consistentWeeks: Int = 0
)

private data class MuscleAdaptation(
    val score: Float = 0f,
    val goalMultiplier: Float = 1f,
    val fatigueMultiplier: Float = 1f,
    val decayAcceleration: Float = 1f
)

private fun normalizeExerciseToken(token: String): String = token
    .lowercase(Locale.US)
    .replace(Regex("""\b\d+\s*(sets?|reps?)\b"""), " ")
    .replace(Regex("""\brpe\s*\d+\b"""), " ")
    .replace(heavyRx, " ")
    .replace(lightRx, " ")
    .replace(Regex("""[^a-z\s-]"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()

private fun collectMuscleHistoryProfile(
    recent: List<WorkoutSummary>,
    now: Instant,
    days: Long = 42
): Map<MuscleGroups, MuscleHistoryProfile> {
    val start = now.minus(Duration.ofDays(days))
    val exposureCounts = mutableMapOf<MuscleGroups, Int>().withDefault { 0 }
    val exerciseCatalog = mutableMapOf<MuscleGroups, MutableSet<String>>()
    val weeklyExposure = mutableMapOf<MuscleGroups, MutableSet<Long>>()

    recent
        .filter { it.date.isAfter(start) && !it.date.isAfter(now) }
        .forEach { workout ->
            val weekBucket = (ChronoUnit.DAYS.between(start, workout.date).coerceAtLeast(0) / 7)
            val touchedThisWorkout = mutableSetOf<MuscleGroups>()

            workoutTokens(workout).forEach { token ->
                val normalized = normalizeExerciseToken(token)
                val tokenLower = token.lowercase(Locale.US)
                muscleMappings.forEach { (regex, impacts) ->
                    if (regex.containsMatchIn(tokenLower)) {
                        impacts.keys.forEach { muscle ->
                            exposureCounts[muscle] = exposureCounts.getValue(muscle) + 1
                            touchedThisWorkout += muscle
                            if (normalized.length > 2) {
                                exerciseCatalog.getOrPut(muscle) { mutableSetOf() }.add(normalized)
                            }
                        }
                    }
                }
            }

            touchedThisWorkout.forEach { muscle ->
                weeklyExposure.getOrPut(muscle) { mutableSetOf() }.add(weekBucket)
            }
        }

    return MuscleGroups.entries.associateWith { muscle ->
        MuscleHistoryProfile(
            exposureCount = exposureCounts.getOrDefault(muscle, 0),
            distinctExercises = exerciseCatalog[muscle]?.size ?: 0,
            consistentWeeks = weeklyExposure[muscle]?.size ?: 0
        )
    }
}

private fun calculateMuscleAdaptation(
    profile: UserProfile,
    survey: SurveyInsights,
    sprintGoal: SprintGoal,
    muscle: MuscleGroups,
    baseTarget: Float,
    history: MuscleHistoryProfile,
    frequencyPerWeek: Float,
    chronicWeeklyVolume: Float,
    chronicWeeklyLoad: Float
): MuscleAdaptation {
    val desiredFrequency = if (muscle.sizeModifier > 1.2f) 2.0f else 2.6f
    val normalizedVolume = (chronicWeeklyVolume / baseTarget.coerceAtLeast(6f)).coerceIn(0f, 1.25f)
    val normalizedLoad = (chronicWeeklyLoad / (baseTarget * 1.15f).coerceAtLeast(6f)).coerceIn(0f, 1.25f)
    val normalizedFrequency = (frequencyPerWeek / desiredFrequency).coerceIn(0f, 1.25f)
    val exposureScore = (history.exposureCount / 14f).coerceIn(0f, 1.25f)
    val varietyScore = (history.distinctExercises / 4f).coerceIn(0f, 1f)
    val consistencyScore = (history.consistentWeeks / 6f).coerceIn(0f, 1f)

    val experienceBase = when {
        profile.experience.contains("advanced") -> 0.16f
        profile.experience.contains("intermediate") || profile.experience.contains("year") -> 0.1f
        profile.experience.contains("start") || profile.experience.contains("beginner") -> 0.03f
        else -> 0.06f
    }
    val ageAdjustment = when {
        profile.age >= 50 -> -0.05f
        profile.age >= 40 -> -0.025f
        profile.age <= 25 -> 0.015f
        else -> 0f
    }
    val priorityBoost = if (
        muscle in profile.importantMuscles ||
        muscle in survey.focusMuscles ||
        (sprintGoal.isActive && muscle in sprintGoal.focusMuscles)
    ) {
        0.04f
    } else {
        0f
    }
    val styleBoost = when {
        profile.preferredStyle.contains("bodybuilding") || survey.goal.contains("Muscle", true) -> 0.04f
        profile.preferredStyle.contains("calisthenics") && muscle.sizeModifier <= 1.1f -> 0.035f
        profile.preferredStyle.contains("strength") && muscle.cnsImpact <= 0.5f -> 0.02f
        else -> 0f
    }

    val score = (
        experienceBase +
            ageAdjustment +
            priorityBoost +
            styleBoost +
            (normalizedVolume * 0.12f) +
            (normalizedLoad * 0.08f) +
            (normalizedFrequency * 0.1f) +
            (exposureScore * 0.07f) +
            (varietyScore * 0.05f) +
            (consistencyScore * 0.12f)
        ).coerceIn(0f, 0.55f)

    return MuscleAdaptation(
        score = score,
        goalMultiplier = (1f + (score * 0.28f)).coerceIn(1f, 1.18f),
        fatigueMultiplier = (1f - (score * 0.22f)).coerceIn(0.82f, 1f),
        decayAcceleration = (1f + (score * 0.32f)).coerceIn(1f, 1.2f)
    )
}

private fun estimateInjuryRisk(
    fatigueScore: Float,
    acuteWeeklyVolume: Float,
    chronicWeeklyVolume: Float,
    acuteWeeklyLoad: Float,
    chronicWeeklyLoad: Float,
    frequencyPerWeek: Float,
    desiredFrequency: Float,
    hrStrain: Float,
    recoveryFactors: RecoveryFactors
): Float {
    val fatigueNorm = (fatigueScore / 60f).coerceIn(0f, 1f)
    val volumeAcwr = if (chronicWeeklyVolume > 0.1f) acuteWeeklyVolume / chronicWeeklyVolume else 1f
    val loadAcwr = if (chronicWeeklyLoad > 0.1f) acuteWeeklyLoad / chronicWeeklyLoad else 1f
    // Coupled ACWR: take the worse of the two so a spike in either tonnage or HR-load registers.
    val combinedAcwr = max(volumeAcwr, loadAcwr).coerceIn(0.2f, 3f)
    // Hulin et al (2016) "sweet spot" 0.8-1.3, danger zone >=1.5 (~1.5x injury likelihood),
    // detraining penalty <0.8 (re-loading after layoff is itself injurious).
    val acwrRisk = when {
        combinedAcwr < 0.8f -> (0.10f + ((0.8f - combinedAcwr) * 0.45f)).coerceAtMost(0.45f)
        combinedAcwr <= 1.3f -> 0.05f  // sweet spot
        combinedAcwr <= 1.5f -> 0.10f + ((combinedAcwr - 1.3f) * 0.5f)
        combinedAcwr <= 2.0f -> 0.25f + ((combinedAcwr - 1.5f) * 0.7f)
        else -> 0.60f + ((combinedAcwr - 2.0f) * 0.5f)
    }.coerceIn(0f, 1f)

    val frequencyDelta = frequencyPerWeek - desiredFrequency
    val frequencyRisk = when {
        frequencyDelta < -1.2f -> 0.18f
        frequencyDelta <= 1f -> 0.04f
        else -> (0.1f + ((frequencyDelta - 1f) * 0.22f)).coerceAtMost(0.7f)
    }

    val recoveryPenalty = (1f - recoveryFactors.recoveryEfficacy).coerceIn(0f, 0.6f)
    // HR strain >0.85 HRR sustained correlates with overtraining markers.
    val hrRisk = ((hrStrain - 0.85f).coerceAtLeast(0f) * 0.5f).coerceAtMost(0.45f)

    // Logistic blend: heavier base on fatigue + ACWR, modulated by recovery.
    val rawRisk = (fatigueNorm * 0.32f) +
        (acwrRisk * 0.38f) +
        (frequencyRisk * 0.15f) +
        (recoveryPenalty * 0.2f) +
        hrRisk
    // Apply mild logistic squashing so risk grows fast in 0.4-0.7 range and saturates near 1.
    val logistic = 1f / (1f + exp((-3.2f * (rawRisk - 0.5f)).toDouble())).toFloat()
    val blended = rawRisk * 0.55f + logistic * 0.45f
    return blended.coerceIn(0f, 1f)
}

private fun calculateBaseTarget(profile: UserProfile, survey: SurveyInsights, muscle: MuscleGroups, goal: SprintGoal): Float {
    // Anchor the target between MEV (beginner) and MAV (advanced) per RP guidelines.
    val landmarks = volumeLandmarksFor(muscle)

    // Experience scales 0..1: beginner ~0, advanced ~1.
    val expFactor = when {
        profile.experience.contains("advanced") -> 1.0f
        profile.experience.contains("intermediate") || profile.experience.contains("year") -> 0.7f
        profile.experience.contains("start") || profile.experience.contains("beginner") -> 0.2f
        else -> 0.4f
    }
    // Beginner: ~MEV+10% of MAV gap. Advanced: ~MAV. Caps below MRV via clamp at end.
    var target = landmarks.mev * (1f - expFactor * 0.7f) +
        landmarks.mav * (expFactor * 0.7f + 0.1f)

    val daysAvailable = survey.daysAvailable.filter { it.isDigit() }.toIntOrNull() ?: 3
    // Per-session recoverable hard sets ~ 6-10 (Schoenfeld 2017 dose-response).
    val maxRecoverablePerSession = 9f
    val maxTheoreticalWeekly = daysAvailable * maxRecoverablePerSession
    target = target.coerceAtMost(maxTheoreticalWeekly)

    if (profile.preferredStyle == "calisthenics") target *= 1.15f
    if (survey.goal.contains("Strength", true)) target *= 0.85f
    if (survey.goal.contains("Endurance", true) && muscle in listOf(
            MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Calves, MuscleGroups.Glutes
        )) target *= 1.1f

    // Age-related recoverable volume decline (Doering 2016, Fragala 2019): ~1% per year past 40.
    if (profile.age > 40) {
        val decay = 1f - ((profile.age - 40) * 0.012f).coerceAtMost(0.35f)
        target *= decay
    }

    // Recovery self-assessment can shift target ±15%.
    target *= when {
        survey.recoverySelfAssessment.contains("Fast", true) -> 1.1f
        survey.recoverySelfAssessment.contains("Slow", true) -> 0.85f
        else -> 1.0f
    }

    // Stress chronically suppresses recoverable volume (Bartholomew 2008).
    if (survey.stressLevel == "High") target *= 0.9f

    // Sleep is a hard ceiling: <6h drops MRV materially.
    val sleepBracket = survey.sleepQuality
    if (sleepBracket.contains("5", true) || sleepBracket.contains("Poor", true)) target *= 0.88f

    // Priority and sprint focus push target up but never beyond 95% MRV.
    if (profile.importantMuscles.contains(muscle) || survey.focusMuscles.contains(muscle)) {
        target *= 1.18f
    }
    if (goal.isActive && goal.focusMuscles.contains(muscle)) {
        target *= 1.3f
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

    // Final clamp inside the recoverable range so we never prescribe junk volume or below MV.
    return target.coerceIn(landmarks.mv.coerceAtLeast(2f), landmarks.mrv * 0.95f)
}

private fun calculateFatigueState(
    muscle: MuscleGroups,
    lastTrained: Instant?,
    now: Instant,
    acuteVolume: Float,
    acuteLoad: Float,
    recoveryEfficacy: Float,
    systemicFatigue: Float,
    frequencyPerWeek: Float,
    hrStrain: Float,
    adaptation: MuscleAdaptation = MuscleAdaptation()
): Float {
    if (lastTrained == null) return 0f

    val hoursSince = ChronoUnit.HOURS.between(lastTrained, now).coerceAtLeast(0)
    val frequencyPressure = ((frequencyPerWeek - 2f) / 2.5f).coerceIn(-0.35f, 0.65f)
    // Larger muscles take longer to recover (Schoenfeld 2016: legs 48-72h, arms 24-48h).
    val baseHalfLife = 24f * muscle.localRecoverySpeed * (1f + (muscle.sizeModifier * 0.3f))
    val effectiveHalfLife = (
        baseHalfLife *
            (1.0f + (systemicFatigue * 0.6f) + (frequencyPressure * 0.45f) + (hrStrain * 0.35f))
        ) / (recoveryEfficacy.coerceAtLeast(0.35f) * adaptation.decayAcceleration.coerceAtLeast(1f))
    val clampedHalfLife = effectiveHalfLife.coerceAtLeast(6f)

    // Banister-style two-component model: fast fatigue (residual) + delayed soreness (DOMS).
    val initialFatigue = ((acuteVolume * 8f) + (acuteLoad * 1.8f)) *
        (0.9f + (muscle.cnsImpact * 0.55f) + (hrStrain * 0.25f)) *
        adaptation.fatigueMultiplier
    val residual = (initialFatigue * exp(-(ln(2.0) / clampedHalfLife) * hoursSince)).toFloat()

    // DOMS: peaks 24-48h post-exercise (Cheung 2003). Larger muscles peak later.
    // Repeated bout effect (Nosaka & Clarkson 1995): trained muscles can show 50-80% less DOMS.
    // adaptation.score (0..0.55) maps to RBE attenuation; fully adapted ≈ 0.55*1.4 = 0.77 reduction.
    val domsCurve = if (hoursSince < 96) {
        val peakTime = 30f + (muscle.sizeModifier * 12f)  // ~30-46h depending on muscle size
        // Asymmetric Gaussian: ramp ~12h, decay ~24h (post-peak fades faster than rise).
        val sigma = if (hoursSince < peakTime) 14f else 22f
        val rbeAttenuation = (1f - (adaptation.score * 1.4f)).coerceIn(0.25f, 1f)
        val domsHeight =
            ((acuteVolume * 4.5f) + (acuteLoad * 1.2f)) *
                (1f / recoveryEfficacy.coerceAtLeast(0.35f)) *
                adaptation.fatigueMultiplier *
                rbeAttenuation
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
    val experienceYears = parseExperienceToYears(profile.experience)
    val experienceNorm = (experienceYears / 6f).coerceIn(0f, 1f)

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

    val recentEnvStress = recent
        .filter { it.date.isAfter(now.minus(Duration.ofDays(3))) }
        .any { it.environment?.contains("Heat", true) == true || it.environment?.contains("40C", true) == true }

    val recoveryFactors = RecoveryFactors(
        sleepHours = lastNightSleepDuration,
        restingHeartRate = restingHr,
        sleepSpo2 = recentSpo2,
        proteinGrams = avgProtein,
        bodyFatPercentage = currentBfp,
        dailyCalories = avgCalories,
        profile = profile,
        survey = survey,
        recentEnvironmentStress = recentEnvStress
    )

    val weeklyVolume = getAccumulatedStimulus(recent, now, 7)
    val acute24h = getAccumulatedStimulus(recent, now, 1)
    val acute7d = getAccumulatedStimulus(recent, now, 7)
    val chronic28d = getAccumulatedStimulus(recent, now, 28)
    val acute24hLoad = getAccumulatedLoad(recent, now, 1)
    val acute7dLoad = getAccumulatedLoad(recent, now, 7)
    val chronic28dLoad = getAccumulatedLoad(recent, now, 28)
    // EWMA-based load: half-life 7 days for acute, 28 days for chronic. Used for ACWR.
    val acuteEwmaLoad = getEwmaStimulus(recent, now, halfLifeDays = 7f)
    val chronicEwmaLoad = getEwmaStimulus(recent, now, halfLifeDays = 28f)
    val muscleFrequency = getMuscleFrequencyPerWeek(recent, now, 14)
    val muscleHrStrain = averageHrStrainByMuscle(recent, now, 14)
    val muscleHistory = collectMuscleHistoryProfile(recent, now, 42)
    val weeklySessionFrequency = recent
        .count { it.date.isAfter(now.minus(Duration.ofDays(14))) && it.date.isBefore(now) }
        .toFloat() / 2f

    // Integrate session intensity/fatigue signals into systemic load calculation if available
    val recentSessionRpe = recent.filter { it.date.isAfter(now.minus(Duration.ofDays(1))) }.mapNotNull { it.sessionRpe }.average()
    val rpeModifier = if (!recentSessionRpe.isNaN() && recentSessionRpe > 8.0) 1.25f else 1.0f
    val recentFatigue = recent.filter { it.date.isAfter(now.minus(Duration.ofDays(1))) }.mapNotNull { it.fatigueLevel }.average()
    val fatigueModifier = when {
        recentFatigue.isNaN() -> 1.0f
        recentFatigue >= 8.0 -> 1.2f
        recentFatigue >= 6.0 -> 1.1f
        else -> 1.0f
    }
    val recentRestSeconds = recent
        .filter { it.date.isAfter(now.minus(Duration.ofDays(1))) }
        .mapNotNull { it.restPeriodSeconds?.toDouble() }
        .average()
    val densityModifier = if (!recentRestSeconds.isNaN() && recentRestSeconds in 1.0..75.0) 1.1f else 1.0f
    val recentHrStrain = recent
        .filter { it.date.isAfter(now.minus(Duration.ofDays(1))) }
        .map { estimateSessionDerivedLoad(it).hrStrain.toDouble() }
        .average()
    val hrModifier = when {
        recentHrStrain.isNaN() -> 1.0f
        recentHrStrain >= 1.0 -> 1.2f
        recentHrStrain >= 0.75 -> 1.1f
        else -> 1.0f
    }
    val frequencyModifier = when {
        weeklySessionFrequency >= 6f -> 1.25f
        weeklySessionFrequency >= 4.5f -> 1.15f
        weeklySessionFrequency <= 1.5f -> 0.92f
        else -> 1.0f
    }
    val recentSystemicDrain = recent
        .filter { it.date.isAfter(now.minus(Duration.ofDays(1))) }
        .mapNotNull { it.systemicDrainScore?.toDouble() }
        .average()
    val drainBoost = if (recentSystemicDrain.isNaN()) 0f else (recentSystemicDrain / 15.0).toFloat()

    val totalSystemicLoad = MuscleGroups.entries
        .sumOf { ((acute24hLoad[it] ?: (acute24h[it] ?: 0f)) * it.sizeModifier).toDouble() }
        .toFloat() * rpeModifier * fatigueModifier * densityModifier * hrModifier * frequencyModifier
    val systemicFatigueNorm = ((totalSystemicLoad / 35f) + drainBoost).coerceIn(0f, 1f)

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
        val frequencyPerWeek = muscleFrequency[muscle] ?: 0f
        val acuteWeeklyVolume = acute7d[muscle] ?: 0f
        val chronicWeeklyVolume = ((chronic28d[muscle] ?: acuteWeeklyVolume) / 4f).coerceAtLeast(0.1f)
        val acuteWeeklyLoad = acute7dLoad[muscle] ?: acuteWeeklyVolume
        val chronicWeeklyLoad = ((chronic28dLoad[muscle] ?: acuteWeeklyLoad) / 4f).coerceAtLeast(0.1f)
        val history = muscleHistory[muscle] ?: MuscleHistoryProfile()
        val adaptation = calculateMuscleAdaptation(
            profile = profile,
            survey = survey,
            sprintGoal = sprintGoal,
            muscle = muscle,
            baseTarget = baseTarget,
            history = history,
            frequencyPerWeek = frequencyPerWeek,
            chronicWeeklyVolume = chronicWeeklyVolume,
            chronicWeeklyLoad = chronicWeeklyLoad
        )
        val frequencyQuality = frequencyQualityMultiplier(muscle, frequencyPerWeek)
        val effectiveTarget =
            baseTarget * adaptation.goalMultiplier * recoveryFactors.recoveryEfficacy * frequencyQuality
        val currentVol = weeklyVolume[muscle] ?: 0f
        val acuteLoad = acute24hLoad[muscle] ?: (acute24h[muscle] ?: 0f)
        val hrStrain = muscleHrStrain[muscle] ?: 0f
        val lastDate = lastTrainedMap[muscle]

        val fatigueScore = calculateFatigueState(
            muscle, lastDate, now,
            acute24h[muscle] ?: 0f,
            acuteLoad,
            recoveryFactors.recoveryEfficacy,
            systemicFatigueNorm,
            frequencyPerWeek,
            hrStrain,
            adaptation
        )
        val desiredFrequency = if (muscle.sizeModifier > 1.2f) 2.0f else 2.6f
        // Prefer EWMA load for ACWR (Williams 2017): smoother, more spike-sensitive.
        val ewmaAcuteLoad = acuteEwmaLoad[muscle] ?: acuteWeeklyLoad
        val ewmaChronicLoad = (chronicEwmaLoad[muscle] ?: chronicWeeklyLoad).coerceAtLeast(0.1f)
        val injuryRisk = estimateInjuryRisk(
            fatigueScore = fatigueScore,
            acuteWeeklyVolume = acuteWeeklyVolume,
            chronicWeeklyVolume = chronicWeeklyVolume,
            acuteWeeklyLoad = ewmaAcuteLoad,
            chronicWeeklyLoad = ewmaChronicLoad,
            frequencyPerWeek = frequencyPerWeek,
            desiredFrequency = desiredFrequency,
            hrStrain = hrStrain,
            recoveryFactors = recoveryFactors
        )
        val volumeAcwr = acuteWeeklyVolume / chronicWeeklyVolume
        val loadAcwr = acuteWeeklyLoad / chronicWeeklyLoad

        val band = when {
            lastDate == null || ChronoUnit.DAYS.between(lastDate, now) > 14 -> LoadBand.NotTrained
            injuryRisk >= 0.8f || fatigueScore > 50f -> LoadBand.Overreached
            injuryRisk >= 0.62f || fatigueScore > 24f -> LoadBand.Recovering
            currentVol > effectiveTarget * 1.4f || volumeAcwr > 1.6f || loadAcwr > 1.6f -> LoadBand.DeloadRecommended
            currentVol >= effectiveTarget * 0.85f && injuryRisk < 0.45f -> LoadBand.OnTrack
            currentVol >= effectiveTarget * 0.3f -> LoadBand.Building
            else -> LoadBand.SlightlyTrained
        }
        val progressPct = (currentVol / effectiveTarget).coerceIn(0f, 1.5f)
        val fatigueNorm = (fatigueScore / 60f).coerceIn(0f, 1f)
        val consistencyScore = (
            ((history.consistentWeeks / 6f).coerceIn(0f, 1f) * 0.55f) +
                ((frequencyPerWeek / desiredFrequency).coerceIn(0f, 1f) * 0.45f)
            ).coerceIn(0f, 1f)
        val developmentScore = (
            ((adaptation.score / 0.55f).coerceIn(0f, 1f) * 0.45f) +
                (progressPct.coerceIn(0f, 1f) * 0.35f) +
                (consistencyScore * 0.15f) +
                (experienceNorm * 0.05f)
            ).coerceIn(0f, 1f)
        val sortingScore = (
            (progressPct * 0.65f) +
                ((1f - fatigueNorm) * 0.2f) +
                ((1f - injuryRisk) * 0.15f)
            ).coerceIn(0f, 1.5f)
        MuscleLoad(
            group = muscle,
            weeklyProgress = currentVol,
            weeklyTarget = effectiveTarget,
            band = band,
            score = sortingScore,
            lastTrainedAgo = lastDate?.let { friendlyAgo(now, it) },
            injuryRisk = injuryRisk,
            adaptationScore = adaptation.score,
            consistencyScore = consistencyScore,
            developmentScore = developmentScore
        )
    }
    loads to recoveryFactors
}

data class Mission(
    val group: MuscleGroups, val setsGoalToday: Int, val setsDoneToday: Int,
    val priorityScore: Float, val reason: String, val exercises: List<String>, val lifestyleHints: List<String>
)

data class MuscleInsight(
    val growthPct: Float, val growthLabel: String, val fatigue: Float,
    val readinessText: String, val suggestedRpeMin: Int, val suggestedRpeMax: Int, val recommendation: String,
    val injuryRiskPct: Int = 0
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
    acute24Load: Map<MuscleGroups, Float>, recovery: RecoveryFactors,
    weeklyFrequency: Float, hrStrain: Float, injuryRisk: Float,
    adaptation: MuscleAdaptation = MuscleAdaptation()
): MuscleInsight {
    val currentVol = cur14[m] ?: 0f
    val prevVol = prev14[m] ?: 0f
    val delta = if (prevVol == 0f) 100f else ((currentVol - prevVol) / prevVol) * 100f

    val systemicLoad = acute24Load.values.sum().coerceAtLeast(acute24.values.sum())
    val sysNorm = (systemicLoad / 45f).coerceIn(0f, 1f)

    val fatigueScore = calculateFatigueState(
        muscle = m,
        lastTrained = lastTrained[m],
        now = now,
        acuteVolume = acute24[m] ?: 0f,
        acuteLoad = acute24Load[m] ?: (acute24[m] ?: 0f),
        recoveryEfficacy = recovery.recoveryEfficacy,
        systemicFatigue = sysNorm,
        frequencyPerWeek = weeklyFrequency,
        hrStrain = hrStrain,
        adaptation = adaptation
    )
    val fatigueNorm = (fatigueScore / 60f).coerceIn(0f, 1f)
    val injuryRiskPct = (injuryRisk * 100f).toInt().coerceIn(0, 100)

    val readiness = when {
        injuryRisk > 0.75f || fatigueNorm > 0.8f -> "High Injury Risk"
        injuryRisk > 0.55f || fatigueNorm > 0.5f -> "Recovering"
        fatigueNorm > 0.2f -> "Ready"
        else -> "Fully Rested"
    }

    val rec = when {
        injuryRisk > 0.75f -> "Injury risk is elevated (~$injuryRiskPct%). Deload volume and prioritize recovery."
        fatigueNorm > 0.6f -> "High CNS load detected. Active recovery only."
        fatigueNorm > 0.4f || injuryRisk > 0.5f -> "Trainable, but keep RPE < 7 and avoid failure sets."
        delta < -15f && weeklyFrequency < 1.5f -> "Volume and frequency are both low. Add a second weekly session."
        delta < -15f -> "Volume dropping. Increase frequency or sets."
        delta > 20f -> "Great progress. Ensure calories match output."
        else -> "System primed for progressive overload."
    }

    val caution = max(fatigueNorm, injuryRisk)
    val suggestedRpeMin = when {
        caution > 0.65f -> 4
        caution > 0.45f -> 5
        else -> 7
    }
    val suggestedRpeMax = when {
        caution > 0.65f -> 6
        caution > 0.45f -> 7
        else -> 9
    }

    return MuscleInsight(
        delta, if (delta > 15f) "rising" else if (delta < -15f) "falling" else "stable",
        fatigueNorm, readiness, suggestedRpeMin, suggestedRpeMax, rec, injuryRiskPct
    )
}

private enum class BodyCategory { All, UpperBody, Arms, Core, LowerBody }
private fun prettyLoadBandName(band: LoadBand): String {
    return band.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
}

private fun categoryOf(name: String): BodyCategory = when (name.lowercase()) {
    "pecs", "delts", "lats", "traps", "upperback", "upper back" -> BodyCategory.UpperBody
    "biceps", "triceps", "forearms" -> BodyCategory.Arms
    "abs", "lowerback", "lower back" -> BodyCategory.Core
    "quads", "hamstrings", "glutes", "calves" -> BodyCategory.LowerBody
    else -> BodyCategory.UpperBody
}

private enum class HeatmapSide { Front, Back }

private data class HeatmapBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private data class HeatmapRegion(
    val id: String,
    val side: HeatmapSide,
    val muscles: List<MuscleGroups>,
    val contour: List<Offset>
)

private data class MuscleVisualState(
    val sizeBoost: Float = 0f,
    val definition: Float = 0f,
    val consistency: Float = 0f
)

private data class BodyShapeInputs(
    val weightKg: Float?,
    val bodyFatPercentage: Float?,
    val experienceYears: Float,
    val muscleStates: Map<MuscleGroups, MuscleVisualState>
)

private fun mirroredContour(contour: List<Offset>): List<Offset> = contour.map { Offset(1f - it.x, it.y) }

private val contourPecLeft = listOf(
    Offset(0.50f, 0.18f), Offset(0.34f, 0.18f), Offset(0.30f, 0.28f), Offset(0.35f, 0.34f), Offset(0.50f, 0.34f)
)
private val contourDeltoidLeft = listOf(
    Offset(0.34f, 0.18f), Offset(0.24f, 0.20f), Offset(0.20f, 0.24f), Offset(0.18f, 0.32f), Offset(0.21f, 0.35f), Offset(0.26f, 0.36f), Offset(0.30f, 0.28f)
)
private val contourBicepsLeft = listOf(
    Offset(0.30f, 0.28f), Offset(0.26f, 0.36f), Offset(0.21f, 0.35f), Offset(0.18f, 0.40f), Offset(0.16f, 0.48f), Offset(0.26f, 0.48f)
)
private val contourForearmLeft = listOf(
    Offset(0.16f, 0.48f), Offset(0.12f, 0.60f), Offset(0.10f, 0.70f), Offset(0.08f, 0.80f),
    Offset(0.11f, 0.84f), Offset(0.14f, 0.78f), Offset(0.16f, 0.76f), Offset(0.16f, 0.70f),
    Offset(0.22f, 0.60f), Offset(0.25f, 0.52f), Offset(0.26f, 0.48f)
)
private val contourAbs = listOf(
    Offset(0.35f, 0.34f), Offset(0.65f, 0.34f), Offset(0.63f, 0.44f), Offset(0.62f, 0.54f), Offset(0.50f, 0.56f), Offset(0.38f, 0.54f), Offset(0.37f, 0.44f)
)
private val contourQuadLeft = listOf(
    Offset(0.38f, 0.54f), Offset(0.34f, 0.65f), Offset(0.34f, 0.76f), Offset(0.48f, 0.76f), Offset(0.48f, 0.65f), Offset(0.50f, 0.56f)
)
private val contourCalfLeft = listOf(
    Offset(0.34f, 0.76f), Offset(0.32f, 0.85f), Offset(0.34f, 0.95f), Offset(0.32f, 1.02f),
    Offset(0.42f, 1.04f), Offset(0.46f, 1.00f), Offset(0.44f, 0.95f), Offset(0.46f, 0.85f), Offset(0.48f, 0.76f)
)

private val contourTraps = listOf(
    Offset(0.50f, 0.16f), Offset(0.34f, 0.17f), Offset(0.40f, 0.28f), Offset(0.50f, 0.32f), Offset(0.60f, 0.28f), Offset(0.66f, 0.17f)
)
private val contourUpperBack = listOf(
    Offset(0.50f, 0.32f), Offset(0.40f, 0.28f), Offset(0.28f, 0.28f), Offset(0.33f, 0.40f), Offset(0.50f, 0.42f), Offset(0.67f, 0.40f), Offset(0.72f, 0.28f), Offset(0.60f, 0.28f)
)
private val contourLatLeft = listOf(
    Offset(0.28f, 0.28f), Offset(0.33f, 0.34f), Offset(0.35f, 0.44f), Offset(0.36f, 0.54f), Offset(0.42f, 0.50f), Offset(0.33f, 0.40f)
)
private val contourLowerBack = listOf(
    Offset(0.50f, 0.42f), Offset(0.33f, 0.40f), Offset(0.42f, 0.50f), Offset(0.50f, 0.52f), Offset(0.58f, 0.50f), Offset(0.67f, 0.40f)
)
private val contourGluteLeft = listOf(
    Offset(0.50f, 0.52f), Offset(0.42f, 0.50f), Offset(0.36f, 0.54f), Offset(0.32f, 0.65f), Offset(0.46f, 0.65f), Offset(0.50f, 0.56f)
)
private val contourHamstringLeft = listOf(
    Offset(0.32f, 0.65f), Offset(0.32f, 0.76f), Offset(0.46f, 0.76f), Offset(0.46f, 0.65f)
)
private val contourCalfLeftBack = listOf(
    Offset(0.32f, 0.76f), Offset(0.30f, 0.85f), Offset(0.32f, 0.95f), Offset(0.30f, 1.02f),
    Offset(0.40f, 1.04f), Offset(0.44f, 1.00f), Offset(0.46f, 0.95f), Offset(0.44f, 0.85f), Offset(0.46f, 0.76f)
)
private val contourDeltoidLeftBack = listOf(
    Offset(0.34f, 0.17f), Offset(0.22f, 0.19f), Offset(0.18f, 0.24f), Offset(0.16f, 0.32f), Offset(0.19f, 0.35f), Offset(0.24f, 0.36f), Offset(0.28f, 0.28f)
)
private val contourTricepsLeft = listOf(
    Offset(0.28f, 0.28f), Offset(0.24f, 0.36f), Offset(0.19f, 0.35f), Offset(0.16f, 0.40f), Offset(0.14f, 0.48f), Offset(0.24f, 0.48f)
)
private val contourForearmLeftBack = listOf(
    Offset(0.14f, 0.48f), Offset(0.10f, 0.60f), Offset(0.08f, 0.70f), Offset(0.06f, 0.80f),
    Offset(0.09f, 0.84f), Offset(0.12f, 0.78f), Offset(0.15f, 0.76f), Offset(0.16f, 0.70f),
    Offset(0.20f, 0.60f), Offset(0.23f, 0.52f), Offset(0.24f, 0.48f)
)

private val frontHeatmapRegions = listOf(
    HeatmapRegion("pecs_l", HeatmapSide.Front, listOf(MuscleGroups.Pecs), contourPecLeft),
    HeatmapRegion("pecs_r", HeatmapSide.Front, listOf(MuscleGroups.Pecs), mirroredContour(contourPecLeft)),
    HeatmapRegion("delts_l", HeatmapSide.Front, listOf(MuscleGroups.Delts), contourDeltoidLeft),
    HeatmapRegion("delts_r", HeatmapSide.Front, listOf(MuscleGroups.Delts), mirroredContour(contourDeltoidLeft)),
    HeatmapRegion("biceps_l", HeatmapSide.Front, listOf(MuscleGroups.Biceps), contourBicepsLeft),
    HeatmapRegion("biceps_r", HeatmapSide.Front, listOf(MuscleGroups.Biceps), mirroredContour(contourBicepsLeft)),
    HeatmapRegion("forearms_l", HeatmapSide.Front, listOf(MuscleGroups.Forearms), contourForearmLeft),
    HeatmapRegion("forearms_r", HeatmapSide.Front, listOf(MuscleGroups.Forearms), mirroredContour(contourForearmLeft)),
    HeatmapRegion("abs", HeatmapSide.Front, listOf(MuscleGroups.Abs), contourAbs),
    HeatmapRegion("quads_l", HeatmapSide.Front, listOf(MuscleGroups.Quads), contourQuadLeft),
    HeatmapRegion("quads_r", HeatmapSide.Front, listOf(MuscleGroups.Quads), mirroredContour(contourQuadLeft)),
    HeatmapRegion("calves_l", HeatmapSide.Front, listOf(MuscleGroups.Calves), contourCalfLeft),
    HeatmapRegion("calves_r", HeatmapSide.Front, listOf(MuscleGroups.Calves), mirroredContour(contourCalfLeft))
)

private val backHeatmapRegions = listOf(
    HeatmapRegion("traps", HeatmapSide.Back, listOf(MuscleGroups.Traps), contourTraps),
    HeatmapRegion("upper_back", HeatmapSide.Back, listOf(MuscleGroups.UpperBack), contourUpperBack),
    HeatmapRegion("lats_l", HeatmapSide.Back, listOf(MuscleGroups.Lats), contourLatLeft),
    HeatmapRegion("lats_r", HeatmapSide.Back, listOf(MuscleGroups.Lats), mirroredContour(contourLatLeft)),
    HeatmapRegion("lower_back", HeatmapSide.Back, listOf(MuscleGroups.LowerBack), contourLowerBack),
    HeatmapRegion("glutes_l", HeatmapSide.Back, listOf(MuscleGroups.Glutes), contourGluteLeft),
    HeatmapRegion("glutes_r", HeatmapSide.Back, listOf(MuscleGroups.Glutes), mirroredContour(contourGluteLeft)),
    HeatmapRegion("hamstrings_l", HeatmapSide.Back, listOf(MuscleGroups.Hamstrings), contourHamstringLeft),
    HeatmapRegion("hamstrings_r", HeatmapSide.Back, listOf(MuscleGroups.Hamstrings), mirroredContour(contourHamstringLeft)),
    HeatmapRegion("calves_l", HeatmapSide.Back, listOf(MuscleGroups.Calves), contourCalfLeftBack),
    HeatmapRegion("calves_r", HeatmapSide.Back, listOf(MuscleGroups.Calves), mirroredContour(contourCalfLeftBack)),
    HeatmapRegion("triceps_l", HeatmapSide.Back, listOf(MuscleGroups.Triceps), contourTricepsLeft),
    HeatmapRegion("triceps_r", HeatmapSide.Back, listOf(MuscleGroups.Triceps), mirroredContour(contourTricepsLeft)),
    HeatmapRegion("delts_l", HeatmapSide.Back, listOf(MuscleGroups.Delts), contourDeltoidLeftBack),
    HeatmapRegion("delts_r", HeatmapSide.Back, listOf(MuscleGroups.Delts), mirroredContour(contourDeltoidLeftBack)),
    HeatmapRegion("forearms_l", HeatmapSide.Back, listOf(MuscleGroups.Forearms), contourForearmLeftBack),
    HeatmapRegion("forearms_r", HeatmapSide.Back, listOf(MuscleGroups.Forearms), mirroredContour(contourForearmLeftBack))
)

private fun regionsForSide(side: HeatmapSide): List<HeatmapRegion> = when (side) {
    HeatmapSide.Front -> frontHeatmapRegions
    HeatmapSide.Back -> backHeatmapRegions
}

private fun heatIntensity(load: MuscleLoad): Float {
    val volumeRatio = if (load.weeklyTarget > 0f) (load.weeklyProgress / load.weeklyTarget) else 0f
    val bandPenalty = when (load.band) {
        LoadBand.Overreached -> 0.35f
        LoadBand.DeloadRecommended -> 0.25f
        LoadBand.Recovering -> 0.12f
        LoadBand.OnTrack -> 0.08f
        LoadBand.Building -> 0.15f
        else -> 0f
    }
    return (volumeRatio * 0.72f + bandPenalty + load.injuryRisk * 0.35f).coerceIn(0f, 1f)
}

private val bodySilhouetteBase = Color(0xFF1D1F24)
private val bodySilhouetteStroke = Color(0xFF444854)
private val bodyRegionBase = Color(0xFF292B31)
private val heatBlue = Color(0xFF0A52FF)
private val heatPurple = Color(0xFF8C2CFF)
private val heatOrange = Color(0xFFFF8A1F)
private val heatRed = Color(0xFFFF3B30)
private val stressOrange = Color(0xFFFF9A2F)
private val stressRed = Color(0xFFFF2D20)

private fun lerpColor(start: Color, end: Color, amount: Float): Color =
    androidx.compose.ui.graphics.lerp(start, end, amount.coerceIn(0f, 1f))

private fun heatColor(intensity: Float): Color {
    val clamped = intensity.coerceIn(0f, 1f)
    return when {
        clamped <= 0.33f -> lerpColor(heatBlue, heatPurple, clamped / 0.33f)
        clamped <= 0.68f -> lerpColor(heatPurple, heatOrange, (clamped - 0.33f) / 0.35f)
        else -> lerpColor(heatOrange, heatRed, (clamped - 0.68f) / 0.32f)
    }
}

private fun fatigueGlowIntensity(load: MuscleLoad): Float {
    val bandStress = when (load.band) {
        LoadBand.Recovering -> 0.38f
        LoadBand.Overreached -> 0.72f
        LoadBand.DeloadRecommended -> 0.88f
        LoadBand.OnTrack -> 0.08f
        LoadBand.Building -> 0.12f
        else -> 0f
    }
    val rawStress = max(
        bandStress,
        max(
            load.injuryRisk * 0.95f,
            ((heatIntensity(load) - 0.58f) * 1.45f).coerceAtLeast(0f)
        )
    )
    return ((rawStress - 0.12f) / 0.88f).coerceIn(0f, 1f)
}

private fun fatigueGlowColor(load: MuscleLoad): Color {
    val heatBias = ((heatIntensity(load) * 0.4f) + (fatigueGlowIntensity(load) * 0.6f)).coerceIn(0f, 1f)
    return lerpColor(stressOrange, stressRed, heatBias)
}

private fun quietHeatFill(load: MuscleLoad?): Color {
    if (load == null) return bodyRegionBase
    val intensity = heatIntensity(load)
    val palette = heatColor(intensity)
    val blend = (0.16f + intensity * 0.62f).coerceIn(0.16f, 0.78f)
    val boosted = lerpColor(palette, Color.White, (intensity - 0.72f).coerceAtLeast(0f) * 0.18f)
    return lerpColor(bodyRegionBase, boosted, blend)
}

private fun bodyFrame(size: Size): Rect {
    val width = size.width * 0.66f
    val height = size.height * 0.96f
    val left = (size.width - width) / 2f
    val top = (size.height - height) / 2f
    return Rect(left = left, top = top, right = left + width, bottom = top + height)
}

private fun influence(y: Float, center: Float, radius: Float): Float {
    val distance = abs(y - center)
    return (1f - distance / radius).coerceIn(0f, 1f)
}

private fun bilateralInfluence(point: Offset, centerX: Float, centerY: Float, radiusX: Float, radiusY: Float): Float {
    val leftDx = (point.x - centerX) / radiusX
    val rightDx = (point.x - (1f - centerX)) / radiusX
    val dy = (point.y - centerY) / radiusY
    val left = (1f - sqrt((leftDx * leftDx) + (dy * dy))).coerceIn(0f, 1f)
    val right = (1f - sqrt((rightDx * rightDx) + (dy * dy))).coerceIn(0f, 1f)
    return max(left, right)
}

private fun midlineInfluence(point: Offset, centerY: Float, radiusX: Float, radiusY: Float): Float {
    val dx = (point.x - 0.5f) / radiusX
    val dy = (point.y - centerY) / radiusY
    return (1f - sqrt((dx * dx) + (dy * dy))).coerceIn(0f, 1f)
}

private fun regionCentroid(contour: List<Offset>): Offset {
    if (contour.isEmpty()) return Offset(0.5f, 0.5f)
    val inv = 1f / contour.size.toFloat()
    return Offset(
        x = contour.sumOf { it.x.toDouble() }.toFloat() * inv,
        y = contour.sumOf { it.y.toDouble() }.toFloat() * inv
    )
}

private fun regionVisualState(region: HeatmapRegion, bodyShape: BodyShapeInputs): MuscleVisualState =
    region.muscles
        .mapNotNull { bodyShape.muscleStates[it] }
        .maxByOrNull { it.sizeBoost + (it.definition * 0.35f) }
        ?: MuscleVisualState()

private fun buildMuscleVisualStates(
    loads: List<MuscleLoad>,
    bodyFatPercentage: Float?,
    experienceYears: Float
): Map<MuscleGroups, MuscleVisualState> {
    val experienceNorm = (experienceYears / 6f).coerceIn(0f, 1f)
    val bodyFatPenalty = bodyFatPercentage
        ?.let { ((it - 14f) / 20f).coerceIn(0f, 1f) * 0.22f }
        ?: 0f

    return loads.associate { load ->
        val adaptationNorm = (load.adaptationScore / 0.55f).coerceIn(0f, 1f)
        val heat = heatIntensity(load)
        val sizeBoost = (
                load.developmentScore * 0.62f +
                        load.consistencyScore * 0.22f +
                        adaptationNorm * 0.16f
                ).coerceIn(0f, 1f)
        val definition = (
                experienceNorm * 0.34f +
                        load.consistencyScore * 0.26f +
                        adaptationNorm * 0.2f +
                        heat * 0.2f -
                        bodyFatPenalty
                ).coerceIn(0f, 1f)

        load.group to MuscleVisualState(
            sizeBoost = sizeBoost,
            definition = definition,
            consistency = load.consistencyScore
        )
    }
}

private fun localEvolutionScale(point: Offset, side: HeatmapSide, inputs: BodyShapeInputs): Float {
    fun state(muscle: MuscleGroups) = inputs.muscleStates[muscle] ?: MuscleVisualState()
    var delta = 0f

    if (side == HeatmapSide.Front) {
        delta += bilateralInfluence(point, 0.27f, 0.26f, 0.12f, 0.11f) * state(MuscleGroups.Delts).sizeBoost * 0.045f
        delta += bilateralInfluence(point, 0.35f, 0.28f, 0.15f, 0.12f) * state(MuscleGroups.Pecs).sizeBoost * 0.04f
        delta += bilateralInfluence(point, 0.22f, 0.41f, 0.08f, 0.14f) * state(MuscleGroups.Biceps).sizeBoost * 0.045f
        delta += bilateralInfluence(point, 0.15f, 0.61f, 0.07f, 0.18f) * state(MuscleGroups.Forearms).sizeBoost * 0.03f
        delta += midlineInfluence(point, 0.46f, 0.15f, 0.18f) * state(MuscleGroups.Abs).sizeBoost * 0.02f
        delta += bilateralInfluence(point, 0.41f, 0.67f, 0.10f, 0.16f) * state(MuscleGroups.Quads).sizeBoost * 0.05f
        delta += bilateralInfluence(point, 0.39f, 0.91f, 0.08f, 0.13f) * state(MuscleGroups.Calves).sizeBoost * 0.032f
    } else {
        delta += midlineInfluence(point, 0.21f, 0.17f, 0.1f) * state(MuscleGroups.Traps).sizeBoost * 0.03f
        delta += midlineInfluence(point, 0.36f, 0.23f, 0.16f) * state(MuscleGroups.UpperBack).sizeBoost * 0.028f
        delta += bilateralInfluence(point, 0.33f, 0.39f, 0.14f, 0.18f) * state(MuscleGroups.Lats).sizeBoost * 0.05f
        delta += bilateralInfluence(point, 0.21f, 0.41f, 0.08f, 0.14f) * state(MuscleGroups.Triceps).sizeBoost * 0.04f
        delta += bilateralInfluence(point, 0.14f, 0.61f, 0.06f, 0.18f) * state(MuscleGroups.Forearms).sizeBoost * 0.026f
        delta += bilateralInfluence(point, 0.40f, 0.58f, 0.12f, 0.11f) * state(MuscleGroups.Glutes).sizeBoost * 0.045f
        delta += bilateralInfluence(point, 0.39f, 0.72f, 0.09f, 0.16f) * state(MuscleGroups.Hamstrings).sizeBoost * 0.04f
        delta += bilateralInfluence(point, 0.38f, 0.91f, 0.08f, 0.13f) * state(MuscleGroups.Calves).sizeBoost * 0.032f
    }

    return delta
}

private fun morphBodyPoint(point: Offset, inputs: BodyShapeInputs, side: HeatmapSide): Offset {
    val weightNorm = (((inputs.weightKg ?: 75f) - 75f) / 45f).coerceIn(-1f, 1f)
    val bodyFatNorm = inputs.bodyFatPercentage
        ?.let { ((it - 18f) / 18f).coerceIn(-1f, 1.1f) }
        ?: 0f
    val softness = (bodyFatNorm * 0.7f + weightNorm * 0.35f).coerceIn(-1f, 1.2f)
    val muscularity = (weightNorm - bodyFatNorm * 0.45f).coerceIn(-1f, 1f)
    val experienceNorm = (inputs.experienceYears / 6f).coerceIn(0f, 1f)

    val y = point.y
    val dx = point.x - 0.5f
    val absDx = abs(dx)

    val torsoZone = (1f - (absDx / 0.22f)).coerceIn(0f, 1f)
    val armZone = ((absDx - 0.18f) / 0.18f).coerceIn(0f, 1f)
    val legZone = if (y > 0.54f) (1f - (absDx / 0.18f)).coerceIn(0f, 1f) else 0f

    var scale = 1f + 0.025f * weightNorm
    scale += influence(y, 0.24f, 0.14f) * torsoZone * (0.035f * muscularity + 0.01f * softness)
    scale += influence(y, 0.40f, 0.16f) * torsoZone * (0.02f * muscularity + 0.025f * softness)
    scale += influence(y, 0.55f, 0.18f) * torsoZone * (0.055f * softness - 0.018f * bodyFatNorm.coerceAtMost(0f))
    scale += influence(y, 0.66f, 0.16f) * legZone * (0.03f * softness + 0.028f * weightNorm)
    scale += influence(y, 0.82f, 0.18f) * legZone * (0.04f * weightNorm + 0.015f * softness)
    scale += influence(y, 0.60f, 0.30f) * armZone * (0.025f * weightNorm + 0.03f * softness)
    scale += influence(y, 0.24f, 0.24f) * torsoZone * (0.012f * experienceNorm)
    scale += localEvolutionScale(point, side, inputs)

    val extremityDamp = when {
        y < 0.14f -> 0.2f
        y > 0.98f -> 0.55f
        else -> 1f
    }
    val adjustedScale = 1f + (scale - 1f) * extremityDamp
    return Offset(x = 0.5f + dx * adjustedScale, y = y)
}

private fun contourPointsInFrame(frame: Rect, region: HeatmapRegion, bodyShape: BodyShapeInputs): List<Offset> {
    val centroid = regionCentroid(region.contour)
    val state = regionVisualState(region, bodyShape)
    val regionExpansion = 1f + (state.sizeBoost * 0.07f) + (state.consistency * 0.02f)

    return region.contour.map { point ->
        val expanded = Offset(
            x = centroid.x + (point.x - centroid.x) * regionExpansion,
            y = centroid.y + (point.y - centroid.y) * regionExpansion
        )
        val morphed = morphBodyPoint(expanded, bodyShape, region.side)
        Offset(
            x = frame.left + frame.width * morphed.x,
            y = frame.top + frame.height * morphed.y
        )
    }
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        val intersects = (pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / ((pj.y - pi.y).let { if (it == 0f) 0.00001f else it }) + pi.x
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

private fun buildPolygonPath(points: List<Offset>): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }
    path.close()
    return path
}

private val frontLeftPoints = listOf(
    // ── Head (rounded skull) ──
    Offset(0.500f, 0.018f), Offset(0.482f, 0.012f), Offset(0.462f, 0.014f),
    Offset(0.446f, 0.024f), Offset(0.434f, 0.040f), Offset(0.427f, 0.060f),
    Offset(0.424f, 0.080f), Offset(0.428f, 0.096f), Offset(0.436f, 0.108f),
    Offset(0.448f, 0.118f),
    // ── Neck ──
    Offset(0.454f, 0.133f), Offset(0.458f, 0.149f), Offset(0.450f, 0.163f),
    // ── Trapezius → Deltoid cap (smooth curve) ──
    Offset(0.422f, 0.169f), Offset(0.385f, 0.175f), Offset(0.345f, 0.184f),
    Offset(0.305f, 0.195f), Offset(0.272f, 0.209f), Offset(0.250f, 0.224f),
    // ── Outer upper arm (bicep bulge) ──
    Offset(0.237f, 0.246f), Offset(0.228f, 0.272f), Offset(0.223f, 0.300f),
    Offset(0.220f, 0.328f), Offset(0.218f, 0.350f),
    // ── Outer elbow ──
    Offset(0.215f, 0.368f), Offset(0.212f, 0.384f),
    // ── Outer forearm (brachioradialis taper) ──
    Offset(0.203f, 0.406f), Offset(0.192f, 0.434f), Offset(0.180f, 0.464f),
    Offset(0.167f, 0.496f), Offset(0.154f, 0.528f), Offset(0.142f, 0.556f),
    // ── Wrist ──
    Offset(0.133f, 0.578f), Offset(0.126f, 0.598f),
    // ── Hand ──
    Offset(0.119f, 0.622f), Offset(0.113f, 0.644f), Offset(0.109f, 0.664f),
    Offset(0.107f, 0.682f),
    // ── Fingertips (round bottom) ──
    Offset(0.108f, 0.698f), Offset(0.115f, 0.710f), Offset(0.127f, 0.708f),
    Offset(0.137f, 0.694f),
    // ── Inner hand / wrist ascending ──
    Offset(0.145f, 0.670f), Offset(0.151f, 0.646f), Offset(0.157f, 0.620f),
    Offset(0.165f, 0.590f), Offset(0.174f, 0.558f),
    // ── Inner forearm ascending ──
    Offset(0.187f, 0.522f), Offset(0.200f, 0.488f), Offset(0.214f, 0.454f),
    Offset(0.227f, 0.424f),
    // ── Inner elbow / inner upper arm ──
    Offset(0.241f, 0.394f), Offset(0.253f, 0.364f), Offset(0.262f, 0.334f),
    Offset(0.269f, 0.304f), Offset(0.274f, 0.278f),
    // ── Armpit → chest side ──
    Offset(0.280f, 0.256f), Offset(0.296f, 0.242f), Offset(0.318f, 0.253f),
    Offset(0.338f, 0.275f),
    // ── Pec lower contour ──
    Offset(0.352f, 0.302f), Offset(0.358f, 0.330f),
    // ── Obliques / waist taper ──
    Offset(0.360f, 0.360f), Offset(0.356f, 0.390f), Offset(0.349f, 0.418f),
    Offset(0.343f, 0.444f),
    // ── Iliac / hip flare ──
    Offset(0.346f, 0.470f), Offset(0.354f, 0.496f), Offset(0.366f, 0.520f),
    Offset(0.380f, 0.540f), Offset(0.386f, 0.556f),
    // ── Outer quad (vastus lateralis bulge) ──
    Offset(0.382f, 0.576f), Offset(0.372f, 0.604f), Offset(0.361f, 0.634f),
    Offset(0.351f, 0.664f), Offset(0.343f, 0.696f), Offset(0.337f, 0.726f),
    // ── Knee ──
    Offset(0.333f, 0.750f), Offset(0.331f, 0.772f), Offset(0.333f, 0.792f),
    // ── Calf (gastrocnemius) ──
    Offset(0.339f, 0.816f), Offset(0.343f, 0.840f), Offset(0.341f, 0.866f),
    Offset(0.335f, 0.893f),
    // ── Ankle / Achilles ──
    Offset(0.327f, 0.920f), Offset(0.321f, 0.946f),
    // ── Foot (anatomic arch + toes) ──
    Offset(0.317f, 0.970f), Offset(0.318f, 0.993f), Offset(0.327f, 1.013f),
    Offset(0.346f, 1.027f), Offset(0.374f, 1.035f), Offset(0.405f, 1.039f),
    Offset(0.430f, 1.037f), Offset(0.447f, 1.027f),
    // ── Inner ankle ──
    Offset(0.457f, 1.005f), Offset(0.462f, 0.978f), Offset(0.464f, 0.950f),
    // ── Inner calf (soleus) ──
    Offset(0.466f, 0.920f), Offset(0.468f, 0.888f), Offset(0.470f, 0.858f),
    Offset(0.472f, 0.830f),
    // ── Inner knee ──
    Offset(0.474f, 0.802f), Offset(0.476f, 0.775f), Offset(0.478f, 0.750f),
    // ── Inner thigh (adductors / VMO) ──
    Offset(0.482f, 0.720f), Offset(0.486f, 0.688f), Offset(0.490f, 0.654f),
    Offset(0.494f, 0.620f), Offset(0.496f, 0.590f),
    // ── Crotch ──
    Offset(0.498f, 0.567f), Offset(0.500f, 0.555f)
)

private val backLeftPoints = listOf(
    // ── Head ──
    Offset(0.500f, 0.018f), Offset(0.482f, 0.012f), Offset(0.462f, 0.014f),
    Offset(0.446f, 0.024f), Offset(0.434f, 0.040f), Offset(0.427f, 0.060f),
    Offset(0.424f, 0.080f), Offset(0.428f, 0.096f), Offset(0.436f, 0.108f),
    Offset(0.448f, 0.118f),
    // ── Neck (thicker for traps view) ──
    Offset(0.452f, 0.132f), Offset(0.454f, 0.148f), Offset(0.447f, 0.162f),
    // ── Trapezius → Deltoid ──
    Offset(0.418f, 0.168f), Offset(0.378f, 0.174f), Offset(0.336f, 0.183f),
    Offset(0.296f, 0.195f), Offset(0.264f, 0.209f), Offset(0.242f, 0.226f),
    // ── Outer upper arm (tricep shape) ──
    Offset(0.230f, 0.248f), Offset(0.222f, 0.274f), Offset(0.217f, 0.302f),
    Offset(0.214f, 0.330f), Offset(0.212f, 0.352f),
    // ── Outer elbow ──
    Offset(0.210f, 0.370f), Offset(0.208f, 0.386f),
    // ── Outer forearm ──
    Offset(0.199f, 0.408f), Offset(0.188f, 0.436f), Offset(0.176f, 0.466f),
    Offset(0.163f, 0.498f), Offset(0.150f, 0.530f), Offset(0.138f, 0.556f),
    // ── Wrist ──
    Offset(0.130f, 0.578f), Offset(0.122f, 0.598f),
    // ── Hand ──
    Offset(0.116f, 0.622f), Offset(0.110f, 0.644f), Offset(0.106f, 0.664f),
    Offset(0.104f, 0.682f),
    // ── Fingertips ──
    Offset(0.105f, 0.698f), Offset(0.112f, 0.710f), Offset(0.124f, 0.708f),
    Offset(0.134f, 0.694f),
    // ── Inner hand ascending ──
    Offset(0.142f, 0.670f), Offset(0.148f, 0.646f), Offset(0.154f, 0.620f),
    Offset(0.162f, 0.590f), Offset(0.170f, 0.558f),
    // ── Inner forearm ascending ──
    Offset(0.184f, 0.522f), Offset(0.197f, 0.488f), Offset(0.210f, 0.454f),
    Offset(0.223f, 0.424f),
    // ── Inner elbow / inner upper arm ──
    Offset(0.237f, 0.394f), Offset(0.249f, 0.364f), Offset(0.258f, 0.334f),
    Offset(0.264f, 0.304f), Offset(0.268f, 0.278f),
    // ── Armpit → lat insertions ──
    Offset(0.275f, 0.254f), Offset(0.292f, 0.240f), Offset(0.316f, 0.248f),
    Offset(0.338f, 0.268f),
    // ── Lat / upper back contour (wider V-taper) ──
    Offset(0.358f, 0.295f), Offset(0.370f, 0.322f), Offset(0.378f, 0.350f),
    // ── Lower back / waist taper (deeper) ──
    Offset(0.375f, 0.380f), Offset(0.366f, 0.408f), Offset(0.352f, 0.432f),
    Offset(0.342f, 0.455f),
    // ── Glute rise ──
    Offset(0.346f, 0.480f), Offset(0.358f, 0.508f), Offset(0.375f, 0.534f),
    Offset(0.392f, 0.556f), Offset(0.396f, 0.576f),
    // ── Outer hamstring ──
    Offset(0.390f, 0.598f), Offset(0.378f, 0.626f), Offset(0.365f, 0.656f),
    Offset(0.353f, 0.686f), Offset(0.344f, 0.716f),
    // ── Knee (back, popliteal) ──
    Offset(0.338f, 0.744f), Offset(0.335f, 0.768f), Offset(0.337f, 0.790f),
    // ── Calf (pronounced gastrocnemius from rear) ──
    Offset(0.344f, 0.816f), Offset(0.350f, 0.842f), Offset(0.347f, 0.868f),
    Offset(0.339f, 0.894f),
    // ── Ankle ──
    Offset(0.329f, 0.920f), Offset(0.323f, 0.946f),
    // ── Heel / foot ──
    Offset(0.319f, 0.970f), Offset(0.316f, 0.993f), Offset(0.319f, 1.013f),
    Offset(0.332f, 1.027f), Offset(0.358f, 1.035f), Offset(0.390f, 1.039f),
    Offset(0.418f, 1.037f), Offset(0.438f, 1.027f),
    // ── Inner ankle ──
    Offset(0.452f, 1.005f), Offset(0.458f, 0.978f), Offset(0.460f, 0.950f),
    // ── Inner calf ──
    Offset(0.462f, 0.920f), Offset(0.464f, 0.888f), Offset(0.466f, 0.858f),
    Offset(0.468f, 0.830f),
    // ── Inner knee ──
    Offset(0.470f, 0.802f), Offset(0.472f, 0.775f), Offset(0.474f, 0.750f),
    // ── Inner thigh ──
    Offset(0.478f, 0.720f), Offset(0.482f, 0.688f), Offset(0.486f, 0.654f),
    Offset(0.490f, 0.620f), Offset(0.494f, 0.590f),
    // ── Crotch ──
    Offset(0.498f, 0.567f), Offset(0.500f, 0.555f)
)

private fun buildBodySilhouettePath(
    frame: Rect,
    side: HeatmapSide,
    bodyShape: BodyShapeInputs
): androidx.compose.ui.graphics.Path {
    fun point(normalized: Offset): Offset {
        val morphed = morphBodyPoint(normalized, bodyShape, side)
        return Offset(
            x = frame.left + frame.width * morphed.x,
            y = frame.top + frame.height * morphed.y
        )
    }

    val leftPoints = if (side == HeatmapSide.Front) frontLeftPoints else backLeftPoints

    return androidx.compose.ui.graphics.Path().apply {
        if (leftPoints.isEmpty()) return@apply

        // Build full loop: left side → mirrored right side
        val all = mutableListOf<Offset>()
        for (p in leftPoints) all.add(point(p))
        for (i in leftPoints.size - 2 downTo 0) {
            all.add(point(Offset(1f - leftPoints[i].x, leftPoints[i].y)))
        }

        val n = all.size
        moveTo(all[0].x, all[0].y)

        // Catmull-Rom → cubic Bézier for smooth curves
        for (i in 0 until n) {
            val p0 = all[(i - 1 + n) % n]
            val p1 = all[i]
            val p2 = all[(i + 1) % n]
            val p3 = all[(i + 2) % n]
            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f
            cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
        close()
    }
}

private fun drawBodyFiberLines(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    frame: Rect,
    side: HeatmapSide,
    bodyShape: BodyShapeInputs
) = with(draw) {
    fun point(vx: Float, vy: Float): Offset {
        val morphed = morphBodyPoint(Offset(vx, vy), bodyShape, side)
        return Offset(frame.left + frame.width * morphed.x, frame.top + frame.height * morphed.y)
    }

    fun definitionFor(vararg muscles: MuscleGroups): Float {
        val values = muscles.mapNotNull { bodyShape.muscleStates[it]?.definition }
        if (values.isEmpty()) return (bodyShape.experienceYears / 8f).coerceIn(0f, 0.45f)
        return values.average().toFloat()
    }

    fun seg(sx: Float, sy: Float, ex: Float, ey: Float, intensity: Float) {
        if (intensity <= 0.02f) return
        drawLine(
            color = Color.White.copy(alpha = 0.06f + intensity * 0.26f),
            start = point(sx, sy), end = point(ex, ey),
            strokeWidth = (0.8f + intensity * 1.6f).dp.toPx()
        )
    }

    if (side == HeatmapSide.Front) {
        val abs = definitionFor(MuscleGroups.Abs)
        val pec = definitionFor(MuscleGroups.Pecs, MuscleGroups.Delts)
        val arm = definitionFor(MuscleGroups.Biceps, MuscleGroups.Forearms)
        val quad = definitionFor(MuscleGroups.Quads)
        val delt = definitionFor(MuscleGroups.Delts)

        // Linea alba (midline)
        seg(0.50f, 0.33f, 0.50f, 0.52f, abs)
        // Abs horizontal separations
        seg(0.46f, 0.36f, 0.54f, 0.36f, abs * 0.9f)
        seg(0.46f, 0.40f, 0.54f, 0.40f, abs)
        seg(0.46f, 0.44f, 0.54f, 0.44f, abs)
        seg(0.47f, 0.48f, 0.53f, 0.48f, abs * 0.85f)
        // Pec lower border
        seg(0.35f, 0.30f, 0.44f, 0.33f, pec)
        seg(0.56f, 0.33f, 0.65f, 0.30f, pec)
        // Sternum
        seg(0.50f, 0.22f, 0.50f, 0.33f, pec * 0.7f)
        // Anterior delt separation
        seg(0.34f, 0.25f, 0.30f, 0.29f, delt * 0.8f)
        seg(0.66f, 0.25f, 0.70f, 0.29f, delt * 0.8f)
        // Serratus
        seg(0.36f, 0.34f, 0.39f, 0.36f, abs * 0.55f)
        seg(0.64f, 0.34f, 0.61f, 0.36f, abs * 0.55f)
        seg(0.37f, 0.37f, 0.40f, 0.39f, abs * 0.45f)
        seg(0.63f, 0.37f, 0.60f, 0.39f, abs * 0.45f)
        // Bicep line
        seg(0.24f, 0.30f, 0.23f, 0.36f, arm * 0.8f)
        seg(0.76f, 0.30f, 0.77f, 0.36f, arm * 0.8f)
        // Forearm
        seg(0.21f, 0.41f, 0.19f, 0.50f, arm * 0.55f)
        seg(0.79f, 0.41f, 0.81f, 0.50f, arm * 0.55f)
        // Quad - rectus femoris
        seg(0.42f, 0.58f, 0.40f, 0.72f, quad * 0.7f)
        seg(0.58f, 0.58f, 0.60f, 0.72f, quad * 0.7f)
        // Vastus lateralis
        seg(0.37f, 0.60f, 0.35f, 0.72f, quad * 0.55f)
        seg(0.63f, 0.60f, 0.65f, 0.72f, quad * 0.55f)
        // VMO teardrop
        seg(0.45f, 0.70f, 0.44f, 0.74f, quad * 0.6f)
        seg(0.55f, 0.70f, 0.56f, 0.74f, quad * 0.6f)
    } else {
        val back = definitionFor(MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps)
        val arm = definitionFor(MuscleGroups.Triceps, MuscleGroups.Forearms)
        val post = definitionFor(MuscleGroups.Glutes, MuscleGroups.Hamstrings)
        val trap = definitionFor(MuscleGroups.Traps)

        // Spinal erectors
        seg(0.48f, 0.20f, 0.48f, 0.50f, back * 0.7f)
        seg(0.52f, 0.20f, 0.52f, 0.50f, back * 0.7f)
        // Spine midline
        seg(0.50f, 0.18f, 0.50f, 0.52f, back * 0.5f)
        // Scapula medial border
        seg(0.43f, 0.24f, 0.44f, 0.36f, back * 0.6f)
        seg(0.57f, 0.24f, 0.56f, 0.36f, back * 0.6f)
        // Lat insertion lines
        seg(0.42f, 0.35f, 0.46f, 0.45f, back * 0.8f)
        seg(0.58f, 0.35f, 0.54f, 0.45f, back * 0.8f)
        // Lower trap
        seg(0.46f, 0.22f, 0.50f, 0.30f, trap * 0.6f)
        seg(0.54f, 0.22f, 0.50f, 0.30f, trap * 0.6f)
        // Tricep long head
        seg(0.24f, 0.30f, 0.22f, 0.37f, arm * 0.7f)
        seg(0.76f, 0.30f, 0.78f, 0.37f, arm * 0.7f)
        // Forearm
        seg(0.21f, 0.41f, 0.18f, 0.50f, arm * 0.5f)
        seg(0.79f, 0.41f, 0.82f, 0.50f, arm * 0.5f)
        // Glute crease
        seg(0.42f, 0.56f, 0.50f, 0.58f, post * 0.7f)
        seg(0.58f, 0.56f, 0.50f, 0.58f, post * 0.7f)
        // Glute-ham tie-in
        seg(0.50f, 0.56f, 0.50f, 0.66f, post * 0.6f)
        // Hamstring - biceps femoris
        seg(0.40f, 0.62f, 0.38f, 0.74f, post * 0.6f)
        seg(0.60f, 0.62f, 0.62f, 0.74f, post * 0.6f)
        // Semitendinosus
        seg(0.44f, 0.62f, 0.45f, 0.74f, post * 0.5f)
        seg(0.56f, 0.62f, 0.55f, 0.74f, post * 0.5f)
    }
}

private fun drawHeatmapRegion(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    frame: Rect,
    region: HeatmapRegion,
    bodyShape: BodyShapeInputs,
    fillColor: Color,
    accentColor: Color,
    fillAlpha: Float,
    accentAlpha: Float,
    strokeWidthPx: Float
) = with(draw) {
    val points = contourPointsInFrame(frame, region, bodyShape)
    val path = buildPolygonPath(points)
    val cornerEffect = androidx.compose.ui.graphics.PathEffect.cornerPathEffect(strokeWidthPx * 6)

    drawPath(path = path, color = fillColor.copy(alpha = fillAlpha))

    drawPath(
        path = path,
        color = bodySilhouetteBase.copy(alpha = 0.9f),
        style = Stroke(width = strokeWidthPx * 3.5f, pathEffect = cornerEffect)
    )

    drawPath(
        path = path,
        color = accentColor.copy(alpha = accentAlpha),
        style = Stroke(width = strokeWidthPx * 1.2f, pathEffect = cornerEffect)
    )
}

private fun drawRegionGlow(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    frame: Rect,
    region: HeatmapRegion,
    bodyShape: BodyShapeInputs,
    color: Color,
    intensity: Float
) = with(draw) {
    if (intensity <= 0.04f) return@with

    val points = contourPointsInFrame(frame, region, bodyShape)
    val path = buildPolygonPath(points)
    val bounds = path.getBounds()
    val center = bounds.center
    val radius = (max(bounds.width, bounds.height) * (0.68f + intensity * 0.34f)).coerceAtLeast(1f)

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.78f * intensity),
                color.copy(alpha = 0.46f * intensity),
                color.copy(alpha = 0.2f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
    )
}

private fun findHeatmapRegionAtTap(
    tap: Offset,
    canvasSize: IntSize,
    regions: List<HeatmapRegion>,
    bodyShape: BodyShapeInputs
): HeatmapRegion? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    val frame = bodyFrame(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()))
    return regions.firstOrNull { region ->
        val polygon = contourPointsInFrame(frame, region, bodyShape)
        pointInPolygon(tap, polygon)
    }
}

private fun topMuscleForRegion(
    region: HeatmapRegion,
    loadsByMuscle: Map<MuscleGroups, MuscleLoad>
): MuscleLoad? = region.muscles
    .mapNotNull { loadsByMuscle[it] }
    .maxByOrNull { heatIntensity(it) }

@Composable
private fun BodyHeatmapCard(
    loads: List<MuscleLoad>,
    visualLoads: List<MuscleLoad>,
    bodyFatPercentage: Float?,
    userWeightKg: Float?,
    experienceYears: Float,
    onSelectLoad: (MuscleLoad) -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    var side by remember { mutableStateOf(HeatmapSide.Front) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val loadsByMuscle = remember(loads) { loads.associateBy { it.group } }
    val regions = remember(side) { regionsForSide(side) }
    val haptics = LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var clickedRegionId by remember { mutableStateOf<String?>(null) }
    val clickPulse = remember { androidx.compose.animation.core.Animatable(0f) }
    val muscleStates = remember(visualLoads, bodyFatPercentage, experienceYears) {
        buildMuscleVisualStates(
            loads = visualLoads,
            bodyFatPercentage = bodyFatPercentage,
            experienceYears = experienceYears
        )
    }
    val bodyShape = remember(bodyFatPercentage, userWeightKg, experienceYears, muscleStates) {
        BodyShapeInputs(
            weightKg = userWeightKg,
            bodyFatPercentage = bodyFatPercentage,
            experienceYears = experienceYears,
            muscleStates = muscleStates
        )
    }

    Surface(
        color = theme.background,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Body Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HeatmapSide.Front to "Front", HeatmapSide.Back to "Back").forEach { (candidate, label) ->
                        val selected = side == candidate
                        Surface(
                            onClick = { side = candidate },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) theme.tertiary else theme.secondary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (selected) theme.primary else theme.secondary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) theme.primary else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(side, loads, canvasSize, bodyFatPercentage, userWeightKg, experienceYears, visualLoads) {
                        detectTapGestures { tap ->
                            val region = findHeatmapRegionAtTap(tap, canvasSize, regions, bodyShape) ?: return@detectTapGestures
                            val load = topMuscleForRegion(region, loadsByMuscle)
                            if (load != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                clickedRegionId = region.id
                                coroutineScope.launch {
                                    clickPulse.snapTo(0f)
                                    clickPulse.animateTo(1f, tween(300, easing = LinearEasing))
                                    clickedRegionId = null
                                }
                                onSelectLoad(load)
                            }
                        }
                    }
            ) {
                val frame = bodyFrame(size)
                val silhouettePath = buildBodySilhouettePath(frame, side, bodyShape)
                val cornerEffect = androidx.compose.ui.graphics.PathEffect.cornerPathEffect(4.dp.toPx())

                drawPath(
                    path = silhouettePath,
                    color = bodySilhouetteBase
                )
                drawPath(
                    path = silhouettePath,
                    color = bodySilhouetteStroke.copy(alpha = 0.95f),
                    style = Stroke(width = 2.5.dp.toPx(), pathEffect = cornerEffect)
                )

                drawLine(
                    color = bodySilhouetteStroke.copy(alpha = 0.2f),
                    start = Offset(frame.left + frame.width * 0.5f, frame.top + frame.height * 0.17f),
                    end = Offset(frame.left + frame.width * 0.5f, frame.top + frame.height * 0.96f),
                    strokeWidth = 1.dp.toPx()
                )

                regions.forEach { region ->
                    val load = topMuscleForRegion(region, loadsByMuscle)
                    val heat = load?.let(::heatIntensity) ?: 0f
                    val fill = quietHeatFill(load)
                    val accent = load?.let { heatColor(heatIntensity(it)) } ?: bodySilhouetteStroke
                    val glowIntensity = load?.let(::fatigueGlowIntensity) ?: 0f
                    val glowColor = load?.let(::fatigueGlowColor) ?: Color.Transparent
                    val isClicked = region.id == clickedRegionId
                    val currentPulse = if (isClicked) clickPulse.value else 0f

                    drawRegionGlow(
                        draw = this,
                        frame = frame,
                        region = region,
                        bodyShape = bodyShape,
                        color = glowColor,
                        intensity = glowIntensity
                    )

                    if (isClicked && currentPulse > 0f) {
                        val bounds = buildPolygonPath(contourPointsInFrame(frame, region, bodyShape)).getBounds()
                        withTransform({
                            val scaleAmt = 1f + (0.2f * kotlin.math.sin(currentPulse.toDouble() * Math.PI)).toFloat()
                            scale(scaleAmt, scaleAmt, bounds.center)
                        }) {
                            drawHeatmapRegion(
                                draw = this,
                                frame = frame,
                                region = region,
                                bodyShape = bodyShape,
                                fillColor = Color.White,
                                accentColor = Color.White,
                                fillAlpha = (0.2f * kotlin.math.sin(currentPulse.toDouble() * Math.PI)).toFloat(),
                                accentAlpha = (0.6f * kotlin.math.sin(currentPulse.toDouble() * Math.PI)).toFloat(),
                                strokeWidthPx = 1.dp.toPx()
                            )
                        }
                    }

                    drawHeatmapRegion(
                        draw = this,
                        frame = frame,
                        region = region,
                        bodyShape = bodyShape,
                        fillColor = fill,
                        accentColor = if (glowIntensity > 0.15f) glowColor else accent,
                        fillAlpha = 0.84f + heat * 0.14f,
                        accentAlpha = 0.36f + heat * 0.58f + glowIntensity * 0.56f +
                            if (isClicked) (0.28f * kotlin.math.sin(currentPulse.toDouble() * Math.PI)).toFloat() else 0f,
                        strokeWidthPx = 1.dp.toPx()
                    )
                }

                drawBodyFiberLines(
                    draw = this,
                    frame = frame,
                    side = side,
                    bodyShape = bodyShape
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Low", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(heatColor(level))
                    )
                }
                Text("High", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
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
        if (ml.injuryRisk > 0.78f) return@mapNotNull null

        val fatiguePenalty = (if (lifestyle.sleepOk) 0f else 0.15f) +
            (if (lifestyle.proteinOk) 0f else 0.1f) +
            (ml.injuryRisk * 0.35f)

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
    val theme = appearanceOptions.colors

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
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    LaunchedEffect(Unit) { SurveyTape.init(context) }

    val healthConnectManager = remember { HealthConnectManager(context) }
    var loads by remember { mutableStateOf<List<MuscleLoad>>(emptyList()) }
    var recoveryFactors by remember { mutableStateOf<RecoveryFactors?>(null) }
    var computing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val prefsManager = remember { UserPreferencesManager(context) }
    val profile = remember { buildUserProfile(prefsManager) }
    val experienceYears = remember(profile.experience) { parseExperienceToYears(profile.experience) }
    var latestBodyFatPercentage by remember { mutableStateOf<Float?>(null) }

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
        latestBodyFatPercentage = bodyfat.maxByOrNull { it.time }?.percentage?.value?.toFloat()

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
        if (finalLoads.isNotEmpty()) {
            GeminiAdaptiveMemoryStore.recordMuscleSignals(
                context = context,
                snapshot = buildGeminiSignalSnapshot(
                    loads = finalLoads,
                    recoveryFactors = factors
                )
            )
        }
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
                    if (sprintGoal.isActive) theme.primary else theme.background,
                    radius = 8.dp
                ),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (sprintGoal.isActive) theme.background.copy(alpha = 0.2f) else theme.background,
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
        if (performanceOptions.showBodyHeatmap) {
            BodyHeatmapCard(
                loads = filtered,
                visualLoads = loads,
                bodyFatPercentage = latestBodyFatPercentage,
                userWeightKg = profile.weightKg.toFloat(),
                experienceYears = experienceYears,
                onSelectLoad = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedMuscle = it
                }
            )
        }

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
        val acute24Load = remember { getAccumulatedLoad(recentWorkouts, now, 1) }
        val chronic28 = remember(recentWorkouts, now) { getAccumulatedStimulus(recentWorkouts, now, 28) }
        val chronic28Load = remember(recentWorkouts, now) { getAccumulatedLoad(recentWorkouts, now, 28) }
        val history42 = remember(recentWorkouts, now) { collectMuscleHistoryProfile(recentWorkouts, now, 42) }
        val frequency14 = remember(recentWorkouts, now) { getMuscleFrequencyPerWeek(recentWorkouts, now, 14) }
        val hrStrain14 = remember(recentWorkouts, now) { averageHrStrainByMuscle(recentWorkouts, now, 14) }
        val lastTrainedMap = remember(recentWorkouts) {
            val m = mutableMapOf<MuscleGroups, Instant>()
            recentWorkouts.forEach { w -> w.exercises.forEach { e -> muscleMappings.forEach { (r, mp) -> if(r.containsMatchIn(e.lowercase())) mp.keys.forEach { m[it] = w.date } } } }
            m
        }
        val recentMap = remember { recentExercisesByMuscle(recentWorkouts, profile) }
        val baseTarget = remember(ml.group, profile, sprintGoal, recoveryFactors) {
            calculateBaseTarget(profile, recoveryFactors!!.survey, ml.group, sprintGoal)
        }
        val chronicWeeklyVolume = (((chronic28[ml.group] ?: 0f) / 4f)).coerceAtLeast(0.1f)
        val chronicWeeklyLoad = (((chronic28Load[ml.group] ?: 0f) / 4f)).coerceAtLeast(0.1f)
        val adaptation = remember(
            ml.group,
            profile,
            sprintGoal,
            recoveryFactors,
            frequency14,
            chronic28,
            chronic28Load,
            history42
        ) {
            calculateMuscleAdaptation(
                profile = profile,
                survey = recoveryFactors!!.survey,
                sprintGoal = sprintGoal,
                muscle = ml.group,
                baseTarget = baseTarget,
                history = history42[ml.group] ?: MuscleHistoryProfile(),
                frequencyPerWeek = frequency14[ml.group] ?: 0f,
                chronicWeeklyVolume = chronicWeeklyVolume,
                chronicWeeklyLoad = chronicWeeklyLoad
            )
        }
        val insight = insightForMuscle(
            m = ml.group,
            now = now,
            lastTrained = lastTrainedMap,
            prev14 = prev14,
            cur14 = cur14,
            acute24 = acute24,
            acute24Load = acute24Load,
            recovery = recoveryFactors!!,
            weeklyFrequency = frequency14[ml.group] ?: 0f,
            hrStrain = hrStrain14[ml.group] ?: 0f,
            injuryRisk = ml.injuryRisk,
            adaptation = adaptation
        )
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
        if (factors.recentEnvironmentStress) FactorPill(-15f, "Heat", Icons.Outlined.Thermostat)
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
    val theme = appearanceOptions.colors

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
    val theme = appearanceOptions.colors

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
    val theme = appearanceOptions.colors


    val color = remember(load.band) {
        when (load.band) {
            LoadBand.Building -> Color(0xFF42A5F5)
            LoadBand.OnTrack -> Color(0xFF00E676)
            LoadBand.Recovering -> Color(0xFFAB47BC)
            LoadBand.Overreached -> Color(0xFFFFD54F)
            LoadBand.DeloadRecommended -> Color(0xFFFFB74D)
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
                        text = prettyLoadBandName(load.band),
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
    val theme = appearanceOptions.colors

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

            if (factors.recentEnvironmentStress) {
                HealthMetricRow(Icons.Outlined.Thermostat, "Heat Stress", "Detected", "High environmental load identified", 0.2f)
            }

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
    val theme = appearanceOptions.colors
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
                listOf(1,2, 4, 6, 8, 12).forEach { weeks ->
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
    val theme = appearanceOptions.colors

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
    val theme = appearanceOptions.colors

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
                    Text(
                        "${insight.readinessText} · Risk ${insight.injuryRiskPct}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.7f)
                    )
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
    val theme = appearanceOptions.colors
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
