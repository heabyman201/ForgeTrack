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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

data class SurveyQuestion(
    val id: String,
    val text: String,
    val options: List<String>
)

val FULL_QUESTION_POOL = listOf(
    SurveyQuestion("goal", "What is your primary fitness goal?", listOf("Build Muscle (Hypertrophy)", "Lose Fat (Cutting)", "Increase Strength", "General Health", "Athletic Performance")),
    SurveyQuestion("exp", "How long have you been training?", listOf("Just starting", "Less than 6 months", "6 months - 2 years", "2+ years", "5+ years")),
    SurveyQuestion("days", "How many days can you train per week?", listOf("1-2 days", "3 days", "4 days", "5 days", "6+ days")),
    SurveyQuestion("equip", "What equipment do you have access to?", listOf("Full Commercial Gym", "Home Gym (Barbell/Dumbbell)", "Dumbbells Only", "Bodyweight Only", "Kettlebells")),
    SurveyQuestion("session_len", "Preferred workout duration?", listOf("Under 30 mins", "45-60 mins", "60-90 mins", "90+ mins")),
    SurveyQuestion("limitations", "Do you have any injuries?", listOf("No injuries", "Shoulder issues", "Knee issues", "Lower back pain", "Wrist mobility issues")),
    SurveyQuestion("focus_area", "Which muscle group needs the most focus?", listOf("Balanced / Full Body", "Chest & Push Muscles", "Back & Pull Muscles", "Legs & Glutes", "Arms (Biceps/Triceps)")),
    SurveyQuestion("recovery_rate", "How is your recovery between sets?", listOf("Fast (<1 min rest needed)", "Average (1-2 mins rest)", "Slow (Need 3+ mins)", "I don't track this")),
    SurveyQuestion("stimulants", "Do you use pre-workout/stimulants?", listOf("High Tolerance (Heavy Stim)", "Low Tolerance (Light Stim)", "Coffee / Natural only", "Stimulant Free")),
    SurveyQuestion("sleep_quality", "How many hours do you sleep on average?", listOf("Less than 5 hours", "5-6 hours", "7-8 hours", "9+ hours")),
    SurveyQuestion("diet_style", "What is your current diet style?", listOf("No restrictions", "High Protein", "Keto / Low Carb", "Vegetarian / Vegan", "Intermittent Fasting")),
    SurveyQuestion("water_intake", "Daily water intake?", listOf("Less than 1 Liter", "1-2 Liters", "3-4 Liters", "4+ Liters")),
    SurveyQuestion("cardio_pref", "How do you prefer to do cardio?", listOf("No Cardio", "HIIT (Sprints/Circuits)", "LISS (Walking/Cycling)", "Sports / Activities")),
    SurveyQuestion("training_time", "When do you prefer to train?", listOf("Early Morning", "Mid-Day / Lunch", "Evening", "Late Night")),
    SurveyQuestion("motivation", "What motivates you most?", listOf("Visual changes", "Strength numbers", "Mental health", "Competition / Sports")),
    SurveyQuestion("tracking_style", "How do you track progress?", listOf("App / Digital Log", "Pen & Paper", "Mental Notes", "I don't track")),
    SurveyQuestion("supplements", "Do you take Creatine?", listOf("Yes, consistently", "Sometimes / Irregularly", "No, never used it", "Planning to start")),
    SurveyQuestion("stress_levels", "How would you rate your daily stress?", listOf("Low / Relaxed", "Moderate", "High", "Very High")),
    SurveyQuestion("grip_strength", "Do you use lifting straps?", listOf("Never (Raw grip)", "Only on heavy pulls", "On most back exercises", "Yes, grip is a weak point")),
    SurveyQuestion("mobility", "Do you have a stretching routine?", listOf("Daily routine", "Before/After workout only", "Rarely", "Never"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSurveyScreen(
    navController: NavController
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { SurveyTape.init(context) }

    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val scope = rememberCoroutineScope()

    val selectedQuestions = remember {
        FULL_QUESTION_POOL.shuffled().take(5)
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentQuestion = remember(currentIndex) { selectedQuestions.getOrNull(currentIndex) }

    fun handleNext() {
        if (currentIndex < selectedQuestions.size - 1) {
            currentIndex++
        } else {
            navController.navigateUp()
        }
    }

    fun submitAnswer(answer: String) {
        scope.launch {
            currentQuestion?.let { q ->
                SurveyTape.logAnswer(q.id, q.text, answer)
            }
            handleNext()
        }
    }

    fun skipQuestion() {
        scope.launch {
            currentQuestion?.let { q ->
                SurveyTape.logAnswer(q.id, q.text, "SKIPPED", skipped = true)
            }
            handleNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = listOf(theme.primary.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 3),
                    radius = size.width
                )
                onDrawBehind { drawRect(brush) }
            }
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Preferences", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Step ${currentIndex + 1} of ${selectedQuestions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(onClick = { skipQuestion() }) {
                            Text("Skip", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1) / selectedQuestions.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = theme.primary,
                    trackColor = theme.secondary.copy(alpha = 0.2f),
                )

                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally { -it } + fadeOut(tween(300))
                    },
                    label = "SurveyTransition"
                ) { question ->
                    if (question != null) {
                        SurveyCard(
                            question = question,
                            primaryColor = theme.primary,
                            secondaryColor = theme.secondary,
                            backgroundColor = theme.background,
                            onOptionSelected = { submitAnswer(it) }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = theme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyCard(
    question: SurveyQuestion,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    onOptionSelected: (String) -> Unit
) {
    val cornerRadius = 24.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.15f),
                        backgroundColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.4f),
                        secondaryColor.copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEach { option ->
                SurveyOptionRow(
                    text = option,
                    highlightColor = primaryColor
                ) {
                    onOptionSelected(option)
                }
            }
        }
    }
}

@Composable
private fun SurveyOptionRow(
    text: String,
    highlightColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = highlightColor.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview
@Composable
fun RecommendationSurveyScreenPreview() {
    val navController = androidx.navigation.compose.rememberNavController()
    RecommendationSurveyScreen(navController = navController)
}