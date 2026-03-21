package com.forgecompose.workouttracker.analytics

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
private val nameToMusclesWeighted: List<Pair<Regex, List<Pair<MuscleGroups, Float>>>> = listOf(
    "bench( press)?|flat bench|barbell bench" to listOf(MuscleGroups.Pecs to 1.0f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.6f),
    "incline( bench)?|incline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Delts to 0.7f, MuscleGroups.Triceps to 0.5f),
    "decline( bench)?|decline press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "close[- ]?grip bench" to listOf(MuscleGroups.Triceps to 0.9f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.5f),
    "push[- ]?up|diamond push[- ]?up|wide push[- ]?up|deficit push[- ]?up|ring push[- ]?up" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "dumbbell press|db press" to listOf(MuscleGroups.Pecs to 0.9f, MuscleGroups.Triceps to 0.6f, MuscleGroups.Delts to 0.5f),
    "machine press|smith press|pec deck" to listOf(MuscleGroups.Pecs to 0.8f, MuscleGroups.Delts to 0.5f),
    "chest fly|flye|cable crossover|low to high fly|high to low fly" to listOf(MuscleGroups.Pecs to 0.5f, MuscleGroups.Delts to 0.25f),
    "dip|bench dip" to listOf(MuscleGroups.Triceps to 0.8f, MuscleGroups.Pecs to 0.6f, MuscleGroups.Delts to 0.4f),
    "overhead press|shoulder press|ohp|military press|arnold press|push press|db shoulder press" to listOf(MuscleGroups.Delts to 1.0f, MuscleGroups.Triceps to 0.7f),
    "lateral raise|side raise|cable lateral" to listOf(MuscleGroups.Delts to 0.4f),
    "front raise" to listOf(MuscleGroups.Delts to 0.4f),
    "reverse fly|rear delt fly|face pull" to listOf(MuscleGroups.Delts to 0.5f, MuscleGroups.UpperBack to 0.5f, MuscleGroups.Traps to 0.4f),
    "pull[- ]?up|chin[- ]?up|neutral grip pull[- ]?up" to listOf(MuscleGroups.Lats to 1.0f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "lat pull|pulldown|wide grip pulldown|close grip pulldown" to listOf(MuscleGroups.Lats to 0.9f, MuscleGroups.Biceps to 0.6f, MuscleGroups.UpperBack to 0.5f),
    "row|barbell row|seated row|cable row|t[- ]?bar row|one[- ]?arm row|db row|pendlay row|meadow row" to listOf(MuscleGroups.UpperBack to 0.9f, MuscleGroups.Lats to 0.8f, MuscleGroups.Biceps to 0.6f),
    "shrug|barbell shrug|db shrug" to listOf(MuscleGroups.Traps to 0.9f),
    "pullover|lat pullover" to listOf(MuscleGroups.Lats to 0.6f, MuscleGroups.Pecs to 0.3f),
    "curl|biceps|bicep curl|barbell curl|ez bar curl|preacher curl|hammer curl|incline curl|concentration curl|cable curl|spider curl" to listOf(MuscleGroups.Biceps to 0.5f, MuscleGroups.Forearms to 0.25f),
    "reverse curl|wrist curl|forearm curl" to listOf(MuscleGroups.Forearms to 0.5f, MuscleGroups.Biceps to 0.25f),
    "tricep( extension)?|skullcrusher|overhead extension|rope pushdown|cable pushdown|v[- ]?bar pushdown" to listOf(MuscleGroups.Triceps to 0.5f),
    "back squat|front squat|squat|hack squat|smith squat|goblet squat|zercher squat|overhead squat" to listOf(MuscleGroups.Quads to 1.0f, MuscleGroups.Glutes to 0.8f, MuscleGroups.Hamstrings to 0.6f, MuscleGroups.LowerBack to 0.4f),
    "lunge|walking lunge|reverse lunge|split squat|bulgarian split squat|cossack squat" to listOf(MuscleGroups.Quads to 0.8f, MuscleGroups.Glutes to 0.8f, MuscleGroups.Hamstrings to 0.5f),
    "step[- ]?up|box step" to listOf(MuscleGroups.Quads to 0.7f, MuscleGroups.Glutes to 0.6f),
    "leg press" to listOf(MuscleGroups.Quads to 0.9f, MuscleGroups.Glutes to 0.7f, MuscleGroups.Hamstrings to 0.5f),
    "deadlift|conventional deadlift|sumo deadlift|trap bar deadlift" to listOf(MuscleGroups.Hamstrings to 1.0f, MuscleGroups.Glutes to 0.9f, MuscleGroups.LowerBack to 0.8f, MuscleGroups.UpperBack to 0.4f),
    "rdl|romanian deadlift|stiff[- ]?leg deadlift" to listOf(MuscleGroups.Hamstrings to 0.9f, MuscleGroups.Glutes to 0.8f, MuscleGroups.LowerBack to 0.6f),
    "good morning" to listOf(MuscleGroups.Hamstrings to 0.7f, MuscleGroups.LowerBack to 0.8f, MuscleGroups.Glutes to 0.5f),
    "hip thrust|glute bridge|barbell hip thrust|single[- ]?leg hip thrust" to listOf(MuscleGroups.Glutes to 0.9f, MuscleGroups.Hamstrings to 0.6f),
    "leg extension" to listOf(MuscleGroups.Quads to 0.4f),
    "leg curl|hamstring curl|seated leg curl|lying leg curl" to listOf(MuscleGroups.Hamstrings to 0.4f),
    "calf raise|standing calf|seated calf|donkey calf" to listOf(MuscleGroups.Calves to 0.5f),
    "hip abduction|abductor machine" to listOf(MuscleGroups.Glutes to 0.4f),
    "hip adduction|adductor machine" to listOf(MuscleGroups.Quads to 0.25f, MuscleGroups.Hamstrings to 0.25f),
    "plank|side plank|hollow hold" to listOf(MuscleGroups.Abs to 0.4f, MuscleGroups.LowerBack to 0.25f),
    "crunch|sit[- ]?up|cable crunch|machine crunch" to listOf(MuscleGroups.Abs to 0.4f),
    "leg raise|hanging leg raise|reverse crunch" to listOf(MuscleGroups.Abs to 0.45f),
    "ab rollout|ab wheel" to listOf(MuscleGroups.Abs to 0.5f, MuscleGroups.LowerBack to 0.3f),
    "russian twist|woodchop|pallof press" to listOf(MuscleGroups.Abs to 0.4f),
    "back extension|hyperextension" to listOf(MuscleGroups.LowerBack to 0.6f, MuscleGroups.Glutes to 0.4f, MuscleGroups.Hamstrings to 0.4f)
).map { (pat, gs) -> pat.toRegex(RegexOption.IGNORE_CASE) to gs }

@Composable
fun WeightHistoryGraph(
    recentWorkouts: List<Workout>,
    allWorkouts: List<Workout>
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val textBrush = remember(theme) {
        Brush.horizontalGradient(
            colors = listOf(
                theme.primary,
                Color.White.copy(alpha = 0.9f)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent Highlights",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = textBrush
                ),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (recentWorkouts.isEmpty()) {
                Text(
                    "No recent activity.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                return@Column
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recentWorkouts.forEach { workout ->
                    WorkoutHighlightRow(workout = workout, allWorkouts = allWorkouts, theme = theme)
                }
            }
        }
    }
}

@Composable
fun WorkoutHighlightRow(
    workout: Workout,
    allWorkouts: List<Workout>,
    theme: ColorSchemeAppTheme
) {
    val pr = remember(workout, allWorkouts) {
        checkPrForExercise(
            allWorkouts = allWorkouts.filter { it.id != workout.id },
            exerciseName = workout.name,
            newWeight = workout.weight?.toFloat() ?: 0f,
            newReps = workout.reps ?: 0,
            newSets = workout.sets ?: 0
        )
    }

    val highlightType = when {
        pr.isStrengthPr -> "Strength PR"
        pr.isVolumePr -> "Volume PR"
        else -> "WR"
    }

    val valueText = when {
        pr.isStrengthPr -> "${workout.weight}kg"
        pr.isVolumePr -> "${((workout.weight ?: 0.0) * (workout.reps ?: 0) * (workout.sets ?: 0)).toInt()}kg"
        else -> {
            val entry = nameToMusclesWeighted.firstOrNull { it.first.containsMatchIn(workout.name) }?.second?.maxByOrNull { it.second }
            val wr = if (entry != null) (workout.reps ?: 0) * (workout.sets ?: 0) * entry.second else 0f
            "%.0f WR".format(wr)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = highlightType,
                style = MaterialTheme.typography.labelSmall,
                color = if (pr.isStrengthPr || pr.isVolumePr) theme.primary else Color.White.copy(alpha = 0.6f),
                fontWeight = if (pr.isStrengthPr || pr.isVolumePr) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (pr.isStrengthPr || pr.isVolumePr) theme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (pr.isStrengthPr || pr.isVolumePr) theme.primary else Color.White
            )
        }
    }
}

