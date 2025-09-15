package com.forgecompose.workouttracker

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
            title = "Welcome to IronTrack",
            description = "Your personal companion to log, track, and conquer your fitness goals.",
            icon = Icons.Default.FitnessCenter
        ),
        OnboardingPage.UserDataCollection,
        OnboardingPage.FeatureHighlight(
            title = "Analyze Your Progress",
            description = "View your workout history and personal records to stay motivated and informed.",
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

    val currentPage = pagerState.currentPage

    val baseTargets = listOf(
        listOf(Color(0xFF070707), Color(0xFF101010), Color(0xFF000000)),
        listOf(Color(0xFF140505), Color(0xFF1F0A0A), Color(0xFF090202)),
        listOf(Color(0xFF120404), Color(0xFF2A0D0D), Color(0xFF0A0303))
    )
    val accentTargets = listOf(
        listOf(Color(0xFF2A86FF), Color(0xFF00E5FF), Color.Transparent),
        listOf(Color(0xFFFF3D3D), Color(0xFFFF7A7A), Color.Transparent),
        listOf(Color(0xFF7C3DFF), Color(0xFFB07AFF), Color.Transparent)
    )

    val targetBase = baseTargets[currentPage.coerceIn(0, 2)]
    val targetAccent = accentTargets[currentPage.coerceIn(0, 2)]

    val b0 by animateColorAsState(targetBase[0], tween(700), label = "b0")
    val b1 by animateColorAsState(targetBase[1], tween(700), label = "b1")
    val b2 by animateColorAsState(targetBase[2], tween(700), label = "b2")
    val g0 by animateColorAsState(targetAccent[0], tween(700), label = "g0")
    val g1 by animateColorAsState(targetAccent[1], tween(700), label = "g1")
    val g2 by animateColorAsState(targetAccent[2], tween(700), label = "g2")

    val wobble = rememberInfiniteTransition(label = "bg")
    val k1 by wobble.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "k1")
    val k2 by wobble.animateFloat(1f, 0f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "k2")
    val k3 by wobble.animateFloat(-1f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "k3")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(b0)
            .drawBehind {
                val cx = size.width * (0.35f + 0.1f * k1)
                val cy = size.height * (0.35f + 0.05f * k2)
                val r = size.minDimension * (0.9f + 0.1f * kotlin.math.abs(k3))
                drawRect(brush = Brush.verticalGradient(listOf(b1, b2)), size = size)
                drawCircle(brush = Brush.radialGradient(listOf(g0.copy(alpha = 0.35f), Color.Transparent), center = Offset(cx, cy), radius = r))
                drawCircle(brush = Brush.radialGradient(listOf(g1.copy(alpha = 0.25f), Color.Transparent), center = Offset(size.width * (0.7f - 0.05f * k2), size.height * (0.75f - 0.06f * k1)), radius = r * 0.75f))
                drawCircle(brush = Brush.radialGradient(listOf(g2.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width * (0.2f + 0.06f * k3), size.height * (0.8f + 0.03f * k2)), radius = r * 0.6f))
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
                            onNameChange = { name = it },
                            age = age,
                            onAgeChange = { age = it },
                            height = height,
                            onHeightChange = { height = it },
                            weight = weight,
                            onWeightChange = { weight = it },
                            experience = experience,
                            onExperienceChange = { experience = it },
                            preferredStyle = preferredStyle,
                            onPreferredStyleChange = { preferredStyle = it },
                            importantMuscles = importantMuscles,
                            onImportantMusclesChange = { importantMuscles = it }
                        )
                    }
                }
            }

            val pulse = rememberInfiniteTransition(label = "btnPulse")
            val glow by pulse.animateFloat(0.65f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.35f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalPagerIndicator(pageCount = pages.size, currentPage = pagerState.currentPage)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backEnabled = pagerState.currentPage > 0
                    Button(
                        onClick = {
                            val p = pagerState.currentPage
                            if (p > 0) scope.launch { pagerState.animateScrollToPage(p - 1) }
                        },
                        enabled = backEnabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back")
                    }
                    Spacer(Modifier.width(12.dp))
                    val canContinue = if (pages[pagerState.currentPage] is OnboardingPage.UserDataCollection) {
                        age.isNotBlank() && height.isNotBlank() && weight.isNotBlank()
                    } else true
                    Button(
                        onClick = {
                            val p = pagerState.currentPage
                            if (pages[p] is OnboardingPage.UserDataCollection) {
                                if (canContinue) {
                                    scope.launch { pagerState.animateScrollToPage(p + 1) }
                                } else {
                                    Toast.makeText(context, "Please fill in Age, Height, and Weight", Toast.LENGTH_SHORT).show()
                                }
                            } else if (p == pages.size - 1) {
                                onOnboardingComplete(
                                    age,
                                    height,
                                    weight,
                                    name,
                                    experience,
                                    preferredStyle,
                                    importantMuscles
                                )
                            } else {
                                scope.launch { pagerState.animateScrollToPage(p + 1) }
                            }
                        },
                        enabled = canContinue || pagerState.currentPage != 1,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = g1.copy(alpha = 0.9f), contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .graphicsLayer { alpha = glow }
                    ) {
                        Text(if (pagerState.currentPage == pages.size - 1) "Finish" else "Next")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePageLayout(page: OnboardingPage.FeatureHighlight) {
    val enter by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(if (enter) 1f else 0f, tween(600), label = "fa")
    val up by animateDpAsState(0.dp, tween(600, easing = FastOutSlowInEasing), label = "fy")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .graphicsLayer { this.alpha = alpha; translationY = up.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(page.icon, null, modifier = Modifier.size(96.dp), tint = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
        Spacer(Modifier.height(32.dp))
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
    val styles = listOf("Cardio", "Weights", "Both")
    val muscles = listOf("Chest", "Arms", "Legs", "Back", "Core", "Shoulders")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Tell Us About Yourself", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                modifier = Modifier.weight(1f),
                label = { Text("Age") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = experience,
                onValueChange = onExperienceChange,
                modifier = Modifier.weight(1f),
                label = { Text("Experience (yrs)") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Height (cm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                modifier = Modifier.weight(1f),
                label = { Text("Weight (kg)") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Preferred Workout Style", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            styles.forEach { style ->
                FilterChip(
                    selected = preferredStyle == style,
                    onClick = { onPreferredStyleChange(style) },
                    label = { Text(style) }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("Most Important Muscles", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp)
        ) {
            muscles.forEach { muscle ->
                val selected = muscle in importantMuscles
                FilterChip(
                    shape = RoundedCornerShape(16.dp),
                    selected = selected,
                    onClick = {
                        if (selected) onImportantMusclesChange(importantMuscles - muscle)
                        else onImportantMusclesChange(importantMuscles + muscle)
                    },
                    label = { Text(muscle) }
                )
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
            val width by animateDpAsState(targetValue = if (active) 36.dp else 10.dp, label = "w")
            val alpha by animateFloatAsState(if (active) 1f else 0.45f, label = "a")
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(width)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = alpha))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(100))
            )
            if (i != pageCount - 1) Spacer(Modifier.width(10.dp))
        }
    }
}
