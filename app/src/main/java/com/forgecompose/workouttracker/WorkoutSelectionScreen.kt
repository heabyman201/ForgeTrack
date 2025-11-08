package com.forgecompose.workouttracker

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

public val Context.presetUsageDataStore by preferencesDataStore("preset_usage")
private fun levenshteinDistance(lhs: String, rhs: String): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i
        for (j in 1..lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1
            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }
        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength]
}
public data class UsageStat(val count: Int, val lastUsed: Long)

public class PresetUsageTracker(private val context: Context) {
    private val KEY = stringSetPreferencesKey("usage_set")
    private val CAP = 999

    val usageFlow: Flow<Map<String, UsageStat>> =
        context.presetUsageDataStore.data.map { prefs ->
            val raw = prefs[KEY] ?: emptySet()
            val map = mutableMapOf<String, UsageStat>()
            for (entry in raw) {
                val parts = entry.split("::")
                if (parts.size >= 2) {
                    val name = parts.dropLast(2).joinToString("::").ifEmpty { parts[0] }
                    val count = parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: 0
                    val last = parts.getOrNull(parts.size - 1)?.toLongOrNull() ?: 0L
                    if (name.isNotBlank()) map[name] = UsageStat(count, last)
                }
            }
            map
        }

    suspend fun increment(name: String) {
        if (name.isBlank()) return
        val now = System.currentTimeMillis()
        context.presetUsageDataStore.edit { prefs ->
            val raw = prefs[KEY] ?: emptySet()
            val map = raw.toMutableMapParsed()
            val current = map[name] ?: UsageStat(0, 0L)
            val next = (current.count + 1).coerceAtMost(CAP)
            map[name] = UsageStat(next, now)
            prefs[KEY] = map.toStringSet()
        }
    }

    private fun Set<String>.toMutableMapParsed(): MutableMap<String, UsageStat> {
        val m = mutableMapOf<String, UsageStat>()
        for (entry in this) {
            val parts = entry.split("::")
            if (parts.size >= 2) {
                val name = parts.dropLast(2).joinToString("::").ifEmpty { parts[0] }
                val count = parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: 0
                val last = parts.getOrNull(parts.size - 1)?.toLongOrNull() ?: 0L
                if (name.isNotBlank()) m[name] = UsageStat(count, last)
            }
        }
        return m
    }

    private fun Map<String, UsageStat>.toStringSet(): Set<String> =
        entries.map { "${it.key}::${it.value.count}::${it.value.lastUsed}" }.toSet()
}

private val Context.favoritePresetsDataStore by preferencesDataStore("favorite_presets")

object FavoritePresetStore {
    private val KEY = stringSetPreferencesKey("favorite_names")

    fun flow(context: Context): Flow<Set<String>> =
        context.favoritePresetsDataStore.data.map { prefs ->
            prefs[KEY] ?: emptySet()
        }

    suspend fun toggle(context: Context, name: String) {
        if (name.isBlank()) return
        context.favoritePresetsDataStore.edit { prefs ->
            val currentFavorites = prefs[KEY] ?: emptySet()
            val newFavorites = currentFavorites.toMutableSet()
            if (name in newFavorites) {
                newFavorites.remove(name)
            } else {
                newFavorites.add(name)
            }
            prefs[KEY] = newFavorites
        }
    }
}


@Composable
private fun MostUsedPill(stat: UsageStat?, modifier: Modifier = Modifier) {
    if (stat == null || stat.count < 1) return
    val pillShape = remember { RoundedCornerShape(12.dp) }
    val now = System.currentTimeMillis()
    val diff = (now - stat.lastUsed).coerceAtLeast(0L)
    val ago = when {
        stat.lastUsed <= 0L -> ""
        diff < 60_000L -> "• just now"
        diff < 3_600_000L -> "• ${diff / 60_000L}m ago"
        diff < 86_400_000L -> "• ${diff / 3_600_000L}h ago"
        else -> "• ${diff / 86_400_000L}d ago"
    }

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
                text = if (stat.count >= 999) "Frequently used $ago" else "Frequently used $ago",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExperimentalPill( modifier: Modifier = Modifier) {



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

                imageVector = Icons.Filled.Warning,
                contentDescription = "Experimental Feature",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))

            Text(
                text = "Experimental Feature",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun CustomPill( modifier: Modifier = Modifier) {



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

                imageVector = Icons.Filled.AddTask,
                contentDescription = "Custom Preset",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))

            Text(
                text = "Custom Preset",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun FavouritePill( modifier: Modifier = Modifier) {



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

                imageVector = Icons.Filled.Favorite,
                contentDescription = "Custom Preset",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))

            Text(
                text = "Favorite",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSelector(
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    val cardioExerciseNames = listOf(
        "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
        "Rowing Machine", "Stationary Bike", "Swimming"
    )

    val haze = remember { HazeState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stages = rememberColdStartStages()
    val usageTracker = remember { PresetUsageTracker(context) }
    val usageMap by usageTracker.usageFlow.collectAsState(initial = emptyMap())
    val customPresets by CustomPresetStore.flow(context).collectAsState(initial = emptyList())
    val favoritePresets by FavoritePresetStore.flow(context).collectAsState(initial = emptySet())

    val abbreviationMap = remember {
        mapOf(
            "db" to "dumbbell",
            "bb" to "barbell",
            "ohp" to "overhead press",
            "bp" to "bench press",
            "dl" to "deadlift"
        )
    }
    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) { Brush.linearGradient(colors = introColors) }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }
    val blurAnim by animateDpAsState(if (showIntro) intensity.value else 0.dp, animationSpec = tween(length.value.toInt()), label = "blur")

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
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

    WorkoutTrackerTheme {
        val intent = remember { Intent(context, WorkoutActivity::class.java) }
        var searchText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }
        val baseCategories = listOf("All", "Bodyweight", "Dumbbell/Kettlebell", "Barbell", "Cardio", "Machines/Cables")
        val workoutCategories by remember(customPresets) {
            mutableStateOf(if (customPresets.isEmpty()) baseCategories else baseCategories + "Custom")
        }

        var showCreate by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Custom") }
        var newReps by remember { mutableStateOf("") }
        var newSets by remember { mutableStateOf("") }
        var newTimeMs by remember { mutableStateOf("") }

        val allPresets = remember(customPresets) { workoutPresets + customPresets }

        val filteredWorkoutsBase by remember(searchText, selectedCategory, abbreviationMap, allPresets) {
            derivedStateOf {
                val base = if (selectedCategory == "All") allPresets else allPresets.filter { it.category == selectedCategory }
                if (searchText.isBlank()) base else {
                    val lowerCaseSearchText = searchText.lowercase()
                    val searchTokens = lowerCaseSearchText.split(" ").filter { it.isNotBlank() }.map { abbreviationMap[it] ?: it }.toSet()
                    base.map { preset ->
                        var score = 0.0
                        val presetNameLower = preset.name.lowercase()
                        val presetTokens = presetNameLower.split(" ").filter { it.isNotBlank() }.toSet()
                        if (presetNameLower == lowerCaseSearchText) score += 1000
                        if (presetNameLower.contains(lowerCaseSearchText)) score += 100
                        val matchedTokens = searchTokens.intersect(presetTokens)
                        score += matchedTokens.size * 10.0
                        if (matchedTokens.size == searchTokens.size) score += 50
                        if (searchText.length > 2) {
                            for (searchToken in searchTokens) {
                                val best = presetTokens.minOfOrNull { presetToken -> levenshteinDistance(searchToken, presetToken) } ?: 100
                                val threshold = (searchToken.length / 4).coerceAtMost(2)
                                if (best <= threshold) score += (5.0 / (best + 1))
                            }
                        }
                        preset to score
                    }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
                }
            }
        }

        val filteredWorkouts by remember(filteredWorkoutsBase, usageMap, favoritePresets) {
            derivedStateOf {
                val now = System.currentTimeMillis()
                val maxCount = (usageMap.values.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                fun recencyScore(ts: Long): Float {
                    if (ts <= 0L) return 0f
                    val days = (now - ts).coerceAtLeast(0L) / 86_400_000f
                    return 1f / (1f + days)
                }
                filteredWorkoutsBase.sortedWith(
                    compareByDescending<WorkoutPreset> { it.name in favoritePresets }
                        .thenByDescending {
                            val stat = usageMap[it.name]
                            if (stat == null) 0f else (stat.count.toFloat() / maxCount) * 0.6f + recencyScore(stat.lastUsed) * 0.4f
                        }.thenBy { it.name.lowercase() }
                )
            }
        }

        val cardShape16 = remember { RoundedCornerShape(16.dp) }

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
                                    val y = baseY + sin(phase) * waveHeight * (0.55f + layer * 0.22f) * g
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
                            drawRect(introBrush, alpha = 1f - introProgress)
                        }
                    }
                }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Select Workout", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreate = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add preset")
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
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
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
                                val exact = allPresets.firstOrNull { it.name.equals(searchText, ignoreCase = true) }
                                if (exact != null) scope.launch { usageTracker.increment(exact.name) }
                            }
                        )
                    )

                    if (stages.afterFirstFrame) {
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
                                        Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    leadingIcon = if (isSelected) { { Icon(Icons.Filled.Done, contentDescription = "Selected", tint = Color.Black) } } else null,
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

                    if (stages.after200ms) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredWorkouts, key = { it.name }, contentType = { "preset" }) { preset ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = tween(100), label = "cardScale")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .hazeEffect(state = haze, style = HazeMaterials.ultraThick())
                                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = {
                                                if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch { usageTracker.increment(preset.name) }
                                                    workout.value = preset.name
                                                    context.startActivity(intent)
                                                }
                                            },
                                            onLongClick = {
                                                if (preset.category == "Custom") {
                                                    customDeletion.showDeleteDialog.value = true
                                                }
                                            }
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE)
                                            Color(0xFF3D0000).copy(alpha = 0.3f) else Color.DarkGray
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = preset.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                            val stat = usageMap[preset.name]
                                            val isFavorite = preset.name in favoritePresets
                                            MostUsedPill(stat = stat, modifier = Modifier.padding(top = 6.dp))
                                            if (preset.name in cardioExerciseNames) {
                                                ExperimentalPill(modifier = Modifier.padding(top = 6.dp))
                                            }
                                            if (preset.category == "Custom") {
                                                CustomPill(modifier = Modifier.padding(top = 6.dp))
                                            }
                                            if (preset.name in favoritePresets){
                                                FavouritePill(modifier = Modifier.padding(top = 6.dp))
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isFavorite = preset.name in favoritePresets
                                            IconButton(onClick = {
                                                scope.launch {
                                                    FavoritePresetStore.toggle(context, preset.name)
                                                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFavorite) Color(0xFFFF0000) else Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreate) {
                Dialog(onDismissRequest = { showCreate = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .hazeEffect(state = haze, style = HazeMaterials.ultraThick()),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF3D0000).copy(alpha = 1.0f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF4A0000).copy(alpha = 0.35f),
                                            Color(0xFF1A0000).copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF8C3131),
                                                Color(0xFF702727)
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "Create Custom Preset",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                    focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                                    unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = newCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    shape = RoundedCornerShape(16.dp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                        focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                                        unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    (baseCategories.drop(1) + "Custom").forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c) },
                                            onClick = { newCategory = c; expanded = false }
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newReps,
                                    onValueChange = { newReps = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Goal reps") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                        focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                                        unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newSets,
                                    onValueChange = { newSets = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Goal sets") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                        focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                                        unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = newTimeMs,
                                onValueChange = { newTimeMs = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Goal time ms") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                    focusedContainerColor = Color(0xFF4A0000).copy(alpha = 0.25f),
                                    unfocusedContainerColor = Color(0xFF3D0000).copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.92f),
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCreate = false },
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.06f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val name = newName.trim()
                                        if (name.isNotEmpty()) {
                                            val reps = newReps.toIntOrNull()
                                            val sets = newSets.toIntOrNull()
                                            val time = newTimeMs.toLongOrNull()
                                            scope.launch {
                                                CustomPresetStore.add(
                                                    context,
                                                    WorkoutPreset(
                                                        name = name,
                                                        category = newCategory.ifBlank { "Custom" },
                                                        goalReps = reps,
                                                        goalSets = sets,
                                                        goalTimeMillis = time
                                                    )
                                                )
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            newName = ""
                                            newCategory = "Custom"
                                            newReps = ""
                                            newSets = ""
                                            newTimeMs = ""
                                            showCreate = false
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.95f),
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save")
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


object customDeletion {
    var showDeleteDialog = mutableStateOf(false)
}