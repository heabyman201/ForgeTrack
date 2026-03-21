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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlagCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val AI_ADVICE_VISIBLE_MS = 8_000L
private const val SCIENCE_FACT_VISIBLE_MS = 9_000L

private val MUSCLE_GROWTH_FACTS = listOf(
    "Muscle growth starts with a training signal, but it depends on recovery to actually build tissue.",
    "Mechanical tension is one of the strongest drivers of muscle hypertrophy.",
    "You do not need extreme soreness for a workout to be effective.",
    "Progressive overload can come from more weight, more reps, more sets, or better technique.",
    "Training through a full range of motion often helps build strength and muscle across the movement.",
    "Muscle protein synthesis rises after resistance training, especially when recovery and protein intake support it.",
    "Aiming for enough protein each day matters more than timing every single meal perfectly.",
    "For many lifters, about 1.6 to 2.2 grams of protein per kilogram of body weight per day is a useful target range.",
    "Sleep is a real recovery tool. Poor sleep can hurt performance and adaptation.",
    "You do not need to train every set to failure to grow muscle.",
    "Compound lifts and isolation work both have a place in building a balanced physique.",
    "Beginners often gain muscle faster at first because their bodies adapt quickly to a new stimulus.",
    "Muscle grows during recovery, not during the lifting itself.",
    "Carbohydrates help fuel harder training sessions by supporting glycogen stores.",
    "Creatine monohydrate is one of the most studied supplements for strength and lean mass support.",
    "A calorie surplus can make muscle gain easier, but trained athletes can still add muscle in more controlled intakes.",
    "Tendons and connective tissues adapt too, but often more slowly than muscle tissue.",
    "Training a muscle more than once per week can be effective when total volume and recovery are managed well.",
    "Eccentric control, the lowering phase, can add useful training stimulus.",
    "Consistency over months matters more than chasing the perfect single workout.",
    "Early strength gains often come from nervous system adaptations, not just bigger muscles.",
    "Muscle does not turn into fat. They are different tissues with different jobs.",
    "Longer rest periods can help preserve performance on heavy compound sets.",
    "You can build muscle without supplements if training, food, and sleep are on point.",
    "Spot reduction is not a real fat-loss strategy. The body loses fat systemically.",
    "Adequate hydration supports strength, endurance, and recovery.",
    "Complete protein sources provide all essential amino acids needed for muscle repair.",
    "Progress can still happen in a calorie deficit, especially for beginners or returning lifters.",
    "Training volume is often a stronger predictor of hypertrophy than load alone.",
    "Warm-ups can improve performance by raising temperature and preparing the nervous system.",
    "Too much training without enough recovery can stall progress or raise injury risk.",
    "Muscle growth is usually slow enough that weekly changes can be hard to notice visually.",
    "Both men and women can build muscle effectively with the same core training principles.",
    "Older adults can still gain muscle and strength from resistance training."
)

private fun randomMuscleGrowthFact(): String = MUSCLE_GROWTH_FACTS.random()

@Composable
fun GlowingTextSkeleton(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton_fluid")
    
    val shimmerTranslate by transition.animateFloat(
        initialValue = -800f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val alphaPulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    val fluidBrush = Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = alphaPulse * 0.3f),
            color.copy(alpha = alphaPulse * 0.7f),
            color.copy(alpha = 1f),
            color.copy(alpha = alphaPulse * 0.7f),
            color.copy(alpha = alphaPulse * 0.3f)
        ),
        start = Offset(shimmerTranslate - 400f, shimmerTranslate - 400f),
        end = Offset(shimmerTranslate + 400f, shimmerTranslate + 400f)
    )

    val lines = listOf(
        listOf(0.4f, 0.3f, 0.2f),
        listOf(0.2f, 0.5f, 0.2f),
        listOf(0.3f, 0.4f)
    )
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { rowWidths ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowWidths.forEach { weight ->
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .weight(weight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(fluidBrush)
                            .glow(color, radius = 8.dp, alpha = alphaPulse * 0.5f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f - rowWidths.sum()))
            }
        }
    }
}

@Composable
fun AdviceSectionUser(
    advice: String,
    isAdviceLoading: Boolean,
    lastWorkoutName: String,
    extraLines: List<String>,
    modifier: Modifier = Modifier,
    maxExtraLines: Int = 4,
    displayDurationMs: Long = AI_ADVICE_VISIBLE_MS
) {
    val context = LocalContext.current
    val aiEnabled = dynamicModel.personaConfig.value.enabled
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val accent = appearanceOptions.selectedTheme.colors.primary
    val linesToShow = remember(extraLines, maxExtraLines) {
        extraLines.filter { it.isNotBlank() }.take(maxExtraLines)
    }
    val adviceText = advice.ifBlank { "Log a workout to unlock short AI performance advice." }
    var showWorkoutStats by remember(adviceText, lastWorkoutName, aiEnabled, isAdviceLoading) {
        mutableStateOf(false)
    }
    var scienceFact by remember(adviceText, lastWorkoutName, aiEnabled) {
        mutableStateOf(randomMuscleGrowthFact())
    }

    LaunchedEffect(adviceText, lastWorkoutName, linesToShow, aiEnabled, isAdviceLoading, displayDurationMs) {
        showWorkoutStats = false
        if (aiEnabled) {
            if (isAdviceLoading) return@LaunchedEffect
            delay(displayDurationMs)
        } else {
            scienceFact = randomMuscleGrowthFact()
            delay(SCIENCE_FACT_VISIBLE_MS)
        }
        showWorkoutStats = true
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = appearanceOptions.selectedTheme.colors.background.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedContent(
                targetState = showWorkoutStats,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "homeAdviceSwap"
            ) { showingWorkoutStats ->
                if (!showingWorkoutStats) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (aiEnabled) {
                                    if (lastWorkoutName.isBlank() || lastWorkoutName == "No workouts yet.") {
                                        "AI performance advice"
                                    } else {
                                        "AI advice for $lastWorkoutName"
                                    }
                                } else "Muscle growth fact",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            androidx.compose.animation.Crossfade(
                                targetState = aiEnabled && isAdviceLoading,
                                label = "adviceLoadingCrossfade",
                                animationSpec = tween(600, easing = LinearEasing)
                            ) { loading ->
                                if (loading) {
                                    Column {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        GlowingTextSkeleton(color = accent, modifier = Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Text(
                                        text = if (aiEnabled) adviceText else scienceFact,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.96f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlagCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Latest workout",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Text(
                                text = lastWorkoutName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                            if (linesToShow.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = accent.copy(alpha = 0.24f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    linesToShow.forEach { line ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = null,
                                                tint = accent.copy(alpha = 0.75f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
