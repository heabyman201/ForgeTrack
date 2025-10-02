package com.forgecompose.workouttracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalConfiguration
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
        "Weights"    -> 0.90f
        "Both" -> 1.10f
        "Cardio"   -> 0.85f
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
    "back extension|hyperextension" to listOf(MuscleGroups.LowerBack to 0.6f, MuscleGroups.Glutes to 0.4f, MuscleGroups.Hamstrings to 0.4f),
).map { (pat, gs) -> pat.toRegex(RegexOption.IGNORE_CASE) to gs }

private val nameToMuscles: List<Pair<Regex, List<MuscleGroups>>> =
    nameToMusclesWeighted.map { (rx, pairs) -> rx to pairs.map { it.first } }

fun computeWeeklyLoads(
    now: Instant,
    recent: List<WorkoutSummary>
): List<MuscleWeeklyLoad> {
    val start = now.minus(Duration.ofDays(7))
    val counters = mutableMapOf<MuscleGroups, Int>()

    fun bump(group: MuscleGroups, sets: Int) {
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
        "pecs", "delts", "lats", "traps", "upper back" -> BodyCategory.UpperBody
        "biceps", "triceps", "forearms" -> BodyCategory.Arms
        "abs", "lower back" -> BodyCategory.Core
        "quads", "hamstrings", "glutes", "calves" -> BodyCategory.LowerBody
        else -> BodyCategory.UpperBody
    }
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
val haptics = LocalHapticFeedback.current
        CategoryFilterBar(selected = selected, onSelect = { selected = it;
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)})

        val filtered = remember(loads, selected) {
            if (selected == BodyCategory.All) loads
            else loads.filter { categoryOf(it.group.name) == selected }
        }

        OverviewRow(loads = filtered)

        val sortedLoads = remember(filtered) {
            filtered.sortedWith(
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
private fun LoadingScreen(progress: Float, modifier: Modifier = Modifier) {
    val cornerRadius = 22.dp
    val density = LocalDensity.current
    val cornerRpx = with(density) { cornerRadius.toPx() }

    var neonPhase by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                neonPhase = (time / 4_000_000_000F) % 1f
            }
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    listOf(
                        Color(0xFF3A0E0E),
                        Color(0xFF120707)
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 1.1f
                )
                val sweepX = size.width * (neonPhase * 2f - 0.5f)
                val neonCore = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF3B30).copy(alpha = 0f),
                        Color(0xFFFF3B30),
                        Color(0xFFFF3B30).copy(alpha = 0f)
                    ),
                    start = Offset(sweepX, 0f),
                    end = Offset(sweepX + size.width * 0.6f, size.height)
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                    )
                    drawRoundRect(
                        brush = neonCore,
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                    )
                }
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Analyzing muscle status...",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        val barBrush = Brush.horizontalGradient(
            listOf(Color(0xFFFF3B30).copy(alpha = 0.7f), Color(0xFFFF7A59))
        )
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
            )
        }
        Text(
            "${(progress * 100f).coerceIn(0f, 100f).toInt()}%",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = Color.White.copy(alpha = 0.8f)
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
            val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)
            val pillRadius = 32.dp

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(pillRadius))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(cat) }
                    )
                    .drawWithCache {
                        val bgBrush = if (isSelected) Brush.radialGradient(
                            listOf(Color(0xFF3A0E0E), Color(0xFF120707)),
                            radius = size.minDimension * 2f
                        ) else Brush.radialGradient(
                            listOf(Color(0x00171717), Color(0xFF525252)),
                            radius = size.minDimension * 2f
                        )

                        val borderBrush = if (isSelected) Brush.linearGradient(
                            listOf(Color(0xFFFF5555).copy(alpha = 0.6f), Color(0xFF8B0000).copy(alpha = 0.45f))
                        ) else Brush.radialGradient(
                            listOf(Color(0x00171717), Color(0xFF525252)),
                            radius = size.minDimension * 2f
                        )

                        onDrawBehind {
                            drawRoundRect(
                                brush = bgBrush,
                                cornerRadius = CornerRadius(pillRadius.toPx())
                            )
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
                Text(
                    label,
                    color = if (isSelected) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.7f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                    )
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                    bandLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
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
                Brush.horizontalGradient(
                    listOf(animatedColor.copy(alpha = 0.6f), animatedColor)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(barBrush)
                    .drawWithCache {
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.9f, size.height / 2f),
                            radius = size.height * 2.5f
                        )
                        onDrawBehind {
                            drawRoundRect(
                                brush = glowBrush,
                                cornerRadius = CornerRadius(10.dp.toPx())
                            )
                        }
                    }
            )
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
            Text(
                readiness,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (load.lastTrainedAgo != null) {
                Text(
                    "Last: ${load.lastTrainedAgo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

