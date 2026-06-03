@file:Suppress("NAME_SHADOWING")

package com.forgecompose.workouttracker.coaching

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.AppearanceOptionsAppTheme
import com.forgecompose.workouttracker.AppearanceOptionsManagerAppTheme
import com.forgecompose.workouttracker.ColorSchemeAppTheme
import com.forgecompose.workouttracker.PerformanceOptions
import com.forgecompose.workouttracker.PerformanceOptionsManager
import com.forgecompose.workouttracker.muscle.MuscleGroups
import com.forgecompose.workouttracker.ui.components.AnimatedBackdrop
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoachingGoalsScreen(navController: NavController) {
    val context = LocalContext.current
    val coachVm: CoachingViewModel = rememberCoachingViewModel()

    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles

    val goals by coachVm.goals.collectAsStateWithLifecycle()
    val signals by coachVm.signals.collectAsStateWithLifecycle()
    val plan by coachVm.currentPlan.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }

    val introColors = remember(theme) {
        listOf(theme.secondary.copy(alpha = 0.8f), theme.tertiary, theme.background, theme.background)
    }
    val introBrush = remember(introColors) { Brush.horizontalGradient(introColors) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Goals & Progress",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(theme.primary, theme.primary, Color.White)),
                            fontSize = 24.sp, fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = theme.primary, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Add goal")
            }
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item("week_progress") { WeekProgressCard(signals, plan?.weeklySetTargets ?: emptyMap(), theme) }

            item("goals_header") {
                Text("Your goals", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            if (goals.isEmpty()) {
                item("no_goals") {
                    CoachCard(theme) {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Flag, contentDescription = null, tint = theme.primary, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No goals yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Set a goal to keep your weekly plans pointed in one direction.",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        theme = theme,
                        onProgress = { coachVm.updateGoalProgress(goal.id, it) },
                        onStatus = { coachVm.setGoalStatus(goal.id, it) },
                        onDelete = { coachVm.removeGoal(goal.id) }
                    )
                }
            }
            item("spacer") { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) {
        AddGoalDialog(
            theme = theme,
            onDismiss = { showAdd = false },
            onConfirm = { title, desc, muscles, weeks ->
                coachVm.addGoal(title, desc, muscles, weeks)
                showAdd = false
            }
        )
    }
}

@Composable
private fun WeekProgressCard(
    signals: List<MuscleSignal>,
    targets: Map<String, Int>,
    theme: ColorSchemeAppTheme
) {
    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Text("This week's volume", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            val overall = if (signals.isEmpty()) 0f
            else signals.map { it.targetCompletion.coerceIn(0f, 1f) }.average().toFloat()
            Text(
                "${(overall * 100).roundToInt()}% of weekly targets hit across all muscles",
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            if (signals.isEmpty()) {
                Text("Open the AI Coach tab to compute your live muscle signals.", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            } else {
                signals.sortedBy { it.targetCompletion }.take(8).forEach { sig ->
                    val target = targets[sig.muscle]
                    VolumeBar(
                        muscle = sig.muscle,
                        progress = sig.targetCompletion.coerceIn(0f, 1.2f),
                        detail = if (target != null) "${sig.weeklyProgress.roundToInt()}/$target sets" else "${(sig.targetCompletion * 100).roundToInt()}%",
                        color = bandColor(sig.band, theme)
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun VolumeBar(muscle: String, progress: Float, detail: String, color: Color) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "vol")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(muscle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text(detail, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: CoachingGoal,
    theme: ColorSchemeAppTheme,
    onProgress: (Int) -> Unit,
    onStatus: (GoalStatus) -> Unit,
    onDelete: () -> Unit
) {
    var sliderVal by remember(goal.id, goal.progressPct) { mutableStateOf(goal.progressPct.toFloat()) }
    var showMenu by remember { mutableStateOf(false) }
    val achieved = goal.statusEnum == GoalStatus.ACHIEVED

    CoachCard(theme) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = goal.progressPct / 100f, color = if (achieved) Color(0xFF3FB984) else theme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (goal.description.isNotBlank()) {
                        Text(goal.description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(goal.statusEnum, theme)
                        Spacer(Modifier.width(8.dp))
                        Text("${goal.targetWeeks}-week target", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.White.copy(alpha = 0.6f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (goal.statusEnum != GoalStatus.ACHIEVED) {
                            DropdownMenuItem(text = { Text("Mark achieved") }, onClick = { onProgress(100); showMenu = false })
                        }
                        if (goal.statusEnum == GoalStatus.ACTIVE) {
                            DropdownMenuItem(text = { Text("Pause") }, onClick = { onStatus(GoalStatus.PAUSED); showMenu = false })
                        } else {
                            DropdownMenuItem(text = { Text("Resume") }, onClick = { onStatus(GoalStatus.ACTIVE); showMenu = false })
                        }
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false })
                    }
                }
            }
            if (goal.focusMuscles.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(goal.focusMuscles.joinToString(" · "), color = theme.primary.copy(alpha = 0.85f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                onValueChangeFinished = { onProgress(sliderVal.roundToInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = theme.primary, activeTrackColor = theme.primary)
            )
            Text("${sliderVal.roundToInt()}% complete", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.1f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text("${(animated * 100).roundToInt()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(status: GoalStatus, theme: ColorSchemeAppTheme) {
    val (label, color) = when (status) {
        GoalStatus.ACTIVE -> "Active" to theme.primary
        GoalStatus.ACHIEVED -> "Achieved" to Color(0xFF3FB984)
        GoalStatus.PAUSED -> "Paused" to Color(0xFFE0A23C)
    }
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddGoalDialog(
    theme: ColorSchemeAppTheme,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var weeks by remember { mutableStateOf(8f) }
    val selectedMuscles = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.background,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        confirmButton = {
            TextButton(onClick = { onConfirm(title, desc, selectedMuscles.toList(), weeks.roundToInt()) }, enabled = title.isNotBlank()) {
                Text("Add goal", color = if (title.isNotBlank()) theme.primary else Color.White.copy(alpha = 0.3f))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) } },
        title = { Text("New goal", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = dialogFieldColors(theme), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Notes (optional)") }, maxLines = 3,
                    colors = dialogFieldColors(theme), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("Focus muscles", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroups.entries.forEach { m ->
                        val sel = m.name in selectedMuscles
                        FilterChip(
                            selected = sel,
                            onClick = { if (sel) selectedMuscles.remove(m.name) else selectedMuscles.add(m.name) },
                            label = { Text(m.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primary.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Target horizon: ${weeks.roundToInt()} weeks", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Slider(
                    value = weeks, onValueChange = { weeks = it }, valueRange = 1f..24f,
                    colors = SliderDefaults.colors(thumbColor = theme.primary, activeTrackColor = theme.primary)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dialogFieldColors(theme: ColorSchemeAppTheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = theme.primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = theme.primary,
    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
    cursorColor = theme.primary
)
