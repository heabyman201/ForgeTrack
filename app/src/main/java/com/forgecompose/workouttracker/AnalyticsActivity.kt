package com.forgecompose.workouttracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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

        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME)
        if (exerciseName == null) {
            finish()
            return
        }

        val application = application as MyApplication
        val workoutRepository = application.workoutRepository
        val factory = WorkoutListViewModelFactory(workoutRepository)
        val workoutListViewModel: WorkoutListViewModel by viewModels { factory }

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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
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

    val gradientProgress1 = (animationClock / 15f) % 2f
    val gradientOffset1 = if (gradientProgress1 > 1f) 2f - gradientProgress1 else gradientProgress1

    val gradientProgress2 = (animationClock / 25f) % 2f
    val gradientOffset2 = if (gradientProgress2 > 1f) 2f - gradientProgress2 else gradientProgress2

    val clampedGlow by remember { derivedStateOf { glowIntensity.coerceIn(0f, 1f) } }
    val clampedPulse by remember { derivedStateOf { pulseAlpha.coerceIn(0f, 1f) } }
    val clampedGrad1 by remember { derivedStateOf { gradientOffset1.coerceIn(0f, 1f) } }
    val clampedGrad2 by remember { derivedStateOf { gradientOffset2.coerceIn(0f, 1f) } }

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
        Brush.linearGradient(colors = introColors, start = Offset.Zero, end = Offset(Float.POSITIVE_INFINITY, 0f))
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
                val darkPurpleSweep = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF10051F).copy(alpha = 0.6f),
                        Color(0xFF050815).copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * clampedGrad2, size.height * (1 - clampedGrad2)),
                    end = Offset(size.width * (1 - clampedGrad2), size.height * clampedGrad2)
                )

                val redGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF451010).copy(alpha = 0.85f + clampedGrad1 * 0.45f),
                        Color(0xFF280B0B).copy(alpha = 0.7f + clampedGrad1 * 0.3f),
                        Color(0xFF1F0808).copy(alpha = 0.8f + clampedGrad1 * 0.2f),
                        Color(0xFF150505).copy(alpha = 0.9f + clampedGrad1 * 0.1f),
                        Color(0xFF050101)
                    ),
                    radius = 1200f + (clampedGrad1 * 400f),
                    center = Offset(
                        size.width * (0.3f + clampedGrad1 * 0.4f),
                        size.height * (0.2f + clampedGrad1 * 0.3f)
                    )
                )

                onDrawBehind {
                    drawRect(Color(0xFF050101))
                    if (shouldAnimate && movingEffectsEnabled) {
                        drawRect(darkPurpleSweep)
                    }
                    drawRect(redGlow)

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
                            val py = h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                            val alpha = baseAlpha * (0.35f + sin(waveOffset + i) * 0.25f) * g
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp), contentAlignment = Alignment.Center
                        ) {
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
                if (weightData.size >= 2 || filteredData.size >= 2) {
                    item {
                        GlassCard {
                            CombinedWorkoutChart(
                                data = filteredData,
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
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = it
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = it
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun AnalysisInsightsCard(data: List<Workout>) {
    if (data.size < 4) {
        GlassCard {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Insights",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select a date range with at least 4 workouts for performance insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        return
    }

    val midpoint = data.size / 2
    val first = data.take(midpoint)
    val second = data.drop(midpoint)

    fun avgD(get: (Workout) -> Double?): Double {
        val vals = first.mapNotNull(get)
        val vals2 = second.mapNotNull(get)
        val a = vals.average().takeIf { !it.isNaN() } ?: 0.0
        val b = vals2.average().takeIf { !it.isNaN() } ?: 0.0
        return if (a > 0.0) ((b - a) / a) * 100.0 else 0.0
    }

    fun avgI(get: (Workout) -> Int?): Double = avgD { get(it)?.toDouble() }

    val dWeight = avgD { it.weight }
    val dReps = avgI { it.reps }
    val dSets = avgI { it.sets }
    val dVolume = run {
        val v1 = first.map { ((it.weight ?: 0.0) * (it.reps ?: 0) * (it.sets ?: 0)).toDouble() }
            .average().takeIf { !it.isNaN() } ?: 0.0
        val v2 = second.map { ((it.weight ?: 0.0) * (it.reps ?: 0) * (it.sets ?: 0)).toDouble() }
            .average().takeIf { !it.isNaN() } ?: 0.0
        if (v1 > 0.0) ((v2 - v1) / v1) * 100.0 else 0.0
    }
    val dFrequency = run {
        val firstDays = (first.lastOrNull()?.date ?: 0L) - (first.firstOrNull()?.date ?: 0L)
        val secondDays = (second.lastOrNull()?.date ?: 0L) - (second.firstOrNull()?.date ?: 0L)
        val f1 = if (firstDays > 0) first.size / (firstDays / 86_400_000.0) else 0.0
        val f2 = if (secondDays > 0) second.size / (secondDays / 86_400_000.0) else 0.0
        if (f1 > 0.0) ((f2 - f1) / f1) * 100.0 else 0.0
    }

    data class Metric(val label: String, val delta: Double, val priority: Int)
    val metrics = listOf(
        Metric("Volume", dVolume, 0),
        Metric("Weight", dWeight, 1),
        Metric("Reps", dReps, 2),
        Metric("Sets", dSets, 3),
        Metric("Frequency", dFrequency, 4)
    )

    fun fmt(p: Double): String {
        val v = abs(p)
        val r = if (v >= 10.0) v.roundToInt().toString() else String.format(Locale.US, "%.1f", v)
        return "$r%"
    }

    fun arrow(p: Double): String = when {
        p > 0.5 -> "↑"
        p < -0.5 -> "↓"
        else -> "↔"
    }

    val headline = run {
        val lead = metrics.maxWithOrNull(compareBy<Metric> { abs(it.delta) }.thenBy { -it.priority })
        if (lead == null || abs(lead.delta) < 0.5) "Performance stable across this period."
        else "${lead.label} ${arrow(lead.delta)} ${fmt(lead.delta)}"
    }

    val onBg = MaterialTheme.colorScheme.onSurface
    val pos = Color(0xFF1DB954)
    val neg = Color(0xFFFF4D4D)
    val neu = onBg.copy(alpha = 0.6f)

    @Composable
    fun MetricRow(m: Metric) {
        val base = when {
            m.delta > 0.5 -> pos
            m.delta < -0.5 -> neg
            else -> neu
        }
        val fg by animateColorAsState(base, label = "fg")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(m.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = arrow(m.delta),
                    style = MaterialTheme.typography.bodyLarge,
                    color = fg,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = fmt(m.delta),
                    style = MaterialTheme.typography.bodyLarge,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    GlassCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Insights",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                metrics.forEach { metric ->
                    MetricRow(m = metric)
                }
            }
        }
    }
}

@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Select Date"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoChip(
            label = formatDate(startDate),
            icon = Icons.Filled.DateRange,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onStartDateClick)
        )
        Text("to", color = Color.White.copy(alpha = 0.7f))
        InfoChip(
            label = formatDate(endDate),
            icon = Icons.Filled.DateRange,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEndDateClick)
        )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}