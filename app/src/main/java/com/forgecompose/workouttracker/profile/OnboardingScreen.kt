package com.forgecompose.workouttracker.profile

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface OnboardingPage {
    data class FeatureHighlight(
        val title: String,
        val description: String,
        val icon: ImageVector
    ) : OnboardingPage
    object UserDataCollection : OnboardingPage
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: (
        age: String,
        height: String,
        weight: String,
        name: String,
        experience: String,
        preferredStyle: String,
        importantMuscles: List<String>
    ) -> Unit
) {
    val pages = listOf(
        OnboardingPage.FeatureHighlight(
            title = "Welcome to ForgeTrack",
            description = "Track your workouts, monitor your progress, and build consistent results.",
            icon = Icons.Default.FitnessCenter
        ),

                OnboardingPage.UserDataCollection,
        OnboardingPage.FeatureHighlight(
            title = "Analyze Your Progress",
            description = "Clean charts, real insights, steady gains.",
            icon = Icons.Default.StackedLineChart
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var preferredStyle by remember { mutableStateOf("Both") }
    var importantMuscles by remember { mutableStateOf(listOf<String>()) }
    val context = LocalContext.current
    val h = LocalHapticFeedback.current

    val baseTargets = listOf(
        listOf(Color(0xFF0B0C0E), Color(0xFF101318), Color(0xFF0A0D11)),
        listOf(Color(0xFF0C0A0C), Color(0xFF121015), Color(0xFF0A0A0D)),
        listOf(Color(0xFF0A0A0F), Color(0xFF121423), Color(0xFF08080D))
    )
    val accentTargets = listOf(
        listOf(Color(0xFF56B8FF), Color(0xFF19E1FF), Color.Transparent),
        listOf(Color(0xFFFF5A5A), Color(0xFFFFA3A3), Color.Transparent),
        listOf(Color(0xFFA28BFF), Color(0xFFCCBBFF), Color.Transparent)
    )
    val targetBase = baseTargets[pagerState.currentPage.coerceIn(0, 2)]
    val targetAccent = accentTargets[pagerState.currentPage.coerceIn(0, 2)]
    val b0 by animateColorAsState(targetBase[0], tween(500), label = "b0")
    val b1 by animateColorAsState(targetBase[1], tween(500), label = "b1")
    val b2 by animateColorAsState(targetBase[2], tween(500), label = "b2")
    val g1 by animateColorAsState(targetAccent[0], tween(500), label = "g1")
    val g2 by animateColorAsState(targetAccent[1], tween(500), label = "g2")

    val wobble = rememberInfiniteTransition(label = "bg")
    val k1 by wobble.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "k1")
    val k2 by wobble.animateFloat(1f, 0f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "k2")
    val k3 by wobble.animateFloat(-1f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "k3")

    LaunchedEffect(pagerState.currentPage) { h.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val db = FirebaseFirestore.getInstance()
    val usersRef = db.collection("userName")






    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(b0)
            .drawBehind {
                val cx = size.width * (0.36f + 0.08f * k1)
                val cy = size.height * (0.38f + 0.04f * k2)
                val r = size.minDimension * (0.85f + 0.08f * abs(k3))
                drawRect(brush = Brush.verticalGradient(listOf(b1, b2)), size = size)
                drawCircle(brush = Brush.radialGradient(listOf(g1.copy(alpha = 0.28f), Color.Transparent)), center = Offset(cx, cy), radius = r)
                drawCircle(brush = Brush.radialGradient(listOf(g2.copy(alpha = 0.14f), Color.Transparent)), center = Offset(size.width * (0.72f - 0.05f * k2), size.height * (0.76f - 0.05f * k1)), radius = r * 0.62f)
            }
            .systemBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                userScrollEnabled = false
            ) { pageIndex ->
                when (val page = pages[pageIndex]) {
                    is OnboardingPage.FeatureHighlight -> FeaturePageLayout(page = page)
                    is OnboardingPage.UserDataCollection -> {
                        UserDataCollectionLayout(
                            name = name,
                            onNameChange = {
                                name = it
                                if (it.isNotBlank()) hapticSuccess(context) else h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            age = age,
                            onAgeChange = { age = it },
                            height = height,
                            onHeightChange = { height = it },
                            weight = weight,
                            onWeightChange = { weight = it },
                            experience = experience,
                            onExperienceChange = { experience = it },
                            preferredStyle = preferredStyle,
                            onPreferredStyleChange = {
                                preferredStyle = it
                                h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            importantMuscles = importantMuscles,
                            onImportantMusclesChange = {
                                importantMuscles = it
                                h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }

            val pulse = rememberInfiniteTransition(label = "btnPulse")
            val pulseAlpha by pulse.animateFloat(
                0.9f,
                1f,
                infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "glow"
            )

            val canContinue = when (pages[pagerState.currentPage]) {
                is OnboardingPage.UserDataCollection -> name.isNotBlank()
                else -> true
            }
            var lastCan by remember { mutableStateOf(false) }
            LaunchedEffect(canContinue) {
                if (!lastCan && canContinue) hapticSuccess(context)
                lastCan = canContinue
            }
            val data2 = mapOf("userName" to name)
            val usersRef = db.collection("userName")
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .drawWithCache {
                        val glass = Brush.verticalGradient(0f to Color.White.copy(0.03f), 1f to Color.White.copy(0.012f))
                        onDrawBehind {
                            drawRoundRect(brush = glass, cornerRadius = CornerRadius(32.dp.toPx()))
                            drawRoundRect(
                                brush = Brush.linearGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.05f))),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                                cornerRadius = CornerRadius(32.dp.toPx())
                            )
                        }
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    HorizontalPagerIndicator(pageCount = pages.size, currentPage = pagerState.currentPage)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backEnabled = pagerState.currentPage > 0
                    MinimalGhostButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        enabled = backEnabled
                    ) {
                        if (backEnabled) h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val p = pagerState.currentPage
                        if (p > 0) scope.launch { pagerState.animateScrollToPage(p - 1) }
                    }
                    Spacer(Modifier.weight(1f))
                    PrimaryCTAButton(
                        text = if (pagerState.currentPage == pages.size - 1) "Finish" else "Next",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        pulse = pulseAlpha,
                        enabled = canContinue,
                        tint = g2
                    ) {
                        if (!canContinue) {
                            hapticError(context)
                            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                            return@PrimaryCTAButton
                        }
                        hapticClick(context)
                        val p = pagerState.currentPage
                        if (pages[p] is OnboardingPage.UserDataCollection) {
                            scope.launch { pagerState.animateScrollToPage(p + 1) }
                        } else if (p == pages.size - 1) {
                            onOnboardingComplete(
                                age.ifBlank { "Unknown" },
                                height.ifBlank { "Unknown" },
                                weight.ifBlank { "Unknown" },
                                name,
                                experience.ifBlank { "0" },
                                preferredStyle,
                                importantMuscles,


                            )
                            hapticSuccess(context)
                            usersRef.document(name)
                                .set(data2)
                                .addOnSuccessListener { Log.d("Firestore", "Username saved!") }
                                .addOnFailureListener { Log.e("Firestore", "Failed to save username", it) }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(p + 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePageLayout(page: OnboardingPage.FeatureHighlight) {
    val alpha by animateFloatAsState(1f, tween(420), label = "fa")
    val up by animateDpAsState(0.dp, tween(420, easing = FastOutSlowInEasing), label = "fy")
    val floaty = rememberInfiniteTransition(label = "float")
    val bob by floaty.animateFloat(-4f, 4f, infiniteRepeatable(animation = tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "bob")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .graphicsLayer { this.alpha = alpha; translationY = up.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                .graphicsLayer { translationY = bob },
            contentAlignment = Alignment.Center
        ) {
            Icon(page.icon, null, modifier = Modifier.size(84.dp), tint = Color.White)
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(page.description, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.82f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserDataCollectionLayout(
    age: String, onAgeChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    name: String, onNameChange: (String) -> Unit,
    experience: String, onExperienceChange: (String) -> Unit,
    preferredStyle: String, onPreferredStyleChange: (String) -> Unit,
    importantMuscles: List<String>, onImportantMusclesChange: (List<String>) -> Unit
) {
    val styles = listOf("Calisthenics", "Weights", "Both")
    val muscles = listOf("Chest", "Arms", "Legs", "Back", "Core", "Shoulders")
    val h = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Tell Us About Yourself", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                onNameChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            placeholder = { Text("Enter your name") }
        )

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth()) {
            SwipeNumberField(
                label = "Age • optional",
                valueText = age,
                onValueText = {
                    onAgeChange(it)
                },
                step = 1f,
                range = 5f..100f
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth()) {
            SwipeNumberField(
                label = "Experience (yrs) • optional",
                valueText = experience,
                onValueText = {
                    onExperienceChange(it)
                },
                step = 0.5f,
                range = 0f..40f,
                decimals = 1
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth()) {
            SwipeNumberField(
                label = "Height (cm) • optional",
                valueText = height,
                onValueText = {
                    onHeightChange(it)
                },
                step = 1f,
                range = 80f..240f
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth()) {
            SwipeNumberField(
                label = "Weight (kg) • optional",
                valueText = weight,
                onValueText = {
                    onWeightChange(it)
                },
                step = 0.5f,
                range = 20f..300f,
                decimals = 1
            )
        }

        Spacer(Modifier.height(18.dp))

        Text("Preferred Workout Style • optional", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        SegmentedSelector(
            options = styles,
            selected = preferredStyle,
            onSelected = {
                onPreferredStyleChange(it)
                h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        )

        Spacer(Modifier.height(18.dp))

        Text("Most Important Muscles • optional", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            muscles.forEach { muscle ->
                val selected = muscle in importantMuscles
                Pill(text = muscle, selected = selected) {
                    if (selected) onImportantMusclesChange(importantMuscles - muscle)
                    else onImportantMusclesChange(importantMuscles + muscle)
                    h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }
}

@Composable
fun HorizontalPagerIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val width by animateDpAsState(if (active) 26.dp else 10.dp, label = "w")
            val alpha by animateFloatAsState(if (active) 1f else 0.4f, label = "a")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = alpha))
            )
            if (i != pageCount - 1) Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun PrimaryCTAButton(
    text: String,
    icon: ImageVector,
    pulse: Float,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    val h = LocalHapticFeedback.current
    val bg = Brush.horizontalGradient(listOf(tint.copy(alpha = 0.95f), tint.copy(alpha = 0.72f)))
    Button(
        onClick = {
            if (enabled) h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
        modifier = Modifier
            .height(56.dp)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
            .background(bg, RoundedCornerShape(32.dp))
            .graphicsLayer { alpha = if (enabled) pulse else 0.6f }
    ) {
        Text(text)
        Spacer(Modifier.width(6.dp))
        Icon(icon, null)
    }
}

@Composable
private fun MinimalGhostButton(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val h = LocalHapticFeedback.current
    Button(
        onClick = {
            if (enabled) h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f), contentColor = Color.White),
        modifier = Modifier.height(48.dp)
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun SegmentedSelector(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { opt ->
            val active = opt == selected
            val tint by animateColorAsState(if (active) Color.Black else Color.White.copy(0.8f), tween(180), label = "segc")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Color(0xFFFF717A) else Color.Transparent)
                    .toggleable(
                        value = active,
                        enabled = true,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelected(opt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = tint, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color(0xFFFF717A) else Color.White.copy(0.06f), tween(180), label = "mc")
    val fg = if (selected) Color.Black else Color.White
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .toggleable(value = selected, onValueChange = { onToggle() }, indication = null, interactionSource = remember { MutableInteractionSource() })
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(text, color = fg, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SwipeNumberField(
    label: String,
    valueText: String,
    onValueText: (String) -> Unit,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    decimals: Int = 0
) {
    val context = LocalContext.current
    val h = LocalHapticFeedback.current
    var internal by remember { mutableStateOf(valueText) }
    var lastQuant by remember { mutableStateOf(valueText) }
    LaunchedEffect(valueText) { internal = valueText }
    val parsed = internal.toFloatOrNull()
    val displayColor by animateColorAsState(
        if (parsed == null && internal.isNotBlank()) Color(0xFFFF6B6B) else Color.White,
        tween(160),
        label = "valc"
    )
    val tilt = remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.02f))
            .pointerInput(step, range) {
                detectDragGestures(
                    onDragStart = { h.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    onDragEnd = { tilt.floatValue = 0f; hapticClick(context) },
                    onDragCancel = { tilt.floatValue = 0f }
                ) { change, drag ->
                    change.consume()
                    val dx = drag.x
                    val base = (internal.toFloatOrNull() ?: 0f)
                    val delta = (dx / 10f) * step
                    val next = (base + delta).coerceIn(range.start, range.endInclusive)
                    val rounded = if (decimals == 0) next.roundToInt().toFloat() else (next * 10f.pow(decimals)).roundToInt() / 10f.pow(decimals)
                    val newVal = rounded.format(decimals)
                    internal = newVal
                    onValueText(newVal)
                    if (newVal != lastQuant) {
                        lastQuant = newVal
                        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    tilt.floatValue = dx.coerceIn(-14f, 14f) / 14f
                }
            }
            .graphicsLayer {
                rotationY = tilt.floatValue * 5f
                translationX = tilt.floatValue * 3f
                cameraDistance = 18f * density
            }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(0.85f), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val base = (internal.toFloatOrNull() ?: 0f) - step
                        val v = base.coerceIn(range.start, range.endInclusive)
                        val nv = v.format(decimals)
                        internal = nv; onValueText(nv)
                        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.12f), CircleShape)
                ) { Icon(Icons.Filled.Remove, null, tint = Color.White, modifier = Modifier.size(18.dp)) }

                IconButton(
                    onClick = {
                        val base = (internal.toFloatOrNull() ?: 0f) + step
                        val v = base.coerceIn(range.start, range.endInclusive)
                        val nv = v.format(decimals)
                        internal = nv; onValueText(nv)
                        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.12f), CircleShape)
                ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = internal,
            onValueChange = {
                val before = internal
                internal = it.filterNumber(decimals)
                onValueText(internal)
                if (internal != before) h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(color = displayColor, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text("Swipe or tap to edit") },
            placeholder = { Text("Unknown") }
        )
    }
}

private fun Float.format(decimals: Int): String {
    return if (decimals <= 0) this.roundToInt().toString()
    else "%.${decimals}f".format(this)
}

private fun String.filterNumber(decimals: Int): String {
    val allowed = "0123456789" + if (decimals > 0) "." else ""
    val cleaned = this.filter { it in allowed }
    if (decimals > 0 && cleaned.count { it == '.' } > 1) {
        val i = cleaned.indexOf('.')
        return cleaned.substring(0, i + 1) + cleaned.substring(i + 1).replace(".", "")
    }
    return cleaned
}

fun Float.pow(p: Int): Float {
    var r = 1f
    repeat(p) { r *= this }
    return r
}

private fun vibrator(context: android.content.Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= 31) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

private fun hapticClick(context: android.content.Context) {
    val v = vibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= 29) {
        v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(25)
    }
}

private fun hapticSuccess(context: android.content.Context) {
    val v = vibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= 29) {
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 18, 60, 22), intArrayOf(90, 160, 0, 200), -1))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(longArrayOf(0, 22, 50, 26), -1)
    }
}

private fun hapticError(context: android.content.Context) {
    val v = vibrator(context) ?: return
    if (Build.VERSION.SDK_INT >= 29) {
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 14, 40, 14, 120, 24), intArrayOf(200, 0, 200, 0, 255, 0), -1))
    } else {
        @Suppress("DEPRECATION")
        v.vibrate(longArrayOf(0, 16, 40, 16, 120, 28), -1)
    }
}
