package com.forgecompose.workouttracker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            val recent = workouts.take(5)
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
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

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

    val haptic = LocalHapticFeedback.current


    val haze = remember { HazeState() }
    val typography = MaterialTheme.typography
    val nameStyle = remember(typography) {
        typography.headlineMedium.copy(
            shadow = Shadow(
                color = Color.White.copy(alpha = 0.3f),
                offset = Offset(0f, 0f),
                blurRadius = 8f
            )
        )
    }

    val dateFormatter = remember {
        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    }

    CompositionLocalProvider(LocalHazeState provides haze) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val base = Color(0xFF0D0404).copy(alpha = 0.8f)
                    onDrawBehind {
                        drawRect(base)
                        drawRect(staticGradientBrush)
                        drawRect(secondaryStaticBrush)
                        if (introProgress < 1f) drawRect(introBrush, alpha = 1f - introProgress)
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
                            IconButton(onClick = { navController.navigate("PersonaSettings") }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
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
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 100.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.1f),
                                                    Color.Transparent
                                                ),
                                                radius = 70.dp.value
                                            ),
                                            CircleShape
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
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
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
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
                                onClick = { navController.navigate("EditUserStats") }
                            ) {
                                ProfileStatRow(
                                    label = "Age",
                                    value = userAge,
                                    icon = Icons.Default.Person
                                )
                                ProfileStatRow(
                                    label = "Height",
                                    value = "$userHeight cm",
                                    icon = Icons.Default.Height
                                )
                                ProfileStatRow(
                                    label = "Weight",
                                    value = "$userWeight kg",
                                    icon = Icons.Default.MonitorWeight
                                )
                                ProfileStatRow(
                                    label = "Experience",
                                    value = prefsManager.getExperience(),
                                    icon = Icons.Default.DataExploration
                                )
                            }
                        }
                    }
                    if (stages.after200ms && recentWorkouts.isNotEmpty()) {
                        item {
                            ProfileSectionCard(
                                title = "Recent Activity",
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                            ) {
                                recentWorkouts.forEach { workout ->
                                    val date = remember(workout.date) { dateFormatter.format(java.util.Date(workout.date)) }
                                    ProfileStatRow(
                                        label = workout.name,
                                        value = date,
                                        icon = Icons.Default.History
                                    )
                                }
                            }
                        }
                    }
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
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun ProfileSectionCard(
    title: String,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,


    ) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(24.dp)

    val baseDark = Color(0xFF1A1A1A).copy(alpha = 0.25f)
    val accentGlow = Color(0xFFFF3535)

    var isPressed by remember { mutableStateOf(false) }
    var rippleOffset by remember { mutableStateOf(Offset.Zero) }
    var showRipple by remember { mutableStateOf(false) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0.6f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 0.15f,
        animationSpec = tween(400),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        showRipple = true
                        rippleOffset = offset
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        scope.launch {
                            rippleRadius.snapTo(0f)
                            rippleRadius.animateTo(
                                size.width.toFloat(),
                                tween(400)
                            )
                        }
                        scope.launch {
                            rippleAlpha.snapTo(0.6f)
                            rippleAlpha.animateTo(0f, tween(400))
                        }

                        tryAwaitRelease()
                        isPressed = false
                        scope.launch {
                            delay(400)
                            showRipple = false
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable {
                    onClick()
                }
                .background(baseDark)

        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = accentGlow.copy(alpha = 0.3f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                }
            }
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            if (showRipple) {
                drawCircle(
                    color = accentGlow.copy(alpha = rippleAlpha.value),
                    radius = rippleRadius.value,
                    center = rippleOffset
                )
            }
        }
    }
}

/**
 * The ProfileStatRow does not need any changes. It will be rendered
 * on top of the shader effect from its parent ProfileSectionCard.
 */
@Composable
private fun ProfileStatRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
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
                maxLines = 1
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
