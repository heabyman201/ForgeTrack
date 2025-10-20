package com.forgecompose.workouttracker

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Assume your existing ViewModel, Factory, and Application setup
// import com.yourpackage.MyApplication
// import com.yourpackage.WorkoutListViewModel
// import com.yourpackage.WorkoutListViewModelFactory
// import com.yourpackage.data.Workout

class ExerciseAnalyticsActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_EXERCISE_NAME = "EXTRA_EXERCISE_NAME"

        fun newIntent(context: Context, exerciseName: String): Intent {
            return Intent(context, ExerciseAnalyticsActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The exercise name must be passed in the Intent
        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME)
        if (exerciseName == null) {
            // Close the activity if the name is missing
            finish()
            return
        }

        // --- ViewModel Initialization ---
        val application = application as MyApplication
        val workoutRepository = application.workoutRepository
        val factory = WorkoutListViewModelFactory(workoutRepository)
        val workoutListViewModel: WorkoutListViewModel by viewModels { factory }

        // -----------------------------

        setContent {

            WorkoutTrackerTheme {
                val workoutsUiState by workoutListViewModel.uiState.collectAsStateWithLifecycle()


                when (val state = workoutsUiState) {
                    is WorkoutListUiState.Success -> {

                        ExerciseAnalyticsScreen(
                            exerciseName = exerciseName,
                            workouts = state.workouts,
                            onBackClicked = { finish() }
                        )
                    }
                    is WorkoutListUiState.Loading -> {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is WorkoutListUiState.Error -> {

                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            }
        }
    }



// --- Main Screen Composable (Modified for Activity) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAnalyticsScreen(
    exerciseName: String,
    workouts: List<Workout>,
    onBackClicked: () -> Unit
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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
    LaunchedEffect(Unit) { showIntro = false }

    val filteredData = remember(workouts, exerciseName, startDate, endDate) {
        workouts
            .filter { it.name == exerciseName }
            .filter { w ->
                val afterStartDate = startDate?.let { w.date >= it } ?: true
                val beforeEndDate = endDate?.let { w.date <= it } ?: true
                afterStartDate && beforeEndDate
            }
            .sortedBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        modifier = Modifier
            .fillMaxSize()
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
                    if (stages.after600ms && shouldAnimate && movingEffectsEnabled) {
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnalysisInsightsCard(data = filteredData)
            }
            item {
                DateRangeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateClick = { showStartDatePicker = true },
                    onEndDateClick = { showEndDatePicker = true }
                )
            }

            if (filteredData.size < 2) {
                item {
                    GlassCard {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Not enough data for the selected range.",
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                val weightData = filteredData.filter { (it.weight ?: 0.0) > 0.0 }
                if (weightData.size >= 2) {
                    item {
                        GlassCard(modifier = Modifier.padding(bottom = 2.dp)) {
                            SectionTitle("Weight Progression")
                            val maxWeight = weightData.maxOf { it.weight!! }
                            val minWeight = weightData.minOf { it.weight!! }
                            GenericLineChart(
                                data = weightData, maxValue = maxWeight, minValue = minWeight,
                                valueSelector = { it.weight ?: 0.0 }, unit = "kg",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFFF85757), Color(0xFFD32F2F))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFF9B111E).copy(alpha = 0.4f), Color.Transparent)),
                                tooltipColor = Color(0xFF1A0808)
                            )
                        }
                    }
                }
                val setData = filteredData.filter { (it.sets ?: 0) > 0 }
                if (setData.size >= 2) {
                    item {
                        GlassCard(modifier = Modifier.padding(bottom = 2.dp)) {
                            SectionTitle("Sets Progression")
                            val maxSets = setData.maxOf { it.sets!! }.toDouble()
                            val minSets = setData.minOf { it.sets!! }.toDouble()
                            GenericLineChart(
                                data = setData, maxValue = maxSets, minValue = minSets,
                                valueSelector = { (it.sets ?: 0).toDouble() }, unit = "sets",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFF00E676), Color(0xFF1B8E4B))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFF00E676).copy(alpha = 0.35f), Color.Transparent)),
                                tooltipColor = Color(0xFF081A12)
                            )
                        }
                    }
                }
                val repData = filteredData.filter { (it.reps ?: 0) > 0 }
                if (repData.size >= 2) {
                    item {
                        GlassCard(modifier = Modifier.padding(bottom = 2.dp)) {
                            SectionTitle("Reps Progression")
                            val maxReps = repData.maxOf { it.reps!! }.toDouble()
                            val minReps = repData.minOf { it.reps!! }.toDouble()
                            GenericLineChart(
                                data = repData, maxValue = maxReps, minValue = minReps,
                                valueSelector = { (it.reps ?: 0).toDouble() }, unit = "reps",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFF01579B).copy(alpha = 0.4f), Color.Transparent)),
                                tooltipColor = Color(0xFF011A27)
                            )

                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = { TextButton(onClick = { startDate = datePickerState.selectedDateMillis; showStartDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = { TextButton(onClick = { endDate = datePickerState.selectedDateMillis; showEndDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}





@Composable
private fun AnalysisInsightsCard(data: List<Workout>) {
    val insightMessage = remember(data) {
        if (data.size < 4) {
            "Select a date range with at least 4 workouts for performance insights."
        } else {
            val midpoint = data.size / 2
            val firstHalf = data.take(midpoint)
            val secondHalf = data.drop(midpoint)
            val avgWeightFirst = firstHalf.mapNotNull { it.weight }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgWeightSecond = secondHalf.mapNotNull { it.weight }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgRepsFirst = firstHalf.mapNotNull { it.reps }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgRepsSecond = secondHalf.mapNotNull { it.reps }.average().takeIf { !it.isNaN() } ?: 0.0
            val weightChange = if (avgWeightFirst > 0) ((avgWeightSecond - avgWeightFirst) / avgWeightFirst) * 100 else 0.0
            val repsChange = if (avgRepsFirst > 0) ((avgRepsSecond - avgRepsFirst) / avgRepsFirst) * 100 else 0.0
            val changes = listOf("Weight" to weightChange, "Reps" to repsChange).filter { it.second != 0.0 }.maxByOrNull { abs(it.second) }
            changes?.let { (metric, percent) ->
                val direction = if (percent > 0) "up" else "down"
                val color = if (percent > 0) "🟢" else "🔴"
                "$color Your $metric is $direction by ${abs(percent).roundToInt()}% compared to the first half of this period."
            } ?: "✅ Your performance has been consistent. Keep up the great work!"
        }
    }
    GlassCard {
        Text(text = insightMessage, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun DateRangeSelector(startDate: Long?, endDate: Long?, onStartDateClick: () -> Unit, onEndDateClick: () -> Unit) {
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Select Date"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        InfoChip(label = formatDate(startDate), icon = Icons.Filled.DateRange, modifier = Modifier.weight(1f).clickable(onClick = onStartDateClick))
        Text("to", color = Color.White.copy(alpha = 0.7f))
        InfoChip(label = formatDate(endDate), icon = Icons.Filled.DateRange, modifier = Modifier.weight(1f).clickable(onClick = onEndDateClick))
    }
}

@Composable
private fun InfoChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}



