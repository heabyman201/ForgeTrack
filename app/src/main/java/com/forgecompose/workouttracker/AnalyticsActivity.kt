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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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

    val movingEnabled = performanceOptions.movingGradientAndParticles
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
        modifier = Modifier
            .fillMaxSize()

    ) { paddingValues ->
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves = movingEnabled,
            enableAnimation = movingEnabled

        )
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
 fun AnalysisInsightsCard(data: List<Workout>) {
    if (data.size < 3) {
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
                    "Complete at least 3 workouts to unlock performance trends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        return
    }

    val sorted = data.sortedBy { it.date }
    val validLifts = sorted.filter { (it.weight ?: 0.0) > 0 && (it.reps ?: 0) > 0 }

    if (validLifts.isEmpty()) return

    val e1rms = validLifts.map {
        val w = it.weight!!
        val r = it.reps!!
        w * (1 + r / 30.0)
    }
    val volumes = validLifts.map { (it.weight!! * it.reps!! * (it.sets ?: 1)).toDouble() }

    val sampleSize = (validLifts.size * 0.3).coerceAtLeast(1.0).toInt()
    val startE1RM = e1rms.take(sampleSize).average()
    val endE1RM = e1rms.takeLast(sampleSize).average()
    val strengthChange = if (startE1RM > 0) ((endE1RM - startE1RM) / startE1RM) * 100 else 0.0

    val startVol = volumes.take(sampleSize).average()
    val endVol = volumes.takeLast(sampleSize).average()
    val volumeChange = if (startVol > 0) ((endVol - startVol) / startVol) * 100 else 0.0

    val dates = sorted.map { it.date }
    val diffs = dates.zipWithNext { a, b -> (b - a) / (1000.0 * 60 * 60 * 24) }
    val avgGap = if (diffs.isNotEmpty()) diffs.average() else 0.0

    val (headline, subtext, sentimentColor) = when {
        strengthChange > 5.0 -> Triple("Peaking", "Estimated 1RM is trending up by ${strengthChange.roundToInt()}%.", Color(0xFF1DB954))
        strengthChange < -5.0 -> Triple("Cooling Down", "Estimated 1RM is down ${abs(strengthChange.roundToInt())}%. Deload active?", Color(0xFFFFB74D))
        volumeChange > 10.0 -> Triple("Building Volume", "Strength is stable, but work capacity is up ${volumeChange.roundToInt()}%.", Color(0xFF1DB954))
        else -> Triple("Maintenance", "Performance is stable. Consistency is key here.", Color(0xFF29B6F6))
    }

    val summary = buildString {
        if (strengthChange > 2) append("You're moving more weight than when you started. ")
        else if (strengthChange < -2) append("Intensity has dropped slightly. ")

        if (volumeChange > 5) append("Work capacity is increasing. ")
        else if (volumeChange < -5) append("Volume load is decreasing. ")

        if (avgGap > 0) {
            if (avgGap < 3) append("High frequency consistency!")
            else if (avgGap > 7) append("Try to train more frequently.")
            else append("Consistent training rhythm.")
        }
    }.trim().ifEmpty { "Keep logging to reveal more patterns." }

    GlassCard {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (strengthChange >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = sentimentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCompact("Est. 1RM", "${endE1RM.roundToInt()}kg", if (strengthChange > 0) "+" else "")
                StatCompact("Avg Volume", "${(endVol / 1000).toString().take(3)}t", "")
                StatCompact("Frequency", "${String.format(Locale.US, "%.1f", avgGap)}d", "")
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatCompact(label: String, value: String, prefix: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            prefix + value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
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