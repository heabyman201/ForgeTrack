package com.forgecompose.workouttracker

import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.length
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context)
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
    val waveOffset = (animationClock * fullPi / 22f) % fullPi
    val pulseAlpha = 0.25f + 0.10f * sin(animationClock * fullPi / 8f)
    val glowIntensity = 0.4f + 0.2f * sin(animationClock * fullPi / 6f)
    val gradientProgress = (animationClock / 15f) % 2f
    val gradientOffset = if (gradientProgress > 1f) 2f - gradientProgress else gradientProgress

    val clampedGlow by remember { derivedStateOf { glowIntensity.coerceIn(0f, 1f) } }
    val clampedPulse by remember { derivedStateOf { pulseAlpha.coerceIn(0f, 1f) } }
    val clampedGrad by remember { derivedStateOf { gradientOffset.coerceIn(0f, 1f) } }

    val wavePath = remember { Path() }
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

    LaunchedEffect(Unit) { showIntro = false
        Firebase.crashlytics.setCustomKey("current_screen", "Workout Details screen")}


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedId: Int? = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get<Int>("selectedWorkoutId")

    val allWorkouts = remember(uiState) { (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty() }
    val selectedWorkout by remember(selectedId, allWorkouts) {
        derivedStateOf { allWorkouts.firstOrNull { it.id == selectedId } }
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

    val chartSeriesSig by remember(allWorkouts) {
        derivedStateOf {
            allWorkouts.fold(1L) { acc, w ->
                acc * 31 + w.id + w.date + (w.durationMillis ?: 0L) + (w.weight?.toLong() ?: 0L)
            }
        }
    }
    val cachedAllWorkouts = remember(chartSeriesSig) { allWorkouts.toList() }

    LaunchedEffect(Unit) { taskbarOverride.shouldOverrideVisiblity.value = false;
        Log.d("WorkoutDetailScreen", "cold: ${selectedWorkout?.name}")}
    val cardioExerciseNames = listOf(
        "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
        "Rowing Machine", "Stationary Bike","Swimming"
    )
    val presetByName = workoutPresets.associateBy { it.name.trim().lowercase() }

    fun isCardioName(name: String?): Boolean {
        val p = presetByName[name?.trim()?.lowercase()] ?: return false
        return p.category.equals("Cardio", ignoreCase = true)
    }
    val isCardio = isCardioName(selectedWorkout?.name)
    val blurLength = length.value.toInt()
    val blurEnabled = performanceOptions.blurEnabled
    val blurIntro by animateDpAsState(
        if (showIntro && blurEnabled) 32.dp else 0.dp,
        animationSpec = tween(durationMillis = blurLength, easing = LinearEasing),
        label = "blurIntro"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedWorkout?.name ?: "Workout Summary",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
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
                                context = context,
                                exerciseName = workout.name
                            )
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "View Analytics"
                            )
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
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()


    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()
            .graphicsLayer{
                renderEffect = RenderEffect.createBlurEffect(blurIntro.value,blurIntro.value,Shader.TileMode.DECAL)
                    .asComposeRenderEffect()
            }

        ) {
            AnimatedBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                introBrush = introBrush,
                introAlpha = 1f- introProgress,
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
                                                Column(
                                                    Modifier.padding(cardPad),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = selectedWorkout!!.name,
                                                        style = titleStyle,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                    StatusChip(selectedWorkout!!.status)
                                                    InfoChip(
                                                        label = dateFormat.format(
                                                            Date(
                                                                selectedWorkout!!.date
                                                            )
                                                        ),
                                                        icon = Icons.Filled.DateRange
                                                    )
                                                    if (selectedWorkout!!.name in cardioExerciseNames) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            InfoChip(
                                                                label = selectedWorkout!!.distance?.let { "$it km" }
                                                                    ?: "No Distance Recorded",
                                                                icon = Icons.Filled.FitnessCenter
                                                            )
                                                        }

                                                    } else {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            InfoChip(
                                                                label = selectedWorkout!!.sets?.let { "$it Sets" }
                                                                    ?: "No Sets Recorded",
                                                                icon = Icons.Filled.FitnessCenter
                                                            )
                                                            InfoChip(
                                                                label = selectedWorkout!!.reps?.let { "$it Reps" }
                                                                    ?: "No Reps Recorded",
                                                                icon = Icons.Filled.FitnessCenter
                                                            )
                                                            InfoChip(
                                                                label = selectedWorkout!!.weight?.let { "$it Kg" }
                                                                    ?: "No Weight Recorded",
                                                                icon = Icons.Filled.FitnessCenter
                                                            )
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
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceAround
                                                    ) {
                                                        LabeledStat("Duration", dur)
                                                        LabeledStat("Start", start)
                                                        LabeledStat("End", end)
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
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceAround
                                                    ) {
                                                        LabeledStat("This Week", thisWeekCount.toString())
                                                        LabeledStat("Streak", "${streak}d")
                                                        LabeledStat(
                                                            "Last Time",
                                                            previousSame?.let { fmtAgo(it.date, selectedWorkout!!.date) } ?: "--"
                                                        )
                                                    }
                                                }
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
                                    if (cachedAllWorkouts.isNotEmpty()) {
                                        item {
                                            AnimatedVisibility(visible = stages.after700ms, enter = fadeIn()) {
                                                GlassCard {
                                                    Column(Modifier.padding(cardPad)) {
                                                        SectionTitle("Progression")
                                                        key(selectedWorkout!!.name, chartSeriesSig) {
                                                            ExerciseWeightProgressionGraph(
                                                                exerciseName = selectedWorkout!!.name,
                                                                workouts = cachedAllWorkouts
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        item {
                                            AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                                GlassCard {
                                                    Column(Modifier.padding(cardPad)) {
                                                        SectionTitle("Sets Progression")
                                                        key(selectedWorkout!!.name, chartSeriesSig) {
                                                            ExerciseSetProgressionGraph(
                                                                exerciseName = selectedWorkout!!.name,
                                                                workouts = cachedAllWorkouts
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        item {
                                            AnimatedVisibility(visible = stages.after800ms, enter = fadeIn()) {
                                                GlassCard {
                                                    Column(Modifier.padding(cardPad)) {
                                                        SectionTitle("Reps Progression")
                                                        key(selectedWorkout!!.name, chartSeriesSig) {
                                                            ExerciseRepProgressionGraph(
                                                                exerciseName = selectedWorkout!!.name,
                                                                workouts = cachedAllWorkouts
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (sameNameWorkouts.size > 1) {
                                        item {
                                            AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                                GlassCard {
                                                    Column(Modifier.padding(cardPad)) {
                                                        SectionTitle("Recent Sessions")
                                                        val recentSessions = remember(sameNameWorkouts) {
                                                            sameNameWorkouts.takeLast(5).asReversed()
                                                        }
                                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            recentSessions.forEach { w ->
                                                                SessionHistoryRow(w)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        val haptics = LocalHapticFeedback.current
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF4A0000).copy(alpha = 0.2f))
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.deleteWorkout(selectedWorkout!!)
                                                    navController.navigateUp()
                                                }
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Delete Workout",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val listState = rememberLazyListState()
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = contentHPad),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                item {
                                    AnimatedVisibility(visible = stages.after200ms, enter = fadeIn()) {
                                        GlassCard {
                                            Column(
                                                Modifier.padding(cardPad),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = selectedWorkout!!.name,
                                                    style = titleStyle,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                                StatusChip(selectedWorkout!!.status)
                                                InfoChip(
                                                    label = dateFormat.format(Date(selectedWorkout!!.date)),
                                                    icon = Icons.Filled.DateRange
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    InfoChip(
                                                        label = selectedWorkout!!.sets?.let { "$it Sets" } ?: "No Sets",
                                                        icon = Icons.Filled.FitnessCenter
                                                    )
                                                    InfoChip(
                                                        label = selectedWorkout!!.reps?.let { "$it Reps" } ?: "No Reps",
                                                        icon = Icons.Filled.FitnessCenter
                                                    )
                                                }
                                                InfoChip(
                                                    label = selectedWorkout!!.weight?.let { "$it kg" } ?: "Bodyweight",
                                                    icon = Icons.Filled.FitnessCenter
                                                )
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
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    LabeledStat("Duration", dur)
                                                    LabeledStat("Start", start)
                                                    LabeledStat("End", end)
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
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    LabeledStat("This Week", thisWeekCount.toString())
                                                    LabeledStat("Streak", "${streak}d")
                                                    LabeledStat(
                                                        "Last Time",
                                                        previousSame?.let { fmtAgo(it.date, selectedWorkout!!.date) } ?: "--"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (sameNameWorkouts.size > 1) {
                                    item {
                                        AnimatedVisibility(visible = stages.after400ms, enter = fadeIn()) {
                                            GlassCard {
                                                Column(Modifier.padding(cardPad)) {
                                                    SectionTitle("Recent Sessions")
                                                    val recentSessions = remember(sameNameWorkouts) {
                                                        sameNameWorkouts.takeLast(5).asReversed()
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        recentSessions.forEach { w ->
                                                            SessionHistoryRow(w)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    val haptics = LocalHapticFeedback.current
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF4A0000).copy(alpha = 0.2f))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.deleteWorkout(selectedWorkout!!)
                                                navController.navigateUp()
                                            }
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Delete Workout",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (stages.after100ms) {
                FloatingTaskbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    navController = navController,
                    cornerRadius = 34.dp,
                    iconAlpha = 1f,
                    uiState = uiState
                )
            }
        }
    }
}



@Composable
fun SessionHistoryRow(w: Workout) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(dateFormat.format(Date(w.date)), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(
            formatDuration(w.durationMillis),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
}

private fun formatDuration(durationMillis: Long?): String {
    val d = durationMillis ?: 0L
    val h = TimeUnit.MILLISECONDS.toHours(d).toInt()
    val m = TimeUnit.MILLISECONDS.toMinutes(d).toInt() % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(d).toInt() % 60
    return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
}

private fun fmtAgo(previous: Long, current: Long): String {
    val diff = current - previous
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    return when {
        days > 0 -> "${days}d ${hours}h"
        else -> "${hours}h"
    }
}

 fun computeStreak(anchorDate: Long, workouts: List<Workout>): Int {
    if (workouts.isEmpty()) return 0
    val dayMillis = 86_400_000L


    fun dayStart(t: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = t }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val daysSet = workouts.map { dayStart(it.date) }.toHashSet()
    var streak = 0
    var cur = dayStart(anchorDate)
    while (daysSet.contains(cur)) {
        streak++
        cur -= dayMillis
    }
    return streak
}
