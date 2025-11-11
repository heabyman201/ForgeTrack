package com.forgecompose.workouttracker

import android.health.connect.datatypes.HeartRateRecord
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.HeartRateRecord as HcHeartRateRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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
        weightKg = toIntSafe(prefs.getWeight(), 70),
        heightCm = toIntSafe(prefs.getHeight(), 170),
        age = toIntSafe(prefs.getAge(), 25),
        experience = prefs.getExperience().lowercase(Locale.US),
        preferredStyle = prefs.getPreferredStyle().lowercase(Locale.US),
        importantMuscles = toListOfMuscles(prefs.getImportantMuscles())
    )
}

enum class MuscleGroups { Pecs, Delts, Biceps, Triceps, Lats, Traps, Abs, Forearms, Quads, Hamstrings, Glutes, Calves, LowerBack, UpperBack }

enum class LoadBand { NotTrained, SlightlyTrained, Building, OnTrack, Recovering, Overreached, DeloadRecommended }

data class MuscleLoad(
    val group: MuscleGroups,
    val weeklyProgress: Float,
    val weeklyTarget: Float,
    val band: LoadBand,
    val score: Float,
    val lastTrainedAgo: String? = null
)

data class RecoveryFactors(
    val sleepHours: Float?,
    val restingHeartRate: Long?,
    val sleepSpo2: Double?,
    val proteinGrams: Double?,
    val bodyFatPercentage: Double?
) {
    val sleepMultiplier: Float = when {
        sleepHours == null -> 1f
        sleepHours >= 8.5f -> 1.15f
        sleepHours >= 7.5f -> 1.08f
        sleepHours >= 6.5f -> 1f
        sleepHours >= 5f -> 0.92f
        else -> 0.85f
    }
    val rhrMultiplier: Float = when {
        restingHeartRate == null -> 1f
        restingHeartRate < 50 -> 1.1f
        restingHeartRate < 60 -> 1.05f
        restingHeartRate < 70 -> 1f
        restingHeartRate < 80 -> 0.95f
        else -> 0.9f
    }
    val spo2Multiplier: Float = when {
        sleepSpo2 == null -> 1f
        sleepSpo2 >= 98.0 -> 1.08f
        sleepSpo2 >= 95.0 -> 1.04f
        sleepSpo2 >= 92.0 -> 1f
        else -> 0.93f
    }
    val proteinMultiplier: Float = when {
        proteinGrams == null -> 1f
        proteinGrams >= 150 -> 1.12f
        proteinGrams >= 100 -> 1.06f
        proteinGrams >= 60 -> 1f
        else -> 0.94f
    }
    val bodyFatMultiplier: Float = when {
        bodyFatPercentage == null -> 1f
        bodyFatPercentage in 12.0..20.0 -> 1.05f
        bodyFatPercentage in 8.0..25.0 -> 1f
        else -> 0.96f
    }
    val totalMultiplier: Float = (sleepMultiplier * rhrMultiplier * spo2Multiplier * proteinMultiplier * bodyFatMultiplier).coerceIn(0.8f, 1.25f)
}

data class WorkoutSummary(val date: Instant, val name: String, val exercises: List<String> = emptyList())

private fun friendlyAgo(now: Instant, then: Instant): String {
    val mins = ChronoUnit.MINUTES.between(then, now)
    if (mins < 60) return "${kotlin.math.max(0L, mins)}m ago"
    val hours = ChronoUnit.HOURS.between(then, now)
    if (hours < 24) return "${hours}h ago"
    val days = ChronoUnit.DAYS.between(then, now)
    return when {
        days == 1L -> "1d ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

data class Targets(val target: Float, val lackingCutoff: Int, val overtrainedCutoff: Int)

private fun bmi(weightKg: Int, heightCm: Int): Float {
    val h = heightCm / 100f
    return if (h <= 0f) 0f else weightKg / (h * h)
}

private fun parseYearsFromExperience(e: String): Float? {
    val rx = Regex("""(\d+(?:\.\d+)?)\s*(?:y|yr|yrs|year|years)?""")
    return rx.find(e)?.groupValues?.getOrNull(1)?.toFloatOrNull()
}
private fun expMultFrom(experience: String): Float {
    val e = experience.trim().lowercase(Locale.US)
    val num = parseYearsFromExperience(e)
    return when {
        num != null && num >= 8f -> 1.2f
        num != null && num >= 5f -> 1.14f
        num != null && num >= 3f -> 1.08f
        num != null && num >= 1f -> 1.04f
        "elite" in e -> 1.2f
        "advanced" in e -> 1.14f
        "intermediate" in e -> 1.08f
        "novice" in e -> 1.04f
        "beginner" in e -> 0.98f
        else -> 1.02f
    }
}
private var frequencyHint: Map<MuscleGroups, Float> = emptyMap()
private fun muscleFatigueBias(m: MuscleGroups): Pair<Float, Float> = when (m) {
    MuscleGroups.LowerBack -> 1.10f to 0.85f
    MuscleGroups.Hamstrings -> 1.08f to 0.9f
    MuscleGroups.Quads -> 1.08f to 0.9f
    MuscleGroups.Glutes -> 1.07f to 0.92f
    MuscleGroups.Lats -> 1.05f to 0.95f
    MuscleGroups.UpperBack -> 1.05f to 0.95f
    MuscleGroups.Delts -> 0.97f to 1.05f
    MuscleGroups.Calves -> 0.92f to 1.15f
    MuscleGroups.Forearms -> 0.92f to 1.15f
    MuscleGroups.Abs -> 0.9f to 1.1f
    MuscleGroups.Traps -> 1.02f to 1.0f
    MuscleGroups.Biceps -> 0.95f to 1.05f
    MuscleGroups.Triceps -> 0.95f to 1.05f
    MuscleGroups.Pecs -> 1.02f to 0.98f
}

private fun computeTargets(profile: UserProfile, muscle: MuscleGroups): Targets {
    var tgt = 14.5f
    tgt *= expMultFrom(profile.experience)
    tgt *= when (profile.preferredStyle) {
        "weights" -> 0.95f
        "both" -> 1.09f
        "cardio" -> 0.9f
        else -> 1.02f
    }
    val age = profile.age
    val ageVolMult = when {
        age < 18 -> 1.06f
        age < 30 -> 1.04f
        age < 40 -> 1.01f
        age < 50 -> 0.98f
        age < 60 -> 0.95f
        age < 70 -> 0.91f
        else -> 0.86f
    }
    tgt *= ageVolMult
    val hCm = profile.heightCm
    val wKg = profile.weightKg
    val b = bmi(wKg, hCm)
    val bmiVolMult = when {
        b < 18.5f -> 0.99f
        b < 22f -> 1.05f
        b < 27f -> 1.01f
        b < 32f -> 0.98f
        else -> 0.94f
    }
    tgt *= bmiVolMult
    val heightBias = when {
        hCm >= 195 -> 1.1f
        hCm >= 185 -> 1.05f
        hCm <= 165 -> 0.99f
        else -> 1.01f
    }
    val weightBias = when {
        wKg >= 100 -> 1.05f
        wKg >= 85 -> 1.03f
        wKg <= 55 -> 0.98f
        else -> 1.0f
    }
    val regional = when (muscle) {
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.LowerBack -> heightBias * weightBias
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps -> 1.03f
        else -> heightBias
    }
    tgt *= regional
    val smallMuscleReduction = when (muscle) {
        MuscleGroups.Biceps -> 0.88f
        MuscleGroups.Triceps, MuscleGroups.Forearms -> 0.9f
        MuscleGroups.Calves, MuscleGroups.Abs -> 0.92f
        else -> 1.0f
    }
    tgt *= smallMuscleReduction
    val isImportant = muscle in profile.importantMuscles
    if (isImportant) tgt *= 1.14f
    val freq = frequencyHint[muscle]?.coerceIn(0f, 7f) ?: 0f
    tgt *= (0.92f + 0.04f * freq)
    val targetMult = muscleFatigueBias(muscle).first
    tgt *= targetMult
    tgt *= 1.04f
    tgt = tgt.coerceIn(8.5f, 28f)
    val lackingFrac = if (isImportant) 0.46f else 0.48f
    val overFrac = if (isImportant) 1.58f else 1.55f
    val lacking = floor(tgt * lackingFrac).toInt().coerceAtLeast(0)
    val over = ceil(tgt * overFrac).toInt()
    return Targets(target = tgt, lackingCutoff = lacking, overtrainedCutoff = over)
}

private data class ParsedVolume(val sets: Int?, val reps: Int?, val weightKg: Float?)
private val tripletRx = Regex("""(?:(\d+)\s*[xX]\s*(\d+)(?:\s*[xX]\s*([0-9]*\.?[0-9]+))?)(?:\s*@\s*([0-9]*\.?[0-9]+|bw|bodyweight))?""")
private val inlineWordsRx = Regex("""(?:(\d+)\s*sets?)|(?:(\d+)\s*reps?)|(?:(\d+)\s*kg)|(?:(\d+)\s*lbs?)""", RegexOption.IGNORE_CASE)
private val kvRx = Regex("""(?:\bsets\s*=\s*(\d+))?|(?:\breps\s*=\s*(\d+))?|(?:\bweight\s*=\s*([0-9]*\.?[0-9]+|bw|bodyweight))?""", RegexOption.IGNORE_CASE)

private fun parseVolumeFromText(s: String): ParsedVolume {
    val lower = s.lowercase()
    tripletRx.find(lower)?.let { m ->
        val sets = m.groupValues[1].toIntOrNull()
        val reps = m.groupValues[2].toIntOrNull()
        val third = m.groupValues.getOrNull(3)?.toFloatOrNull()
        val at = m.groupValues.getOrNull(4)?.trim().orEmpty()
        val w = when {
            at.isEmpty() && third != null -> third
            at == "bw" || at == "bodyweight" -> null
            at.isNotEmpty() -> at.toFloatOrNull()
            else -> null
        }
        return ParsedVolume(sets, reps, w)
    }
    var sets: Int? = null
    var reps: Int? = null
    var weight: Float? = null
    inlineWordsRx.findAll(lower).forEach { m ->
        val g1 = m.groups[1]?.value?.toIntOrNull()
        val g2 = m.groups[2]?.value?.toIntOrNull()
        val g3 = m.groups[3]?.value?.toFloatOrNull()
        val g4 = m.groups[4]?.value?.toFloatOrNull()
        if (g1 != null) sets = g1
        if (g2 != null) reps = g2
        if (g3 != null) weight = g3
        if (g4 != null) weight = (g4 * 0.45359237f)
    }
    kvRx.findAll(lower).forEach { m ->
        val g1 = m.groups[1]?.value?.toIntOrNull()
        val g2 = m.groups[2]?.value?.toIntOrNull()
        val g3raw = m.groups[3]?.value?.trim()
        if (g1 != null) sets = g1
        if (g2 != null) reps = g2
        if (g3raw != null) {
            weight = when (g3raw) {
                "bw", "bodyweight" -> null
                else -> g3raw.toFloatOrNull()
            }
        }
    }
    return ParsedVolume(sets, reps, weight)
}

private fun difficultyWordMult(token: String): Float {
    var m = 1.12f
    if ("amrap" in token || "failure" in token) m *= 1.12f
    if ("drop set" in token || "dropset" in token) m *= 1.08f
    if ("super set" in token || "superset" in token) m *= 1.06f
    if ("rest pause" in token || "rest-pause" in token) m *= 1.06f
    if ("tempo" in token) m *= 1.04f
    return m
}

private fun effectiveSetsFromToken(token: String, profile: UserProfile): Float {
    val p = parseVolumeFromText(token)
    val baseSets = (p.sets ?: 1).coerceAtLeast(1)
    val reps = (p.reps ?: 8).coerceAtLeast(1)
    val repsFactor = when {
        reps < 5 -> 0.96f
        reps <= 30 -> 1.06f
        else -> 0.98f
    }
    val loadFactor = p.weightKg?.let { w ->
        val bw = profile.weightKg.coerceAtLeast(50).toFloat()
        val ratio = (w / bw).coerceIn(0.15f, 2.5f)
        0.78f + 0.38f * ratio.pow(0.45f)
    } ?: 0.9f
    val wordMult = difficultyWordMult(token)
    val optimismBoost = 1.18f
    return baseSets * repsFactor * loadFactor * wordMult * optimismBoost
}

private val nameToMusclesWeighted: List<Pair<Regex, List<Pair<MuscleGroups, Float>>>> = listOf(
    "bench( press)?|flat bench|barbell bench" to listOf(MuscleGroups.Pecs to 1.0f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.6f),
    "incline( bench)?|incline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Delts to 0.7f, MuscleGroups.Triceps to 0.5f),
    "decline( bench)?|decline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "close[- ]?grip bench" to listOf(MuscleGroups.Triceps to 0.9f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.5f),
    "push[- ]?up|diamond push[- ]?up|wide push[- ]?up|deficit push[- ]?up|ring push[- ]?up" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "dumbbell press|db press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "machine press|smith press|pec deck" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Delts to 0.5f),
    "chest fly|flye|cable crossover|low to high fly|high to low fly" to listOf(MuscleGroups.Pecs to 0.5f, MuscleGroups.Delts to 0.25f),
    "dip|bench dip" to listOf(MuscleGroups.Triceps to 0.8f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.4f),
    "overhead press|shoulder press|ohp|military press|arnold press|push press|db shoulder press" to listOf(MuscleGroups.Delts to 1.0f, MuscleGroups.Triceps to 0.7f),
    "lateral raise|side raise|cable lateral" to listOf(MuscleGroups.Delts to 0.4f),
    "front raise" to listOf(MuscleGroups.Delts to 0.4f),
    "reverse fly|rear delt fly|face pull" to listOf(MuscleGroups.Delts to 0.5f, MuscleGroups.UpperBack to 0.5f, MuscleGroups.Traps to 0.4f),
    "pull[- ]?up|chin[- ]?up|neutral grip pull[- ]?up" to listOf(MuscleGroups.Lats to 1.0f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "lat pull|pulldown|wide grip pulldown|close grip pulldown" to listOf(MuscleGroups.Lats to 0.9f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "row|barbell row|seated row|cable row|t[- ]?bar row|one[- ]?arm row|db row|pendlay row|meadow row" to listOf(MuscleGroups.UpperBack to 0.9f, MuscleGroups.Lats to 0.8f, MuscleGroups.Biceps to 0.6f),
    "shrug|barbell shrug|db shrug" to listOf(MuscleGroups.Traps to 0.9f),
    "pullover|lat pullover" to listOf(MuscleGroups.Lats to 0.6f, MuscleGroups.Pecs to 0.3f),
    "curl|biceps|bicep curl|barbell curl|ez bar curl|preacher curl|hammer curl|incline curl|concentration curl|cable curl|spider curl" to listOf(MuscleGroups.Biceps to 0.5f, MuscleGroups.Forearms to 0.25f),
    "reverse curl|wrist curl|forearm curl" to listOf(MuscleGroups.Forearms to 0.5f, MuscleGroups.Biceps to 0.25f),
    "tricep( extension)?|skullcrusher|overhead extension|rope pushdown|cable pushdown|v[- ]?bar pushdown" to listOf(MuscleGroups.Triceps to 0.5f),
    "back squat|front squat|squat|hack squat|smith squat|goblet squat|zercher squat|overhead squat" to listOf(MuscleGroups.Quads to 1.0f, MuscleGroups.Glutes to 0.8f, MuscleGroups.Hamstrings to 0.6f, MuscleGroups.LowerBack to 0.4f),
    "lunge|walking lunge|reverse lunge|split squat|bulgarian split squat|cossack squat" to listOf(MuscleGroups.Quads to 0.8f, MuscleGroups.Glutes to 0.8f, MuscleGroups.Hamstrings to 0.5f),
    "step[- ]?up|box step" to listOf(MuscleGroups.Quads to 0.7f, MuscleGroups.Glutes to 0.6f),
    "leg press" to listOf(MuscleGroups.Quads to 0.9f, MuscleGroups.Glutes to 0.7f, MuscleGroups.Hamstrings to 0.5f),
    "deadlift|conventional deadlift|sumo deadlift|trap bar deadlift" to listOf(MuscleGroups.Hamstrings to 1.0f, MuscleGroups.Glutes to 0.9f, MuscleGroups.LowerBack to 0.8f, MuscleGroups.UpperBack to 0.4f),
    "rdl|romanian deadlift|stiff[- ]?leg deadlift" to listOf(MuscleGroups.Hamstrings to 0.9f, MuscleGroups.Glutes to 0.8f, MuscleGroups.LowerBack to 0.6f),
    "good morning" to listOf(MuscleGroups.Hamstrings to 0.7f, MuscleGroups.LowerBack to 0.8f, MuscleGroups.Glutes to 0.5f),
    "hip thrust|glute bridge|barbell hip thrust|single[- ]?leg hip thrust" to listOf(MuscleGroups.Glutes to 0.9f, MuscleGroups.Hamstrings to 0.6f),
    "leg extension" to listOf(MuscleGroups.Quads to 0.4f),
    "leg curl|hamstring curl|seated leg curl|lying leg curl" to listOf(MuscleGroups.Hamstrings to 0.4f),
    "calf raise|standing calf|seated calf|donkey calf" to listOf(MuscleGroups.Calves to 0.5f),
    "hip abduction|abductor machine" to listOf(MuscleGroups.Glutes to 0.4f),
    "hip adduction|adductor machine" to listOf(MuscleGroups.Quads to 0.25f, MuscleGroups.Hamstrings to 0.25f),
    "plank|side plank|hollow hold" to listOf(MuscleGroups.Abs to 0.4f, MuscleGroups.LowerBack to 0.25f),
    "crunch|sit[- ]?up|cable crunch|machine crunch" to listOf(MuscleGroups.Abs to 0.4f),
    "leg raise|hanging leg raise|reverse crunch" to listOf(MuscleGroups.Abs to 0.45f),
    "ab rollout|ab wheel" to listOf(MuscleGroups.Abs to 0.5f, MuscleGroups.LowerBack to 0.3f),
    "russian twist|woodchop|pallof press" to listOf(MuscleGroups.Abs to 0.4f),
    "back extension|hyperextension" to listOf(MuscleGroups.LowerBack to 0.6f, MuscleGroups.Glutes to 0.4f, MuscleGroups.Hamstrings to 0.4f)
).map { (pat, gs) -> pat.toRegex(RegexOption.IGNORE_CASE) to gs }

private fun stimulusFor(entry: WorkoutSummary, profile: UserProfile): Map<MuscleGroups, Float> {
    val tokens = if (entry.exercises.isNotEmpty()) entry.exercises else listOf(entry.name)
    val per = mutableMapOf<MuscleGroups, Float>()
    tokens.forEach { raw ->
        val token = raw.lowercase(Locale.US)
        val eff = effectiveSetsFromToken(token, profile)
        nameToMusclesWeighted.forEach { (rx, pairs) ->
            if (rx.containsMatchIn(token)) {
                pairs.forEach { (m, wt) ->
                    per[m] = (per[m] ?: 0f) + eff * wt
                }
            }
        }
    }
    return per
}

private fun buildFrequencyHints(now: Instant, recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(28))
    val month = recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }.sortedBy { it.date }
    val byDay = month.groupBy { it.date.truncatedTo(ChronoUnit.DAYS) }
    val trainedDaysPerMuscle = mutableMapOf<MuscleGroups, Int>().withDefault { 0 }
    byDay.forEach { (_, entries) ->
        val hit = mutableSetOf<MuscleGroups>()
        entries.forEach { w ->
            val per = stimulusFor(w, profile)
            per.filterValues { it > 0.2f }.keys.forEach { hit += it }
        }
        hit.forEach { m -> trainedDaysPerMuscle[m] = trainedDaysPerMuscle.getValue(m) + 1 }
    }
    val weeks = 4f
    return MuscleGroups.entries.associateWith { m ->
        (trainedDaysPerMuscle.getValue(m) / weeks).coerceAtLeast(0f)
    }
}

private fun weeklyEffectiveSets(now: Instant, recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(7))
    val week = recent.filter { it.date.isAfter(start) && it.date.isBefore(now) }
    val acc = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    week.forEach { w ->
        val per = stimulusFor(w, profile)
        per.forEach { (m, v) -> acc[m] = acc.getValue(m) + v }
    }
    return acc
}

private fun windowEffectiveSets(start: Instant, end: Instant, recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Float> {
    val window = recent.filter { it.date.isAfter(start) && it.date.isBefore(end) }
    val acc = mutableMapOf<MuscleGroups, Float>().withDefault { 0f }
    window.forEach { w ->
        val per = stimulusFor(w, profile)
        per.forEach { (m, v) -> acc[m] = acc.getValue(m) + v }
    }
    return acc
}

private fun dailyEffectiveSets(now: Instant, recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofDays(1))
    return windowEffectiveSets(start, now, recent, profile)
}

private fun recentAcuteStimulus(now: Instant, horizonHours: Long, recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Float> {
    val start = now.minus(Duration.ofHours(horizonHours))
    return windowEffectiveSets(start, now, recent, profile)
}

private fun buildLastTrainedMap(recent: List<WorkoutSummary>, profile: UserProfile): Map<MuscleGroups, Instant> {
    val last = mutableMapOf<MuscleGroups, Instant>()
    recent.sortedBy { it.date }.forEach { w ->
        val per = stimulusFor(w, profile)
        per.forEach { (m, v) ->
            if (v > 0.15f) last[m] = w.date
        }
    }
    return last
}

private data class LifestyleSignals(
    val sleepOk: Boolean,
    val sleepAvg28d: Float?,
    val rhrOk: Boolean,
    val rhrLatest: Long?,
    val spo2Ok: Boolean,
    val spo2Avg: Double?,
    val proteinOk: Boolean,
    val proteinLast24g: Double,
    val proteinTargetG: Int
)

private fun lifestyleSignalsFrom(profile: UserProfile, factors: RecoveryFactors): LifestyleSignals {
    val sleepAvg = factors.sleepHours
    val rhr = factors.restingHeartRate
    val spo2 = factors.sleepSpo2
    val protein = factors.proteinGrams ?: 0.0
    val protTarget = (profile.weightKg * 1.6).toInt()
    val sleepOk = (sleepAvg ?: 7.2f) >= 7.0f
    val rhrOk = (rhr ?: 65L) < 70L
    val spo2Ok = (spo2 ?: 96.0) >= 95.0
    val proteinOk = protein >= protTarget * 0.9
    return LifestyleSignals(
        sleepOk = sleepOk,
        sleepAvg28d = sleepAvg,
        rhrOk = rhrOk,
        rhrLatest = rhr,
        spo2Ok = spo2Ok,
        spo2Avg = spo2,
        proteinOk = protein.isFinite() && proteinOk,
        proteinLast24g = protein,
        proteinTargetG = protTarget
    )
}
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> = withContext(Dispatchers.Default) {
    val sleepSpo2Recent = run {
        val sleepWindowStart = now.minus(Duration.ofDays(7))
        val sleeps = sleepSessions.filter { it.startTime.isAfter(sleepWindowStart) && it.startTime.isBefore(now) }
        if (sleeps.isEmpty()) null
        else {
            val vals = oxygenSaturations.filter { r ->
                sleeps.any { s -> r.time.isAfter(s.startTime) && r.time.isBefore(s.endTime) }
            }.map { it.percentage.value }
            vals.average().let { if (it.isNaN()) null else it }
        }
    }
    val proteinLast24h = run {
        val start24 = now.minus(Duration.ofHours(24))
        nutrition.filter { it.startTime.isAfter(start24) && it.startTime.isBefore(now) }
            .sumOf { it.protein?.inGrams ?: 0.0 }
    }
    val avgHrLast24: Long? = run {
        val start24 = now.minus(Duration.ofHours(24))
        val hrs = readHeartRate(start24, now)
        val samples = hrs.flatMap { it.samples }.map { it.beatsPerMinute.toDouble() }
        val avg = samples.average()
        if (samples.isEmpty() || avg.isNaN()) null else kotlin.math.round(avg).toLong()
    }
    val recovery = RecoveryFactors(
        sleepHours = sleepHoursInLast(sleepSessions, now, 28L * 24L),
        restingHeartRate = avgHrLast24,
        sleepSpo2 = sleepSpo2Recent,
        proteinGrams = proteinLast24h,
        bodyFatPercentage = bodyFat.maxByOrNull { it.time }?.percentage?.value
    )
    val weekly = weeklyEffectiveSets(now, recent, profile)
    val lastTrained = buildLastTrainedMap(recent, profile)
    val acute12h = recentAcuteStimulus(now, 12, recent, profile)
    val rMult = recovery.totalMultiplier
    val softCapMult = (0.95f + (rMult - 1f) * 0.15f).coerceIn(0.85f, 1.1f)
    val loads = MuscleGroups.entries.map { m ->
        val target = computeTargets(profile, m).target
        val wk = (weekly[m] ?: 0f)
        val last = lastTrained[m]
        val hoursSince = last?.let { ChronoUnit.HOURS.between(it, now).coerceAtLeast(0) } ?: Long.MAX_VALUE
        val acute = (acute12h[m] ?: 0f)
        val recWindow = (6f + acute.coerceAtMost(10f)).coerceIn(4f, 16f)
        val pct = if (target <= 0f) 0f else wk / target
        val band = when {
            last == null || ChronoUnit.DAYS.between(last, now) >= 21L -> LoadBand.NotTrained
            acute > 0.25f && hoursSince < recWindow -> LoadBand.Recovering
            pct < 0.25f -> LoadBand.SlightlyTrained
            pct < 0.75f -> LoadBand.Building
            pct <= 1.10f -> LoadBand.OnTrack
            pct > 1.60f && rMult < 0.96f -> LoadBand.DeloadRecommended
            pct > 1.40f -> LoadBand.Overreached
            else -> LoadBand.OnTrack
        }
        val softCap = target * 1.6f * softCapMult
        val score = (wk / softCap).coerceIn(0f, 1f)
        MuscleLoad(group = m, weeklyProgress = wk, weeklyTarget = target, band = band, score = score, lastTrainedAgo = last?.let { friendlyAgo(now, it) })
    }
    loads to recovery
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun deriveMuscleLoadsStepwise(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?,
    onStep: (List<MuscleLoad>, RecoveryFactors, Int, Int) -> Unit,
    sleepSessions: List<SleepSessionRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>,
    readHeartRate: suspend (Instant, Instant) -> List<HcHeartRateRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> {
    val (final, factors) = deriveMuscleLoads(now, recent, profile, aiText, sleepSessions, oxygenSaturations, nutrition, bodyFat, readHeartRate)
    val total = final.size
    val acc = mutableListOf<MuscleLoad>()
    for ((i, item) in final.sortedBy { it.group.name }.withIndex()) {
        acc += item
        onStep(acc.toList(), factors, i + 1, total)
        delay(45)
    }
    return acc to factors
}

private fun sleepHoursInLast(records: List<SleepSessionRecord>, now: Instant, lookbackHours: Long): Float {
    if (lookbackHours <= 0L) return 0f
    val start = now.minus(Duration.ofHours(lookbackHours))
    var total = 0L
    records.forEach { s ->
        val st = if (s.startTime.isBefore(start)) start else s.startTime
        val en = if (s.endTime.isAfter(now)) now else s.endTime
        if (en.isAfter(st)) total += Duration.between(st, en).toMinutes()
    }
    val days = lookbackHours / 24f
    return if (days > 0f) (total / 60f) / days else 0f
}
private data class Mission(
    val group: MuscleGroups,
    val setsGoalToday: Int,
    val setsDoneToday: Int,
    val priorityScore: Float,
    val reason: String,
    val exercises: List<String> = emptyList(),
    val lifestyleHints: List<String> = emptyList()
)

private fun recentExercisesByMuscle(recent: List<WorkoutSummary>, profile: UserProfile, limitDays: Long = 45): Map<MuscleGroups, List<String>> {
    val cutoff = Instant.now().minus(Duration.ofDays(limitDays))
    val buckets = mutableMapOf<MuscleGroups, MutableMap<String, Float>>()
    recent.filter { it.date.isAfter(cutoff) }.forEach { w ->
        val tokens = if (w.exercises.isNotEmpty()) w.exercises else listOf(w.name)
        tokens.forEach { raw ->
            val token = raw.trim()
            val low = token.lowercase(Locale.US)
            val eff = effectiveSetsFromToken(low, profile)
            nameToMusclesWeighted.forEach { (rx, pairs) ->
                if (rx.containsMatchIn(low)) {
                    pairs.forEach { (m, wt) ->
                        val map = buckets.getOrPut(m) { mutableMapOf() }
                        map[token] = (map[token] ?: 0f) + eff * wt
                    }
                }
            }
        }
    }
    return buckets.mapValues { (_, m) ->
        m.entries.sortedByDescending { it.value }.map { it.key }.distinct().take(6)
    }
}

private val defaultExercises: Map<MuscleGroups, List<String>> = mapOf(
    MuscleGroups.Pecs to listOf("Barbell Bench Press", "Incline Dumbbell Press", "Cable Fly"),
    MuscleGroups.Delts to listOf("Overhead Press", "Lateral Raise", "Face Pull"),
    MuscleGroups.Biceps to listOf("EZ-Bar Curl", "Incline DB Curl", "Hammer Curl"),
    MuscleGroups.Triceps to listOf("Rope Pushdown", "Overhead Extension", "Close-Grip Bench"),
    MuscleGroups.Lats to listOf("Pull-Up", "Lat Pulldown", "Seated Cable Row"),
    MuscleGroups.Traps to listOf("Barbell Shrug", "Face Pull", "Farmer’s Carry"),
    MuscleGroups.Abs to listOf("Hanging Leg Raise", "Cable Crunch", "Ab Wheel"),
    MuscleGroups.Forearms to listOf("Reverse Curl", "Wrist Curl", "Farmer’s Carry"),
    MuscleGroups.Quads to listOf("Back Squat", "Leg Press", "Bulgarian Split Squat"),
    MuscleGroups.Hamstrings to listOf("Romanian Deadlift", "Lying Leg Curl", "Good Morning"),
    MuscleGroups.Glutes to listOf("Hip Thrust", "Lunge", "Step-Up"),
    MuscleGroups.Calves to listOf("Standing Calf Raise", "Seated Calf Raise", "Donkey Calf"),
    MuscleGroups.LowerBack to listOf("Back Extension", "RDL (light)", "Bird Dog"),
    MuscleGroups.UpperBack to listOf("Barbell Row", "Chest-Supported Row", "Face Pull"),
)

private fun chooseExercisesFor(m: MuscleGroups, recentMap: Map<MuscleGroups, List<String>>, maxItems: Int = 3): List<String> {
    val fromRecent = recentMap[m].orEmpty().take(maxItems)
    if (fromRecent.isNotEmpty()) return fromRecent
    return defaultExercises[m].orEmpty().take(maxItems)
}

private enum class BodyCategory { All, UpperBody, Arms, Core, LowerBody }
private fun categoryOf(name: String): BodyCategory = when (name.lowercase()) {
    "pecs", "delts", "lats", "traps", "upperback", "upper back" -> BodyCategory.UpperBody
    "biceps", "triceps", "forearms" -> BodyCategory.Arms
    "abs", "lowerback", "lower back" -> BodyCategory.Core
    "quads", "hamstrings", "glutes", "calves" -> BodyCategory.LowerBody
    else -> BodyCategory.UpperBody
}

private data class MuscleInsight(
    val growthPct: Float,
    val growthLabel: String,
    val fatigue: Float,
    val readinessText: String,
    val suggestedRpeMin: Int,
    val suggestedRpeMax: Int,
    val recommendation: String
)

private fun twoWindowGrowth(now: Instant, recent: List<WorkoutSummary>, profile: UserProfile): Pair<Map<MuscleGroups, Float>, Map<MuscleGroups, Float>> {
    val curStart = now.minus(Duration.ofDays(14))
    val prevStart = now.minus(Duration.ofDays(28))
    val prev = windowEffectiveSets(prevStart, curStart, recent, profile)
    val cur = windowEffectiveSets(curStart, now, recent, profile)
    return prev to cur
}

private fun fatigueEstimate(m: MuscleGroups, now: Instant, lastTrained: Map<MuscleGroups, Instant>, acute24: Map<MuscleGroups, Float>, recovery: RecoveryFactors): Float {
    val last = lastTrained[m]
    val hours = last?.let { ChronoUnit.HOURS.between(it, now).coerceAtLeast(0) } ?: 999L
    val acute = (acute24[m] ?: 0f)
    val decay = when {
        hours < 6 -> 1f
        hours < 24 -> 0.75f
        hours < 36 -> 0.55f
        hours < 48 -> 0.42f
        else -> 0.28f
    }
    val rec = recovery.totalMultiplier
    val base = (acute / 6f).coerceIn(0f, 1.4f) * decay
    val recAdj = when {
        rec >= 1.10f -> 0.85f
        rec >= 1.03f -> 0.92f
        rec <= 0.92f -> 1.18f
        rec <= 0.98f -> 1.08f
        else -> 1f
    }
    return (base * recAdj).coerceIn(0f, 1f)
}

private fun insightForMuscle(
    m: MuscleGroups,
    now: Instant,
    profile: UserProfile,
    weekly: Map<MuscleGroups, Float>,
    lastTrained: Map<MuscleGroups, Instant>,
    prev14: Map<MuscleGroups, Float>,
    cur14: Map<MuscleGroups, Float>,
    acute24: Map<MuscleGroups, Float>,
    recovery: RecoveryFactors,
    mission: Mission?
): MuscleInsight {
    val target = computeTargets(profile, m).target
    val cur = cur14[m] ?: 0f
    val prev = prev14[m] ?: 0f
    val growthPct = if (prev <= 0.01f) if (cur > 0f) 100f else 0f else ((cur - prev) / prev * 100f).coerceIn(-100f, 200f)
    val growthLabel = when {
        growthPct > 12f -> "rising"
        growthPct < -8f -> "falling"
        else -> "stable"
    }
    val fatigue = fatigueEstimate(m, now, lastTrained, acute24, recovery)
    val dose = (weekly[m] ?: 0f)
    val pct = if (target <= 0f) 0f else dose / target
    val readinessText = when {
        fatigue > 0.75f -> "fatigued"
        fatigue > 0.5f -> "warm but workable"
        pct < 0.25f -> "needs volume"
        pct in 0.25f..0.75f -> "good to build"
        pct in 0.75f..1.1f -> "maintain or top-up"
        pct > 1.4f -> "overreached"
        else -> "on track"
    }
    val rpeMinMax = when {
        fatigue > 0.8f -> 6 to 7
        fatigue > 0.6f -> 6 to 8
        pct > 1.3f -> 6 to 7
        pct < 0.5f -> 7 to 9
        else -> 7 to 8
    }
    val rec = when {
        pct > 1.6f -> "deload or pump work, higher RIR"
        pct > 1.3f -> "reduce sets, keep technique crisp"
        fatigue > 0.75f -> "light work only, focus on form"
        growthPct < -8f && pct < 0.7f -> "add a set and keep RPE ${rpeMinMax.first}"
        growthPct > 12f && pct < 0.9f -> "keep momentum, similar plan"
        pct < 0.25f -> "prioritize today with extra sets"
        else -> "standard progression"
    }
    val suggested = mission?.let { it.setsGoalToday } ?: ((target / 7f) * when {
        pct < 0.25f -> 1.6f
        pct < 0.5f -> 1.3f
        pct < 0.75f -> 1.15f
        pct < 0.95f -> 1.0f
        else -> 0.6f
    }).roundToInt().coerceIn(2, 10)
    return MuscleInsight(growthPct = growthPct, growthLabel = growthLabel, fatigue = fatigue, readinessText = "$readinessText • $suggested sets", suggestedRpeMin = rpeMinMax.first, suggestedRpeMax = rpeMinMax.second, recommendation = rec)
}

// 2) Add this missing composable
@Composable
private fun CategoryFilterBar(selected: BodyCategory, onSelect: (BodyCategory) -> Unit) {
    val items = listOf(
        BodyCategory.All to "All",
        BodyCategory.UpperBody to "Upper",
        BodyCategory.Arms to "Arms",
        BodyCategory.Core to "Core",
        BodyCategory.LowerBody to "Lower"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (cat, label) ->
            val isSelected = selected == cat
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "")
            val pillRadius = 32.dp
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(pillRadius))
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(cat) }
                    .drawWithCache {
                        val bgBrush =
                            if (isSelected) Brush.radialGradient(
                                listOf(Color(0xFF2C0404), Color(0xFF2B0404)),
                                radius = size.minDimension * 2f
                            ) else Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                                radius = size.minDimension * 2f
                            )
                        val borderBrush =
                            if (isSelected) Brush.linearGradient(
                                listOf(
                                    Color(0xFFFF5555).copy(alpha = 0.6f),
                                    Color(0xFF8B0000).copy(alpha = 0.45f)
                                )
                            ) else Brush.radialGradient(
                                listOf(Color(0x00171717), Color(0xFF525252)),
                                radius = size.minDimension * 2f
                            )
                        onDrawBehind {
                            drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(pillRadius.toPx()))
                            drawRoundRect(
                                brush = borderBrush,
                                style = Stroke(width = 1.dp.toPx()),
                                cornerRadius = CornerRadius(pillRadius.toPx())
                            )
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSelected) Color(0xFFF8BDC1) else Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
private fun planDailyMissions(
    now: Instant,
    profile: UserProfile,
    loads: List<MuscleLoad>,
    weekly: Map<MuscleGroups, Float>,
    daily: Map<MuscleGroups, Float>,
    lastTrained: Map<MuscleGroups, Instant>,
    recentExerciseMap: Map<MuscleGroups, List<String>>,
    lifestyle: LifestyleSignals,
    maxMissions: Int = 4
): List<Mission> {
    fun hoursSince(m: MuscleGroups): Long =
        lastTrained[m]?.let { ChronoUnit.HOURS.between(it, now).coerceAtLeast(0) } ?: Long.MAX_VALUE

    val priorityBoost = profile.importantMuscles.associateWith { 1.15f }

    val missions = loads.mapNotNull { ml ->
        val wk = weekly[ml.group] ?: 0f
        val day = daily[ml.group] ?: 0f
        val pct = if (ml.weeklyTarget <= 0f) 0f else wk / ml.weeklyTarget
        val hrs = hoursSince(ml.group)

        if (ml.band == LoadBand.DeloadRecommended) return@mapNotNull null
        if (ml.band == LoadBand.Recovering && hrs < 6) return@mapNotNull null

        val boost = priorityBoost[ml.group] ?: 1f
        val fatiguePenalty =
            (if (lifestyle.sleepOk) 0f else 0.12f) +
                    (if (lifestyle.proteinOk) 0f else 0.08f) +
                    (if (lifestyle.rhrOk) 0f else 0.06f) +
                    (if (lifestyle.spo2Ok) 0f else 0.05f)

        val basePerDay = (ml.weeklyTarget / 7f)
        val bump = when {
            pct < 0.25f -> 0.85f
            pct < 0.50f -> 0.55f
            pct < 0.75f -> 0.35f
            pct < 0.95f -> 0.18f
            else -> 0.08f
        }
        val goal = (basePerDay * (1f + bump) * boost * (1f - fatiguePenalty))
            .coerceIn(2f, 10f).toInt()

        val reason = when (ml.band) {
            LoadBand.NotTrained -> "No recent work — quick primer."
            LoadBand.SlightlyTrained -> "Barely touched this week — easy win."
            LoadBand.Building -> "Momentum going — stack smart volume."
            LoadBand.OnTrack -> "On pace — lock it in."
            LoadBand.Recovering -> "Recovered enough — technique top-ups."
            LoadBand.Overreached -> "Volume high — keep it sub-max."
            LoadBand.DeloadRecommended -> "Deload flagged."
        }

        val lifestyleHints = buildList {
            if (!lifestyle.sleepOk) add("Cap at RPE 7, longer rest")
            if (!lifestyle.proteinOk) add("Aim ~${lifestyle.proteinTargetG}g protein")
            if (!lifestyle.rhrOk) add("RHR up — avoid grinders")
            if (!lifestyle.spo2Ok) add("Shorter sets, nasal breathing")
            if (ml.group in profile.importantMuscles) add("Priority muscle — protect form")
        }

        val deficit = (1f - pct).coerceIn(0f, 1.2f)
        val bandWeight = when (ml.band) {
            LoadBand.NotTrained -> 1.0f
            LoadBand.SlightlyTrained -> 0.9f
            LoadBand.Building -> 0.8f
            LoadBand.OnTrack -> 0.6f
            LoadBand.Recovering -> 0.3f
            LoadBand.Overreached -> 0.2f
            LoadBand.DeloadRecommended -> -1f
        }
        if (bandWeight <= 0f) return@mapNotNull null

        val recencyBonus = (hrs / 72f).coerceIn(0f, 1f) * 0.35f
        val score = (bandWeight + 0.7f * deficit + recencyBonus - (ml.score * 0.2f)) * (1f - fatiguePenalty)

        Mission(
            group = ml.group,
            setsGoalToday = goal,
            setsDoneToday = day.toInt().coerceAtLeast(0),
            priorityScore = score,
            reason = reason,
            exercises = chooseExercisesFor(ml.group, recentExerciseMap, 3),
            lifestyleHints = lifestyleHints
        )
    }

    return missions.sortedByDescending { it.priorityScore }.take(maxMissions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleStatusSection(
    recentWorkouts: List<WorkoutSummary>,
    advicePayload: String?,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier,
    weeklySummaryAvailable: Boolean = false,
    onOpenWeeklySummary: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var loads by remember { mutableStateOf<List<MuscleLoad>>(emptyList()) }
    var recoveryFactors by remember { mutableStateOf<RecoveryFactors?>(null) }
    var computing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val prefsManager = remember { UserPreferencesManager(context) }
    val profile = remember { buildUserProfile(prefsManager) }
    val recomputeKey = remember(recentWorkouts, advicePayload, profile) {
        val w = recentWorkouts.joinToString("|") { "${it.date.toEpochMilli()}#${it.name}#${it.exercises.joinToString(",")}" }
        val p = "${profile.age}-${profile.heightCm}-${profile.weightKg}-${profile.experience}-${profile.preferredStyle}-${profile.importantMuscles.joinToString(",")}"
        (w + "@" + (advicePayload ?: "") + "@" + p).hashCode()
    }
    LaunchedEffect(recomputeKey) {
        computing = true
        progress = 0f
        loads = emptyList()
        val now = Instant.ofEpochMilli(nowEpochMillis)
        frequencyHint = buildFrequencyHints(now, recentWorkouts, profile)
        val start = now.minus(30, ChronoUnit.DAYS)
        val sleep = healthConnectManager.readSleepSessions(start, now)
        val spo2 = healthConnectManager.readOxygenSaturation(start, now)
        val nutrition = healthConnectManager.readNutrition(now.minus(2, ChronoUnit.DAYS), now)
        val bodyfat = healthConnectManager.readBodyFat(start, now)
        val (finalLoads, factors) = deriveMuscleLoadsStepwise(
            now = now,
            recent = recentWorkouts,
            profile = profile,
            aiText = advicePayload,
            onStep = { partial, fac, done, total ->
                loads = partial
                recoveryFactors = fac
                progress = done.toFloat() / total.toFloat()
            },
            sleepSessions = sleep,
            oxygenSaturations = spo2,
            nutrition = nutrition,
            bodyFat = bodyfat,
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
    val weekSets = remember(recentWorkouts, profile, nowEpochMillis) { weeklyEffectiveSets(now, recentWorkouts, profile) }
    val daySets = remember(recentWorkouts, profile, nowEpochMillis) { dailyEffectiveSets(now, recentWorkouts, profile) }
    val lastTrained = remember(recentWorkouts, profile) { buildLastTrainedMap(recentWorkouts, profile) }
    val recentMap = remember(recentWorkouts, profile) { recentExercisesByMuscle(recentWorkouts, profile) }
    val lifestyle = remember(recoveryFactors) { recoveryFactors?.let { lifestyleSignalsFrom(profile, it) } }

    val missions: List<Mission> = remember(loads, weekSets, daySets, lastTrained, recentMap, lifestyle, nowEpochMillis) {
        if (lifestyle == null) emptyList<Mission>() else planDailyMissions(
            now = now,
            profile = profile,
            loads = loads,
            weekly = weekSets,
            daily = daySets,
            lastTrained = lastTrained,
            recentExerciseMap = recentMap,
            lifestyle = lifestyle
        )
    }

    val (prev14, cur14) = remember(recentWorkouts, profile, nowEpochMillis) { twoWindowGrowth(now, recentWorkouts, profile) }
    val acute24 = remember(recentWorkouts, profile, nowEpochMillis) { recentAcuteStimulus(now, 24, recentWorkouts, profile) }
    val filtered = remember(loads, selected) { if (selected == BodyCategory.All) loads else loads.filter { categoryOf(it.group.name) == selected } }
    val sortedLoads = remember(filtered) {
        filtered.sortedWith(
            compareBy<MuscleLoad> {
                when (it.band) {
                    LoadBand.DeloadRecommended -> 0
                    LoadBand.Overreached -> 1
                    LoadBand.Recovering -> 2
                    LoadBand.OnTrack -> 3
                    LoadBand.Building -> 4
                    LoadBand.SlightlyTrained -> 5
                    LoadBand.NotTrained -> 6
                }
            }.thenByDescending { it.score }
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val borderBrush = Brush.horizontalGradient(colors = listOf(Color(0xFF8B0000), Color.LightGray))
            FilledTonalButton(
                onClick = onOpenWeeklySummary,
                contentPadding = PaddingValues(8.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF4A0000).copy(alpha = 0.4f), contentColor = Color(0xFFF48A8A)),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFFF5555).copy(alpha = 0.6f), Color(0xFF8B0000).copy(alpha = 0.45f)))),
                enabled = true
            ) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.padding(2.dp))
                Text("Weekly Summary")
            }
            FilledTonalButton(
                onClick = {
                    if (recoveryFactors != null) {
                        showHealth = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                contentPadding = PaddingValues(8.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2B0404).copy(alpha = 0.6f), contentColor = Color(0xFFF8BDC1)),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFFF5555).copy(alpha = 0.6f), Color(0xFF8B0000).copy(alpha = 0.45f))))
            ) {
                Icon(Icons.Outlined.MonitorHeart, null, modifier = Modifier.padding(end = 6.dp))
                Text("Recovery Details")
            }
        }
        if (recoveryFactors != null) RecoveryFactorRow(recoveryFactors!!, onClick = {
            showHealth = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        })
        OverviewRow(loads = filtered)
        CategoryFilterBar(
            selected = selected,
            onSelect = {
                selected = it
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            }
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            sortedLoads.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 0.dp, max = 650.dp)
                        .weight(1f, fill = false)
                        .clickable {
                            selectedMuscle = item
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                ) { MuscleCircleTile(load = item) }
            }
        }
    }
    if (showHealth && recoveryFactors != null) {
        RecoveryDetailSheet(state = sheetState, onDismiss = { showHealth = false }, factors = recoveryFactors!!)
    }
    selectedMuscle?.let { ml ->
        val mission = missions.firstOrNull { it.group == ml.group }
        val insight = insightForMuscle(
            m = ml.group,
            now = now,
            profile = profile,
            weekly = weekSets,
            lastTrained = lastTrained,
            prev14 = prev14,
            cur14 = cur14,
            acute24 = acute24,
            recovery = recoveryFactors ?: RecoveryFactors(null, null, null, null, null),
            mission = mission
        )
        MuscleDetailSheet(
            state = sheetState,
            onDismiss = { selectedMuscle = null },
            load = ml,
            insight = insight,
            exercises = chooseExercisesFor(ml.group, recentMap, 3)
        )
    }
}

@Composable
private fun RecoveryFactorRow(factors: RecoveryFactors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (factors.sleepHours != null) FactorPill(value = (factors.sleepMultiplier - 1f) * 100f, label = "Sleep")
        if (factors.restingHeartRate != null) FactorPill(value = (factors.rhrMultiplier - 1f) * 100f, label = "Resting HR")
        if (factors.sleepSpo2 != null) FactorPill(value = (factors.spo2Multiplier - 1f) * 100f, label = "Sleep SpO2")
        if (factors.proteinGrams != null) FactorPill(value = (factors.proteinMultiplier - 1f) * 100f, label = "Protein")
        if (factors.bodyFatPercentage != null) FactorPill(value = (factors.bodyFatMultiplier - 1f) * 100f, label = "Body Comp")
    }
}

@Composable
private fun FactorPill(value: Float, label: String) {
    val isPositive = value >= 0
    val color = if (isPositive) Color(0xFF00E676) else Color(0xFFFF3B30)
    val valueText = String.format(Locale.US, "%+.0f%%", value)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = if (isPositive) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Text(text = valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun LoadingScreen(progress: Float, modifier: Modifier = Modifier) {
    val cornerRadius = 22.dp
    val density = LocalDensity.current
    val cornerRpx = with(density) { cornerRadius.toPx() }
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    val neonPhase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2500)), label = "neon_phase")
    Column(
        modifier = modifier
            .padding(16.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val bgBrush = Brush.radialGradient(listOf(Color(0xFF3A0E0E), Color(0xFF120707)), center = Offset(size.width / 2f, size.height / 2f), radius = size.minDimension * 1.1f)
                val sweepX = size.width * (neonPhase * 1.5f - 0.25f)
                val neonCore = Brush.linearGradient(colors = listOf(Color.Transparent, Color(0xFFFF5E55).copy(alpha = 0.8f), Color.Transparent), start = Offset(sweepX - size.width * 0.25f, 0f), end = Offset(sweepX, size.height))
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                    drawRoundRect(brush = neonCore, style = Stroke(width = 2.dp.toPx()), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                }
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Analyzing muscle status...", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        val barBrush = Brush.horizontalGradient(listOf(Color(0xFFFF3B30).copy(alpha = 0.7f), Color(0xFFFF7A59)))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f))) {
            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(barBrush)
                    .drawWithCache {
                        val glowBrush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), center = Offset(size.width * 0.9f, size.height / 2f), radius = size.height * 2.5f)
                        onDrawBehind { drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(10.dp.toPx())) }
                    }
            )
        }
        Text("${(progress * 100f).coerceIn(0f, 100f).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChipRow(items: List<String>, icon: ImageVector, tint: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { text ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewRow(loads: List<MuscleLoad>) {
    val notCount = remember(loads) { loads.count { it.band == LoadBand.NotTrained } }
    val slightCount = remember(loads) { loads.count { it.band == LoadBand.SlightlyTrained } }
    val buildCount = remember(loads) { loads.count { it.band == LoadBand.Building } }
    val onTrackCount = remember(loads) { loads.count { it.band == LoadBand.OnTrack } }
    val recCount = remember(loads) { loads.count { it.band == LoadBand.Recovering } }
    val overCount = remember(loads) { loads.count { it.band == LoadBand.Overreached } }
    val deloadCount = remember(loads) { loads.count { it.band == LoadBand.DeloadRecommended } }
    val cornerRadius = 18.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius))
            .padding(12.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notCount > 0) MiniStatPill("Not Trained", notCount.toString(), Color(0xFF9E9E9E))
            if (slightCount > 0) MiniStatPill("Slightly", slightCount.toString(), Color(0xFF2979FF))
            if (buildCount > 0) MiniStatPill("Building", buildCount.toString(), Color(0xFF42A5F5))
            if (onTrackCount > 0) MiniStatPill("On Track", onTrackCount.toString(), Color(0xFF00E676))
            if (recCount > 0) MiniStatPill("Recovering", recCount.toString(), Color(0xFF6A0DAD))
            if (overCount > 0) MiniStatPill("Overreached", overCount.toString(), Color(0xFFFFA500))
            if (deloadCount > 0) MiniStatPill("Deload", deloadCount.toString(), Color(0xFFFF3B30))
        }
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint)
                .drawBehind {
                    drawCircle(brush = Brush.radialGradient(colors = listOf(tint.copy(alpha = 0.5f), Color.Transparent), radius = size.minDimension * 1.5f))
                }
        )
        Text(text = "$label: $value", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun MuscleCircleTile(load: MuscleLoad) {
    val bandColor = when (load.band) {
        LoadBand.NotTrained -> Color(0xFF9E9E9E)
        LoadBand.SlightlyTrained -> Color(0xFF2979FF)
        LoadBand.Building -> Color(0xFF42A5F5)
        LoadBand.OnTrack -> Color(0xFF00E676)
        LoadBand.Recovering -> Color(0xFF6A0DAD)
        LoadBand.Overreached -> Color(0xFFFFA500)
        LoadBand.DeloadRecommended -> Color(0xFFFF3B30)
    }
    val bandLabel = when (load.band) {
        LoadBand.NotTrained -> "Not Trained"
        LoadBand.SlightlyTrained -> "Slightly"
        LoadBand.Building -> "Building"
        LoadBand.OnTrack -> "On Track"
        LoadBand.Recovering -> "Recovering"
        LoadBand.Overreached -> "Overreached"
        LoadBand.DeloadRecommended -> "Deload"
    }
    val pct = if (load.weeklyTarget > 0f) ((load.weeklyProgress / load.weeklyTarget) * 100f).coerceIn(0f, 140f) else 0f
    val animatedPct by animateFloatAsState(targetValue = pct.coerceIn(0f, 100f), animationSpec = tween(700, easing = LinearOutSlowInEasing), label = "pct")
    val animatedColor by animateColorAsState(bandColor, animationSpec = tween(300), label = "tileColor")
    val r = 54.dp
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .drawWithCache {
                val bg = Brush.radialGradient(listOf(animatedColor.copy(alpha = 0.10f), Color(0xFF120707)), center = Offset(size.width / 2f, size.height * -0.2f), radius = size.width * 1.2f)
                val border = Brush.linearGradient(listOf(animatedColor.copy(alpha = 0.30f), animatedColor.copy(alpha = 0.10f)))
                onDrawBehind {
                    drawRoundRect(brush = bg, cornerRadius = CornerRadius(18.dp.toPx()))
                    drawRoundRect(brush = border, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(18.dp.toPx()))
                }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(r * 2)
                .drawBehind {
                    val strokeW = 10.dp.toPx()
                    val radius = size.minDimension / 2f
                    drawCircle(color = Color.White.copy(alpha = 0.06f), radius = radius)
                    drawArc(color = Color.White.copy(alpha = 0.10f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeW, cap = StrokeCap.Round), size = Size(size.width, size.height), topLeft = Offset(0f, 0f))
                    val sweep = animatedPct / 100f * 360f
                    drawArc(brush = Brush.sweepGradient(listOf(animatedColor.copy(alpha = 0.6f), animatedColor)), startAngle = -90f, sweepAngle = sweep, useCenter = false, style = Stroke(strokeW, cap = StrokeCap.Round), size = Size(size.width, size.height), topLeft = Offset(0f, 0f))
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(String.format(Locale.US, "%.0f%%", animatedPct), style = MaterialTheme.typography.titleMedium, color = animatedColor, fontWeight = FontWeight.Black)
                Text(bandLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Text(load.group.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val readiness = when (load.band) {
                LoadBand.NotTrained -> "Muscle isn't trained"
                LoadBand.SlightlyTrained -> "Ready for volume"
                LoadBand.Building -> "Keep building"
                LoadBand.OnTrack -> "Maintain"
                LoadBand.Recovering -> "Active recovery"
                LoadBand.Overreached -> "Light work"
                LoadBand.DeloadRecommended -> "Deload"
            }
            Text(readiness, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        }
        if (load.lastTrainedAgo != null) {
            Text("Last: ${load.lastTrainedAgo}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryDetailSheet(state: SheetState, onDismiss: () -> Unit, factors: RecoveryFactors) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Color(0xFF1A0A0A)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Recovery Details", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
            HealthMetricRow(icon = Icons.Outlined.NightlightRound, label = "Sleep (avg)", value = factors.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "—", pct = ((factors.sleepMultiplier - 0.85f) / (1.25f - 0.85f)).coerceIn(0f, 1f))
            HealthMetricRow(icon = Icons.Outlined.MonitorHeart, label = "Average HR", value = factors.restingHeartRate?.toString() ?: "—", pct = when {
                factors.restingHeartRate == null -> 0.60f
                factors.restingHeartRate < 60 -> 1.00f
                factors.restingHeartRate < 65 -> 0.95f
                factors.restingHeartRate < 70 -> 0.90f
                factors.restingHeartRate < 75 -> 0.85f
                factors.restingHeartRate < 80 -> 0.80f
                factors.restingHeartRate < 85 -> 0.72f
                factors.restingHeartRate < 90 -> 0.64f
                factors.restingHeartRate < 100 -> 0.54f
                else -> 0.42f
            })
            HealthMetricRow(icon = Icons.Outlined.WaterDrop, label = "SpO₂ (sleep)", value = factors.sleepSpo2?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—", pct = when {
                factors.sleepSpo2 == null -> 0.5f
                factors.sleepSpo2 >= 98.0 -> 1f
                factors.sleepSpo2 >= 95.0 -> 0.85f
                factors.sleepSpo2 >= 92.0 -> 0.65f
                else -> 0.4f
            })
            HealthMetricRow(icon = Icons.Outlined.Restaurant, label = "Protein last 24h", value = factors.proteinGrams?.let { String.format(Locale.US, "%.0f g", it) } ?: "—", pct = when {
                factors.proteinGrams == null -> 0.5f
                factors.proteinGrams >= 150.0 -> 1f
                factors.proteinGrams >= 100.0 -> 0.8f
                factors.proteinGrams >= 60.0 -> 0.6f
                else -> 0.35f
            })
            HealthMetricRow(icon = Icons.Outlined.Scale, label = "Body Fat", value = factors.bodyFatPercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—", pct = when {
                factors.bodyFatPercentage == null -> 0.5f
                factors.bodyFatPercentage in 12.0..20.0 -> 1f
                factors.bodyFatPercentage in 8.0..25.0 -> 0.8f
                else -> 0.5f
            })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HealthMetricRow(icon: ImageVector, label: String, value: String, pct: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawWithCache {
                val col = Color(0xFFFF5E55)
                val bg = Brush.radialGradient(listOf(col.copy(alpha = 0.10f), Color(0xFF120707)), radius = size.width * 0.9f)
                val border = Brush.linearGradient(listOf(col.copy(alpha = 0.28f), col.copy(alpha = 0.08f)))
                onDrawBehind {
                    val r = 16.dp.toPx()
                    drawRoundRect(brush = bg, cornerRadius = CornerRadius(r))
                    drawRoundRect(brush = border, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(r))
                }
            }
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = Color(0xFFF8BDC1))
                Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.85f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF8A80), Color(0xFFFF5252))))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDetailSheet(state: SheetState, onDismiss: () -> Unit, load: MuscleLoad, insight: MuscleInsight, exercises: List<String>) {
    val tint = when (load.band) {
        LoadBand.NotTrained -> Color(0xFF9E9E9E)
        LoadBand.SlightlyTrained -> Color(0xFF2979FF)
        LoadBand.Building -> Color(0xFF42A5F5)
        LoadBand.OnTrack -> Color(0xFF00E676)
        LoadBand.Recovering -> Color(0xFF6A0DAD)
        LoadBand.Overreached -> Color(0xFFFFA500)
        LoadBand.DeloadRecommended -> Color(0xFFFF3B30)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Color(0xFF1A0A0A)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(load.group.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pct = if (load.weeklyTarget > 0f) (load.weeklyProgress / load.weeklyTarget * 100f).coerceIn(0f, 200f) else 0f
                    Text(String.format(Locale.US, "%.0f%%", pct), style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Black)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightChip(label = "Growth ${String.format(Locale.US, "%+.0f%%", insight.growthPct)}", color = when (insight.growthLabel) {
                    "rising" -> Color(0xFF00E676)
                    "falling" -> Color(0xFFFF3B30)
                    else -> Color(0xFFFFA500)
                })
                InsightChip(label = "Fatigue ${(insight.fatigue * 100f).toInt()}%", color = if (insight.fatigue > 0.7f) Color(0xFFFF3B30) else if (insight.fatigue > 0.5f) Color(0xFFFFA500) else Color(0xFF00E676))
                InsightChip(label = "RPE ${insight.suggestedRpeMin}–${insight.suggestedRpeMax}", color = Color(0xFFF8BDC1))
            }
            Text(insight.readinessText, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.85f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .drawWithCache {
                        val bg = Brush.radialGradient(listOf(tint.copy(alpha = 0.10f), Color(0xFF120707)), radius = size.width * 1.1f)
                        val border = Brush.linearGradient(listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.08f)))
                        onDrawBehind {
                            val r = 16.dp.toPx()
                            drawRoundRect(brush = bg, cornerRadius = CornerRadius(r))
                            drawRoundRect(brush = border, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(r))
                        }
                    }
                    .padding(12.dp)
            ) {
                Text(insight.recommendation, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            if (exercises.isNotEmpty()) {
                Text("Suggested Work", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                ChipRow(items = exercises.map { "$it" }, icon = Icons.Outlined.FitnessCenter, tint = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InsightChip(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
    }
}
