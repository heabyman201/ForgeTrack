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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sign

data class TaskbarItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val id: String = route
)

private fun taskbarLabelForRoute(route: String): String = when (route) {
    "HomeScreen" -> "Home"
    "WorkoutSelector" -> "Plan"
    "MuscleGroup" -> "Muscles"
    "UserProfile" -> "Profile"
    else -> route
}

@Composable
private fun NativeTaskbar(
    navController: NavController,
    currentRoute: String?,
    backgroundColor: Color,
    primaryColor: Color,
    iconAlpha: Float,
    taskbarCornerRadius: Dp,
    taskbarHaptics: TaskbarHaptics
) {
    val containerShape = remember(taskbarCornerRadius) { RoundedCornerShape(taskbarCornerRadius) }
    val items = remember {
        listOf(
            TaskbarItem("HomeScreen", Icons.Filled.Home, taskbarLabelForRoute("HomeScreen")),
            TaskbarItem("WorkoutSelector", Icons.Filled.AddCircle, taskbarLabelForRoute("WorkoutSelector")),
            TaskbarItem("MuscleGroup", Icons.Filled.FitnessCenter, taskbarLabelForRoute("MuscleGroup")),
            TaskbarItem("UserProfile", Icons.Filled.Person, taskbarLabelForRoute("UserProfile"))
        )
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .navigationBarsPadding()
            .imePadding()
            .fillMaxWidth()
            .clip(containerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorLerp(backgroundColor, Color.Black, 0.12f),
                        colorLerp(backgroundColor, Color.Black, 0.26f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = containerShape
            )
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        taskbarHaptics.navTap(item.route)
                        if (selected) return@NavigationBarItem
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = iconAlpha * 0.82f),
                        unselectedTextColor = Color.White.copy(alpha = 0.62f),
                        indicatorColor = primaryColor.copy(alpha = 0.22f)
                    )
                )
            }
        }
    }
}

private class TaskbarHaptics(
    context: Context,
    private val fallback: HapticFeedback
) {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var lastNavAt = 0L
    private var lastReorderTickAt = 0L
    private var lastRestoreAt = 0L

    private fun performFallback(type: HapticFeedbackType) {
        runCatching { fallback.performHapticFeedback(type) }
    }

    private fun vibratePredefined(effectId: Int, fallbackType: HapticFeedbackType) {
        val localVibrator = vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && localVibrator?.hasVibrator() == true) {
            localVibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            performFallback(fallbackType)
        }
    }

    private fun vibrateWaveform(
        timings: LongArray,
        amplitudes: IntArray,
        fallbackType: HapticFeedbackType
    ) {
        val localVibrator = vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && localVibrator?.hasVibrator() == true) {
            localVibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            performFallback(fallbackType)
        }
    }

    fun stopWarning() {
        performFallback(HapticFeedbackType.Reject)
        vibrateWaveform(
            timings = longArrayOf(0, 12, 28, 18),
            amplitudes = intArrayOf(0, 120, 0, 190),
            fallbackType = HapticFeedbackType.LongPress
        )
    }

    fun stopConfirm() {
        performFallback(HapticFeedbackType.Confirm)
        vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK, HapticFeedbackType.Confirm)
    }

    fun stopCancel() {
        performFallback(HapticFeedbackType.GestureEnd)
    }

    fun navTap(route: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavAt < 80L) return
        lastNavAt = now

        performFallback(HapticFeedbackType.ContextClick)
        when ((route.hashCode() and Int.MAX_VALUE) % 3) {
            0 -> vibrateWaveform(
                timings = longArrayOf(0, 8, 18, 10),
                amplitudes = intArrayOf(0, 70, 0, 105),
                fallbackType = HapticFeedbackType.ContextClick
            )
            1 -> vibrateWaveform(
                timings = longArrayOf(0, 10, 22, 8),
                amplitudes = intArrayOf(0, 85, 0, 75),
                fallbackType = HapticFeedbackType.ContextClick
            )
            else -> vibratePredefined(
                VibrationEffect.EFFECT_CLICK,
                HapticFeedbackType.ContextClick
            )
        }
    }

    fun reorderStart() {
        performFallback(HapticFeedbackType.GestureThresholdActivate)
        vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK, HapticFeedbackType.LongPress)
    }

    fun reorderStep() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastReorderTickAt < 55L) return
        lastReorderTickAt = now
        performFallback(HapticFeedbackType.TextHandleMove)
        vibratePredefined(VibrationEffect.EFFECT_TICK, HapticFeedbackType.TextHandleMove)
    }

    fun restoreTaskbar() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRestoreAt < 140L) return
        lastRestoreAt = now
        performFallback(HapticFeedbackType.ToggleOn)
        vibrateWaveform(
            timings = longArrayOf(0, 8, 20, 12),
            amplitudes = intArrayOf(0, 80, 0, 125),
            fallbackType = HapticFeedbackType.ToggleOn
        )
    }

    fun randomWorkoutLaunch() {
        performFallback(HapticFeedbackType.Confirm)
        vibrateWaveform(
            timings = longArrayOf(0, 10, 24, 16),
            amplitudes = intArrayOf(0, 90, 0, 160),
            fallbackType = HapticFeedbackType.Confirm
        )
    }
}

@Composable
private fun rememberTaskbarHaptics(
    context: Context,
    fallback: HapticFeedback
): TaskbarHaptics {
    return remember(context, fallback) { TaskbarHaptics(context, fallback) }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FloatingTaskbar(
    modifier: Modifier = Modifier,
    navController: NavController,
    cornerRadius: Dp,
    iconAlpha: Float,
    uiState: WorkoutListUiState
) {
    val ctx = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.current.collectAsState(initial = PerformanceOptions.Defaults)

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(ctx)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    val primaryColor = theme.primary
    val secondaryColor = theme.secondary
    val backgroundColor = theme.background
    val taskbarCornerRadius = if (cornerRadius > 18.dp) 18.dp else cornerRadius

    val animationsEnabled = performanceOptions.taskbarAnimations
    val taskbarBlurRadius = if (performanceOptions.blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 8.dp else 0.dp

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(
            targetState = false,
            label = "TaskbarState",
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(420, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) togetherWith
                            fadeOut(tween(380, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
                } else {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            }
        ) { workoutIsActive ->
            if (workoutIsActive) {
                val density = LocalDensity.current
                val haptics = LocalHapticFeedback.current
                val taskbarHaptics = rememberTaskbarHaptics(ctx, haptics)
                var showStopConfirm by remember { mutableStateOf(false) }
                val t = 0f
                val heightAnim = 80.dp * (1f - t) + 260.dp * t
                val radiusAnim = taskbarCornerRadius * (1f - 0.35f * t)
                val containerShape = RoundedCornerShape(radiusAnim)
                val cornerRpx = with(density) { radiusAnim.toPx() }
                var neonPhase by remember { mutableStateOf(0f) }
                if (animationsEnabled) {
                    LaunchedEffect(Unit) {
                        val frameMs = 26L
                        val dur = 5200f
                        while (true) {
                            neonPhase += frameMs / dur
                            if (neonPhase > 1f) neonPhase -= 1f
                            delay(frameMs)
                        }
                    }
                }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .navigationBarsPadding()
                            .imePadding()
                            .fillMaxWidth()
                            .height(heightAnim)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(containerShape)
                                .background(
                                    colorLerp(
                                        backgroundColor,
                                        Color.Black,
                                        0.28f
                                    )
                                )
                                .then(if (taskbarBlurRadius > 0.dp) Modifier.blur(taskbarBlurRadius) else Modifier)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(containerShape)
                                .graphicsLayer {}
                                .drawWithCache {
                                    val surface = Brush.verticalGradient(
                                        listOf(
                                            colorLerp(backgroundColor, Color.Black, 0.18f),
                                            colorLerp(backgroundColor, Color.Black, 0.34f)
                                        )
                                    )
                                    val accentWash = Brush.horizontalGradient(
                                        listOf(
                                            primaryColor.copy(alpha = 0.05f),
                                            secondaryColor.copy(alpha = 0.03f),
                                            Color.Transparent
                                        )
                                    )
                                    val sheenX = size.width * (0.15f + (neonPhase * 0.7f))
                                    val sheen = Brush.linearGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.04f),
                                            Color.Transparent
                                        ),
                                        start = Offset(sheenX - size.width * 0.22f, 0f),
                                        end = Offset(sheenX + size.width * 0.08f, size.height)
                                    )
                                    val border = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.12f),
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    )
                                    onDrawBehind {
                                        drawRoundRect(brush = surface, cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                                        drawRoundRect(
                                            brush = accentWash,
                                            cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                        )
                                        drawRoundRect(
                                            brush = sheen,
                                            cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                        )
                                        drawRoundRect(
                                            brush = border,
                                            style = Stroke(width = 1.dp.toPx()),
                                            cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                        )
                                    }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = "Workout Active",
                                tint = primaryColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Workout Active",
                                color = Color.White,
                                fontSize = 20.sp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                taskbarHaptics.stopWarning()
                                showStopConfirm = true
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF8E2323).copy(alpha = 0.88f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.matchParentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop Workout",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showStopConfirm) {
                        AlertDialog(
                            onDismissRequest = { showStopConfirm = false },
                            title = { Text("End workout?") },
                            text = { Text("This will instantly end the current workout and clear unsaved progress.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showStopConfirm = false
                                        taskbarHaptics.stopConfirm()
                                        ConnectedWorkout.forceEndSession(ctx)
                                    }
                                ) { Text("End") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showStopConfirm = false
                                    taskbarHaptics.stopCancel()
                                }) { Text("Cancel") }
                            }
                        )
                    }
                }
            } else {
                val baseIsVisible = uiState is WorkoutListUiState.Success && !taskbarOverride.shouldOverrideVisiblity.value
                var isDismissedByUser by remember { mutableStateOf(false) }
                val offsetY = remember { Animatable(0f) }
                val scope = rememberCoroutineScope()
                val buttonSize = 56.dp
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val haptic = LocalHapticFeedback.current
                val taskbarHaptics = rememberTaskbarHaptics(ctx, haptic)

                AnimatedVisibility(
                    visible = baseIsVisible,
                    enter = if (animationsEnabled)
                        slideInVertically(initialOffsetY = { it }, animationSpec = spring(0.8f, Spring.StiffnessLow)) + fadeIn(tween(320))
                    else fadeIn(tween(0)),
                    exit = if (animationsEnabled)
                        fadeOut(tween(220, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
                    else fadeOut(tween(0))
                ) {
                    NativeTaskbar(
                        navController = navController,
                        currentRoute = currentRoute,
                        backgroundColor = backgroundColor,
                        primaryColor = primaryColor,
                        iconAlpha = iconAlpha,
                        taskbarCornerRadius = taskbarCornerRadius,
                        taskbarHaptics = taskbarHaptics
                    )
                }
                return@AnimatedContent


                                }
        }
    }
}
