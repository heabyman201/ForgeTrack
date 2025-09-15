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

// -----------------------------
// Types
// -----------------------------
data class UserProfile(
    val name: String,
    val weightKg: Int,
    val heightCm: Int,
    val age: Int,
    val experience: String,        // e.g. "beginner", "intermediate", "advanced"
    val preferredStyle: String,    // e.g. "strength", "hypertrophy", "endurance"
    val importantMuscles: List<MuscleGroups>
)
// Adapter: safely convert from prefsManager String values → typed profile
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

// For the bar UI (0..1 score + last trained text)
data class MuscleLoad(
    val group: MuscleGroups,
    val score: Float,                 // 0f..1f normalized signal (history + AI)
    val band: LoadBand,
    val lastTrainedAgo: String? = null
)

// Deterministic 7-day aggregation (sets/week style)
data class MuscleWeeklyLoad(
    val group: MuscleGroups,
    val weeklySets: Int,
    val band: LoadBand
)

// Minimal workout summary you already pass around
data class WorkoutSummary(
    val date: Instant,
    val name: String,
    val exercises: List<String> = emptyList()
)

// -----------------------------
// Helpers & mappings
// -----------------------------
private val nameToMuscles: List<Pair<Regex, List<MuscleGroups>>> = listOf(
    // --- Chest & pressing ---
    "bench( press)?|flat bench|chest press" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Triceps, MuscleGroups.Delts
    ),
    "incline( bench)?|incline press" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Triceps
    ),
    "decline( bench)?|decline press" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Triceps, MuscleGroups.Delts
    ),
    "push[- ]?up|wide push[- ]?up|diamond push[- ]?up|close grip push[- ]?up" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Triceps, MuscleGroups.Delts
    ),
    "dumbbell press|db press" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Triceps, MuscleGroups.Delts
    ),
    "machine press|pec deck|chest fly|flye" to listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts
    ),
    "overhead press|shoulder press|ohp|military press|arnold press|db shoulder press" to listOf(
        MuscleGroups.Delts, MuscleGroups.Triceps
    ),
    "tricep( extension)?|skullcrusher|close[- ]?grip bench|dip|bench dip|cable pushdown|pushdown" to listOf(
        MuscleGroups.Triceps
    ),

    // --- Back & pulling ---
    "row|barbell row|seated row|cable row|t bar row|one[- ]?arm row|db row|pendlay row" to listOf(
        MuscleGroups.Lats, MuscleGroups.Biceps, MuscleGroups.UpperBack
    ),
    "pull[- ]?up|chin[- ]?up|lat pull|pulldown" to listOf(
        MuscleGroups.Lats, MuscleGroups.Biceps
    ),
    "face pull|rear delt fly|reverse fly" to listOf(
        MuscleGroups.Delts, MuscleGroups.Traps, MuscleGroups.UpperBack
    ),
    "shrug|barbell shrug|db shrug" to listOf(
        MuscleGroups.Traps
    ),

    // --- Biceps & arms ---
    "curl|biceps|bicep curl|barbell curl|ez bar curl|preacher curl|hammer curl|db curl|dumbbell curl|concentration curl|incline curl|cable curl" to listOf(
        MuscleGroups.Biceps, MuscleGroups.Forearms
    ),
    "forearm curl|wrist curl|reverse curl" to listOf(
        MuscleGroups.Forearms, MuscleGroups.Biceps
    ),

    // --- Legs & lower body ---
    "squat|front squat|back squat|hack squat|db squat|goblet squat|zercher squat|smith squat|overhead squat" to listOf(
        MuscleGroups.Quads, MuscleGroups.Glutes, MuscleGroups.Hamstrings
    ),
    "lunge|split squat|bulgarian split squat|walking lunge|reverse lunge" to listOf(
        MuscleGroups.Quads, MuscleGroups.Glutes, MuscleGroups.Hamstrings
    ),
    "step[- ]?up" to listOf(
        MuscleGroups.Quads, MuscleGroups.Glutes
    ),
    "leg press" to listOf(
        MuscleGroups.Quads, MuscleGroups.Glutes, MuscleGroups.Hamstrings
    ),
    "deadlift|rdl|romanian deadlift|stiff leg deadlift|sumo deadlift|trap bar deadlift" to listOf(
        MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.LowerBack
    ),
    "good morning" to listOf(
        MuscleGroups.Hamstrings, MuscleGroups.LowerBack, MuscleGroups.Glutes
    ),
    "hip thrust|glute bridge" to listOf(
        MuscleGroups.Glutes, MuscleGroups.Hamstrings
    ),
    "leg extension" to listOf(
        MuscleGroups.Quads
    ),
    "leg curl|hamstring curl" to listOf(
        MuscleGroups.Hamstrings
    ),
    "calf raise|standing calf|seated calf" to listOf(
        MuscleGroups.Calves
    ),

    // --- Core & abs ---
    "plank|crunch|sit[- ]?up|ab rollout|ab wheel|leg raise|hanging leg raise|bicycle crunch|russian twist" to listOf(
        MuscleGroups.Abs, MuscleGroups.LowerBack
    ),
    "side plank|side bend|oblique" to listOf(
        MuscleGroups.Abs
    ),
    "back extension|hyperextension" to listOf(
        MuscleGroups.LowerBack, MuscleGroups.Glutes, MuscleGroups.Hamstrings
    )
).map { (pat, gs) -> pat.toRegex(RegexOption.IGNORE_CASE) to gs }

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
    val target: Float,           // personalized target sets/week for this muscle
    val lackingCutoff: Int,      // sets < this -> Lacking
    val overtrainedCutoff: Int   // sets > this -> Overtrained
)

private fun bmi(weightKg: Int, heightCm: Int): Float {
    val h = heightCm / 100f
    return if (h <= 0f) 0f else weightKg / (h * h)
}

private fun computeTargets(profile: UserProfile, muscle: MuscleGroups): Targets {

    var tgt = 12f


    tgt *= when {
        "beginner" in profile.experience -> 0.90f   // start a bit lower
        "advanced" in profile.experience -> 1.15f   // can handle more
        else -> 1.00f                               // intermediate / unknown
    }

    tgt *= when (profile.preferredStyle) {
        "strength"    -> 0.90f   // fewer sets, higher intensity
        "hypertrophy" -> 1.10f   // more weekly volume
        "endurance"   -> 0.85f   // less local muscular volume
        else          -> 1.00f
    }

    // Age recovery adjustment (reduce targets & overtrained threshold modestly as age increases)
    val age = profile.age
    val ageVolMult = when {
        age < 20 -> 1.00f
        age < 40 -> 1.00f
        age < 50 -> 0.95f
        age < 60 -> 0.90f
        else     -> 0.85f
    }
    tgt *= ageVolMult

    // BMI adjustment (favor conservative targets at the extremes)
    val b = bmi(profile.weightKg, profile.heightCm)
    val bmiVolMult = when {
        b < 18.5f -> 0.95f
        b < 25f   -> 1.00f
        b < 30f   -> 0.98f
        else      -> 0.92f
    }
    tgt *= bmiVolMult

    // Important muscles: nudge target up (focus) and widen balanced band a bit on the high side
    val isImportant = muscle in profile.importantMuscles
    if (isImportant) {
        tgt *= 1.10f
    }

    // Clamp target to a sane range
    tgt = tgt.coerceIn(6f, 18f)


    val lackingFrac = if (isImportant) 0.60f else 0.50f
    val overFrac    = if (isImportant) 1.35f else 1.30f

    val lacking = kotlin.math.floor(tgt * lackingFrac).toInt().coerceAtLeast(0)
    val over    = kotlin.math.ceil(tgt * overFrac).toInt()

    return Targets(target = tgt, lackingCutoff = lacking, overtrainedCutoff = over)
}


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
        tokens.forEach { token ->
            val lower = token.lowercase()
            nameToMuscles.forEach { (rx, groups) ->
                if (rx.containsMatchIn(lower)) groups.forEach { bump(it, 1) }
            }
        }
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

// -----------------------------
// Hybrid scorer used by the UI
// -----------------------------
/**
 * Build MuscleLoad from (a) history with recency decay and (b) AI hints.
 * - History → rolling score by recency (last 10 days), name→muscle mapping fallback.
 * - AI hints → nudge bands (trained/overtrained) and optional score overrides.
 */
suspend fun deriveMuscleLoads(
    now: Instant,
    recent: List<WorkoutSummary>,
    profile: UserProfile,
    aiText: String?
): List<MuscleLoad> = withContext(Dispatchers.Default) {
    // 1) Count weekly sets per muscle (7-day window), one hit per muscle per workout
    val start = now.minus(Duration.ofDays(7))
    val weeklySets = mutableMapOf<MuscleGroups, Int>()
    val lastTime  = mutableMapOf<MuscleGroups, Instant>()

    recent.forEach { w ->
        if (w.date.isBefore(start)) return@forEach
        val tokens = w.exercises.ifEmpty { listOf(w.name) }

        // collect all groups hit in this workout (de-duped)
        val groupsHit = buildSet<MuscleGroups> {
            tokens.forEach { raw ->
                val token = raw.lowercase(java.util.Locale.US)
                nameToMuscles.forEach { (rx, groups) ->
                    if (rx.containsMatchIn(token)) addAll(groups)
                }
            }
        }

        // bump each hit muscle ONCE
        groupsHit.forEach { g ->
            weeklySets[g] = (weeklySets[g] ?: 0) + 1
            val prev = lastTime[g]
            if (prev == null || w.date.isAfter(prev)) lastTime[g] = w.date
        }
    }


    fun bandOf(sets: Int): LoadBand = when {
        sets < 4    -> LoadBand.Lacking
        sets <= 12  -> LoadBand.Balanced
        else        -> LoadBand.Overtrained
    }


    // 3) Build stable ordered output; score is visual only (normalized by 25 sets)
    val order = listOf(
        MuscleGroups.Pecs, MuscleGroups.Delts, MuscleGroups.Biceps, MuscleGroups.Triceps,
        MuscleGroups.Lats, MuscleGroups.UpperBack, MuscleGroups.Traps, MuscleGroups.Abs,
        MuscleGroups.Quads, MuscleGroups.Hamstrings, MuscleGroups.Glutes, MuscleGroups.Calves,
        MuscleGroups.LowerBack
    )

    order.map { g ->
        val sets = weeklySets[g] ?: 0

        // Personalized targets for THIS user and THIS muscle
        val t = computeTargets(profile, g)  // <-- you need `profile` in scope (see note below)

        // Band classification using personalized cutoffs
        val band = when {
            sets < t.lackingCutoff      -> LoadBand.Lacking
            sets > t.overtrainedCutoff  -> LoadBand.Overtrained
            else                        -> LoadBand.Balanced
        }

        // Score: fill faster early, taper near target (sqrt curve),
        // and normalize against the personalized target.
        val score = kotlin.math.sqrt((sets / t.target).coerceIn(0f, 1f))

        MuscleLoad(
            group = g,
            score = score,
            band = band,
            lastTrainedAgo = lastTime[g]?.let { friendlyAgo(now, it) }
        )
    }

}


// -----------------------------
// Prompt builder (for your AI call)
// -----------------------------
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

// -----------------------------
// UI Section
// -----------------------------


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
        LoadBand.Lacking -> "Not Enough Training"
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


