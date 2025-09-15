package com.forgecompose.workouttracker

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

public val Context.presetUsageDataStore by preferencesDataStore("preset_usage")

public class PresetUsageTracker(private val context: Context) {
    private val KEY = stringSetPreferencesKey("usage_set")
    private val CAP = 999

    val usageFlow: Flow<Map<String, Int>> =
        context.presetUsageDataStore.data.map { prefs ->
            val raw = prefs[KEY] ?: emptySet()
            val map = mutableMapOf<String, Int>()
            for (entry in raw) {
                val idx = entry.lastIndexOf("::")
                if (idx > 0) {
                    val name = entry.substring(0, idx)
                    val count = entry.substring(idx + 2).toIntOrNull() ?: 0
                    if (name.isNotBlank()) map[name] = count
                }
            }
            map
        }

    suspend fun increment(name: String) {
        if (name.isBlank()) return
        context.presetUsageDataStore.edit { prefs ->
            val raw = prefs[KEY] ?: emptySet()
            val map = raw.toMutableMapParsed()
            val next = (map[name] ?: 0) + 1
            map[name] = next.coerceAtMost(CAP) // (3) cap to 999
            prefs[KEY] = map.toStringSet()
        }
    }

    // Helpers to parse/encode the Set<String> representation
    private fun Set<String>.toMutableMapParsed(): MutableMap<String, Int> {
        val m = mutableMapOf<String, Int>()
        for (entry in this) {
            val idx = entry.lastIndexOf("::")
            if (idx > 0) {
                val name = entry.substring(0, idx)
                val count = entry.substring(idx + 2).toIntOrNull() ?: 0
                if (name.isNotBlank()) m[name] = count
            }
        }
        return m
    }

    private fun Map<String, Int>.toStringSet(): Set<String> =
        entries.map { "${it.key}::${it.value}" }.toSet()
}

// ---------- Tiny UI bits ----------
@Composable
private fun MostUsedPill(count: Int, modifier: Modifier = Modifier) {
    // Show only if meaningful
    if (count < 3) return

    val pillShape = remember { RoundedCornerShape(12.dp) }

    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.2f),
            shape = pillShape
        ),
        shape = pillShape,
        color = Color(0xFF4A0000).copy(alpha = 0.6f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(

                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Most Used",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (count >= 999) "Most used" else "Most used • $count",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSelector(
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    // Theming from WorkoutDetailScreen
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
    val haze = remember { HazeState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stages = rememberColdStartStages()

    val usageTracker = remember { PresetUsageTracker(context) }
    val usageMap by usageTracker.usageFlow.collectAsState(initial = emptyMap())

    WorkoutTrackerTheme {
        val intent = remember { Intent(context, WorkoutActivity::class.java) }
        var searchText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val workoutCategories = listOf("All", "Bodyweight", "Dumbbell/Kettlebell", "Barbell", "Cardio", "Machines/Cables")

        val filteredWorkoutsBase by remember(searchText, selectedCategory) {
            derivedStateOf {
                val base = if (selectedCategory == "All") workoutPresets
                else workoutPresets.filter { it.category == selectedCategory }
                if (searchText.isBlank()) base
                else base.filter { it.name.contains(searchText, ignoreCase = true) }
            }
        }

        val filteredWorkouts by remember(filteredWorkoutsBase, usageMap) {
            derivedStateOf {
                filteredWorkoutsBase.sortedWith(
                    compareByDescending<WorkoutPreset> { usageMap[it.name] ?: 0 }
                        .thenBy { it.name.lowercase() }
                )
            }
        }

        val cardShape16 = remember { RoundedCornerShape(16.dp) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060202))
                .background(staticGradientBrush)
                .background(secondaryStaticBrush)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Select Workout",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
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
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search workouts...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f))
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                            unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val exact = workoutPresets.firstOrNull { it.name.equals(searchText, ignoreCase = true) }
                                if (exact != null) scope.launch { usageTracker.increment(exact.name) }
                            }
                        )
                    )

                    if (stages.after200ms) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(workoutCategories, key = { it }, contentType = { "cat" }) { category ->
                                val isSelected = category == selectedCategory
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    label = {
                                        Text(
                                            category,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Filled.Done, contentDescription = "Selected", tint = Color.Black) }
                                    } else null,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White.copy(alpha = 0.95f),
                                        containerColor = Color(0xFF3D0000).copy(alpha = 0.4f),
                                        selectedLabelColor = Color.Black,
                                        labelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.White.copy(alpha = 0.2f),
                                        selectedBorderColor = Color.White.copy(alpha = 0.7f),
                                        borderWidth = if (isSelected) 2.dp else 1.dp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (stages.after600ms) {

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredWorkouts, key = { it.name }, contentType = { "preset" }) { preset ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = tween(100), label = "cardScale")
                                val count = usageMap[preset.name] ?: 0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .hazeEffect(state = haze, style = HazeMaterials.ultraThick())
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.1f),
                                            shape = cardShape16
                                        )
                                        .clickable(interactionSource = interactionSource, indication = null) {
                                            if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                scope.launch { usageTracker.increment(preset.name) }
                                                workout.value = preset.name
                                                context.startActivity(intent)
                                            }
                                        },
                                    shape = cardShape16,
                                    colors = CardDefaults.cardColors(containerColor = if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE)
                                        Color(0xFF3D0000).copy(alpha = 0.3f) else Color.DarkGray)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            MostUsedPill(count = count, modifier = Modifier.padding(top = 6.dp))
                                        }
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingTaskbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f),
                navController = navController,
                cornerRadius = 34.dp,
                iconAlpha = 1f,
                uiState = uiState
            )
        }
    }
}