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

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditUserStats(navcontroller: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager(context) }
    val auth = remember { Firebase.auth }

    // --- User State ---
    var userAge by remember { mutableStateOf(prefsManager.getAge()) }
    var userHeight by remember { mutableStateOf(prefsManager.getHeight()) }
    var userWeight by remember { mutableStateOf(prefsManager.getWeight()) }
    var userName by remember { mutableStateOf(prefsManager.getName()) }
    var getExp by remember { mutableStateOf(prefsManager.getExperience()) }
    var preferredStyle by remember { mutableStateOf(prefsManager.getPreferredStyle()) }
    var importantMuscles by remember { mutableStateOf(prefsManager.getImportantMuscles()) }

    // --- UI/Animation State ---
    val styles = listOf("Calisthenics", "Weights", "Both")
    val muscles = listOf("Chest", "Arms", "Legs", "Back", "Core", "Shoulders")

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = tween(140, easing = FastOutSlowInEasing))
    val elevate by animateDpAsState(targetValue = if (pressed) 2.dp else 10.dp, animationSpec = tween(200, easing = FastOutSlowInEasing))
    val bgPulse = rememberInfiniteTransition(label = "pulse")
    val pulse by bgPulse.animateFloat(0.0f, 1.0f, animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    val glow by animateFloatAsState(if (pressed) 0.65f else 1f, tween(240, easing = FastOutSlowInEasing))
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
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

    LaunchedEffect(Unit) {
        showIntro = false;
        Firebase.crashlytics.setCustomKey("current_screen", "Edit user stats screen")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                       theme.background,
                        theme.background,
                        theme.secondary
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
                            val db = FirebaseFirestore.getInstance()


                            val usersRef = db.collection("users")

                            val data = mapOf(
                                "userName" to userName,
                                "uid" to (auth.currentUser?.uid ?: "anonymous"),
                                "email" to (auth.currentUser?.email ?: ""),

                                "age" to userAge,
                                "height" to userHeight,
                                "weight" to userWeight,
                                "experience" to getExp,
                                "preferredStyle" to preferredStyle,
                                "importantMuscles" to importantMuscles
                            )

                            usersRef.document(userName)
                                .set(data)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "User stats saved successfully!")
                                    Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Failed to save stats", e)
                                    Toast.makeText(context, "Failed to sync to cloud", Toast.LENGTH_SHORT).show()
                                }

                            navcontroller.navigate("UserProfile")
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
                                        theme.secondary,
                                        theme.tertiary,
                                        theme.background
                                    )
                                ),
                                CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.background.copy(alpha = 0.65f + 0.2f * pulse),
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
                UserField(
                    value = userName,
                    onValueChange = {
                        userName = it
                    },
                    label = "Name"

                )
                UserField(
                    value = userAge,
                    onValueChange = {
                        userAge = it
                    },
                    label = "Age"

                )
                UserField(
                    value = userHeight,
                    onValueChange = {
                        userHeight = it
                    },
                    label = "Height"

                )
                UserField(
                    value = userWeight,
                    onValueChange = {
                        userWeight = it
                    },
                    label = "Weight"

                )
                UserField(
                    value = getExp,
                    onValueChange = {
                        getExp = it
                    },
                    label = "Experience"

                )
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
                                selectedContainerColor = theme.secondary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x22FFFFFF),
                                labelColor = Color(0xEEFFFFFF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) theme.primary else Color(0x44FFFFFF),
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
                                selectedContainerColor =   theme.primary,
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

@Composable
fun UserField(value: String, onValueChange: (String) -> Unit, label: String){
val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        label = { Text(label) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x331F1011),
            unfocusedContainerColor = Color(0x22161011),
            focusedBorderColor = theme.primary,
            unfocusedBorderColor = theme.primary.copy(alpha = 0.25f),
            focusedLabelColor = theme.primary,
            cursorColor =theme.primary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(Modifier.height(14.dp))
}
