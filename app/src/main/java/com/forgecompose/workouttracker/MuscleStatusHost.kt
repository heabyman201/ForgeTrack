@file:Suppress("NAME_SHADOWING", "UnusedImport")

package com.forgecompose.workouttracker

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
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

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val cold = rememberColdStartStages()
    val density = LocalDensity.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val shouldAnimate = cold.afterFirstFrame
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
                kotlinx.coroutines.delay(42)
            }
        }
    }
    val fullPi = 2f * PI.toFloat()
    val pulseAlpha = 0.25f + 0.10f * sin(animationClock * fullPi / 8f)
    val glowIntensity = 0.4f + 0.2f * sin(animationClock * fullPi / 6f)
    val gradientProgress = (animationClock / 15f) % 2f
    val gradientOffset = if (gradientProgress > 1f) 2f - gradientProgress else gradientProgress

    val particleSeed = remember { Random(42) }
    val particles = remember {
        List(12) { i ->
            val baseX = i / 12f
            val yOff = 0.15f + particleSeed.nextFloat() * 0.25f
            val r = 1.8f + particleSeed.nextFloat() * 2.0f
            Triple(baseX, yOff, r)
        }
    }

    // --- Dynamic Intro Colors based on Theme ---
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
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allWorkouts = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
    val recent = remember(uiState) {
        allWorkouts
            .sortedByDescending { it.date }
            .take(40)
            .map { w ->
                WorkoutSummary(
                    date = Instant.ofEpochMilli(w.date),
                    name = w.name,
                    exercises = listOf(w.name.lowercase())
                )
            }
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
//            .redGridBackground(
//                animationClock = if (movingEffectsEnabled && shouldAnimate) animationClock else 0f,
//                clampedPulse = clampedPulse,
//                clampedGlow = clampedGlow,
//                clampedGrad = clampedGrad,
//                density = density,
//                themeColors = theme
//            )
//            .drawWithCache {
//                val introBrush = introBrush
//                onDrawBehind {
//                    if (introProgress < 1f) {
//                        drawRect(brush = introBrush, alpha = 1f - introProgress)
//                    }
//                }
//            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Muscle Status",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
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
            AnimatedBackdrop(
                modifier = Modifier,
                introBrush = introBrush,
                introAlpha = 1f - introProgress,
                enableWaves = movingEffectsEnabled,
                enableAnimation =  movingEffectsEnabled

            )
            Box(modifier = Modifier.fillMaxSize()) {
                val haptics = LocalHapticFeedback.current
                when (uiState) {
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
                            item {
                                AnimatedVisibility(
                                    visible = cold.after100ms,
                                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it / 2 }
                                ) {
                                    GlowingCard {
                                        Column(Modifier.padding(20.dp)) {
                                            SectionTitle("Summary")
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                val weekStartEnd = remember(allWorkouts) {
                                                    if (allWorkouts.isEmpty()) null else {
                                                        val cal = Calendar.getInstance().apply {
                                                            timeInMillis = System.currentTimeMillis()
                                                            firstDayOfWeek = Calendar.MONDAY
                                                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                                                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                                        }
                                                        val start = cal.timeInMillis
                                                        val end = start + TimeUnit.DAYS.toMillis(6)
                                                        start to end
                                                    }
                                                }
                                                val thisWeekCount = weekStartEnd?.let { (start, end) ->
                                                    allWorkouts.count { it.date in start until end }
                                                } ?: 0
                                                val streak = if (allWorkouts.isEmpty()) 0 else computeStreak(System.currentTimeMillis(), allWorkouts)
                                                LabeledStat("This Week", thisWeekCount.toString())
                                                LabeledStat("Streak", "${streak}d")
                                                LabeledStat("Total", allWorkouts.size.toString())
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = cold.after200ms,
                                    enter = fadeIn(animationSpec = tween(500, 100)) + slideInVertically(animationSpec = tween(500, 100)) { it / 2 }
                                ) {
                                    GlowingCard {
                                        Column(Modifier.padding(vertical = 16.dp)) {
                                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                                val hasRoomForButton = maxWidth >= 360.dp
                                                MuscleStatusSection(
                                                    recentWorkouts = recent,
                                                    advicePayload = "",
                                                    nowEpochMillis = System.currentTimeMillis(),
                                                    modifier = Modifier.fillMaxWidth(),

                                                    onOpenWeeklySummary = {
                                                        navController.navigate("WeeklySummary")
                                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.KeyboardTap)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                }
                if (cold.afterFirstFrame) {
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