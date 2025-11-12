package com.forgecompose.workouttracker

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutHistory(
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }

    val hour = remember { LocalTime.now().hour }
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

    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val stages = rememberColdStartStages()
    val enableAnim = remember(movingEffectsEnabled, stages.afterFirstFrame) { movingEffectsEnabled && stages.afterFirstFrame }

    LaunchedEffect(Unit) { taskbarOverride.shouldOverrideVisiblity.value = false }

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
                        modifier = Modifier.background(Color(0xFF2E0F0F).copy(alpha = 0.95f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort Oldest to Newest") },
                            onClick = {
                                sortAscending = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort Newest to Oldest") },
                            onClick = {
                                sortAscending = false
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete All", color = MaterialTheme.colorScheme.error) },
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
                    .padding(paddingValues),
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
                )

                when (val state = uiState) {
                    is WorkoutListUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFFFF3B30))
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
}

@Composable
private fun WorkoutHistoryList(
    workouts: List<Workout>,
    onWorkoutClicked: (Workout) -> Unit,
    onDeleteClicked: (Workout) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = workouts, key = { it.id }) { workout ->
            WorkoutHistoryItem(
                workout = workout,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                val bgBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF130606).copy(alpha = 0.75f), Color(0xFF100404).copy(alpha = 0.75f)),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5555).copy(alpha = 0.2f),
                        Color(0xFF8B0000).copy(alpha = 0.1f)
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
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                                containerColor = Color(0xFFFF3535),
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
                    tint = Color(0xFFFF3535).copy(alpha = 0.9f)
                )
            }
        }
    }
}
