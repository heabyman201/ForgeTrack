package com.forgecompose.workouttracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.sqrt

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

enum class LoadBand { Lacking, Balanced, Overtrained }

data class MuscleLoad(
    val group: MuscleGroups,
    val score: Float,
    val band: LoadBand,
    val lastTrainedAgo: String? = null
)

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
    val days = ChronoUnit.DAYS.between(then, now)
    return when {
        days <= 0 -> "today"
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

private fun expMultFrom(experience: String): Float {
    val e = experience.trim().lowercase(Locale.US)
    val num = e.toFloatOrNull()
    return when {
        num != null && num >= 8f -> 1.18f
        num != null && num >= 5f -> 1.12f
        num != null && num >= 3f -> 1.06f
        num != null && num >= 1f -> 1.02f
        "elite" in e -> 1.18f
        "advanced" in e -> 1.12f
        "intermediate" in e -> 1.06f
        "novice" in e -> 1.02f
        "beginner" in e -> 0.96f
        else -> 1.00f
    }
}

private fun computeTargets(profile: UserProfile, muscle: MuscleGroups): Targets {
    var tgt = 13.5f
    tgt *= expMultFrom(profile.experience)
    tgt *= when (profile.preferredStyle) {
        "weights" -> 0.93f
        "both" -> 1.07f
        "cardio" -> 0.88f
        else -> 1.00f
    }
    val age = profile.age
    val ageVolMult = when {
        age < 18 -> 1.05f
        age < 30 -> 1.03f
        age < 40 -> 1.00f
        age < 50 -> 0.97f
        age < 60 -> 0.94f
        age < 70 -> 0.90f
        else -> 0.85f
    }
    tgt *= ageVolMult
    val hCm = profile.heightCm
    val wKg = profile.weightKg
    val b = bmi(wKg, hCm)
    val bmiVolMult = when {
        b < 18.5f -> 0.98f
        b < 22f -> 1.03f
        b < 27f -> 1.00f
        b < 32f -> 0.97f
        else -> 0.93f
    }
    tgt *= bmiVolMult
    val heightBias = when {
        hCm >= 195 -> 1.08f
        hCm >= 185 -> 1.04f
        hCm <= 165 -> 0.98f
        else -> 1.00f
    }
    val weightBias = when {
        wKg >= 100 -> 1.04f
        wKg >= 85 -> 1.02f
        wKg <= 55 -> 0.97f
        else -> 1.00f
    }
    val regional = when (muscle) {
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.LowerBack -> heightBias * weightBias
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps -> 1.02f
        else -> heightBias
    }
    tgt *= regional
    val isImportant = muscle in profile.importantMuscles
    if (isImportant) tgt *= 1.12f
    tgt *= 1.05f
    tgt = tgt.coerceIn(8f, 22f)
    val lackingFrac = if (isImportant) 0.54f else 0.50f
    val overFrac = if (isImportant) 1.46f else 1.40f
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

private val nameToMuscles: List<Pair<Regex, List<MuscleGroups>>> =
    nameToMusclesWeighted.map { (rx, pairs) -> rx to pairs.map { it.first } }

private data class ParsedVolume(val sets: Int?, val reps: Int?, val weightKg: Float?)

private val tripletRx = Regex("""(?:(\d+)\s*[xX]\s*(\d+))(?:\s*@\s*([0-9]*\.?[0-9]+|bw|bodyweight))?""")
private val kvRx = Regex("""(?:\bsets\s*=\s*(\d+))?|(?:\breps\s*=\s*(\d+))?|(?:\bweight\s*=\s*([0-9]*\.?[0-9]+|bw|bodyweight))?""", RegexOption.IGNORE_CASE)

private fun parseVolumeFromText(s: String): ParsedVolume {
    val lower = s.lowercase()
    tripletRx.find(lower)?.let { m ->
        val sets = m.groupValues[1].toIntOrNull()
        val reps = m.groupValues[2].toIntOrNull()
        val wtStr = m.groupValues.getOrNull(3)?.trim().orEmpty()
        val weight = when {
            wtStr.isEmpty() -> null
            wtStr == "bw" || wtStr == "bodyweight" -> null
            else -> wtStr.toFloatOrNull()
        }
        return ParsedVolume(sets, reps, weight)
    }
    var sets: Int? = null
    var reps: Int? = null
    var weight: Float? = null
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

private fun effectiveSetsFromToken(token: String, profile: UserProfile): Float {
    val p = parseVolumeFromText(token)
    val baseSets = (p.sets ?: 1).coerceAtLeast(1)
    val reps = (p.reps ?: 8).coerceAtLeast(1)
    val repsFactor = when {
        reps <= 4 -> 0.82f
        reps <= 8 -> 1.00f
        reps <= 12 -> 0.95f
        reps <= 20 -> 0.85f
        else -> 0.72f
    }
    val loadFactor = p.weightKg?.let { w ->
        val bw = profile.weightKg.coerceAtLeast(50).toFloat()
        val ratio = (w / bw).coerceIn(0.2f, 2.0f)
        0.70f + 0.30f * ratio.pow(0.5f)
    } ?: 0.82f
    return baseSets * repsFactor * loadFactor
}

suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?
): List<MuscleLoad> = withContext(Dispatchers.Default) {
    val horizonDays = 28L
    val start = now.minus(Duration.ofDays(horizonDays))
    val halfLifeDays = 7f
    val k = ln(2.0).toFloat() / halfLifeDays
    val lastTime = mutableMapOf<MuscleGroups, Instant>()
    val decayed = mutableMapOf<MuscleGroups, Float>()
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
        val ageDays = max(0f, ChronoUnit.HOURS.between(w.date, now).toFloat() / 24f)
        val decay = exp(-k * ageDays)
        sessionMuscleStimulus.forEach { (m, stim) ->
            decayed[m] = (decayed[m] ?: 0f) + stim * decay
            val prev = lastTime[m]
            if (prev == null || w.date.isAfter(prev)) lastTime[m] = w.date
        }
    }
    val order = listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Biceps, MuscleGroups.Triceps,
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps, MuscleGroups.Abs,
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves,
        MuscleGroups.LowerBack
    )
    order.map { g ->
        val t = computeTargets(profile, g)
        var effWeekly = (decayed[g] ?: 0f) * (1f - exp(-k)) * 7f
        val last = lastTime[g]
        if (last != null && ChronoUnit.DAYS.between(last, now) <= 14) effWeekly *= 1.05f
        val band = when {
            effWeekly > t.overtrainedCutoff.toFloat() -> LoadBand.Overtrained
            effWeekly < t.lackingCutoff.toFloat() -> LoadBand.Lacking
            else -> LoadBand.Balanced
        }
        val ratio = (effWeekly / t.target).coerceIn(0f, 1f)
        val score = ratio.toDouble().pow(0.45).toFloat()
        MuscleLoad(
            group = g,
            score = score,
            band = band,
            lastTrainedAgo = last?.let { friendlyAgo(now, it) }
        )
    }
}

suspend fun deriveMuscleLoadsStepwise(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?,
    onStep: (List<MuscleLoad>, Int, Int) -> Unit
): List<MuscleLoad> {
    val base = deriveMuscleLoads(now, recent, profile, aiText)
    val total = base.size
    val acc = mutableListOf<MuscleLoad>()
    for ((i, item) in base.withIndex()) {
        acc += item
        onStep(acc.toList(), i + 1, total)
        delay(45)
    }
    return acc
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
    var loads by remember { mutableStateOf<List<MuscleLoad>>(emptyList()) }
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
        deriveMuscleLoadsStepwise(
            now = now,
            recent = recentWorkouts,
            profile = profile,
            aiText = advicePayload
        ) { partial, done, total ->
            loads = partial
            progress = done.toFloat() / total.toFloat()
        }
        delay(120)
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
            Text(
                "Muscle Status",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

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
        CategoryFilterBar(selected = selected, onSelect = { selected = it; haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap) })
        val filtered = remember(loads, selected) { if (selected == BodyCategory.All) loads else loads.filter { categoryOf(it.group.name) == selected } }
        OverviewRow(loads = filtered)
        val sortedLoads = remember(filtered) {
            filtered.sortedWith(
                compareByDescending<MuscleLoad> { it.band == LoadBand.Overtrained }
                    .thenByDescending { it.band == LoadBand.Balanced }
                    .thenByDescending { it.score }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sortedLoads.forEach { item -> MuscleBarRow(item) }
        }
    }
}

@Composable
private fun LoadingScreen(progress: Float, modifier: Modifier = Modifier) {
    val cornerRadius = 22.dp
    val density = LocalDensity.current
    val cornerRpx = with(density) { cornerRadius.toPx() }
    var neonPhase by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time -> neonPhase = (time / 4_000_000_000F) % 1f }
        }
    }
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
                val sweepX = size.width * (neonPhase * 2f - 0.5f)
                val neonCore = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF3B30).copy(alpha = 0f), Color(0xFFFF3B30), Color(0xFFFF3B30).copy(alpha = 0f)),
                    start = Offset(sweepX, 0f),
                    end = Offset(sweepX + size.width * 0.6f, size.height)
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
        Text("Analyzing muscle status...", style = MaterialTheme.typography.titleMedium, color = Color.White,

            fontWeight = FontWeight.Bold)
        val barBrush = Brush.horizontalGradient(listOf(Color(0xFFFF3B30).copy(alpha = 0.7f), Color(0xFFFF7A59)))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
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
        Text("${(progress * 100f).coerceIn(0f, 100f).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold)
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
    val balCount = remember(loads) { loads.count { it.band == LoadBand.Balanced } }
    val lacCount = remember(loads) { loads.count { it.band == LoadBand.Lacking } }
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
            MiniStatPill("Overtrained", overCount.toString(), Color(0xFFFF3B30))
            MiniStatPill("Balanced", balCount.toString(), Color(0xFF00E676))
            MiniStatPill("Lacking", lacCount.toString(), Color(0xFF2979FF))
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
                .height(8.dp)
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
        LoadBand.Overtrained -> Color(0xFFFF3B30)
    }
    val bandLabel = when (load.band) {
        LoadBand.Lacking -> "Lacking"
        LoadBand.Balanced -> "Balanced"
        LoadBand.Overtrained -> "Overtrained"
    }
    val bandIcon = when (load.band) {
        LoadBand.Lacking -> Icons.Outlined.KeyboardArrowDown
        LoadBand.Balanced -> Icons.Outlined.Check
        LoadBand.Overtrained -> Icons.Outlined.Warning
    }
    val animatedColor by animateColorAsState(bandColor, label = "bandColor")
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
                Icon(bandIcon, contentDescription = null, tint = animatedColor, modifier = Modifier)
                Text(bandLabel, style = MaterialTheme.typography.bodySmall, color = Color.White,
                    fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val percentText = String.format(Locale.US, "%.0f%%", fill * 100f)
            Text(percentText, style = MaterialTheme.typography.labelLarge, color = animatedColor,
                fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val readiness = when (load.band) {
                LoadBand.Lacking -> "Ready to train"
                LoadBand.Balanced -> "Maintain or focus light"
                LoadBand.Overtrained -> "Prioritize recovery"
            }
            Text(readiness, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold)
            if (load.lastTrainedAgo != null) {
                Text("Last: ${load.lastTrainedAgo}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

private fun Float.roundToInt(): Int = round(this).toInt()
