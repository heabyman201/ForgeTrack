@file:Suppress("NAME_SHADOWING", "UnusedImport")

package com.forgecompose.workouttracker.muscle

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ui.components.blurAnim.intensity
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.time.Instant

import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMuscleStatusRoute(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val context = LocalContext.current

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    val cold = rememberColdStartStages()
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val shouldAnimate = cold.afterFirstFrame

    // Use a primitive state to avoid overhead
    var animationClock by remember { mutableFloatStateOf(0f) }

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
                kotlinx.coroutines.delay(42)
            }
        }
    }

    // Wrap intro colors and brush to prevent re-allocation on every recomposition
    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "introFade"
    )


    LaunchedEffect(Unit) {
        Firebase.crashlytics.setCustomKey("current_screen", "MuscleStatus")
        showIntro = false
        taskbarOverride.shouldOverrideVisiblity.value = false
        //Testing Purposes Only
//        val recommendation = engine.recommend(
//            sleepHours = 7.5f,
//            daysSinceLast = 2f,
//            weeklyVolume = floatArrayOf(8f, 10f, 6f, 4f, 4f, 10f, 6f, 6f),
//            trainingWeek = 9f,
//            avgSleep7d = 7.2f,
//        )
//
//        println(recommendation)
//        WorkoutRecommendation(
//        intensityTier="hard",
//        intensityConfidence=0.87f,
//        exerciseCategory="push",
//        categoryConfidence=0.72f
//    )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    val recent = remember(uiState) {
        (uiState as? WorkoutListUiState.Success)?.workouts
            ?.sortedByDescending { it.date }
            ?.take(40)
            ?.map { w ->
                val summaryToken = buildString {
                    append(w.name.lowercase())
                    w.sets?.takeIf { it > 0 }?.let { append(" $it sets") }
                    w.reps?.takeIf { it > 0 }?.let { append(" $it reps") }
                    (w.sessionRpe ?: w.rpe)?.takeIf { it > 0 }?.let { append(" rpe $it") }
                    if ((w.weight ?: 0.0) > 0.0) append(" heavy")
                }
                WorkoutSummary(
                    date = Instant.ofEpochMilli(w.date),
                    name = w.name,
                    exercises = listOf(summaryToken),
                    environment = w.trainingEnvironment,
                    sessionRpe = w.sessionRpe ?: w.rpe,
                    fatigueLevel = w.fatigueLevel,
                    restPeriodSeconds = w.restPeriodSeconds,
                    durationMinutes = w.durationMillis?.takeIf { it > 0 }?.let { it / 60000f },
                    sets = w.sets,
                    reps = w.reps,
                    weight = w.weight,
                    distance = w.distance,
                    heartRateAvg = w.heartRateAvg,
                    heartRateMax = w.heartRateMax,
                    systemicDrainScore = w.systemicDrainScore
                )
            }.orEmpty()
    }

    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(blurAnim)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {

                        Text(
                            text = "Muscle Status",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        theme.primary,
                                        theme.primary.copy(alpha = 1f),
                                        Color.White
                                    )
                                ),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            // CALCULATE DERIVED VALUES HERE ONLY IF NEEDED BY CHILD
            // Ideally, pass animationClock to AnimatedBackdrop and let IT handle the sin() math
            AnimatedBackdrop(
                modifier = Modifier,
                introBrush = introBrush,
                introAlpha = 1f - introProgress,
                enableWaves = movingEffectsEnabled,
                enableAnimation = movingEffectsEnabled
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val haptics = LocalHapticFeedback.current

                when (val state = uiState) {
                    is WorkoutListUiState.Loading -> LoadingBlock(padding)
                    is WorkoutListUiState.Error -> ErrorBlock(padding)
                    is WorkoutListUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)
                        ) {
                            item(key = "summary") {
                                SummarySection(allWorkouts = state.workouts, visible = cold.after100ms)
                            }
                            item(key = "ai_coach_cta") {
                                CoachCtaSection(
                                    visible = cold.after100ms,
                                    theme = theme,
                                    onOpen = {
                                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                        navController.navigate("Coaching")
                                    }
                                )
                            }
                            item(key = "muscle_status") {
                                MuscleStatusSectionWrapper(
                                    recent = recent,
                                    visible = cold.after200ms,
                                    navController = navController,
                                    haptics = haptics
                                )
                            }
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    theme.background.copy(alpha = 0.82f),
                                    Color.Transparent
                                )
                            )
                        )
                )

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
}

@Composable
private fun SummarySection(allWorkouts: List<Workout>, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
    ) {
        GlowingCard {
            Column(Modifier.padding(20.dp)) {
                SectionTitle("Summary")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val stats = remember(allWorkouts) {
                        val cal = Calendar.getInstance().apply {
                            firstDayOfWeek = Calendar.MONDAY
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val start = cal.timeInMillis
                        val end = start + TimeUnit.DAYS.toMillis(6)

                        val thisWeekCount = allWorkouts.count { it.date in start until end }
                        val streak = if (allWorkouts.isEmpty()) 0 else computeStreak(System.currentTimeMillis(), allWorkouts)
                        Triple(thisWeekCount, streak, allWorkouts.size)
                    }

                    LabeledStat("This Week", stats.first.toString())
                    LabeledStat("Streak", "${stats.second}d")
                    LabeledStat("Total", stats.third.toString())
                }
            }
        }
    }
}

@Composable
private fun CoachCtaSection(
    visible: Boolean,
    theme: ColorSchemeAppTheme,
    onOpen: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, 50)) + slideInVertically(tween(500, 50)) { it / 2 }
    ) {
        GlowingCard(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).clickable { onOpen() }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(theme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = theme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI Coach",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Turn these signals into a custom weekly plan",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = theme.primary)
            }
        }
    }
}

@Composable
private fun MuscleStatusSectionWrapper(
    recent: List<WorkoutSummary>,
    visible: Boolean,
    navController: NavController,
    haptics: HapticFeedback
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { it / 2 }
    ) {
        GlowingCard {
            Column(Modifier.padding(vertical = 16.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    MuscleStatusSection(
                        recentWorkouts = recent,
                        advicePayload = "",
                        nowEpochMillis = System.currentTimeMillis(),
                        modifier = Modifier.fillMaxWidth(),
                        onOpenWeeklySummary = {
                            navController.navigate("WeeklySummary")
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        }
                    )
                }
            }
        }
    }
}

fun Modifier.redGridBackground(
    animationClock: Float,
    clampedPulse: Float,
    clampedGlow: Float,
    clampedGrad: Float,
    density: androidx.compose.ui.unit.Density,
    themeColors: ColorSchemeAppTheme
): Modifier = this.then(
    Modifier.drawWithCache {
        val minSpacingPx = with(density) { 36.dp.toPx() }
        val majorEvery = 4
        val thin = with(density) { 0.75.dp.toPx() }
        val thick = with(density) { 1.5.dp.toPx() }
        val fullPi = (2f * PI).toFloat()
        val driftPx = if (animationClock == 0f) 0f else 16f * sin(animationClock * fullPi / 18f)
        val driftDiag = driftPx * 0.7f

        val baseLinear = Brush.linearGradient(
            colors = listOf(
                themeColors.background,
                themeColors.tertiary,
                themeColors.secondary.copy(alpha = 0.2f),
                themeColors.background
            ),
            start = Offset(0f, size.height * (0.15f + 0.15f * clampedGrad)),
            end = Offset(size.width, size.height * (0.85f - 0.15f * clampedGrad))
        )
        val radialCore = Brush.radialGradient(
            colors = listOf(
                themeColors.primary.copy(alpha = 0.35f + 0.25f * clampedGlow),
                Color.Transparent
            ),
            center = Offset(
                size.width * (0.28f + 0.44f * clampedGrad),
                size.height * (0.18f + 0.30f * clampedGrad)
            ),
            radius = max(size.width, size.height) * (0.55f + 0.25f * clampedGrad)
        )
        val radialHalo = Brush.radialGradient(
            colors = listOf(
                themeColors.primary.copy(alpha = 0.10f + 0.12f * clampedPulse),
                Color.Transparent
            ),
            center = Offset(
                size.width * (0.50f - 0.20f * clampedGrad),
                size.height * (0.65f - 0.20f * clampedGrad)
            ),
            radius = max(size.width, size.height) * (0.75f + 0.15f * clampedGlow)
        )
        val sweepGlow = Brush.sweepGradient(
            0f to Color.Transparent,
            0.25f to themeColors.secondary.copy(alpha = 0.06f + 0.08f * clampedPulse),
            0.5f to Color.Transparent,
            0.75f to themeColors.primary.copy(alpha = 0.04f + 0.06f * clampedGlow),
            1f to Color.Transparent,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        val vignette = Brush.radialGradient(
            colors = listOf(
                themeColors.tertiary.copy(alpha = 0.95f),
                themeColors.background
            ),
            center = Offset(
                size.width * (0.30f + 0.40f * clampedGrad),
                size.height * (0.22f + 0.28f * clampedGrad)
            ),
            radius = max(size.width, size.height) * (0.9f + 0.15f * clampedGrad)
        )

        val gridMinor = themeColors.secondary.copy(alpha = 0.3f)
        val gridMajor = themeColors.secondary.copy(alpha = 0.5f)
        val minorAlpha = (0.10f + 0.08f * clampedPulse).coerceIn(0.06f, 0.20f)
        val majorAlpha = (0.16f + 0.16f * clampedGlow).coerceIn(0.12f, 0.32f)
        val pathMinor = Path()
        val pathMajor = Path()
        run {
            pathMinor.reset()
            pathMajor.reset()
            val cols = max(1, (size.width / minSpacingPx).toInt() + 2)
            val rows = max(1, (size.height / minSpacingPx).toInt() + 2)
            val x0 = -minSpacingPx * 2
            val y0 = -minSpacingPx * 2
            for (i in 0..cols) {
                val x = x0 + i * minSpacingPx
                val target = if (i % majorEvery == 0) pathMajor else pathMinor
                target.moveTo(x, y0)
                target.lineTo(x, size.height + minSpacingPx * 2)
            }
            for (j in 0..rows) {
                val y = y0 + j * minSpacingPx
                val target = if (j % majorEvery == 0) pathMajor else pathMinor
                target.moveTo(x0, y)
                target.lineTo(size.width + minSpacingPx * 2, y)
            }
        }
        val strokeMinor = Stroke(width = thin)
        val strokeMajor = Stroke(width = thick)
        val sheen = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                themeColors.primary.copy(alpha = 0.02f),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        onDrawWithContent {
            drawRect(brush = baseLinear)
            drawRect(brush = radialCore)
            drawRect(brush = radialHalo)
            drawRect(brush = sweepGlow)
            drawRect(brush = vignette)
            withTransform({ translate(driftDiag, driftDiag) }) {
                drawPath(path = pathMinor, color = gridMinor.copy(alpha = minorAlpha), style = strokeMinor)
                drawPath(path = pathMajor, color = gridMajor.copy(alpha = majorAlpha), style = strokeMajor)
            }
            drawRect(brush = sheen)
            drawContent()
        }
    }
)
