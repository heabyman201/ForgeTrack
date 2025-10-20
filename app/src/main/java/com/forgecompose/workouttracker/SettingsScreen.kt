package com.forgecompose.workouttracker


import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {

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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                            val py =
                                h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                            val alpha =
                                baseAlpha * (0.35f + sin(waveOffset + i) * 0.25f) * g
                            drawCircle(Color.White.copy(alpha = alpha), r, Offset(px, py))
                        }
                    }
                    if (introProgress < 1f) {
                        drawRect(brush = introBrush, alpha = 1f - introProgress)
                    }
                }
            }
    ) {
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


                item {
                    SettingsSectionCard(title = "Preferences") {
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


            }
        }
    }
}



@Composable
private fun SettingsSectionCard(
    title: String,
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
                    colors = listOf(
                        Color(0xFF180909).copy(alpha = 0.9f),
                        Color(0xFF100404).copy(alpha = 0.95f)
                    ),
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
            color = Color(0xFFFF3535).copy(alpha = 0.3f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}


@Composable
private fun SettingsOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
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
            tint = Color(0xFFFF3B30),
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
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}