package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import java.time.Duration


data class PersonalRecord(val exerciseName: String, val maxWeight: Double)

class SurveyPromptManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_TAKEN = "last_survey_timestamp"
    private val COOLDOWN_MS = Duration.ofDays(1).toMillis()

    fun shouldShowPrompt(): Boolean {
        val lastTaken = prefs.getLong(KEY_LAST_TAKEN, 0L)
        val now = System.currentTimeMillis()
        return (now - lastTaken) > COOLDOWN_MS || lastTaken == 0L
    }

    fun markSurveyTaken() {
        prefs.edit().putLong(KEY_LAST_TAKEN, System.currentTimeMillis()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: WorkoutListViewModel,
    badgeViewModel: BadgeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager(context) }
    val surveyManager = remember { SurveyPromptManager(context) }

    // Theme Hooks
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val userAge = remember { prefsManager.getAge() }
    val userHeight = remember { prefsManager.getHeight() }
    val userWeight = remember { prefsManager.getWeight() }
    val userName = remember { prefsManager.getName() }
    val stages = rememberColdStartStages()
    val badges by badgeViewModel.badges.collectAsState()

    var showSurveyPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showSurveyPrompt = surveyManager.shouldShowPrompt()
    }

    val (personalRecords, recentWorkouts) = remember(uiState) {
        if (uiState is WorkoutListUiState.Success) {
            val workouts = (uiState as WorkoutListUiState.Success).workouts
            val prs = workouts
                .asSequence()
                .filter { (it.weight ?: 0.0) > 0.0 }
                .groupBy { it.name }
                .map { (name, workoutList) -> PersonalRecord(name, workoutList.maxOf { it.weight!! }) }
                .sortedByDescending { it.maxWeight }
                .take(4)
            val recent = workouts.take(4)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
    }

    // Intro logic...
    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) { Brush.linearGradient(colors = introColors) }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")

    LaunchedEffect(Unit) {
        showIntro = false
        Firebase.crashlytics.setCustomKey("current_screen", "Profile Screen")
    }

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val shouldAnimate = stages.afterFirstFrame
    var animationClock by remember { mutableStateOf(0f) }

    LaunchedEffect(shouldAnimate, movingEffectsEnabled) {
        if (shouldAnimate && movingEffectsEnabled) {
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
    }

    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )
    val blurToApply = remember(blurAnim) { if (blurAnim < 0.6.dp) 0.dp else blurAnim.coerceAtMost(60.dp) }

    val nameStyle = MaterialTheme.typography.headlineMedium.copy(
        shadow = Shadow(color = Color.White.copy(alpha = 0.3f), blurRadius = 8f)
    )
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurToApply > 0.dp) {
                    Modifier.blur(blurToApply)
                } else Modifier
            )
    ) {
        AnimatedBackdrop(
            modifier = Modifier.matchParentSize(),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves = movingEffectsEnabled,
            enableAnimation = movingEffectsEnabled,
        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold) },
//                    navigationIcon = {
//                        IconButton(onClick = { navController.navigateUp() }) {
//                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
//                        }
//                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("Settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pass theme colors to Avatar
                        GlowingAvatar(
                            icon = Icons.Default.Person,
                            primaryColor = theme.primary,
                            secondaryColor = theme.secondary
                        )
                        Text(
                            userName,
                            style = nameStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (showSurveyPrompt && stages.after600ms) {
                    item {
                        SurveyPromptCard(
                            onClick = {
                                surveyManager.markSurveyTaken()
                                showSurveyPrompt = false
                                navController.navigate("Survey")
                            }
                        )
                    }
                }

                item {
                    ProfileSectionCard(
                        title = "Badges",
                        themeColors = theme, // Pass theme
                        action = {
                            CardActionButton("View All") { navController.navigate("badges") }
                        }
                    ) {
                        BadgeSection(
                            badges = badges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                if (stages.after600ms && personalRecords.isNotEmpty()) {
                    item {
                        ProfileSectionCard(
                            title = "Personal Records",
                            themeColors = theme, // Pass theme
                            action = {
                                CardActionButton("More") { navController.navigate("RepMax") }
                            }
                        ) {
                            personalRecords.forEach { pr ->
                                ProfileStatRow(
                                    label = pr.exerciseName,
                                    value = "${String.format("%.1f", pr.maxWeight)} kg",
                                    icon = Icons.Default.Stars,
                                    iconTint = Color(0xFFfce18a), // Gold for Stars logic usually stays
                                    textColor = Color.White
                                )
                            }
                        }
                    }
                }

                if (stages.after200ms && recentWorkouts.isNotEmpty()) {
                    item {
                        ProfileSectionCard(
                            title = "Recent Activity",
                            themeColors = theme, // Pass theme
                            action = {
                                CardActionButton("View History") { navController.navigate("WorkoutHistory") }
                            }
                        ) {
                            recentWorkouts.forEach { workout ->
                                val date = remember(workout.date) { dateFormatter.format(Date(workout.date)) }
                                // Dynamic tint for History icon
                                ProfileStatRow(
                                    label = workout.name,
                                    value = date,
                                    icon = Icons.Default.History,
                                    iconTint = theme.primary
                                )
                            }
                        }
                    }
                }

                if (stages.after200ms) {
                    item {
                        ProfileSectionCard(
                            title = "Body Stats",
                            themeColors = theme, // Pass theme
                            action = {
                                CardActionButton("Edit Profile") { navController.navigate("EditUserStats") }
                            }
                        ) {
                            ProfileStatRow("Age", userAge, Icons.Default.Person, iconTint = theme.primary)
                            ProfileStatRow("Height", "$userHeight cm", Icons.Default.Height, iconTint = theme.primary)
                            ProfileStatRow("Weight", "$userWeight kg", Icons.Default.MonitorWeight, iconTint = theme.primary)
                            ProfileStatRow("Experience", prefsManager.getExperience(), Icons.Default.DataExploration, iconTint = theme.primary)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        if (stages.after100ms) {
            FloatingTaskbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navController = navController,
                cornerRadius = 32.dp,
                iconAlpha = 1f,
                uiState = uiState
            )
        }
    }
}

// ... [SurveyPromptCard remains the same - Gold is usually a distinct CTA color] ...
@Composable
fun SurveyPromptCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .clip(RoundedCornerShape(24.dp))
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = listOf(
                      appearanceOptions.colors.primary.copy(alpha = 0.15f),
                        appearanceOptions.colors.primary.copy(alpha = 0.05f)
                    )
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        appearanceOptions.colors.primary.copy(alpha = glowAlpha),
                        appearanceOptions.colors.primary.copy(alpha = glowAlpha * 0.7f)
                    )
                )

                onDrawBehind {
                    drawRoundRect(brush, cornerRadius = CornerRadius(24.dp.toPx()))
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(appearanceOptions.colors.background.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint =     appearanceOptions.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Update Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Refresh your recovery goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = appearanceOptions.colors.primary
            )
        }
    }
}


@Composable
private fun GlowingAvatar(
    icon: ImageVector,
    primaryColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .drawWithCache {
                // Use Secondary for deep background, mix with black
                val bgBrush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.6f), Color(0xFF050505)),
                    radius = size.minDimension / 2f * 1.5f
                )
                // Border uses Primary
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        secondaryColor.copy(alpha = 0.3f)
                    )
                )
                onDrawBehind {
                    drawCircle(bgBrush)
                    drawCircle(borderBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "User Avatar",
            tint = primaryColor, // Use Primary for the Icon
            modifier = Modifier
                .size(60.dp)
                .drawWithCache {
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                        radius = size.minDimension
                    )
                    onDrawBehind {
                        drawCircle(glowBrush)
                    }
                }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    themeColors: ColorSchemeAppTheme,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                // Dynamic Background based on theme
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.secondary.copy(alpha = 0.6f),
                        themeColors.background.copy(alpha = 0.8f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                // Dynamic Border based on theme
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.primary.copy(alpha = 0.3f),
                        themeColors.secondary.copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            action?.invoke()
        }

        // Dynamic Divider
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = themeColors.primary.copy(alpha = 0.3f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun CardActionButton(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    TextButton(
        onClick = {
            onClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileStatRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFFFF3B30), // Default value if not provided
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}