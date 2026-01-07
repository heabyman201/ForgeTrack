package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentTime
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.GoalDistance
import com.forgecompose.workouttracker.ConnectedWorkout.GoalReps
import com.forgecompose.workouttracker.ConnectedWorkout.GoalSets
import com.forgecompose.workouttracker.ConnectedWorkout.GoalTime
import com.forgecompose.workouttracker.ConnectedWorkout.GoalType
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.currentDistance
import com.forgecompose.workouttracker.ConnectedWorkout.interHour
import com.forgecompose.workouttracker.ConnectedWorkout.interMinute
import com.forgecompose.workouttracker.ConnectedWorkout.interSecond
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant

@Composable
fun PreventBackGesture() {
    BackHandler(enabled = true) {

    }
}

class WorkoutActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = application as MyApplication
        val workoutRepository = application.workoutRepository
        val factory = WorkoutListViewModelFactory(workoutRepository)
        val workoutListViewModel: WorkoutListViewModel by viewModels { factory }
        val badgeViewModel: BadgeViewModel by viewModels {
            BadgeViewModelFactory(
                badgeStorage = InMemoryBadgeStorage()
            )
        }



        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MainScreen(viewModel = workoutListViewModel, badgeViewModel = badgeViewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(viewModel: WorkoutListViewModel,badgeViewModel: BadgeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentMode by ConnectedWorkout.currentMode


    LaunchedEffect(currentMode) {
        when (currentMode) {
            WorkoutMode.ACTIVE, WorkoutMode.RESTING -> WorkoutForegroundService.start(context)
            WorkoutMode.INACTIVE -> WorkoutForegroundService.stop(context)
        }
    }


    NavHost(navController = navController, startDestination = "GoalScreen",
        modifier = Modifier.background(Color(0xFF0D0404))) {
        composable(
            route = "GoalScreen",
            enterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            }
        ) {
            GoalScreen(
                navController = navController,
                viewModel = viewModel,
            )
        }
        composable(
            route = "WorkoutScreen",
            enterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            }
        ) {
            WorkoutScreen(
                navController = navController,
                viewModel = viewModel,
               vm = badgeViewModel
            )
        }
        composable(
            route = "RestScreen",
            enterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500)) +
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) +
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            }
        ) {
            RestScreen(
                navController = navController,
            )
        }
    }
}
@Composable
fun AdviceSection(
    advice: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var glow by remember { mutableFloatStateOf(0.35f) }
//    var glowDir by remember { mutableStateOf(1) }
    var starRotation by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

//    LaunchedEffect(Unit) {
//        while (true) {
//            glow += glowDir * 0.01f
//            if (glow >= 0.9f) glowDir = -1
//            if (glow <= 0.35f) glowDir = 1
//            starRotation = (starRotation + 1.5f) % 360f
//            delay(1000L / 24L)
//        }
//    }

    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = appearanceOptions.colors.background,
                spotColor = appearanceOptions.colors.tertiary
            ),
        shape = cardShape,
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                listOf(
                    appearanceOptions.colors.primary.copy(alpha = 0.4f * glow),
                    Color(0xFF8B0000).copy(alpha = 0.25f)
                )
            )
        ),
        color = Color(0xFF120707).copy(alpha = 0.75f)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        listOf(
                            appearanceOptions.colors.background.copy(alpha = 0.35f * glow),
                            appearanceOptions.colors.background.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = appearanceOptions.colors.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(starRotation)
                )
                Spacer(modifier = Modifier.size(14.dp))
                Text(
                    text = advice,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 8,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 32.dp)
                )
            }

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 10.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 4.dp),
                        color = Color(0xFF8B0000),
                        trackColor = Color.Black.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}



@Composable
fun CountdownOverlay(countdownValue: Int, theme: ColorSchemeAppTheme) {
    val smallRipple = remember { Animatable(0f) }
    val bigRipple = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(countdownValue) {
        if (countdownValue in 1..3) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            smallRipple.snapTo(0f)
            smallRipple.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = EaseOutExpo)
            )
        }
        if (countdownValue == 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            bigRipple.snapTo(0f)
            bigRipple.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        // --- Background Ripples ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (bigRipple.value > 0f) {
                val progress = bigRipple.value
                drawCircle(
                    color = Color.White.copy(alpha = (1f - progress) * 0.45f),
                    radius = size.maxDimension * progress,
                    style = Stroke(width = 50.dp.toPx() * (1f - progress))
                )
            }

            if (smallRipple.value > 0f) {
                val progress = smallRipple.value
                val rippleColor = when (countdownValue) {
                    3 -> theme.tertiary
                    2 -> theme.secondary
                    1 -> theme.primary
                    else -> Color.Transparent
                }
                drawCircle(
                    color = rippleColor.copy(alpha = (1f - progress) * 0.6f),
                    radius = (size.minDimension * 0.35f) + (progress * 250.dp.toPx()),
                    style = Stroke(width = 15.dp.toPx() * (1f - progress))
                )
            }
        }

        // --- Main Animation Content ---
        // We create the transition explicitly to avoid type ambiguity (Int vs EnterExitState)
        val transition = updateTransition(targetState = countdownValue, label = "Countdown")

        transition.AnimatedContent(
            transitionSpec = {
                val duration = 700
                (fadeIn(tween(duration)) + scaleIn(initialScale = 0.6f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(tween(duration)) + scaleOut(targetScale = 1.4f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
            }
        ) { targetCountdown ->
            if (targetCountdown > 0) {
                val infiniteTransition = rememberInfiniteTransition(label = "core")

                // Subtle breathing pulse
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                // Slow, constant rotation
                val idleRotation by infiniteTransition.animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rotation"
                )

                // The fast spin transition
                // If 'state' (the transition target) == 'targetCountdown' (this content), we are entering -> rotate to 0.
                // If 'state' != 'targetCountdown', we are exiting -> rotate to 180.
                val spinRotation by transition.animateFloat(
                    transitionSpec = { tween(700, easing = FastOutSlowInEasing) },
                    label = "spin"
                ) { state ->
                    if (state == targetCountdown) 0f else 180f
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer {
                        // Combine idle sway with the fast spin
                        rotationZ = idleRotation + spinRotation
                        scaleX = pulse
                        scaleY = pulse
                    }
                ) {
                    Canvas(modifier = Modifier.size(280.dp)) {
                        val shapeSize = size * 0.82f
                        val shapeTopLeft = Offset((size.width - shapeSize.width) / 2f, (size.height - shapeSize.height) / 2f)

                        when (targetCountdown) {
                            3 -> {
                                drawIntoCanvas { c ->
                                    // FIXED: Added .asFrameworkPaint() to access setShadowLayer
                                    val p = Paint().asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        setShadowLayer(35.dp.toPx(), 0f, 0f, theme.secondary.toArgb())
                                    }
                                    c.nativeCanvas.drawRoundRect(
                                        shapeTopLeft.x, shapeTopLeft.y,
                                        shapeTopLeft.x + shapeSize.width, shapeTopLeft.y + shapeSize.height,
                                        32.dp.toPx(), 32.dp.toPx(), p
                                    )
                                }
                                drawRoundRect(
                                    color = theme.secondary,
                                    topLeft = shapeTopLeft,
                                    size = shapeSize,
                                    cornerRadius = CornerRadius(32.dp.toPx())
                                )
                            }
                            2 -> {
                                drawIntoCanvas { c ->
                                    val p = Paint().asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        setShadowLayer(40.dp.toPx(), 0f, 0f, theme.primary.toArgb())
                                    }
                                    c.nativeCanvas.drawCircle(center.x, center.y, shapeSize.minDimension / 2f, p)
                                }
                                drawCircle(color = theme.secondary, radius = shapeSize.minDimension / 2f, center = center)
                            }
                            1 -> {
                                val path = Path().apply {
                                    moveTo(center.x, shapeTopLeft.y)
                                    lineTo(shapeTopLeft.x + shapeSize.width, shapeTopLeft.y + shapeSize.height)
                                    lineTo(shapeTopLeft.x, shapeTopLeft.y + shapeSize.height)
                                    close()
                                }
                                drawIntoCanvas { c ->
                                    val p = Paint().asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        setShadowLayer(45.dp.toPx(), 0f, 0f, theme.primary.toArgb())
                                    }
                                    c.nativeCanvas.drawPath(path.asAndroidPath(), p)
                                }
                                drawPath(
                                    path = path,
                                    color = theme.primary
                                )
                            }
                        }

                        // Glossy overlay
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.15f))
                            ),
                            topLeft = shapeTopLeft,
                            size = shapeSize,
                            blendMode = BlendMode.Overlay
                        )
                    }

                    Text(
                        text = targetCountdown.toString(),
                        fontSize = 165.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            // Counter-rotate the text so it stays upright while the shape spins
                            // (Optional: remove this modifier if you want the text to spin with the shape)
                            rotationZ = -spinRotation - idleRotation
                        }
                    )
                }
            }
        }
    }
}




@Composable
fun DistanceProgressTracker(
    modifier: Modifier = Modifier,
    currentDistance: Double,
    goalDistance: Double,
    hype: Float
) {

    val progress = remember(currentDistance, goalDistance) {
        if (goalDistance > 0) (currentDistance / goalDistance).toFloat().coerceIn(0f, 1f) else 0f
    }


    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "distanceProgress"
    )


    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerPosition by shimmerTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPosition"
    )


    val progressStartColor = lerp(Color(0xFFB71C1C), Color(0xFFFF5A5A), hype)
    val progressEndColor = lerp(Color(0xFF8B0000), Color(0xFFD32F2F), hype)
    val flagColor = Color(0xFFDC143C)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(20.dp),

    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)) {
                            append("DISTANCE\n")
                        }
                        withStyle(style = SpanStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)) {
                            append("%.2f km".format(currentDistance))
                        }
                    }
                )


                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)) {
                            append("GOAL\n")
                        }
                        withStyle(style = SpanStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)) {
                            append("%.2f km".format(goalDistance))
                        }
                    },
                    textAlign = TextAlign.End
                )
            }


            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val trackCornerRadius = CornerRadius(size.height / 2f)
                val flagPoleWidth = 2.dp.toPx()
                val flagPoleX = size.width - flagPoleWidth / 2 - 8.dp.toPx()


                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    size = size,
                    cornerRadius = trackCornerRadius
                )


                if (animatedProgress > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(progressStartColor, progressEndColor)
                        ),
                        size = Size(width = size.width * animatedProgress, height = size.height),
                        cornerRadius = trackCornerRadius
                    )
                }


                val shimmerWidth = size.width * 0.3f
                val shimmerRect = Rect(
                    left = (size.width + shimmerWidth) * shimmerPosition - shimmerWidth,
                    top = 0f,
                    right = (size.width + shimmerWidth) * shimmerPosition,
                    bottom = size.height
                )

                drawIntoCanvas { canvas ->
                    canvas.saveLayer(size.toRect(), Paint())

                    drawRoundRect(
                        size = Size(width = size.width * animatedProgress, height = size.height),
                        cornerRadius = trackCornerRadius,
                        color = Color.Transparent
                    )

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.0f),
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startX = shimmerRect.left,
                            endX = shimmerRect.right
                        ),
                        topLeft = shimmerRect.topLeft,
                        size = shimmerRect.size,
                        blendMode = BlendMode.SrcIn
                    )
                    canvas.restore()
                }



                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(flagPoleX, 0f),
                    end = Offset(flagPoleX, size.height),
                    strokeWidth = flagPoleWidth,
                    cap = StrokeCap.Round
                )

                val flagPath = Path().apply {
                    moveTo(flagPoleX - flagPoleWidth / 2, 2.dp.toPx())
                    lineTo(flagPoleX - 12.dp.toPx(), size.height / 2f)
                    lineTo(flagPoleX - flagPoleWidth / 2, size.height - 2.dp.toPx())
                    close()
                }
                drawPath(path = flagPath, color = flagColor)
            }
        }
    }
}
@SuppressLint("WakelockTimeout")
suspend fun continuousStepDetectionAndDistanceCalculation(
    context: Context,
    onStepDetected: () -> Unit,
    onUpdate: (distanceKm: Double) -> Unit
) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
    val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val mainHandler = Handler(Looper.getMainLooper())
    val sensorThread = try { HandlerThread("step-detector").apply { start() } } catch (_: Throwable) { return }
    val sensorHandler = Handler(sensorThread.looper)

    // Registration flags
    var isAccelRegistered = false
    var isStepRegistered = false

    // Distance & cadence/stride state
    var distanceMeters = 0.0
    fun safeInc(meters: Double) {
        if (meters.isFinite()) {
            val nm = distanceMeters + meters
            if (nm.isFinite() && nm >= 0.0) distanceMeters = nm
        }
    }

    // Gravity (LPF) state
    var gx = 0.0; var gy = 0.0; var gz = 0.0
    val tauSec = 0.8

    // Streaming stats (variance/entropy proxy)
    var mean = 0.0; var meanSq = 0.0
    val beta = 0.02
    var sampleCount = 0

    // Peak detection state
    var prev2 = 0.0; var prev1 = 0.0
    var lastValley = Double.POSITIVE_INFINITY
    var lastTimestampNs: Long? = null
    var lastStepTimeNs = 0L
    val minStepNs = 250_000_000L
    val maxStepNs = 2_000_000_000L

    // HW step suppression window
    var lastHwStepNs = 0L

    // Cadence & stride blending
    // EWMA cadence (steps per second)
    var cadenceEwma = 0.0
    var lastCadenceTickNs = 0L
    fun updateCadence(nowNs: Long) {
        if (lastCadenceTickNs != 0L) {
            val dt = (nowNs - lastCadenceTickNs).coerceAtLeast(1L).toDouble() / 1e9
            val inst = (1.0 / dt).coerceIn(0.25, 4.0) // 15–240 spm range
            val gamma = 0.25
            cadenceEwma = if (cadenceEwma == 0.0) inst else (1 - gamma) * cadenceEwma + gamma * inst
        }
        lastCadenceTickNs = nowNs
    }

    // UI update coalescing
    var lastPushValue = Double.NaN
    var lastPushMs = 0L
    var updateThrottleMs = 600L
    fun fastRound3(x: Double): Double {
        if (!x.isFinite()) return Double.NaN
        val t = x * 1000.0
        return kotlin.math.round(t) / 1000.0
    }
    fun pushUpdate(force: Boolean = false) {
        val km = distanceMeters / 1000.0
        val rounded = fastRound3(km)
        val now = SystemClock.uptimeMillis()
        if (!rounded.isFinite()) return
        if (force || rounded != lastPushValue || now - lastPushMs >= updateThrottleMs) {
            lastPushValue = rounded
            lastPushMs = now
            mainHandler.post {
                try { onUpdate(rounded) } catch (_: Throwable) {}
            }
        }
    }

    // Power/lifecycle state
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val wakeLock = try {
        // Only keep a partial wakelock if HW step sensor is unavailable
        if (stepDetector == null) pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wt:step")
            ?.apply { setReferenceCounted(false); acquire() } else null
    } catch (_: Throwable) { null }

    var lastEventUptimeMs = SystemClock.uptimeMillis()
    var idleSinceMs = lastEventUptimeMs
    var lowMotion = false

    // Start with moderate rate; adapt down if HW steps flowing or motion low
    var accelRateUs = 20_000           // ~50 Hz
    var accelLatencyUs = 200_000       // 0.2 s batch
    var maxLatencyLowUs = 800_000      // 0.8 s batch when idle

    // Re-register helper (will be reassigned)
    var reRegisterAccel: ((Int, Int) -> Unit)? = null

    // ---- Accelerometer listener (fallback step detection + motion scoring) ----
    val accelListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            lastEventUptimeMs = SystemClock.uptimeMillis()
            try {
                e ?: return
                val vals = e.values ?: return
                if (vals.size < 3) return
                val ts = e.timestamp

                // Time step & LPF for gravity
                val dtNs = lastTimestampNs?.let { d -> (ts - d).takeIf { it > 0L } ?: 20_000_000L } ?: 20_000_000L
                lastTimestampNs = ts
                val dtSec = (dtNs.toDouble() / 1e9).coerceIn(1e-6, 1.0)
                val alpha = (tauSec / (tauSec + dtSec)).coerceIn(0.0, 1.0)

                val vx = vals.getOrNull(0)?.toDouble() ?: return
                val vy = vals.getOrNull(1)?.toDouble() ?: return
                val vz = vals.getOrNull(2)?.toDouble() ?: return

                gx = alpha * gx + (1.0 - alpha) * vx
                gy = alpha * gy + (1.0 - alpha) * vy
                gz = alpha * gz + (1.0 - alpha) * vz

                val lx = vx - gx
                val ly = vy - gy
                val lz = vz - gz

                var mag = (lx * lx + ly * ly + lz * lz)
                if (!mag.isFinite() || mag <= 0.0) return
                mag = kotlin.math.sqrt(mag)
                if (!mag.isFinite()) return

                // Motion statistics
                mean = (1 - beta) * mean + beta * mag
                meanSq = (1 - beta) * meanSq + beta * (mag * mag)
                sampleCount++

                val variance = (meanSq - mean * mean).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                val sigma = kotlin.math.sqrt(variance).let { if (it.isFinite()) it else 0.0 }

                // Dynamic threshold for peaks
                val dynamicThreshold = max(1.05, (mean + 1.10 * sigma).coerceIn(0.8, 20.0))
                val curr = mag

                if (curr < prev1) lastValley = min(lastValley, curr)
                val isPeak = (sampleCount > 25) && prev1 > prev2 && prev1 > curr && prev1 > dynamicThreshold

                if (isPeak) {
                    val dtSinceLast = ts - lastStepTimeNs
                    if (lastStepTimeNs == 0L) {
                        lastStepTimeNs = ts
                        lastValley = curr
                    } else if (dtSinceLast in minStepNs..maxStepNs) {
                        // Suppress if HW step very recently fired (prefer HW to avoid double counting)
                        val suppressByHw = (ts - lastHwStepNs) in 0L..150_000_000L
                        if (!suppressByHw) {
                            mainHandler.post(onStepDetected)
                            updateCadence(ts)

                            // Amplitude & cadence blended stride (clamped)
                            val amplitude = (prev1 - lastValley).coerceAtLeast(0.0)
                            val ampTerm = 0.54 * min(amplitude, 12.0).pow(0.25)
                            // cadenceEwma is steps/sec; rough stride growth up to ~1.0m at high cadence
                            val cadTerm = (0.48 + 0.18 * cadenceEwma.coerceIn(0.0, 3.0))
                            val stepLen = (0.5 * ampTerm + 0.5 * cadTerm).coerceIn(0.45, 0.95)

                            safeInc(stepLen)
                            lastStepTimeNs = ts
                            lastValley = curr
                            pushUpdate()
                        } else {
                            lastStepTimeNs = ts
                            lastValley = curr
                        }
                    } else if (dtSinceLast <= 0L || dtSinceLast > 5_000_000_000L) {
                        lastStepTimeNs = ts
                        lastValley = curr
                    }
                }

                prev2 = prev1
                prev1 = curr

                // Adaptive power policy
                val now = SystemClock.uptimeMillis()
                val wasLow = lowMotion
                lowMotion = sigma < 0.08

                // If HW steps are present and recent, keep accel in low-rate "guard" mode.
                val hwRecent = (ts - lastHwStepNs) in 0L..2_000_000_000L

                when {
                    // Deep idle: low motion & no HW steps recently → slow + long batch
                    lowMotion && !hwRecent && (accelRateUs != 33_000 || accelLatencyUs != maxLatencyLowUs || updateThrottleMs != 900L) -> {
                        accelRateUs = 33_000      // ~30 Hz
                        accelLatencyUs = maxLatencyLowUs
                        updateThrottleMs = 900L
                        reRegisterAccel?.invoke(accelRateUs, accelLatencyUs)
                    }
                    // HW steps flowing → keep accel very cheap to validate motion shape
                    hwRecent && (accelRateUs != 40_000 || accelLatencyUs != 600_000 || updateThrottleMs != 800L) -> {
                        accelRateUs = 40_000      // ~25 Hz
                        accelLatencyUs = 600_000
                        updateThrottleMs = 800L
                        reRegisterAccel?.invoke(accelRateUs, accelLatencyUs)
                    }
                    // Active motion spike or we just exited low motion → faster refresh & shorter batch
                    (!lowMotion && (wasLow || accelRateUs != 20_000 || accelLatencyUs != 200_000 || updateThrottleMs != 600L)) -> {
                        accelRateUs = 20_000      // ~50 Hz
                        accelLatencyUs = 200_000
                        updateThrottleMs = 600L
                        reRegisterAccel?.invoke(accelRateUs, accelLatencyUs)
                    }
                }

                if (!lowMotion) idleSinceMs = now
            } catch (_: Throwable) { /* swallow */ }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Helper to (re)register accelerometer with given params
    reRegisterAccel = { newRateUs: Int, newLatencyUs: Int ->
        try { sensorManager.unregisterListener(accelListener) } catch (_: Throwable) {}
        try {
            isAccelRegistered = sensorManager.registerListener(
                accelListener,
                accel,
                newRateUs,
                newLatencyUs,
                sensorHandler
            )
        } catch (_: Throwable) { isAccelRegistered = false }
    }

    // ---- Hardware Step Detector (primary when available) ----
    val stepListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            lastEventUptimeMs = SystemClock.uptimeMillis()
            try {
                e ?: return
                val v = e.values
                if (v.isEmpty()) return
                if (v[0] == 1f) {
                    val ts = e.timestamp
                    lastHwStepNs = ts
                    mainHandler.post(onStepDetected)
                    updateCadence(ts)

                    // Cadence-biased default stride; HW lacks amplitude → use cadence EWMA with safe clamp
                    val stride = (0.60 + 0.20 * cadenceEwma.coerceIn(0.0, 3.0)).coerceIn(0.55, 0.90)
                    safeInc(stride)
                    pushUpdate()

                    // When HW is active, keep accel cheap (guard mode). Slight nudge handled in accel listener.
                }
            } catch (_: Throwable) { /* swallow */ }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun registerAll() {
        try {
            isAccelRegistered = sensorManager.registerListener(
                accelListener,
                accel,
                accelRateUs,
                accelLatencyUs,
                sensorHandler
            )
        } catch (_: Throwable) { isAccelRegistered = false }

        try {
            isStepRegistered = stepDetector?.let {
                sensorManager.registerListener(
                    stepListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    /* maxReportLatencyUs */ 1_000_000, // 1s batch for HW steps (good balance)
                    sensorHandler
                )
            } ?: false
        } catch (_: Throwable) { isStepRegistered = false }
    }

    fun unregisterAll() {
        try { if (isAccelRegistered) sensorManager.unregisterListener(accelListener) } catch (_: Throwable) {}
        try { if (isStepRegistered) sensorManager.unregisterListener(stepListener) } catch (_: Throwable) {}
        isAccelRegistered = false
        isStepRegistered = false
    }

    // ---- Watchdog with exponential backoff (fewer wakeups) ----
    var wdIntervalMs = 12_000L
    val wdMaxMs = 60_000L
    val watchdog = object : Runnable {
        override fun run() {
            try {
                val now = SystemClock.uptimeMillis()
                // If nothing delivered for a while, bounce drivers.
                if (now - lastEventUptimeMs > wdIntervalMs) {
                    unregisterAll()
                    registerAll()
                    lastEventUptimeMs = now
                    pushUpdate(force = true)
                    // Back off next checks to avoid thrashing when sensors misbehave.
                    wdIntervalMs = (wdIntervalMs * 1.5).toLong().coerceAtMost(wdMaxMs)
                } else {
                    // If stream is healthy, slowly relax toward max.
                    wdIntervalMs = (wdIntervalMs + 2_000L).coerceAtMost(wdMaxMs)
                }
            } catch (_: Throwable) {
            } finally {
                try { sensorHandler.postDelayed(this, wdIntervalMs) } catch (_: Throwable) {}
            }
        }
    }

    try {
        registerAll()
        try { sensorHandler.postDelayed(watchdog, wdIntervalMs) } catch (_: Throwable) {}

        // If nothing registered, exit cleanly
        if (!isAccelRegistered && !isStepRegistered) {
            try { sensorThread.quitSafely() } catch (_: Throwable) {}
            try { wakeLock?.release() } catch (_: Throwable) {}
            return
        }

        pushUpdate(force = true)
        awaitCancellation()
    } finally {
        try { sensorHandler.removeCallbacksAndMessages(null) } catch (_: Throwable) {}
        unregisterAll()
        try { sensorThread.quitSafely() } catch (_: Throwable) {}
        try { wakeLock?.release() } catch (_: Throwable) {}
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun WorkoutScreen(viewModel: WorkoutListViewModel, navController: NavController, vm: BadgeViewModel, bpVM: HrViewModel = viewModel()) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val performanceOptions by PerformanceOptionsManager.current.collectAsState(initial = PerformanceOptions.Defaults)
    val movingGradientAndParticlesEnabled = performanceOptions.movingGradientAndParticles
    val intent = remember(context) { Intent(context, MainActivity::class.java) }
    var hours by interHour
    var minutes by interMinute
    var seconds by interSecond
    var isPaused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    var timeMillis by remember { mutableStateOf(0L) }
    var pauseText by remember { mutableStateOf("Pause") }
    val activity = remember(context) { context as? Activity }
    var showCompletionAnimation by remember { mutableStateOf(false) }
    var isStepping by remember { mutableStateOf(false) }
    var lastStepTimestamp by remember { mutableStateOf(0L) }

    LaunchedEffect(lastStepTimestamp) {
        if (lastStepTimestamp > 0) {
            isStepping = true
            while (SystemClock.uptimeMillis() - lastStepTimestamp < 1500) {
                delay(100)
            }
            isStepping = false
        }
    }
    PreventBackGesture()
    val uiState by viewModel.uiState.collectAsState()
    val workouts: List<Workout> = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()

    fun List<Workout>.contentKey(): Int =
        fold(1) { acc, w ->
            acc * 31 +
                    w.name.hashCode() * 31 +
                    (w.weight ?: 0.0).hashCode() * 31 +
                    w.durationMillis.hashCode()
        }

    val contentKey = remember(workouts) { workouts.contentKey() }

    val (personalRecords, recentWorkouts) = remember(contentKey) {
        if (workouts.isNotEmpty()) {
            val prs = workouts
                .filter { (it.weight ?: 0.0) > 0.0 }
                .groupBy { it.name }
                .map { (name, list) -> PersonalRecord(name, list.maxOf { it.weight!! }) }
                .sortedByDescending { it.maxWeight }
            val recent = workouts.take(5)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
    }

    val fitnessContext = "You are a fitness coach. The user provides sets, reps, and either weights , time or distance. Give them encouragement. Max 15 words only."
    val scope = rememberCoroutineScope()
    val aiState = useGeminiAdviceGenerator(
        contextPrompt = "$fitnessContext."
    )
    val advice = aiState.currentAdvice
    var showAdvice by remember { mutableStateOf(false) }


    fun timeToMillis() {
        val hoursInMillis = hours * 60 * 60 * 1000L
        val minutesInMillis = minutes * 60 * 1000L
        val secondsInMillis = seconds * 1000L
        timeMillis = hoursInMillis + minutesInMillis + secondsInMillis
        CurrentTime.value = timeMillis
    }

    fun triggerSetGoal() {
        when (GoalType) {
            "Reps" -> {
                if (CurrentSets.intValue >= GoalSets.intValue && CurrentReps.intValue >= GoalReps.intValue) {
                    showCompletionAnimation = true
                    GoalSets.intValue = 0
                    GoalReps.intValue = 0
                    GoalTime.value = 0
                    scope.launch(Dispatchers.IO) {
                        isPaused = false
                        delay(10000)
                        hours = 0; minutes = 0; seconds = 0
                        CurrentSets.intValue = 0; CurrentReps.intValue = 0
                    }
                }
            }
            "Distance" -> {
                if (currentDistance.value >= GoalDistance.value) {
                    showCompletionAnimation = true
                    GoalDistance.value = 0.0
                    currentDistance.value = 0.0
                }
            }
            else -> {

                if (CurrentTime.value >= GoalTime.value && !showCompletionAnimation) {
                    showCompletionAnimation = true
                    isPaused = true
                }
            }
        }
    }

    fun EnterRestMode() {
        if (CurrentReps.intValue >= GoalReps.intValue && CurrentSets.intValue >= GoalSets.intValue) {
            triggerSetGoal()
        } else {
            ConnectedWorkout.currentMode.value = WorkoutMode.RESTING
            navController.navigate("RestScreen") { popUpTo("RestScreen") { inclusive = true } }
        }
    }

    fun SetsGoalSafetyCheck() {
        when (GoalType) {
            "Reps" -> if (GoalReps.intValue <= 0 && !showCompletionAnimation) {
                Toast.makeText(context, "Reps must be greater than 0", Toast.LENGTH_SHORT).show()
                navController.navigate("GoalScreen") { popUpTo("GoalScreen") { inclusive = true } }
            }
            "Distance" -> if (GoalDistance.value <= 0 && !showCompletionAnimation) {
                Toast.makeText(context, "Distance must be greater than 0", Toast.LENGTH_SHORT).show()
            }
            else -> if (GoalTime.value <= 0 && !showCompletionAnimation) {
                Toast.makeText(context, "Time or distance must be greater than 0", Toast.LENGTH_SHORT).show()
                navController.navigate("GoalScreen") { popUpTo("GoalScreen") { inclusive = true } }
            }
        }
    }

    val progress by remember {
        derivedStateOf {
            if (GoalTime.value > 0) (CurrentTime.value.toFloat() / GoalTime.value.toFloat()).coerceIn(0f, 1f)
            else 0f
        }
    }
    val progressDistance by remember {
        derivedStateOf {
            if (GoalDistance.value > 0) (currentDistance.value / GoalDistance.value).toFloat().coerceIn(0f, 1f)
            else 0f
        }
    }

    fun easeOutExpo(x: Float): Float = if (x >= 1f) 1f else 1f - 2f.pow(-10f * x)
    val unified by remember {
        derivedStateOf {
            when (GoalType) {
                "Reps" -> {
                    val setPart = if (GoalSets.intValue > 0)
                        (CurrentSets.intValue.toFloat() / GoalSets.intValue.toFloat()).coerceIn(0f, 1f) else 0f
                    val repPart = if (GoalReps.intValue > 0)
                        (CurrentReps.intValue.toFloat() / GoalReps.intValue.toFloat()).coerceIn(0f, 1f) else 0f
                    (0.35f * repPart + 0.65f * setPart).coerceIn(0f, 1f)
                }
                "Distance" -> progressDistance
                else -> progress
            }
        }
    }
    val hype by animateFloatAsState(targetValue = easeOutExpo(unified), label = "hype", animationSpec = tween(1200))

    var lastMilestone by remember { mutableStateOf(0) }
    LaunchedEffect(unified) {
        val m = when {
            unified >= 0.85f -> 2
            unified >= 0.50f -> 1
            else -> 0
        }
        if (m != lastMilestone) {
            lastMilestone = m
            when (m) {
                1 -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                2 -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "p", animationSpec = tween(900))
    val animatedProgressDistance by animateFloatAsState(targetValue = progressDistance, label = "pd", animationSpec = tween(900))

    val startAt = rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var accMs by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        accMs = ((hours * 3600L + minutes * 60L + seconds) * 1000L)
    }
    fun incrementTime() {
        if (!isPaused) {
            val now = SystemClock.elapsedRealtime()
            val elapsed = (now - startAt.longValue) + accMs
            val totalSec = (elapsed / 1000L).toInt()
            seconds = totalSec % 60
            minutes = (totalSec / 60) % 60
            hours = totalSec / 3600
        }
    }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        val isStartingFresh = (hours == 0 && minutes == 0 && seconds == 0 && accMs == 0L)

        if (isStartingFresh) {
            showCountdown = true
            isPaused = true

            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1000)
            countdownValue = 2

            delay(1000)
            countdownValue = 1

            delay(1000)
            countdownValue = 0

            delay(500)
            showCountdown = false
            isPaused = false
            startAt.longValue = SystemClock.elapsedRealtime()
            accMs = 0L
        }
        while (true) {
            incrementTime()
            timeToMillis()
            triggerSetGoal()
            SetsGoalSafetyCheck()
            delay(1000)
        }
    }

    DisposableEffect(GoalType) {
        var job: Job? = null
        if (GoalType == "Distance") {
            job = scope.launch {
                continuousStepDetectionAndDistanceCalculation(
                    context = context,
                    onStepDetected = { lastStepTimestamp = SystemClock.uptimeMillis() }
                ) { distanceKm ->
                    currentDistance.value = distanceKm
                    Log.d("Distance", "Distance: $distanceKm")
                }
            }
        }
        onDispose { job?.cancel() }
    }

    var animationClock by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (isActive) {
            val currentTime = withFrameNanos { it }
            if (lastFrameTime != 0L) {
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                animationClock += deltaTime
            }
            lastFrameTime = currentTime
            delay(19)
        }
    }

    val setCompletionProgress by remember {
        derivedStateOf {
            if (GoalType == "Reps" && GoalSets.intValue > 0) {
                (CurrentSets.intValue.toFloat() / GoalSets.intValue.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val riseEffectProgress by animateFloatAsState(
        targetValue = setCompletionProgress,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "riseEffectProgress"
    )

    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) {
        Brush.radialGradient(
            colors = introColors,
            center = Offset(0.5f, 0.5f),
            radius = 2000f
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(750, easing = LinearEasing),
        label = "introFade"
    )
    LaunchedEffect(Unit) {
        if(showCountdown)
        aiState.generateBatch(
            "The User has performed $CurrentReps reps and $CurrentSets so far",
            "The Workout weight is $CurrentWeight",
            "The Workout name is $workout"
        )

        showIntro = false

    }

    val glowColor = theme.secondary
    val deepColor = theme.background
    val allWorkouts = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
    val totalWorkoutCount = remember(uiState) {
        allWorkouts.size
    }
    val particles = remember {
        List(6) { i ->
            val s = (i * 37.123f) % 1000f
            ParticleData(
                s = s,
                speed = 0.25f + (s % 0.35f),
                baseXRatio = (s % 1f),
                wobbleSpeed = 0.8f + (s % 0.7f),
                wobbleOffset = s,
                baseRadius = 6f + (s % 1f) * 18f,
                wobbleMagnitudeBase = 16f,
                wobbleMagnitudeExtra = 28f
            )
        }
    }
    LaunchedEffect(totalWorkoutCount) {

        vm.syncTotalWorkouts(totalWorkoutCount)
    }
    val primaryColor = theme.primary
    val secondaryColor = theme.secondary
    val gradientStops = remember(theme) {
        arrayOf(
            0.0f to Color.Transparent,
            0.25f to theme.secondary.copy(alpha = 0.4f),
            0.55f to theme.primary.copy(alpha = 0.6f),
            0.85f to theme.primary.copy(alpha = 0.8f),
            1.0f to Color.Transparent
        )
    }

    WorkoutTrackerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = workout.value,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val currentHype = if (movingGradientAndParticlesEnabled) hype else 0f
                        val radiusMultiplier = 1.0f + 0.5f * currentHype
                        val verticalShift = size.height * 0.1f
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.46f + 0.32f * currentHype), deepColor),
                                center = Offset(size.width / 2f, size.height + verticalShift),
                                radius = (size.width * 1.15f) * radiusMultiplier,
                                tileMode = TileMode.Clamp
                            )
                        )
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = (0.10f + 0.26f * hype) * riseEffectProgress }
                    ) {
                        val h = size.height
                        val startY = h * (1f - 0.65f * riseEffectProgress)
                        val endY = h

                        drawRect(
                            brush = Brush.verticalGradient(
                                *gradientStops,
                                startY = startY,
                                endY = endY
                            )
                        )
                    }
                    if (movingGradientAndParticlesEnabled) {

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {

                                    alpha = (0.12f + 0.32f * hype) * riseEffectProgress
                                }
                        ) {
                            val w = size.width
                            val h = size.height


                            particles.forEach { p ->

                                val phase = (animationClock * p.speed + (p.s * 0.013f)) % 1f
                                val y = h * (1f - phase)


                                val wobble = kotlin.math.sin((animationClock * p.wobbleSpeed) * 6.28318f + p.wobbleOffset) * (p.wobbleMagnitudeBase + p.wobbleMagnitudeExtra * (1f - phase))

                                val x = (p.baseXRatio * w + wobble).coerceIn(-40f, w + 40f)
                                val r = p.baseRadius * (0.4f + 0.6f * (1f - phase))

                                val a = (0.30f + 0.70f * (1f - phase)) * riseEffectProgress
                                val constrainedAlpha = a.coerceIn(0f, 1f)




                                drawCircle(
                                    color = primaryColor,
                                    radius = r,
                                    center = Offset(x, y),
                                    alpha = constrainedAlpha
                                )


                                drawCircle(
                                    color = secondaryColor,
                                    radius = r * 1.8f,
                                    center = Offset(x, y + r * 0.2f),
                                    alpha = (constrainedAlpha * 0.6f)
                                )
                            }
                        }
//                     DiscoAtmosphere(
//
//                         riseEffectProgress = riseEffectProgress,
//                         themeColors = listOf(
//                             theme.primary,
//                             theme.secondary,
//                             theme.tertiary
//                         )
//
//                     )
                    }
                }
                if (introProgress < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(introBrush, alpha = 1f - introProgress)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val prFlags = remember(workouts, workout.value, CurrentWeight.value, CurrentReps.intValue, CurrentSets.intValue) {
                        val pr = checkPrForExercise(
                            workouts, workout.value,
                            CurrentWeight.value.toFloat(),
                            CurrentReps.intValue,
                            CurrentSets.intValue
                        )
                        PrFlags(
                            strengthPr = pr.isStrengthPr,
                            volumePr = pr.isVolumePr,
                            repsPr = false,
                            setsPr = false
                        )
                    }
                    if (showCompletionAnimation) {
                        GoalCompletionAnimation(
                            onAnimationFinished = {
                                val healthConnectManager = HealthConnectManager(context.applicationContext)
                                val cardioExerciseNames = listOf(
                                    "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                                    "Rowing Machine", "Stationary Bike", "Swimming"
                                )

                                scope.launch(Dispatchers.IO) {
                                    viewModel.LogWorkout(
                                        workout.value, WorkoutStatus.COMPLETED,
                                        CurrentTime.value, CurrentWeight.value,
                                        CurrentSets.intValue, CurrentReps.intValue,
                                        currentDistance.value,
                                        ""
                                    )
                                    PDE.logWorkout(
                                        workout.value,
                                        CurrentTime.value,
                                        CurrentWeight.value.toFloat(),
                                        CurrentReps.intValue,
                                        CurrentSets.intValue,
                                        currentDistance.value.toFloat()
                                    )
                                    if (healthConnectManager.hasAllPermissions()) {
                                        val endInstant = Clock.System.now().toJavaInstant()
                                        val workoutDetails = WorkoutDetails(
                                            title = workout.value,
                                            startTime = endInstant.minusMillis(accMs),
                                            endTime = endInstant,
                                            exerciseType =
                                                if (workout.value in cardioExerciseNames) {
                                                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                                                } else {
                                                    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                                                }
                                        )
                                        healthConnectManager.writeWorkout(workoutDetails)
                                    }
                                    vm.onWorkoutLogged(totalWorkoutCount)
                                    vm.refresh()
                                }

                                WorkoutLog.sets.clear()
                                WorkoutForegroundService.stop(context)
                                ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                                context.startActivity(intent)
                                activity?.finishAffinity()
                                hours = 0
                                minutes = 0
                                seconds = 0
                                accMs = 0L
                                startAt.longValue = SystemClock.elapsedRealtime()
                                CurrentSets.intValue = 0
                                CurrentReps.intValue = 0
                                CurrentTime.value = 0
                                GoalSets.intValue = 0
                                GoalReps.intValue = 0
                                GoalTime.value = 0
                                GoalDistance.value = 0.0
                            },
                            prFlags = prFlags
                        )

                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        lerp(Color.White, theme.primary, hype),
                                        Color.White
                                    )
                                ),
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (GoalType == "Reps") {
                        SetProgressDetails(
                            currentReps = CurrentReps.intValue,
                            goalReps = GoalReps.intValue,
                            currentSet = CurrentSets.intValue,
                            goalSets = GoalSets.intValue,
                            theme = theme
                        )
                    } else if (GoalType == "Distance") {
                        DistanceProgressTracker(
                            currentDistance = currentDistance.value,
                            goalDistance = GoalDistance.value,
                            hype = hype
                        )
                    } else {
                        CircularTimerProgressBar(
                            progress = animatedProgress,
                            hype = hype,
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.75f))
                    val aiEnabled = dynamicModel.personaConfig.value.enabled

                    if (aiEnabled) {
                        AdviceSection(
                            advice = advice,
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = false
                        )
                    }

                    Spacer(modifier = Modifier
                        .weight(0.65f)
                        .height(2.dp))

                    if (GoalType == "Reps") {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "buttonScale")
                        val animatedBg by animateColorAsState(
                            targetValue = if (isPressed) {
                                lerp(
                                    theme.secondary,
                                    theme.primary,
                                    riseEffectProgress
                                ).copy(alpha = (0.70f + 0.22f * hype).coerceIn(0f, 1f))
                            } else {
                                lerp(
                                    theme.tertiary,
                                    theme.secondary,
                                    riseEffectProgress
                                ).copy(alpha = (0.45f + 0.30f * hype).coerceIn(0f, 1f))
                            },
                            label = "btnBg"
                        )


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            lerp(theme.primary, theme.secondary, riseEffectProgress)
                                                .copy(alpha = (0.70f + 0.26f * hype).coerceIn(0f, 1f)),
                                            lerp(theme.secondary, theme.tertiary, riseEffectProgress)
                                                .copy(alpha = (0.55f + 0.28f * hype).coerceIn(0f, 1f))
                                        )
                                    ),
                                    CircleShape
                                )
                                .background(animatedBg, CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!showCountdown) {
                                        timeToMillis()
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        CurrentReps.intValue += 10
                                        CurrentSets.intValue += 1
                                        EnterRestMode()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Finish Set",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                val now = SystemClock.elapsedRealtime()
                                if (!isPaused) {
                                    accMs += now - startAt.longValue
                                } else {
                                    startAt.longValue = now
                                }
                                isPaused = !isPaused
                                pauseText = if (isPaused) "Resume" else "Pause"
                            },
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lerp(theme.tertiary, theme.secondary, riseEffectProgress).copy(alpha = 0.5f + 0.15f * hype),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        ) { Text(pauseText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = { showSyncDialog.showSyncDialog.value = true },
                            enabled = isPaused && !showCountdown,
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lerp(theme.secondary, theme.tertiary, riseEffectProgress).copy(alpha = 0.8f),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2A0D0D).copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        ) { Text("End Workout", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (showCountdown) {
                    CountdownOverlay(countdownValue = countdownValue, theme = theme)
                }
                if (showSyncDialog.showSyncDialog.value) {

                    ThemedConfirmationDialog(
                        title = "Sync to health connect",
                        text = "Sync this workout to health connect?",
                        buttonText = "Sync and finish workout",
                        additionalButton = true,
                        additionalButtonText = "Finish Workout Only",
                        onCustomAction = {
                            timeToMillis()
                            scope.launch(Dispatchers.IO) {
                                viewModel.LogWorkout(
                                    workout.value, WorkoutStatus.COMPLETED,
                                    CurrentTime.value, CurrentWeight.value,
                                    CurrentSets.intValue, CurrentReps.intValue,
                                    currentDistance.value,
                                    ""
                                )
                                PDE.logWorkout(
                                    workout.value,
                                    CurrentTime.value,
                                    CurrentWeight.value.toFloat(),
                                    CurrentReps.intValue,
                                    CurrentSets.intValue,
                                    currentDistance.value.toFloat()
                                )
                            }
                            WorkoutLog.sets.clear()
                            WorkoutForegroundService.stop(context)
                            ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                            context.startActivity(intent)
                            activity?.finishAffinity()
                            hours = 0
                            minutes = 0
                            seconds = 0
                            accMs = 0L
                            startAt.longValue = SystemClock.elapsedRealtime()
                            CurrentSets.intValue = 0
                            CurrentReps.intValue = 0
                            CurrentTime.value = 0
                            GoalSets.intValue = 0
                            GoalReps.intValue = 0
                            GoalTime.value = 0
                            GoalDistance.value = 0.0
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDismiss = { showSyncDialog.showSyncDialog.value = false },
                        onConfirm = {
                            val healthConnectManager = HealthConnectManager(context.applicationContext)
                            val cardioExerciseNames = listOf(
                                "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                                "Rowing Machine", "Stationary Bike", "Swimming"
                            )
                            scope.launch(Dispatchers.IO) {
                                viewModel.LogWorkout(
                                    workout.value, WorkoutStatus.COMPLETED,
                                    CurrentTime.value, CurrentWeight.value,
                                    CurrentSets.intValue, CurrentReps.intValue,
                                    currentDistance.value,
                                    ""
                                )
                                PDE.logWorkout(
                                    workout.value,
                                    CurrentTime.value,
                                    CurrentWeight.value.toFloat(),
                                    CurrentReps.intValue,
                                    CurrentSets.intValue,
                                    currentDistance.value.toFloat()
                                )
                                if (healthConnectManager.hasAllPermissions()) {
                                    val endInstant = Clock.System.now().toJavaInstant()
                                    val workoutDetails = WorkoutDetails(
                                        title = workout.value,
                                        startTime = endInstant.minusMillis(accMs),
                                        endTime = endInstant,
                                        exerciseType =
                                            if (workout.value in cardioExerciseNames) {
                                                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                                            } else {
                                                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                                            }
                                    )
                                    healthConnectManager.writeWorkout(workoutDetails)
                                }
                            }
                            WorkoutLog.sets.clear()
                            WorkoutForegroundService.stop(context)
                            ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                            context.startActivity(intent)
                            activity?.finishAffinity()
                            hours = 0
                            minutes = 0
                            seconds = 0
                            accMs = 0L
                            startAt.longValue = SystemClock.elapsedRealtime()
                            CurrentSets.intValue = 0
                            CurrentReps.intValue = 0
                            CurrentTime.value = 0
                            GoalSets.intValue = 0
                            GoalReps.intValue = 0
                            GoalTime.value = 0
                            GoalDistance.value = 0.0

                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SetProgressDetails(
    currentReps: Int,
    goalReps: Int,
    currentSet: Int,
    goalSets: Int,
    theme: ColorSchemeAppTheme
) {
    val accent = theme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(theme.secondary.copy(0.12f), RoundedCornerShape(18.dp))
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Reps",
                            color = Color.White.copy(alpha = 0.70f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            if (goalReps > 0) "$currentReps / $goalReps" else currentReps.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(theme.secondary.copy(0.12f), RoundedCornerShape(18.dp))
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Set",
                            color = Color.White.copy(alpha = 0.70f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "$currentSet / $goalSets",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                DetailedSetsProgressBar(currentSet = currentSet, goalSets = goalSets, theme = theme)
            }
        }
    }
}

@Composable
fun DetailedSetsProgressBar(
    currentSet: Int,
    goalSets: Int,
    modifier: Modifier = Modifier,
    theme: ColorSchemeAppTheme
) {
    if (goalSets <= 0) return

    // Calculate target and progress outside the draw loop
    val target = remember(currentSet, goalSets) {
        when {
            goalSets <= 1 -> if (currentSet >= 1) 1f else 0f
            else -> ((currentSet - 1).toFloat() / (goalSets - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    val progressState = animateFloatAsState(
        targetValue = target,
        label = "SetProgressBarProgress",
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

    // Use FloatState to avoid auto-boxing overhead
    var animationClock by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
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

    val accent = theme.secondary
    val accentBright = theme.primary.copy(alpha = 0.95f)


    val isLinear = goalSets <= 8

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isLinear) 44.dp else 220.dp)
            .drawWithCache {
                onDrawBehind {
                    // Logic inside here does NOT trigger recomposition
                    val progress = progressState.value
                    val shimmer = (animationClock / 1.8f) % 1.4f - 0.2f
                    val pulse = 1.05f + 0.15f * sin(animationClock * 2 * PI.toFloat())

                    if (isLinear) {
                        drawLinearProgress(
                            goalSets, currentSet, progress,
                            shimmer, pulse, accent, accentBright
                        )
                    } else {
                        drawCircularProgress(
                            goalSets, currentSet, progress,
                            shimmer, pulse, accent
                        )
                    }
                }
            }
    )
}

// Extension functions to keep the draw logic clean and isolated
private fun DrawScope.drawLinearProgress(
    goalSets: Int, currentSet: Int, progress: Float,
    shimmer: Float, pulse: Float, accent: Color, accentBright: Color
) {
    val y = size.height / 2f
    val base = 6.dp.toPx()
    val prog = 9.dp.toPx()
    val dotR = 8.dp.toPx()
    val w = size.width - (dotR * 2)
    val startPad = dotR

    drawLine(
        color = Color.White.copy(0.1f),
        start = Offset(startPad, y),
        end = Offset(startPad + w, y),
        strokeWidth = base,
        cap = StrokeCap.Round
    )

    if (progress > 0f) {
        drawLine(
            brush = Brush.horizontalGradient(listOf(accent, accentBright)),
            start = Offset(startPad, y),
            end = Offset(startPad + w * progress, y),
            strokeWidth = prog,
            cap = StrokeCap.Round
        )
    }

    val shWidth = w * 0.4f
    val shStart = (w + shWidth) * shimmer - shWidth + startPad
    drawLine(
        brush = Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(0.18f), Color.Transparent),
            start = Offset(shStart, y),
            end = Offset(shStart + shWidth, y)
        ),
        start = Offset(startPad, y),
        end = Offset(startPad + w, y),
        strokeWidth = base,
        cap = StrokeCap.Round
    )

    for (i in 1..goalSets) {
        val x = if (goalSets > 1) startPad + (w * ((i - 1).toFloat() / (goalSets - 1))) else size.width / 2f
        val completed = i < currentSet
        val isCurrent = i == currentSet

        when {
            completed -> drawCircle(color = accent, radius = dotR, center = Offset(x, y))
            isCurrent -> {
                val r = dotR * pulse
                drawCircle(color = Color.White, radius = r, center = Offset(x, y))
                drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(x, y))
            }
            else -> drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(x, y))
        }
    }
}

private fun DrawScope.drawCircularProgress(
    goalSets: Int, currentSet: Int, progress: Float,
    shimmer: Float, pulse: Float, accent: Color
) {
    val strokeBase = 8.dp.toPx()
    val strokeProg = 10.dp.toPx()
    val dotR = 6.dp.toPx()
    val pad = 16.dp.toPx()
    val radius = (min(size.width, size.height) / 2f) - (strokeProg + pad)
    val arcCenter = center
    val startAngle = -90f
    val sweep = 360f

    drawCircle(
        color = Color.White.copy(0.1f),
        radius = radius,
        center = arcCenter,
        style = Stroke(width = strokeBase, cap = StrokeCap.Round)
    )

    if (progress > 0f) {
        drawArc(
            color = accent,
            startAngle = startAngle,
            sweepAngle = sweep * progress,
            useCenter = false,
            topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeProg, cap = StrokeCap.Round)
        )
    }

    for (i in 1..goalSets) {
        val t = if (goalSets > 1) (i - 1).toFloat() / (goalSets - 1).toFloat() else 0.5f
        val ang = Math.toRadians((startAngle + t * sweep).toDouble())
        val cx = (arcCenter.x + cos(ang).toFloat() * radius)
        val cy = (arcCenter.y + sin(ang).toFloat() * radius)
        val completed = i < currentSet
        val isCurrent = i == currentSet

        when {
            completed -> drawCircle(color = accent, radius = dotR, center = Offset(cx, cy))
            isCurrent -> {
                val r = dotR * pulse
                drawCircle(color = Color.White, radius = r, center = Offset(cx, cy))
                drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(cx, cy))
            }
            else -> drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(cx, cy))
        }
    }
}




@Composable
private fun distancePillBackground(): Brush = Brush.linearGradient(
    listOf(
        Crimson.copy(alpha = 0.85f),
        Crimson.copy(alpha = 0.65f)
    )
)

@Composable
private fun distanceIconBackground(pressed: Boolean): Brush {
    val start = if (pressed) Crimson.copy(alpha = 0.25f) else Crimson.copy(alpha = 0.20f)
    val end = if (pressed) Crimson.copy(alpha = 0.12f) else Crimson.copy(alpha = 0.10f)
    return Brush.radialGradient(listOf(start, end))
}

@Composable
private fun distanceIconTint(pressed: Boolean): Color {
    val target = if (pressed) Crimson.copy(alpha = 1f) else Crimson.copy(alpha = 0.9f)
    val animated by animateColorAsState(targetValue = target, animationSpec = tween(160, easing = FastOutSlowInEasing), label = "distanceIconTint")
    return animated
}

@Composable
fun DistanceSelector(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val display = remember(value) { String.format("%.1f", value.coerceIn(0.0, 99.9)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Decrement
            val decInteraction = remember { MutableInteractionSource() }
            val decPressed by decInteraction.collectIsPressedAsState()
            val decScale by animateFloatAsState(
                targetValue = if (decPressed) 0.92f else 1f,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "distanceDecScale"
            )
            IconButton(
                onClick = {
                    val next = (value - 1.0).coerceIn(0.0..99.9)
                    onValueChange(next)
                    GoalDistance.value = next
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(44.dp)
                    .scale(decScale)
                    .clip(CircleShape)
                    .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                    .background(distanceIconBackground(decPressed)),
                interactionSource = decInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Decrement $label",
                    tint = distanceIconTint(decPressed)
                )
            }

            // Value pill (editable)
            OutlinedTextField(
                value = display,
                onValueChange = { raw ->
                    val cleaned = raw.replace(',', '.')
                    val parsed = cleaned.toDoubleOrNull()
                    if (parsed != null) {
                        val coerced = parsed.coerceIn(0.0, 99.9)
                        onValueChange(coerced)
                        GoalDistance.value = coerced
                    } else if (raw.isEmpty()) {
                        onValueChange(0.0)
                        GoalDistance.value = 0.0
                    }
                },
                modifier = Modifier
                    .width(110.dp)
                    .heightIn(min = 56.dp)
                    .clip(CircleShape)
                    .background(distancePillBackground()),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Crimson.copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Crimson
                )
            )

            // Increment
            val incInteraction = remember { MutableInteractionSource() }
            val incPressed by incInteraction.collectIsPressedAsState()
            val incScale by animateFloatAsState(
                targetValue = if (incPressed) 0.92f else 1f,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "distanceIncScale"
            )
            IconButton(
                onClick = {
                    val next = (value + 1.0).coerceIn(0.0..99.9)
                    onValueChange(next)
                    GoalDistance.value = next
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(44.dp)
                    .scale(incScale)
                    .clip(CircleShape)
                    .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                    .background(distanceIconBackground(incPressed)),
                interactionSource = incInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = distanceIconTint(incPressed)
                )
            }
        }
    }
}

private fun getWrappedValue(value: Int, delta: Int, range: IntRange): Int {
    if (range.isEmpty()) return 0
    val rangeSize = range.last - range.first + 1
    val relativeValue = value - range.first
    val newRelativeValue = relativeValue + delta
    val mod = ((newRelativeValue % rangeSize) + rangeSize) % rangeSize
    return mod + range.first
}

@Composable
fun DraggableTimeComponent(
    modifier: Modifier = Modifier,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(value) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetY.value + dragAmount
                        scope.launch {  offsetY.snapTo(newOffset)}
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    },
                    onDragEnd = {
                        val steps = (offsetY.value / itemHeightPx).roundToInt()
                        val target = steps * itemHeightPx
                        scope.launch {
                            offsetY.animateTo(target, animationSpec = spring())
                            val newValue = getWrappedValue(value, -steps, range)
                            if (newValue != value) onValueChange(newValue)
                            offsetY.snapTo(0f)
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetY.animateTo(0f, animationSpec = spring()) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val displayRange = -20..20
        for (i in displayRange) {
            val displayValue = getWrappedValue(value, i, range)
            val verticalOffset = (i * itemHeightPx) + offsetY.value
            val distanceRatio = verticalOffset / itemHeightPx
            val scale = 1f - (abs(distanceRatio) * 0.15f).coerceAtMost(0.4f)
            val alpha = 1f - (abs(distanceRatio) * 0.5f).coerceAtMost(1f)
            val rotationX = -20f * distanceRatio.coerceIn(-2f, 2f)

            Text(
                text = String.format("%02d", displayValue),
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        translationY = verticalOffset
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationX = rotationX
                    }
            )
        }
    }
}

@Composable
fun TimerSelector(
    navController: NavController
) {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }

    fun setGoalTime() {
        val hoursInMillis = hours * 3_600_000L
        val minutesInMillis = minutes * 60_000L
        val secondsInMillis = seconds * 1_000L
        GoalTime.value = hoursInMillis + minutesInMillis + secondsInMillis
    }

    LaunchedEffect(hours, minutes, seconds) {
        setGoalTime()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val fieldModifier = Modifier
            .width(80.dp)
            .height(100.dp)

        val separator: @Composable () -> Unit = {
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Text(
            text = "Timer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp)
        )
        if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
            DraggableTimeComponent(
                modifier = fieldModifier,
                value = hours,
                range = 0..99,
                onValueChange = { hours = it }
            )
            separator()
            DraggableTimeComponent(
                modifier = fieldModifier,
                value = minutes,
                range = 0..59,
                onValueChange = { minutes = it }
            )
            separator()
            DraggableTimeComponent(
                modifier = fieldModifier,
                value = seconds,
                range = 0..59,
                onValueChange = { seconds = it }
            )
        }
    }
}


@Composable
fun GoalSelector(
    navController: NavController,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val workoutState by remember { workout }
    val cardioExerciseNames = listOf(
        "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
        "Rowing Machine", "Stationary Bike","Swimming"
    )

    var canShowDistance by remember { mutableStateOf(false) }
    LaunchedEffect(workoutState) {
        canShowDistance = workoutState in cardioExerciseNames
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.secondary.copy(alpha = 0.10f), // Dynamic container color
        ),
        border = BorderStroke(
            1.dp,
            theme.primary.copy(alpha = 0.2f) // Dynamic border color
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                SegmentedButton(
                    text = "Time",
                    isSelected = selectedType == "Time",
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    onClick = { onTypeSelected("Time") },
                    themeColors = theme
                )
            }
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(theme.primary.copy(alpha = 0.3f)) // Dynamic divider
            )
            if (!canShowDistance && ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                SegmentedButton(
                    text = "Sets",
                    isSelected = selectedType == "Reps",
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    onClick = { onTypeSelected("Reps") },
                    themeColors = theme
                )
            } else {
                SegmentedButton(
                    text = "Distance",
                    isSelected = selectedType == "Distance",
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    onClick = { onTypeSelected("Distance") },
                    themeColors = theme
                )
            }
        }
    }
}

@Composable
private fun segmentContainerBrush(selected: Boolean, pressed: Boolean, themeColors: ColorSchemeAppTheme): Brush {
    return if (selected) {
        Brush.linearGradient(
            listOf(
                themeColors.secondary.copy(alpha = if (pressed) 0.95f else 0.90f),
                themeColors.primary.copy(alpha = if (pressed) 0.75f else 0.65f)
            )
        )
    } else {
        val start = if (pressed) themeColors.primary.copy(alpha = 0.10f) else Color.Transparent
        val end = if (pressed) themeColors.secondary.copy(alpha = 0.06f) else Color.Transparent
        Brush.linearGradient(listOf(start, end))
    }
}

@Composable
private fun segmentBorderBrush(selected: Boolean, pressed: Boolean, themeColors: ColorSchemeAppTheme): Brush {
    val hi = if (selected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.20f)
    val lo = if (selected) themeColors.primary.copy(alpha = if (pressed) 0.35f else 0.25f)
    else Color.White.copy(alpha = 0.06f)
    return Brush.linearGradient(listOf(hi, lo))
}

@Composable
private fun segmentTextColor(selected: Boolean, pressed: Boolean): Color {
    val target = when {
        selected -> Color.White
        pressed  -> Color.White.copy(alpha = 0.92f)
        else     -> Color.White.copy(alpha = 0.85f)
    }
    val animated by animateColorAsState(targetValue = target, label = "segmentTextColor")
    return animated
}

@Composable
private fun pressedScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "segmentScale")
    return scale
}

@Composable
private fun isInactive(): Boolean =
    ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE

@Composable
private fun RowScope.SegmentedButtonInternal(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    themeColors: ColorSchemeAppTheme
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val inactive = isInactive()

    val scale = pressedScale(isPressed)
    val bgBrush = segmentContainerBrush(isSelected && inactive, isPressed && inactive, themeColors)
    val border = segmentBorderBrush(isSelected && inactive, isPressed && inactive, themeColors)
    val labelColor = segmentTextColor(isSelected && inactive, isPressed && inactive)

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(brush = bgBrush, shape = shape)
            .border(width = 1.5.dp, brush = border, shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = inactive,
                onClick = {
                    if (inactive) {
                        onClick()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected && inactive) FontWeight.Bold else FontWeight.Medium,
            color = labelColor
        )
    }
}

@Composable
fun RowScope.SegmentedButton(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    themeColors: ColorSchemeAppTheme
) {
    SegmentedButtonInternal(
        text = text,
        isSelected = isSelected,
        shape = shape,
        onClick = onClick,
        themeColors = themeColors
    )
}

@Composable
private fun stepperPillBackground(themeColors: ColorSchemeAppTheme): Brush {
    return Brush.linearGradient(
        listOf(
            themeColors.secondary.copy(alpha = 0.85f),
            themeColors.primary.copy(alpha = 0.55f)
        )
    )
}

@Composable
private fun stepperIconBackground(pressed: Boolean, themeColors: ColorSchemeAppTheme): Brush {
    val start = if (pressed) themeColors.primary.copy(alpha = 0.25f) else themeColors.primary.copy(alpha = 0.1f)
    val end = if (pressed) themeColors.primary.copy(alpha = 0.12f) else themeColors.secondary.copy(alpha = 0.1f)
    return Brush.radialGradient(listOf(start, end))
}

@Composable
private fun stepperIconTint(pressed: Boolean, themeColors: ColorSchemeAppTheme): Color {
    val base = themeColors.primary
    val elevated by animateColorAsState(
        targetValue = if (pressed) base.copy(alpha = 1f) else base.copy(alpha = 0.9f),
        animationSpec = tween(160, easing = FastOutSlowInEasing)
    )
    return elevated
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 0..999
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val haptics = LocalHapticFeedback.current
    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val decInteraction = remember { MutableInteractionSource() }
                val decPressed by decInteraction.collectIsPressedAsState()
                val decScale by animateFloatAsState(
                    targetValue = if (decPressed) 0.92f else 1f,
                    animationSpec = tween(120, easing = FastOutSlowInEasing)
                )

                IconButton(
                    onClick = { /* handled inside combinedClickable */ },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(decScale)
                        .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                        .clip(CircleShape)
                        .background(stepperIconBackground(decPressed, theme))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement $label",
                        tint = stepperIconTint(decPressed, theme),
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = decInteraction,
                                indication = null,
                                onClick = {
                                    onValueChange((value - 1).coerceIn(range))
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onLongClick = {
                                    onValueChange((value - 999_999).coerceIn(range))
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(stepperPillBackground(theme))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                val incInteraction = remember { MutableInteractionSource() }
                val incPressed by incInteraction.collectIsPressedAsState()
                val incScale by animateFloatAsState(
                    targetValue = if (incPressed) 0.92f else 1f,
                    animationSpec = tween(120, easing = FastOutSlowInEasing)
                )

                IconButton(
                    onClick = {
                        onValueChange((value + 1).coerceIn(range))
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(incScale)
                        .clip(CircleShape)
                        .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                        .background(stepperIconBackground(incPressed, theme)),
                    interactionSource = incInteraction
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment $label",
                        modifier = Modifier,
                        tint = stepperIconTint(incPressed, theme)
                    )
                }
            }
        }
    }
}



// The specific list of exercises that trigger the Barbell UI
val barbellVisualExercises = setOf(
    "Barbell Bench Press",
    "Incline Barbell Press",
    "Barbell Back Squat",
    "Front Squat",
    "Deadlifts",
    "Sumo Deadlifts",
    "Romanian Deadlifts",
    "Overhead Press (Barbell)",
    "Push Press",
    "Bent-Over Rows",
    "Pendlay Rows",
    "Good Mornings",
    "Hip Thrusts",
    "Barbell Curls",
    "Barbell Shrugs"
)








@Composable
fun NumberStepperWeights(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    workoutName: String, // Pass workout.value here
    step: Double = 1.0
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    // Check if the current workout is in the barbell list
    val useBarbellVisual = remember(workoutName) {
        barbellVisualExercises.contains(workoutName)
    }

    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
        if (useBarbellVisual) {
            BarbellStyleInput(
                label = label,
                value = value,
                onValueChange = onValueChange,
                theme = theme
            )
        } else {
            StandardStepperInput(
                label = label,
                value = value,
                onValueChange = onValueChange,
                step = step,
                theme = theme
            )
        }
    }
}

// --- Sub-Component: The New Barbell Visual UI ---

@Composable
private fun BarbellStyleInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    theme: ColorSchemeAppTheme
) {
    val haptics = LocalHapticFeedback.current
    val sidePlates = remember(value) { calculateSidePlates(value) }
    val barColor = Color.LightGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = theme.primary.copy(alpha = 0.95f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${String.format("%.1f", value)} kg",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = theme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Barbell Visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Shaft
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )

            // Plates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    sidePlates.reversed().forEach { config -> PlateVisual(config) }
                    Box(Modifier.size(width = 8.dp, height = 25.dp).background(barColor.copy(alpha=0.8f)))
                    Spacer(Modifier.width(10.dp))
                }

                // Center Grip
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .background(barColor.copy(0.6f))
                )

                // Right Side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(width = 8.dp, height = 25.dp).background(barColor.copy(alpha=0.8f)))
                    sidePlates.forEach { config -> PlateVisual(config) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Text(
            text = "Add/Remove Pair",
            style = MaterialTheme.typography.bodySmall,
            color = theme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            standardPlates.forEach { config ->
                PlateControlColumn(
                    config = config,
                    onAdd = {
                        onValueChange(value + (config.weightKg * 2))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onRemove = {
                        val newValue = (value - (config.weightKg * 2)).coerceAtLeast(BAR_WEIGHT)
                        onValueChange(newValue)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    theme = theme
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset
        OutlinedButton(
            onClick = {
                onValueChange(BAR_WEIGHT)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
            border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.5f))
        ) {
            Text("Reset to Empty Bar (${BAR_WEIGHT.toInt()}kg)")
        }
    }
}

// --- Sub-Component: The Old Standard Stepper UI ---

@Composable
private fun StandardStepperInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    theme: ColorSchemeAppTheme
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Decrement Button
            val decInteraction = remember { MutableInteractionSource() }
            val decPressed by decInteraction.collectIsPressedAsState()
            val decScale by animateFloatAsState(
                targetValue = if (decPressed) 0.92f else 1f,
                animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
            IconButton(
                onClick = {
                    val newValue = (value - step).coerceAtLeast(0.0)
                    onValueChange(newValue)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(48.dp)
                    .scale(decScale)
                    .clip(CircleShape)
                    .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                    .background(stepperIconBackground(decPressed, theme)),
                interactionSource = decInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrement $label",
                    tint = stepperIconTint(decPressed, theme)
                )
            }

            // Text Field
            OutlinedTextField(
                value = String.format("%.1f", value.coerceAtLeast(0.0)),
                onValueChange = { raw ->
                    val cleaned = raw.replace(',', '.')
                    val parsed = cleaned.toDoubleOrNull()
                    when {
                        parsed == null && raw.isEmpty() -> onValueChange(0.0)
                        parsed != null -> onValueChange(parsed.coerceAtLeast(0.0))
                    }
                },
                modifier = Modifier
                    .width(120.dp)
                    .heightIn(min = 56.dp)
                    .clip(CircleShape)
                    .background(stepperPillBackground(theme)),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = theme.primary.copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = theme.primary
                )
            )

            // Increment Button
            val incInteraction = remember { MutableInteractionSource() }
            val incPressed by incInteraction.collectIsPressedAsState()
            val incScale by animateFloatAsState(
                targetValue = if (incPressed) 0.92f else 1f,
                animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
            IconButton(
                onClick = {
                    onValueChange((value + step).coerceAtLeast(0.0))
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(48.dp)
                    .scale(incScale)
                    .clip(CircleShape)
                    .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                    .background(stepperIconBackground(incPressed, theme)),
                interactionSource = incInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = stepperIconTint(incPressed, theme)
                )
            }
        }
    }
}

// --- Helper Functions for UI (Colors/Visuals) ---

@Composable
fun PlateVisual(config: PlateConfig) {
    Box(
        modifier = Modifier
            .width(10.dp)
            .height(config.heightDp)
            .clip(RoundedCornerShape(2.dp))
            .background(config.color)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
    )
}

@Composable
fun PlateControlColumn(
    config: PlateConfig,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    theme: ColorSchemeAppTheme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = config.color,
            modifier = Modifier.size(56.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, theme.primary.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = config.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RemoveCircleOutline,
                contentDescription = "Remove pair of ${config.label}",
                tint = theme.secondary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable fun WeightSelector() = NumberStepperWeights("Weight", CurrentWeight.value, workoutName = workout.value ,
    onValueChange = { CurrentWeight.value = it })
@Composable fun RepSelector() = NumberStepper("Reps", GoalReps.intValue, { GoalReps.intValue = it })
@Composable fun SetSelector() = NumberStepper("Sets", GoalSets.intValue, { GoalSets.intValue = it })
@Composable
fun stepperIconBackground(pressed: Boolean, theme: ColorScheme): Color {
    return if (pressed) theme.primary.copy(alpha = 0.3f) else Color.Transparent
}

@Composable
fun stepperIconTint(pressed: Boolean, theme: ColorScheme): Color {
    return if (pressed) Color.White else theme.primary
}

@Composable
fun stepperPillBackground(theme: ColorScheme): Color {
    return theme.surfaceVariant.copy(alpha = 0.3f)
}
data class SetRecord(
    val reps: MutableState<String>,
    val weight: MutableState<String>
)

object WorkoutLog {
    val sets = mutableStateListOf<SetRecord>()
}



@Composable
fun EditableSetRow(
    setNumber: Int,
    record: SetRecord
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Set $setNumber",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetDetailTextField(
                label = "Reps",
                value = record.reps.value,
                onValueChange = { record.reps.value = it },
                modifier = Modifier.width(80.dp)
            )
            SetDetailTextField(
                label = "Weight",
                value = record.weight.value,
                onValueChange = { record.weight.value = it },
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

@Composable
private fun SetDetailTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char == '.' }.take(5)) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier.defaultMinSize(minHeight = 58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
            focusedLabelColor = Color.White.copy(alpha = 0.7f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color(0xFF9BE7FF)
        ),
        textStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}





private data class Particle(
    val color: Color,
    val velocity: Offset,
    val size: Float,
    val rotationSpeed: Float,
    val type: ParticleType
)

private enum class ParticleType { CIRCLE, SQUARE, SHARD }
private enum class ParticlePalette { CRIMSON, GOLDEN }

data class PrFlags(
    val strengthPr: Boolean = false,
    val volumePr: Boolean = false,
    val repsPr: Boolean = false,
    val setsPr: Boolean = false
) {
    val any get() = strengthPr || volumePr || repsPr || setsPr
}

@Composable
fun GoalCompletionAnimation(
    onAnimationFinished: () -> Unit,
    prFlags: PrFlags,
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current

    val animationTime = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0f) }
    val iconRotation = remember { Animatable(-30f) }
    val lightBurst = remember { Animatable(0f) }
    val ornamentProgress = remember { Animatable(0f) }

    val sets = remember { ConnectedWorkout.CurrentSets.intValue.coerceAtLeast(1) }
    val reps = remember { ConnectedWorkout.CurrentReps.intValue.coerceAtLeast(1) }

    val prShockwave = remember { Animatable(0f) }
    val prFlash = remember { Animatable(0f) }
    val prPulse = remember { Animatable(1f) }
    val prTextAlpha = remember { Animatable(0f) }
    val prTextScale = remember { Animatable(0.9f) }

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(Unit) {
        particles = if (prFlags.any) {
            generateParticles(ParticlePalette.GOLDEN, count = 1100)
        } else {
            generateParticles(ParticlePalette.CRIMSON, count = 600)
        }

        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope {
            launch {
                animationTime.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                )
                onAnimationFinished()
            }
            launch {
                lightBurst.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
                lightBurst.animateTo(0f, tween(500))
            }

            if (prFlags.any) {
                launch {
                    delay(60)
                    prFlash.snapTo(1f)
                    prFlash.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(80)
                    prShockwave.snapTo(0f)
                    prShockwave.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(90)
                    repeat(2) {
                        prPulse.animateTo(1.12f, tween(120, easing = FastOutSlowInEasing))
                        prPulse.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                    }
                }
                launch {
                    delay(180)
                    prTextAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(180)
                    prTextScale.snapTo(0.9f)
                    prTextScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 450f))
                }
                launch {
                    delay(120)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(120)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            launch {
                delay(100)
                iconScale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 200f))
            }
            launch {
                delay(100)
                iconRotation.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 150f))
            }
            launch {
                delay(150)
                ornamentProgress.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 100f))
            }
            launch {
                delay(200)
                textAlpha.animateTo(1f, tween(300))
            }
            launch {
                delay(200)
                textScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
            }
            launch {
                delay(100)
                repeat(3) {
                    delay(150)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    val mainText = "WORKOUT\nCOMPLETE"
    val styledText = remember(mainText, theme.primary, theme.secondary) {
        val gradient = Brush.verticalGradient(
            colors = listOf(theme.primary, theme.secondary, theme.primary)
        )
        buildAnnotatedString {
            withStyle(SpanStyle(brush = gradient)) { append(mainText) }
        }
    }

    val prLabel = remember(prFlags) {
        when {
            prFlags.strengthPr && prFlags.volumePr -> "DOUBLE PR"
            prFlags.strengthPr -> "NEW STRENGTH PR"
            prFlags.volumePr -> "NEW VOLUME PR"
            prFlags.repsPr && prFlags.setsPr -> "RECORDS SHATTERED"
            prFlags.repsPr -> "REP RECORD"
            prFlags.setsPr -> "SET RECORD"
            else -> "NEW PR"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = animationTime.value * 2.5f

            if (lightBurst.value > 0f) {
                drawCircle(
                    color = theme.primary.copy(alpha = lightBurst.value * 0.5f),
                    radius = size.maxDimension * lightBurst.value,
                    center = center
                )
            }

            if (prFlags.any && prFlash.value > 0f) {
                drawRect(
                    color = theme.secondary.copy(alpha = 0.18f * prFlash.value),
                    size = size
                )
            }

            if (prFlags.any && prShockwave.value > 0f) {
                val t = prShockwave.value
                val r = size.maxDimension * (0.15f + 0.95f * t)
                val a = (1f - t).coerceIn(0f, 1f)
                drawCircle(
                    color = theme.primary.copy(alpha = 0.55f * a),
                    radius = r,
                    center = center,
                    style = Stroke(width = (10.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                )
            }

            if (time > 0f) {
                particles.forEach { particle ->
                    val gravity = 2000f * density
                    val x = center.x + (particle.velocity.x * density * time)
                    val y = center.y + (particle.velocity.y * density * time) + (0.5f * gravity * time * time)
                    val particleAlpha = (1f - (time / 2.0f)).coerceIn(0f, 1f)

                    if (particleAlpha > 0f) {
                        rotate(degrees = particle.rotationSpeed * time * 100f, pivot = Offset(x, y)) {
                            when (particle.type) {
                                ParticleType.CIRCLE -> drawCircle(
                                    color = particle.color,
                                    center = Offset(x, y),
                                    radius = particle.size * density * particleAlpha,
                                    alpha = particleAlpha
                                )
                                ParticleType.SQUARE -> drawRect(
                                    color = particle.color,
                                    topLeft = Offset(x - particle.size, y - particle.size),
                                    size = Size(particle.size * 2, particle.size * 2),
                                    alpha = particleAlpha
                                )
                                ParticleType.SHARD -> drawLine(
                                    color = particle.color,
                                    start = Offset(x, y),
                                    end = Offset(x + particle.velocity.x * 0.05f, y + particle.velocity.y * 0.05f),
                                    strokeWidth = particle.size * density * 0.5f,
                                    alpha = particleAlpha
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = (-60).dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        val pulse = prPulse.value
                        scaleX = iconScale.value * pulse
                        scaleY = iconScale.value * pulse
                        rotationZ = iconRotation.value
                    }
            ) {
                val w = size.width
                val h = size.height
                val centerX = w / 2
                val centerY = h / 2

                if (ornamentProgress.value > 0f) {
                    val ornamentCount = 8 + (sets * 2).coerceAtMost(24)
                    val maxRadius = (w * 0.4f) + (reps * 2f * density).coerceAtMost(w * 0.3f)
                    val baseRadius = w * 0.25f

                    rotate(degrees = animationTime.value * 20f) {
                        for (i in 0 until ornamentCount) {
                            val angle = (2 * PI / ornamentCount) * i
                            val currentRadius = baseRadius + (maxRadius - baseRadius) * ornamentProgress.value

                            val startX = centerX + cos(angle).toFloat() * baseRadius
                            val startY = centerY + sin(angle).toFloat() * baseRadius
                            val endX = centerX + cos(angle).toFloat() * currentRadius
                            val endY = centerY + sin(angle).toFloat() * currentRadius

                            val spikePath = Path().apply {
                                moveTo(startX, startY)
                                lineTo(endX, endY)
                                lineTo(
                                    centerX + cos(angle + 0.1).toFloat() * (baseRadius + 10f),
                                    centerY + sin(angle + 0.1).toFloat() * (baseRadius + 10f)
                                )
                                close()
                            }

                            val spikeBrush = if (prFlags.any) {
                                Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFDC143C), Color(0xFF8B0000)))
                            }

                            drawPath(path = spikePath, brush = spikeBrush)

                            drawCircle(
                                color = if (prFlags.any) Color(0xFFFFD700) else Color(0xFFFF1744),
                                radius = 3.dp.toPx() * ornamentProgress.value,
                                center = Offset(endX, endY),
                                alpha = ornamentProgress.value
                            )
                        }
                    }
                }

                val crownPath = Path().apply {
                    val cw = w * 0.6f
                    val ch = h * 0.6f
                    val ox = (w - cw) / 2
                    val oy = (h - ch) / 2 + (h * 0.1f)

                    moveTo(ox + cw * 0.2f, oy + ch * 0.7f)
                    lineTo(ox + cw * 0.8f, oy + ch * 0.7f)
                    lineTo(ox + cw * 0.9f, oy + ch * 0.3f)
                    lineTo(ox + cw * 0.65f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.5f, oy + ch * 0.15f)
                    lineTo(ox + cw * 0.35f, oy + ch * 0.5f)
                    lineTo(ox + cw * 0.1f, oy + ch * 0.3f)
                    close()
                }

                val crownBrush = if (prFlags.any) {
                    Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD700), Color(0xFFFFB300)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFD50000), Color(0xFFDC143C), Color(0xFFB71C1C)))
                }

                drawPath(path = crownPath, brush = crownBrush)

                drawPath(
                    path = crownPath,
                    style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round),
                    color = if (prFlags.any) Color(0xFFFFF8E1) else Color(0xFFFF8A80)
                )
            }
        }

        Text(
            text = styledText,
            modifier = Modifier
                .offset(y = 60.dp)
                .graphicsLayer {
                    val pulse = prPulse.value
                    scaleX = textScale.value * pulse
                    scaleY = textScale.value * pulse
                    alpha = textAlpha.value
                },
            textAlign = TextAlign.Center,
            lineHeight = 50.sp,
            style = TextStyle(
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
            )
        )

        if (prFlags.any) {
            Text(
                text = prLabel,
                modifier = Modifier
                    .offset(y = 140.dp)
                    .graphicsLayer {
                        alpha = prTextAlpha.value
                        scaleX = prTextScale.value * prPulse.value
                        scaleY = prTextScale.value * prPulse.value
                    },
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                ),
                color = Color(0xFFFFD700)
            )
        }
    }
}

private fun generateParticles(
    palette: ParticlePalette,
    count: Int = 600
): List<Particle> {
    val rng = Random(System.currentTimeMillis())

    val colors = when (palette) {
        ParticlePalette.CRIMSON -> listOf(
            Color(0xFFDC143C),
            Color(0xFFD50000),
            Color(0xFFFF1744),
            Color(0xFFB71C1C),
            Color(0xFFFF8A80),
            Color.White
        )
        ParticlePalette.GOLDEN -> listOf(
            Color(0xFFFFD700),
            Color(0xFFFFC107),
            Color(0xFFFFE082),
            Color(0xFFFFF8E1),
            Color(0xFFFFB300),
            Color.White
        )
    }

    return List(count) {
        val angle = rng.nextDouble(0.0, 2 * PI)
        val speed = rng.nextFloat() * 1400f + 600f

        val vx = cos(angle).toFloat() * speed * rng.nextFloat()
        val vy = sin(angle).toFloat() * speed * rng.nextFloat() - 1000f

        Particle(
            color = colors.random(rng),
            velocity = Offset(vx, vy),
            size = rng.nextFloat() * 9f + 3f,
            rotationSpeed = (rng.nextFloat() - 0.5f) * 12f,
            type = ParticleType.entries.toTypedArray().random(rng)
        )
    }
}



object ConnectedWorkout{
    enum class WorkoutMode { INACTIVE, ACTIVE, RESTING }
    var currentMode = mutableStateOf(WorkoutMode.INACTIVE)
    var restTimeRemaining = mutableLongStateOf(0L)

    var workout = mutableStateOf("")
    var GoalReps = mutableIntStateOf(0)
    var GoalSets = mutableIntStateOf(0)
    var CurrentReps = mutableIntStateOf(0)
    var CurrentSets = mutableIntStateOf(0)
    var CurrentTime = mutableStateOf(0L)
    var GoalTime = mutableStateOf(0L)
    var GoalDistance = mutableStateOf(0.0)
    var currentDistance = mutableStateOf(0.0)
    var CurrentWeight = mutableStateOf(0.0)
    var GoalType = "Reps"
    val workoutGoalTypeMap = mutableMapOf<String, String>()
    var selectedWorkout: MutableState<String> = mutableStateOf("")
    var restTime = mutableLongStateOf(60000L)
    var interHour = mutableIntStateOf(0)
    var healthConnectEnabled = mutableStateOf(false)
    var interMinute = mutableIntStateOf(0)
    var interSecond = mutableIntStateOf(0)
}
data class PlateConfig(
    val weightKg: Double,
    val color: Color,
    val heightDp: Dp,
    val label: String
)

// Standard Olympic Plate colors and relative sizes
val standardPlates = listOf(
    PlateConfig(20.0, Color(0xFFCE2B37), 90.dp, "20"), // Red
    PlateConfig(10.0, Color(0xFF005BD6), 80.dp, "10"), // Blue
    PlateConfig(5.0, Color(0xFFF1C40F), 70.dp, "5"),   // Yellow
    PlateConfig(2.5, Color(0xFF2ECC71), 60.dp, "2.5"), // Green
    PlateConfig(1.25, Color(0xFF95A5A6), 50.dp, "1.25"),
            PlateConfig(0.5, Color(0xFF6C6C6C), 50.dp, "0.5")
// Grey/White
)

val BAR_WEIGHT = 20.0

// Helper to determine which plates are on one side based on total weight
fun calculateSidePlates(totalWeight: Double): List<PlateConfig> {
    var remainingWeightPerSide = ((totalWeight - BAR_WEIGHT).coerceAtLeast(0.0)) / 2.0
    val plates = mutableListOf<PlateConfig>()

    // Greedy algorithm: fit biggest plates first
    standardPlates.forEach { plateConfig ->
        // Using a small epsilon for floating point comparison safety
        while (remainingWeightPerSide >= plateConfig.weightKg - 0.01) {
            plates.add(plateConfig)
            remainingWeightPerSide -= plateConfig.weightKg
        }
    }
    return plates
}
@Composable
fun StormyAtmosphere(
    modifier: Modifier = Modifier,
    stormIntensity: Float = 1f,
    rainColor: Color = Color(0xFFAAAAAA),
    lightningColor: Color = Color(0xFFDDEEFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain_loop")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val lightningAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 12000))

            val flashType = Random.nextInt(3)

            if (flashType == 0) {
                lightningAlpha.animateTo(0.6f, tween(50, easing = LinearEasing))
                lightningAlpha.animateTo(0f, tween(300, easing = EaseOutQuad))
            } else if (flashType == 1) {
                lightningAlpha.animateTo(0.3f, tween(50))
                lightningAlpha.animateTo(0.1f, tween(50))
                lightningAlpha.animateTo(0.8f, tween(50))
                lightningAlpha.animateTo(0f, tween(800, easing = EaseOutCubic))
            } else {
                lightningAlpha.animateTo(0.2f, tween(100))
                lightningAlpha.animateTo(0f, tween(500))
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f }
    ) {
        val w = size.width
        val h = size.height

        val baseRainCount = 60
        val count = (baseRainCount * stormIntensity).toInt().coerceAtLeast(10)

        val flash = lightningAlpha.value

        drawRect(
            color = lightningColor.copy(alpha = flash * 0.15f),
            size = size
        )

        for (i in 0 until count) {
            val seed = (i * 1367.123f)

            val layer = (seed % 3).toInt()
            val speedBase = 0.5f + (seed % 0.5f)
            val layerSpeedMult = when(layer) {
                0 -> 0.6f
                1 -> 0.85f
                else -> 1.1f
            }

            val fallSpeed = h * (speedBase * layerSpeedMult)
            val cycleOffset = seed % 1f

            val progress = (time + cycleOffset) % 1f
            val y = progress * (h + 100f) - 50f

            val wind = sin(time * 6.28f + seed) * 10f
            val xBase = (seed * 97.531f) % w
            val x = xBase + (y * 0.1f) + wind

            val dropLength = 15f * layerSpeedMult * (1f + flash * 0.5f)

            val baseAlpha = when(layer) {
                0 -> 0.15f
                1 -> 0.35f
                else -> 0.6f
            }

            val finalAlpha = (baseAlpha + flash * 0.4f).coerceIn(0f, 1f)

            val strokeWidth = when(layer) {
                0 -> 1f
                1 -> 2f
                else -> 2.5f
            }

            drawLine(
                color = rainColor.copy(alpha = finalAlpha),
                start = Offset(x, y),
                end = Offset(x - (wind * 0.1f), y + dropLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            if (layer == 2 && y > h - 50f && y < h) {
                val splashRadius = (y % 4f) + 1f
                drawCircle(
                    color = rainColor.copy(alpha = finalAlpha * 0.5f),
                    radius = splashRadius,
                    center = Offset(x, y + dropLength)
                )
            }
        }

        if (flash > 0.05f) {
            val glowSize = size.maxDimension * (0.8f + flash * 0.4f)
            drawCircle(
                color = lightningColor.copy(alpha = flash * 0.1f),
                radius = glowSize,
                center = Offset(w * 0.5f, h * 0.3f)
            )
        }
    }
}
@Composable
fun DiscoAtmosphere(
    modifier: Modifier = Modifier,
    riseEffectProgress: Float = 0f, // Controls visibility and intensity
    partyingIntensity: Float = 1f, // Similar to 'hype', controls speed/wobble
    themeColors: List<Color> = listOf(
        Color(0xFF6200EE), // Primary-ish
        Color(0xFF03DAC6), // Secondary-ish
        Color(0xFFBB86FC), // Tertiary-ish
        Color(0xFF3700B3)  // Deep variant
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disco_loop")

    // Very slow, infinite time loop for organic movement
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing), // 20 seconds per cycle for slow drift
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Global alpha fade based on rise progress, similar to the original snippet
                alpha = (0.1f + 0.9f * riseEffectProgress).coerceIn(0f, 1f) * riseEffectProgress
            }
    ) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension

        // Ensure we have colors to work with
        val safeColors = if (themeColors.isEmpty()) listOf(Color.Magenta, Color.Cyan) else themeColors
        val orbCount = 5

        for (i in 0 until orbCount) {
            val seed = i * 452.123f
            val color = safeColors[i % safeColors.size]

            // Calculate organic movement using sine/cosine interactions
            val speed = 0.5f + (seed % 0.5f) * partyingIntensity
            val phase = (time * speed + (seed * 0.1f)) % 1f

            // Movement logic: Lissajous-like organic paths
            // We use 'partyingIntensity' to slightly amplify the movement range if desired
            val xWobble = sin(time * 6.28318f * speed + seed)
            val yWobble = cos(time * 6.28318f * (speed * 0.8f) + seed + 1f)

            val baseX = w * (0.2f + 0.6f * ((seed * 13f) % 1f)) // Random-ish start X within center area
            val baseY = h * (0.2f + 0.6f * ((seed * 7f) % 1f))  // Random-ish start Y within center area

            // The orbs float around their base position
            val x = baseX + xWobble * (w * 0.25f)
            val y = baseY + yWobble * (h * 0.25f)

            // Radius breathes slightly
            val baseRadius = minDim * (0.2f + 0.15f * (seed % 1f))
            val r = baseRadius * (0.9f + 0.2f * sin(time * 10f + seed))

            // Alpha calculation based on the requested logic
            // "only appear based on the riseEffectProgress"
            // We blend the individual orb alpha with the global riseEffectProgress
            val pulse = 0.5f + 0.5f * sin(time * 3f + seed)
            val orbAlpha = (0.3f + 0.5f * pulse) * riseEffectProgress

            // Gradient for that "disco" feel - lighter in center, fading out
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = orbAlpha.coerceIn(0f, 1f)),
                    color.copy(alpha = (orbAlpha * 0.4f).coerceIn(0f, 1f)),
                    color.copy(alpha = 0f)
                ),
                center = Offset(x, y),
                radius = r
            )

            drawCircle(
                brush = gradientBrush,
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}
object showSyncDialog {
    var showSyncDialog = mutableStateOf(false)
}
val Crimson = Color(0xFFB00020)

val CrimsonDark = Color(0xFF660011)

val CrimsonLight = Color(0xFFFF5370)

val CrimsonDull = Color(0xFF442226)

private val CrimsonBorderLocked = Color(0x33FF5370)
private data class ParticleData(
    val s: Float,
    val speed: Float,
    val baseXRatio: Float,
    val wobbleSpeed: Float,
    val wobbleOffset: Float,
    val baseRadius: Float,
    val wobbleMagnitudeBase: Float,
    val wobbleMagnitudeExtra: Float
)
data class PrResult(
    val isStrengthPr: Boolean,
    val isVolumePr: Boolean,
    val prevBestE1rm: Float?,
    val prevBestVolume: Float?
)

private fun epley1RM(weight: Float, reps: Int): Float {
    if (weight <= 0f || reps <= 0) return 0f
    return weight * (1f + reps / 30f)
}

fun checkPrForExercise(
    allWorkouts: List<Workout>,
    exerciseName: String,
    newWeight: Float,
    newReps: Int,
    newSets: Int
): PrResult {
    // Guard against garbage input
    if (newWeight <= 0f || newReps <= 0 || newSets <= 0) {
        return PrResult(
            isStrengthPr = false,
            isVolumePr = false,
            prevBestE1rm = null,
            prevBestVolume = null
        )
    }

    val newE1rm = epley1RM(newWeight, newReps)
    val newVolume = newWeight * newReps * newSets

    val previous = allWorkouts
        .asSequence()
        .filter { it.name == exerciseName }
        .mapNotNull { w ->
            val weight = w.weight?.toFloat()
            val reps = w.reps
            val sets = w.sets

            // Skip invalid or incomplete entries
            if (weight == null || weight <= 0f || reps!! <= 0 || sets!! <= 0) {
                null
            } else {
                Triple(weight, reps, sets)
            }
        }
        .toList()

    val bestPrevE1rm = previous.maxOfOrNull { (weight, reps, _) ->
        epley1RM(weight, reps)
    }

    val bestPrevVolume = previous.maxOfOrNull { (weight, reps, sets) ->
        weight * reps * sets
    }

    val isStrengthPr = bestPrevE1rm == null || newE1rm > bestPrevE1rm
    val isVolumePr = bestPrevVolume == null || newVolume > bestPrevVolume

    return PrResult(
        isStrengthPr = isStrengthPr,
        isVolumePr = isVolumePr,
        prevBestE1rm = bestPrevE1rm,
        prevBestVolume = bestPrevVolume
    )
}
