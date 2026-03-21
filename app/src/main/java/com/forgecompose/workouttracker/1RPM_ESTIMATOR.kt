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

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.abs
import kotlin.math.pow

enum class OneRmFormula(val label: String) {
    EPLEY("Epley"),
    BRZYCKI("Brzycki"),
    LOMBARDI("Lombardi"),
    OCONNER("O'Conner")
}

private fun estimate1Rm(weight: Double, reps: Int, formula: OneRmFormula): Double {
    if (weight <= 0.0) return 0.0
    val r = reps.coerceAtLeast(1)
    return when (formula) {
        OneRmFormula.EPLEY    -> weight * (1.0 + r / 30.0)
        OneRmFormula.BRZYCKI  -> weight * (36.0 / (37.0 - r))
        OneRmFormula.LOMBARDI -> weight * r.toDouble().pow(0.10)
        OneRmFormula.OCONNER  -> weight * (1.0 + 0.025 * r)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRepMaxEstimator(
    personalRecords: List<PersonalRecord>,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val baseDark = Color(0xFF1A1A1A).copy(alpha = 0.25f)
    val accentGlow = Color(0xFFFF3535)

    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF0A0404),
                Color(0xFF2A0F0F),
                Color(0xFF3D0000),
                Color(0xFF4A0000),
                Color(0xFF060202)
            ),
            radius = 1000f,
            center = Offset(0.5f, 0.4f)
        )
    }
    val secondaryStaticBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0000).copy(alpha = 0.2f),
                Color.Transparent,
                Color(0xFF2A0F0F).copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    }

    var selectedFormula by remember { mutableStateOf(OneRmFormula.EPLEY) }
    val assumedRepsByExercise = remember {
        mutableStateMapOf<String, Int>().apply {
            personalRecords.forEach { put(it.exerciseName, 1) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()

    ) {
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = staticGradientBrush,
            introAlpha = 0f,
            enableWaves = false,
            enableAnimation = false
        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("1 Rep Max", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = baseDark)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "1RM Estimator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = accentGlow.copy(alpha = 0.3f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Formula", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                                Spacer(Modifier.width(8.dp))
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(selectedFormula.label, color = Color.White)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        OneRmFormula.values().forEach { f ->
                                            DropdownMenuItem(
                                                text = { Text(f.label) },
                                                onClick = { selectedFormula = f; expanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (personalRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = baseDark)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("No records yet", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = baseDark)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                personalRecords.forEach { pr ->
                                    val reps = assumedRepsByExercise[pr.exerciseName] ?: 1
                                    val est = estimate1Rm(pr.maxWeight, reps, selectedFormula)

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(pr.exerciseName, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                                            Text(formatWeight(est), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Assumed reps", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        val newVal = (reps - 1).coerceAtLeast(1)
                                                        assumedRepsByExercise[pr.exerciseName] = newVal
                                                    }
                                                ) { Icon(Icons.Filled.Remove, contentDescription = null, tint = Color.White) }
                                                Text("$reps", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                                                IconButton(
                                                    onClick = {
                                                        val newVal = (reps + 1).coerceAtMost(20)
                                                        assumedRepsByExercise[pr.exerciseName] = newVal
                                                    }
                                                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) }

                                            }

                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.fillMaxWidth(),
                                            thickness = 2.dp,
                                            color = Color(0xFF450000)
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

private fun formatWeight(w: Double): String {
    val rounded = if (abs(w - w.toInt()) < 1e-6) "%d".format(w.toInt()) else String.format("%.1f", w)
    return "$rounded kg"
}
