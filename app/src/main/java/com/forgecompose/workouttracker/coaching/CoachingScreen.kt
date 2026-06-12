@file:Suppress("NAME_SHADOWING")

package com.forgecompose.workouttracker.coaching

import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.AiModelProvider
import com.forgecompose.workouttracker.health.HealthConnectManager
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.SurveyTape
import com.forgecompose.workouttracker.profile.UserPreferencesManager
import com.forgecompose.workouttracker.ui.components.AnimatedBackdrop
import com.forgecompose.workouttracker.workout.MainScreenViewModel
import com.forgecompose.workouttracker.workout.WorkoutListUiState
import com.forgecompose.workouttracker.workout.WorkoutListViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/* ------------------------------------------------------------------ */
/* Band → colour / label helpers shared across coaching screens.       */
/* ------------------------------------------------------------------ */

internal fun bandColor(band: String, theme: ColorSchemeAppTheme): Color = when (band) {
    "NotTrained", "SlightlyTrained" -> Color(0xFF5B8DEF)
    "Building" -> theme.primary
    "OnTrack" -> Color(0xFF3FB984)
    "Recovering" -> Color(0xFFE0A23C)
    "Overreached", "DeloadRecommended" -> Color(0xFFE05C5C)
    else -> theme.secondary
}

internal fun prettyBand(band: String): String =
    band.replace(Regex("([a-z])([A-Z])"), "$1 $2")

/**
 * Returns a [CoachingViewModel] scoped to the host Activity so the Coaching screen
 * and the Goals screen share the same instance (and the same computed signals,
 * current plan, and goal list). Mirrors the activity-scoping used by the existing
 * Gemini advice generator.
 */
@Composable
internal fun rememberCoachingViewModel(): CoachingViewModel {
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is ComponentActivity) break
            c = c.baseContext
        }
        c as? ComponentActivity
    }
    return if (activity != null) viewModel(viewModelStoreOwner = activity) else viewModel()
}

/* ------------------------------------------------------------------ */
/* Route                                                               */
/* ------------------------------------------------------------------ */

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingRoute(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val context = LocalContext.current
    val coachVm: CoachingViewModel = rememberCoachingViewModel()

    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val genState by coachVm.genState.collectAsStateWithLifecycle()
    val currentPlan by coachVm.currentPlan.collectAsStateWithLifecycle()
    val provider by coachVm.provider.collectAsStateWithLifecycle()
    val signals by coachVm.signals.collectAsStateWithLifecycle()

    var recovery by remember { mutableStateOf(RecoverySnapshot()) }
    var computingSignals by remember { mutableStateOf(true) }
    var planRequestBase by remember { mutableStateOf<PlanRequest?>(null) }
    var promptText by remember { mutableStateOf("") }

    val introColors = remember(theme) {
        listOf(theme.secondary.copy(alpha = 0.8f), theme.tertiary, theme.background, theme.background)
    }
    val introBrush = remember(introColors) { Brush.horizontalGradient(introColors) }

    // Compute the live muscle signals once history is available (reuses the same
    // engine + data sources as the Muscle Status screen).
    LaunchedEffect(uiState) {
        val success = uiState as? WorkoutListUiState.Success ?: return@LaunchedEffect
        computingSignals = true
        val now = Instant.now()
        val start = now.minus(30, ChronoUnit.DAYS)

        val recent = success.workouts.sortedByDescending { it.date }.take(40).map { w ->
            val token = buildString {
                append(w.name.lowercase())
                w.sets?.takeIf { it > 0 }?.let { append(" $it sets") }
                w.reps?.takeIf { it > 0 }?.let { append(" $it reps") }
                (w.sessionRpe ?: w.rpe)?.takeIf { it > 0 }?.let { append(" rpe $it") }
                if ((w.weight ?: 0.0) > 0.0) append(" heavy")
            }
            WorkoutSummary(
                date = Instant.ofEpochMilli(w.date),
                name = w.name,
                exercises = listOf(token),
                environment = w.trainingEnvironment,
                sessionRpe = w.sessionRpe ?: w.rpe,
                fatigueLevel = w.fatigueLevel,
                restPeriodSeconds = w.restPeriodSeconds,
                durationMinutes = w.durationMillis?.takeIf { it > 0 }?.let { it / 60000f },
                sets = w.sets, reps = w.reps, weight = w.weight, distance = w.distance,
                heartRateAvg = w.heartRateAvg, heartRateMax = w.heartRateMax,
                systemicDrainScore = w.systemicDrainScore
            )
        }

        SurveyTape.init(context)
        val prefsManager = UserPreferencesManager(context)
        val profile = buildUserProfile(prefsManager)
        val surveyContent = SurveyTape.readTape()
        val survey = parseSurveyTape(surveyContent)
        val sprintGoal = SprintGoalPreferences(context).load()
        val hc = HealthConnectManager(context)

        val (loads, factors) = runCatching {
            deriveMuscleLoadsStepwise(
                now = now,
                recent = recent,
                profile = profile,
                surveyTapeContent = surveyContent,
                sprintGoal = sprintGoal,
                onStep = { _, _, _, _ -> },
                sleepSessions = hc.readSleepSessions(start, now),
                oxygenSaturations = hc.readOxygenSaturation(start, now),
                nutrition = hc.readNutrition(now.minus(2, ChronoUnit.DAYS), now),
                bodyFat = hc.readBodyFat(start, now),
                caloriesBurned = hc.readTotalCalories(now.minus(7, ChronoUnit.DAYS), now),
                readHeartRate = { s, e -> hc.readHeartRateRecords(s, e) }
            )
        }.getOrNull() ?: (emptyList<MuscleLoad>() to null)

        val mapped = loads.map { l ->
            MuscleSignal(
                muscle = l.group.name,
                band = l.band.name,
                weeklyProgress = l.weeklyProgress,
                weeklyTarget = l.weeklyTarget,
                injuryRiskPct = (l.injuryRisk * 100f).roundToInt().coerceIn(0, 100),
                adaptationScore = l.adaptationScore,
                consistencyScore = l.consistencyScore,
                developmentScore = l.developmentScore,
                lastTrainedAgo = l.lastTrainedAgo
            )
        }
        coachVm.updateSignals(mapped)
        factors?.let {
            recovery = RecoverySnapshot(
                recoveryEfficacy = it.recoveryEfficacy,
                sleepHours = it.sleepHours,
                restingHeartRate = it.restingHeartRate,
                proteinGrams = it.proteinGrams,
                environmentStress = it.recentEnvironmentStress
            )
        }
        planRequestBase = PlanRequest(
            userPrompt = "",
            goalTitle = coachVm.goals.value.firstOrNull { it.statusEnum == GoalStatus.ACTIVE }?.title.orEmpty(),
            signals = mapped,
            recovery = recovery,
            experience = profile.experience,
            preferredStyle = profile.preferredStyle,
            importantMuscles = profile.importantMuscles.map { it.name },
            daysAvailable = parseDays(survey.daysAvailable),
            sessionMinutes = parseMinutes(survey.sessionLength),
            equipment = survey.equipmentAccess
        )
        computingSignals = false
    }

    fun launchGeneration() {
        val base = planRequestBase ?: return
        coachVm.generatePlan(base.copy(userPrompt = promptText.trim(), signals = signals, recovery = recovery))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Coach",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(theme.primary, theme.primary, Color.White)),
                            fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("CoachingGoals") }) {
                        Icon(Icons.Filled.Flag, contentDescription = "Goals", tint = theme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        AnimatedBackdrop(
            introBrush = introBrush,
            introAlpha = 1f,
            enableWaves = movingEffectsEnabled,
            enableAnimation = movingEffectsEnabled
        )

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
            ) {
                item("provider") {
                    ProviderSelectorCard(
                        selected = provider,
                        readiness = { coachVm.providerReady(it) },
                        onSelect = { coachVm.setProvider(it) },
                        theme = theme
                    )
                }
                item("prompt") {
                    PromptCard(
                        prompt = promptText,
                        onPromptChange = { promptText = it },
                        onGenerate = { launchGeneration() },
                        isGenerating = genState is PlanGenState.Loading,
                        ready = planRequestBase != null,
                        theme = theme
                    )
                }
                item("readiness") {
                    ReadinessStrip(signals = signals, computing = computingSignals, theme = theme)
                }
                when (val s = genState) {
                    is PlanGenState.Loading -> item("loading") { GeneratingCard(stage = s.stage, theme = theme) }
                    is PlanGenState.Error -> item("error") { ErrorCard(s.message, theme) }
                    else -> {}
                }
                val plan = currentPlan
                if (plan != null) {
                    item("plan_header") { PlanHeaderCard(plan, theme) }
                    items(plan.days, key = { it.dayName }) { day -> DayCard(day, theme) }
                    item("plan_footer") { PlanFooter(plan, theme) { navController.navigate("CoachingGoals") } }
                } else if (!computingSignals && genState !is PlanGenState.Loading) {
                    item("empty") { EmptyCoachCard(theme) }
                }
                item("spacer") { Spacer(Modifier.height(24.dp)) }
            }

            FloatingTaskbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navController = navController,
                cornerRadius = 32.dp,
                iconAlpha = 1f,
                uiState = uiState
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Provider selector                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun ProviderSelectorCard(
    selected: AiModelProvider,
    readiness: (AiModelProvider) -> Boolean,
    onSelect: (AiModelProvider) -> Unit,
    theme: ColorSchemeAppTheme
) {
    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Text("Coaching engine", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose which model designs your plan.",
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            val options = listOf(
                Triple(AiModelProvider.EDGE_ON_DEVICE, "On-Device", Icons.Filled.PhoneAndroid),
                Triple(AiModelProvider.LOCAL_NETWORK, "Local", Icons.Filled.Lan),
                Triple(AiModelProvider.GOOGLE_AI_STUDIO, "Gemini", Icons.Filled.AutoAwesome)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { (p, label, icon) ->
                    val isSel = p == selected
                    val ready = readiness(p)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) theme.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (isSel) theme.primary else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(p) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, contentDescription = label, tint = if (isSel) theme.primary else Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(6.dp))
                        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (ready) "Ready" else "Set up",
                            color = if (ready) Color(0xFF3FB984) else Color.White.copy(alpha = 0.45f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            if (!readiness(selected)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "This engine isn't configured — you'll still get a smart plan from ForgeTrack's built-in coach.",
                    color = Color(0xFFE0A23C), fontSize = 11.sp
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Prompt input                                                        */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean,
    ready: Boolean,
    theme: ColorSchemeAppTheme
) {
    val quickPrompts = listOf(
        "Build muscle, 4 days", "Get stronger on big lifts", "Lean out, keep muscle",
        "Bring up my weak points", "Go easy — I'm beat up"
    )
    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Text("Tell your coach what you want", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Focus on my shoulders and back, 4 days, 45 min sessions", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = theme.primary,
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                )
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPrompts.forEach { q ->
                    AssistChip(
                        onClick = { onPromptChange(q) },
                        label = { Text(q, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = Color.White.copy(alpha = 0.85f)
                        ),
                        border = null
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGenerate,
                enabled = ready && !isGenerating,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.White)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Generating…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ready) "Generate weekly plan" else "Reading your data…", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Readiness strip                                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun ReadinessStrip(signals: List<MuscleSignal>, computing: Boolean, theme: ColorSchemeAppTheme) {
    CoachCard(theme) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live muscle status", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (computing) CircularProgressIndicator(Modifier.size(16.dp), color = theme.primary, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(12.dp))
            if (signals.isEmpty() && !computing) {
                Text(
                    "Log a few workouts and your muscle signals will appear here to drive your plans.",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            } else {
                val sorted = signals.sortedBy { it.targetCompletion }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sorted, key = { it.muscle }) { sig -> MuscleChip(sig, theme) }
                }
            }
        }
    }
}

@Composable
private fun MuscleChip(sig: MuscleSignal, theme: ColorSchemeAppTheme) {
    val c = bandColor(sig.band, theme)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.copy(alpha = 0.12f))
            .border(1.dp, c.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(sig.muscle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        Text(prettyBand(sig.band), color = c, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text("${(sig.targetCompletion * 100).roundToInt()}% vol", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
        if (sig.injuryRiskPct > 50) {
            Text("⚠ ${sig.injuryRiskPct}% risk", color = Color(0xFFE05C5C), fontSize = 9.sp)
        }
    }
}

/* ------------------------------------------------------------------ */
/* Plan rendering                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun PlanHeaderCard(plan: WeeklyPlan, theme: ColorSchemeAppTheme) {
    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(theme.primary.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(plan.source, color = theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(plan.summary, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MiniStat("${plan.trainingDays}", "days", theme)
                MiniStat("${plan.totalExercises}", "exercises", theme)
                MiniStat("${plan.totalSets}", "sets", theme)
                MiniStat("~${plan.estimatedWeeklyMinutes}", "min/wk", theme)
            }
            if (plan.coachNotes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))
                plan.coachNotes.forEach { note ->
                    Row(Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = theme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(note, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, theme: ColorSchemeAppTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

@Composable
private fun DayCard(day: PlanDay, theme: ColorSchemeAppTheme) {
    var expanded by remember { mutableStateOf(!day.isRestDay) }
    val accent = if (day.isRestDay) theme.secondary else theme.primary
    CoachCard(theme) {
        Column(Modifier.fillMaxWidth().animateContentSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (day.isRestDay) Icons.Filled.Bedtime else Icons.Filled.FitnessCenter,
                        contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(day.dayName, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    Text(day.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                if (!day.isRestDay && day.estimatedMinutes > 0) {
                    Text("~${day.estimatedMinutes}m", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null, tint = Color.White.copy(alpha = 0.5f)
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    if (day.intensityNote.isNotBlank()) {
                        Text(day.intensityNote, color = accent.copy(alpha = 0.9f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    if (day.isRestDay) {
                        Text("No lifting scheduled.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    } else {
                        day.exercises.forEachIndexed { i, ex ->
                            ExerciseRow(i + 1, ex, theme)
                            if (i < day.exercises.lastIndex) Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(index: Int, ex: PlanExercise, theme: ColorSchemeAppTheme) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.04f)).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("$index", color = theme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(ex.targetMuscle, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            if (ex.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(ex.notes, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${ex.sets} × ${ex.reps}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("RPE ${ex.rpe}", color = theme.primary, fontSize = 10.sp)
            Text("${ex.restSeconds}s rest", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PlanFooter(plan: WeeklyPlan, theme: ColorSchemeAppTheme, onOpenGoals: () -> Unit) {
    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Text("Weekly volume targets", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            plan.weeklySetTargets.entries.sortedByDescending { it.value }.take(8).forEach { (m, sets) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text("$sets sets", color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onOpenGoals,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View goals & progress")
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* State cards                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun GeneratingCard(stage: String, theme: ColorSchemeAppTheme) {
    val alpha by animateFloatAsState(0.6f, animationSpec = tween(600, easing = LinearEasing), label = "pulse")
    CoachCard(theme) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), color = theme.primary, strokeWidth = 2.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Coaching in progress", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("$stage…", color = Color.White.copy(alpha = alpha), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, theme: ColorSchemeAppTheme) {
    CoachCard(theme) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFE05C5C))
            Spacer(Modifier.width(12.dp))
            Text(message, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyCoachCard(theme: ColorSchemeAppTheme) {
    CoachCard(theme) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = theme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("No plan yet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tell the coach what you want above and generate a week built around your live muscle recovery.",
                color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Shared card shell                                                   */
/* ------------------------------------------------------------------ */

@Composable
internal fun CoachCard(theme: ColorSchemeAppTheme, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(theme.tertiary.copy(alpha = 0.55f), theme.background.copy(alpha = 0.75f))
                )
            )
            .border(1.dp, theme.primary.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
    ) { content() }
}

/* ------------------------------------------------------------------ */
/* Parsing helpers                                                     */
/* ------------------------------------------------------------------ */

internal fun parseDays(raw: String): Int =
    Regex("\\d+").find(raw)?.value?.toIntOrNull()?.coerceIn(2, 6) ?: 4

internal fun parseMinutes(raw: String): Int {
    val nums = Regex("\\d+").findAll(raw).map { it.value.toInt() }.toList()
    return when {
        nums.isEmpty() -> 50
        nums.size >= 2 -> ((nums[0] + nums[1]) / 2).coerceIn(20, 120)
        else -> nums[0].coerceIn(20, 120)
    }
}
