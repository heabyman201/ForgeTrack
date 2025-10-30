package com.forgecompose.workouttracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMuscleStatusRoute(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val cold = rememberColdStartStages()
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)

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

    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
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
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF702727).copy(alpha = 0.85f + clampedGrad * 0.45f),
                        Color(0xFF3A1515).copy(alpha = 0.7f + clampedGrad * 0.3f),
                        Color(0xFF2A0D0D).copy(alpha = 0.8f + clampedGrad * 0.2f),
                        Color(0xFF1A0808).copy(alpha = 0.9f + clampedGrad * 0.1f),
                        Color(0xFF0D0404)
                    ),
                    radius = 1200f + (clampedGrad * 400f),
                    center = Offset(0.3f + clampedGrad * 0.4f, 0.2f + clampedGrad * 0.3f)
                )
                onDrawBehind {
                    drawRect(bgBrush)
                    if (cold.after600ms && shouldAnimate && movingEffectsEnabled) {
                        val baseAlpha = clampedPulse
                        val g = clampedGlow
                        val w = size.width
                        val h = size.height
                        for (layer in 0..2) {
                            val layerOffset = waveOffset + (layer * PI.toFloat() / 4)
                            val layerAlpha = baseAlpha * (0.25f + layer * 0.12f) * g
                            val layerColor = when (layer) {
                                0 -> Color(0xFF4A1A1A).copy(alpha = layerAlpha)
                                1 -> Color(0xFF3A1515).copy(alpha = layerAlpha * 0.8f)
                                else -> Color(0xFF2A0D0D).copy(alpha = layerAlpha * 0.6f)
                            }
                            wavePath.reset()
                            val baseY = h * (0.22f + layer * 0.16f)
                            val step = (w / 36f).coerceAtLeast(10f)
                            var x = 0f
                            val waveHeight = 90f
                            while (x <= w) {
                                val t = x / w
                                val phase = t * 3f * PI.toFloat() + layerOffset
                                val y =
                                    baseY + sin(phase) * waveHeight * (0.55f + layer * 0.22f) * g
                                wavePath.lineTo(x, y)
                                x += step
                            }
                            wavePath.lineTo(w, h)
                            wavePath.lineTo(0f, h)
                            wavePath.close()
                            drawPath(path = wavePath, color = layerColor)
                        }
                        particles.forEachIndexed { i, (baseX, yOff, r) ->
                            val px = w * baseX + sin(waveOffset * 0.7f + i) * 60f * g
                            val py =
                                h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                            val alpha =
                                baseAlpha * (0.35f + sin(waveOffset + i) * 0.25f) * g
                            drawCircle(Color.White.copy(alpha = alpha), r, Offset(px, py))
                        }
                    }
                    if (introProgress < 1f) {
                        drawRect(brush = introBrush, alpha = 1f - introProgress)
                    }
                }
            }
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
                                                    weeklySummaryAvailable = hasRoomForButton,
                                                    onOpenWeeklySummary = { navController.navigate("WeeklySummary")
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        cornerRadius = 34.dp,
                        iconAlpha = 1f,
                        uiState = uiState
                    )
                }
            }
        }
    }
}