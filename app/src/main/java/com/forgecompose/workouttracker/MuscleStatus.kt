package com.forgecompose.workouttracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.E
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    Pecs, Delts, Biceps, Triceps, Lats, Traps, Abs, Forearms,
    Quads, Hamstrings, Glutes, Calves, LowerBack, UpperBack
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
        days < 7   -> "${days}d ago"
        else       -> "${days / 7}w ago"
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

private fun computeTargets(profile: UserProfile, muscle: MuscleGroups): Targets {
    var tgt = 12f
    tgt *= when {
        "beginner" in profile.experience -> 0.90f
        "advanced" in profile.experience -> 1.15f
        else -> 1.00f
    }
    tgt *= when (profile.preferredStyle) {
        "strength"    -> 0.90f
        "hypertrophy" -> 1.10f
        "endurance"   -> 0.85f
        else          -> 1.00f
    }
    val age = profile.age
    val ageVolMult = when {
        age < 20 -> 1.00f
        age < 40 -> 1.00f
        age < 50 -> 0.95f
        age < 60 -> 0.90f
        else     -> 0.85f
    }
    tgt *= ageVolMult
    val b = bmi(profile.weightKg, profile.heightCm)
    val bmiVolMult = when {
        b < 18.5f -> 0.95f
        b < 25f   -> 1.00f
        b < 30f   -> 0.98f
        else      -> 0.92f
    }
    tgt *= bmiVolMult
    val isImportant = muscle in profile.importantMuscles
    if (isImportant) tgt *= 1.10f
    tgt = tgt.coerceIn(6f, 18f)
    val lackingFrac = if (isImportant) 0.60f else 0.50f
    val overFrac    = if (isImportant) 1.35f else 1.30f
    val lacking = floor(tgt * lackingFrac).toInt().coerceAtLeast(0)
    val over    = ceil(tgt * overFrac).toInt()
    return Targets(target = tgt, lackingCutoff = lacking, overtrainedCutoff = over)
}

/**
 * Weighted exercise→muscle mapping. Values are relative per-session contribution caps by muscle.
 * 1.0 = heavy compound focus, 0.6 = strong secondary, 0.4 = accessory, 0.25 = isolation.
 */
private val nameToMusclesWeighted: List<Pair<Regex, List<Pair<MuscleGroups, Float>>>> = listOf(
    // Chest / pressing
    "bench( press)?|flat bench|barbell bench" to listOf(MuscleGroups.Pecs to 1.0f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.6f),
    "incline( bench)?|incline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Delts to 0.7f, MuscleGroups.Triceps to 0.5f),
    "decline( bench)?|decline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "close[- ]?grip bench" to listOf(MuscleGroups.Triceps to 0.9f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.5f),
    "push[- ]?up|diamond push[- ]?up|wide push[- ]?up|deficit push[- ]?up|ring push[- ]?up" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "dumbbell press|db press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "machine press|smith press|pec deck" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Delts to 0.5f),
    "chest fly|flye|cable crossover|low to high fly|high to low fly" to listOf(MuscleGroups.Pecs to 0.5f, MuscleGroups.Delts to 0.25f),
    "dip|bench dip" to listOf(MuscleGroups.Triceps to 0.8f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.4f),
    // Shoulders
    "overhead press|shoulder press|ohp|military press|arnold press|push press|db shoulder press" to listOf(MuscleGroups.Delts to 1.0f, MuscleGroups.Triceps to 0.7f),
    "lateral raise|side raise|cable lateral" to listOf(MuscleGroups.Delts to 0.4f),
    "front raise" to listOf(MuscleGroups.Delts to 0.4f),
    "reverse fly|rear delt fly|face pull" to listOf(MuscleGroups.Delts to 0.5f, MuscleGroups.UpperBack to 0.5f, MuscleGroups.Traps to 0.4f),
    // Back / pulling
    "pull[- ]?up|chin[- ]?up|neutral grip pull[- ]?up" to listOf(MuscleGroups.Lats to 1.0f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "lat pull|pulldown|wide grip pulldown|close grip pulldown" to listOf(MuscleGroups.Lats to 0.9f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "row|barbell row|seated row|cable row|t[- ]?bar row|one[- ]?arm row|db row|pendlay row|meadow row" to listOf(MuscleGroups.UpperBack to 0.9f, MuscleGroups.Lats to 0.8f, MuscleGroups.Biceps to 0.6f),
    "shrug|barbell shrug|db shrug" to listOf(MuscleGroups.Traps to 0.9f),
    "pullover|lat pullover" to listOf(MuscleGroups.Lats to 0.6f, MuscleGroups.Pecs to 0.3f),
    // Biceps / forearms
    "curl|biceps|bicep curl|barbell curl|ez bar curl|preacher curl|hammer curl|incline curl|concentration curl|cable curl|spider curl" to listOf(MuscleGroups.Biceps to 0.5f, MuscleGroups.Forearms to 0.25f),
    "reverse curl|wrist curl|forearm curl" to listOf(MuscleGroups.Forearms to 0.5f, MuscleGroups.Biceps to 0.25f),
    // Triceps
    "tricep( extension)?|skullcrusher|overhead extension|rope pushdown|cable pushdown|v[- ]?bar pushdown" to listOf(MuscleGroups.Triceps to 0.5f),
    // Legs / hips
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
    // Core
    "plank|side plank|hollow hold" to listOf(MuscleGroups.Abs to 0.4f, MuscleGroups.LowerBack to 0.25f),
    "crunch|sit[- ]?up|cable crunch|machine crunch" to listOf(MuscleGroups.Abs to 0.4f),
    "leg raise|hanging leg raise|reverse crunch" to listOf(MuscleGroups.Abs to 0.45f),
    "ab rollout|ab wheel" to listOf(MuscleGroups.Abs to 0.5f, MuscleGroups.LowerBack to 0.3f),
    "russian twist|woodchop|pallof press" to listOf(MuscleGroups.Abs to 0.4f),
    "back extension|hyperextension" to listOf(MuscleGroups.LowerBack to 0.6f, MuscleGroups.Glutes to 0.4f, MuscleGroups.Hamstrings to 0.4f),
).map { (pat, gs) -> pat.toRegex(RegexOption.IGNORE_CASE) to gs }

/**
 * Plain mapping for quick counting (computeWeeklyLoads) derived from the weighted map.
 */
private val nameToMuscles: List<Pair<Regex, List<MuscleGroups>>> =
    nameToMusclesWeighted.map { (rx, pairs) -> rx to pairs.map { it.first } }

fun computeWeeklyLoads(
    now: Instant,
    recent: List<WorkoutSummary>
): List<MuscleWeeklyLoad> {
    val start = now.minus(Duration.ofDays(7))
    val counters = mutableMapOf<MuscleGroups, Int>()

    fun bump(group: MuscleGroups, sets: Int = 1) {
        counters[group] = (counters[group] ?: 0) + sets
    }

    for (w in recent) {
        if (w.date.isBefore(start)) continue
        val tokens = if (w.exercises.isNotEmpty()) w.exercises else listOf(w.name)
        val hit = buildSet<MuscleGroups> {
            tokens.forEach { token ->
                val lower = token.lowercase()
                nameToMuscles.forEach { (rx, groups) ->
                    if (rx.containsMatchIn(lower)) addAll(groups)
                }
            }
        }
        hit.forEach { bump(it, 1) }
    }

    fun bandOf(sets: Int): LoadBand = when {
        sets < 6    -> LoadBand.Lacking
        sets <= 20  -> LoadBand.Balanced
        else        -> LoadBand.Overtrained
    }

    val order = listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Biceps, MuscleGroups.Triceps,
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps, MuscleGroups.Abs,
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves,
        MuscleGroups.LowerBack
    )

    return order.map { g ->
        val s = counters[g] ?: 0
        MuscleWeeklyLoad(g, s, bandOf(s))
    }
}

/**
 * Decayed volume model with slow growth and visible recovery.
 * - 21d horizon
 * - exponential half-life = 5d
 * - per-session per-muscle cap = 1.0
 * - convert decayed sum to weekly-equivalent using (1 - e^-k) * 7 scaling
 */
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?
): List<MuscleLoad> = withContext(Dispatchers.Default) {
    val horizonDays = 21L
    val start = now.minus(Duration.ofDays(horizonDays))
    val halfLifeDays = 5f
    val k = ln(2.0).toFloat() / halfLifeDays
    val lastTime = mutableMapOf<MuscleGroups, Instant>()
    val decayed = mutableMapOf<MuscleGroups, Float>()

    recent.forEach { w ->
        if (w.date.isBefore(start)) return@forEach
        val tokens = w.exercises.ifEmpty { listOf(w.name) }

        val sessionWeights = mutableMapOf<MuscleGroups, Float>()
        tokens.forEach { raw ->
            val token = raw.lowercase(Locale.US)
            nameToMusclesWeighted.forEach { (rx, pairs) ->
                if (rx.containsMatchIn(token)) {
                    pairs.forEach { (m, wt) ->
                        sessionWeights[m] = (sessionWeights[m] ?: 0f) + wt
                    }
                }
            }
        }

        if (sessionWeights.isEmpty()) return@forEach

        sessionWeights.replaceAll { _, v -> v.coerceIn(0f, 1f) }
        val ageDays = max(0f, ChronoUnit.HOURS.between(w.date, now).toFloat() / 24f)
        val decay = exp(-k * ageDays)

        sessionWeights.forEach { (m, unit) ->
            decayed[m] = (decayed[m] ?: 0f) + unit * decay
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
        val effWeekly = (decayed[g] ?: 0f) * (1f - exp(-k)) * 7f
        val band = when {
            effWeekly > t.overtrainedCutoff.toFloat() -> LoadBand.Overtrained
            effWeekly < t.lackingCutoff.toFloat()     -> LoadBand.Lacking
            else                                      -> LoadBand.Balanced
        }
        val ratio = (effWeekly / t.target).coerceIn(0f, 1f)
        val score = sqrt(ratio)
        MuscleLoad(
            group = g,
            score = score,
            band = band,
            lastTrainedAgo = lastTime[g]?.let { friendlyAgo(now, it) }
        )
    }
}


fun buildMuscleJsonPrompt(loads: List<MuscleWeeklyLoad>): String {
    val trained = loads.filter { it.band != LoadBand.Lacking }.map { it.group.name }
    val over    = loads.filter { it.band == LoadBand.Overtrained }.map { it.group.name }
    val lacking = loads.filter { it.band == LoadBand.Lacking }.map { it.group.name }

    return """
Return ONLY compact JSON with these keys:
"trained":[...], "overtrained":[...], "lacking":[...].
Use these exact muscle labels: ${MuscleGroups.values().joinToString()}.
trained: $trained
overtrained: $over
lacking: $lacking
""".trimIndent()
}

@Composable
fun MuscleStatusSection(
    recentWorkouts: List<WorkoutSummary>,
    advicePayload: String?,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loads by remember { mutableStateOf<List<MuscleLoad>>(emptyList()) }
    val prefsManager = remember { UserPreferencesManager(context) }
    val profile = remember { buildUserProfile(prefsManager) }
    LaunchedEffect(recentWorkouts, advicePayload, nowEpochMillis) {
        val now = Instant.ofEpochMilli(nowEpochMillis)
        loads = deriveMuscleLoads(now, recentWorkouts, profile, advicePayload)
    }

    Column(
        modifier = modifier.padding(16.dp),
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (loads.isNotEmpty()) {
                val trained = loads.count { it.lastTrainedAgo != null }
                AssistChip(onClick = {}, label = {
                    Text("$trained/${loads.size} active")
                })
            }
        }

        val last = remember(loads) {
            loads.maxByOrNull {
                when (it.lastTrainedAgo) {
                    "today" -> 999
                    null -> 0
                    else -> {
                        if (it.lastTrainedAgo.endsWith("d ago")) {
                            val d = it.lastTrainedAgo.removeSuffix("d ago").trim().toIntOrNull()
                            if (d == null) 0 else 100 - d
                        } else 0
                    }
                }
            }
        }
        if (last?.lastTrainedAgo != null) {
            AssistChipRow(last)
        }

        val sortedLoads = remember(loads) {
            loads.sortedWith(
                compareByDescending<MuscleLoad> { it.band == LoadBand.Overtrained }
                    .thenByDescending { it.band == LoadBand.Balanced }
                    .thenByDescending { it.score }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sortedLoads.forEach { item ->
                MuscleBarRow(item)
            }
        }
    }
}

@Composable
private fun AssistChipRow(last: MuscleLoad) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text("Last trained: ${last.group.name}") })
        AssistChip(onClick = {}, label = { Text(last.lastTrainedAgo ?: "") })
    }
}

@Composable
private fun MuscleBarRow(load: MuscleLoad) {
    val baseColor = when (load.band) {
        LoadBand.Lacking -> Color(0xFF1565A4)
        LoadBand.Balanced -> Color(0xFF62E766)
        LoadBand.Overtrained -> Color(0xFFFF2A19)
    }
    val onBand = when (load.band) {
        LoadBand.Lacking -> Color(0xFFB3E5FC)
        LoadBand.Balanced -> Color(0xFFE8F5E9)
        LoadBand.Overtrained -> Color(0xFFFFCDD2)
    }
    val label = when (load.band) {
        LoadBand.Lacking -> "Lacking"
        LoadBand.Balanced -> "Balanced"
        LoadBand.Overtrained -> "Overtrained"
    }
    val icon = when (load.band) {
        LoadBand.Lacking -> Icons.Outlined.KeyboardArrowDown
        LoadBand.Balanced -> Icons.Outlined.Check
        LoadBand.Overtrained -> Icons.Outlined.Warning
    }
    val animatedColor by animateColorAsState(baseColor, label = "bandColor")
    val intensity = (load.score.coerceIn(0f, 1f) * 100f)
    val fill by animateFloatAsState(load.score.coerceIn(0f, 1f), tween(650, 0, LinearOutSlowInEasing), label = "fill")
    val capsuleBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
                Text(load.group.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(capsuleBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = animatedColor, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = onBand)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            val barBrush = remember(animatedColor) {
                Brush.horizontalGradient(
                    listOf(
                        animatedColor.copy(alpha = 0.55f),
                        animatedColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(barBrush)
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val readiness = when (load.band) {
                LoadBand.Lacking -> "Good time to train"
                LoadBand.Balanced -> "Maintain or light focus"
                LoadBand.Overtrained -> "Prioritize recovery"
            }
            Text(readiness, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${"%.0f".format(intensity)}%", style = MaterialTheme.typography.labelLarge, color = animatedColor, fontWeight = FontWeight.SemiBold)
                if (load.lastTrainedAgo != null) {
                    Text("Last: ${load.lastTrainedAgo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val zoneColor = animatedColor.copy(alpha = 0.35f)
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(zoneColor)
                )
                if (it < 2) Spacer(Modifier.width(6.dp))
            }
        }
    }
}

private fun ln2() = kotlin.math.ln(2.0).toFloat()
