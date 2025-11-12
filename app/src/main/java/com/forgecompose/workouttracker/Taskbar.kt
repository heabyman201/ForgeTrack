package com.forgecompose.workouttracker

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FloatingTaskbar(
    modifier: Modifier = Modifier,
    navController: NavController,
    cornerRadius: Dp,
    iconAlpha: Float,
    uiState: WorkoutListUiState
) {
    val isWorkoutInProgress = ConnectedWorkout.currentMode.value != ConnectedWorkout.WorkoutMode.INACTIVE
    val ctx = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.current.collectAsState(initial = PerformanceOptions.Defaults)
    val animationsEnabled = performanceOptions.taskbarAnimations
    val glowAlpha = 1f
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(
            targetState = isWorkoutInProgress,
            label = "TaskbarState",
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) togetherWith
                            fadeOut(tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
                } else {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            }
        ) { workoutIsActive ->
            if (workoutIsActive) {
                val density = LocalDensity.current
                val haptics = LocalHapticFeedback.current
                val expand = remember { Animatable(0f) }
                var expanding by remember { mutableStateOf(false) }
                val pressSource = remember { MutableInteractionSource() }
                val isPressed by pressSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed && !expanding) 0.97f else 1f,
                    animationSpec = if (animationsEnabled) tween(350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0),
                    label = "pressScale"
                )
                LaunchedEffect(expanding) {
                    if (expanding) {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        if (animationsEnabled) expand.animateTo(0.65f, tween(220, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) else expand.snapTo(0.65f)
                        ctx.startActivity(Intent(ctx, NotificationDispatcherActivity::class.java))
                        if (animationsEnabled) expand.animateTo(1f, tween(700, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) else expand.snapTo(1f)
                        expand.snapTo(0f)
                        expanding = false
                    }
                }
                val t = expand.value
                val heightAnim = 80.dp * (1f - t) + 260.dp * t
                val radiusAnim = cornerRadius * (1f - 0.6f * t)
                val containerShape = RoundedCornerShape(radiusAnim)
                val cornerRpx = with(density) { radiusAnim.toPx() }
                var neonPhase by remember { mutableStateOf(0f) }
                if (animationsEnabled) {
                    LaunchedEffect(Unit) {
                        val frameMs = 16L
                        val dur = 3200f
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
                            .clickable(interactionSource = pressSource, indication = null) { if (!expanding) expanding = true }
                            .graphicsLayer {
                                val baseScaleX = 1f + 0.04f * t
                                val baseScaleY = 1f + 0.18f * t
                                scaleX = baseScaleX * pressScale
                                scaleY = baseScaleY * pressScale
                            }
                            .drawWithCache {
                                val bg = Brush.radialGradient(
                                    listOf(Color(0xFF1A0A0A), Color(0xFF0B0505)),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * (0.92f + 0.22f * t)
                                )
                                val sweepX = size.width * (neonPhase * 2f - 0.5f)
                                val start = Offset(sweepX, 0f)
                                val end = Offset(sweepX + size.width * 0.6f, size.height)
                                val core = Brush.linearGradient(
                                    listOf(Color(0xFFFF3B30).copy(alpha = 0f), Color(0xFFFF3B30), Color(0xFFFF3B30).copy(alpha = 0f)),
                                    start = start,
                                    end = end
                                )
                                val outer = Brush.linearGradient(
                                    listOf(Color(0xFFFF3B30).copy(alpha = 0f), Color(0xFFFF3B30).copy(alpha = 0.28f), Color(0xFFFF3B30).copy(alpha = 0f)),
                                    start = start,
                                    end = end
                                )
                                val border = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
                                onDrawBehind {
                                    drawRoundRect(brush = bg, cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                                    drawRoundRect(brush = border, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                                    drawRoundRect(brush = outer, style = Stroke(width = 5.dp.toPx() * (1f + 0.5f * t)), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                                    drawRoundRect(brush = core, style = Stroke(width = 2.dp.toPx() * (1f + 0.7f * t)), cornerRadius = CornerRadius(cornerRpx, cornerRpx))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = "Return to Workout",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(38.dp * (1f + 0.1f * t))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Return to Workout",
                                color = Color.White,
                                fontSize = 24.sp * (1f + 0.06f * t),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    AnimatedVisibility(
                        visible = baseIsVisible && !isDismissedByUser,
                        enter = if (animationsEnabled)
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(1900, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) + fadeIn(tween(400))
                        else fadeIn(tween(0)),
                        exit = if (animationsEnabled) fadeOut(tween(450, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))) else fadeOut(tween(0))
                    ) {
                        val items = remember {
                            listOf(
                                "HomeScreen" to Icons.Filled.Home,
                                "WorkoutSelector" to Icons.Filled.AddCircle,
                                "MuscleGroup" to Icons.Filled.FitnessCenter,
                                "UserProfile" to Icons.Filled.Person
                            )
                        }
                        val containerShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
                        val accent = remember { Color(0xFFFF5858) }
                        val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                        val squish = remember { Animatable(1f) }
                        val skew = remember { Animatable(0f) }
                        val sharedInteraction = remember { MutableInteractionSource() }
                        val isPressed by sharedInteraction.collectIsPressedAsState()
                        val routeOrder = remember { listOf("WorkoutHistory", "HomeScreen", "MuscleGroup", "UserProfile") }
                        fun routeIndex(r: String?) = routeOrder.indexOf(r).let { if (it >= 0) it else 1 }
                        var prevRoute by remember { mutableStateOf(currentRoute) }
                        LaunchedEffect(currentRoute) {
                            if (animationsEnabled) {
                                val from = routeIndex(prevRoute)
                                val to = routeIndex(currentRoute)
                                val dir = (to - from).coerceIn(-1, 1)
                                val kick = 14f * dir
                                val spin = 0.05f * dir
                                pos.snapTo(Offset(-kick, 0f))
                                skew.snapTo(spin)
                                squish.snapTo(1.04f)
                                launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow), initialVelocity = Offset(450f * dir, 0f)) }
                                launch { skew.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow)) }
                                launch { squish.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow)) }
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
                            if (animationsEnabled) animationScope.launch { pressedProgress.animateTo(target, spring(0.7f, Spring.StiffnessVeryLow, 0.001f)) } else pressedProgress.snapTo(target)
                        }
                        var boxSize by remember { mutableStateOf(IntSize.Zero) }
                        val density2 = LocalDensity.current
                        val cornerRpx2 = with(density2) { cornerRadius.toPx() }
                        val borderColor = Color(0xFF712424)
                        var dragPreviewIndex by remember { mutableStateOf<Int?>(null) }
                        var dragProgress by remember { mutableStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                                    offsetY.animateTo(boxSize.height * 1.5f, if (animationsEnabled) tween(600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0))
                                                    isDismissedByUser = true
                                                    offsetY.snapTo(0f)
                                                } else {
                                                    offsetY.animateTo(0f, if (animationsEnabled) spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow) else tween(0))
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
                                    .indication(sharedInteraction, null)
                                    .graphicsLayer {
                                        translationX = pos.value.x
                                        translationY = pos.value.y
                                        val s = squish.value
                                        val baseScaleX = 1f + (s - 1f) * 1.1f
                                        val baseScaleY = 1f - (s - 1f) * 0.55f
                                        val p = pressedProgress.value
                                        val pressScale = lerp(1f, 1.06f, p)
                                        scaleX = baseScaleX * pressScale
                                        scaleY = baseScaleY * pressScale
                                        rotationZ = skew.value * 5f
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        clip = true
                                        shadowElevation = 0f
                                    }
                                    .drawWithCache {
                                        val bg = Brush.radialGradient(
                                            listOf(Color(0xFF1A0A0A), Color(0xFF0B0505)),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.minDimension * 0.94f
                                        )
                                        val border = Brush.linearGradient(listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF)))
                                        onDrawBehind {
                                            drawRoundRect(brush = bg, cornerRadius = CornerRadius(cornerRpx2, cornerRpx2))
                                            drawRoundRect(brush = border, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx2, cornerRpx2))
                                        }
                                    }
                            ) {
                                val pillRadius = 28.dp
                                Row(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(containerShape)
                                        .border(width = 0.5.dp, color = borderColor, shape = RoundedCornerShape(32.dp))
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val order = listOf("HomeScreen", "WorkoutSelector", "MuscleGroup", "UserProfile")
                                    items.forEachIndexed { index, (route, icon) ->
                                        val selected = currentRoute == route
                                        val glowTarget by animateFloatAsState(targetValue = if (selected) 1f else 0f, animationSpec = if (animationsEnabled) tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0), label = "")
                                        val baseScale by animateFloatAsState(targetValue = if (selected) 1.055f else 1f, animationSpec = if (animationsEnabled) tween(500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0), label = "")
                                        val pressSquish by animateFloatAsState(targetValue = if (selected && isPressed) 0.99f else 1f, animationSpec = if (animationsEnabled) tween(450, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)) else tween(0), label = "")
                                        val burst = remember(route) { Animatable(0f) }
                                        val densityLocal = LocalDensity.current
                                        val dragThresholdPx = with(densityLocal) { 48.dp.toPx() }
                                        var dragAccum by remember(route) { mutableStateOf(0f) }
                                        val isTargetPreview = dragPreviewIndex == index
                                        val isCurrentSelectedPreviewing = selected && dragPreviewIndex != null
                                        val extraScaleTarget = if (isTargetPreview) 1f + 0.22f * dragProgress else 1f
                                        val extraScaleSelected = if (isCurrentSelectedPreviewing && selected) 1f + 0.1f * dragProgress else 1f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 6.dp)
                                                .graphicsLayer {
                                                    val burstScale = 1f + 0.14f * burst.value
                                                    val s = (baseScale * pressSquish) * burstScale * extraScaleTarget * extraScaleSelected
                                                    scaleX = s
                                                    scaleY = (baseScale / pressSquish) * burstScale * extraScaleTarget * extraScaleSelected
                                                }
                                                .clip(RoundedCornerShape(pillRadius))
                                                .pointerInput(selected) {
                                                    val densityLocal = this

                                                    val dragThresholdPx = with(densityLocal) { 32.dp.toPx() }

                                                    if (selected) {
                                                        detectDragGestures(
                                                            onDragEnd = {

                                                                dragAccum = 0f
                                                                dragPreviewIndex = null
                                                                dragProgress = 0f
                                                            }
                                                        ) { change, dragAmount ->
                                                            change.consume()
                                                            dragAccum += dragAmount.x

                                                            val absAccum = kotlin.math.abs(dragAccum)


                                                            var progress = (absAccum / dragThresholdPx).coerceIn(0f, 1f)
                                                            progress = progress * progress

                                                            val dir = dragAccum.sign.toInt().coerceIn(-1, 1)


                                                            val idx = order.indexOf(route).let { if (it < 0) 0 else it }
                                                            val next = (idx + dir).coerceIn(0, order.lastIndex)

                                                            if (dir != 0 && next != idx) {

                                                                dragPreviewIndex = next
                                                                dragProgress = progress
                                                            } else {

                                                                dragPreviewIndex = null
                                                                dragProgress = 0f
                                                            }

                                                            if (absAccum > dragThresholdPx) {

                                                                val nextRoute = order[next]
                                                                if (nextRoute != route) {
                                                                    navController.navigate(nextRoute) {

                                                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                        launchSingleTop = true
                                                                        restoreState = true
                                                                    }

                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                }

                                                                dragAccum = 0f
                                                                dragPreviewIndex = null
                                                                dragProgress = 0f
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
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (animationsEnabled) {
                                                        scope.launch {
                                                            burst.snapTo(1f)
                                                            burst.animateTo(0f, tween(1200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
                                                        }
                                                        scope.launch {
                                                            val tapNudge = if (route.hashCode() % 2 == 0) 6f else -6f
                                                            pos.snapTo(Offset(tapNudge, 0f))
                                                            squish.snapTo(1.03f)
                                                            skew.snapTo(if (tapNudge >= 0f) 0.05f else -0.05f)
                                                            launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow)) }
                                                            launch { squish.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow)) }
                                                            launch { skew.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow)) }
                                                        }
                                                    }
                                                }
                                                .background(
                                                    if (selected)
                                                        Brush.verticalGradient(
                                                            0f to Color(0xFF210909).copy(alpha = 0.65f),
                                                            1f to Color(0xFF240A0A).copy(alpha = 0.65f)
                                                        )
                                                    else Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent)
                                                )
                                                .border(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    brush = if (selected) Brush.linearGradient(listOf(Color(0xFF742525), Color(0xFF6A1E1E))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                                    shape = RoundedCornerShape(pillRadius)
                                                )
                                                .size(buttonSize),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .drawWithCache {
                                                        val r = size.minDimension / 2f
                                                        val glow = Brush.radialGradient(
                                                            listOf(
                                                                accent.copy(alpha = 0.5f * glowAlpha),
                                                                accent.copy(alpha = 0.18f * glowAlpha),
                                                                Color.Transparent
                                                            ),
                                                            center = Offset(size.width / 2f, size.height / 2f),
                                                            radius = r * 1.3f
                                                        )
                                                        onDrawBehind {
                                                            if (glowAlpha > 0f && selected) {
                                                                drawRoundRect(brush = glow, topLeft = Offset.Zero, size = size, cornerRadius = CornerRadius(r, r))
                                                            }
                                                        }
                                                    }
                                            )
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) accent else Color.White.copy(alpha = iconAlpha * 0.92f),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = baseIsVisible && isDismissedByUser && currentRoute == "HomeScreen",
                        enter = if (animationsEnabled) fadeIn(tween(300, delayMillis = 250)) + scaleIn(tween(650, delayMillis = 250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) else fadeIn(tween(0)),
                        exit = if (animationsEnabled) fadeOut(tween(350)) + scaleOut(tween(350)) else fadeOut(tween(0))
                    ) {
                        Box(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .imePadding()
                                .fillMaxWidth()
                                .height(80.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -5) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF1A0A0A), Color(0xFF0B0505))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))),
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
                                .height(60.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -5) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isDismissedByUser = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

