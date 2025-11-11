package com.forgecompose.workouttracker

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditUserStats(navcontroller: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager(context) }

    var userAge by remember { mutableStateOf(prefsManager.getAge()) }
    var userHeight by remember { mutableStateOf(prefsManager.getHeight()) }
    var userWeight by remember { mutableStateOf(prefsManager.getWeight()) }
    var userName by remember { mutableStateOf(prefsManager.getName()) }
    var getExp by remember { mutableStateOf(prefsManager.getExperience()) }
    var preferredStyle by remember { mutableStateOf(prefsManager.getPreferredStyle()) }
    var importantMuscles by remember { mutableStateOf(prefsManager.getImportantMuscles()) }

    val styles = listOf("Cardio", "Weights", "Both")
    val muscles = listOf("Chest", "Arms", "Legs", "Back", "Core", "Shoulders")

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = tween(140, easing = FastOutSlowInEasing))
    val elevate by animateDpAsState(targetValue = if (pressed) 2.dp else 10.dp, animationSpec = tween(200, easing = FastOutSlowInEasing))
    val bgPulse = rememberInfiniteTransition(label = "pulse")
    val pulse by bgPulse.animateFloat(0.0f, 1.0f, animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    val glow by animateFloatAsState(if (pressed) 0.65f else 1f, tween(240, easing = FastOutSlowInEasing))
    val performanceOptions by PerformanceOptionsManager.flow(context)
        .collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(colors = introColors)
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0708),
                        Color(0xFF11090A),
                        Color(0xFF160B0C)
                    )
                )
            )

    ) {
        AnimatedBackdrop(
            modifier = Modifier
                .matchParentSize(),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves =  movingEffectsEnabled,
            enableAnimation =  movingEffectsEnabled
        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Edit Profile",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navcontroller.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            UserPreferencesManager(context).saveUserData(
                                userName, userAge, userHeight, userWeight, getExp, preferredStyle, importantMuscles
                            )
                            navcontroller.navigate("UserProfile")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .border(
                                2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8F0A00).copy(alpha = 0.85f),
                                        Color(0xFF56241E).copy(alpha = 0.7f),
                                        Color(0xFF620000).copy(alpha = 0.9f)
                                    )
                                ),
                                CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A0E10).copy(alpha = 0.65f + 0.2f * pulse),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevate, pressedElevation = elevate),
                        interactionSource = interaction
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.titleMedium.copy(
                                shadow = Shadow(color = Color.White.copy(alpha = 0.4f * glow), blurRadius = 12f)
                            ),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        clip = true
                        shape = RoundedCornerShape(24.dp)
                    }
                    .background(Color(0x1A1C0E10))
                    .border(1.dp, Color(0x33F04E3E), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Name") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331F1011),
                        unfocusedContainerColor = Color(0x22161011),
                        focusedBorderColor = Color(0xFFBE0000),
                        unfocusedBorderColor = Color(0x44FF6A4E),
                        focusedLabelColor = Color(0xFFB91400),
                        cursorColor = Color(0xFFFF6161),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = userAge,
                    onValueChange = { userAge = it },
                    label = { Text("Age") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331F1011),
                        unfocusedContainerColor = Color(0x22161011),
                        focusedBorderColor = Color(0xFFFF6A4E),
                        unfocusedBorderColor = Color(0x44FF6A4E),
                        focusedLabelColor = Color(0xFFFF6A4E),
                        cursorColor = Color(0xFFFF6A4E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = userHeight,
                    onValueChange = { userHeight = it },
                    label = { Text("Height") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331F1011),
                        unfocusedContainerColor = Color(0x22161011),
                        focusedBorderColor = Color(0xFFFF6A4E),
                        unfocusedBorderColor = Color(0x44FF6A4E),
                        focusedLabelColor = Color(0xFFFF6A4E),
                        cursorColor = Color(0xFFFF6A4E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = userWeight,
                    onValueChange = { userWeight = it },
                    label = { Text("Weight") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331F1011),
                        unfocusedContainerColor = Color(0x22161011),
                        focusedBorderColor = Color(0xFFFF6A4E),
                        unfocusedBorderColor = Color(0x44FF6A4E),
                        focusedLabelColor = Color(0xFFFF6A4E),
                        cursorColor = Color(0xFFFF6A4E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = getExp,
                    onValueChange = { getExp = it },
                    label = { Text("Experience") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x331F1011),
                        unfocusedContainerColor = Color(0x22161011),
                        focusedBorderColor = Color(0xFFFF6A4E),
                        unfocusedBorderColor = Color(0x44FF6A4E),
                        focusedLabelColor = Color(0xFFFF6A4E),
                        cursorColor = Color(0xFFFF6A4E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preferred Workout Style", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    styles.forEach { style ->
                        val selected = preferredStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { preferredStyle = style },
                            label = {
                                Text(
                                    style,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x33FF6A4E),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x22FFFFFF),
                                labelColor = Color(0xEEFFFFFF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) Color(0xFFFF0000) else Color(0x44FFFFFF),
                                selectedBorderColor = Color(0xFF940000),
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Most Important Muscles", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    muscles.forEach { muscle ->
                        val selected = muscle in importantMuscles
                        FilterChip(
                            selected = selected,
                            onClick = {
                                importantMuscles = if (selected) importantMuscles - muscle else importantMuscles + muscle
                            },
                            label = {
                                Text(
                                    muscle,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x33FF6A4E),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x22FFFFFF),
                                labelColor = Color(0xEEFFFFFF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) Color(0xFFFF0000) else Color(0x44FFFFFF),
                                selectedBorderColor = Color(0xFF960000),
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}








