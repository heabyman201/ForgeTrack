package com.forgecompose.workouttracker.workout

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ui.components.blurAnim.intensity
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ExportDateRange {
    LAST_WEEK,
    LAST_MONTH,
    LAST_YEAR,
    LIFETIME
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutHistory(
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }


    // --- Dynamic Intro Colors based on Theme ---
    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) { Brush.linearGradient(colors = introColors) }

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }
    val blurAnim by animateDpAsState(if (showIntro) intensity.value else 0.dp, animationSpec = tween(length.value.toInt()), label = "blur")

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val stages = rememberColdStartStages()
    val enableAnim = remember(movingEffectsEnabled, stages.afterFirstFrame) { movingEffectsEnabled && stages.afterFirstFrame }

    LaunchedEffect(Unit) { taskbarOverride.shouldOverrideVisiblity.value = false;
        Firebase.crashlytics.setCustomKey("current_screen", "Workout History Screen")}
    LaunchedEffect(Unit) {
        viewModel.syncHealthConnectWorkouts(context)
    }

    val forgeBackdrop = rememberForgeBackdrop()
    CompositionLocalProvider(LocalForgeBackdrop provides forgeBackdrop) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        // Dynamic Menu Background
                        modifier = Modifier.background(theme.tertiary.copy(alpha = 0.95f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort Oldest to Newest", color = Color.White) },
                            onClick = {
                                sortAscending = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort Newest to Oldest", color = Color.White) },
                            onClick = {
                                sortAscending = false
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to CSV", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showExportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete All", color = theme.primary) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmation = true
                            }
                        )
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
        modifier = Modifier
            .fillMaxSize()
            .blur(blurAnim)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedBackdrop(
                modifier = Modifier
                    .matchParentSize()
                    .padding(paddingValues)
                    .forgeBackdropSource(forgeBackdrop),
                introBrush = introBrush,
                introAlpha = 1f - introProgress,
                enableWaves = stages.after600ms && enableAnim,
                enableAnimation = enableAnim,

                )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search workouts...", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = theme.secondary.copy(alpha = 0.25f),
                        unfocusedContainerColor = theme.tertiary.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                        cursorColor = theme.primary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                )

                when (val state = uiState) {
                    is WorkoutListUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = theme.primary)
                    }
                    is WorkoutListUiState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is WorkoutListUiState.Success -> {
                        val workouts = state.workouts
                        val filteredAndSortedWorkouts = remember(workouts, searchQuery, sortAscending) {
                            val filtered = workouts.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            if (sortAscending) filtered.sortedBy { it.id } else filtered.sortedByDescending { it.id }
                        }
                        if (filteredAndSortedWorkouts.isEmpty()) {
                            EmptyState()
                        } else {
                            WorkoutHistoryList(
                                workouts = filteredAndSortedWorkouts,
                                theme = theme, // Pass theme down
                                onWorkoutClicked = { workout ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedWorkoutId", workout.id)
                                    navController.navigate("${Routes.DetailedWorkout}/${workout.id}")
                                },
                                onDeleteClicked = { workout ->
                                    scope.launch(Dispatchers.IO) { viewModel.deleteWorkout(workout) }
                                }
                            )
                        }
                    }
                }
            }


        }
    }
    }

    val haptics = LocalHapticFeedback.current
    if (showDeleteConfirmation) {
        ThemedConfirmationDialog(
            title = "Confirm Deletion",
            text = "Are you sure you want to permanently delete all workout history? This action cannot be undone.",
            buttonText = "Delete All",
            additionalButton = false,
            additionalButtonText = "Cancel",
            onCustomAction = {},
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch(Dispatchers.IO) { viewModel.deleteAllWorkouts() }
                showDeleteConfirmation = false
            },
            onDismiss = {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                showDeleteConfirmation = false
            }
        )
    }
    if (showExportDialog) {
        ExportOptionsDialog(
            theme = theme,
            onDismiss = { showExportDialog = false },
            onExport = { range ->
                showExportDialog = false
                viewModel.exportWorkoutsToCsv(context, range) { message ->
//                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
fun ExportOptionsDialog(
    theme: ColorSchemeAppTheme,
    onDismiss: () -> Unit,
    onExport: (ExportDateRange) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Workout History", color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select the time range to export:", color = Color.White.copy(alpha = 0.8f))
                Button(
                    onClick = { onExport(ExportDateRange.LAST_WEEK) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("Last 7 Days")
                }
                Button(
                    onClick = { onExport(ExportDateRange.LAST_MONTH) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("Last 30 Days")
                }
                Button(
                    onClick = { onExport(ExportDateRange.LAST_YEAR) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("Last 365 Days")
                }
                Button(
                    onClick = { onExport(ExportDateRange.LIFETIME) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("All Time")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.primary)
            }
        },
        containerColor = theme.background,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun WorkoutHistoryList(
    workouts: List<Workout>,
    theme: ColorSchemeAppTheme,
    onWorkoutClicked: (Workout) -> Unit,
    onDeleteClicked: (Workout) -> Unit
) {
    val liststate = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(workouts) {
        liststate.animateScrollToItem(0)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = liststate,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = workouts, key = { it.id }) { workout ->
            WorkoutHistoryItem(
                workout = workout,
                theme = theme,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onWorkoutClicked(workout)
                },
                onDelete = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDeleteClicked(workout)
                }
            )
        }
    }
}

@Composable
private fun WorkoutHistoryItem(
    workout: Workout,
    theme: ColorSchemeAppTheme,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cornerRadius = 34.dp
    var isPressed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "scale")
    val durationText = remember(workout.durationMillis) {
        val safe = workout.durationMillis ?: 0L
        val hours = (safe / 3_600_000).toInt()
        val minutes = ((safe / 60_000) % 60).toInt()
        val seconds = ((safe / 1_000) % 60).toInt()
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${seconds}s"
    }
    val haptics = LocalHapticFeedback.current
    val isImportedFromHealthConnect = workout.notes?.contains("[HC_EXTERNAL]") == true &&
        workout.notes?.contains("[HC_ID:") == true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .forgeSharedBounds("workout-card-${workout.id}")
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                // Dynamic Item Background
                val bgBrush = Brush.radialGradient(
                    colors = listOf(theme.background.copy(alpha = 0.75f), Color.Transparent.copy(alpha = 0.75f)),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.2f),
                        theme.secondary.copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx))
                    drawRoundRect(brush = borderBrush, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { if (!confirmDelete) onClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = confirmDelete,
                label = "confirmSwap",
                modifier = Modifier.weight(1f)
            ) { confirming ->
                if (!confirming) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = workout.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (workout.notes?.contains("Watch Workout",ignoreCase = true) ?: false ) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = "Watch Workout",
                                    tint = theme.primary.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isImportedFromHealthConnect) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Imported from Health Connect",
                                    tint = theme.primary.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duration: $durationText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                confirmDelete = false
                                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                confirmDelete = false
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDelete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary,
                                contentColor = Color.White
                            )
                        ) { Text("Delete") }
                    }
                }
            }
            IconButton(onClick = { confirmDelete = !confirmDelete }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete workout",
                    tint = theme.primary.copy(alpha = 0.9f)
                )
            }
        }
    }
}
