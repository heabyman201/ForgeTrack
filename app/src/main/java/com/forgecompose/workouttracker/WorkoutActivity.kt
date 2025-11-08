package com.forgecompose.workouttracker

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.annotation.SuppressLint
import java.time.Duration
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
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
import android.view.HapticFeedbackConstants
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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.workouttracker.ConnectedWorkout.GoalDistance
import com.forgecompose.workouttracker.ConnectedWorkout.currentDistance
import com.forgecompose.workouttracker.ConnectedWorkout.interHour
import com.forgecompose.workouttracker.ConnectedWorkout.interMinute
import com.forgecompose.workouttracker.ConnectedWorkout.interSecond
import com.forgecompose.workouttracker.ConnectedWorkout.restTimeRemaining
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.toDuration
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.milliseconds
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
    var glowDir by remember { mutableStateOf(1) }
    var starRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            glow += glowDir * 0.01f
            if (glow >= 0.9f) glowDir = -1
            if (glow <= 0.35f) glowDir = 1
            starRotation = (starRotation + 1.5f) % 360f
            delay(1000L / 24L)
        }
    }

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
private fun CountdownOverlay(countdownValue: Int) {
    val smallRipple = remember { Animatable(0f) }
    val bigRipple = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(countdownValue) {
        if (countdownValue in 1..2) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            smallRipple.snapTo(0f)
            smallRipple.animateTo(1f, tween(400, easing = LinearOutSlowInEasing))
        }
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue == 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            bigRipple.snapTo(0f)
            bigRipple.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            if (bigRipple.value > 0f) {
                val progress = bigRipple.value
                val radius = size.maxDimension * 1.5f * progress
                val alpha = 1f - progress
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.5f),
                    radius = radius,
                    style = Stroke(width = 40.dp.toPx() * (1f - progress))
                )
            }

            if (smallRipple.value > 0f) {
                val progress = smallRipple.value
                val radius = size.minDimension * 0.7f * progress
                val alpha = 1f - progress
                val rippleColor = when (countdownValue) {
                    2 -> Color(0xFFFFC300)
                    1 -> Color(0xFF33D4FF)
                    else -> Color.Transparent
                }
                drawCircle(
                    color = rippleColor.copy(alpha = alpha * 0.6f),
                    radius = radius,
                    style = Stroke(width = 20.dp.toPx() * (1f - progress))
                )
            }
        }

        AnimatedContent(
            targetState = countdownValue,
            label = "CountdownAnimation",
            transitionSpec = {
                (fadeIn(animationSpec = tween(150, easing = LinearEasing)) +
                        scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), initialScale = 1.3f))
                    .togetherWith(
                        fadeOut(animationSpec = tween(150, easing = LinearEasing)) +
                                scaleOut(animationSpec = tween(150), targetScale = 0.7f)
                    )
            }
        ) { targetCountdown ->
            if (targetCountdown > 0) {
                val pulse by rememberInfiniteTransition(label = "glowPulse").animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowPulseAnim"
                )

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(250.dp)) {
                        val shapeSize = size * 0.9f
                        val shapeTopLeft = Offset(
                            (size.width - shapeSize.width) / 2f,
                            (size.height - shapeSize.height) / 2f
                        )

                        // helpers
                        fun drawNeonRoundRect(
                            topLeft: Offset,
                            size: Size,
                            baseColor: Color,
                            glowColor: Color,
                            radiusDp: Float = 16f,
                            glowRadiusDp: Float = 28f
                        ) {
                            val r = radiusDp.dp.toPx()
                            val glow = (glowRadiusDp * pulse).dp.toPx()

                            // big blurred glow underneath (true blur via framework paint)
                            drawIntoCanvas { c ->
                                val paint = Paint()
                                val fp = paint.asFrameworkPaint()
                                fp.isAntiAlias = true
                                fp.color = glowColor.copy(alpha = 0.55f).toArgb()
                                fp.setShadowLayer(glow, 0f, 0f, glowColor.copy(alpha = 0.95f).toArgb())
                                val rect = RectF(
                                    topLeft.x, topLeft.y,
                                    topLeft.x + size.width, topLeft.y + size.height
                                )
                                c.nativeCanvas.drawRoundRect(rect, r, r, fp)
                            }

                            // solid fill
                            drawRoundRect(
                                color = baseColor,
                                topLeft = topLeft,
                                size = size,
                                cornerRadius = CornerRadius(r, r)
                            )

                            // additive highlight stroke for “neon tube” feel
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.20f)
                                    ),
                                    start = topLeft,
                                    end = topLeft + Offset(size.width, size.height)
                                ),
                                topLeft = topLeft,
                                size = size,
                                cornerRadius = CornerRadius(r, r),
                                style = Stroke(width = 2.dp.toPx()),
                                blendMode = BlendMode.Plus
                            )
                        }

                        fun drawNeonCircle(
                            center: Offset,
                            radius: Float,
                            baseColor: Color,
                            glowColor: Color,
                            glowRadiusDp: Float = 28f
                        ) {
                            val glow = (glowRadiusDp * pulse).dp.toPx()

                            drawIntoCanvas { c ->
                                val paint = Paint()
                                val fp = paint.asFrameworkPaint()
                                fp.isAntiAlias = true
                                fp.color = glowColor.copy(alpha = 0.55f).toArgb()
                                fp.setShadowLayer(glow, 0f, 0f, glowColor.copy(alpha = 0.95f).toArgb())
                                c.nativeCanvas.drawCircle(center.x, center.y, radius, fp)
                            }

                            drawCircle(color = baseColor, radius = radius, center = center)

                            // additive rim
                            drawCircle(
                                color = Color.White.copy(alpha = 0.18f),
                                radius = radius - 1.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.dp.toPx()),
                                blendMode = BlendMode.Plus
                            )
                        }

                        fun drawNeonTriangle(
                            p1: Offset, p2: Offset, p3: Offset,
                            baseColor: Color,
                            glowColor: Color,
                            glowRadiusDp: Float = 28f
                        ) {
                            val glow = (glowRadiusDp * pulse).dp.toPx()

                            // build both Compose and framework paths
                            val composePath = Path().apply {
                                moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                            }
                            val fwPath = android.graphics.Path().apply {
                                moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                            }

                            drawIntoCanvas { c ->
                                val paint = Paint()
                                val fp = paint.asFrameworkPaint()
                                fp.isAntiAlias = true
                                fp.style = android.graphics.Paint.Style.FILL
                                fp.color = glowColor.copy(alpha = 0.55f).toArgb()
                                fp.setShadowLayer(glow, 0f, 0f, glowColor.copy(alpha = 0.95f).toArgb())
                                c.nativeCanvas.drawPath(fwPath, fp)
                            }

                            drawPath(path = composePath, color = baseColor)

                            // additive edge highlight
                            drawPath(
                                path = composePath,
                                color = Color.White.copy(alpha = 0.18f),
                                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round),
                                blendMode = BlendMode.Plus
                            )
                        }

                        // pick colors per shape
                        when (targetCountdown) {
                            3 -> {
                                // crimson rectangle with magenta glow
                                drawNeonRoundRect(
                                    topLeft = shapeTopLeft,
                                    size = shapeSize,
                                    baseColor = Color(0xFFC70039),
                                    glowColor = Color(0xFF411616),
                                    radiusDp = 16f,
                                    glowRadiusDp = 36f
                                )
                            }
                            2 -> {

                                drawNeonCircle(
                                    center = center,
                                    radius = shapeSize.minDimension / 2f,
                                    baseColor = Color(0xFFC60000),
                                    glowColor = Color(0xFF853A3A),
                                    glowRadiusDp = 34f
                                )
                            }
                            1 -> {

                                val p1 = Offset(center.x, shapeTopLeft.y)
                                val p2 = Offset(shapeTopLeft.x + shapeSize.width, shapeTopLeft.y + shapeSize.height)
                                val p3 = Offset(shapeTopLeft.x, shapeTopLeft.y + shapeSize.height)
                                drawNeonTriangle(
                                    p1, p2, p3,
                                    baseColor = Color(0xFFFF3D3D),
                                    glowColor = Color(0xFF350202),
                                    glowRadiusDp = 32f
                                )
                            }
                        }
                    }

                    Text(
                        text = targetCountdown.toString(),
                        fontSize = 150.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(5f, 5f),
                                blurRadius = 10f
                            )
                        )
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
fun WorkoutScreen(viewModel: WorkoutListViewModel, navController: NavController, vm: HrViewModel = viewModel()) {
    val performanceOptions by PerformanceOptionsManager.current.collectAsState(initial = PerformanceOptions.Defaults)
    val movingGradientAndParticlesEnabled = performanceOptions.movingGradientAndParticles
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
    LaunchedEffect(Unit) { showIntro = false }

    val glowColor = Color(0xFF3B0E0E)
    val deepColor = Color(0xFF0D0404)

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
                                0f to Color.Transparent,
                                0.25f to Color(0x66B71C1C),
                                0.55f to Color(0x99D32F2F),
                                0.85f to Color(0xCCF44336),
                                1f to Color.Transparent,
                                startY = startY,
                                endY = endY
                            ),
                            size = size
                        )
                    }
                    if (movingGradientAndParticlesEnabled) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = (0.12f + 0.32f * hype) * riseEffectProgress }
                        ) {
                            val n = 6
                            val w = size.width
                            val h = size.height
                            for (i in 0 until n) {
                                val s = (i * 37.123f) % 1000f
                                val speed = 0.25f + (s % 0.35f)
                                val phase = (animationClock * speed + (s * 0.013f)) % 1f
                                val y = h * (1f - phase)
                                val baseX = (s % 1f) * w
                                val wobble = sin((animationClock * (0.8f + (s % 0.7f))) * 6.28318f + s) * (16f + 28f * (1f - phase))
                                val x = (baseX + wobble).coerceIn(-40f, w + 40f)
                                val r = 6f + (s % 1f) * 18f * (0.4f + 0.6f * (1f - phase))
                                val a = (0.30f + 0.70f * (1f - phase)) * riseEffectProgress
                                drawCircle(Color(0xFFF44336).copy(alpha = a.coerceIn(0f, 1f)), r, Offset(x, y))
                                drawCircle(Color(0x66EF5350).copy(alpha = (a * 0.6f).coerceIn(0f, 1f)), r * 1.8f, Offset(x, y + r * 0.2f))
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
                                val healthConnectManager = HealthConnectManager(context.applicationContext)
                                val cardioExerciseNames = listOf(
                                    "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                                    "Rowing Machine", "Stationary Bike", "Swimming"
                                )
                                scope.launch(Dispatchers.IO) {
                                    viewModel.addSampleWorkout(
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
                            }
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
                                        lerp(Color.White, Color(0xFFFF7272), hype),
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
                            goalSets = GoalSets.intValue
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
                                    Color(0xFF8B0000),
                                    Color(0xFF7A285A),
                                    riseEffectProgress
                                ).copy(alpha = (0.70f + 0.22f * hype).coerceIn(0f, 1f))
                            } else {
                                lerp(
                                    Color(0xFF650000),
                                    Color(0xFF5C1D4D),
                                    riseEffectProgress
                                ).copy(alpha = (0.45f + 0.30f * hype).coerceIn(0f, 1f))
                            },
                            label = "btnBg"
                        )
                        LaunchedEffect(Unit) {
                            while (true) {
                                val uVal =
                                    if (CurrentWeight.value > 0) "current weight is ${CurrentWeight.value}Kg"
                                    else if (CurrentTime.value < 0) "current distance walked or ran is ${currentDistance.value}km"
                                    else "current time elapsed is ${CurrentTime.value}"
                                generateAdvice(
                                    """
The user is performing ${workout.value}.
They have completed ${CurrentReps.intValue}/${GoalReps.intValue} reps and ${CurrentSets.intValue}/${GoalSets.intValue} sets.
Respond with energetic, focused encouragement only — no questions, no analysis.
Examples:
• “Keep that rhythm — power through the last few reps!”
• “Perfect pace — lock in, finish strong!”
• “Explosive form — stay tight, last push!”
The output doesn't have to be like the examples but stay in a similar layout.
Output ≤1 line, purely motivational.
""".trimIndent(),
                                    "",
                                    uVal
                                )
                                delay(60000)
                            }
                        }

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
                                            lerp(Color(0xFFFF7A7A), Color(0xFFFF3D3D), riseEffectProgress)
                                                .copy(alpha = (0.70f + 0.26f * hype).coerceIn(0f, 1f)),
                                            lerp(Color(0xFF4A1515), Color(0xFF7A1F1F), riseEffectProgress)
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
                                    timeToMillis()
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    CurrentReps.intValue += 10
                                    CurrentSets.intValue += 1
                                    EnterRestMode()
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
                                containerColor = lerp(Color(0xFF4A2515), Color(0xFF4A1F3D), riseEffectProgress).copy(alpha = 0.5f + 0.15f * hype),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        ) { Text(pauseText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = { showSyncDialog.showSyncDialog.value = true },
                            enabled = isPaused,
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lerp(Color(0xFF8B0000), Color(0xFF6C1A52), riseEffectProgress).copy(alpha = 0.8f),
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
                    CountdownOverlay(countdownValue = countdownValue)
                }
                if (showSyncDialog.showSyncDialog.value) {
                    val cardioExerciseNames = listOf(
                        "Running (Treadmill)", "Stair Climber", "Elliptical Trainer",
                        "Rowing Machine", "Stationary Bike", "Swimming"
                    )
                    ThemedConfirmationDialog(
                        title = "Sync to health connect",
                        text = "Sync this workout to health connect?",
                        buttonText = "Sync and finish workout",
                        additionalButton = true,
                        additionalButtonText = "Finish Workout Only",
                        onCustomAction = {
                            timeToMillis()
                            ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                            scope.launch(Dispatchers.IO) {
                                viewModel.addSampleWorkout(
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
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            activity?.finishAffinity()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        },
                        onDismiss = { showSyncDialog.showSyncDialog.value = false },
                        onConfirm = {
                            WorkoutForegroundService.stop(context)
                            val healthConnectManager = HealthConnectManager(context.applicationContext)
                            scope.launch {
                                WorkoutForegroundService.stop(context)
                                timeToMillis()
                                ConnectedWorkout.currentMode.value = WorkoutMode.INACTIVE
                                scope.launch(Dispatchers.IO) {
                                    viewModel.addSampleWorkout(
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
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                activity?.finishAffinity()
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            }
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
    goalSets: Int
) {
    val accent = Color(0xFF8B0000)
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
                            .background(Color(0xFF3D0000).copy(0.12f), RoundedCornerShape(18.dp))
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
                            .background(Color(0xFF3D0000).copy(0.12f), RoundedCornerShape(18.dp))
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
                DetailedSetsProgressBar(currentSet = currentSet, goalSets = goalSets)
            }
        }
    }
}

@Composable
fun DetailedSetsProgressBar(currentSet: Int, goalSets: Int, modifier: Modifier = Modifier) {
    if (goalSets <= 0) return
    val target = when {
        goalSets <= 1 -> if (currentSet >= 1) 1f else 0f
        else -> ((currentSet - 1).toFloat() / (goalSets - 1).toFloat()).coerceIn(0f, 1f)
    }
    val progress by animateFloatAsState(
        targetValue = target,
        label = "SetProgressBarProgress",
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

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
            delay(42)
        }
    }

    val shimmer = (animationClock / 1.8f) % 1.4f - 0.2f
    val pulse = 1.05f + 0.15f * sin(animationClock * 2 * PI.toFloat())

    val accent = Color(0xFF8B0000)
    val accentBright = Color(0xFFFF6666).copy(alpha = 0.95f)

    if (goalSets <= 8) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val y = size.height / 2f
            val base = 6.dp.toPx()
            val prog = 9.dp.toPx()
            val dot = 8.dp.toPx()
            val startPad = dot
            val endPad = dot
            val w = size.width - startPad - endPad

            drawLine(
                color = Color.White.copy(0.1f),
                start = Offset(startPad, y),
                end = Offset(startPad + w, y),
                strokeWidth = base,
                cap = StrokeCap.Round
            )
            if (progress > 0f) {
                val endX = startPad + w * progress
                drawLine(
                    brush = Brush.horizontalGradient(listOf(accent, accentBright)),
                    start = Offset(startPad, y),
                    end = Offset(endX, y),
                    strokeWidth = prog,
                    cap = StrokeCap.Round
                )
            }
            val shProg = shimmer
            val shWidth = w * 0.4f
            val shStart = (w + shWidth) * shProg - shWidth + startPad
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
            (1..goalSets).forEach { i ->
                val x = if (goalSets > 1) startPad + (w * ((i - 1).toFloat() / (goalSets - 1))) else size.width / 2f
                val completed = i < currentSet
                val current = i == currentSet
                val dotR = 8.dp.toPx()
                if (completed) {
                    drawCircle(color = accent, radius = dotR, center = Offset(x, y))
                } else if (current) {
                    val r = dotR * pulse
                    drawCircle(color = Color.White, radius = r, center = Offset(x, y))
                    drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(x, y))
                } else {
                    drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(x, y))
                }
            }
        }
    } else {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val strokeBase = 8.dp.toPx()
            val strokeProg = 10.dp.toPx()
            val dotR = 6.dp.toPx()
            val pad = 16.dp.toPx()
            val radius = (min(size.width, size.height) / 2f) - (strokeProg + pad)
            val center = Offset(size.width / 2f, size.height / 2f)
            val startAngle = -90f
            val sweep = 360f
            drawCircle(
                color = Color.White.copy(0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeBase, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = startAngle,
                    sweepAngle = sweep * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeProg, cap = StrokeCap.Round)
                )
            }
            val shAngle = ((shimmer.coerceIn(0f, 1f)) * 360f)
//            drawArc(
//                brush = Brush.sweepGradient(
//                    listOf(
//                        Color.Transparent,
//                        Color.White.copy(0.18f),
//                        Color.Transparent
//                    )
//                ),
//                startAngle = startAngle + shAngle - 20f,
//                sweepAngle = 40f,
//                useCenter = false,
//                topLeft = Offset(center.x - radius, center.y - radius),
//                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
//                style = Stroke(width = strokeBase)
//            )
            (1..goalSets).forEach { i ->
                val t = if (goalSets > 1) (i - 1).toFloat() / (goalSets - 1).toFloat() else 0.5f
                val ang = Math.toRadians((startAngle + t * sweep).toDouble())
                val cx = (center.x + cos(ang).toFloat() * radius)
                val cy = (center.y + sin(ang).toFloat() * radius)
                val completed = i < currentSet
                val current = i == currentSet
                if (completed) {
                    drawCircle(color = accent, radius = dotR, center = Offset(cx, cy))
                } else if (current) {
                    val r = dotR * pulse
                    drawCircle(color = Color.White, radius = r, center = Offset(cx, cy))
                    drawCircle(color = accent.copy(0.6f), radius = r * 1.5f, center = Offset(cx, cy))
                } else {
                    drawCircle(color = Color.White.copy(0.4f), radius = dotR, center = Offset(cx, cy))
                }
            }
        }
    }
}






@Composable
private fun distancePillBackground(): Brush = Brush.linearGradient(
    listOf(
        DarkMaroon.copy(alpha = 0.85f),
        DeepRed.copy(alpha = 0.65f)
    )
)

@Composable
private fun distanceIconBackground(pressed: Boolean): Brush {
    val start = if (pressed) Crimson.copy(alpha = 0.25f) else Crimson.copy(alpha = 0.20f)
    val end = if (pressed) Crimson.copy(alpha = 0.12f) else DarkMaroon.copy(alpha = 0.10f)
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
            if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                SegmentedButton(
                    text = "Time",
                    isSelected = selectedType == "Time",
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    onClick = { onTypeSelected("Time") }
                )
            }
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
            if (!canShowDistance && ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
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

private val Crimson = Color(0xFFDC143C)
private val DarkMaroon = Color(0xFF2C0A0A)
private val DeepRed = Color(0xFF7B1113)
private val SoftRed = Color(0x33DC143C)

@Composable
private fun segmentContainerBrush(selected: Boolean, pressed: Boolean): Brush {
    return if (selected) {

        Brush.linearGradient(
            listOf(
                DarkMaroon.copy(alpha = if (pressed) 0.95f else 0.90f),
                DeepRed.copy(alpha = if (pressed) 0.75f else 0.65f)
            )
        )
    } else {

        val start = if (pressed) Crimson.copy(alpha = 0.10f) else Color.Transparent
        val end = if (pressed) DarkMaroon.copy(alpha = 0.06f) else Color.Transparent
        Brush.linearGradient(listOf(start, end))
    }
}

@Composable
private fun segmentBorderBrush(selected: Boolean, pressed: Boolean): Brush {
    val hi = if (selected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.20f)
    val lo = if (selected) Crimson.copy(alpha = if (pressed) 0.35f else 0.25f)
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
private fun defaultSegmentShape(): Shape = RoundedCornerShape(12.dp)

@Composable
private fun pressedScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "segmentScale")
    return scale
}

@Composable
private fun selectable(connectedInactive: Boolean, onTap: () -> Unit): (() -> Unit)? =
    if (connectedInactive) onTap else null

@Composable
private fun isInactive(): Boolean =
    ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE

@Composable
private fun RowScope.SegmentedButtonInternal(
    text: String,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val inactive = isInactive()

    val scale = pressedScale(isPressed)
    val bgBrush = segmentContainerBrush(isSelected && inactive, isPressed && inactive)
    val border = segmentBorderBrush(isSelected && inactive, isPressed && inactive)
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
    onClick: () -> Unit
) {
    SegmentedButtonInternal(
        text = text,
        isSelected = isSelected,
        shape = shape,
        onClick = onClick
    )
}



@Composable
private fun stepperPillBackground(): Brush {
    return Brush.linearGradient(
        listOf(
            DarkMaroon.copy(alpha = 0.85f),
            DeepRed.copy(alpha = 0.65f)
        )
    )
}

@Composable
private fun stepperIconBackground(pressed: Boolean): Brush {
    val start = if (pressed) Crimson.copy(alpha = 0.25f) else SoftRed
    val end = if (pressed) Crimson.copy(alpha = 0.12f) else DarkMaroon.copy(alpha = 0.1f)
    return Brush.radialGradient(listOf(start, end))
}

@Composable
private fun stepperIconTint(pressed: Boolean): Color {
    val base = Crimson
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
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape)
                        .background(stepperIconBackground(decPressed))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement $label",
                        tint = stepperIconTint(decPressed),
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
                        .background(stepperPillBackground())
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
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                        .background(stepperIconBackground(incPressed)),
                    interactionSource = incInteraction
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment $label",
                        modifier = Modifier,
                        tint = stepperIconTint(incPressed)
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
    step: Double = 1.0
) {
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
                    onClick = {
                        val newValue = (value - step).coerceAtLeast(0.0)
                        onValueChange(newValue)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(decScale)
                        .clip(CircleShape)
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                        .background(stepperIconBackground(decPressed)),
                    interactionSource = decInteraction
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement $label",
                        tint = stepperIconTint(decPressed)
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
                        .width(120.dp)
                        .heightIn(min = 56.dp)
                        .clip(CircleShape)
                        .background(stepperPillBackground()),
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
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                        .background(stepperIconBackground(incPressed)),

                    interactionSource = incInteraction
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment $label",
                        tint = stepperIconTint(incPressed)
                    )
                }
            }
        }
    }
}

@Composable fun WeightSelector() = NumberStepperWeights("Weight", CurrentWeight.value, { CurrentWeight.value = it })
@Composable fun RepSelector() = NumberStepper("Reps", GoalReps.intValue, { GoalReps.intValue = it })
@Composable fun SetSelector() = NumberStepper("Sets", GoalSets.intValue, { GoalSets.intValue = it })

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

    val progress = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }
    val shockwave = remember { Animatable(0f) }

    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                delay(50)
                particles = generateParticles(isPr, aggression)
            }
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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = progress.value
            val shockwaveT = shockwave.value
            val shockwaveRadius = size.maxDimension * 0.8f * shockwaveT
            val shockwaveAlpha = (1f - shockwaveT.pow(2))

            if (shockwaveAlpha > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f * shockwaveAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = shockwaveRadius.coerceAtLeast(0.1f)
                    ),
                    radius = shockwaveRadius,
                    center = center
                )
            }

            particles.forEach { particle ->
                val particleProgress = (t * particle.maxLife).coerceIn(0f, 1f)
                if (particleProgress > 0) {
                    val easedProgress = 1 - (1 - particleProgress).pow(3)

                    val currentPos = particle.startPosition + (particle.velocity * easedProgress * density * 2f) +
                            Offset(0f, 2500f * easedProgress.pow(2) * density)

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

        val mainText = if (isPr) "NEW PR!\nMONSTER MODE" else "WORKOUT\nCOMPLETE"
        val gradient = if (isPr) {
            Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFD54F), Color(0xFFFFA000)))
        } else {
            Brush.linearGradient(listOf(Color(0xFFFF5454), Color(0xFFFF6F00), Color(0xFFFFB800)))
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
            startPosition = Offset(0f, 0f),
            velocity = Offset(cos(angle).toFloat() * speed, sin(angle).toFloat() * speed),
            size = rng.nextFloat() * 4f + 2f,
            maxLife = rng.nextFloat() * 0.6f + 0.4f
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


object showSyncDialog {
    var showSyncDialog = mutableStateOf(false)
}