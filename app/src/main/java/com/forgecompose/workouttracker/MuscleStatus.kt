package com.forgecompose.workouttracker

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.pow
import kotlin.math.round

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

enum class MuscleGroups {
    Pecs, Delts, Biceps, Triceps, Lats, Traps, Abs, Forearms, Quads, Hamstrings, Glutes, Calves, LowerBack, UpperBack
}

enum class LoadBand { Recovering, Lacking, Balanced, ModeratelyStrained, Overtrained }

data class MuscleLoad(
    val group: MuscleGroups,
    val score: Float,
    val band: LoadBand,
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
        sleepHours == null -> 1.0f
        sleepHours >= 8.5f -> 1.15f
        sleepHours >= 7.5f -> 1.08f
        sleepHours >= 6.5f -> 1.0f
        sleepHours >= 5.0f -> 0.92f
        else -> 0.85f
    }
    val rhrMultiplier: Float = when {
        restingHeartRate == null -> 1.0f
        restingHeartRate < 50 -> 1.1f
        restingHeartRate < 60 -> 1.05f
        restingHeartRate < 70 -> 1.0f
        restingHeartRate < 80 -> 0.95f
        else -> 0.9f
    }
    val spo2Multiplier: Float = when {
        sleepSpo2 == null -> 1.0f
        sleepSpo2 >= 98.0 -> 1.08f
        sleepSpo2 >= 95.0 -> 1.04f
        sleepSpo2 >= 92.0 -> 1.0f
        else -> 0.93f
    }
    val proteinMultiplier: Float = when {
        proteinGrams == null -> 1.0f
        proteinGrams >= 150 -> 1.12f
        proteinGrams >= 100 -> 1.06f
        proteinGrams >= 60 -> 1.0f
        else -> 0.94f
    }
    val bodyFatMultiplier: Float = when {
        bodyFatPercentage == null -> 1.0f
        bodyFatPercentage in 12.0..20.0 -> 1.05f
        bodyFatPercentage in 8.0..25.0 -> 1.0f
        else -> 0.96f
    }
    val totalMultiplier = sleepMultiplier * rhrMultiplier * spo2Multiplier * proteinMultiplier * bodyFatMultiplier
}

data class MuscleWeeklyLoad(
    val group: MuscleGroups,
    val weeklySets: Int,
    val band: LoadBand
)

data class WorkoutSummary(
    val date: Instant,
    val name: String,
    val exercises: List<String> = emptyList()
)

private fun friendlyAgo(now: Instant, then: Instant): String {
    val mins = ChronoUnit.MINUTES.between(then, now)
    if (mins < 60) return "${max(0L, mins)}m ago"
    val hours = ChronoUnit.HOURS.between(then, now)
    if (hours < 24) return "${hours}h ago"
    val days = ChronoUnit.DAYS.between(then, now)
    return when {
        days == 1L -> "1d ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

data class Targets(
    val target: Float,
    val lackingCutoff: Int,
    val overtrainedCutoff: Int
)

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
    tgt *= 1.07f
    tgt = tgt.coerceIn(8.5f, 26f)
    val lackingFrac = if (isImportant) 0.46f else 0.48f
    val overFrac = if (isImportant) 1.58f else 1.55f
    val lacking = floor(tgt * lackingFrac).toInt().coerceAtLeast(0)
    val over = ceil(tgt * overFrac).toInt()
    return Targets(target = tgt, lackingCutoff = lacking, overtrainedCutoff = over)
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
    var m = 1.08f
    if ("amrap" in token || "failure" in token) m *= 1.1f
    if ("drop set" in token || "dropset" in token) m *= 1.07f
    if ("super set" in token || "superset" in token) m *= 1.05f
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
    val optimismBoost = 1.12f
    return baseSets * repsFactor * loadFactor * wordMult * optimismBoost
}

private fun sleepHoursInLast(records: List<SleepSessionRecord>, now: Instant, lookbackHours: Long): Float {
    val start = now.minus(Duration.ofHours(lookbackHours))
    var total = 0L
    records.forEach { s ->
        val st = if (s.startTime.isBefore(start)) start else s.startTime
        val en = if (s.endTime.isAfter(now)) now else s.endTime
        if (en.isAfter(st)) total += Duration.between(st, en).toMinutes()
    }
    return total / 60f
}
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?,
    sleepSessions: List<SleepSessionRecord>,
    restingHeartRates: List<RestingHeartRateRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> = withContext(Dispatchers.Default) {
    val horizonDays = 28L
    val start = now.minus(Duration.ofDays(horizonDays))
    val halfLifeDays = 12.0f
    val k = ln(2.0).toFloat() / halfLifeDays
    val lastTime = mutableMapOf<MuscleGroups, Instant>()
    val decayed = mutableMapOf<MuscleGroups, Float>()
    val recentCounts = mutableMapOf<MuscleGroups, Int>()
    val acuteWindowHours = 30L
    val recoverWindowHours = 36L

    val sleepSpo2 = oxygenSaturations.filter { r ->
        sleepSessions.any { s -> r.time.isAfter(s.startTime) && r.time.isBefore(s.endTime) }
    }.map { it.percentage.value }.average()

    val recoveryFactors = RecoveryFactors(
        sleepHours = sleepHoursInLast(sleepSessions, now, 28),
        restingHeartRate = restingHeartRates.maxByOrNull { it.time }?.beatsPerMinute,
        sleepSpo2 = if (sleepSpo2.isNaN()) null else sleepSpo2,
        proteinGrams = nutrition.sumOf { it.protein?.inGrams ?: 0.0 },
        bodyFatPercentage = bodyFat.maxByOrNull { it.time }?.percentage?.value
    )
    val recoveryMult = recoveryFactors.totalMultiplier

    recent.forEach { w ->
        if (w.date.isBefore(start)) return@forEach
        val tokens = if (w.exercises.isNotEmpty()) w.exercises else listOf(w.name)
        val sessionMuscleStimulus = mutableMapOf<MuscleGroups, Float>()
        tokens.forEach { raw ->
            val token = raw.lowercase(Locale.US)
            val eff = effectiveSetsFromToken(token, profile)
            nameToMusclesWeighted.forEach { (rx, pairs) ->
                if (rx.containsMatchIn(token)) {
                    pairs.forEach { (m, wt) ->
                        sessionMuscleStimulus[m] = (sessionMuscleStimulus[m] ?: 0f) + eff * wt
                    }
                }
            }
        }
        if (sessionMuscleStimulus.isEmpty()) return@forEach
        val hoursAgo = ChronoUnit.HOURS.between(w.date, now)
        val ageDays = max(0f, hoursAgo.toFloat() / 24f)
        val decay = exp(-k * ageDays)
        sessionMuscleStimulus.forEach { (m, stim) ->
            decayed[m] = (decayed[m] ?: 0f) + stim * recoveryMult * decay
            val prev = lastTime[m]
            if (prev == null || w.date.isAfter(prev)) lastTime[m] = w.date
            if (hoursAgo <= acuteWindowHours) recentCounts[m] = (recentCounts[m] ?: 0) + 1
        }
    }

    val order = listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Biceps, MuscleGroups.Triceps,
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps, MuscleGroups.Abs,
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves,
        MuscleGroups.LowerBack
    )

    val loads = order.map { g ->
        val t = computeTargets(profile, g)
        var effWeekly = (decayed[g] ?: 0f) * (1f - exp(-k)) * 7f
        val last = lastTime[g]
        val daysSince = last?.let { ChronoUnit.DAYS.between(it, now) } ?: 99L
        if (last != null && ChronoUnit.DAYS.between(last, now) <= 14) effWeekly *= 1.15f
        val recentCount = recentCounts[g] ?: 0
        val inRecoverWindow = last?.let { ChronoUnit.HOURS.between(it, now) <= recoverWindowHours } == true
        val recoveringGate = inRecoverWindow && recentCount < 2
        val band = when {
            recoveringGate -> LoadBand.Recovering
            effWeekly > t.overtrainedCutoff.toFloat() -> LoadBand.Overtrained
            effWeekly > t.target * 1.3f -> LoadBand.ModeratelyStrained
            effWeekly < t.lackingCutoff.toFloat() -> LoadBand.Lacking
            else -> LoadBand.Balanced
        }
        val ratio = (effWeekly / t.target).coerceIn(0f, 2.2f)
        val score = ratio.pow(0.42f).coerceIn(0f, 1f)
        MuscleLoad(
            group = g,
            score = score,
            band = band,
            lastTrainedAgo = last?.let { friendlyAgo(now, it) }
        )
    }
    loads to recoveryFactors
}



suspend fun deriveMuscleLoadsStepwise(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?,
    onStep: (List<MuscleLoad>, RecoveryFactors, Int, Int) -> Unit,
    sleepSessions: List<SleepSessionRecord>,
    restingHeartRates: List<RestingHeartRateRecord>,
    oxygenSaturations: List<OxygenSaturationRecord>,
    nutrition: List<NutritionRecord>,
    bodyFat: List<BodyFatRecord>
): Pair<List<MuscleLoad>, RecoveryFactors> {
    val (base, factors) = deriveMuscleLoads(now, recent, profile, aiText, sleepSessions, restingHeartRates, oxygenSaturations, nutrition, bodyFat)
    val total = base.size
    val acc = mutableListOf<MuscleLoad>()
    for ((i, item) in base.withIndex()) {
        acc += item
        onStep(acc.toList(), factors, i + 1, total)
        delay(45)
    }
    return acc to factors
}

enum class BodyCategory { All, UpperBody, Arms, Core, LowerBody }

private fun categoryOf(name: String): BodyCategory {
    return when (name.lowercase()) {
        "pecs", "delts", "lats", "traps", "upperback", "upper back" -> BodyCategory.UpperBody
        "biceps", "triceps", "forearms" -> BodyCategory.Arms
        "abs", "lowerback", "lower back" -> BodyCategory.Core
        "quads", "hamstrings", "glutes", "calves" -> BodyCategory.LowerBody
        else -> BodyCategory.UpperBody
    }
}

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
        val start = now.minus(30, ChronoUnit.DAYS)
        val sleep = healthConnectManager.readSleepSessions(start, now)
        val rhr = healthConnectManager.readRestingHeartRate(start, now)
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
            restingHeartRates = rhr,
            oxygenSaturations = spo2,
            nutrition = nutrition,
            bodyFat = bodyfat
        )
        loads = finalLoads
        recoveryFactors = factors
        computing = false
    }

    var selected by remember { mutableStateOf(BodyCategory.All) }
    if (computing) {
        LoadingScreen(progress = progress, modifier = modifier.fillMaxWidth())
        return
    }
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val borderBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF8B0000), Color.LightGray)
            )
            FilledTonalButton(
                onClick = onOpenWeeklySummary,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF4A0000).copy(alpha = 0.4f),
                    contentColor = Color(0xFFF48A8A)
                ),
                border = BorderStroke(width = 1.dp, brush = borderBrush)
            ) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.padding(2.dp))
                Text("Weekly Summary")
            }
        }
        recoveryFactors?.let { RecoveryFactorRow(it) }
        CategoryFilterBar(selected = selected, onSelect = { selected = it; haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap) })
        val filtered = remember(loads, selected) { if (selected == BodyCategory.All) loads else loads.filter { categoryOf(it.group.name) == selected } }
        OverviewRow(loads = filtered)
        val sortedLoads = remember(filtered) {
            filtered.sortedWith(
                compareBy<MuscleLoad> {
                    when (it.band) {
                        LoadBand.Overtrained -> 0
                        LoadBand.ModeratelyStrained -> 1
                        LoadBand.Recovering -> 2
                        LoadBand.Balanced -> 3
                        LoadBand.Lacking -> 4
                    }
                }.thenByDescending { it.score }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sortedLoads.forEach { item -> MuscleBarRow(item) }
        }
    }
}

@Composable
private fun RecoveryFactorRow(factors: RecoveryFactors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
        Icon(
            imageVector = if (isPositive) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun LoadingScreen(progress: Float, modifier: Modifier = Modifier) {
    val cornerRadius = 22.dp
    val density = LocalDensity.current
    val cornerRpx = with(density) { cornerRadius.toPx() }
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    val neonPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2500),
        ), label = "neon_phase"
    )
    Column(
        modifier = modifier
            .padding(16.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    listOf(Color(0xFF3A0E0E), Color(0xFF120707)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 1.1f
                )
                val sweepX = size.width * (neonPhase * 1.5f - 0.25f)
                val neonCore = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFF5E55).copy(alpha = 0.8f), Color.Transparent),
                    start = Offset(sweepX - size.width * 0.25f, 0f),
                    end = Offset(sweepX, size.height)
                )
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                    drawRoundRect(brush = neonCore, style = Stroke(width = 2.dp.toPx()), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                }
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Analyzing muscle status...", style = MaterialTheme.typography.titleMedium, color = Color.White,
            fontWeight = FontWeight.Bold
        )
        val barBrush = Brush.horizontalGradient(listOf(Color(0xFFFF3B30).copy(alpha = 0.7f), Color(0xFFFF7A59)))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(barBrush)
                    .drawWithCache {
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height / 2f),
                            radius = size.height * 2.5f
                        )
                        onDrawBehind { drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(10.dp.toPx())) }
                    }
            )
        }
        Text(
            "${(progress * 100f).coerceIn(0f, 100f).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
    }
}

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
                        val bgBrush = if (isSelected) Brush.radialGradient(
                            listOf(Color(0xFF3A0E0E), Color(0xFF120707)),
                            radius = size.minDimension * 2f
                        ) else Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                            radius = size.minDimension * 2f
                        )
                        val borderBrush = if (isSelected) Brush.linearGradient(
                            listOf(Color(0xFFFF5555).copy(alpha = 0.6f), Color(0xFF8B0000).copy(alpha = 0.45f))
                        ) else Brush.radialGradient(
                            listOf(Color(0x00171717), Color(0xFF525252)),
                            radius = size.minDimension * 2f
                        )
                        onDrawBehind {
                            drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(pillRadius.toPx()))
                            drawRoundRect(brush = borderBrush, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(pillRadius.toPx()))
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewRow(loads: List<MuscleLoad>) {
    val overCount = remember(loads) { loads.count { it.band == LoadBand.Overtrained } }
    val strainedCount = remember(loads) { loads.count { it.band == LoadBand.ModeratelyStrained } }
    val balCount = remember(loads) { loads.count { it.band == LoadBand.Balanced } }
    val lacCount = remember(loads) { loads.count { it.band == LoadBand.Lacking } }
    val recCount = remember(loads) { loads.count { it.band == LoadBand.Recovering } }

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
            if (overCount > 0) MiniStatPill("Overtrained", overCount.toString(), Color(0xFFFF3B30))
            if (strainedCount > 0) MiniStatPill("Strained", strainedCount.toString(), Color(0xFFFFA500))
            if (balCount > 0) MiniStatPill("Balanced", balCount.toString(), Color(0xFF00E676))
            if (lacCount > 0) MiniStatPill("Lacking", lacCount.toString(), Color(0xFF2979FF))
            if (recCount > 0) MiniStatPill("Recovering", recCount.toString(), Color(0xFF6A0DAD))
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
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.5f), Color.Transparent),
                            radius = size.minDimension * 1.5f
                        )
                    )
                }
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun MuscleBarRow(load: MuscleLoad) {
    val bandColor = when (load.band) {
        LoadBand.Lacking -> Color(0xFF2979FF)
        LoadBand.Balanced -> Color(0xFF00E676)
        LoadBand.ModeratelyStrained -> Color(0xFFFFA500)
        LoadBand.Overtrained -> Color(0xFFFF3B30)
        LoadBand.Recovering -> Color(0xFF6A0DAD)
    }
    val bandLabel = when (load.band) {
        LoadBand.Lacking -> "Lacking"
        LoadBand.Balanced -> "Balanced"
        LoadBand.ModeratelyStrained -> "Strained"
        LoadBand.Overtrained -> "Overtrained"
        LoadBand.Recovering -> "Recovering"
    }
    val bandIcon = when (load.band) {
        LoadBand.Lacking -> Icons.Outlined.KeyboardArrowDown
        LoadBand.Balanced -> Icons.Outlined.Check
        LoadBand.ModeratelyStrained -> Icons.Outlined.Warning
        LoadBand.Overtrained -> Icons.Outlined.Warning
        LoadBand.Recovering -> Icons.Outlined.Restore
    }
    val animatedColor by animateColorAsState(bandColor, label = "bandColor", animationSpec = tween(400))
    val fill by animateFloatAsState(
        targetValue = load.score.coerceIn(0f, 1f),
        animationSpec = tween(800, 0, LinearOutSlowInEasing),
        label = "fill"
    )
    val cornerRadius = 22.dp
    val density = LocalDensity.current
    val cornerRpx = with(density) { cornerRadius.toPx() }
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    listOf(animatedColor.copy(alpha = 0.1f), Color(0xFF120707)),
                    center = Offset(size.width / 2f, size.height * -0.2f),
                    radius = size.width * 1.2f
                )
                val borderBrush = Brush.linearGradient(
                    listOf(animatedColor.copy(alpha = 0.3f), animatedColor.copy(alpha = 0.1f))
                )
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                    drawRoundRect(brush = borderBrush, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                }
            }
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                load.group.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedColor.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        radius = size.height * 1.5f
                    )
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(bandIcon, contentDescription = null, tint = animatedColor, modifier = Modifier.size(18.dp))
                Text(
                    bandLabel, style = MaterialTheme.typography.bodySmall, color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                val percentText = String.format(Locale.US, "%.0f%%", fill * 100f)
                Text(
                    "($percentText)",
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedColor.copy(alpha = 0.8f)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            val barBrush = remember(animatedColor) {
                Brush.horizontalGradient(listOf(animatedColor.copy(alpha = 0.6f), animatedColor))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .height(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(barBrush)
                    .drawWithCache {
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height / 2f),
                            radius = size.height * 2.5f
                        )
                        onDrawBehind { drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(10.dp.toPx())) }
                    }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val readiness = when (load.band) {
                LoadBand.Lacking -> "Ready to train"
                LoadBand.Balanced -> "Maintain or focus light"
                LoadBand.Overtrained -> "Prioritize recovery"
                LoadBand.ModeratelyStrained -> "Needs light work or rest"
                LoadBand.Recovering -> "Actively recovering"
            }
            Text(
                readiness, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            if (load.lastTrainedAgo != null) {
                Text("Last: ${load.lastTrainedAgo}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}