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
private const val SCIENCE_FACT_VISIBLE_MS = 7_000L

private val MUSCLE_GROWTH_FACTS = listOf(
    "Your muscles are about 75% water. Hydration isn't just for health—it's what keeps your muscles 'pumped' and firing!",
    "The 'pump' you feel is 'transient hypertrophy'—blood rushing to your muscles to deliver oxygen and clear out waste.",
    "The smallest muscle in your body is the stapedius, located in your middle ear. It's less than 2mm long!",
    "Muscle is three times more efficient at burning calories than fat, even while you're just sitting on the couch.",
    "Your gluteus maximus is the largest muscle in your body. It’s the powerhouse responsible for keeping you upright.",
    "Muscles can only pull, they never push. To move your arm back and forth, pairs of muscles take turns pulling.",
    "The 'burn' you feel during a high-rep set comes from hydrogen ions dropping the pH in your muscle, not just lactic acid!",
    "DOMS (Delayed Onset Muscle Soreness) usually peaks 24–48 hours after training. It’s the sound of your muscle fibers rebuilding.",
    "Your heart is the only muscle that never gets tired—it beats about 100,000 times a day without a single rest day.",
    "It takes about 17 muscles to smile but 43 to frown. Lifting weights is easier than being grumpy!",
    "Skeletal muscle is the largest organ in the human body, making up about 40% of your total body mass.",
    "Muscles have 'memory.' Nuclei gained during training stay in your fibers, making it easier to regain muscle after a break.",
    "Your tongue is the only muscle in your body that is attached at only one end.",
    "Mass for mass, muscle is denser than fat. That’s why the scale might not move even as your clothes fit better.",
    "The masseter (your jaw muscle) is the strongest muscle in the body based on its weight—it can close teeth with 200lbs of force.",
    "Sleep is the ultimate 'anabolic' state. Your body releases the most growth hormone while you're in deep REM sleep.",
    "Eccentric training (the lowering phase) causes more micro-tears than the lifting phase, leading to more growth.",
    "Your muscles generate enough heat every day to boil nearly 2 liters of water. Movement is your internal heater!",
    "The brain actually limits your muscle strength to prevent you from snapping your own tendons—you're stronger than you think!",
    "Nitric oxide, found in foods like beets, relaxes blood vessels to improve blood flow and 'the pump.'",
    "Creatine isn't just for muscles—it’s also been shown to support brain function and mental processing speed.",
    "Grip strength is one of the best biological markers for overall longevity and cardiovascular health.",
    "Lifting heavy things strengthens your bones, not just your muscles, by increasing bone mineral density.",
    "The Sartorius is the longest muscle in your body, running from your hip all the way down to your knee.",
    "Your muscles use ATP as their primary energy currency. You recycle your body weight in ATP every single day!",
    "Even your hair has muscles! The arrector pili are tiny muscles that give you 'goosebumps' when you're cold or scared.",
    "Muscle protein synthesis stays elevated for up to 48 hours after a heavy lifting session.",
    "The more muscle mass you have, the better your body manages blood sugar levels.",
    "Tendons act like organic springs, storing and releasing energy to make movements like running more efficient.",
    "You have over 600 muscles in your body, all working together to help you move, breathe, and circulate blood.",
    "The eye muscles are the most active muscles in the body, moving more than 100,000 times a day!",
    "Proteins called actin and myosin are the 'molecular motors' that slide past each other to make your muscles contract.",
    "Stretching doesn't just improve flexibility; it can actually trigger muscle-building signals via mechanical tension.",
    "Caffeine doesn't just wake you up—it reduces the perception of effort, helping you squeeze out those last two reps."
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
    val accent = appearanceOptions.selectedTheme.colors.primary
    val secondaryAccent = appearanceOptions.selectedTheme.colors.secondary
    val surfaceColor = appearanceOptions.selectedTheme.colors.background.copy(alpha = 0.85f)
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    linesToShow.forEach { line ->
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
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color.White.copy(alpha = 0.74f),
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
