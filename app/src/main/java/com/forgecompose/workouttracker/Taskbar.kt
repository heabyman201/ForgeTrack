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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
    val glow = 1f

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(
            targetState = isWorkoutInProgress,
            label = "TaskbarState",
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
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
                    targetValue = if (isPressed && !expanding) 0.965f else 1f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "pressScale"
                )

                LaunchedEffect(expanding) {
                    if (expanding) {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        expand.animateTo(0.65f, tween(95, easing = FastOutSlowInEasing))
                        val intent = Intent(ctx, NotificationDispatcherActivity::class.java)
                        ctx.startActivity(intent)
                        expand.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                        expand.snapTo(0f)
                        expanding = false
                    }
                }

                val t = expand.value
                val heightAnim = 80.dp * (1f - t) + 260.dp * t
                val radiusAnim = cornerRadius * (1f - 0.6f * t)
                val containerShape = RoundedCornerShape(radiusAnim)
                val cornerRpx = with(density) { radiusAnim.toPx() }

                val neonPhase by rememberInfiniteTransition(label = "neonPhase")
                    .animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
                        label = "phase"
                    )

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
                            .clickable(
                                interactionSource = pressSource,
                                indication = null
                            ) {
                                if (!expanding) expanding = true
                            }
                            .graphicsLayer {
                                val baseScaleX = 1f + 0.04f * t
                                val baseScaleY = 1f + 0.18f * t
                                scaleX = baseScaleX * pressScale
                                scaleY = baseScaleY * pressScale
                            }
                            .drawWithCache {
                                val bgBrush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFF3A0E0E),
                                        Color(0xFF120707)
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * (0.95f + 0.25f * t)
                                )

                                val sweepX = size.width * (neonPhase * 2f - 0.5f)
                                val start = Offset(sweepX, 0f)
                                val end = Offset(sweepX + size.width * 0.6f, size.height)

                                val neonCore = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF3B30).copy(alpha = 0f),
                                        Color(0xFFFF3B30),
                                        Color(0xFFFF3B30).copy(alpha = 0f)
                                    ),
                                    start = start,
                                    end = end
                                )
                                val neonOuter = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF3B30).copy(alpha = 0f),
                                        Color(0xFFFF3B30).copy(alpha = 0.35f),
                                        Color(0xFFFF3B30).copy(alpha = 0f)
                                    ),
                                    start = start,
                                    end = end
                                )

                                onDrawBehind {
                                    drawRoundRect(
                                        brush = bgBrush,
                                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                    )
                                    drawRoundRect(
                                        brush = neonOuter,
                                        style = Stroke(width = 6.dp.toPx() * (1f + 0.6f * t)),
                                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                    )
                                    drawRoundRect(
                                        brush = neonCore,
                                        style = Stroke(width = 2.dp.toPx() * (1f + 0.8f * t)),
                                        cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                                fontSize = (24.sp * (1f + 0.06f * t)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                val baseIsVisible = uiState is WorkoutListUiState.Success &&
                        uiState.workouts.isNotEmpty() &&
                        !taskbarOverride.shouldOverrideVisiblity.value
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
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(1450, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(250))
                    ) {
                        val items = remember {
                            listOf(
                                "WorkoutHistory" to Icons.Filled.History,
                                "HomeScreen" to Icons.Filled.Home,
                                "MuscleGroup" to Icons.Filled.FitnessCenter,
                                "UserProfile" to Icons.Filled.Person
                            )
                        }

                        val containerShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
                        val accentGlow = remember { Color(0xFFFF3535) }

                        val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                        val squish = remember { Animatable(1f) }
                        val skew = remember { Animatable(0f) }

                        val sharedInteraction = remember { MutableInteractionSource() }
                        val isPressed by sharedInteraction.collectIsPressedAsState()

                        val am = remember(ctx) { ctx.getSystemService(android.app.ActivityManager::class.java) }
                        val lowSpec = remember { (am?.isLowRamDevice == true)  }
                        val idle = rememberInfiniteTransition(label = "idle")
                        val idleBob by idle.animateFloat(
                            initialValue = if (lowSpec) 0f else -0.6f, targetValue = if (lowSpec) 0f else 0.6f,
                            animationSpec = infiniteRepeatable(tween(if (lowSpec) 4200 else 3200, easing = LinearEasing)),
                            label = "idleBob"
                        )
                        val idleBreath by idle.animateFloat(
                            initialValue = 0f, targetValue = if (lowSpec) 0f else 1f,
                            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
                            label = "idleBreath"
                        )

                        val routeOrder = remember { listOf("WorkoutHistory", "HomeScreen", "MuscleGroup", "UserProfile") }
                        fun routeIndex(r: String?) = routeOrder.indexOf(r).let { if (it >= 0) it else 1 }
                        var prevRoute by remember { mutableStateOf(currentRoute) }

                        LaunchedEffect(currentRoute) {
                            val from = routeIndex(prevRoute)
                            val to = routeIndex(currentRoute)
                            val dir = (to - from).coerceIn(-1, 1)
                            val kick = 18f * dir
                            val spin = 0.07f * dir
                            pos.snapTo(Offset(-kick, 0f))
                            skew.snapTo(spin)
                            squish.snapTo(1.06f - 0.02f * abs(dir.toFloat()))
                            launch {
                                pos.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
                                    initialVelocity = Offset(650f * dir, 0f)
                                )
                            }
                            launch {
                                skew.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                                )
                            }
                            launch {
                                squish.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                                )
                            }
                            prevRoute = currentRoute
                        }

                        LaunchedEffect(isPressed) {
                            if (isPressed) squish.animateTo(1.035f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
                            else squish.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
                        }

                        var boxSize by remember { mutableStateOf(IntSize.Zero) }
                        val density2 = LocalDensity.current
                        val cornerRpx2 = with(density2) { cornerRadius.toPx() }

                        val borderShape = remember { RoundedCornerShape(32.dp) }
                        val borderColor = remember { Color.White.copy(alpha = 0.11f) }
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
                                                    offsetY.animateTo(
                                                        boxSize.height * 1.5f,
                                                        tween(250, easing = FastOutSlowInEasing)
                                                    )
                                                    isDismissedByUser = true
                                                    offsetY.snapTo(0f)
                                                } else {
                                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                                }
                                            }
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f))
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(containerShape)
                                    .indication(
                                        sharedInteraction,
                                        null
                                    )
                                    .graphicsLayer {
                                        translationX = pos.value.x
                                        translationY = pos.value.y + idleBob * 0.5f
                                        val breath = if (!isPressed && baseIsVisible && !isDismissedByUser) 1f + (if (lowSpec) 0f else (0.003f * kotlin.math.sin(idleBreath * (Math.PI * 2)).toFloat())) else 1f
                                        val s = squish.value
                                        scaleX = (1f + (s - 1f) * 1.1f) * breath
                                        scaleY = (1f - (s - 1f) * 0.55f) * (2f - breath)
                                        rotationZ = skew.value * 6f
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        clip = true
                                        shadowElevation = 0f
                                    }
                                    .drawWithCache {
                                        val bgBrush = Brush.radialGradient(
                                            listOf(
                                                Color(0xFF3A0E0E),
                                                Color(0xFF120707)
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.minDimension * 0.95f
                                        )
                                        val borderBrush = Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFF5555).copy(alpha = 0.6f),
                                                Color(0xFF8B0000).copy(alpha = 0.45f)
                                            )
                                        )
                                        onDrawBehind {
                                            drawRoundRect(
                                                brush = bgBrush,
                                                cornerRadius = CornerRadius(cornerRpx2, cornerRpx2)
                                            )
                                            drawRoundRect(
                                                brush = borderBrush,
                                                style = Stroke(width = 1.dp.toPx()),
                                                cornerRadius = CornerRadius(cornerRpx2, cornerRpx2)
                                            )
                                        }
                                    }
                            ) {
                                val borderShapeInner = RoundedCornerShape(32.dp)
                                Row(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(containerShape)
                                        .border(width = 0.5.dp, color = borderColor, shape = borderShapeInner)
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val routeOrder = listOf("WorkoutHistory", "HomeScreen", "MuscleGroup", "UserProfile")
                                    items.forEachIndexed { index, (route, icon) ->
                                        val selected = currentRoute == route
                                        val glowAlpha by animateFloatAsState(
                                            targetValue = if (selected) 1f else 0f,
                                            animationSpec = tween(150, easing = FastOutSlowInEasing),
                                            label = ""
                                        )
                                        val baseScale by animateFloatAsState(
                                            targetValue = if (selected) 1.055f else 1f,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                                            label = ""
                                        )
                                        val pressSquish by animateFloatAsState(
                                            targetValue = if (selected && isPressed) 0.985f else 1f,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                                            label = ""
                                        )
                                        val burst = remember(route) { Animatable(0f) }
                                        val densityLocal = LocalDensity.current
                                        val dragThresholdPx = with(densityLocal) { 36.dp.toPx() }
                                        var dragAccum by remember(route) { mutableStateOf(0f) }

                                        val isTargetPreview = dragPreviewIndex == index
                                        val isCurrentSelectedPreviewing = selected && dragPreviewIndex != null
                                        val extraScaleTarget = if (isTargetPreview) 1f + 0.18f * dragProgress else 1f
                                        val extraScaleSelected = if (isCurrentSelectedPreviewing && selected) 1f + 0.08f * dragProgress else 1f
                                        val extraScale = extraScaleTarget * extraScaleSelected

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 6.dp)
                                                .graphicsLayer {
                                                    val burstScale = 1f + 0.16f * burst.value
                                                    val s = (baseScale * pressSquish) * burstScale * extraScale
                                                    scaleX = s
                                                    scaleY = (baseScale / pressSquish) * burstScale * extraScale
                                                }
                                                .clip(RoundedCornerShape(32.dp))
                                                .pointerInput(selected) {
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
                                                            val absAccum = abs(dragAccum)
                                                            val progress = (absAccum / dragThresholdPx).coerceIn(0f, 1f)
                                                            val dir = dragAccum.sign.toInt().coerceIn(-1, 1)
                                                            val idx = routeOrder.indexOf(route).let { if (it < 0) 1 else it }
                                                            val next = (idx + dir).coerceIn(0, routeOrder.lastIndex)
                                                            if (dir != 0 && next != idx) {
                                                                dragPreviewIndex = next
                                                                dragProgress = progress
                                                            } else {
                                                                dragPreviewIndex = null
                                                                dragProgress = 0f
                                                            }
                                                            if (absAccum > dragThresholdPx) {
                                                                val nextRoute = routeOrder[next]
                                                                if (nextRoute != route) {
                                                                    navController.navigate(nextRoute) {
                                                                        popUpTo(navController.graph.startDestinationId)
                                                                        launchSingleTop = true
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
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = sharedInteraction
                                                ) {
                                                    navController.navigate(route) {
                                                        popUpTo(navController.graph.startDestinationId)
                                                        launchSingleTop = true
                                                    }
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val tapNudge = if (selected) 0f else if (route.hashCode() % 2 == 0) 8f else -8f
                                                    scope.launch {
                                                        burst.snapTo(1f)
                                                        burst.animateTo(0f, tween(900))
                                                    }
                                                    scope.launch {
                                                        pos.snapTo(Offset(tapNudge, 0f))
                                                        squish.snapTo(1.04f)
                                                        skew.snapTo(if (tapNudge >= 0f) 0.06f else -0.06f)
                                                        launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
                                                        launch { squish.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) }
                                                        launch { skew.animateTo(0f,    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) }
                                                    }
                                                }
                                                .size(buttonSize),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val pillRadius = 32.dp
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .clip(RoundedCornerShape(pillRadius))
                                                    .background(
                                                        if (selected)
                                                            Brush.verticalGradient(
                                                                0f to Color(0xFF2D0C0C).copy(alpha = 0.45f),
                                                                1f to Color(0xFF310D0D).copy(alpha = 0.45f)
                                                            )
                                                        else
                                                            Brush.verticalGradient(
                                                                0f to Color.Transparent,
                                                                1f to Color.Transparent
                                                            )
                                                    )
                                                    .border(
                                                        width = 2.0.dp,
                                                        shape = RoundedCornerShape(pillRadius),
                                                        color = if (selected) Color(0xFF752626) else Color.Transparent
                                                    )
                                                    .drawWithCache {
                                                        val r = size.minDimension / 2f
                                                        val glowBrush = Brush.radialGradient(
                                                            listOf(
                                                                accentGlow.copy(alpha = 0.50f * glowAlpha),
                                                                accentGlow.copy(alpha = 0.22f * glowAlpha),
                                                                Color.Transparent
                                                            ),
                                                            center = Offset(size.width / 2f, size.height / 2f),
                                                            radius = r * 1.35f
                                                        )
                                                        onDrawBehind {
                                                            if (glowAlpha > 0f) {
                                                                drawRoundRect(
                                                                    brush = glowBrush,
                                                                    cornerRadius = CornerRadius(r, r)
                                                                )
                                                            }
                                                        }
                                                    }
                                            )

                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) Color(0xFFFF3B30) else Color.White.copy(alpha = iconAlpha * 0.9f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = baseIsVisible && isDismissedByUser && currentRoute == "HomeScreen",
                        enter = fadeIn(animationSpec = tween(200, delayMillis = 200)) + scaleIn(animationSpec = tween(400, delayMillis = 200)),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200))
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
                                        val intent = Intent(ctx, WorkoutActivity::class.java)
                                        ctx.startActivity(intent)
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
                                            colors = listOf(
                                                Color(0xFF3A0E0E),
                                                Color(0xFF120707)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFF5555).copy(alpha = 0.6f),
                                                Color(0xFF8B0000).copy(alpha = 0.45f)
                                            )
                                        ),
                                        shape = FloatingActionButtonDefaults.extendedFabShape
                                    )
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = baseIsVisible && isDismissedByUser && currentRoute != "HomeScreen",
                        enter = fadeIn(animationSpec = tween(600)),
                        exit = fadeOut(animationSpec = tween(300))
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
