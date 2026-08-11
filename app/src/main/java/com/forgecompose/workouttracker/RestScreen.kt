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

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.GoalReps
import com.forgecompose.workouttracker.ConnectedWorkout.GoalSets
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.restTimeRemaining
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RestAdviceSection(
    modifier: Modifier = Modifier
) {
    val tips = remember {
        listOf(
            "Most of your strength recovers in the first 2–3 minutes of rest because phosphocreatine refills during that window.",
            "Walking slowly between sets keeps blood flowing and helps clear metabolites faster than just sitting.",
            "Deep belly breathing during rest lowers heart rate and lets your nervous system reset for the next heavy set.",
            "If your form broke down in the last set, lower the weight slightly next set instead of forcing ugly reps.",
            "Grip the bar the same way every set — consistent grip width keeps joints happier and progress easier to track.",
            "For compound lifts, stopping with 1–3 reps in reserve is usually enough to grow without frying recovery.",
            "If your technique gets worse each set, your rest is probably too short or the weight is too heavy.",
            "On upper-body days, light band pull-aparts or face pulls between sets can help your shoulders stay stable.",
            "If your breathing is still chaotic when the timer ends, add 20–30 seconds before your next heavy set.",
            "Most injuries happen when you’re tired and rushing — use rest time to reset stance, grip, and bracing.",
            "Shaking your arms or legs between sets can help reduce local stiffness and keep range of motion smoother.",
            "Rest longer after heavy compound sets than isolation work — your nervous system needs more time to reset.",
            "If a joint, not a muscle, is what feels tired, adjust the setup or range of motion before the next set.",
            "Logging your last set’s reps and RPE during rest turns each session into data you can actually improve from.",
            "If a muscle never feels involved in a compound lift, use rest time to quickly practice a lighter, slower rep.",
            "The last 3–5 controlled reps before failure drive most hypertrophy — warm-up sets don’t need to be hard.",
            "Use the first 10 seconds of rest to rate the set in your head: too easy, on point, or too hard — then adjust.",
            "Stretching aggressively between heavy sets can lower force output — stick to gentle mobility, not deep stretches.",
            "If your heart rate stays sky-high for several sets in a row, reduce load or volume to avoid digging too deep.",
            "Tilting your phone up and shoulders back during rest keeps your upper back out of “phone hunch” mode.",
            "Calves, forearms, and abs usually recover faster — they can handle shorter rest than heavy squats or deadlifts.",
            "If tempo slipped (faster eccentrics, rushed reps), consciously slow down your first rep of the next set.",
            "Neck and jaw tension during lifts wastes energy — use rest to unclench and reset head position.",
            "Using the same rest length every session turns your training into a controlled experiment instead of chaos.",
            "Dynamic warm-ups increase blood flow and range of motion without reducing power output like static stretching does.",
            "Mild dehydration of just 2% can significantly reduce strength and endurance performance.",
            "The eccentric phase (lowering the weight) causes the most muscle damage and growth stimulus — don't drop the weight.",
            "Sleep is when growth hormone peaks; missing sleep literally reduces the muscle-building effect of your workout.",
            "Progressive overload isn't just adding weight; adding reps, sets, or improving technique counts as progress too.",
            "Hypertrophy can occur across a wide rep range (5–30) as long as you are training close to muscular failure.",
            "Internal cues (focusing on the muscle squeezing) are better for isolation; external cues (moving the bar) suit heavy compounds.",
            "Caffeine taken 30–60 minutes pre-workout is a proven ergogenic aid that reduces perceived effort.",
            "Total daily protein intake matters far more for muscle retention than rushing to drink a shake immediately post-workout.",
            "Active recovery days (walking, light swimming) clear metabolic waste better than complete bed rest.",
            "Systemic fatigue accumulates over weeks; a scheduled deload week helps prevent burnout and central nervous system fry.",
            "Creatine monohydrate aids in recycling ATP, helping you squeeze out maybe one or two extra reps on heavy sets.",
            "Training to absolute failure on every set increases recovery time disproportionately compared to the extra growth stimulus gained.",
            "Compound movements spike testosterone transiently, but consistent mechanical tension is the primary driver of growth."
        )
    }

    var index by remember { mutableStateOf(0) }


    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            index = (index + 1) % tips.size
        }
    }

    val accent = Color(0xFF7BD1FF)
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = Color(0xFF020A11).copy(alpha = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val borderBrush = Brush.linearGradient(
                        listOf(
                            Color(0xFF7BD1FF).copy(alpha = 0.55f),
                            Color(0xFF3B9CF8).copy(alpha = 0.35f)
                        )
                    )
                    val bgBrush = Brush.radialGradient(
                        listOf(
                            Color(0xFF041523).copy(alpha = 0.7f),
                            Color(0xFF020A11)
                        )
                    )
                    onDrawBehind {
                        drawRoundRect(
                            brush = bgBrush,
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            style = Stroke(width = 1.dp.toPx()),
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rest tip",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tips[index],
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.97f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize(
                            animationSpec = tween(300)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RestScreen(
    navController: NavController,
    bpVM: HrViewModel,

) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
val scope = rememberCoroutineScope()
    // Cap the display refresh rate at 60 fps for this screen; the timer text
    // only changes once a second and the ring animation doesn't need 120 Hz.
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            scope.launch(Dispatchers.Main) {
                val attrs = window.attributes
                delay(1750)
                attrs.preferredRefreshRate = 60f
                window.attributes = attrs
            }
        }
        onDispose {
            if (window != null) {
                val attrs = window.attributes
                attrs.preferredRefreshRate = 0f   // restore: let system choose
                window.attributes = attrs
            }
        }
    }

    val initialTotal = rememberSaveable { ConnectedWorkout.restTime.longValue }
    val autoRestTimeEnabled = remember { isAutoRestTimeEnabled(context) }
    val liveHeartRate by bpVM.hr.collectAsState()
    var restOutcomeHandled by rememberSaveable { mutableStateOf(false) }
    PreventBackGesture()

    fun applyAutoRestAdjustment(skipped: Boolean) {
        if (restOutcomeHandled) return
        restOutcomeHandled = true
        ConnectedWorkout.restTime.longValue = AutoRestTimer.finishRest(
            currentRestMillis = initialTotal,
            skipped = skipped,
            autoAdjustEnabled = autoRestTimeEnabled
        )
        ConnectedWorkout.saveSnapshot(context)
    }

    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF02121E), Color(0xFF031A28), Color(0xFF06273A), Color(0xFF09324A))
            in 11..16 -> listOf(Color(0xFF031420), Color(0xFF052133), Color(0xFF073049), Color(0xFF0A3F60))
            in 17..20 -> listOf(Color(0xFF020914), Color(0xFF041223), Color(0xFF08233E), Color(0xFF0B3356))
            else      -> listOf(Color(0xFF00040A), Color(0xFF041222), Color(0xFF0A2440), Color(0xFF0F3256))
        }
    }
    val introProgress = 1f
    LaunchedEffect(Unit) {
        Firebase.crashlytics.setCustomKey("current_screen", "Rest Screen")
    }

    val currentSetCount = CurrentSets.intValue.coerceAtLeast(1)

    LaunchedEffect(currentSetCount) {
        val repsPerSet = (GoalReps.intValue / GoalSets.intValue.coerceAtLeast(1)).toString()
        val weightPerSet = String.format("%.1f", CurrentWeight.value)
        while (WorkoutLog.sets.size < currentSetCount) {
            WorkoutLog.sets.add(
                SetRecord(
                    reps = mutableStateOf(repsPerSet),
                    weight = mutableStateOf(weightPerSet)
                )
            )
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    val totalReps by remember {
        derivedStateOf {
            WorkoutLog.sets.sumOf { it.reps.value.toIntOrNull() ?: 0 }
        }
    }

    LaunchedEffect(totalReps) {
        CurrentReps.intValue = totalReps
    }

    val blurAnim = 0.dp

    WorkoutTrackerTheme {
        AnimatedBackdropBlue(
            modifier = Modifier.fillMaxSize(),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves = false,
            enableAnimation = false
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurAnim)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))

                val ringSize = 300.dp

                RestCountdownSection(
                    modifier = Modifier.size(ringSize),
                    initialTotal = initialTotal,
                    heartRateBpm = liveHeartRate,
                    useHeartRateRecovery = autoRestTimeEnabled,
                    onFinished = {
                        applyAutoRestAdjustment(skipped = false)
                        ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                        navController.navigate("WorkoutScreen") {
                            popUpTo("WorkoutScreen") { inclusive = true }
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))
                RestHeartRateCard(
                    bpVM = bpVM,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))

                var isExpanded by remember { mutableStateOf(false) }
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "expand_icon")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 32.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF89D8FF).copy(alpha = 0.04f)
                    ) {}

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    0f to Color(0xFFFFFFFF).copy(alpha = 0.06f),
                                    1f to Color(0xFF000000).copy(alpha = 0.06f)
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    isExpanded = !isExpanded
                                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Workout Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.rotate(rotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    itemsIndexed(WorkoutLog.sets) { index, record ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {
                                            IconButton(
                                                onClick = {
                                                    WorkoutLog.sets.removeAt(index)
                                                    CurrentSets.intValue -= 1
                                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                }

                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Set",
                                                    tint = Color.White.copy(alpha = 0.72f),
                                                    modifier = Modifier.size(36.dp).padding(4.dp)
                                                )

                                            }


                                            EditableSetRow(
                                                setNumber = index + 1,
                                                record = record
                                            )
                                        }

                                        if (index < WorkoutLog.sets.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                thickness = (0.7).dp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "btnScale")

                val progressAnimation = remember { Animatable(0f) }
                LaunchedEffect(pressed) {
                    val spec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)
                    progressAnimation.animateTo(if (pressed) 1f else 0f, spec)
                }

                val buttonShape = RoundedCornerShape(32.dp)
                RestAdviceSection(
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .clip(buttonShape)
                        .graphicsLayer {
                            val liquidScale = lerp(1f, 1.08f, progressAnimation.value)
                            scaleX = scale * liquidScale
                            scaleY = scale * liquidScale
                        }
                        .background(
                            color = Color(0xFF89D8FF).copy(alpha = 0.07f),
                            shape = buttonShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            applyAutoRestAdjustment(skipped = true)
                            ConnectedWorkout.currentMode.value = WorkoutMode.ACTIVE
                            navController.navigate("WorkoutScreen") {
                                popUpTo("WorkoutScreen") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0C1A26).copy(alpha = 0.38f),
                            contentColor = Color(0xFFE6F7FF)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        interactionSource = interactionSource
                    ) {
                        Text("Skip Rest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RestCountdownSection(
    initialTotal: Long,
    heartRateBpm: Int,
    useHeartRateRecovery: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remaining by restTimeRemaining
    val latestHeartRate by rememberUpdatedState(heartRateBpm)
    val heartRateRecoveryEnabled by rememberUpdatedState(useHeartRateRecovery)

    LaunchedEffect(initialTotal) {
        remaining = initialTotal
        while (remaining > 0) {
            delay(1000)
            remaining = if (heartRateRecoveryEnabled) {
                AutoRestTimer.nextRestCountdownMillis(
                    remainingMillis = remaining,
                    heartRateBpm = latestHeartRate
                )
            } else {
                (remaining - 1000L).coerceAtLeast(0L)
            }
        }
        onFinished()
    }

    val safeTotal = initialTotal.coerceAtLeast(1L)
    // Only recomputed when `remaining` changes (once per second). The smooth
    // animation lives entirely inside RestTimerCanvas so this composable does
    // NOT recompose every frame.
    val progressTarget by remember {
        derivedStateOf {
            (1f - (remaining.toFloat() / safeTotal.toFloat())).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        RestTimerCanvas(progressTarget = progressTarget)
        RestTimerText(remaining = remaining)
    }
}

// Canvas-only leaf — recomposes just once per second (when progressTarget
// changes). The Animatable smooths progress between ticks; its value is read
// inside onDrawWithContent (draw phase), so no extra recomposition occurs
// during the 800 ms animation window.
@Composable
private fun RestTimerCanvas(
    progressTarget: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(progressTarget) }
    LaunchedEffect(progressTarget) {
        animatedProgress.animateTo(
            targetValue = progressTarget,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val center = Offset(cx, cy)
                val stroke = 18f
                val radius = min(w, h) / 2f - stroke

                // Static geometry — cached until size changes.
                val bgBrush = Brush.radialGradient(
                    listOf(Color(0xFF0A1420), Color(0xFF0F1E2E)),
                    center = center,
                    radius = radius * 1.2f
                )
                val arcBrush = Brush.sweepGradient(
                    0f to Color(0xFF3B9CF8),
                    0.28f to Color(0xFF54C7FF),
                    0.64f to Color(0xFF9CEBFF),
                    1f to Color(0xFF3B9CF8),
                    center = center
                )
                val arcRectTopLeft = Offset(cx - radius, cy - radius)
                val arcRectSize = Size(radius * 2, radius * 2)
                val deg2rad = (Math.PI / 180.0).toFloat()
                val innerR = radius - stroke * 0.6f
                val outerR = radius + stroke * 0.6f
                val tickSegments: List<Pair<Offset, Offset>> =
                    (0..100 step 10).map { i ->
                        val ang = (i * 3.6f - 90f) * deg2rad
                        val c = cos(ang)
                        val s = sin(ang)
                        Offset(cx + c * innerR, cy + s * innerR) to
                                Offset(cx + c * outerR, cy + s * outerR)
                    }

                onDrawWithContent {
                    // Reading animatedProgress.value here (draw phase) causes
                    // a draw-only invalidation each animation frame — never a
                    // full recomposition.
                    val p = animatedProgress.value
                    val glow = lerp(0.45f, 0.70f, p)

                    drawCircle(
                        brush = bgBrush,
                        radius = radius,
                        center = center,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = Color(0xFF7BD1FF).copy(alpha = 0.10f + 0.07f * glow),
                        radius = radius,
                        center = center,
                        style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round)
                    )
                    tickSegments.forEachIndexed { idx, (start, end) ->
                        val major = (idx % 2 == 0)
                        drawLine(
                            color = Color(0xFF7BD1FF).copy(alpha = if (major) 0.35f else 0.15f),
                            start = start,
                            end = end,
                            strokeWidth = if (major) 4f else 2f,
                            cap = StrokeCap.Round
                        )
                    }
                    val sweep = 360f * p
                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = arcRectTopLeft,
                        size = arcRectSize
                    )
                    drawArc(
                        color = Color(0xFF7BD1FF).copy(alpha = 0.16f + 0.10f * glow),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke * 1.7f, cap = StrokeCap.Round),
                        topLeft = arcRectTopLeft,
                        size = arcRectSize
                    )
                    if (p > 0f) {
                        val capAng = (sweep - 90f) * deg2rad
                        val capCenter = Offset(cx + cos(capAng) * radius, cy + sin(capAng) * radius)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFB2EBFF), Color.Transparent),
                                center = capCenter,
                                radius = 26f
                            ),
                            center = capCenter,
                            radius = 26f * (0.7f + 0.3f * glow),
                            alpha = 0.85f
                        )
                        drawCircle(color = Color(0xFFCCF4FF), center = capCenter, radius = 6f)
                    }
                }
            }
    ) {}
}

// Text-only leaf — recomposes exactly once per second when `remaining` ticks.
// Completely isolated from the canvas animation above.
@Composable
private fun RestTimerText(
    remaining: Long,
    modifier: Modifier = Modifier
) {
    val seconds = (remaining / 1000) % 60
    val minutes = (remaining / (1000 * 60)) % 60
    val hours = remaining / (1000 * 60 * 60)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}

@Composable
private fun RestHeartRateCard(
    bpVM: HrViewModel,
    modifier: Modifier = Modifier
) {
    val latestBpmRef = remember { AtomicInteger(0) }
    var bpm by remember { mutableIntStateOf(0) }
    var sum by remember { mutableFloatStateOf(0f) }
    var count by remember { mutableFloatStateOf(0f) }
    // derivedStateOf means avg/zone only propagate to children when their
    // computed value actually changes, not on every bpm tick.
    val avg by remember { derivedStateOf { if (count > 0f) (sum / count).toInt() else 0 } }
    val zone by remember { derivedStateOf { restHrZone(bpm) } }

    LaunchedEffect(bpVM) {
        bpVM.hr.collect { incoming ->
            if (incoming > 0) latestBpmRef.set(incoming)
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            val sampled = latestBpmRef.get()
            if (sampled > 0) {
                bpm = sampled
                sum += sampled
                count += 1f
            }
            delay(1000)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF061422).copy(alpha = 0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RestHrReadout(bpm = bpm, avg = avg, zone = zone)
            RestHrZoneBars(zone = zone)
        }
    }
}

// Recomposes when bpm or avg changes (once per second).
@Composable
private fun RestHrReadout(bpm: Int, avg: Int, zone: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Heart rate",
            tint = Color(0xFF7BD1FF)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (bpm > 0) "$bpm BPM" else "-- BPM",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (avg > 0) "Avg $avg | Z$zone" else "Avg -- | --",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.82f)
        )
    }
}

// Recomposes only when the HR zone crosses a threshold — typically once or
// twice per rest period, not every second.
@Composable
private fun RestHrZoneBars(zone: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (z in 1..5) {
            val active = zone > 0 && z == zone
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) Color(0xFF3B9CF8).copy(alpha = 0.9f)
                        else Color(0xFF7BD1FF).copy(alpha = 0.18f)
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z$z",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = if (active) 1f else 0.7f),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun restHrZone(bpm: Int): Int {
    return when {
        bpm <= 0  -> 0   // no reading — zone bars stay all-dim
        bpm < 110 -> 1
        bpm < 130 -> 2
        bpm < 150 -> 3
        bpm < 170 -> 4
        else      -> 5
    }
}
