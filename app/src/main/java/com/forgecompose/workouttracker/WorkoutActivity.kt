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
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentTime
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.GoalReps
import com.forgecompose.workouttracker.ConnectedWorkout.GoalSets
import com.forgecompose.workouttracker.ConnectedWorkout.GoalTime
import com.forgecompose.workouttracker.ConnectedWorkout.GoalType
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.workouttracker.ConnectedWorkout.GoalDistance
import com.forgecompose.workouttracker.ConnectedWorkout.currentDistance
import com.forgecompose.workouttracker.ConnectedWorkout.interHour
import com.forgecompose.workouttracker.ConnectedWorkout.interMinute
import com.forgecompose.workouttracker.ConnectedWorkout.interSecond
import com.forgecompose.workouttracker.ConnectedWorkout.restTimeRemaining
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class WorkoutActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = application as MyApplication
        val workoutRepository = application.workoutRepository
        val factory = WorkoutListViewModelFactory(workoutRepository)
        val workoutListViewModel: WorkoutListViewModel by viewModels { factory }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        setContent {
            MainScreen(viewModel = workoutListViewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(viewModel: WorkoutListViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentMode by ConnectedWorkout.currentMode

    // This effect now controls the service lifecycle for the entire workout session
    LaunchedEffect(currentMode) {
        when (currentMode) {
            WorkoutMode.ACTIVE, WorkoutMode.RESTING -> WorkoutForegroundService.start(context)
            WorkoutMode.INACTIVE -> WorkoutForegroundService.stop(context)
        }
    }

    NavHost(navController = navController, startDestination = "GoalScreen") {
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
    val glowTransition = rememberInfiniteTransition(label = "adviceGlow")
    val glow by glowTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val starRotation by glowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )

    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = Color(0xFF8B0000),
                spotColor = Color(0xFF8B0000)
            ),
        shape = cardShape,
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFFF5555).copy(alpha = 0.4f * glow),
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
                            Color(0xFF3A0E0E).copy(alpha = 0.35f * glow),
                            Color(0xFF120707).copy(alpha = 0.85f)
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
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
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
fun CircularTimerProgressBar(
    progress: Float,
    hype: Float,
    modifier: Modifier = Modifier
) {
    // ---- Milestone ripple & haptics ----
    val STEP = 0.25f
    val lastStepIdx = remember { mutableIntStateOf(-1) }
    val lastProgress = remember { mutableStateOf(0f) }
    val ripple = remember { Animatable(0f) } // 0..1
    val haptics = LocalHapticFeedback.current

    // Fire ripple when crossing the next step going forward; give a gentle haptic tap
    LaunchedEffect(progress) {
        val pNow = progress.coerceIn(0f, 1f)
        val pPrev = lastProgress.value
        lastProgress.value = pNow

        // Only react on forward progress and step boundary crossing
        if (pNow > pPrev + 1e-4f) {
            val idx = (pNow / STEP).toInt()
            if (idx > lastStepIdx.intValue) {
                lastStepIdx.intValue = idx
                // Haptic ping
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                // Ripple anim (quick expand + fade)
                launch {
                    ripple.snapTo(0f)
                    ripple.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                    )
                    ripple.snapTo(0f)
                }
            }
        }
    }

    // ---- Drawing ----
    Canvas(modifier = modifier) {
        val strokeWidth = 20.dp.toPx()
        val p = progress.coerceIn(0f, 1f)

        inset(strokeWidth / 2f) {
            val c = center
            val radius = size.minDimension / 2f

            // Track ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0E0E10), Color(0xFF1A1A1F)), // neutral, not red
                    center = c,
                    radius = radius + strokeWidth / 2f
                ),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Ticks
            for (i in 0 until 60) {
                val angle = i * 6f
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val tickColor = Color.White.copy(alpha = if (isMajor) 0.35f else 0.18f)
                val startR = radius - strokeWidth / 2f
                val endR = startR + tickLen
                withTransform({ rotate(angle, c) }) {
                    drawLine(
                        color = tickColor,
                        start = Offset(c.x, c.y - startR),
                        end = Offset(c.x, c.y - endR),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }


            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )


            if (p > 0f) {
                val sweep = 360f * p
                val arcTopLeft = Offset(c.x - radius, c.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)


                drawArc(
                    color = Color.White.copy(alpha = 0.08f + 0.07f * hype.coerceIn(0f, 1f)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )


                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF7979),
                            Color(0xFFFF3232),
                            Color(0xFFFF0000)
                        ),
                        center = c
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // End-cap highlight
                val angleRad = Math.toRadians((sweep - 90f).toDouble()).toFloat()
                val capCenter = Offset(
                    x = c.x + radius * kotlin.math.cos(angleRad),
                    y = c.y + radius * kotlin.math.sin(angleRad)
                )
                val capHaloR = strokeWidth * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                        center = capCenter,
                        radius = capHaloR
                    ),
                    radius = capHaloR,
                    center = capCenter
                )
                drawCircle(color = Color.White, radius = strokeWidth / 3f, center = capCenter)
            }


            val rv = ripple.value
            if (rv > 0f) {

                val startR = radius + strokeWidth * 0.1f
                val endR = radius * 1.6f
                val ringR = lerp(startR, endR, rv)


                val ringW = lerp(strokeWidth * 0.8f, strokeWidth * 0.2f, rv)


                val alpha = (1f - rv) * 0.35f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = ringR,
                    style = Stroke(width = ringW, cap = StrokeCap.Round)
                )
            }
        }
    }
}


private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}




@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(viewModel: WorkoutListViewModel, navController: NavController, vm: HrPhoneViewModel = viewModel()) {
    val context = LocalContext.current
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
    var lastStepTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lastStepTimestamp) {
        if (lastStepTimestamp > 0) {
            isStepping = true
            while (SystemClock.uptimeMillis() - lastStepTimestamp < 1500) {
                delay(100)
            }
            isStepping = false
        }
    }

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
    val (advice, _, generateAdvice) = useGeminiAdviceGenerator(contextPrompt = fitnessContext)

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
                        WellnessAI.WellnessLogger.logWorkout(
                            durationMinutes = CurrentTime.value.toInt(),
                            intensity = 10f,
                            type = "Strength",
                            injury = false
                        )
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
                    WellnessAI.WellnessLogger.logWorkout(
                        durationMinutes = CurrentTime.value.toInt(),
                        intensity = 10f,
                        type = "Cardio",
                        injury = false
                    )
                    GoalDistance.value = 0.0
                    currentDistance.value = 0.0
                }
            }
            else -> {
                if (CurrentTime.value >= GoalTime.value) {
                    hours = 0
                    CurrentTime.value = 0
                    minutes = 0
                    seconds = 0
                    showCompletionAnimation = true
                    isPaused = false
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
        var isAccelRegistered = false
        var isStepRegistered = false
        var distanceMeters = 0.0
        var gx = 0.0; var gy = 0.0; var gz = 0.0
        val tauSec = 0.8
        var mean = 0.0; var meanSq = 0.0
        val beta = 0.02
        var sampleCount = 0
        var prev2 = 0.0; var prev1 = 0.0
        var lastValley = Double.POSITIVE_INFINITY
        var lastTimestampNs: Long? = null
        var lastStepTimeNs = 0L
        val minStepNs = 250_000_000L
        val maxStepNs = 2_000_000_000L
        var lastEventUptimeMs = SystemClock.uptimeMillis()
        var lastPushValue = Double.NaN
        var lastPushMs = 0L
        var lastHwStepNs = 0L
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try { pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wt:step")?.apply { setReferenceCounted(false); acquire() } } catch (_: Throwable) { null }
        fun fastRound3(x: Double): Double {
            if (!x.isFinite()) return Double.NaN
            val t = x * 1000.0
            return kotlin.math.round(t) / 1000.0
        }
        fun safeInc(meters: Double) {
            if (meters.isFinite()) {
                val nm = distanceMeters + meters
                if (nm.isFinite() && nm >= 0.0) distanceMeters = nm
            }
        }
        fun pushUpdate(force: Boolean = false) {
            val km = distanceMeters / 1000.0
            val rounded = fastRound3(km)
            val now = SystemClock.uptimeMillis()
            if (!rounded.isFinite()) return
            if (force || rounded != lastPushValue || now - lastPushMs >= 300L) {
                lastPushValue = rounded
                lastPushMs = now
                mainHandler.post {
                    try { onUpdate(rounded) } catch (_: Throwable) {}
                }
            }
        }
        val accelListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                lastEventUptimeMs = SystemClock.uptimeMillis()
                try {
                    e ?: return
                    val vals = e.values ?: return
                    if (vals.size < 3) return
                    val ts = e.timestamp
                    val dtNs = lastTimestampNs?.let { val d = ts - it; if (d > 0L) d else 20_000_000L } ?: 20_000_000L
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
                    mean = (1 - beta) * mean + beta * mag
                    meanSq = (1 - beta) * meanSq + beta * (mag * mag)
                    sampleCount++
                    val variance = (meanSq - mean * mean).let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    val sigma = kotlin.math.sqrt(variance).let { if (it.isFinite()) it else 0.0 }
                    val dynamicThreshold = max(1.05, (mean + 1.15 * sigma).coerceIn(0.8, 20.0))
                    val curr = mag
                    if (curr < prev1) lastValley = min(lastValley, curr)
                    val isPeak = (sampleCount > 25) && prev1 > prev2 && prev1 > curr && prev1 > dynamicThreshold
                    if (isPeak) {
                        val dtSinceLast = ts - lastStepTimeNs
                        if (lastStepTimeNs == 0L) {
                            lastStepTimeNs = ts
                            lastValley = curr
                        } else if (dtSinceLast in minStepNs..maxStepNs) {
                            val suppressByHw = (ts - lastHwStepNs) in 0L..150_000_000L
                            if (!suppressByHw) {
                                mainHandler.post(onStepDetected)
                                val amplitude = (prev1 - lastValley).coerceAtLeast(0.0)
                                val k = 0.52
                                val stepLen = (k * (min(amplitude, 12.0).pow(0.25))).coerceIn(0.45, 0.9)
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
                } catch (_: Throwable) {}
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val stepListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                lastEventUptimeMs = SystemClock.uptimeMillis()
                try {
                    e ?: return
                    val v = e.values
                    if (v.isEmpty()) return
                    if (v[0] == 1f) {
                        mainHandler.post(onStepDetected)
                        val ts = e.timestamp
                        lastHwStepNs = ts
                        safeInc(0.72)
                        pushUpdate()
                    }
                } catch (_: Throwable) {}
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        fun registerAll() {
            try {
                isAccelRegistered = sensorManager.registerListener(
                    accelListener,
                    accel,
                    SensorManager.SENSOR_DELAY_GAME,
                    0,
                    sensorHandler
                )
            } catch (_: Throwable) { isAccelRegistered = false }
            try {
                isStepRegistered = stepDetector?.let {
                    sensorManager.registerListener(
                        stepListener,
                        it,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        0,
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
        val watchdog = object : Runnable {
            override fun run() {
                try {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastEventUptimeMs > 5_000L) {
                        unregisterAll()
                        registerAll()
                        lastEventUptimeMs = now
                        pushUpdate(force = true)
                    }
                } catch (_: Throwable) {
                } finally {
                    try { sensorHandler.postDelayed(this, 5_000L) } catch (_: Throwable) {}
                }
            }
        }
        try {
            registerAll()
            try { sensorHandler.postDelayed(watchdog, 5_000L) } catch (_: Throwable) {}
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

    fun SetsGoalSafetyCheck() {
        when (GoalType) {
            "Reps" -> if (GoalReps.intValue <= 0 && !showCompletionAnimation) {
                Toast.makeText(context, "Reps must be greater than 0", Toast.LENGTH_SHORT).show()
                navController.navigate("GoalScreen") { popUpTo("GoalScreen") { inclusive = true } }
            }
            "Distance" -> if (GoalDistance.value <= 0 && !showCompletionAnimation) {
                Toast.makeText(context, "Distance must be greater than 0", Toast.LENGTH_SHORT).show()
            }
            else -> if (GoalTime.value <= 0  && !showCompletionAnimation) {
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
    val hype by animateFloatAsState(targetValue = easeOutExpo(unified), label = "hype")

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

    LaunchedEffect(Unit) {
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

    val basePhase = remember { Animatable(0f) }
    val corePhase = remember { Animatable(0f) }
    fun basePeriod(h: Float) = (10000 - (4200 * h)).toInt().coerceAtLeast(1200)
    fun corePeriod(h: Float) = (5000 - (2600 * h)).toInt().coerceAtLeast(600)

    LaunchedEffect(Unit) {
        while (isActive) {
            basePhase.snapTo(0f)
            basePhase.animateTo(1f, tween(durationMillis = basePeriod(hype), easing = LinearEasing))
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            corePhase.snapTo(0f)
            corePhase.animateTo(1f, tween(durationMillis = corePeriod(hype), easing = LinearEasing))
        }
    }

    fun sin01(t: Float) = ((sin(t * 2f * Math.PI).toFloat()) * 0.5f + 0.5f)
    val basePulse = remember(hype, basePhase.value) { 0.55f + 0.45f * sin01(basePhase.value) * (0.6f + 0.4f * hype) }
    val corePulse = remember(hype, corePhase.value) { 0.35f + 0.65f * sin01(corePhase.value) * (0.5f + 0.5f * hype) }
    val centerX = 0.5f + (0.06f + 0.10f * hype) * sin(basePhase.value * 2f * Math.PI).toFloat()
    val centerY = 0.55f + (0.05f + 0.08f * hype) * cos(basePhase.value * 2f * Math.PI).toFloat()
    val baseRadius = 1400f * (1.05f - 0.35f * basePulse)
    val coreRadius = 760f * (1.10f - 0.45f * corePulse)
    val deep = Color(0xFF0D0404)
    val ember = Color(0xFF2A0D0D).copy(alpha = 0.65f + 0.25f * hype)
    val flame = Color(0xFFB71C1C).copy(alpha = 0.35f + 0.35f * corePulse)
    val whiteHot = Color(0xFFFF6D6D).copy(alpha = 0.15f + 0.25f * corePulse)
    val baseBrush = Brush.radialGradient(
        colors = listOf(ember, deep),
        center = Offset(centerX, centerY),
        radius = baseRadius
    )
    val coreBrush = Brush.radialGradient(
        colors = listOf(whiteHot, flame, Color.Transparent),
        center = Offset(
            0.5f + (0.10f + 0.12f * hype) * sin(corePhase.value * 4f * Math.PI).toFloat(),
            0.60f + (0.08f + 0.10f * hype) * cos(corePhase.value * 4f * Math.PI).toFloat()
        ),
        radius = coreRadius
    )
    val shimmerPhase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            shimmerPhase.snapTo(0f)
            shimmerPhase.animateTo(1f, tween((5000 - 2400 * hype).toInt().coerceAtLeast(400), 300))
        }
    }
    val shimmerAlpha = 0.05f + 0.10f * hype

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
                        containerColor = Color(0xFF1A0808).copy(alpha = 0.70f + 0.20f * hype),
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
                    .background(baseBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(coreBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = shimmerAlpha }
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.14f + 0.10f * hype),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                start = Offset(shimmerPhase.value * -800f, 0f),
                                end = Offset(800f - shimmerPhase.value * 800f, 1600f)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showCompletionAnimation) {
                        GoalCompletionAnimation(
                            onAnimationFinished = {
                                viewModel.addSampleWorkout(
                                    workout.value,
                                    WorkoutStatus.COMPLETED,
                                    timeMillis,
                                    CurrentWeight.value
                                )
                                ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                                context.startActivity(intent)
                                activity?.finish()
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
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val timerScale by animateFloatAsState(1f + 0.06f * corePulse, label = "timerScale")
                        Text(
                            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { scaleX = timerScale; scaleY = timerScale }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (GoalType == "Reps") {
                        SetProgressDetails(
                            currentReps = CurrentReps.intValue,
                            goalReps = GoalReps.intValue,
                            currentSet = CurrentSets.intValue,
                            goalSets = GoalSets.intValue
                        )
                    } else if (GoalType == "Distance") {
                        Text(
                            text = "Distance: ${"%.2f".format(currentDistance.value)} / ${"%.2f".format(GoalDistance.value)} km",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgressDistance },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFFFF5A5A).copy(alpha = 0.50f + 0.45f * hype),
                            trackColor = Color.Black.copy(alpha = 0.22f),
                            strokeCap = StrokeCap.Round,
                        )
                    } else {
                        CircularTimerProgressBar(
                            progress = animatedProgress,
                            hype = hype,
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    val aiEnabled = dynamicModel.personaConfig.value.enabled
                    if (aiEnabled) {
                        AdviceSection(
                            advice = advice,
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = false
                        )
                    }
                    val bpm by vm.bpm.collectAsState()
                    Spacer(modifier = Modifier.weight(1f))

                    if (GoalType == "Reps") {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "buttonScale")
                        val animatedBg by animateColorAsState(
                            targetValue =
                                if (isPressed)
                                    Color(0xFF8B0000).copy(alpha = (0.70f + 0.22f * hype).coerceIn(0f, 1f))
                                else
                                    Color(0xFF650000).copy(alpha = (0.45f + 0.30f * hype).coerceIn(0f, 1f)),
                            label = "btnBg"
                        )
                        val shadow by animateDpAsState(targetValue = if (isPressed) 8.dp else 4.dp, label = "btnShadow")

                        LaunchedEffect(Unit) {
                            while (true) {
                                val uVal =
                                    if (CurrentWeight.value > 0) "current weight is ${CurrentWeight.value}Kg"
                                    else if (CurrentTime.value < 0) "current distance walked or ran is ${currentDistance.value}km"
                                    else "current time elapsed is ${CurrentTime.value}"
                                generateAdvice(
                                    "so far the user has performed ${CurrentReps.intValue} and their goal is to reach ${GoalReps.intValue} ",
                                    "current sets are ${CurrentSets.intValue} and the goal is ${GoalSets.intValue}" +
                                            "Increase encouragement as they get closer to it",
                                    uVal
                                )
                                delay(60000)
                            }
                        }

                        Button(
                            onClick = {
                                timeToMillis()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                CurrentReps.intValue += 10
                                CurrentSets.intValue += 1
                                EnterRestMode()
                            },
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    shadowElevation = shadow.toPx()
                                }
                                .border(
                                    2.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFF7A7A).copy(alpha = (0.65f + 0.30f * hype).coerceIn(0f, 1f)),
                                            Color(0xFF4A1515).copy(alpha = (0.45f + 0.35f * hype).coerceIn(0f, 1f))
                                        )
                                    ),
                                    CircleShape
                                ),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = Color.White),
                            elevation = null
                        ) {
                            Text("Finish Set", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val now = SystemClock.elapsedRealtime()
                                if (!isPaused) {
                                    accMs += now - startAt.longValue
                                } else {
                                    startAt.longValue = now
                                }
                                isPaused = !isPaused
                                pauseText = if (isPaused) "Resume" else "Pause"
                            },
                            modifier = Modifier.animateContentSize(tween(300)),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A2515).copy(alpha = 0.5f + 0.15f * hype),
                                contentColor = Color.White
                            )
                        ) { Text(pauseText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = {
                                timeToMillis()
                                ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                                scope.launch(Dispatchers.IO) {
                                    viewModel.addSampleWorkout(
                                        workout.value, WorkoutStatus.SKIPPED,
                                        CurrentTime.value, CurrentWeight.value
                                    )
                                }
                                CurrentReps.intValue = 0
                                CurrentSets.intValue = 0
                                hours = 0
                                minutes = 0
                                seconds = 0
                                accMs = 0L
                                startAt.longValue = SystemClock.elapsedRealtime()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch(Dispatchers.Main) {
                                    context.startActivity(intent)
                                    activity?.finish()
                                }
                            },
                            modifier = Modifier.animateContentSize(tween(300)),
                            enabled = isPaused,
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B0000).copy(alpha = 0.7f),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2A0D0D).copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        ) { Text("End Workout", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
    goalSets: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Reps", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    text = if (goalReps > 0) "$currentReps / $goalReps" else currentReps.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Set", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    "$currentSet / $goalSets",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DetailedSetsProgressBar(
            currentSet = currentSet,
            goalSets = goalSets
        )
    }
}

@Composable
fun DetailedSetsProgressBar(currentSet: Int, goalSets: Int, modifier: Modifier = Modifier) {
    if (goalSets <= 0) return
    val progressTarget = when {
        goalSets <= 1 -> if (currentSet >= 1) 1f else 0f
        else -> ((currentSet - 1).toFloat() / (goalSets - 1).toFloat()).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        label = "SetProgressBarProgress",
        animationSpec = tween(500, easing = FastOutSlowInEasing)
    )
    val shimmerPosition = remember { Animatable(-0.2f) }
    LaunchedEffect(Unit) {
        shimmerPosition.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = 1600,
                delayMillis = 300,
                easing = LinearEasing
            )
        )
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        val yCenter = size.height / 2
        val trackStrokeWidth = 4.dp.toPx()
        val progressStrokeWidth = 6.dp.toPx()
        val dotRadius = 6.dp.toPx()
        val startPadding = dotRadius
        val endPadding = dotRadius
        val drawableWidth = size.width - startPadding - endPadding
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(startPadding, yCenter),
            end = Offset(startPadding + drawableWidth, yCenter),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )
        val shimmerProgress = shimmerPosition.value
        val shimmerWidth = drawableWidth * 0.4f
        val shimmerStartX = (drawableWidth + shimmerWidth) * shimmerProgress - shimmerWidth + startPadding
        val shimmerBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.2f),
                Color.Transparent
            ),
            start = Offset(shimmerStartX, yCenter),
            end = Offset(shimmerStartX + shimmerWidth, yCenter)
        )
        drawLine(
            brush = shimmerBrush,
            start = Offset(startPadding, yCenter),
            end = Offset(startPadding + drawableWidth, yCenter),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )
        if (animatedProgress > 0) {
            drawLine(
                color = Color(0xFF8B0000),
                start = Offset(startPadding, yCenter),
                end = Offset(startPadding + (drawableWidth * animatedProgress), yCenter),
                strokeWidth = progressStrokeWidth,
                cap = StrokeCap.Round
            )
        }
        (1..goalSets).forEach { i ->
            val dotX = if (goalSets > 1) {
                startPadding + (drawableWidth * ((i - 1).toFloat() / (goalSets - 1)))
            } else {
                size.width / 2
            }
            val isCompleted = i < currentSet
            val isCurrent = i == currentSet
            val dotColor = when {
                isCompleted -> Color(0xFF8B0000)
                isCurrent -> Color.White
                else -> Color.White.copy(alpha = 0.4f)
            }
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(dotX, yCenter)
            )
            if (isCurrent) {
                drawCircle(
                    color = Color(0xFF8B0000),
                    radius = dotRadius,
                    center = Offset(dotX, yCenter),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(navController: NavController, viewModel: WorkoutListViewModel) {
    val workoutState by remember { workout }
    var selectedGoalType by remember(workoutState) {
        mutableStateOf(ConnectedWorkout.workoutGoalTypeMap[workoutState] ?: "Time")
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "buttonScale")
    GoalType = selectedGoalType
    val infiniteTransition = rememberInfiniteTransition(label = "goalBackgroundAnimation")
    val intensePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "intensePulse"
    )
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    var repsPerSet by remember { mutableIntStateOf(GoalReps.intValue.takeIf { it > 0 } ?: 10) }
    val totalGoalReps by derivedStateOf { repsPerSet * GoalSets.intValue }

    LaunchedEffect(Unit) {
        if (ConnectedWorkout.currentMode.value == WorkoutMode.ACTIVE){
            navController.navigate("WorkoutScreen")
        } else if (ConnectedWorkout.currentMode.value == WorkoutMode.RESTING){
            navController.navigate("RestScreen")
        }
    }

    WorkoutTrackerTheme {
        val aggressiveGradientBrush = remember(gradientOffset, glowIntensity, intensePulse) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A0808),
                    Color(0xFF4A1515).copy(alpha = 0.9f + gradientOffset * 0.1f),
                    Color(0xFF650000).copy(alpha = 0.8f + glowIntensity * 0.2f),
                    Color(0xFF8B0000).copy(alpha = 0.7f + intensePulse * 0.3f),
                    Color(0xFF0D0404)
                ),
                radius = 1000f + (gradientOffset * 600f),
                center = Offset(
                    0.5f + sin(gradientOffset * PI.toFloat() * 2f) * 0.3f,
                    0.4f + cos(gradientOffset * PI.toFloat() * 1.5f) * 0.2f
                )
            )
        }
        val secondaryGradientBrush = remember(intensePulse, glowIntensity) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8B0000).copy(alpha = 0.3f + intensePulse * 0.4f),
                    Color.Transparent,
                    Color(0xFF4A1515).copy(alpha = 0.2f + glowIntensity * 0.3f),
                    Color.Transparent
                )
            )
        }
        val animatedContainerColor = remember(intensePulse) {
            Color(0xFF0D0404).copy(alpha = 0.8f + intensePulse * 0.1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(brush = aggressiveGradientBrush)
                    drawRect(brush = secondaryGradientBrush)
                    drawRect(color = animatedContainerColor)
                }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = workout.value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp))

                    Text(
                        text = "Set Your Goal",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,

                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    GoalSelector(
                        selectedType = selectedGoalType,
                        onTypeSelected = { newType ->
                            selectedGoalType = newType
                            ConnectedWorkout.workoutGoalTypeMap[workout.value] = newType
                        },
                        navController = navController
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    AnimatedContent(
                        targetState = selectedGoalType,
                        label = "GoalTypeAnimation",
                        transitionSpec = {
                            fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 2 }) togetherWith
                                    fadeOut(tween(400)) + slideOutVertically(tween(400), targetOffsetY = { -it / 2 })
                        }
                    ) { targetType ->
                        val animatedDividerModifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .drawBehind {
                                val animatedColor = Color(0xFFFF4444).copy(alpha = 0.2f + glowIntensity * 0.2f)
                                drawLine(
                                    color = animatedColor,
                                    start = Offset(0f, center.y),
                                    end = Offset(size.width, center.y),
                                    strokeWidth = size.height
                                )
                            }
                        if (targetType == "Reps") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WeightSelector()
                                Box(modifier = animatedDividerModifier)
                                RepSelector()
                                Box(modifier = animatedDividerModifier)
                                SetSelector()
                            }
                        } else if (targetType == "Distance") {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box(modifier = animatedDividerModifier)
                                DistanceSelector(
                                    label = "Distance",
                                    value = GoalDistance.value,
                                    onValueChange = { GoalDistance.value = it }
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                WeightSelector()
                                Box(modifier = animatedDividerModifier)
                                TimerSelector(navController)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val animatedButtonContainerColor = remember(intensePulse) {
                        Color(0xFF4A1515).copy(alpha = 0.4f + intensePulse * 0.2f)
                    }
                    val animatedBorderBrush = remember(intensePulse, glowIntensity) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B0000).copy(alpha = 0.8f + intensePulse * 0.2f),
                                Color(0xFFFF8800).copy(alpha = 0.6f + glowIntensity * 0.3f),
                                Color(0xFF650000).copy(alpha = 0.7f)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            when (selectedGoalType) {
                                "Reps" -> if (repsPerSet != 0 && GoalSets.intValue != 0) {
                                    GoalReps.intValue = totalGoalReps
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                                "Time" -> if (GoalTime.value != 0L) {
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                                "Distance" -> if (GoalDistance.value != 0.0) {
                                    navController.navigate("WorkoutScreen")
                                    ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .drawBehind {
                                drawRoundRect(
                                    color = animatedButtonContainerColor,
                                    cornerRadius = CornerRadius(size.height / 2f)
                                )
                                drawRoundRect(
                                    brush = animatedBorderBrush,
                                    cornerRadius = CornerRadius(size.height / 2f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        interactionSource = interactionSource
                    ) {
                        Text(
                            text = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) "Start Workout" else "Resume Workout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,

                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
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
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    val next = (value - 1.0).coerceIn(0.0..99.9)
                    onValueChange(next)
                    GoalDistance.value = next
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Decrement $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
                    .width(100.dp)
                    .heightIn(min = 56.dp),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                )
            )

            IconButton(
                onClick = {
                    val next = (value + 1.0).coerceIn(0.0..99.9)
                    onValueChange(next)
                    GoalDistance.value = next
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = MaterialTheme.colorScheme.primary
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
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(value) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetY.value + dragAmount
                        scope.launch {  offsetY.snapTo(newOffset)}
                    },
                    onDragEnd = {
                        val steps = (offsetY.value / itemHeightPx).roundToInt()
                        val target = steps * itemHeightPx
                        scope.launch {
                            offsetY.animateTo(target, animationSpec = spring())
                            val newValue = getWrappedValue(value, -steps, range)
                            if (newValue != value) onValueChange(newValue)
                            offsetY.snapTo(0f)
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetY.animateTo(0f, animationSpec = spring()) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val displayRange = -5..5
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
    var minutes by remember { mutableStateOf(5) }
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


@Composable
fun GoalSelector(
    navController: NavController,
    selectedType: String,
    onTypeSelected: (String) -> Unit,

    ) {
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
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.10f),
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
            SegmentedButton(
                text = "Time",
                isSelected = selectedType == "Time",
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                onClick = { onTypeSelected("Time") }
            )
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
            if (!canShowDistance) {
                SegmentedButton(
                    text = "Sets",
                    isSelected = selectedType == "Reps",
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    onClick = { onTypeSelected("Reps") }
                )
            } else {
                SegmentedButton(
                    text = "Distance",
                    isSelected = selectedType == "Distance",
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    onClick = { onTypeSelected("Distance") }
                )
            }
        }
    }
}

@Composable
private fun RowScope.SegmentedButton(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "segmentScale")
    val containerColor = if (isSelected
        && ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE
    ) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {

        Color.Transparent
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isSelected) 0.5f else 0.2f),
            Color.White.copy(alpha = if (isSelected) 0.1f else 0.05f)
        )
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(color = containerColor, shape = shape)
            .border(width = 1.5.dp, brush = borderBrush, shape = shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                        onClick(); haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 0..999
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
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    onValueChange((value - 1).coerceIn(range))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrement $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 64.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = {
                    onValueChange((value + 1).coerceIn(range))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun NumberStepperWeights(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double = 1.0
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
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = {
                    val newValue = (value - step).coerceAtLeast(0.0)
                    onValueChange(newValue)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrement $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
                    .width(110.dp)
                    .heightIn(min = 56.dp),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                )
            )

            IconButton(
                onClick = {
                    onValueChange((value + step).coerceAtLeast(0.0))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun WeightSelector() {
    NumberStepperWeights(
        label = "Weight",
        value = CurrentWeight.value,
        onValueChange = { newWeight -> CurrentWeight.value = newWeight },
        step = 1.0
    )
}

@Composable
fun RepSelector() {
    NumberStepper(
        label = "Reps",
        value = GoalReps.intValue,
        onValueChange = { GoalReps.intValue = it }
    )
}

@Composable
fun SetSelector() {
    NumberStepper(
        label = "Sets",
        value = GoalSets.intValue,
        onValueChange = { GoalSets.intValue = it }
    )
}


@Composable
fun RestScreen(
    navController: NavController,
    vm: HrPhoneViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val haptics = LocalHapticFeedback.current
    val bpm by vm.bpm.collectAsState()
    val initialTotal = rememberSaveable { 60000L } // Default 1 minute rest

    // Use and update the global rest timer
    var remaining by restTimeRemaining

    LaunchedEffect(Unit) {
        remaining = initialTotal
        while (remaining > 0) {
            delay(1000)
            remaining -= 1000
        }
        if (remaining <= 0) {
            ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
            navController.navigate("WorkoutScreen") { popUpTo("WorkoutScreen") { inclusive = true } }
        }
    }

    val progress = (1f - (remaining.toFloat() / initialTotal.toFloat())).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "p"
    )

    val seconds = (remaining / 1000) % 60
    val minutes = (remaining / (1000 * 60)) % 60
    val hours = (remaining / (1000 * 60 * 60))

    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else      -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(if (showIntro) 0f else 1f, tween(2000, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

    val bgTransition = rememberInfiniteTransition("rest_bg")
    val gradientOffset by bgTransition.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "grad"
    )
    val glow by bgTransition.animateFloat(
        0.35f, 0.7f,
        animationSpec = infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    WorkoutTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val bg = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF040D1A),
                            Color(0xFF0D2A3A).copy(alpha = 0.85f + gradientOffset * 0.05f),
                            Color(0xFF02040A)
                        ),
                        radius = 1100f + gradientOffset * 500f,
                        center = Offset(size.width * 0.45f, size.height * 0.78f)
                    )
                    drawRect(bg)
                    if (introProgress < 1f) drawRect(
                        brush = Brush.radialGradient(introColors, radius = 1200f, center = Offset(size.width * 0.4f, size.height * 0.28f)),
                        alpha = 1f - introProgress
                    )
                    drawContent()
                }
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))

                val ringSize = 320.dp

                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f
                        val stroke = 18f
                        val radius = min(w, h) / 2f - stroke

                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF0B1117), Color(0xFF0E1620)),
                                center = center,
                                radius = radius * 1.2f
                            ),
                            radius = radius,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        val tickCount = 60
                        val majorEvery = 5
                        repeat(tickCount) { i ->
                            val ang = Math.toRadians((i * 6 - 90).toDouble()).toFloat()
                            val isMajor = i % majorEvery == 0
                            val tickLen = if (isMajor) 18f else 10f
                            val tickW = if (isMajor) 3f else 2f
                            val outer = radius + stroke * 0.2f
                            val inner = outer - tickLen
                            val sx = cx + cos(ang) * inner
                            val sy = cy + sin(ang) * inner
                            val ex = cx + cos(ang) * outer
                            val ey = cy + sin(ang) * outer
                            drawLine(
                                color = Color.White.copy(alpha = if (isMajor) 0.18f else 0.10f),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = tickW
                            )
                        }

                        val sweep = 360f * animatedProgress
                        val arcRect = Rect(
                            Offset(cx - radius, cy - radius),
                            Size(radius * 2, radius * 2)
                        )
                        val arcBrush = Brush.sweepGradient(
                            0f to Color(0xFF4CA3FF),
                            0.35f to Color(0xFF66D4FF),
                            0.7f to Color(0xFF9BE7FF),
                            1f to Color(0xFF4CA3FF),
                            center = center
                        )
                        drawArc(
                            brush = arcBrush,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                            topLeft = arcRect.topLeft,
                            size = arcRect.size
                        )

                        drawArc(
                            color = Color(0xFF7BD1FF).copy(alpha = 0.18f + 0.12f * glow),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke * 1.6f, cap = StrokeCap.Round),
                            topLeft = arcRect.topLeft,
                            size = arcRect.size
                        )

                        if (animatedProgress > 0f) {
                            val capAngle = Math.toRadians((sweep - 90).toDouble()).toFloat()
                            val px = cx + cos(capAngle) * radius
                            val py = cy + sin(capAngle) * radius
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFFB2EBFF), Color.Transparent),
                                    center = Offset(px, py),
                                    radius = 26f
                                ),
                                radius = 26f * (0.7f + 0.3f * glow),
                                center = Offset(px, py),
                                alpha = 0.8f
                            )
                            drawCircle(
                                color = Color(0xFFCCF4FF),
                                radius = 6f,
                                center = Offset(px, py)
                            )
                        }

                        drawCircle(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                            ),
                            radius = radius - stroke * 0.65f,
                            style = Stroke(width = 6f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "REST",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF9BE7FF).copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format("%02d:%02d:%02d", hours, minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$bpm bpm",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF9BE7FF).copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = CurrentReps.intValue.toString(),
                    onValueChange = { CurrentReps.intValue = it.toIntOrNull() ?: 0 },
                    label = { Text("Enter Reps") },
                    modifier = Modifier.width(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF9BE7FF).copy(alpha = 0.9f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.06f + glow * 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedLabelColor = Color.White.copy(alpha = 0.85f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.55f),
                        cursorColor = Color.White
                    ),
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.weight(1f))



                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "btnScale")
                val elevation by animateDpAsState(if (pressed) 2.dp else 8.dp, label = "btnElev")

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                        navController.navigate("WorkoutScreen") {
                            popUpTo("WorkoutScreen") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))),
                            CircleShape
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9BE7FF).copy(alpha = 0.12f + glow * 0.08f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation, pressedElevation = elevation),
                    interactionSource = interactionSource
                ) {
                    Text("Skip Rest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}




private data class ConfettiParticle(
    val color: Color,
    val startOffset: Offset,
    val velocity: Offset,
    val startRotation: Float,
    val endRotation: Float,
    val rotationSpeed: Float
)
enum class FxVariant { Fireworks, Confetti, Stars, Ribbons }


object GoalCompletionFX {
    @JvmStatic var isPr: Boolean = false
}



private data class Particle(
    val color: Color,
    val startPosition: Offset,
    val velocity: Offset,
    val size: Float,
    val maxLife: Float
)

@Composable
fun GoalCompletionAnimation(
    onAnimationFinished: () -> Unit,
    aggression: Float = 1.0f,
    preferred: FxVariant? = null
) {
    val isPr = remember { GoalCompletionFX.isPr }

    // Animation drivers
    val progress = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }
    val shockwave = remember { Animatable(0f) }

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(Unit) {
        // This coroutine orchestrates the entire animation sequence
        coroutineScope {
            // Generate particles once
            launch {
                // Short delay to let the screen settle
                delay(50)
                particles = generateParticles(isPr, aggression)
            }
            // Animate everything in parallel
            launch {
                shockwave.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = 150f)
                )
            }
            launch {
                delay(100)
                textAlpha.animateTo(1f, spring(stiffness = 300f))
                textScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = 10f)
                )
            }
        }

        GoalCompletionFX.isPr = false
        onAnimationFinished()
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current.density

        // --- 1. Particle and Shockwave Canvas ---
        // This canvas draws the background effects. It's drawn first, so it's underneath the text.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = progress.value
            val shockwaveT = shockwave.value
            val shockwaveRadius = size.maxDimension * 0.8f * shockwaveT
            val shockwaveAlpha = (1f - shockwaveT.pow(2))

            // Draw Shockwave
            if (shockwaveAlpha > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f * shockwaveAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        // FIX: Ensure radius is always > 0 to prevent the crash.
                        radius = shockwaveRadius.coerceAtLeast(0.1f)
                    ),
                    radius = shockwaveRadius,
                    center = center
                )
            }

            // Draw Particles
            particles.forEach { particle ->
                val particleProgress = (t * particle.maxLife).coerceIn(0f, 1f)
                if (particleProgress > 0) {
                    val easedProgress = 1 - (1 - particleProgress).pow(3) // Ease-out effect

                    // Physics: Position is based on initial velocity, drag (1-eased), and gravity
                    val currentPos = particle.startPosition + (particle.velocity * easedProgress * density * 2f) +
                            Offset(0f, 2500f * easedProgress.pow(2) * density)

                    // Visuals: Fade out over its lifetime
                    val alpha = (1f - particleProgress).pow(0.5f)

                    drawCircle(
                        color = particle.color,
                        center = currentPos,
                        radius = particle.size * density * (1f - easedProgress),
                        alpha = alpha,
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }

        // --- 2. The Text Layer ---
        // A single, clean Text composable. The glow is part of its style.
        // This solves all clipping and rendering bugs from the previous version.
        val mainText = if (isPr) "NEW PR!\nMONSTER MODE" else "GOAL\nCOMPLETE"
        val gradient = if (isPr) {
            Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD54F), Color(0xFFFFA000)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF80FFE5), Color(0xFF7DB3FF), Color(0xFFFF7DF3)))
        }
        val glowColor = (if (isPr) Color(0xFFFFC107) else Color(0xFF7DB3FF)).copy(alpha = 0.8f)
        val textSize = if (isPr) 62.sp else 52.sp

        val styledText = remember(mainText, gradient) {
            buildAnnotatedString { withStyle(SpanStyle(brush = gradient)) { append(mainText) } }
        }

        Text(
            text = styledText,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = textScale.value
                    scaleY = textScale.value
                    alpha = textAlpha.value
                },
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = glowColor,
                    offset = Offset.Zero,
                    blurRadius = 30f
                )
            )
        )
    }
}

private fun generateParticles(isPr: Boolean, aggression: Float): List<Particle> {
    val rng = Random(System.currentTimeMillis())
    val count = 400
    val palette = if (isPr) {
        listOf(Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFE082), Color(0xFFFFF3E0), Color.White)
    } else {
        listOf(Color(0xFFFCE18A), Color(0xFFFF726D), Color(0xFFB48DEF), Color(0xFFF4306D), Color(0xFF8CE99A))
    }

    return List(count) {
        val angle = rng.nextDouble(0.0, 2 * PI)
        val speed = (rng.nextFloat() * 300f + 150f) * (0.8f + aggression * 0.4f)
        Particle(
            color = palette.random(rng),
            startPosition = Offset(0f, 0f), // Will be centered in the Canvas
            velocity = Offset(cos(angle).toFloat() * speed, sin(angle).toFloat() * speed),
            size = rng.nextFloat() * 4f + 2f,
            maxLife = rng.nextFloat() * 0.6f + 0.4f // Each particle has a varied lifetime
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
    var interHour = mutableIntStateOf(0)
    var healthConnectEnabled = mutableStateOf(false)
    var interMinute = mutableIntStateOf(0)
    var interSecond = mutableIntStateOf(0)
}

@Composable
fun RunningStickFigure(isStepping: Boolean, progress: Float, modifier: Modifier = Modifier) {
    val stickFigureColor = Color(0xFFE53935)
    val sweatColor = Color(0xFF65B2FF)
    val animationDuration = (400 + 250 * progress).toInt()

    val transition = rememberInfiniteTransition(label = "running_transition")
    val legAngle by transition.animateFloat(
        initialValue = -35f, targetValue = 35f,
        animationSpec = infiniteRepeatable(tween(animationDuration, easing = LinearEasing), RepeatMode.Reverse),
        label = "legAngle"
    )
    val armAngle by transition.animateFloat(
        initialValue = 30f, targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(animationDuration, easing = LinearEasing), RepeatMode.Reverse),
        label = "armAngle"
    )
    val bodyBob by transition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(animationDuration / 2, easing = LinearEasing), RepeatMode.Reverse),
        label = "bodyBob"
    )

    val sweatTransition = rememberInfiniteTransition(label = "sweat_transition")
    val sweatProgress1 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
    val sweatProgress2 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, delayMillis = 300, easing = LinearEasing)))
    val sweatProgress3 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, delayMillis = 500, easing = LinearEasing)))
    val sweatAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)

    val animationProgress by animateFloatAsState(
        targetValue = if (isStepping) 1f else 0f,
        animationSpec = tween(500),
        label = "stand_to_run_transition"
    )

    val slouchAngle = 20f * progress

    Canvas(modifier = modifier
        .height(200.dp)
        .fillMaxWidth()) {
        val scale = 3.5f
        val strokeWidth = 8f * scale
        val headRadius = 18f * scale
        val bodyLength = 50f * scale
        val limbLength = 45f * scale

        val startX = size.width / 2
        val startY = size.height - 550f

        val currentBodyBob = bodyBob * animationProgress
        val currentLegAngle = legAngle * animationProgress
        val currentArmAngle = armAngle * animationProgress

        val hip = Offset(startX, startY + headRadius + bodyLength + currentBodyBob)

        withTransform({ rotate(degrees = slouchAngle, pivot = hip) }) {
            val headCenter = Offset(startX, startY + currentBodyBob)
            val neck = Offset(startX, startY + headRadius + currentBodyBob)
            val shoulder = Offset(startX, startY + headRadius + 10f * scale + currentBodyBob)

            drawLine(stickFigureColor, neck, hip, strokeWidth, StrokeCap.Round)
            drawCircle(stickFigureColor, headRadius, headCenter, style = Stroke(strokeWidth))

            if (sweatAlpha > 0 && isStepping) {
                val sweatRotation = Math.toRadians(slouchAngle.toDouble()).toFloat()
                val cosR = cos(sweatRotation)
                val sinR = sin(sweatRotation)
                fun rotated(offset: Offset): Offset {
                    val x = offset.x * cosR - offset.y * sinR
                    val y = offset.x * sinR + offset.y * cosR
                    return Offset(x, y)
                }

                drawSweatDroplet(sweatProgress1, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
                drawSweatDroplet(sweatProgress2, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
                drawSweatDroplet(sweatProgress3, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
            }

            withTransform({ rotate(degrees = currentArmAngle, pivot = shoulder) }) {
                drawLine(stickFigureColor, shoulder, Offset(shoulder.x, shoulder.y + limbLength), strokeWidth, StrokeCap.Round)
            }
            withTransform({ rotate(degrees = -currentArmAngle, pivot = shoulder) }) {
                drawLine(stickFigureColor, shoulder, Offset(shoulder.x, shoulder.y + limbLength), strokeWidth, StrokeCap.Round)
            }
        }

        withTransform({ rotate(degrees = currentLegAngle, pivot = hip) }) {
            drawLine(stickFigureColor, hip, Offset(hip.x, hip.y + limbLength), strokeWidth, StrokeCap.Round)
        }
        withTransform({ rotate(degrees = -currentLegAngle, pivot = hip) }) {
            drawLine(stickFigureColor, hip, Offset(hip.x, hip.y + limbLength), strokeWidth, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawSweatDroplet(
    t: Float,
    headCenter: Offset,
    color: Color,
    applyRotation: (Offset) -> Offset
) {
    val initialVelX = -120f
    val initialVelY = -150f
    val gravity = 300f
    val rawDx = initialVelX * t
    val rawDy = initialVelY * t + 0.5f * gravity * t * t
    val rotatedOffset = applyRotation(Offset(rawDx, rawDy))

    val dropletCenter = headCenter + rotatedOffset + Offset(-20f, -20f)
    drawCircle(color, radius = 8f - 4*t, center = dropletCenter)
}