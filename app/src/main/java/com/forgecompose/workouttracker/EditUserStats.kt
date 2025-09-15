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

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120, easing = FastOutSlowInEasing))
    val pulse = rememberInfiniteTransition(label = "pulse")
    val intensePulse by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "intense")
    val glowIntensity by animateFloatAsState(if (pressed) 0.6f else 1f, tween(250, easing = FastOutSlowInEasing))
    val elevation by animateDpAsState(if (pressed) 4.dp else 10.dp, tween(200, easing = FastOutSlowInEasing))

    val styles = listOf("Cardio", "Weights", "Both")
    val muscles = listOf("Chest", "Arms", "Legs", "Back", "Core", "Shoulders")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0606))
            .drawBehind {
                val r = size.minDimension * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4A0000), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = r
                    ),
                    center = Offset(0f, 0f),
                    radius = r
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navcontroller.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
                            .height(60.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .border(
                                2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8B0000).copy(alpha = 0.8f + intensePulse * 0.2f),
                                        Color(0xFFFF8800).copy(alpha = 0.6f + glowIntensity * 0.3f),
                                        Color(0xFF650000).copy(alpha = 0.7f)
                                    )
                                ),
                                CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A1515).copy(alpha = 0.4f + intensePulse * 0.2f),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation, pressedElevation = elevation),
                        interactionSource = interactionSource
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.titleLarge.copy(
                                shadow = Shadow(color = Color.White.copy(alpha = glowIntensity * 0.5f), blurRadius = 10f)
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Name") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = userAge,
                    onValueChange = { userAge = it },
                    label = { Text("Age") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = userHeight,
                    onValueChange = { userHeight = it },
                    label = { Text("Height") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = userWeight,
                    onValueChange = { userWeight = it },
                    label = { Text("Weight") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = getExp,
                    onValueChange = { getExp = it },
                    label = { Text("Experience") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Text("Preferred Workout Style", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    styles.forEach { style ->
                        FilterChip(
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4A1515),
                                selectedLabelColor = Color.White
                            ),
                            selected = preferredStyle == style,
                            onClick = { preferredStyle = style },
                            label = { Text(style) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Most Important Muscles", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    muscles.forEach { muscle ->
                        val selected = muscle in importantMuscles
                        FilterChip(
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4A1515),
                                selectedLabelColor = Color.White
                            ),
                            selected = selected,
                            onClick = {
                                importantMuscles = if (selected) importantMuscles - muscle else importantMuscles + muscle
                            },
                            label = { Text(muscle) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}







