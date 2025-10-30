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
    val introBrush = remember(introColors) {
        Brush.linearGradient(colors = introColors)
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }
    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )


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


    fun NavController.openWorkout(id: Long) {
        navigate("${Routes.DetailedWorkout}/$id")
    }

    LaunchedEffect(Unit) {
        taskbarOverride.shouldOverrideVisiblity.value = false
    }

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
                            val py = h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                            val alpha = baseAlpha * (0.35f + sin(waveOffset + i) * 0.25f) * g
                            drawCircle(Color.White.copy(alpha = alpha), r, Offset(px, py))
                        }
                    }
                    if (introProgress < 1f) {
                        drawRect(introBrush, alpha = 1f - introProgress)
                    }
                }
                // --- End of Replaced Block ---
            }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                            val filtered = workouts.filter {
                                it.name.contains(searchQuery, ignoreCase = true)
                            }
                            if (sortAscending) {
                                filtered.sortedBy { it.id }
                            } else {
                                filtered.sortedByDescending { it.id }
                            }
                        }

                        if (filteredAndSortedWorkouts.isEmpty()) {
                            EmptyState()
                        } else {
                            WorkoutHistoryList(
                                workouts = filteredAndSortedWorkouts,
                                onWorkoutClicked = { workout ->
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("selectedWorkoutId", workout.id)
                                    navController.openWorkout(workout.id.toLong())
                                },
                                onDeleteClicked = { workout ->
                                    scope.launch(Dispatchers.IO) { viewModel.deleteWorkout(workout) }
                                }
                            )
                        }
                    }
                }
            }

            FloatingTaskbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                navController = navController,
                cornerRadius = 34.dp,
                iconAlpha = 1f,
                uiState = uiState
            )
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
        items(
            items = workouts,
            key = { it.id }
        ) { workout ->
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                            onClick = { confirmDelete = false
                                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }
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
                        ) {
                            Text("Delete")
                        }
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