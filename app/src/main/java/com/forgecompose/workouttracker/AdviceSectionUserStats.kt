package com.forgecompose.workouttracker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlagCircle
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentReps
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentSets
import com.forgecompose.workouttracker.ConnectedWorkout.CurrentWeight
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.checkPrForExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext





@Composable
fun AdviceSectionUser(
    advice: String,
    lastWorkoutName: String,
    extraLines: List<String>,
    maxExtraLines: Int = 4,
    modifier: Modifier = Modifier,
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val aiEnabled = dynamicModel.personaConfig.value.enabled
    val cold = rememberColdStartStages()
    val uiState by viewModel.uiState.collectAsState()
    val allWorkouts = remember(uiState) { (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty() }
    val selectedWorkout by remember(workout, allWorkouts) {
        derivedStateOf { allWorkouts.firstOrNull { it.name == workout.value } }
    }

    val prFlags = remember(selectedWorkout, CurrentWeight.value, CurrentReps.intValue, CurrentSets.intValue) {
        val currentWorkout = selectedWorkout
        if (currentWorkout == null) {

            PrFlags(strengthPr = false, volumePr = false, repsPr = false, setsPr = false)
        } else {

            val historicalWorkouts = allWorkouts.filter { it.id != currentWorkout.id }

            val pr = checkPrForExercise(
                allWorkouts = historicalWorkouts,
                exerciseName = currentWorkout.name,
                newWeight = currentWorkout.weight?.toFloat() ?: 0.0.toFloat(),
                newReps = currentWorkout.reps ?: 0,
                newSets = currentWorkout.sets ?: 0
            )
            PrFlags(
                strengthPr = pr.isStrengthPr,
                volumePr = pr.isVolumePr,
                repsPr = false,
                setsPr = false
            )
        }
    }
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)

    val accent = remember(aiEnabled, appearanceOptions.selectedTheme.colors.primary) {
        if (aiEnabled) {
            appearanceOptions.selectedTheme.colors.primary
        } else {
            appearanceOptions.selectedTheme.colors.primary.copy(alpha = 0.7f)
        }
    }

    val tips = remember {
        listOf(
            "Muscles grow during deep sleep because protein synthesis spikes at night.",
            "Creatine restores phosphocreatine,letting you push more reps near failure.",
            "Strength drops before size during breaks—your nervous system detunes first.",
            "Caffeine peaks around 45 minutes,which is why pre-workouts hit late.",
            "Training close to failure activates the highest-threshold motor units.",
            "Walking boosts blood flow enough to clear waste from heavy training.",
            "Slow eccentrics create more micro-tension,which triggers hypertrophy.",
            "Creatine pulls water into muscle cells, activating anabolic signaling.",
            "Muscle doesn’t turn into fat,you lose muscle and gain fat separately.",
            "Warm muscles contract more efficiently because enzymes work faster.",
            "Muscle protein synthesis lasts ~48 hours,which is why 2×/week works.",
            "Beginners gain fast because their nervous system suddenly gets efficient.",
            "Low electrolytes can literally weaken muscle contractions.",
            "Grip strength often mirrors overall nervous-system readiness.",
            "High reps vs low reps both build muscle if you reach near failure.",
            "Creatine also boosts brain ATP, improving cognitive endurance.",
            "Soreness doesn’t equal progress—it's usually just a novel stimulus.",
            "A small calorie surplus enhances hormones and training output.",
            "Strength sets need 2–3 minutes for full ATP recovery.",
            "Heart rate stays elevated longer post-lifting because of EPOC.",
            "Protein digestion doesn’t ‘cap’,the body just absorbs slower.",
            "The pump is blood + metabolites,but it still signals muscle growth.",
            "Sweating only means heat loss,not fat loss.",
            "Low sleep raises cortisol,slowing recovery dramatically.",
            "Bar speed in warm-ups predicts how strong you’ll be that day.",
            "A weak core wastes force in compound lifts.",
            "Partial reps overload sticking points for faster progress.",
            "Carbs refill glycogen,which directly boosts reps per set.",
            "Leg training spikes systemic hormones more than upper-body alone.",
            "Hard training can drain over half your 400–500g glycogen stores.",
            "Tendons adapt far slower than muscle—progress too fast and they snap.",
            "The nervous system adapts before muscle, causing early strength jumps.",
            "Cold environments reduce power because contraction efficiency drops.",
            "Vitamin D helps regulate calcium, which muscles use to contract.",
            "You burn more calories recovering from lifting than during the workout.",
            "Unilateral work fixes imbalances because each side must stabilize.",
            "High protein increases thermogenesis—your body burns more digesting it.",
            "Skipping warm-ups lowers force because motor units aren’t recruited.",
            "Muscle is easy to maintain on low volume, but hard to build that way.",
            "Heavy lifting enlarges the left ventricle, boosting stroke volume.",
            "Training consistently raises bone density in the legs and spine.",
            "Mouth-breathing during sets raises perceived exertion faster than nasal breathing.",
            "Testosterone spikes from big lifts are short-lived—consistency builds muscle.",
            "Explosive training makes nerves fire faster, improving bar speed.",
            "Inter-set stretching can increase fascial expansion for hypertrophy.",
            "High-velocity eccentrics strengthen tendon-to-bone attachment points.",
            "Magnesium glycinate improves neuromuscular relaxation and sleep quality.",
            "A 10% reduction in sleep reduces natural testosterone levels by 15%.",
            "Post-activation potentiation uses heavy sets to 'prime' explosive ones.",
            "Citrulline Malate increases arginine levels more effectively than arginine itself.",
            "Hyperplasia is rare; most growth is hypertrophy (cell size increase).",
            "The mind-muscle connection increases EMG activity in target tissues.",
            "Resting less than 60 seconds increases metabolic stress but limits load.",
            "Omega-3 fatty acids reduce systemic inflammation, aiding joint recovery.",
            "Active recovery at 30% intensity flushes lactic acid faster than rest.",
            "Isometric holds at the sticking point can break strength plateaus.",
            "Casein protein before bed provides a 7-hour amino acid release.",
            "The SAID principle means your body adapts specifically to the stressor.",
            "Deep squats recruit more glute fibers than shallow 'power' squats."
        )
    }

    val funTip = remember { tips.random() }

    val displayTitle = remember(prFlags) {
        if (prFlags.strengthPr || prFlags.volumePr) "New Record!" else "Quick tip"
    }


    val displayBody = remember(prFlags, funTip) {
        when {
            prFlags.strengthPr -> "You just set a new Strength PR! Moving more weight than ever."
            prFlags.volumePr -> "Volume PR achieved! Your total workload just hit a new high."
            else -> funTip
        }
    }

    val displayIcon = remember(prFlags) {
        if (prFlags.strengthPr || prFlags.volumePr) Icons.Rounded.FlagCircle else Icons.Rounded.AutoAwesome
    }

    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(9500)
        isLoading = false
    }

    val glowAnimation = rememberInfiniteTransition(label = "borderGlow")
    val borderAlpha by glowAnimation.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = appearanceOptions.selectedTheme.colors.background.copy(alpha = 0.85f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .drawWithCache {
                    onDrawBehind {
                        val currentGlow = if (aiEnabled && isLoading) borderAlpha else 0.25f
                        val borderBrush = Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = currentGlow),
                                accent.copy(alpha = currentGlow * 0.5f)
                            )
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (cold.after200ms) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(tween(750)) togetherWith fadeOut(tween(1000))
                        },
                        label = "adviceContentSwitch"
                    ) { loading ->
                        if (loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = displayIcon,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = displayBody,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.95f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (aiEnabled) Icons.Rounded.Flag else Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Last workout:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lastWorkoutName.ifBlank { "None" },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = appearanceOptions.colors.primary,
                                        overflow = TextOverflow.Visible
                                    )
                                    val linesToShow = remember(extraLines, maxExtraLines) {
                                        extraLines.filter { it.isNotBlank() }.take(maxExtraLines)
                                    }
                                    AnimatedVisibility(
                                        visible = linesToShow.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(clip = false),
                                        exit = fadeOut() + shrinkVertically(clip = false)
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Divider(
                                                color = accent.copy(alpha = 0.25f),
                                                thickness = 1.dp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                linesToShow.forEach { line ->
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ChevronRight,
                                                            contentDescription = null,
                                                            tint = accent.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = line,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Bold,
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
        }
    }
}
