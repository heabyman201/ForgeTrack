package com.forgecompose.workouttracker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

data class PersonalRecord(val exerciseName: String, val maxWeight: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: WorkoutListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager(context) }
    val userAge = remember { prefsManager.getAge() }
    val userHeight = remember { prefsManager.getHeight() }
    val userWeight = remember { prefsManager.getWeight() }
    val userName = remember { prefsManager.getName() }
    val stages = rememberColdStartStages()

    val (personalRecords, recentWorkouts) = remember(uiState) {
        if (uiState is WorkoutListUiState.Success) {
            val workouts = (uiState as WorkoutListUiState.Success).workouts
            val prs = workouts
                .asSequence()
                .filter { (it.weight ?: 0.0) > 0.0 }
                .groupBy { it.name }
                .map { (name, workoutList) -> PersonalRecord(name, workoutList.maxOf { it.weight!! }) }
                .sortedByDescending { it.maxWeight }
                .take(4)
            val recent = workouts.take(4)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
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
    val introBrush = remember(introColors) {
        Brush.linearGradient(colors = introColors)
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

    val performanceOptions by PerformanceOptionsManager.flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)
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

    val nameStyle = MaterialTheme.typography.headlineMedium.copy(
        shadow = Shadow(color = Color.White.copy(alpha = 0.3f), blurRadius = 8f)
    )

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )
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
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("Settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlowingAvatar(icon = Icons.Default.Person)
                        Text(
                            userName,
                            style = nameStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                if (stages.after600ms && personalRecords.isNotEmpty()) {
                    item {
                        ProfileSectionCard(
                            title = "Personal Records",
                            action = {
                                CardActionButton("More") { navController.navigate("RepMax") }
                            }
                        ) {
                            personalRecords.forEach { pr ->
                                ProfileStatRow(
                                    label = pr.exerciseName,
                                    value = "${String.format("%.1f", pr.maxWeight)} kg",
                                    icon = Icons.Default.Stars,
                                    iconTint = Color(0xFFfce18a)
                                )
                            }
                        }
                    }
                }
                if (stages.after200ms) {
                    item {
                        ProfileSectionCard(
                            title = "Body Stats",
                            action = {
                                CardActionButton("Edit Profile") { navController.navigate("EditUserStats") }
                            }
                        ) {
                            ProfileStatRow("Age", userAge, Icons.Default.Person)
                            ProfileStatRow("Height", "$userHeight cm", Icons.Default.Height)
                            ProfileStatRow("Weight", "$userWeight kg", Icons.Default.MonitorWeight)
                            ProfileStatRow("Experience", prefsManager.getExperience(), Icons.Default.DataExploration)
                        }
                    }
                }



                if (stages.after200ms && recentWorkouts.isNotEmpty()) {
                    item {
                        ProfileSectionCard(
                            title = "Recent Activity",
                            action = {
                                CardActionButton("View History") { navController.navigate("WorkoutHistory") }
                            }
                        ) {
                            recentWorkouts.forEach { workout ->
                                val date = remember(workout.date) { dateFormatter.format(Date(workout.date)) }
                                ProfileStatRow(workout.name, date, Icons.Default.History)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        if (stages.after100ms) {
            FloatingTaskbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navController = navController,
                cornerRadius = 34.dp,
                iconAlpha = 1f,
                uiState = uiState
            )
        }
    }
}

@Composable
private fun GlowingAvatar(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3A0E0E), Color(0xFF120707)),
                    radius = size.minDimension / 2f * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5555).copy(alpha = 0.5f),
                        Color(0xFF8B0000).copy(alpha = 0.3f)
                    )
                )
                onDrawBehind {
                    drawCircle(bgBrush)
                    drawCircle(borderBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "User Avatar",
            tint = Color(0xFFFF3B30),
            modifier = Modifier
                .size(60.dp)
                .drawWithCache {
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF3B30).copy(alpha = 0.4f), Color.Transparent),
                        radius = size.minDimension
                    )
                    onDrawBehind {
                        drawCircle(glowBrush)
                    }
                }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                val bgBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF180909).copy(alpha = 0.9f), Color(0xFF100404).copy(alpha = 0.95f)),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5555).copy(alpha = 0.2f),
                        Color(0xFF8B0000).copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            action?.invoke()
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color(0xFFFF3535).copy(alpha = 0.3f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun CardActionButton(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    TextButton(
        onClick = {
            onClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileStatRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFFFF3B30)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}