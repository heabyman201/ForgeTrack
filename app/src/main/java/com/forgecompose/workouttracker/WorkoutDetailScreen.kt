package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutDetailScreen(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val cfg = LocalConfiguration.current
    val widthDp = cfg.screenWidthDp
    val heightDp = cfg.screenHeightDp
    val isTall = heightDp >= 600
    val isTwoPane = widthDp >= 840 || (widthDp >= 600 && isTall)
    val contentHPad = if (widthDp >= 400) 16.dp else 12.dp
    val cardPad = if (widthDp >= 400) 20.dp else 16.dp
    val titleStyle = if (widthDp >= 400) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium

    val screenContext = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(screenContext)
        .collectAsState(initial = PerformanceOptions.Defaults)

    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles

    val stages = rememberColdStartStages()
    val shouldAnimate = stages.afterFirstFrame
    var animationClock by remember { mutableStateOf(0f) }

    LaunchedEffect(shouldAnimate, movingEffectsEnabled) {
        if (shouldAnimate && movingEffectsEnabled) {
            var lastFrameTime = 0L
            while (true) {
                val currentTime = withFrameNanos { it }
                if (lastFrameTime != 0L) {
                    val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                    animationClock += deltaTime
                }
                lastFrameTime = currentTime
                delay(42)
            }
        }
    }

    val fullPi = 2f * PI.toFloat()
    val waveOffset by remember { derivedStateOf { (animationClock * fullPi / 22f) % fullPi } }
    val glowIntensity by remember { derivedStateOf { 0.4f + 0.2f * sin(animationClock * fullPi / 6f) } }
    val gradientProgress by remember { derivedStateOf { (animationClock / 15f) % 2f } }

    val clampedGlow by remember { derivedStateOf { glowIntensity.coerceIn(0f, 1f) } }

    val particleSeed = remember { Random(42) }
    val particles = remember {
        List(12) { i ->
            val baseX = i / 12f
            val yOff = 0.15f + particleSeed.nextFloat() * 0.25f
            val r = 1.8f + particleSeed.nextFloat() * 2.0f
            Triple(baseX, yOff, r)
        }
    }

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(700, easing = LinearEasing),
        label = "introProgress"
    )
    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(
                Color(0xFF2B1A00),
                Color(0xFF3C2405),
                Color(0xFF5A360A),
                Color(0xFF7A4A12)
            )
            in 11..16 -> listOf(
                Color(0xFF332300),
                Color(0xFF4A3408),
                Color(0xFF6B4B0F),
                Color(0xFF8C6217)
            )
            in 17..20 -> listOf(
                Color(0xFF1A0614),
                Color(0xFF2A0A20),
                Color(0xFF3D0F2D),
                Color(0xFF52153A)
            )
            else -> listOf(
                Color(0xFF02040A),
                Color(0xFF0A1324),
                Color(0xFF15243D),
                Color(0xFF1E3352)
            )
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset(Float.POSITIVE_INFINITY, 0f),
            end = Offset.Zero
        )
    }

    LaunchedEffect(Unit) {
        showIntro = false
        Firebase.crashlytics.setCustomKey("current_screen", "Workout Details screen")
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedId: Int? = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get<Int>("selectedWorkoutId")

    val allWorkouts = remember(uiState) { (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty() }
    val selectedWorkout by remember(selectedId, allWorkouts) {
        derivedStateOf { allWorkouts.firstOrNull { it.id == selectedId } }
    }
    val minuteHrPoints by remember(selectedWorkout?.heartRateTimeline) {
        derivedStateOf { parseMinuteHrTimeline(selectedWorkout?.heartRateTimeline) }
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val sameNameWorkouts by remember(selectedWorkout, allWorkouts) {
        derivedStateOf {
            if (selectedWorkout != null)
                allWorkouts.asSequence().filter { it.name == selectedWorkout!!.name }.sortedBy { it.startTime }.toList()
            else emptyList()
        }
    }

    val previousSame by remember(selectedWorkout, sameNameWorkouts) {
        derivedStateOf {
            selectedWorkout?.let { sameNameWorkouts.lastOrNull { it.startTime < selectedWorkout!!.startTime } }
        }
    }

    val weekStartEnd by remember(selectedWorkout) {
        derivedStateOf {
            if (selectedWorkout == null) null else {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedWorkout!!.date }
                cal.firstDayOfWeek = Calendar.MONDAY
                cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + TimeUnit.DAYS.toMillis(6)
                start to end
            }
        }
    }

    val thisWeekCount by remember(weekStartEnd, allWorkouts) {
        derivedStateOf {
            weekStartEnd?.let { (start, end) ->
                allWorkouts.count { it.date in start until end }
            } ?: 0
        }
    }

    val streak by remember(selectedWorkout, allWorkouts) {
        derivedStateOf { if (selectedWorkout == null) 0 else computeStreak(selectedWorkout!!.date, allWorkouts) }
    }

    val aiEnabled = dynamicModel.personaConfig.value.enabled
    val detailAdviceState = useGeminiAdviceGenerator(
        contextPrompt = "You are a performance coach. Summarize this workout quality, compare with previous similar sessions, and give practical next-step advice."
    )

    LaunchedEffect(selectedWorkout?.id) {
        val workout = selectedWorkout ?: return@LaunchedEffect
        if (!aiEnabled) return@LaunchedEffect

        val currentRpe = workout.sessionRpe ?: workout.rpe ?: 0
        val currentFatigue = workout.fatigueLevel ?: 0
        val previousWeight = previousSame?.weight ?: 0.0
        val currentWeight = workout.weight ?: 0.0
        val detailPhase = when {
            currentFatigue >= 8 || currentRpe >= 9 -> "Deload Window"
            previousWeight > 0.0 && currentWeight > previousWeight * 1.02 && currentFatigue <= 6 -> "Peaking"
            previousWeight > 0.0 && currentWeight < previousWeight * 0.97 -> "Cooling Down"
            currentFatigue >= 6 -> "Recovering"
            else -> "Maintenance"
        }

        val previousSummary = previousSame?.let {
            "Previous same exercise: ${it.sets ?: 0} sets, ${it.reps ?: 0} reps, ${it.weight ?: 0.0} kg, duration ${formatDuration(it.durationMillis)}."
        } ?: "No previous matching session found."

        GeminiAdaptiveMemoryStore.recordWorkoutTrendSnapshot(
            screenContext,
            snapshot = WorkoutTrendSnapshot(
                workoutName = workout.name,
                source = "detail",
                capturedAtEpochMs = System.currentTimeMillis(),
                phase = detailPhase,
                rpe = currentRpe,
                fatigue = currentFatigue,
                weight = workout.weight,
                sets = workout.sets,
                reps = workout.reps,
                intensityScore = workout.intensityScore
            )
        )

        detailAdviceState.generateBatchWithLimit(
            "Workout: ${workout.name} on ${dateFormat.format(Date(workout.date))}. Duration ${formatDuration(workout.durationMillis)}.",
            "Current metrics: sets ${workout.sets ?: 0}, reps ${workout.reps ?: 0}, weight ${workout.weight ?: 0.0}, distance ${workout.distance ?: 0.0}, RPE $currentRpe, fatigue $currentFatigue.",
            "Trend context: phase $detailPhase, week count $thisWeekCount, streak ${streak}d. $previousSummary",
            100
        )
    }

    val cachedAllWorkouts = remember(allWorkouts) { allWorkouts.toList() }

    LaunchedEffect(Unit) {
        taskbarOverride.shouldOverrideVisiblity.value = false
        Log.d("WorkoutDetailScreen", "cold: ${selectedWorkout?.name}")
    }

    val cardioExerciseNames = remember {
        setOf(
            "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
            "Rowing Machine", "Stationary Bike","Swimming"
        ).map { it.lowercase() }.toSet()
    }
    val presetByName = remember(workoutPresets) { workoutPresets.associateBy { it.name.trim().lowercase() } }

    val isCardioName = remember(presetByName) {
        { name: String? ->
            val key = name?.trim()?.lowercase().orEmpty()
            if (key.isBlank()) {
                false
            } else {
                val presetCardio = presetByName[key]?.category.equals("Cardio", ignoreCase = true)
                presetCardio || key in cardioExerciseNames
            }
        }
    }

    val blurLength = length.value.toInt()
    val blurEnabled = performanceOptions.blurEnabled
    val blurIntro by animateDpAsState(
        if (showIntro && blurEnabled) 32.dp else 0.dp,
        animationSpec = tween(durationMillis = blurLength, easing = LinearEasing),
        label = "blurIntro"
    )

    Scaffold(
        modifier = Modifier.forgeSharedBounds("workout-card-$selectedId"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedWorkout?.name ?: "Workout Summary",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    selectedWorkout?.let { workout ->
                        IconButton(onClick = {
                                val intent = ExerciseAnalyticsActivity.newIntent(
                                screenContext,
                                workout.name
                            )
                            screenContext.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Analytics, contentDescription = "View Analytics")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        // Backdrop the frosted ForgeCards/GlassCards sample + blur (the animated waves/orbs).
        val forgeBackdrop = rememberForgeBackdrop()
        CompositionLocalProvider(LocalForgeBackdrop provides forgeBackdrop) {
        AnimatedBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .forgeBackdropSource(forgeBackdrop),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves = movingEffectsEnabled,
            enableAnimation = movingEffectsEnabled
        )
        when (uiState) {
            is WorkoutListUiState.Loading -> LoadingBlock(padding)
            is WorkoutListUiState.Error -> ErrorBlock(padding)
            is WorkoutListUiState.Success -> {
                if (selectedWorkout == null) {
                    MissingBlock(padding)
                } else {
                    if (isTwoPane) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = contentHPad),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val leftState = rememberLazyListState()
                            val rightState = rememberLazyListState()
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                state = leftState,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                item {
                                    AnimatedVisibility(visible = stages.after200ms, enter = fadeIn()) {
                                        GlassCard {
                                            WorkoutOverviewCard(
                                                workout = selectedWorkout!!,
                                                dateLabel = dateFormat.format(Date(selectedWorkout!!.date)),
                                                cardPad = cardPad,
                                                titleStyle = titleStyle,
                                                isCardio = isCardioName(selectedWorkout!!.name)
                                            )
                                        }

                                    }
                                }
                                item {
                                    GlassCard {
                                        AnalysisInsightsCard(
                                            exerciseName = selectedWorkout!!.name,
                                            data = sameNameWorkouts
                                        )
                                    }

                                }
                                item {
                                    AnimatedVisibility(visible = stages.afterFirstFrame, enter = fadeIn()) {
                                        GlassCard {
                                            Column(Modifier.padding(cardPad)) {
                                                SectionTitle("Timing")
                                                val dur = formatDuration(selectedWorkout!!.durationMillis)
                                                val start = timeFormat.format(Date(selectedWorkout!!.startTime))
                                                val end = selectedWorkout!!.endTime?.let { timeFormat.format(Date(it)) } ?: "--"
                                                DetailStatRow(
                                                    firstLabel = "Duration",
                                                    firstValue = dur,
                                                    secondLabel = "Start",
                                                    secondValue = start,
                                                    thirdLabel = "End",
                                                    thirdValue = end
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    AnimatedVisibility(visible = stages.after600ms, enter = fadeIn()) {
                                        GlassCard {
                                            Column(Modifier.padding(cardPad)) {
                                                SectionTitle("Highlights")
                                                DetailStatRow(
                                                    firstLabel = "This Week",
                                                    firstValue = thisWeekCount.toString(),
                                                    secondLabel = "Streak",
                                                    secondValue = "${streak}d",
                                                    thirdLabel = "Last Time",
                                                    thirdValue = previousSame?.let { fmtAgo(it.date, selectedWorkout!!.date) } ?: "--"
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    AnimatedVisibility(visible = stages.after600ms, enter = fadeIn()) {
                                        GlassCard {
                                            DetailedAdviceCard(
                                                advice = detailAdviceState.currentAdvice,
                                                isLoading = detailAdviceState.isLoading
                                            )
                                        }
                                    }
                                }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                .fillMaxHeight(),
                                state = rightState,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                if (minuteHrPoints.isNotEmpty()) {
                                    item {
                                        AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                            GlassCard {
                                                Column(Modifier.padding(cardPad)) {
                                                    SectionTitle("Heart Rate Timeline")
                                                    MinuteHeartRateZoneGraph(
                                                        points = minuteHrPoints,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(220.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (cachedAllWorkouts.isNotEmpty()) {
                                    item {
                                        AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                            GlassCard {
                                                Column(Modifier.padding(cardPad)) {
                                                    SectionTitle("Average HR Progression")
                                                    key(selectedWorkout!!.name) {
                                                        ExerciseAverageHrProgressionGraph(
                                                            exerciseName = selectedWorkout!!.name,
                                                            workouts = cachedAllWorkouts
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = contentHPad),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                AnimatedVisibility(visible = stages.after200ms, enter = fadeIn()) {
                                    GlassCard {
                                        WorkoutOverviewCard(
                                            workout = selectedWorkout!!,
                                            dateLabel = dateFormat.format(Date(selectedWorkout!!.date)),
                                            cardPad = cardPad,
                                            titleStyle = titleStyle,
                                            isCardio = isCardioName(selectedWorkout!!.name)
                                        )
                                    }
                                }
                            }
                            item {
                                GlassCard {
                                    AnalysisInsightsCard(
                                        exerciseName = selectedWorkout!!.name,
                                        data = sameNameWorkouts
                                    )
                                }
                            }
                            if (selectedWorkout!!.notes != null) {

                                item {
                                    AnimatedVisibility(
                                        visible = stages.after600ms,
                                        enter = fadeIn()
                                    ) {
                                        val currentNotes = selectedWorkout!!.notes.orEmpty()
                                        var tempNote by remember(currentNotes) { mutableStateOf(currentNotes) }
                                        var editMode by remember { mutableStateOf(false) }
                                        GlassCard {
                                            Column(
                                                modifier = Modifier.padding(cardPad),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        SectionTitle("Notes")
                                                        Text(
                                                            text = if (editMode) "Update your workout notes" else "Captured thoughts from this session",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.White.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (!editMode) {
                                                                tempNote = currentNotes
                                                            }
                                                            editMode = !editMode
                                                        },
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            tint = Color.White,
                                                            contentDescription = "Edit notes"
                                                        )
                                                    }
                                                }

                                                if (!editMode) {
                                                    Text(
                                                        text = currentNotes,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White.copy(alpha = 0.9f)
                                                    )
                                                } else {
                                                    TextField(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        value = tempNote,
                                                        onValueChange = { tempNote = it },
                                                        minLines = 4,
                                                        textStyle = MaterialTheme.typography.bodyMedium,
                                                        label = { Text("Workout notes") }
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        Button(onClick = {
                                                            selectedWorkout!!.notes = tempNote
                                                            viewModel.updateWorkoutNotes(
                                                                workoutId = selectedWorkout!!.id.toLong(),
                                                                notes = tempNote
                                                            )
                                                            editMode = false
                                                        }) {
                                                            Text(text = "Save")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(visible = stages.afterFirstFrame, enter = fadeIn()) {
                                    GlassCard {
                                        Column(Modifier.padding(cardPad)) {
                                            SectionTitle("Timing")
                                            val dur = formatDuration(selectedWorkout!!.durationMillis)
                                            val start = timeFormat.format(Date(selectedWorkout!!.startTime))
                                            val end = selectedWorkout!!.endTime?.let { timeFormat.format(Date(it)) } ?: "--"
                                            DetailStatRow(
                                                firstLabel = "Duration",
                                                firstValue = dur,
                                                secondLabel = "Start",
                                                secondValue = start,
                                                thirdLabel = "End",
                                                thirdValue = end
                                            )
                                        }
                                    }
                                }
                            }
                            if (minuteHrPoints.isNotEmpty()) {
                                item {
                                    AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                        GlassCard {
                                            Column(Modifier.padding(cardPad)) {
                                                SectionTitle("Heart Rate Timeline")
                                                MinuteHeartRateZoneGraph(
                                                    points = minuteHrPoints,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(220.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(visible = stages.after600ms, enter = fadeIn()) {
                                    GlassCard {
                                        Column(Modifier.padding(cardPad)) {
                                            SectionTitle("Highlights")
                                            DetailStatRow(
                                                firstLabel = "This Week",
                                                firstValue = thisWeekCount.toString(),
                                                secondLabel = "Streak",
                                                secondValue = "${streak}d",
                                                thirdLabel = "Last Time",
                                                thirdValue = previousSame?.let { fmtAgo(it.date, selectedWorkout!!.date) } ?: "--"
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(visible = stages.after600ms, enter = fadeIn()) {
                                    GlassCard {
                                        DetailedAdviceCard(
                                            advice = detailAdviceState.currentAdvice,
                                            isLoading = detailAdviceState.isLoading
                                        )
                                    }
                                }
                            }
                            if (cachedAllWorkouts.isNotEmpty()) {
                                item {
                                    AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                        GlassCard {
                                            Column(Modifier.padding(cardPad)) {
                                                SectionTitle("Average HR Progression")
                                                key(selectedWorkout!!.name) {
                                                    ExerciseAverageHrProgressionGraph(
                                                        exerciseName = selectedWorkout!!.name,
                                                        workouts = cachedAllWorkouts
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutOverviewCard(
    workout: Workout,
    dateLabel: String,
    cardPad: Dp,
    titleStyle: TextStyle,
    isCardio: Boolean
) {
    Column(
        modifier = Modifier.padding(cardPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = workout.name,
            style = titleStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        StatusChip(workout.status)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                label = dateLabel,
                icon = Icons.Filled.DateRange
            )
            if (isCardio) {
                InfoChip(
                    label = workout.distance?.let { "${"%.2f".format(Locale.getDefault(), it)} km" } ?: "No distance recorded",
                    icon = Icons.Filled.FitnessCenter
                )
            } else {
                InfoChip(
                    label = workout.sets?.let { "$it sets" } ?: "No sets recorded",
                    icon = Icons.Filled.FitnessCenter
                )
                InfoChip(
                    label = workout.reps?.let { "$it reps" } ?: "No reps recorded",
                    icon = Icons.Filled.FitnessCenter
                )
                InfoChip(
                    label = workout.weight?.let { "${"%.1f".format(Locale.getDefault(), it)} kg" } ?: "No weight recorded",
                    icon = Icons.Filled.FitnessCenter
                )
            }
        }
    }
}

@Composable
private fun DetailStatRow(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    thirdLabel: String,
    thirdValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LabeledStat(label = firstLabel, value = firstValue, modifier = Modifier.weight(1f))
        LabeledStat(label = secondLabel, value = secondValue, modifier = Modifier.weight(1f))
        LabeledStat(label = thirdLabel, value = thirdValue, modifier = Modifier.weight(1f))
    }
}

fun formatDuration(millis: Long?): String {
    if (millis == null) return "--"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun fmtAgo(prev: Long, current: Long): String {
    val diff = current - prev
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}

fun computeStreak(lastDate: Long, all: List<Workout>): Int {
    val sorted = all.asSequence().map { it.date }.distinct().sortedDescending().toList()
    if (sorted.isEmpty()) return 0
    var streak = 0
    var current = lastDate
    for (date in sorted) {
        val diff = current - date
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days <= 1L) {
            streak++
            current = date
        } else {
            break
        }
    }
    return streak
}

@Composable
private fun DetailedAdviceCard(
    advice: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "AI insight",
                tint = Color(0xFFFFD54F)
            )
            Text(
                text = "Detailed AI Insight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = if (isLoading) "Analyzing your progress and generating detailed advice..." else advice,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

private data class MinuteHrPoint(val minute: Int, val bpm: Int)

private fun parseMinuteHrTimeline(raw: String?): List<MinuteHrPoint> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",")
        .mapNotNull { token ->
            val parts = token.split(":", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val minute = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
            val bpm = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
            if (minute <= 0 || bpm <= 0) return@mapNotNull null
            MinuteHrPoint(minute = minute, bpm = bpm)
        }
        .sortedBy { it.minute }
        .distinctBy { it.minute }
}

private fun zoneColorForBpm(bpm: Int, estimatedMaxHr: Int): Color {
    val ratio = bpm.toFloat() / estimatedMaxHr.coerceAtLeast(1).toFloat()
    return when {
        ratio < 0.60f -> Color(0xFF4FC3F7) // Zone 1
        ratio < 0.70f -> Color(0xFF66BB6A) // Zone 2
        ratio < 0.80f -> Color(0xFFFFCA28) // Zone 3
        ratio < 0.90f -> Color(0xFFFF8A65) // Zone 4
        else -> Color(0xFFEF5350)          // Zone 5
    }
}

@Composable
private fun MinuteHeartRateZoneGraph(
    points: List<MinuteHrPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesManager(context) }
    val age = remember { prefs.getAge().toIntOrNull() }
    val estimatedMaxHr = remember(age) {
        val safeAge = age?.takeIf { it in 10..95 }
        if (safeAge != null) 220 - safeAge else 190
    }

    val minMinute = points.minOfOrNull { it.minute } ?: 1
    val maxMinute = points.maxOfOrNull { it.minute } ?: 1
    val minBpm = (points.minOfOrNull { it.bpm } ?: 60).coerceAtLeast(40)
    val maxBpm = (points.maxOfOrNull { it.bpm } ?: 160).coerceAtLeast(minBpm + 10)
    val bpmRange = (maxBpm - minBpm).coerceAtLeast(1)
    val minuteRange = (maxMinute - minMinute).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepX = w / minuteRange.toFloat()

            for (i in 0..4) {
                val y = h * (i / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.zipWithNext().forEach { (a, b) ->
                val x1 = (a.minute - minMinute) * stepX
                val x2 = (b.minute - minMinute) * stepX
                val y1 = h - ((a.bpm - minBpm).toFloat() / bpmRange.toFloat()) * h
                val y2 = h - ((b.bpm - minBpm).toFloat() / bpmRange.toFloat()) * h
                val color = zoneColorForBpm((a.bpm + b.bpm) / 2, estimatedMaxHr)
                drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 3.dp.toPx()
                )
            }

            points.forEach { p ->
                val x = (p.minute - minMinute) * stepX
                val y = h - ((p.bpm - minBpm).toFloat() / bpmRange.toFloat()) * h
                drawCircle(
                    color = zoneColorForBpm(p.bpm, estimatedMaxHr),
                    radius = 3.2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Min $minMinute", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
            Text("Peak $maxBpm bpm", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            Text("Min $maxMinute", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Z1-2", color = Color(0xFF66BB6A), style = MaterialTheme.typography.labelSmall)
            Text("Z3", color = Color(0xFFFFCA28), style = MaterialTheme.typography.labelSmall)
            Text("Z4-5", color = Color(0xFFEF5350), style = MaterialTheme.typography.labelSmall)
        }
    }
}
