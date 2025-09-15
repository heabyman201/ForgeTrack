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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FloatingTaskbar(
    modifier: Modifier = Modifier,
    navController: NavController,
    cornerRadius: Dp,
    iconAlpha: Float,
    uiState: WorkoutListUiState
) {
    // FIX: Check the 'currentMode' state instead of the old 'isWorkoutActive' boolean
    val isWorkoutInProgress = ConnectedWorkout.currentMode.value != ConnectedWorkout.WorkoutMode.INACTIVE
    val ctx = LocalContext.current

    val chroma = rememberInfiniteTransition(label = "tbGlow")
    val glow by chroma.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(
            targetState = isWorkoutInProgress,
            label = "TaskbarState",
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }
        ) { workoutIsActive ->
            if (workoutIsActive) {
                val containerShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
                val density = LocalDensity.current
                val cornerRpx = with(density) { cornerRadius.toPx() }
val haptics = LocalHapticFeedback.current
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(containerShape)
                        .clickable {
                           haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            val intent = Intent(ctx, NotificationDispatcherActivity::class.java)
                            ctx.startActivity(intent)
                        }
                        .drawWithCache {
                            val bgBrush = Brush.radialGradient(
                                listOf(
                                    Color(0xFF3A0E0E).copy(alpha = 0.65f * glow),
                                    Color(0xFF120707).copy(alpha = 0.95f)
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.95f
                            )
                            val borderBrush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFFF5555).copy(alpha = 0.4f * glow),
                                    Color(0xFF8B0000).copy(alpha = 0.25f)
                                )
                            )
                            onDrawBehind {
                                drawRoundRect(
                                    brush = bgBrush,
                                    cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                )
                                drawRoundRect(
                                    brush = borderBrush,
                                    style = Stroke(width = 1.dp.toPx()),
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
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Return to Workout",
                            color = Color.White,
                            fontSize = 24.sp,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                        fun taskbarSafetyCheck(){
                            if (taskbarOverride.shouldOverrideVisiblity.value && currentRoute.toString() == "HomeScreen" ){
                                taskbarOverride.shouldOverrideVisiblity.value = false
                                Log.d("TaskbarSafetyCheck", "Taskbar safety check triggered")
                            }
                        }
                        LaunchedEffect(Unit) {
                            while (true) {
                                taskbarSafetyCheck()
                                delay(2000)
                            }
                        }
                        val containerShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
                        val accentGlow = remember { Color(0xFFFF3535) }

                        val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                        val squish = remember { Animatable(1f) }
                        val skew = remember { Animatable(0f) }

                        val interactions = remember { MutableInteractionSource() }
                        val isPressed by interactions.collectIsPressedAsState()

                        val am = remember(ctx) { ctx.getSystemService(android.app.ActivityManager::class.java) }
                        val lowSpec = remember { (am?.isLowRamDevice == true) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S }
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

                        LaunchedEffect(currentRoute) {
                            val nudgeX = if ((currentRoute ?: "").hashCode() % 2 == 0) 10f else -10f
                            pos.snapTo(Offset(nudgeX, 0f))
                            skew.snapTo(if (nudgeX > 0) 0.08f else -0.08f)
                            squish.snapTo(1.05f)
                            launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) }
                            launch {
                                squish.animateTo(0.95f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
                                squish.animateTo(1f,   spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
                            }
                            launch { skew.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) }
                        }

                        LaunchedEffect(isPressed) {
                            if (isPressed) squish.animateTo(1.035f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
                            else squish.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
                        }

                        var boxSize by remember { mutableStateOf(IntSize.Zero) }
                        val density = LocalDensity.current
                        val cornerRpx = with(density) { cornerRadius.toPx() }

                        val borderShape = remember { RoundedCornerShape(32.dp) }
                        val borderColor = remember { Color.White.copy(alpha = 0.11f) }

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
                                    .indication(interactions, indication = null)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                val w = boxSize.width.toFloat().coerceAtLeast(1f)
                                                val h = boxSize.height.toFloat().coerceAtLeast(1f)
                                                val cx = w * 0.5f
                                                val cy = h * 0.5f
                                                val dx = ((offset.x - cx) / (w * 0.5f)).coerceIn(-1f, 1f)
                                                val dy = ((offset.y - cy) / (h * 0.5f)).coerceIn(-1f, 1f)
                                                val tx = dx * 10f
                                                val ty = dy * 6f
                                                val kx = dx * 0.08f
                                                val squeeze = 1f + (-0.05f * (0.6f * kotlin.math.abs(dx) + 0.4f * kotlin.math.abs(dy)))
                                                scope.launch {
                                                    pos.snapTo(Offset(tx, ty))
                                                    skew.snapTo(kx)
                                                    squish.snapTo(squeeze)
                                                    launch { pos.animateTo(Offset.Zero, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
                                                    launch { skew.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)) }
                                                    launch { squish.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)) }
                                                }
                                                tryAwaitRelease()
                                            }
                                        )
                                    }
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
                            ) {

                                val glassColor = Color.Black.copy(alpha = 0.5f)
                                AndroidView(
                                    factory = { context ->
                                        View(context).apply {
                                            setBackgroundColor(glassColor.toArgb())
                                        }
                                    },
                                    modifier = Modifier.matchParentSize(),
                                    update = { view ->
                                        val blurEffect = RenderEffect.createBlurEffect(
                                            80f, 80f, Shader.TileMode.DECAL
                                        )
                                        view.setRenderEffect(blurEffect)
                                    }
                                )


                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .drawWithCache {
                                            val bgBrush = Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF3A0E0E).copy(alpha = 0.65f * glow),
                                                    Color(0xFF120707).copy(alpha = 0.95f)
                                                ),
                                                center = Offset(size.width / 2f, size.height / 2f),
                                                radius = size.minDimension * 0.95f
                                            )
                                            val borderBrush = Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFFF5555).copy(alpha = 0.4f * glow),
                                                    Color(0xFF8B0000).copy(alpha = 0.25f)
                                                )
                                            )
                                            onDrawBehind {
                                                drawRoundRect(
                                                    brush = bgBrush,
                                                    cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                                )
                                                drawRoundRect(
                                                    brush = borderBrush,
                                                    style = Stroke(width = 1.dp.toPx()),
                                                    cornerRadius = CornerRadius(cornerRpx, cornerRpx)
                                                )
                                            }
                                        }
                                )

                                Row(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(containerShape)
                                        .border(width = 0.5.dp, color = borderColor, shape = borderShape)
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items.forEach { (route, icon) ->
                                        val selected = currentRoute == route
                                        val glowAlpha by animateFloatAsState(
                                            targetValue = if (selected) 1f else 0f,
                                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                                            label = ""
                                        )
                                        val scale by animateFloatAsState(
                                            targetValue = if (selected) 1.055f else 1f,
                                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                                            label = ""
                                        )
                                        val pressSquish by animateFloatAsState(
                                            targetValue = if (selected && isPressed) 0.985f else 1f,
                                            animationSpec = tween(130, easing = FastOutSlowInEasing),
                                            label = ""
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 6.dp)
                                                .graphicsLayer {
                                                    scaleX = scale * pressSquish
                                                    scaleY = scale / pressSquish
                                                }
                                                .clip(RoundedCornerShape(32.dp))
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    navController.navigate(route) {
                                                        popUpTo(navController.graph.startDestinationId)
                                                        launchSingleTop = true
                                                    }
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val tapNudge = if (selected) 0f else if (route.hashCode() % 2 == 0) 8f else -8f
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
                                                                0f to Color.White.copy(alpha = 0.10f),
                                                                1f to Color.White.copy(alpha = 0.06f)
                                                            )
                                                        else
                                                            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent)
                                                    )

                                                    .drawWithCache {
                                                        val r = size.minDimension / 2f
                                                        val glowBrush = Brush.radialGradient(
                                                            listOf(
                                                                accentGlow.copy(alpha = 0.42f * glow),
                                                                accentGlow.copy(alpha = 0.18f * glow),
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
                                                Color(0xFF3A0E0E).copy(alpha = 0.85f * glow),
                                                Color(0xFF120707).copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFF5555).copy(alpha = 0.5f * glow),
                                                Color(0xFF8B0000).copy(alpha = 0.35f)
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
