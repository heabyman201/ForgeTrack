package com.forgecompose.workouttracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GraphBar(
    viewModel: WorkoutListViewModel,
    searchQuery: String,
    sortAscending: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is WorkoutListUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF3B30))
            }
        }
        is WorkoutListUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is WorkoutListUiState.Success -> {
            val workouts = state.workouts
            val filteredAndSorted = remember(workouts, searchQuery, sortAscending) {
                val base = if (searchQuery.isBlank()) workouts
                else workouts.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (sortAscending) base.sortedBy { it.id } else base.sortedByDescending { it.id }
            }
            val lastTwoMuscles = remember(filteredAndSorted) {
                val out = mutableListOf<Pair<MuscleGroups, Float>>()
                for (w in filteredAndSorted) {
                    val reps = w.reps ?: 0
                    val sets = w.sets ?: 0
                    if (reps <= 0 || sets <= 0) continue
                    val entry = nameToMusclesWeighted.firstOrNull { it.first.containsMatchIn(w.name) }?.second
                        ?.maxByOrNull { it.second }
                    if (entry != null) {
                        val (m, weight) = entry
                        out += m to (reps * sets * weight)
                        if (out.size == 2) break
                    }
                }
                out
            }
            if (lastTwoMuscles.isEmpty()) {
                EmptyState()
            } else {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    lastTwoMuscles.forEach { (muscle, wr) ->
                        MuscleLoadRow(muscle = muscle.name, wr = wr)
                    }
                }
            }
        }
    }
}
fun parseExperienceToYears(raw: String?): Float {
    if (raw.isNullOrBlank()) return 0f

    val lower = raw.lowercase()

    // grab first number like "6", "1.5", "12"
    val number = Regex("""\d+(\.\d+)?""")
        .find(lower)
        ?.value
        ?.toFloatOrNull()
        ?: return 0f

    return when {
        "month" in lower -> number / 12f
        "year" in lower  -> number
        "years" in lower -> number
        "months" in lower -> number / 12f
        else             -> number
    }
}

@Composable
fun MuscleLoadRow(
    muscle: String,
    wr: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    // --- User prefs (all stored as Strings) ---
    val prefsManager = remember { UserPreferencesManager(context) }

    val userNameRaw = remember { prefsManager.getName() }
    val userAgeRaw = remember { prefsManager.getAge() }
    val userHeightRaw = remember { prefsManager.getHeight() }
    val userWeightRaw = remember { prefsManager.getWeight() }
    val userExperienceRaw = remember { prefsManager.getExperience() }

    // --- Parsed values (numbers) ---
    val userAge = remember(userAgeRaw) { userAgeRaw.toIntOrNull() ?: 0 }
    val userHeight = remember(userHeightRaw) { userHeightRaw.toFloatOrNull() ?: 0f }
    val userWeight = remember(userWeightRaw) { userWeightRaw.toFloatOrNull() ?: 0f }
    val experienceYears = remember(userExperienceRaw) {
        parseExperienceToYears(userExperienceRaw)
    }

    // --- Experience factor ---
    val experienceFactor = remember(experienceYears) {
        when {
            experienceYears < 0.5f -> 0.7f
            experienceYears < 2f -> 1.0f
            else -> 1.1f
        }
    }

    // --- Age factor ---
    val ageFactor = remember(userAge) {
        when {
            userAge <= 0 -> 1.0f
            userAge < 16 -> 0.8f
            userAge < 20 -> 0.9f
            userAge < 40 -> 1.0f
            userAge < 55 -> 0.9f
            else -> 0.8f
        }
    }

    // --- BMI factor ---
    val bmi = remember(userHeight, userWeight) {
        val hMeters = when {
            userHeight <= 0f -> 0f
            userHeight > 3f -> userHeight / 100f
            else -> userHeight
        }
        if (hMeters > 0f && userWeight > 0f) {
            userWeight / (hMeters * hMeters)
        } else null
    }

    val bmiFactor = remember(bmi) {
        when {
            bmi == null -> 1.0f
            bmi < 18f -> 0.85f
            bmi < 26f -> 1.00f
            bmi < 30f -> 0.95f
            else -> 0.90f
        }
    }

    // --- Recovery Factor ---
    val recoveryFactor = remember(experienceFactor, ageFactor, bmiFactor) {
        (experienceFactor * ageFactor * bmiFactor)
            .coerceIn(0.6f, 1.2f)
    }

    // --- Base caps ---
    val baseLowCap = 30f
    val baseMedCap = 60f
    val baseHighCap = 90f

    val lowCap = baseLowCap * recoveryFactor
    val medCap = baseMedCap * recoveryFactor
    val highCap = baseHighCap * recoveryFactor

    val target = (wr / highCap).coerceIn(0f, 1f)

    val anim = remember { Animatable(0f) }
    var fill by remember { mutableStateOf(0f) }

    LaunchedEffect(target) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = target,
            animationSpec = tween(900, 100, LinearOutSlowInEasing)
        )
    }

    LaunchedEffect(anim) {
        snapshotFlow { anim.value }.collect { fill = it }
    }

    // --- Dynamic Theme Colors ---
    val glow = theme.primary
    val trackBrush = Brush.verticalGradient(
        listOf(theme.background, theme.tertiary.copy(alpha = 0.6f))
    )
    val innerHighlight = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.08f),
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.10f)
    )
    val barBrush = Brush.horizontalGradient(
        listOf(theme.secondary, glow)
    )

    val zone = when {
        wr < lowCap -> "Low"
        wr < medCap -> "Medium"
        else -> "High"
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = muscle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White, // Keep white for readability against dark theme backgrounds
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.0f WR".format(wr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(theme.primary.copy(alpha = 0.15f)) // Tinted background based on theme
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = zone,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primary, // Colored text for the zone
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(CircleShape)
                .background(trackBrush)
                .drawWithCache {
                    val corner = CornerRadius(size.minDimension, size.minDimension)
                    val third = size.width / 3f
                    onDrawBehind {
                        drawRoundRect(
                            brush = innerHighlight,
                            cornerRadius = corner,
                            alpha = 1f
                        )
                        drawRect(
                            Color(0x33FFFFFF),
                            topLeft = Offset(third, 0f),
                            size = Size(1.dp.toPx(), size.height)
                        )
                        drawRect(
                            Color(0x33FFFFFF),
                            topLeft = Offset(third * 2f, 0f),
                            size = Size(1.dp.toPx(), size.height)
                        )
                    }
                }
                .padding(horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .graphicsLayer {
                        shape = CircleShape
                        clip = true
                        shadowElevation = 10.dp.toPx()
                        spotShadowColor = glow.copy(alpha = 0.45f)
                        ambientShadowColor = glow.copy(alpha = 0.30f)
                    }
                    .background(barBrush)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            if (fill > 0f) {
                                val tipX = size.width
                                drawRect(
                                    brush = Brush.radialGradient(
                                        listOf(
                                            glow.copy(alpha = 0.75f),
                                            Color.Transparent
                                        ),
                                        center = Offset(tipX, size.height / 2f),
                                        radius = 22f
                                    )
                                )
                            }
                        }
                    }
            )
        }
    }
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

@Composable
fun WorkoutHighlightItem(
    workout: Workout
) {
    val reps = workout.reps ?: 0
    val sets = workout.sets ?: 0
    val name = workout.name
    val matches = remember(name) {
        nameToMusclesWeighted.firstOrNull { it.first.containsMatchIn(name) }?.second.orEmpty()
            .sortedByDescending { it.second }
            .take(2)
    }
    val weightedReps = matches.map { it.first to (reps * sets * it.second) }
    val lowCap = 30f
    val medCap = 60f
    val highCap = 90f
    val zoneCap = highCap
    val targets = weightedReps.map { (m, wr) -> m to (wr.toFloat() / zoneCap).coerceIn(0f, 1f) }

    val animA = remember { Animatable(0f) }
    val animB = remember { Animatable(0f) }
    var fillA by remember { mutableStateOf(0f) }
    var fillB by remember { mutableStateOf(0f) }
    val aTarget = targets.getOrNull(0)?.second ?: 0f
    val bTarget = targets.getOrNull(1)?.second ?: 0f

    LaunchedEffect(aTarget, bTarget) {
        animA.snapTo(0f); animB.snapTo(0f)
        animA.animateTo(aTarget, tween(900, 100, LinearOutSlowInEasing))
        animB.animateTo(bTarget, tween(900, 100, LinearOutSlowInEasing))
    }
    LaunchedEffect(animA) { snapshotFlow { animA.value }.collect { fillA = it } }
    LaunchedEffect(animB) { snapshotFlow { animB.value }.collect { fillB = it } }

    val glow = Color(0xFFA43434)
    val trackBrush = Brush.verticalGradient(listOf(Color(0xFF160C0C), Color(0xFF1E0E0E)))
    val innerHighlight = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.08f),
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.10f)
    )
    val barBrush = Brush.horizontalGradient(listOf(Color(0xFF5A1E1E), glow))

    fun zoneText(v: Float): String = when {
        v < lowCap -> "Low"
        v < medCap -> "Medium"
        else -> "High"
    }

    @Composable
    fun RowBar(label: String, wr: Float, fill: Float) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.0f WR".format(wr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = zoneText(wr),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(trackBrush)
                    .drawWithCache {
                        val corner = CornerRadius(size.minDimension, size.minDimension)
                        val third = size.width / 3f
                        onDrawBehind {
                            drawRoundRect(brush = innerHighlight, cornerRadius = corner, alpha = 1f)
                            drawRect(Color(0x33FFFFFF), topLeft = Offset(third, 0f), size = Size(1.dp.toPx(), size.height))
                            drawRect(Color(0x33FFFFFF), topLeft = Offset(third * 2f, 0f), size = Size(1.dp.toPx(), size.height))
                        }
                    }
                    .padding(horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .graphicsLayer {
                            shape = CircleShape
                            clip = true
                            shadowElevation = 10.dp.toPx()
                            spotShadowColor = glow.copy(alpha = 0.45f)
                            ambientShadowColor = glow.copy(alpha = 0.30f)
                        }
                        .background(barBrush)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                if (fill > 0f) {
                                    val tipX = size.width
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            listOf(glow.copy(alpha = 0.75f), Color.Transparent),
                                            center = Offset(tipX, size.height / 2f),
                                            radius = 22f
                                        )
                                    )
                                }
                            }
                        }
                )
            }
        }
    }

    val aLabel = targets.getOrNull(0)?.first?.name ?: "—"
    val bLabel = targets.getOrNull(1)?.first?.name ?: "—"
    val aWr = weightedReps.getOrNull(0)?.second?.toFloat() ?: 0f
    val bWr = weightedReps.getOrNull(1)?.second?.toFloat() ?: 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x19000000))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            RowBar(aLabel, aWr, fillA)
            RowBar(bLabel, bWr, fillB)
        }
    }
}

