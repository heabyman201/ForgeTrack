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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.forgecompose.workouttracker.AI_HEART_ADAPT.showAIHeart
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
import androidx.core.content.edit

private const val EQUIPMENT_PREFS = "equipment_prefs"
private const val PREF_BARBELL_WEIGHT = "barbell_Weight"
private const val PREF_EZ_BARBELL_WEIGHT = "ez_barbell_weight"
private const val PREF_BARBELL_SIDE_PLATES_PREFIX = "barbell_side_plates_"
private const val PREF_EZ_BARBELL_SIDE_PLATES_PREFIX = "ez_barbell_side_plates_"
private const val DEFAULT_BARBELL_WEIGHT = 20.0
private const val DEFAULT_EZ_BARBELL_WEIGHT = 10.0
private const val MIN_CONFIGURABLE_BAR_WEIGHT = 1.0
private const val MAX_CONFIGURABLE_BAR_WEIGHT = 30.0
private const val BAR_WEIGHT_STEP = 0.5

internal fun readBarbellWeightPreference(context: Context): Double {
    return context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .getFloat(PREF_BARBELL_WEIGHT, DEFAULT_BARBELL_WEIGHT.toFloat())
        .toDouble()
}

internal fun writeBarbellWeightPreference(context: Context, value: Double) {
    context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .edit {
            putFloat(PREF_BARBELL_WEIGHT, value.toFloat())
        }
}

internal fun readEzBarbellWeightPreference(context: Context): Double {
    return context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .getFloat(PREF_EZ_BARBELL_WEIGHT, DEFAULT_EZ_BARBELL_WEIGHT.toFloat())
        .toDouble()
}

internal fun writeEzBarbellWeightPreference(context: Context, value: Double) {
    context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .edit {
            putFloat(PREF_EZ_BARBELL_WEIGHT, value.toFloat())
        }
}

private fun equipmentWorkoutKey(workoutName: String): String {
    return workoutName.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}

private fun readSavedSidePlateWeights(
    context: Context,
    workoutName: String,
    prefix: String
): List<Double>? {
    val raw = context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .getString(prefix + equipmentWorkoutKey(workoutName), null)
        ?: return null

    if (raw.isBlank()) return emptyList()

    return raw.split(",")
        .mapNotNull { it.toDoubleOrNull() }
}

private fun writeSavedSidePlateWeights(
    context: Context,
    workoutName: String,
    prefix: String,
    sidePlateWeights: List<Double>
) {
    val serialized = sidePlateWeights.joinToString(",")
    context.getSharedPreferences(EQUIPMENT_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(prefix + equipmentWorkoutKey(workoutName), serialized)
        .apply()
}

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
                badgeStorage = PersistentBadgeStorage(applicationContext)
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



    val navAppearance by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    NavHost(navController = navController, startDestination = "GoalScreen",
        modifier = Modifier.background(navAppearance.colors.background)) {
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
    loadingText: String = "Generating advice...",
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
                    appearanceOptions.colors.tertiary.copy(alpha = 0.30f)
                )
            )
        ),
        color = appearanceOptions.colors.background.copy(alpha = 0.75f)
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
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = loadingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 4.dp),
                        color = appearanceOptions.colors.primary,
                        trackColor = appearanceOptions.colors.background.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}



@Composable
fun CountdownOverlay(
    countdownValue: Int,
    theme: ColorSchemeAppTheme,
    onSkip: () -> Unit
) {
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
        val transition = updateTransition(targetState = countdownValue, label = "Countdown")

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            transition.AnimatedContent(
                transitionSpec = {
                    val duration = 650
                    (fadeIn(animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing)) +
                        scaleIn(initialScale = 1.38f, animationSpec = tween(duration, easing = EaseOutExpo)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing)) +
                                scaleOut(targetScale = 0.68f, animationSpec = tween(duration, easing = FastOutSlowInEasing))
                        )
                },

            ) { targetCountdown ->
                if (targetCountdown > 0) {
                    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    val shapeColor = when (targetCountdown) {
                        3 -> theme.tertiary
                        2 -> theme.secondary
                        else -> theme.primary
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        ) {
                            val shapeSize = Size(size.width * 0.8f, size.height * 0.8f)
                            val shapeTopLeft = Offset(
                                (size.width - shapeSize.width) / 2f,
                                (size.height - shapeSize.height) / 2f
                            )
                            val glowColor = shapeColor.copy(alpha = 0.28f)
                            val trianglePath = Path().apply {
                                moveTo(center.x, shapeTopLeft.y)
                                lineTo(shapeTopLeft.x + shapeSize.width, shapeTopLeft.y + shapeSize.height)
                                lineTo(shapeTopLeft.x, shapeTopLeft.y + shapeSize.height)
                                close()
                            }

                            when (targetCountdown) {
                                3 -> {
                                    drawRoundRect(
                                        color = glowColor,
                                        topLeft = shapeTopLeft - Offset(10.dp.toPx(), 10.dp.toPx()),
                                        size = Size(
                                            shapeSize.width + 20.dp.toPx(),
                                            shapeSize.height + 20.dp.toPx()
                                        ),
                                        cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx())
                                    )
                                    drawRoundRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                shapeColor.copy(alpha = 0.98f),
                                                lerp(shapeColor, Color.White, 0.12f)
                                            ),
                                            start = Offset(shapeTopLeft.x, shapeTopLeft.y),
                                            end = Offset(
                                                shapeTopLeft.x + shapeSize.width,
                                                shapeTopLeft.y + shapeSize.height
                                            )
                                        ),
                                        topLeft = shapeTopLeft,
                                        size = shapeSize,
                                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                                    )
                                }
                                2 -> {
                                    drawCircle(
                                        color = glowColor,
                                        radius = (shapeSize.minDimension / 2f) + 12.dp.toPx(),
                                        center = center
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                lerp(shapeColor, Color.White, 0.14f),
                                                shapeColor
                                            ),
                                            center = center,
                                            radius = shapeSize.minDimension / 2f
                                        ),
                                        radius = shapeSize.minDimension / 2f,
                                        center = center
                                    )
                                }
                                1 -> {
                                    drawPath(path = trianglePath, color = glowColor)
                                    drawPath(
                                        path = trianglePath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                lerp(shapeColor, Color.White, 0.16f),
                                                shapeColor
                                            ),
                                            startY = shapeTopLeft.y,
                                            endY = shapeTopLeft.y + shapeSize.height
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = targetCountdown.toString(),
                            fontSize = 165.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.06f),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Skip countdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}




@Composable
fun DistanceProgressTracker(
    modifier: Modifier = Modifier,
    currentDistance: Double,
    goalDistance: Double,
    hype: Float,
    theme: ColorSchemeAppTheme
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


    val progressStartColor = lerp(theme.secondary, theme.primary, hype)
    val progressEndColor = lerp(theme.tertiary, theme.secondary, hype)
    val flagColor = theme.primary

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

private fun formatElapsedHms(elapsedMillis: Long): String {
    val safe = elapsedMillis.coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun WorkoutScreen(viewModel: WorkoutListViewModel, navController: NavController, vm: BadgeViewModel, bpVM: HrViewModel = viewModel()) {
    val context = LocalContext.current
    val liveBpm by bpVM.hr.collectAsState()
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val performanceOptions by PerformanceOptionsManager.current.collectAsState(initial = PerformanceOptions.Defaults)
    val movingGradientAndParticlesEnabled = performanceOptions.movingGradientAndParticles
    val intent = remember(context) { Intent(context, MainActivity::class.java) }
    var isPaused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    var pauseText by remember { mutableStateOf("Pause") }
    val activity = remember(context) { context as? Activity }
    var showCompletionAnimation by remember { mutableStateOf(false) }
    var isStepping by remember { mutableStateOf(false) }
    var lastStepTimestamp by remember { mutableStateOf(0L) }
    val hrAccumulator = remember { HrAccumulator() }

    val pausedState = rememberUpdatedState(isPaused)
    val completionState = rememberUpdatedState(showCompletionAnimation)

    DisposableEffect(bpVM) {
        bpVM.start()
        bpVM.setWorkoutHrRecording(true)
        onDispose {
            bpVM.setWorkoutHrRecording(false)
            bpVM.stop()
        }
    }

    LaunchedEffect(bpVM) {
        bpVM.hr.collect { bpm ->
            if (!pausedState.value && !completionState.value && bpm > 0) {
                hrAccumulator.add(bpm, CurrentTime.value)
            }
        }
    }

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

    LaunchedEffect(
        workout.value,
        GoalType,
        GoalReps.intValue,
        GoalSets.intValue,
        CurrentReps.intValue,
        CurrentSets.intValue,
        GoalTime.value,
        GoalDistance.value,
        currentDistance.value,
        CurrentWeight.value,
        ConnectedWorkout.currentMode.value,
        ConnectedWorkout.restTimeRemaining.longValue
    ) {
        ConnectedWorkout.saveSnapshot(context)
    }

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

    val fitnessContext = "You are a supportive cheerleader coach during an active workout. Output must be very short: 1 line preferred, 2 lines max, and each line under 10 words."
    val scope = rememberCoroutineScope()
    val aiState = useGeminiAdviceGenerator(
        contextPrompt = "$fitnessContext."
    )
    val advice = aiState.currentAdvice
    var showAdvice by remember { mutableStateOf(false) }


    fun triggerSetGoal() {
        when (GoalType) {
            "Reps" -> {
                if (GoalSets.intValue <= 0 || GoalReps.intValue <= 0) return
                if (CurrentSets.intValue >= GoalSets.intValue && CurrentReps.intValue >= GoalReps.intValue) {
                    showCompletionAnimation = true
                }
            }
            "Distance" -> {
                if (GoalDistance.value <= 0.0) return
                if (currentDistance.value >= GoalDistance.value) {
                    showCompletionAnimation = true
                }
            }
            else -> {
                if (GoalTime.value <= 0L) return

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
            AutoRestTimer.startRest()
            ConnectedWorkout.currentMode.value = WorkoutMode.RESTING
            navController.navigate("RestScreen") { popUpTo("RestScreen") { inclusive = true } }
        }
    }

    fun SetsGoalSafetyCheck() {
        if (showCompletionAnimation) return
        if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) return
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
        accMs = CurrentTime.value
    }
    fun incrementTime() {
        if (!isPaused) {
            val now = SystemClock.elapsedRealtime()
            CurrentTime.value = (now - startAt.longValue) + accMs
        }
    }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    fun finishCountdown() {
        showCountdown = false
        isPaused = false
        startAt.longValue = SystemClock.elapsedRealtime()
        accMs = 0L
    }

    LaunchedEffect(Unit) {
        val isStartingFresh = (CurrentTime.value == 0L && accMs == 0L)

        if (isStartingFresh) {
            showCountdown = true
            isPaused = true
            countdownValue = 3

            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1000)
            if (showCountdown) {
                countdownValue = 2
            }

            delay(1000)
            if (showCountdown) {
                countdownValue = 1
            }

            delay(1000)
            if (showCountdown) {
                countdownValue = 0
                delay(500)
            }

            if (showCountdown) {
                finishCountdown()
            }
        }
        while (true) {
            incrementTime()
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
    LaunchedEffect(lastMilestone, GoalType, workout.value, GoalSets.intValue, GoalReps.intValue) {
        val goalProgressPct = (unified * 100f).roundToInt()
        val stageCue = when {
            goalProgressPct >= 90 -> "Final push. One last surge."
            goalProgressPct >= 70 -> "Almost there. Keep pace high."
            goalProgressPct >= 40 -> "Mid workout. Stay consistent and focused."
            else -> "Start strong. Build momentum now."
        }
        val goalSummary = when (GoalType) {
            "Reps" -> "Goal: ${GoalSets.intValue} sets and ${GoalReps.intValue} reps."
            "Distance" -> "Goal: ${"%.2f".format(GoalDistance.value)} km."
            else -> "Goal time: ${GoalTime.value / 1000L} seconds."
        }

        aiState.generateBatch(
            "Workout: ${workout.value}. $goalSummary",
            "Progress: $goalProgressPct%. Stage: $stageCue",
            "Current stats: ${CurrentSets.intValue} sets, ${CurrentReps.intValue} reps, ${CurrentTime.value / 1000L}s elapsed. Format rule: 1 short line (max 10 words), 2 lines absolute maximum."
        )
        showIntro = false
    }

    val glowColor = theme.secondary
    val deepColor = theme.background
    val primaryColor = theme.primary
    val secondaryColor = theme.secondary
    val profilePrefs = remember(context) { UserPreferencesManager(context) }
    val profileAge = remember { profilePrefs.getAge().toIntOrNull() }
    val profileWeightKg = remember { profilePrefs.getWeight().toDoubleOrNull() }
    val profileExperienceYears = remember { parseExperienceToYears(profilePrefs.getExperience()) }
    val adjustedHrZonesForEffects = remember(profileAge, profileWeightKg, profileExperienceYears) {
        buildAdjustedHrZones(
            age = profileAge,
            weightKg = profileWeightKg,
            experienceYears = profileExperienceYears
        )
    }
    val liveHrZoneForEffects by remember(liveBpm, adjustedHrZonesForEffects) {
        derivedStateOf { heartRateZone(liveBpm, adjustedHrZonesForEffects) }
    }
    val hrFireBoost by remember(liveHrZoneForEffects) {
        derivedStateOf {
            when {
                liveHrZoneForEffects >= 5 -> 1f
                liveHrZoneForEffects == 4 -> 0.65f
                liveHrZoneForEffects == 3 -> 0.35f
                else -> 0f
            }
        }
    }
    val hrParticleMultiplier by remember(liveHrZoneForEffects) {
        derivedStateOf {
            when {
                liveHrZoneForEffects >= 5 -> 3
                liveHrZoneForEffects == 4 -> 2
                liveHrZoneForEffects == 3 -> 1
                else -> 0
            }
        }
    }
    val animatedHrFireBoost by animateFloatAsState(
        targetValue = hrFireBoost,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "animatedHrFireBoost"
    )
    val animatedGlowColor by animateColorAsState(
        targetValue = lerp(glowColor, Color(0xFFFF4A3D), animatedHrFireBoost),
        animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing),
        label = "animatedGlowColor"
    )
    val animatedParticlePrimaryColor by animateColorAsState(
        targetValue = lerp(primaryColor, Color(0xFFFF3B30), animatedHrFireBoost),
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "animatedParticlePrimary"
    )
    val animatedParticleSecondaryColor by animateColorAsState(
        targetValue = lerp(secondaryColor, Color(0xFFFF7A45), animatedHrFireBoost),
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "animatedParticleSecondary"
    )
    val animatedClockStartColor by animateColorAsState(
        targetValue = lerp(Color.White, Color(0xFFFF3B30), animatedHrFireBoost),
        animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing),
        label = "animatedClockStartColor"
    )
    val animatedClockEndColor by animateColorAsState(
        targetValue = lerp(Color.White, theme.primary, (0.55f * hype + 0.70f * animatedHrFireBoost).coerceIn(0f, 1f)),
        animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing),
        label = "animatedClockEndColor"
    )
    val allWorkouts = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
    val totalWorkoutCount = remember(uiState) {
        allWorkouts.size
    }
    val particles = remember {
        List(7) { i ->
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
    val gradientStops = remember(theme) {
        arrayOf(
            0.0f to Color.Transparent,
            0.25f to theme.secondary.copy(alpha = 0.4f),
            0.55f to theme.primary.copy(alpha = 0.6f),
            0.85f to theme.primary.copy(alpha = 0.8f),
            1.0f to Color.Transparent
        )
    }
    val primaryPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = false
            color = primaryColor.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
    }

    val secondaryPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = false
            color = secondaryColor.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
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
                        val currentHype =
                            if (movingGradientAndParticlesEnabled) {
                                (hype + (0.45f * animatedHrFireBoost)).coerceIn(0f, 1f)
                            } else 0f
                        val radiusMultiplier = 1.0f + 0.5f * currentHype
                        val verticalShift = size.height * 0.1f
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(animatedGlowColor.copy(alpha = 0.46f + 0.32f * currentHype), deepColor),
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

                                    alpha = (0.12f + 0.32f * hype + 0.14f * animatedHrFireBoost).coerceIn(0f, 1f) * riseEffectProgress
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                val baseCount = particles.size
                                val count = baseCount * (1 + hrParticleMultiplier)
                                primaryPaint.color = animatedParticlePrimaryColor.toArgb()
                                secondaryPaint.color = animatedParticleSecondaryColor.toArgb()

                                for (i in 0 until count) {
                                    val p = particles[i % baseCount]
                                    val layerBoost = 1f + ((i / baseCount) * 0.18f)
                                    val phase = (animationClock * (p.speed * (1f + 0.35f * animatedHrFireBoost) * layerBoost) + (p.s * 0.013f)) % 1f
                                    val invPhase = 1f - phase
                                    val y = h * invPhase

                                    val wobble = kotlin.math.sin((animationClock * p.wobbleSpeed) * 6.28318f + p.wobbleOffset) *
                                            (p.wobbleMagnitudeBase + p.wobbleMagnitudeExtra * invPhase) * (1f + 0.22f * animatedHrFireBoost)

                                    val x = (p.baseXRatio * w + wobble).coerceIn(-40f, w + 40f)
                                    val r = p.baseRadius * (0.4f + 0.6f * invPhase) * (1f + 0.20f * animatedHrFireBoost)

                                    val alphaBase = (0.30f + 0.70f * invPhase) * riseEffectProgress * (1f + 0.25f * animatedHrFireBoost)
                                    val alpha = (alphaBase.coerceIn(0f, 1f) * 255).toInt()

                                    primaryPaint.alpha = alpha
                                    nativeCanvas.drawCircle(x, y, r, primaryPaint)

                                    secondaryPaint.alpha = (alpha * 0.6f).toInt()
                                    nativeCanvas.drawCircle(x, y + r * 0.2f, r * 1.8f, secondaryPaint)
                                }
                            }
                        }
                    }
                }
                if (introProgress < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(introBrush, alpha = 1f - introProgress)
                    )
                }
                BpmEdgePulseOverlay(
                    bpm = liveBpm,
                    paused = isPaused || showCompletionAnimation,
                    tint = theme.primary,
                    modifier = Modifier.fillMaxSize()
                )
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
                        val timingMetrics = ConnectedWorkout.deriveSetTimingMetrics()
                        val avgHrForLog = hrAccumulator.avgOrNull()
                        val maxHrForLog = hrAccumulator.maxOrNull()
                        val hrTimelineForLog = hrAccumulator.minuteAverageCsv(CurrentTime.value)
                        val estimatedRpe = ConnectedWorkout.estimateRPE(timingMetrics, avgHrForLog, maxHrForLog)
                        val estimatedFatigue = ConnectedWorkout.estimateFatigueLevel(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                        val systemicDrain = ConnectedWorkout.calculateSystemicDrain(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                        val trainingEnv = ConnectedWorkout.inferEnvironment()
                        val completionSummary = remember {
                            WorkoutCompletionSummary(
                                workingWeight = CurrentWeight.value,
                                totalVolume = CurrentWeight.value * CurrentSets.intValue * CurrentReps.intValue,
                                intensityScore = timingMetrics.intensityScore,
                                sets = CurrentSets.intValue,
                                reps = CurrentReps.intValue,
                                durationMillis = CurrentTime.value
                            )
                        }

                        GoalCompletionAnimation(
                            onAnimationFinished = {
                                val healthConnectManager = HealthConnectManager(context.applicationContext)
                                val cardioExerciseNames = listOf(
                                    "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                                    "Rowing Machine", "Stationary Bike", "Swimming"
                                )

                                scope.launch(Dispatchers.IO) {
                                    viewModel.LogWorkout(
                                        name = workout.value,
                                        status = WorkoutStatus.COMPLETED,
                                        durationMillis = CurrentTime.value,
                                        weight = CurrentWeight.value,
                                        sets = CurrentSets.intValue,
                                        reps = CurrentReps.intValue,
                                        distance = currentDistance.value,
                                        notes = "",
                                        rpe = estimatedRpe,
                                        restPeriodSeconds = timingMetrics.averageRestSeconds,
                                        fatigueLevel = estimatedFatigue,
                                        trainingEnvironment = trainingEnv,
                                        sessionRpe = estimatedRpe,
                                        systemicDrainScore = systemicDrain,
                                        intensityScore = timingMetrics.intensityScore,
                                        timingFatigueScore = timingMetrics.fatigueScore,
                                        heartRateAvg = avgHrForLog,
                                        heartRateMax = maxHrForLog,
                                        heartRateTimeline = hrTimelineForLog
                                    )
                                    GeminiAdaptiveMemoryStore.recordWorkoutCompletionSignals(
                                        context = context.applicationContext,
                                        workoutName = workout.value,
                                        rpe = estimatedRpe,
                                        fatigue = estimatedFatigue,
                                        intensityScore = timingMetrics.intensityScore,
                                        systemicDrain = systemicDrain,
                                        weight = CurrentWeight.value,
                                        sets = CurrentSets.intValue,
                                        reps = CurrentReps.intValue
                                    )
                                    PDE.logWorkout(
                                        workout.value,
                                        CurrentTime.value,
                                        CurrentWeight.value.toFloat(),
                                        CurrentReps.intValue,
                                        CurrentSets.intValue,
                                        currentDistance.value.toFloat(),
                                        rpe = estimatedRpe
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
                                ConnectedWorkout.clearSetTimingData()
                                WorkoutForegroundService.stop(context)
                                ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                                bpVM.setWorkoutHrRecording(false)

                                accMs = 0L
                                startAt.longValue = SystemClock.elapsedRealtime()
                                CurrentSets.intValue = 0
                                CurrentReps.intValue = 0
                                CurrentTime.value = 0
                                GoalSets.intValue = 0
                                GoalReps.intValue = 0
                                GoalTime.value = 0
                                GoalDistance.value = 0.0
                                ConnectedWorkout.clearSnapshot(context)
                                hrAccumulator.reset()
                            },
                            onFinishAnimation = {
                                context.startActivity(intent)
                                activity?.finishAffinity()
                            },
                            summary = completionSummary,
                            prFlags = prFlags,
                            estimatedRpe = estimatedRpe
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
                            text = formatElapsedHms(CurrentTime.value),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        animatedClockStartColor,
                                        animatedClockEndColor
                                    )
                                ),
                            ),
                        )
                    }

                    if (GoalType == "Reps") {
                        SetProgressDetails(
                            currentReps = CurrentReps.intValue,
                            goalReps = GoalReps.intValue,
                            currentSet = CurrentSets.intValue,
                            goalSets = GoalSets.intValue,
                            theme = theme
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WorkoutLiveHeartRateCard(
                            bpVM = bpVM,
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (GoalType == "Distance") {
                        DistanceProgressTracker(
                            currentDistance = currentDistance.value,
                            goalDistance = GoalDistance.value,
                            hype = hype,
                            theme = theme
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WorkoutLiveHeartRateCard(
                            bpVM = bpVM,
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CircularTimerProgressBar(
                            progress = animatedProgress,
                            hype = hype,
                            modifier = Modifier.size(250.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WorkoutLiveHeartRateCard(
                            bpVM = bpVM,
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.75f))
                    val aiEnabled = dynamicModel.personaConfig.value.enabled

                    if (aiEnabled && showAIHeart.value) {
                        AdviceSection(
                            advice = advice,
                            modifier = Modifier.fillMaxWidth().animateContentSize(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ),
                            isLoading = aiState.isLoading,
                            loadingText = ""
                        )
                    }

                    Spacer(modifier = Modifier
                        .weight(0.65f)
                        .height(2.dp))

                    if (GoalType == "Reps") {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            if (isPressed) 0.96f else 1f,
                            label = "buttonScale"
                        )
                        val animatedBg by animateColorAsState(
                            targetValue = if (isPressed) {
                                lerp(
                                    theme.secondary,
                                    theme.primary,
                                    riseEffectProgress
                                ).copy(alpha = (0.70f + 0.22f * hype).coerceIn(0f, 1f))
                            } else {
                                lerp(
                                    theme.background,
                                    theme.background,
                                    riseEffectProgress
                                ).copy(alpha = (0.45f + 0.30f * hype).coerceIn(0f, 1f))
                            },
                            label = "btnBg"
                        )

                        if (showAIHeart.value) {
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
                                                lerp(
                                                    theme.primary,
                                                    theme.secondary,
                                                    riseEffectProgress
                                                )
                                                    .copy(
                                                        alpha = (0.70f + 0.26f * hype).coerceIn(
                                                            0f,
                                                            1f
                                                        )
                                                    ),
                                                lerp(
                                                    theme.secondary,
                                                    theme.tertiary,
                                                    riseEffectProgress
                                                )
                                                    .copy(
                                                        alpha = (0.55f + 0.28f * hype).coerceIn(
                                                            0f,
                                                            1f
                                                        )
                                                    )
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .background(animatedBg, CircleShape)
                                    .animateContentSize(
                                        tween(300, easing = FastOutSlowInEasing)
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (!showCountdown) {
                                            incrementTime()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            ConnectedWorkout.restTime.longValue = AutoRestTimer.finishSet(
                                                currentRestMillis = ConnectedWorkout.restTime.longValue,
                                                autoAdjustEnabled = isAutoRestTimeEnabled(context)
                                            )
                                            ConnectedWorkout.recordSetCompletionTimestamp()
                                            CurrentReps.intValue += 10
                                            CurrentSets.intValue += 1
                                            EnterRestMode()
                                            ConnectedWorkout.saveSnapshot(context)
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
                    CountdownOverlay(
                        countdownValue = countdownValue,
                        theme = theme,
                        onSkip = {
                            countdownValue = 0
                            finishCountdown()
                        }
                    )
                }
                if (showSyncDialog.showSyncDialog.value) {

                    ThemedConfirmationDialog(
                        title = "Sync to health connect",
                        text = "Sync this workout to health connect?",
                        buttonText = "Sync and finish workout",
                        additionalButton = true,
                        additionalButtonText = "Finish Workout Only",
                        onCustomAction = {
                            incrementTime()
                            val timingMetrics = ConnectedWorkout.deriveSetTimingMetrics()
                            val avgHrForLog = hrAccumulator.avgOrNull()
                            val maxHrForLog = hrAccumulator.maxOrNull()
                            val hrTimelineForLog = hrAccumulator.minuteAverageCsv(CurrentTime.value)
                            val estimatedRpe = ConnectedWorkout.estimateRPE(timingMetrics, avgHrForLog, maxHrForLog)
                            val estimatedFatigue = ConnectedWorkout.estimateFatigueLevel(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                            val systemicDrain = ConnectedWorkout.calculateSystemicDrain(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                            val trainingEnv = ConnectedWorkout.inferEnvironment()

                            scope.launch(Dispatchers.IO) {
                                viewModel.LogWorkout(
                                    name = workout.value,
                                    status = WorkoutStatus.COMPLETED,
                                    durationMillis = CurrentTime.value,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue,
                                    distance = currentDistance.value,
                                    notes = "",
                                    rpe = estimatedRpe,
                                    restPeriodSeconds = timingMetrics.averageRestSeconds,
                                    fatigueLevel = estimatedFatigue,
                                    trainingEnvironment = trainingEnv,
                                    sessionRpe = estimatedRpe,
                                    systemicDrainScore = systemicDrain,
                                    intensityScore = timingMetrics.intensityScore,
                                    timingFatigueScore = timingMetrics.fatigueScore,
                                    heartRateAvg = avgHrForLog,
                                    heartRateMax = maxHrForLog,
                                    heartRateTimeline = hrTimelineForLog
                                )
                                GeminiAdaptiveMemoryStore.recordWorkoutCompletionSignals(
                                    context = context.applicationContext,
                                    workoutName = workout.value,
                                    rpe = estimatedRpe,
                                    fatigue = estimatedFatigue,
                                    intensityScore = timingMetrics.intensityScore,
                                    systemicDrain = systemicDrain,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue
                                )
                                PDE.logWorkout(
                                    workout.value,
                                    CurrentTime.value,
                                    CurrentWeight.value.toFloat(),
                                    CurrentReps.intValue,
                                    CurrentSets.intValue,
                                    currentDistance.value.toFloat(),
                                    rpe = estimatedRpe
                                )
                            }
                            WorkoutLog.sets.clear()
                            ConnectedWorkout.clearSetTimingData()
                            WorkoutForegroundService.stop(context)
                            ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                            bpVM.setWorkoutHrRecording(false)
                            context.startActivity(intent)
                            activity?.finishAffinity()
                            accMs = 0L
                            startAt.longValue = SystemClock.elapsedRealtime()
                            CurrentSets.intValue = 0
                            CurrentReps.intValue = 0
                            CurrentTime.value = 0
                            GoalSets.intValue = 0
                            GoalReps.intValue = 0
                            GoalTime.value = 0
                            GoalDistance.value = 0.0
                            ConnectedWorkout.clearSnapshot(context)
                            hrAccumulator.reset()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDismiss = { showSyncDialog.showSyncDialog.value = false },
                        onConfirm = {
                            val healthConnectManager = HealthConnectManager(context.applicationContext)
                            val cardioExerciseNames = listOf(
                                "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                                "Rowing Machine", "Stationary Bike", "Swimming"
                            )
                            val timingMetrics = ConnectedWorkout.deriveSetTimingMetrics()
                            val avgHrForLog = hrAccumulator.avgOrNull()
                            val maxHrForLog = hrAccumulator.maxOrNull()
                            val hrTimelineForLog = hrAccumulator.minuteAverageCsv(CurrentTime.value)
                            val estimatedRpe = ConnectedWorkout.estimateRPE(timingMetrics, avgHrForLog, maxHrForLog)
                            val estimatedFatigue = ConnectedWorkout.estimateFatigueLevel(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                            val systemicDrain = ConnectedWorkout.calculateSystemicDrain(estimatedRpe, timingMetrics, avgHrForLog, maxHrForLog)
                            val trainingEnv = ConnectedWorkout.inferEnvironment()

                            scope.launch(Dispatchers.IO) {
                                viewModel.LogWorkout(
                                    name = workout.value,
                                    status = WorkoutStatus.COMPLETED,
                                    durationMillis = CurrentTime.value,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue,
                                    distance = currentDistance.value,
                                    notes = "",
                                    rpe = estimatedRpe,
                                    restPeriodSeconds = timingMetrics.averageRestSeconds,
                                    fatigueLevel = estimatedFatigue,
                                    trainingEnvironment = trainingEnv,
                                    sessionRpe = estimatedRpe,
                                    systemicDrainScore = systemicDrain,
                                    intensityScore = timingMetrics.intensityScore,
                                    timingFatigueScore = timingMetrics.fatigueScore,
                                    heartRateAvg = avgHrForLog,
                                    heartRateMax = maxHrForLog,
                                    heartRateTimeline = hrTimelineForLog
                                )
                                GeminiAdaptiveMemoryStore.recordWorkoutCompletionSignals(
                                    context = context.applicationContext,
                                    workoutName = workout.value,
                                    rpe = estimatedRpe,
                                    fatigue = estimatedFatigue,
                                    intensityScore = timingMetrics.intensityScore,
                                    systemicDrain = systemicDrain,
                                    weight = CurrentWeight.value,
                                    sets = CurrentSets.intValue,
                                    reps = CurrentReps.intValue
                                )
                                PDE.logWorkout(
                                    workout.value,
                                    CurrentTime.value,
                                    CurrentWeight.value.toFloat(),
                                    CurrentReps.intValue,
                                    CurrentSets.intValue,
                                    currentDistance.value.toFloat(),
                                    rpe = estimatedRpe
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
                            ConnectedWorkout.clearSetTimingData()
                            WorkoutForegroundService.stop(context)
                            ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                            bpVM.setWorkoutHrRecording(false)
                            context.startActivity(intent)
                            activity?.finishAffinity()
                            accMs = 0L
                            startAt.longValue = SystemClock.elapsedRealtime()
                            CurrentSets.intValue = 0
                            CurrentReps.intValue = 0
                            CurrentTime.value = 0
                            GoalSets.intValue = 0
                            GoalReps.intValue = 0
                            GoalTime.value = 0
                            GoalDistance.value = 0.0
                            ConnectedWorkout.clearSnapshot(context)
                            hrAccumulator.reset()

                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BpmEdgePulseOverlay(
    bpm: Int,
    paused: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val clampedBpm = bpm.coerceIn(45, 190)
    val pulseEnabled = !paused && bpm > 0
    val beatMillis = (60000f / clampedBpm.toFloat()).toInt().coerceIn(315, 1300)

    val pulseTransition = rememberInfiniteTransition(label = "bpm_edge_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bpm_edge_alpha"
    )

    val intensity = if (pulseEnabled) pulse else 0.18f
    val edgeAlpha = (0.05f + (clampedBpm - 45) / 145f * 0.06f) * intensity

    Canvas(modifier = modifier) {
        val topHeight = size.height * 0.18f
        val sideWidth = size.width * 0.10f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    tint.copy(alpha = edgeAlpha),
                    Color.Transparent
                ),
                startY = 0f,
                endY = topHeight
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = edgeAlpha * 0.9f)
                ),
                startY = size.height - topHeight,
                endY = size.height
            )
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    tint.copy(alpha = edgeAlpha * 0.85f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = sideWidth
            )
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = edgeAlpha * 0.85f)
                ),
                startX = size.width - sideWidth,
                endX = size.width
            )
        )
    }
}

@Composable
private fun WorkoutLiveHeartRateCard(
    bpVM: HrViewModel,
    theme: ColorSchemeAppTheme,
    modifier: Modifier = Modifier
) {
    val bpm by bpVM.hr.collectAsState()
    var sum by remember { mutableLongStateOf(0L) }
    var count by remember { mutableIntStateOf(0) }
    var maxBpm by remember { mutableIntStateOf(0) }
    val avgBpm by remember(sum, count) {
        derivedStateOf { if (count > 0) (sum / count).toInt() else 0 }
    }
    LaunchedEffect(bpm) {
        if (bpm > 0) {
            sum += bpm.toLong()
            count += 1
            if (bpm > maxBpm) maxBpm = bpm
        }
    }
    LiveHeartRateCard(
        bpm = bpm,
        avgBpm = avgBpm,
        maxBpm = maxBpm,
        theme = theme,
        modifier = modifier
    )
}

private class HrAccumulator {
    private data class MinuteBucket(var sum: Long = 0L, var count: Int = 0)

    private var sum: Long = 0L
    private var count: Int = 0
    private var max: Int = 0
    private val minuteBuckets = linkedMapOf<Int, MinuteBucket>()

    fun add(bpm: Int, elapsedMillis: Long? = null) {
        if (bpm <= 0) return
        sum += bpm.toLong()
        count += 1
        if (bpm > max) max = bpm

        val elapsed = elapsedMillis ?: return
        val minute = ((elapsed.coerceAtLeast(0L) / 60_000L).toInt() + 1).coerceAtLeast(1)
        val bucket = minuteBuckets.getOrPut(minute) { MinuteBucket() }
        bucket.sum += bpm.toLong()
        bucket.count += 1
    }

    fun avgOrNull(): Int? = if (count > 0) (sum / count).toInt() else null
    fun maxOrNull(): Int? = max.takeIf { it > 0 }
    fun minuteAverageCsv(totalDurationMillis: Long? = null): String? {
        if (minuteBuckets.isEmpty()) return null

        val minuteAverages = minuteBuckets
            .mapValues { (_, bucket) -> if (bucket.count > 0) (bucket.sum / bucket.count).toInt() else 0 }
            .filterValues { it > 0 }

        if (minuteAverages.isEmpty()) return null

        val maxRecordedMinute = minuteAverages.keys.maxOrNull() ?: 1
        val maxByDuration = totalDurationMillis
            ?.takeIf { it > 0L }
            ?.let { ((it + 59_999L) / 60_000L).toInt().coerceAtLeast(1) }
            ?: 1
        val totalMinutes = maxOf(maxRecordedMinute, maxByDuration)

        val firstValue = minuteAverages[minuteAverages.keys.minOrNull() ?: 1] ?: return null
        var carry = firstValue

        return (1..totalMinutes).joinToString(",") { minute ->
            val current = minuteAverages[minute] ?: carry
            carry = current
            "$minute:$current"
        }
    }

    fun reset() {
        sum = 0L
        count = 0
        max = 0
        minuteBuckets.clear()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveHeartRateCard(
    bpm: Int,
    avgBpm: Int,
    maxBpm: Int,
    theme: ColorSchemeAppTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveBpm = bpm.coerceAtLeast(0)
    val hasAverage = avgBpm > 0
    val hasMax = maxBpm > 0
    val prefs = remember(context) { UserPreferencesManager(context) }
    val age = remember { prefs.getAge().toIntOrNull() }
    val weightKg = remember { prefs.getWeight().toDoubleOrNull() }
    val experienceYears = remember { parseExperienceToYears(prefs.getExperience()) }
    val adjustedZones = remember(age, weightKg, experienceYears) {
        buildAdjustedHrZones(age = age, weightKg = weightKg, experienceYears = experienceYears)
    }
    val zone = remember(liveBpm, adjustedZones) { heartRateZone(liveBpm, adjustedZones) }
    var previousBpm by remember { mutableIntStateOf(0) }
    var trendVisible by remember { mutableStateOf(false) }
    var trendKind by remember { mutableStateOf(HrTrend.STEADY) }
    var trendEventId by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(liveBpm) {
        if (liveBpm <= 0) return@LaunchedEffect
        if (previousBpm > 0) {
            val delta = liveBpm - previousBpm
            trendKind = when {
                delta >= 4 -> HrTrend.UP
                delta <= -4 -> HrTrend.DOWN
                else -> HrTrend.STEADY
            }
            trendVisible = true
            trendEventId += 1
        }
        previousBpm = liveBpm
    }

    LaunchedEffect(trendEventId) {
        if (trendEventId == 0) return@LaunchedEffect
        delay(2400)
        trendVisible = false
    }

    val trendTransition = rememberInfiniteTransition(label = "hr_trend_arrow")
    val trendPulse by trendTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hr_trend_pulse"
    )
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) {
                LaunchedEffect(pagerState.currentPage) {

                     if (pagerState.currentPage == 0) {
                         showAIHeart.value = true
} else {
    showAIHeart.value = false
}
                }
                if (it == 0) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.secondary.copy(0.12f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart rate",
                            tint = theme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (trendVisible) {
                            val trendTint = when (trendKind) {
                                HrTrend.UP -> Color(0xFF8DEFAE)
                                HrTrend.DOWN -> Color(0xFFFF9A9A)
                                HrTrend.STEADY -> Color.White.copy(alpha = 0.72f)
                            }
                            val trendShiftX = when (trendKind) {
                                HrTrend.STEADY -> (trendPulse - 0.5f) * 6f
                                else -> 0f
                            }
                            val trendShiftY = when (trendKind) {
                                HrTrend.UP -> -3f * trendPulse
                                HrTrend.DOWN -> 3f * trendPulse
                                HrTrend.STEADY -> 0f
                            }
                            Icon(
                                imageVector = when (trendKind) {
                                    HrTrend.UP -> Icons.Default.KeyboardArrowUp
                                    HrTrend.DOWN -> Icons.Default.KeyboardArrowDown
                                    HrTrend.STEADY -> Icons.Default.KeyboardArrowRight
                                },
                                contentDescription = "Heart rate trend",
                                tint = trendTint,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        alpha = 0.45f + (trendPulse * 0.4f)
                                        translationX = trendShiftX
                                        translationY = trendShiftY
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (liveBpm > 0) "$liveBpm BPM" else "-- BPM",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Swipe for zones",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        }
                    }
                } else {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.secondary.copy(0.10f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Avg ${if (hasAverage) "$avgBpm bpm" else "--"}", color = Color.White)
                            Text("Peak ${if (hasMax) "$maxBpm bpm" else "--"}", color = Color.White)
                            Text("Zone Z$zone", color = Color.White)
                        }
                        Text(
                            text = "Adjusted max HR: ${adjustedZones.adjustedMaxHr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.74f)
                        )
                        adjustedZones.ranges.forEach { range ->
                            val upperText = if (range.zone == 5) "+" else "${range.max}"
                            val isActive = liveBpm >= range.min && (range.zone == 5 || liveBpm <= range.max)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (isActive) theme.primary.copy(alpha = 0.30f)
                                        else theme.secondary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Z${range.zone}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${range.min}-$upperText bpm", color = Color.White.copy(alpha = 0.82f))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == idx) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == idx) theme.primary.copy(alpha = 0.95f)
                                else Color.White.copy(alpha = 0.28f)
                            )
                        )
                    }
                }
            }
        }
    }
object AI_HEART_ADAPT{
    var showAIHeart = mutableStateOf(true)

}

private enum class HrTrend { UP, DOWN, STEADY }

private data class HrZoneRange(
    val zone: Int,
    val min: Int,
    val max: Int
)

private data class AdjustedHrZones(
    val adjustedMaxHr: Int,
    val ranges: List<HrZoneRange>
)

private fun buildAdjustedHrZones(
    age: Int?,
    weightKg: Double?,
    experienceYears: Float
): AdjustedHrZones {
    val baseMaxHr = if ((age ?: 0) > 0) 220 - (age ?: 0) else 190
    val weightAdjustment = when {
        weightKg == null -> 0
        weightKg < 55.0 -> 2
        weightKg > 100.0 -> -5
        weightKg > 85.0 -> -2
        else -> 0
    }
    val experienceAdjustment = when {
        experienceYears < 0.5f -> -4
        experienceYears < 2f -> -2
        experienceYears > 5f -> 2
        else -> 0
    }
    val adjustedMaxHr = (baseMaxHr + weightAdjustment + experienceAdjustment).coerceIn(150, 205)
    val ranges = listOf(
        HrZoneRange(1, (adjustedMaxHr * 0.50f).roundToInt(), (adjustedMaxHr * 0.60f).roundToInt()),
        HrZoneRange(2, (adjustedMaxHr * 0.60f).roundToInt(), (adjustedMaxHr * 0.70f).roundToInt()),
        HrZoneRange(3, (adjustedMaxHr * 0.70f).roundToInt(), (adjustedMaxHr * 0.80f).roundToInt()),
        HrZoneRange(4, (adjustedMaxHr * 0.80f).roundToInt(), (adjustedMaxHr * 0.90f).roundToInt()),
        HrZoneRange(5, (adjustedMaxHr * 0.90f).roundToInt(), adjustedMaxHr)
    )
    return AdjustedHrZones(adjustedMaxHr = adjustedMaxHr, ranges = ranges)
}

private fun heartRateZone(bpm: Int, zones: AdjustedHrZones): Int {
    if (bpm <= 0) return 1
    return zones.ranges.firstOrNull { range ->
        bpm >= range.min && (range.zone == 5 || bpm <= range.max)
    }?.zone ?: 5
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



// Extension functions to keep the draw logic clean and isolated





@Composable
private fun distancePillBackground(theme: ColorSchemeAppTheme): Brush = Brush.linearGradient(
    listOf(
        theme.secondary.copy(alpha = 0.85f),
        theme.primary.copy(alpha = 0.55f)
    )
)

@Composable
private fun distanceIconBackground(pressed: Boolean, theme: ColorSchemeAppTheme): Brush {
    val start = if (pressed) theme.primary.copy(alpha = 0.25f) else theme.primary.copy(alpha = 0.12f)
    val end = if (pressed) theme.primary.copy(alpha = 0.12f) else theme.secondary.copy(alpha = 0.08f)
    return Brush.radialGradient(listOf(start, end))
}

@Composable
private fun distanceIconTint(pressed: Boolean, theme: ColorSchemeAppTheme): Color {
    val target = if (pressed) theme.primary else theme.primary.copy(alpha = 0.85f)
    val animated by animateColorAsState(targetValue = target, animationSpec = tween(160, easing = FastOutSlowInEasing), label = "distanceIconTint")
    return animated
}

@Composable
fun DistanceSelector(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors
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
                    .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                    .background(distanceIconBackground(decPressed, theme)),
                interactionSource = decInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Decrement $label",
                    tint = distanceIconTint(decPressed, theme)
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
                    .background(distancePillBackground(theme)),
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
                    focusedBorderColor = theme.primary.copy(alpha = 0.85f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = theme.primary
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
                    .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape)
                    .background(distanceIconBackground(incPressed, theme)),
                interactionSource = incInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment $label",
                    tint = distanceIconTint(incPressed, theme)
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

    val scale = pressedScale(isPressed)
    val bgBrush = segmentContainerBrush(isSelected, isPressed, themeColors)
    val border = segmentBorderBrush(isSelected, isPressed, themeColors)
    val labelColor = segmentTextColor(isSelected, isPressed)

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
                enabled = true,
                onClick = {
                    onClick()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
            themeColors.secondary.copy(alpha = 0.35f),
            themeColors.secondary.copy(alpha = 0.35f)
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
        ConnectedWorkout.barbellVisualExercises.contains(workoutName)
    }
    val useEzBarVisual = remember(workoutName) {
        ConnectedWorkout.ezBarVisualExercises.contains(workoutName)
    }

    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
        if (useBarbellVisual) {
            BarbellStyleInput(
                label = label,
                value = value,
                onValueChange = onValueChange,
                workoutName = workoutName,
                theme = theme
            )
        } else if (useEzBarVisual) {
            EzBarStyleInput(
                label = label,
                value = value,
                onValueChange = onValueChange,
                workoutName = workoutName,
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
    workoutName: String,
    theme: ColorSchemeAppTheme
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var barWeight by remember { mutableStateOf(readBarbellWeightPreference(context)) }
    var sidePlateWeights by remember {
        mutableStateOf(
            readSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX)
                ?.takeIf {
                    abs(calculateTotalWeightFromSidePlates(barWeight, it) - value) <= 0.01
                }
                ?: calculateSidePlateWeights(value, barWeight)
        )
    }
    val sidePlates = sidePlateWeights.mapNotNull(::plateConfigForWeight)
    val barColor = Color.LightGray
    val totalWeight = remember(barWeight, sidePlateWeights) {
        calculateTotalWeightFromSidePlates(barWeight, sidePlateWeights)
    }

    LaunchedEffect(value, barWeight) {
        if (abs(totalWeight - value) > 0.01) {
            sidePlateWeights = calculateSidePlateWeights(value, barWeight)
            writeSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX, sidePlateWeights)
        }
    }

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
            text = "${String.format("%.1f", totalWeight)} kg",
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
                        val updated = sidePlateWeights + config.weightKg
                        sidePlateWeights = updated
                        writeSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX, updated)
                        onValueChange(calculateTotalWeightFromSidePlates(barWeight, updated))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onRemove = {
                        sidePlateWeights.removeSinglePlate(config.weightKg)?.let { updated ->
                            sidePlateWeights = updated
                            writeSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX, updated)
                            onValueChange(calculateTotalWeightFromSidePlates(barWeight, updated))
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    theme = theme
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        BarWeightAdjustmentRow(
            title = "Barbell Weight",
            currentWeight = barWeight,
            onWeightChange = { updatedWeight ->
                barWeight = updatedWeight
                writeBarbellWeightPreference(context, updatedWeight)
                writeSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX, sidePlateWeights)
                onValueChange(calculateTotalWeightFromSidePlates(updatedWeight, sidePlateWeights))
            },
            theme = theme
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reset
        OutlinedButton(
            onClick = {
                sidePlateWeights = emptyList()
                writeSavedSidePlateWeights(context, workoutName, PREF_BARBELL_SIDE_PLATES_PREFIX, emptyList())
                onValueChange(barWeight)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
            border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.5f))
        ) {
            Text("Reset to Empty Bar (${String.format("%.1f", barWeight)}kg)")
        }
    }
}

@Composable
private fun EzBarStyleInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    workoutName: String,
    theme: ColorSchemeAppTheme
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var ezBarWeight by remember { mutableStateOf(readEzBarbellWeightPreference(context)) }
    var sidePlateWeights by remember {
        mutableStateOf(
            readSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX)
                ?.takeIf {
                    abs(calculateTotalWeightFromSidePlates(ezBarWeight, it) - value) <= 0.01
                }
                ?: calculateSidePlateWeights(value, ezBarWeight)
        )
    }
    val sidePlates = sidePlateWeights.mapNotNull(::plateConfigForWeight)
    val barColor = Color.LightGray
    val totalWeight = remember(ezBarWeight, sidePlateWeights) {
        calculateTotalWeightFromSidePlates(ezBarWeight, sidePlateWeights)
    }

    LaunchedEffect(value, ezBarWeight) {
        if (abs(totalWeight - value) > 0.01) {
            sidePlateWeights = calculateSidePlateWeights(value, ezBarWeight)
            writeSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX, sidePlateWeights)
        }
    }

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
            text = "${String.format("%.1f", totalWeight)} kg",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = theme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // EZ Bar Visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Zigzag Shaft
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(30.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2
                val zigzagW = w * 0.4f
                val startX = (w - zigzagW) / 2
                val endX = startX + zigzagW
                val amplitude = 8.dp.toPx()

                val path = Path().apply {
                    moveTo(0f, midY)
                    lineTo(startX, midY)
                    // EZ Zigzag
                    lineTo(startX + zigzagW * 0.25f, midY - amplitude)
                    lineTo(startX + zigzagW * 0.5f, midY + amplitude)
                    lineTo(startX + zigzagW * 0.75f, midY - amplitude)
                    lineTo(endX, midY)
                    lineTo(w, midY)
                }

                drawPath(
                    path = path,
                    color = barColor,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

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

                // Center Gap (EZ bar curves are in the center)
                Spacer(modifier = Modifier.width(60.dp))

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
                        val updated = sidePlateWeights + config.weightKg
                        sidePlateWeights = updated
                        writeSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX, updated)
                        onValueChange(calculateTotalWeightFromSidePlates(ezBarWeight, updated))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onRemove = {
                        sidePlateWeights.removeSinglePlate(config.weightKg)?.let { updated ->
                            sidePlateWeights = updated
                            writeSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX, updated)
                            onValueChange(calculateTotalWeightFromSidePlates(ezBarWeight, updated))
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    theme = theme
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        BarWeightAdjustmentRow(
            title = "EZ Bar Weight",
            currentWeight = ezBarWeight,
            onWeightChange = { updatedWeight ->
                ezBarWeight = updatedWeight
                writeEzBarbellWeightPreference(context, updatedWeight)
                writeSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX, sidePlateWeights)
                onValueChange(calculateTotalWeightFromSidePlates(updatedWeight, sidePlateWeights))
            },
            theme = theme
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reset
        OutlinedButton(
            onClick = {
                sidePlateWeights = emptyList()
                writeSavedSidePlateWeights(context, workoutName, PREF_EZ_BARBELL_SIDE_PLATES_PREFIX, emptyList())
                onValueChange(ezBarWeight)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
            border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.5f))
        ) {
            Text("Reset to Empty EZ Bar (${String.format("%.1f", ezBarWeight)}kg)")
        }
    }
}

@Composable
private fun BarWeightAdjustmentRow(
    title: String,
    currentWeight: Double,
    onWeightChange: (Double) -> Unit,
    theme: ColorSchemeAppTheme
) {
    val haptics = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val updatedWeight = (currentWeight - BAR_WEIGHT_STEP)
                        .coerceAtLeast(MIN_CONFIGURABLE_BAR_WEIGHT)
                    onWeightChange(updatedWeight)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
                border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.45f))
            ) {
                Text("-0.5")
            }
            Text(
                text = "${String.format("%.1f", currentWeight)} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.primary
            )
            OutlinedButton(
                onClick = {
                    val updatedWeight = (currentWeight + BAR_WEIGHT_STEP)
                        .coerceAtMost(MAX_CONFIGURABLE_BAR_WEIGHT)
                    onWeightChange(updatedWeight)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
                border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.45f))
            ) {
                Text("+0.5")
            }
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
            cursorColor = Color.White.copy(alpha = 0.8f)
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





data class Particle(
    val color: Color,
    val velocity: Offset,
    val size: Float,
    val rotationSpeed: Float,
    val type: ParticleType
)

data class WorkoutCompletionSummary(
    val workingWeight: Double,
    val totalVolume: Double,
    val intensityScore: Int,
    val sets: Int,
    val reps: Int,
    val durationMillis: Long
)

 enum class ParticleType { CIRCLE, SQUARE, SHARD }




public data class WorkoutBackdropOrb(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

public fun generateWorkoutBackdropOrbs(count: Int): List<WorkoutBackdropOrb> {
    return List(count) {
        val topCluster = Random.nextBoolean()
        val x = Random.nextFloat()
        val y = if (topCluster) {
            Random.nextFloat() * 0.42f
        } else {
            0.40f + Random.nextFloat() * 0.58f
        }

        WorkoutBackdropOrb(
            x = x,
            y = y.coerceIn(0f, 1f),
            radius = 120f + Random.nextFloat() * 360f,
            alpha = 0.08f + Random.nextFloat() * 0.18f
        )
    }
}

 fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWorkoutBackdropOrbs(
    orbs: List<WorkoutBackdropOrb>,
    size: Size,
    theme: ColorSchemeAppTheme
) {
    orbs.forEachIndexed { index, orb ->
        val tint = when (index % 3) {
            0 -> theme.primary
            1 -> theme.secondary
            else -> theme.tertiary
        }
        val center = Offset(orb.x * size.width, orb.y * size.height)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = orb.alpha),
                    tint.copy(alpha = orb.alpha * 0.45f),
                    Color.Transparent
                ),
                center = center,
                radius = orb.radius
            ),
            center = center,
            radius = orb.radius
        )
    }
}

@Composable
 fun WorkoutCompletionSummaryScreen(
    summary: WorkoutCompletionSummary,
    estimatedRpe: Int,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    onContinue: () -> Unit
) {
    val weightText = if (summary.workingWeight > 0.0) {
        "${summary.workingWeight.roundToInt()} kg"
    } else {
        "Bodyweight"
    }
    val volumeText = if (summary.totalVolume > 0.0) {
        "${summary.totalVolume.roundToInt()} kg"
    } else {
        "${summary.reps} reps"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.98f),
                        secondaryColor.copy(alpha = 0.14f),
                        primaryColor.copy(alpha = 0.18f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "SESSION SUMMARY",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Volume, intensity, and load at a glance.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            SummaryStatCard(
                title = "Volume",
                value = volumeText,
                subtitle = "${summary.sets} sets / ${summary.reps} reps",
                accent = primaryColor
            )
            SummaryStatCard(
                title = "Intensity",
                value = "${summary.intensityScore}/10",
                subtitle = "Estimated session RPE $estimatedRpe/10",
                accent = secondaryColor
            )
            SummaryStatCard(
                title = "Weight",
                value = weightText,
                subtitle = formatElapsedHms(summary.durationMillis),
                accent = lerp(primaryColor, secondaryColor, 0.45f)
            )
        }

        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = backgroundColor
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

 fun generateParticles(
    colors: List<Color>,
    count: Int = 600
): List<Particle> {
    val rng = Random(System.currentTimeMillis())

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



object ConnectedWorkout {
    private const val SESSION_PREFS = "connected_workout_session"
    private const val KEY_WORKOUT = "workout"
    private const val KEY_GOAL_TYPE = "goal_type"
    private const val KEY_GOAL_REPS = "goal_reps"
    private const val KEY_GOAL_SETS = "goal_sets"
    private const val KEY_CURRENT_REPS = "current_reps"
    private const val KEY_CURRENT_SETS = "current_sets"
    private const val KEY_CURRENT_TIME = "current_time"
    private const val KEY_GOAL_TIME = "goal_time"
    private const val KEY_GOAL_DISTANCE = "goal_distance"
    private const val KEY_CURRENT_DISTANCE = "current_distance"
    private const val KEY_CURRENT_WEIGHT = "current_weight"
    private const val KEY_REST_TIME = "rest_time"
    private const val KEY_REST_REMAINING = "rest_remaining"
    private const val KEY_MODE = "mode"
    private const val KEY_IS_BODYWEIGHT = "is_bodyweight"

    data class SetTimingMetrics(
        val intensityScore: Int,
        val fatigueScore: Int,
        val averageRestSeconds: Int,
        val intervalsMillis: List<Long>
    )

    enum class WorkoutMode { INACTIVE, ACTIVE, RESTING }
    var currentMode = mutableStateOf(WorkoutMode.INACTIVE)
    var restTimeRemaining = mutableLongStateOf(0L)

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
        "Hip Thrusts"
    )

    val ezBarVisualExercises = setOf(
        "Barbell Curls",
        "Barbell Shrugs",
        "EZ Bar Curls",
        "EZ Bar Shrugs"
    )

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
    val setCompletionTimestamps = mutableStateListOf<Long>()
    var isBodyweight = mutableStateOf(false)
    var GoalType = "Reps"
    val workoutGoalTypeMap = mutableMapOf<String, String>()
    var selectedWorkout: MutableState<String> = mutableStateOf("")
    var restTime = mutableLongStateOf(60000L)
    var interHour = mutableIntStateOf(0)
    var healthConnectEnabled = mutableStateOf(false)
    var interMinute = mutableIntStateOf(0)
    var interSecond = mutableIntStateOf(0)

    val BAR_WEIGHT = DEFAULT_BARBELL_WEIGHT
    val EZ_BAR_WEIGHT = DEFAULT_EZ_BARBELL_WEIGHT

    fun recordSetCompletionTimestamp(timestampMillis: Long = System.currentTimeMillis()) {
        val lastTimestamp = setCompletionTimestamps.lastOrNull()
        if (lastTimestamp == null || timestampMillis > lastTimestamp) {
            setCompletionTimestamps.add(timestampMillis)
        }
    }

    fun clearSetTimingData() {
        setCompletionTimestamps.clear()
        AutoRestTimer.reset()
    }

    fun saveSnapshot(context: Context) {
        val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WORKOUT, workout.value)
            .putString(KEY_GOAL_TYPE, GoalType)
            .putInt(KEY_GOAL_REPS, GoalReps.intValue)
            .putInt(KEY_GOAL_SETS, GoalSets.intValue)
            .putInt(KEY_CURRENT_REPS, CurrentReps.intValue)
            .putInt(KEY_CURRENT_SETS, CurrentSets.intValue)
            .putLong(KEY_CURRENT_TIME, CurrentTime.value)
            .putLong(KEY_GOAL_TIME, GoalTime.value)
            .putFloat(KEY_GOAL_DISTANCE, GoalDistance.value.toFloat())
            .putFloat(KEY_CURRENT_DISTANCE, currentDistance.value.toFloat())
            .putFloat(KEY_CURRENT_WEIGHT, CurrentWeight.value.toFloat())
            .putLong(KEY_REST_TIME, restTime.longValue)
            .putLong(KEY_REST_REMAINING, restTimeRemaining.longValue)
            .putString(KEY_MODE, currentMode.value.name)
            .putBoolean(KEY_IS_BODYWEIGHT, isBodyweight.value)
            .apply()
    }

    fun restoreSnapshot(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_MODE)) return false
        val restoredMode = runCatching {
            WorkoutMode.valueOf(prefs.getString(KEY_MODE, WorkoutMode.INACTIVE.name) ?: WorkoutMode.INACTIVE.name)
        }.getOrDefault(WorkoutMode.INACTIVE)

        workout.value = prefs.getString(KEY_WORKOUT, "").orEmpty()
        GoalType = prefs.getString(KEY_GOAL_TYPE, "Reps") ?: "Reps"
        GoalReps.intValue = prefs.getInt(KEY_GOAL_REPS, 0)
        GoalSets.intValue = prefs.getInt(KEY_GOAL_SETS, 0)
        CurrentReps.intValue = prefs.getInt(KEY_CURRENT_REPS, 0)
        CurrentSets.intValue = prefs.getInt(KEY_CURRENT_SETS, 0)
        CurrentTime.value = prefs.getLong(KEY_CURRENT_TIME, 0L)
        GoalTime.value = prefs.getLong(KEY_GOAL_TIME, 0L)
        GoalDistance.value = prefs.getFloat(KEY_GOAL_DISTANCE, 0f).toDouble()
        currentDistance.value = prefs.getFloat(KEY_CURRENT_DISTANCE, 0f).toDouble()
        CurrentWeight.value = prefs.getFloat(KEY_CURRENT_WEIGHT, 0f).toDouble()
        restTime.longValue = prefs.getLong(KEY_REST_TIME, 60_000L)
        restTimeRemaining.longValue = prefs.getLong(KEY_REST_REMAINING, 0L)
        isBodyweight.value = prefs.getBoolean(KEY_IS_BODYWEIGHT, false)
        currentMode.value = restoredMode

        return hasSessionSnapshot() || currentMode.value != WorkoutMode.INACTIVE
    }

    fun clearSnapshot(context: Context) {
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun forceEndSession(context: Context) {
        clearSetTimingData()
        WorkoutForegroundService.stop(context)
        currentMode.value = WorkoutMode.INACTIVE
        restTimeRemaining.longValue = 0L

        interHour.intValue = 0
        interMinute.intValue = 0
        interSecond.intValue = 0
        CurrentTime.value = 0L
        GoalTime.value = 0L

        CurrentSets.intValue = 0
        CurrentReps.intValue = 0
        GoalSets.intValue = 0
        GoalReps.intValue = 0

        CurrentWeight.value = 0.0
        currentDistance.value = 0.0
        GoalDistance.value = 0.0
        workout.value = ""
        GoalType = "Reps"
        isBodyweight.value = false
        clearSnapshot(context)
    }

    fun hasSessionSnapshot(): Boolean {
        if (GoalSets.intValue > 0 || GoalReps.intValue > 0) return true
        if (GoalTime.value > 0L || GoalDistance.value > 0.0) return true
        if (CurrentSets.intValue > 0 || CurrentReps.intValue > 0) return true
        if (CurrentTime.value > 0L || currentDistance.value > 0.0) return true
        return false
    }

    fun deriveSetTimingMetrics(): SetTimingMetrics {
        if (setCompletionTimestamps.size < 2) {
            val fallbackRestSeconds = (restTime.longValue / 1000L).coerceAtLeast(1L).toInt()
            return SetTimingMetrics(
                intensityScore = 5,
                fatigueScore = 5,
                averageRestSeconds = fallbackRestSeconds,
                intervalsMillis = emptyList()
            )
        }

        val intervals = setCompletionTimestamps
            .zipWithNext { previous, current -> (current - previous).coerceAtLeast(1L) }
            .filter { it > 0L }

        if (intervals.isEmpty()) {
            return SetTimingMetrics(
                intensityScore = 5,
                fatigueScore = 5,
                averageRestSeconds = 60,
                intervalsMillis = emptyList()
            )
        }

        val averageIntervalMillis = intervals.average()
        val firstInterval = intervals.first().toDouble()
        val lastInterval = intervals.last().toDouble()
        val fatigueTrend = ((lastInterval - firstInterval) / firstInterval).coerceIn(-1.0, 2.0)

        val variance = intervals
            .map { (it - averageIntervalMillis) * (it - averageIntervalMillis) }
            .average()
        val standardDeviation = kotlin.math.sqrt(variance)
        val variability = (standardDeviation / averageIntervalMillis).coerceIn(0.0, 1.0)
        val density = ((180000.0 - averageIntervalMillis) / 150000.0).coerceIn(0.0, 1.0)

        val intensityScore = (4.0 + density * 6.0).roundToInt().coerceIn(1, 10)
        val fatigueScore = (3.0 + (fatigueTrend * 3.0) + (variability * 2.0) + (density * 2.0))
            .roundToInt()
            .coerceIn(1, 10)

        return SetTimingMetrics(
            intensityScore = intensityScore,
            fatigueScore = fatigueScore,
            averageRestSeconds = (averageIntervalMillis / 1000.0).roundToInt().coerceAtLeast(1),
            intervalsMillis = intervals
        )
    }

    private data class SessionLoadProfile(
        val volumeUnits: Double,
        val loadUnits: Double,
        val hrStrain: Double,
        val densityPerMinute: Double
    )

    private fun estimateSessionLoadProfile(
        timingMetrics: SetTimingMetrics,
        avgHeartRate: Int?,
        maxHeartRate: Int?
    ): SessionLoadProfile {
        val sets = CurrentSets.intValue.coerceAtLeast(0)
        val reps = CurrentReps.intValue.coerceAtLeast(0)
        val weight = CurrentWeight.value.coerceAtLeast(0.0)
        val distance = currentDistance.value.coerceAtLeast(0.0)
        val durationMinutes = (CurrentTime.value / 60000.0).coerceAtLeast(0.0)

        val tonnageUnits = (sets * reps * weight) / 1000.0
        val enduranceUnits = distance * 2.4
        val durationUnits = durationMinutes / 30.0
        val fallbackUnits = if (tonnageUnits == 0.0 && enduranceUnits == 0.0) {
            (durationMinutes / 40.0).coerceAtLeast(0.6)
        } else {
            0.0
        }
        val volumeUnits = (tonnageUnits + enduranceUnits + durationUnits + fallbackUnits).coerceAtLeast(0.2)

        val avgHrValue = avgHeartRate ?: 0
        val maxHrValue = maxHeartRate ?: 0
        val avgHrLoad = if (avgHrValue > 0) {
            ((avgHrValue.toDouble() - 95.0) / 70.0).coerceIn(0.0, 1.3)
        } else {
            0.0
        }
        val peakHrLoad = if (maxHrValue > 0) {
            ((maxHrValue.toDouble() - 130.0) / 65.0).coerceIn(0.0, 1.3)
        } else {
            0.0
        }
        val hrStrain = (avgHrLoad * 0.65) + (peakHrLoad * 0.35)

        val densityMultiplier = when {
            timingMetrics.averageRestSeconds <= 75 -> 1.12
            timingMetrics.averageRestSeconds <= 120 -> 1.05
            else -> 0.95
        }
        val timingMultiplier = 1.0 +
            ((timingMetrics.intensityScore - 5).coerceIn(-4, 5) * 0.07) +
            ((timingMetrics.fatigueScore - 5).coerceIn(-4, 5) * 0.05)

        val loadUnits = volumeUnits * (1.0 + (hrStrain * 0.38)) * timingMultiplier * densityMultiplier
        val densityPerMinute = loadUnits / durationMinutes.coerceAtLeast(12.0)

        return SessionLoadProfile(
            volumeUnits = volumeUnits,
            loadUnits = loadUnits,
            hrStrain = hrStrain,
            densityPerMinute = densityPerMinute
        )
    }

    fun estimateRPE(
        timingMetrics: SetTimingMetrics = deriveSetTimingMetrics(),
        avgHeartRate: Int? = null,
        maxHeartRate: Int? = null
    ): Int {
        val sets = CurrentSets.intValue
        val reps = CurrentReps.intValue
        val timeMillis = CurrentTime.value
        val timeMinutes = timeMillis / 60000.0

        if (sets == 0 && reps == 0 && currentDistance.value == 0.0) return 0

        val sessionProfile = estimateSessionLoadProfile(timingMetrics, avgHeartRate, maxHeartRate)
        var baseRPE = 6.0

        if (GoalType == "Distance") {
            val km = currentDistance.value
            if (km > 0 && timeMinutes > 0) {
                val pace = timeMinutes / km
                val paceFactor = (7.0 - pace).coerceIn(-1.5, 2.5)
                baseRPE += paceFactor
                if (km > 5) baseRPE += 0.5
                if (km > 10) baseRPE += 1.0
            }
        } else {
            if (sets > 0) {
                val repsPerSet = reps.toDouble() / sets
                baseRPE += (sets - 3).coerceAtLeast(0) * 0.4
                if (repsPerSet > 12) baseRPE += 0.5
                if (repsPerSet > 20) baseRPE += 1.0
            }
        }

        baseRPE += (timingMetrics.intensityScore - 5) * 0.22
        baseRPE += (timingMetrics.fatigueScore - 5) * 0.18
        baseRPE += (sessionProfile.densityPerMinute - 0.35).coerceIn(-0.5, 2.5) * 1.2
        baseRPE += (sessionProfile.volumeUnits / 10.0).coerceIn(0.0, 1.8) * 0.7
        baseRPE += sessionProfile.hrStrain * 1.4

        if (timeMinutes > 30) baseRPE += 0.5
        if (timeMinutes > 60) baseRPE += 1.0

        return baseRPE.roundToInt().coerceIn(1, 10)
    }

    fun estimateFatigueLevel(
        rpe: Int,
        timingMetrics: SetTimingMetrics = deriveSetTimingMetrics(),
        avgHeartRate: Int? = null,
        maxHeartRate: Int? = null
    ): Int {
        val sessionProfile = estimateSessionLoadProfile(timingMetrics, avgHeartRate, maxHeartRate)
        val durationMinutes = (CurrentTime.value / 60000.0).coerceAtLeast(0.0)

        var fatigue = 2.2 + (rpe * 0.52)
        fatigue += (timingMetrics.fatigueScore - 5) * 0.38
        fatigue += sessionProfile.hrStrain * 1.8
        fatigue += (sessionProfile.loadUnits / 10.0).coerceIn(0.0, 2.2)

        if (durationMinutes > 60.0) fatigue += 0.6
        if (durationMinutes > 90.0) fatigue += 0.6

        return fatigue.roundToInt().coerceIn(1, 10)
    }

    fun calculateSystemicDrain(
        rpe: Int,
        timingMetrics: SetTimingMetrics = deriveSetTimingMetrics(),
        avgHeartRate: Int? = null,
        maxHeartRate: Int? = null
    ): Float {
        val sessionProfile = estimateSessionLoadProfile(timingMetrics, avgHeartRate, maxHeartRate)
        val compoundMultiplier = if (barbellVisualExercises.any { workout.value.contains(it, true) }) 1.4 else 1.0
        val intensityMultiplier = 1.0 + (rpe.toDouble() / 10.0).pow(1.35)
        val timingLoadMultiplier = 1.0 + ((timingMetrics.intensityScore + timingMetrics.fatigueScore) / 20.0) * 0.22

        val baseDrain = (sessionProfile.loadUnits * 7.8) + (sessionProfile.volumeUnits * 2.8)

        return (baseDrain * intensityMultiplier * compoundMultiplier * timingLoadMultiplier)
            .toFloat()
            .coerceIn(0f, 100f)
    }

    fun inferEnvironment(): String {
        val hour = java.time.LocalTime.now().hour
        return if (hour in 11..16) "Possible Heat" else "Standard"
    }
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

private fun plateConfigForWeight(weightKg: Double): PlateConfig? {
    return standardPlates.firstOrNull { abs(it.weightKg - weightKg) < 0.01 }
}

private fun List<Double>.removeSinglePlate(weightKg: Double): List<Double>? {
    val removeIndex = indexOfLast { abs(it - weightKg) < 0.01 }
    if (removeIndex == -1) return null
    return toMutableList().apply { removeAt(removeIndex) }
}

private fun calculateTotalWeightFromSidePlates(barWeight: Double, sidePlateWeights: List<Double>): Double {
    return (barWeight + sidePlateWeights.sum() * 2.0)
        .coerceAtLeast(barWeight)
}

fun calculateSidePlateWeights(totalWeight: Double, barWeight: Double = ConnectedWorkout.BAR_WEIGHT): List<Double> {
    var remainingWeightPerSide = ((totalWeight - barWeight).coerceAtLeast(0.0)) / 2.0
    val plates = mutableListOf<Double>()

    // Greedy algorithm: fit biggest plates first
    standardPlates.forEach { plateConfig ->
        // Using a small epsilon for floating point comparison safety
        while (remainingWeightPerSide >= plateConfig.weightKg - 0.01) {
            plates.add(plateConfig.weightKg)
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
