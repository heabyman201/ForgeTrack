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
    val label: String = "",
    val id: String = route
)

private class TaskbarHaptics(
    context: Context,
    private val fallback: HapticFeedback
) {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
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
                val radiusAnim = cornerRadius * (1f - 0.6f * t)
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
                            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                val buttonSize = 60.dp
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val haptic = LocalHapticFeedback.current
                val taskbarHaptics = rememberTaskbarHaptics(ctx, haptic)

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    AnimatedVisibility(
                        visible = baseIsVisible && !isDismissedByUser,
                        enter = if (animationsEnabled)
                            slideInVertically(initialOffsetY = { it }, animationSpec = spring(0.8f, Spring.StiffnessLow)) + fadeIn(tween(400))
                        else fadeIn(tween(0)),
                        exit = if (animationsEnabled) fadeOut(tween(450, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))) else fadeOut(tween(0))
                    ) {
                        val prefs = remember { ctx.getSharedPreferences("taskbar_prefs", Context.MODE_PRIVATE) }
                        val allItemsMap = remember {
                            mapOf(
                                "HomeScreen" to (Icons.Filled.Home to "Home"),
                                "WorkoutSelector" to (Icons.Filled.AddCircle to "Start"),
                                "MuscleGroup" to (Icons.Filled.FitnessCenter to "Muscles"),
                                "UserProfile" to (Icons.Filled.Person to "Profile")
                            )
                        }

                        val items = remember { mutableStateListOf<TaskbarItem>() }

                        LaunchedEffect(Unit) {
                            val savedOrder = prefs.getString("order", null)
                            if (savedOrder != null) {
                                val keys = savedOrder.split(",")
                                val orderedItems = keys.mapNotNull { key ->
                                    allItemsMap[key]?.let { (icon, label) -> TaskbarItem(key, icon, label) }
                                }
                                if (orderedItems.size == 4) {
                                    items.clear()
                                    items.addAll(orderedItems)
                                } else {
                                    items.clear()
                                    allItemsMap.forEach { (k, v) -> items.add(TaskbarItem(k, v.first, v.second)) }
                                }
                            } else {
                                items.clear()
                                allItemsMap.forEach { (k, v) -> items.add(TaskbarItem(k, v.first, v.second)) }
                            }
                        }

                        val effectiveCornerRadius = maxOf(cornerRadius, 32.dp)
                        val containerShape = remember(effectiveCornerRadius) { RoundedCornerShape(effectiveCornerRadius) }
                        val accent = primaryColor
                        val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                        val squish = remember { Animatable(1f) }
                        val skew = remember { Animatable(0f) }
                        val sharedInteraction = remember { MutableInteractionSource() }
                        val isPressed by sharedInteraction.collectIsPressedAsState()

                        val routeOrder = items.map { it.route }

                        fun routeIndex(r: String?) = routeOrder.indexOf(r).let { if (it >= 0) it else 1 }
                        var prevRoute by remember { mutableStateOf(currentRoute) }
                        LaunchedEffect(currentRoute) {
                            if (animationsEnabled) {
                                val from = routeIndex(prevRoute)
                                val to = routeIndex(currentRoute)
                                val dir = (to - from).coerceIn(-1, 1)


                                val velocity = 480f * dir
                                val tiltForce = 0.035f * dir
                                val kickBack = 14f * dir


                                pos.snapTo(Offset(-kickBack, 0f))
                                skew.snapTo(tiltForce)
                                squish.snapTo(0.975f)

                                launch {
                                    pos.animateTo(
                                        targetValue = Offset.Zero,

                                        animationSpec = spring(
                                            dampingRatio = 0.92f,
                                            stiffness = 220f
                                        ),
                                        initialVelocity = Offset(velocity, 0f)
                                    )
                                }
                                launch {
                                    skew.animateTo(
                                        targetValue = 0f,

                                        animationSpec = spring(
                                            dampingRatio = 0.92f,
                                            stiffness = 240f
                                        )
                                    )
                                }
                                launch {
                                    squish.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.9f,
                                            stiffness = 260f
                                        )
                                    )
                                }
                            } else {
                                pos.snapTo(Offset.Zero)
                                skew.snapTo(0f)
                                squish.snapTo(1f)
                            }
                            prevRoute = currentRoute
                        }
                        val animationScope = rememberCoroutineScope()
                        val pressedProgress = remember { Animatable(0f) }
                        LaunchedEffect(isPressed) {
                            val target = if (isPressed) 1f else 0f
                            if (animationsEnabled) animationScope.launch { pressedProgress.animateTo(target, spring(0.9f, Spring.StiffnessLow)) } else pressedProgress.snapTo(target)
                        }
                        var boxSize by remember { mutableStateOf(IntSize.Zero) }
                        val density2 = LocalDensity.current
                        val cornerRpx2 = with(density2) { effectiveCornerRadius.toPx() }

                        val borderColor = Color.White.copy(alpha = 0.08f)

                        var dragPreviewIndex by remember { mutableStateOf<Int?>(null) }
                        var dragProgress by remember { mutableStateOf(0f) }

                        var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
                        var draggingItemOffset by remember { mutableStateOf(0f) }

                        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                        val jiggleRotation by infiniteTransition.animateFloat(
                            initialValue = -1.1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(180, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "rotation"
                        )

                        val isReordering = draggingItemIndex != null
                        val containerBorderColor by animateColorAsState(
                            if (isReordering) primaryColor else Color(0x26FFFFFF),
                            label = "border"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                                .imePadding()
                                .fillMaxWidth()
                                .height(80.dp)
                                .onGloballyPositioned { boxSize = it.size }
                                .graphicsLayer { translationY = offsetY.value }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            scope.launch {
                                                val threshold = boxSize.height * 0.6f
                                                if (offsetY.value > threshold) {
                                                    offsetY.animateTo(boxSize.height * 1.5f, if (animationsEnabled) spring(0.85f, Spring.StiffnessLow) else tween(0))
                                                    isDismissedByUser = true
                                                    offsetY.snapTo(0f)
                                                } else {
                                                    offsetY.animateTo(0f, if (animationsEnabled) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow) else tween(0))
                                                }
                                            }
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        scope.launch { offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f)) }
                                    }
                                }
                        ) {
                            Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(containerShape)
                                .background(
                                    colorLerp(
                                        backgroundColor,
                                        Color.Black,
                                        0.32f
                                    )
                                )
                                .then(if (taskbarBlurRadius > 0.dp) Modifier.blur(taskbarBlurRadius) else Modifier)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(containerShape)
                                    .indication(sharedInteraction, null)
                                    .graphicsLayer {
                                        translationX = pos.value.x
                                        translationY = pos.value.y
                                        val s = squish.value
                                        val baseScaleX = 1f + (s - 1f) * 1.2f
                                        val baseScaleY = 1f - (s - 1f) * 0.7f
                                        val p = pressedProgress.value
                                        val pressScale = lerp(1f, 0.98f, p)
                                        scaleX = baseScaleX * pressScale
                                        scaleY = baseScaleY * pressScale
                                        rotationZ = skew.value * 6f
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        clip = true
                                        shadowElevation = 0f
                                    }
                                    .drawWithCache {
                                        val bg = Brush.verticalGradient(
                                            listOf(
                                                colorLerp(backgroundColor, Color.Black, 0.18f),
                                                colorLerp(backgroundColor, Color.Black, 0.38f)
                                            )
                                        )
                                        val accentWash = Brush.horizontalGradient(
                                            listOf(
                                                secondaryColor.copy(alpha = if (isReordering) 0.08f else 0.03f),
                                                primaryColor.copy(alpha = if (isReordering) 0.06f else 0.02f),
                                                Color.Transparent
                                            )
                                        )
                                        val border = Brush.verticalGradient(
                                            listOf(
                                                containerBorderColor.copy(alpha = if (isReordering) 0.55f else 0.20f),
                                                Color.White.copy(alpha = 0.03f)
                                            )
                                        )
                                        onDrawBehind {
                                            drawRoundRect(brush = bg, cornerRadius = CornerRadius(cornerRpx2, cornerRpx2))
                                            drawRoundRect(brush = accentWash, cornerRadius = CornerRadius(cornerRpx2, cornerRpx2))
                                            drawRoundRect(brush = border, style = Stroke(width = if(isReordering) 2.dp.toPx() else 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx2, cornerRpx2))
                                        }
                                    }
                            ) {
                                val pillRadius = 999.dp
                                Row(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(containerShape)
                                        .border(width = 1.dp, color = Color(0xFF3D1E00).copy(alpha = 0.7f), shape = RoundedCornerShape(999.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items.forEachIndexed { index, item ->
                                        key(item.id) {
                                            val route = item.route
                                            val icon = item.icon
                                            val selected = currentRoute == route
                                            val glowTarget by animateFloatAsState(targetValue = if (selected) 1f else 0f, animationSpec = if (animationsEnabled) tween(260, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0), label = "")
                                            val baseScale by animateFloatAsState(targetValue = if (selected) 1.025f else 1f, animationSpec = if (animationsEnabled) spring(0.88f, Spring.StiffnessLow) else tween(0), label = "")
                                            val pressSquish by animateFloatAsState(targetValue = if (selected && isPressed) 0.985f else 1f, animationSpec = if (animationsEnabled) spring(0.85f, Spring.StiffnessMedium) else tween(0), label = "")
                                            val burst = remember(route) { Animatable(0f) }

                                            val isTargetPreview = dragPreviewIndex == index
                                            val isCurrentSelectedPreviewing = selected && dragPreviewIndex != null
                                            val extraScaleTarget = if (isTargetPreview) 1f + 0.22f * dragProgress else 1f
                                            val extraScaleSelected = if (isCurrentSelectedPreviewing && selected) 1f + 0.1f * dragProgress else 1f

                                            val isBeingDragged = draggingItemIndex == index
                                            val reorderScale by animateFloatAsState(if (isBeingDragged) 1.12f else 1f, label = "reorderScale")
                                            val reorderOffset = if (isBeingDragged) draggingItemOffset else 0f

                                            val rotation = if (isReordering && !isBeingDragged) jiggleRotation else 0f

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 2.dp)
                                                    .zIndex(if (isBeingDragged) 10f else 0f)
                                                    .offset { IntOffset(reorderOffset.roundToInt(), 0) }
                                                    .graphicsLayer {
                                                        val burstScale = 1f + 0.07f * burst.value
                                                        val s = (baseScale * pressSquish) * burstScale * extraScaleTarget * extraScaleSelected * reorderScale
                                                        scaleX = s
                                                        scaleY = (baseScale / pressSquish) * burstScale * extraScaleTarget * extraScaleSelected * reorderScale
                                                        shadowElevation = if(isBeingDragged) 12.dp.toPx() else 0f
                                                        rotationZ = rotation
                                                    }
                                                    .pointerInput(Unit) {
                                                        val densityLocal = this
                                                        val reorderThreshold = with(densityLocal) { 40.dp.toPx() }

                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                draggingItemIndex = index
                                                                taskbarHaptics.reorderStart()
                                                            },
                                                            onDragEnd = {
                                                                draggingItemIndex = null
                                                                draggingItemOffset = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggingItemIndex = null
                                                                draggingItemOffset = 0f
                                                            }
                                                        ) { change, dragAmount ->
                                                            change.consume()
                                                            draggingItemOffset += dragAmount.x

                                                            val currentOffset = draggingItemOffset
                                                            val direction = if (currentOffset > 0) 1 else -1

                                                            if (kotlin.math.abs(currentOffset) > reorderThreshold) {
                                                                val nextIndex = index + direction
                                                                if (nextIndex in items.indices) {
                                                                    val itemToMove = items[index]
                                                                    items.removeAt(index)
                                                                    items.add(nextIndex, itemToMove)

                                                                    val newOrder = items.joinToString(",") { it.route }
                                                                    prefs.edit().putString("order", newOrder).apply()

                                                                    draggingItemIndex = nextIndex
                                                                    draggingItemOffset = 0f
                                                                    taskbarHaptics.reorderStep()
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .clickable(indication = null, interactionSource = sharedInteraction) {
                                                        if (selected) return@clickable
                                                        navController.navigate(route) {
                                                            popUpTo(navController.graph.startDestinationId)
                                                            launchSingleTop = true
                                                        }
                                                        taskbarHaptics.navTap(route)
                                                        if (animationsEnabled) {
                                                            scope.launch {
                                                                burst.snapTo(1f)
                                                                burst.animateTo(0f, spring(0.88f, Spring.StiffnessMedium))
                                                            }
                                                            scope.launch {
                                                                val tapNudge = if (route.hashCode() % 2 == 0) 3f else -3f
                                                                pos.snapTo(Offset(tapNudge, 0f))
                                                                squish.snapTo(0.992f)
                                                                skew.snapTo(if (tapNudge >= 0f) 0.012f else -0.012f)
                                                                launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)) }
                                                                launch { squish.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)) }
                                                                launch { skew.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)) }
                                                            }
                                                        }
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(buttonSize)
                                                        .clip(RoundedCornerShape(pillRadius))
                                                        .background(
                                                            if (selected)
                                                                Color(0xFF1E0C00)
                                                            else Color.Transparent
                                                        )
                                                        .border(
                                                            width = if (selected) 1.5.dp else 0.dp,
                                                            brush = if (selected) {
                                                                Brush.verticalGradient(
                                                                    listOf(
                                                                        primaryColor.copy(alpha = 0.55f),
                                                                        Color(0xFF5C2A00).copy(alpha = 0.40f)
                                                                    )
                                                                )
                                                            } else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                                            shape = RoundedCornerShape(pillRadius)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = if (selected) primaryColor else Color(0xFF7A7A7A),
                                                            modifier = Modifier.size(if (selected) 26.dp else 22.dp)
                                                        )
                                                        if (selected) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = item.label,
                                                                fontSize = 9.sp,
                                                                color = primaryColor,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    }
                                                }
                                                if (!selected) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.label,
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF7A7A7A),
                                                        fontWeight = FontWeight.Normal,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = baseIsVisible && isDismissedByUser && currentRoute == "HomeScreen",
                        enter = if (animationsEnabled) fadeIn(tween(300, delayMillis = 250)) + scaleIn(spring(0.8f, Spring.StiffnessLow)) else fadeIn(tween(0)),
                        exit = if (animationsEnabled) fadeOut(tween(350)) + scaleOut(tween(350)) else fadeOut(tween(0))
                    ) {
                        Box(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .imePadding()
                                .fillMaxWidth()
                                .height(64.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -5) {
                                            taskbarHaptics.restoreTaskbar()
                                            isDismissedByUser = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    if (workoutPresets.isNotEmpty()) {
                                        val suggestedPreset = workoutPresets.random()
                                        workout.value = suggestedPreset.name
                                        ctx.startActivity(Intent(ctx, WorkoutActivity::class.java))
                                        taskbarHaptics.randomWorkoutLaunch()
                                        isDismissedByUser = false
                                    }
                                },
                                text = { Text("Random Workout") },
                                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "Suggest Workout") },
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                                modifier = Modifier
                                    .clip(FloatingActionButtonDefaults.extendedFabShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.10f),
                                                secondaryColor.copy(alpha = 0.16f),
                                                backgroundColor.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))),
                                        shape = FloatingActionButtonDefaults.extendedFabShape
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = baseIsVisible && isDismissedByUser && currentRoute != "HomeScreen",
                        enter = if (animationsEnabled) fadeIn(tween(700, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) else fadeIn(tween(0)),
                        exit = if (animationsEnabled) fadeOut(tween(400, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))) else fadeOut(tween(0))
                    ) {
                        Box(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .imePadding()
                                .fillMaxWidth()
                                .height(52.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -5) {
                                            taskbarHaptics.restoreTaskbar()
                                            isDismissedByUser = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.24f),
                                                Color.White.copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.75.dp,
                                        color = Color.White.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
