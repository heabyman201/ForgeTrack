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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlagCircle
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val AI_ADVICE_VISIBLE_MS = 6_000L
private const val SCIENCE_FACT_VISIBLE_MS = 5_500L

private val MUSCLE_GROWTH_FACTS = listOf(
    "Muscles are 75% water. Hydration is key to keeping them 'pumped' and firing.","The 'pump' is blood rushing to your muscles to deliver oxygen and clear waste.",
    "The smallest muscle is the stapedius in your ear—it's less than 2mm long!",
    "Muscle burns 3x more calories than fat, even while you are resting.",
    "The gluteus maximus is your largest muscle and keeps you standing upright.",
    "Muscles only pull; they never push. They work in pairs to move your limbs.",
    "The 'burn' comes from hydrogen ions dropping your muscle pH, not just lactic acid.",
    "DOMS usually peaks 24–48 hours after training as your fibers rebuild.",
    "Your heart never gets tired—it beats about 100,000 times a day without rest.",
    "It takes 17 muscles to smile and 43 to frown. Lifting is easier than being grumpy!",
    "Skeletal muscle is your largest organ, making up 40% of your total body mass.",
    "Muscle 'memory' makes it much easier to regain mass after taking a break.",
    "Your tongue is the only muscle in your body attached at only one end.",
    "Muscle is denser than fat. You can look leaner even if the scale doesn't move.",
    "The jaw muscle (masseter) is the strongest in the body based on its weight.",
    "Growth hormone peaks during deep REM sleep—the ultimate recovery state.",
    "The lowering phase (eccentric) causes more micro-tears and growth than the lift.",
    "Your muscles generate enough daily heat to boil nearly 2 liters of water.",
    "Your brain limits strength to protect tendons. You are stronger than you think!",
    "Nitric oxide, found in beets, relaxes vessels to improve blood flow and 'the pump.'",
    "Creatine supports muscle power and improves mental processing speed.",
    "Grip strength is one of the best biological markers for overall longevity.",
    "Lifting heavy things strengthens your bones by increasing mineral density.",
    "The sartorius is your longest muscle, running from your hip to your knee.",
    "Your body recycles its own weight in ATP energy every single day.",
    "Tiny muscles called arrector pili give you 'goosebumps' when you're cold.",
    "Muscle protein synthesis stays elevated for up to 48 hours after heavy lifting.",
    "Increasing your muscle mass helps your body manage blood sugar levels better.",
    "Tendons act like springs, storing energy to make movements more efficient.",
    "You have over 600 muscles working together to help you move and breathe.",
    "The eye muscles are your most active, moving over 100,000 times a day.",
    "Actin and myosin are the 'molecular motors' that make your muscles contract.",
    "Stretching can trigger muscle-building signals via mechanical tension.",
    "Caffeine reduces the perception of effort, helping you finish those last reps."
)

private fun randomMuscleGrowthFact(): String = MUSCLE_GROWTH_FACTS.random()

private val adviceCardIntroStartOffsets = listOf(
    Offset(-540f, -180f),
    Offset(-460f, 160f),
    Offset(220f, -260f),
    Offset(420f, 220f),
    Offset(-120f, -340f),
    Offset(520f, -80f)
)

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
    val accent = appearanceOptions.colors.primary
    val secondaryAccent = appearanceOptions.colors.secondary
    val surfaceColor = appearanceOptions.colors.background.copy(alpha = 0.85f)
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
    val introStartOffset = remember(adviceText, lastWorkoutName, linesToShow, aiEnabled) {
        adviceCardIntroStartOffsets.random(Random.Default)
    }
    val introEndOffset = Offset(320f, 420f)
    var playIntroGradient by remember(adviceText, lastWorkoutName, linesToShow, aiEnabled) { mutableStateOf(false) }
    val introProgress by animateFloatAsState(
        targetValue = if (playIntroGradient) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "adviceCardIntroGradient"
    )

    LaunchedEffect(adviceText, lastWorkoutName, linesToShow, aiEnabled) {
        playIntroGradient = false
        playIntroGradient = true
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
        color = Color.Transparent
    ) {
        val settledStartOffset = Offset(0f, 0f)
        val settledEndOffset = Offset(720f, 0f)
        val introBrush = Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.12f + ((1f - introProgress) * 0.12f)),
                secondaryAccent.copy(alpha = 0.08f + ((1f - introProgress) * 0.08f)),
                surfaceColor.copy(alpha = 0.92f)
            ),
            start = Offset(
                x = androidx.compose.ui.util.lerp(introStartOffset.x, settledStartOffset.x, introProgress),
                y = androidx.compose.ui.util.lerp(introStartOffset.y, settledStartOffset.y, introProgress)
            ),
            end = Offset(
                x = androidx.compose.ui.util.lerp(introEndOffset.x, settledEndOffset.x, introProgress),
                y = androidx.compose.ui.util.lerp(introEndOffset.y, settledEndOffset.y, introProgress)
            )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(introBrush)
                // The latest-workout view lives in a fixed-height home card. Tighten
                // its vertical padding so the complete stat grid stays inside it.
                .padding(
                    horizontal = 16.dp,
                    vertical = if (showWorkoutStats) 10.dp else 14.dp
                ),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (linesToShow.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Two compact columns keep every workout detail in the
                                    // card, while allowing Sets/Reps to wrap rather than get
                                    // truncated with an ellipsis.
                                    linesToShow.chunked(2).forEach { statRow ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            statRow.forEach { line ->
                                                val separatorIndex = line.indexOf(':')
                                                val statText = buildAnnotatedString {
                                                    if (separatorIndex in 1 until line.lastIndex) {
                                                        append(line.substring(0, separatorIndex + 1))
                                                        withStyle(
                                                            SpanStyle(
                                                                color = Color.White.copy(alpha = 0.96f),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        ) {
                                                            append(" ")
                                                            append(line.substring(separatorIndex + 1).trim())
                                                        }
                                                    } else {
                                                        withStyle(
                                                            SpanStyle(
                                                                color = Color.White.copy(alpha = 0.96f),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        ) {
                                                            append(line)
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = statText,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = Color.White.copy(alpha = 0.74f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (statRow.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
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
}
