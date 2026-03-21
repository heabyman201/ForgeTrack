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


import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "introFade"
    )
    LaunchedEffect(Unit) {
        showIntro = false
        Firebase.crashlytics.setCustomKey("current_screen", "Settings Screen")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = introBrush,
            introAlpha = 0f,
            enableAnimation = movingEffectsEnabled,
            enableWaves = movingEffectsEnabled,




        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {
                    SettingsSectionCard(title = "Account") {
                        SettingsOptionRow(
                            title = "Edit Profile",
                            subtitle = "Change your name, weight, height, etc.",
                            icon = Icons.Default.Person,
                            onClick = { navController.navigate("EditUserStats")  }
                        )
                        SettingsOptionRow(
                            title = "Health Connect",
                            subtitle = "Enable or Disable Health Connect",
                            icon = Icons.Default.HealthAndSafety,
                            onClick = { navController.navigate("HealthConnect")  }
                        )
                    }
                }
                item{
                    SettingsSectionCard(title = "Appearance") {
                        SettingsOptionRow(
                            title = "App theme",
                            subtitle = "Change app theme",
                            icon = Icons.Default.Palette,
                            onClick = {
                                navController.navigate("AppearanceScreen")

                            }
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Preferences") {
                        SettingsToggleRow(
                            title = "Show Body Heatmap",
                            subtitle = "Show anatomical heatmap above the muscle tiles",
                            icon = Icons.Default.Visibility,
                            checked = performanceOptions.showBodyHeatmap,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    PerformanceOptionsManager.setShowBodyHeatmap(context, enabled)
                                }
                            }
                        )
                        SettingsOptionRow(
                            title = "Performance",
                            subtitle = "Adjust settings for best performance and battery life",
                            icon = Icons.Default.Speed,
                            onClick = {  navController.navigate("PerformanceOptions")  }
                        )



                        SettingsOptionRow(
                            title = "AI Settings",
                            subtitle = "Change AI persona, disable or enable AI features",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {  navController.navigate("PersonaSettings")  }
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Misc") {
                        SettingsOptionRow(
                            title = "Memory Monitor",
                            subtitle = "Watch live heap usage and memory pressure",
                            icon = Icons.Default.Storage,
                            onClick = { navController.navigate("MemoryMonitor") }
                        )
                        SettingsOptionRow(
                            title = "Rep Predictor Example",
                            subtitle = "Basic TFLITE model to predict reps",
                            icon = Icons.Default.Preview,
                            onClick = {  navController.navigate("RepPredictor")  }
                        )
                    }
                }




            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryMonitorScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    val monitorState by AppMemoryMonitorStore.state.collectAsState()
    val latestSnapshot = monitorState.latestSnapshot ?: remember { captureAppMemorySnapshot(context) }
    val history = monitorState.history

    LaunchedEffect(Unit) {
        Firebase.crashlytics.setCustomKey("current_screen", "Memory Monitor")
        MemoryMonitorForegroundService.start(context)
    }

    val heapUsageRatio by remember(latestSnapshot) {
        derivedStateOf {
            if (latestSnapshot.maxHeapMb <= 0) 0f
            else latestSnapshot.usedHeapMb.toFloat() / latestSnapshot.maxHeapMb.toFloat()
        }
    }
    val pssUsageRatio by remember(latestSnapshot) {
        derivedStateOf {
            if (latestSnapshot.largeMemoryClassMb <= 0) 0f
            else latestSnapshot.totalPssMb.toFloat() / latestSnapshot.largeMemoryClassMb.toFloat()
        }
    }
    val samplesAgeSeconds by remember(latestSnapshot, history.size) {
        derivedStateOf {
            if (history.size < 2) 0
            else ((latestSnapshot.capturedAtMs - history.first().capturedAtMs) / 1000L).toInt()
        }
    }
    val deltaSinceMonitorStartMb by remember(latestSnapshot, monitorState.sessionStartSnapshot) {
        derivedStateOf {
            monitorState.sessionStartSnapshot?.let { latestSnapshot.totalPssMb - it.totalPssMb }
        }
    }
    val peakSinceMonitorStartMb by remember(history, monitorState.sessionStartSnapshot, latestSnapshot) {
        derivedStateOf {
            monitorState.sessionStartSnapshot?.let { start ->
                (history.maxOfOrNull { it.totalPssMb } ?: latestSnapshot.totalPssMb) - start.totalPssMb
            }
        }
    }
    val deltaSinceAnchorMb by remember(latestSnapshot, monitorState.anchorSnapshot) {
        derivedStateOf {
            monitorState.anchorSnapshot?.let { latestSnapshot.totalPssMb - it.totalPssMb }
        }
    }
    val peakSinceAnchorMb by remember(monitorState.anchorSnapshot, monitorState.peakSinceAnchorMb) {
        derivedStateOf {
            monitorState.anchorSnapshot?.let { anchor ->
                (monitorState.peakSinceAnchorMb ?: anchor.totalPssMb) - anchor.totalPssMb
            }
        }
    }
    val deltaSinceWorkoutEndMb by remember(latestSnapshot, monitorState.workoutEndSnapshot) {
        derivedStateOf {
            monitorState.workoutEndSnapshot?.let { latestSnapshot.totalPssMb - it.totalPssMb }
        }
    }
    val peakDeltaSinceWorkoutEndMb by remember(monitorState.workoutEndSnapshot, monitorState.peakSinceWorkoutEndMb) {
        derivedStateOf {
            monitorState.workoutEndSnapshot?.let { end ->
                (monitorState.peakSinceWorkoutEndMb ?: end.totalPssMb) - end.totalPssMb
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = Brush.linearGradient(
                colors = listOf(
                    theme.background.copy(alpha = 0.96f),
                    theme.background.copy(alpha = 0.88f),
                    theme.tertiary.copy(alpha = 0.32f)
                )
            ),
            introAlpha = 0f,
            enableAnimation = false,
            enableWaves = false
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Memory Monitor", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SettingsSectionCard(title = "Live Snapshot") {
                        MemoryHighlightRow(
                            label = "Java Heap",
                            value = "${latestSnapshot.usedHeapMb} MB / ${latestSnapshot.maxHeapMb} MB",
                            progress = heapUsageRatio,
                            accent = theme.primary
                        )
                        MemoryHighlightRow(
                            label = "Total PSS",
                            value = "${latestSnapshot.totalPssMb} MB / ${latestSnapshot.largeMemoryClassMb} MB budget",
                            progress = pssUsageRatio,
                            accent = theme.secondary
                        )
                        MemoryMetricGrid(
                            items = listOf(
                                "Committed Heap" to "${latestSnapshot.committedHeapMb} MB",
                                "Native Heap" to "${latestSnapshot.nativeHeapMb} MB",
                                "Avail System" to "${latestSnapshot.availSystemMb} MB",
                                "Threshold" to "${latestSnapshot.thresholdMb} MB"
                            )
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Trend") {
                        Text(
                            text = "Last ${kotlin.math.max(samplesAgeSeconds, 1)}s sampled every second",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MemoryHistoryChart(
                            points = history.map { it.usedHeapMb.toFloat() },
                            strokeColor = theme.primary,
                            fillColor = theme.secondary.copy(alpha = 0.18f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MemoryMetricGrid(
                            items = listOf(
                                "Min Heap" to "${history.minOfOrNull { it.usedHeapMb } ?: latestSnapshot.usedHeapMb} MB",
                                "Peak Heap" to "${history.maxOfOrNull { it.usedHeapMb } ?: latestSnapshot.usedHeapMb} MB",
                                "Latest PSS" to "${latestSnapshot.totalPssMb} MB",
                                "Low Memory" to if (latestSnapshot.lowMemory) "Yes" else "No"
                            )
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Deltas") {
                        MemoryMetricGrid(
                            items = listOf(
                                "Since Start" to (deltaSinceMonitorStartMb?.let(::formatDeltaMb) ?: "Waiting for samples"),
                                "Peak Since Start" to (peakSinceMonitorStartMb?.let(::formatDeltaMb) ?: "Waiting for samples"),
                                "Since Anchor" to (deltaSinceAnchorMb?.let(::formatDeltaMb) ?: "Mark an anchor"),
                                "Peak Since Anchor" to (peakSinceAnchorMb?.let(::formatDeltaMb) ?: "Mark an anchor"),
                                "Since Workout End" to (deltaSinceWorkoutEndMb?.let(::formatDeltaMb) ?: "Finish a workout while monitor runs"),
                                "Peak After End" to (peakDeltaSinceWorkoutEndMb?.let(::formatDeltaMb) ?: "Waiting for workout to end")
                            )
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Service") {
                        MemoryMetricGrid(
                            items = listOf(
                                "Monitor Status" to if (monitorState.isMonitoring) "Foreground service active" else "Stopped",
                                "Notification" to if (monitorState.isMonitoring) "Persistent" else "Not shown"
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (monitorState.isMonitoring) {
                                        MemoryMonitorForegroundService.stop(context)
                                    } else {
                                        MemoryMonitorForegroundService.start(context)
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = theme.primary
                                ),
                                border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.55f))
                            ) {
                                Text(if (monitorState.isMonitoring) "Stop Monitor" else "Start Monitor")
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { AppMemoryMonitorStore.markAnchor() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = theme.secondary
                                ),
                                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.55f))
                            ) {
                                Text("Mark Anchor")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sampling keeps running while you move through the app until you stop the service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "PSS Breakdown") {
                        MemoryMetricGrid(
                            items = listOf(
                                "Dalvik PSS" to "${latestSnapshot.dalvikPssMb} MB",
                                "Native PSS" to "${latestSnapshot.nativePssMb} MB",
                                "Other PSS" to "${latestSnapshot.otherPssMb} MB",
                                "Private Dirty" to "${latestSnapshot.privateDirtyMb} MB",
                                "Graphics" to "${latestSnapshot.graphicsMb} MB",
                                "Code" to "${latestSnapshot.codeMb} MB",
                                "Stack" to "${latestSnapshot.stackMb} MB",
                                "System" to "${latestSnapshot.systemMb} MB"
                            )
                        )
                    }
                }

                item {
                    SettingsSectionCard(title = "Runtime Limits") {
                        MemoryMetricGrid(
                            items = listOf(
                                "memoryClass" to "${latestSnapshot.memoryClassMb} MB",
                                "largeMemoryClass" to "${latestSnapshot.largeMemoryClassMb} MB",
                                "Heap Headroom" to "${(latestSnapshot.maxHeapMb - latestSnapshot.usedHeapMb).coerceAtLeast(0)} MB",
                                "Monitor Tip" to "Finish a workout and watch whether heap falls back"
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatDeltaMb(deltaMb: Int): String {
    return if (deltaMb > 0) "+${deltaMb} MB" else "${deltaMb} MB"
}

@Composable
private fun MemoryHighlightRow(
    label: String,
    value: String,
    progress: Float,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = accent,
            trackColor = accent.copy(alpha = 0.18f)
        )
    }
}

@Composable
private fun MemoryMetricGrid(
    items: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.045f),
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.58f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MemoryHistoryChart(
    points: List<Float>,
    strokeColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val safePoints = remember(points) { if (points.isEmpty()) listOf(0f) else points }
    val minValue = safePoints.minOrNull() ?: 0f
    val maxValue = (safePoints.maxOrNull() ?: minValue).coerceAtLeast(minValue + 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.03f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Heap history",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = "${minValue.toInt()}-${maxValue.toInt()} MB",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.58f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val verticalStep = size.height / 4f
                repeat(4) { index ->
                    val y = verticalStep * index
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (safePoints.size == 1) {
                    drawCircle(
                        color = strokeColor,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    return@Canvas
                }

                val strokePath = Path()
                val fillPath = Path()

                safePoints.forEachIndexed { index, point ->
                    val x = size.width * index.toFloat() / (safePoints.lastIndex.coerceAtLeast(1)).toFloat()
                    val normalized = ((point - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                    val y = size.height - (normalized * size.height)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                drawPath(path = fillPath, color = fillColor)
                drawPath(
                    path = strokePath,
                    color = strokeColor,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}



@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                // Dynamic background using Secondary -> Tertiary/Background
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        theme.background.copy(alpha = 0.6f),
                        theme.background.copy(alpha = 0.95f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                // Dynamic border using Primary -> Secondary
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.2f),
                        theme.secondary.copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = theme.primary.copy(alpha = 0.3f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}


@Composable
fun SettingsOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.primary, // Use Theme Primary
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = theme.secondary.copy(alpha = 0.5f), // Use Theme Secondary for navigation arrow
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.background,
                checkedTrackColor = theme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = theme.secondary.copy(alpha = 0.45f)
            )
        )
    }
}
