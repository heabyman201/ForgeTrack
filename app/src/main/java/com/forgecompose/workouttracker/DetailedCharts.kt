package com.forgecompose.workouttracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAnalyticsScreen(
    navController: NavController,
    exerciseName: String,
    workouts: List<Workout>
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AnalysisInsightsCard(data = filteredData)
                Spacer(modifier = Modifier.height(16.dp))
                DateRangeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateClick = { showStartDatePicker = true },
                    onEndDateClick = { showEndDatePicker = true }
                )
            }

            if (filteredData.size < 2) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough data for the selected range to show a progression.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // --- Weight Chart ---
                item {
                    val weightData = filteredData.filter { (it.weight ?: 0.0) > 0.0 }
                    if (weightData.size >= 2) {
                        val maxWeight = weightData.maxOf { it.weight!! }
                        val minWeight = weightData.minOf { it.weight!! }
                        ChartCard(title = "Weight Progression") {
                            GenericLineChart(
                                data = weightData,
                                maxValue = maxWeight,
                                minValue = minWeight,
                                valueSelector = { it.weight ?: 0.0 },
                                unit = "kg",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFFF85757), Color(0xFFD32F2F))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFF9B111E).copy(alpha = 0.4f), Color.Transparent)),
                                tooltipColor = Color(0xFF1A0808)
                            )
                        }
                    }
                }

                // --- Sets Chart ---
                item {
                    val setData = filteredData.filter { (it.sets ?: 0) > 0 }
                    if (setData.size >= 2) {
                        val maxSets = setData.maxOf { it.sets!! }.toDouble()
                        val minSets = setData.minOf { it.sets!! }.toDouble()
                        ChartCard(title = "Sets Progression") {
                            GenericLineChart(
                                data = setData,
                                maxValue = maxSets,
                                minValue = minSets,
                                valueSelector = { (it.sets ?: 0).toDouble() },
                                unit = "sets",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFF00E676), Color(0xFF1B8E4B))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFF00E676).copy(alpha = 0.35f), Color.Transparent)),
                                tooltipColor = Color(0xFF081A12)
                            )
                        }
                    }
                }

                // --- Reps Chart ---
                item {
                    val repData = filteredData.filter { (it.reps ?: 0) > 0 }
                    if (repData.size >= 2) {
                        val maxReps = repData.maxOf { it.reps!! }.toDouble()
                        val minReps = repData.minOf { it.reps!! }.toDouble()
                        ChartCard(title = "Reps Progression") {
                            GenericLineChart(
                                data = repData,
                                maxValue = maxReps,
                                minValue = minReps,
                                valueSelector = { (it.reps ?: 0).toDouble() },
                                unit = "reps",
                                lineBrush = Brush.verticalGradient(colors = listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))),
                                areaBrush = Brush.verticalGradient(colors = listOf(Color(0xFFFF3B30).copy(alpha = 0.35f), Color.Transparent)),
                                        tooltipColor = Color(0xFF1A0808)
                            )



                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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

            val changes = listOf(
                "Weight" to weightChange,
                "Reps" to repsChange
            ).filter { it.second != 0.0 }.maxByOrNull { abs(it.second) }

            changes?.let { (metric, percent) ->
                val direction = if (percent > 0) "up" else "down"
                val color = if (percent > 0) "🟢" else "🔴"
                "$color Your $metric is $direction by ${abs(percent).roundToInt()}% compared to the first half of this period."
            } ?: "✅ Your performance has been consistent. Keep up the great work!"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        Text(
            text = insightMessage,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onStartDateClick, modifier = Modifier.weight(1f)) {
            Text(formatDate(startDate))
        }
        Text("to", color = Color.White.copy(alpha = 0.7f))
        Button(onClick = onEndDateClick, modifier = Modifier.weight(1f)) {
            Text(formatDate(endDate))
        }
    }
}


@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}