package com.forgecompose.app_wear.presentation



import android.content.Context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString


@Serializable
data class WorkoutPreset(
    val name: String,
    val category: String,
    val goalReps: Int? = null,
    val goalSets: Int? = null,
    val goalTimeMillis: Long? = null
)



@Serializable
data class PresetState(
    val weightKg: Float? = null,
    val goalReps: Int? = null,
    val goalSets: Int? = null,
    val goalTimeMillis: Long? = null
)


val workoutPresets = listOf(
    // --- Bodyweight (Calisthenics) ---
    WorkoutPreset("Push-ups", "Bodyweight"),
    WorkoutPreset("Diamond Push-ups", "Bodyweight"),
    WorkoutPreset("Wide Grip Push-ups", "Bodyweight"),
    WorkoutPreset("Pull-ups", "Bodyweight"),
    WorkoutPreset("Chin-ups", "Bodyweight"),
    WorkoutPreset("Dips", "Bodyweight"),
    WorkoutPreset("Bodyweight Squats", "Bodyweight"),
    WorkoutPreset("Pistol Squats", "Bodyweight"),
    WorkoutPreset("Walking Lunges", "Bodyweight"),
    WorkoutPreset("Plank", "Bodyweight"),
    WorkoutPreset("Side Plank", "Bodyweight"),
    WorkoutPreset("Crunches", "Bodyweight"),
    WorkoutPreset("Reverse Crunches", "Bodyweight"),
    WorkoutPreset("Leg Raises", "Bodyweight"),
    WorkoutPreset("Hanging Leg Raises", "Bodyweight"),
    WorkoutPreset("Bicycle Crunches", "Bodyweight"),
    WorkoutPreset("Russian Twists", "Bodyweight"),
    WorkoutPreset("Burpees", "Bodyweight"),
    WorkoutPreset("Mountain Climbers", "Bodyweight"),
    WorkoutPreset("Handstand Push-ups", "Bodyweight"),
    WorkoutPreset("Muscle-ups", "Bodyweight"),
    WorkoutPreset("Glute Bridges", "Bodyweight"),
    WorkoutPreset("Calf Raises", "Bodyweight"),
    WorkoutPreset("Inverted Rows", "Bodyweight"),

    // --- Dumbbell / Kettlebell ---
    WorkoutPreset("Dumbbell Bench Press", "Dumbbell/Kettlebell"),
    WorkoutPreset("Incline Dumbbell Press", "Dumbbell/Kettlebell"),
    WorkoutPreset("Dumbbell Flyes", "Dumbbell/Kettlebell"),
    WorkoutPreset("Goblet Squats", "Dumbbell/Kettlebell"),
    WorkoutPreset("Kettlebell Swings", "Dumbbell/Kettlebell"),
    WorkoutPreset("Dumbbell Rows", "Dumbbell/Kettlebell"),
    WorkoutPreset("Single Arm Dumbbell Row", "Dumbbell/Kettlebell"),
    WorkoutPreset("Bicep Curls", "Dumbbell/Kettlebell"),
    WorkoutPreset("Hammer Curls", "Dumbbell/Kettlebell"),
    WorkoutPreset("Concentration Curls", "Dumbbell/Kettlebell"),
    WorkoutPreset("Tricep Extensions (Overhead)", "Dumbbell/Kettlebell"),
    WorkoutPreset("Tricep Kickbacks", "Dumbbell/Kettlebell"),
    WorkoutPreset("Skull Crushers (Dumbbell)", "Dumbbell/Kettlebell"),
    WorkoutPreset("Overhead Press (Dumbbell)", "Dumbbell/Kettlebell"),
    WorkoutPreset("Arnold Press", "Dumbbell/Kettlebell"),
    WorkoutPreset("Lateral Raises", "Dumbbell/Kettlebell"),
    WorkoutPreset("Front Raises", "Dumbbell/Kettlebell"),
    WorkoutPreset("Dumbbell Lunges", "Dumbbell/Kettlebell"),
    WorkoutPreset("Bulgarian Split Squats", "Dumbbell/Kettlebell"),
    WorkoutPreset("Dumbbell Step-ups", "Dumbbell/Kettlebell"),
    WorkoutPreset("Renegade Rows", "Dumbbell/Kettlebell"),
    WorkoutPreset("Turkish Get-ups", "Dumbbell/Kettlebell"),
    WorkoutPreset("Dumbbell Shrugs", "Dumbbell/Kettlebell"),

    // --- Barbell ---
    WorkoutPreset("Barbell Bench Press", "Barbell"),
    WorkoutPreset("Incline Barbell Press", "Barbell"),
    WorkoutPreset("Barbell Back Squat", "Barbell"),
    WorkoutPreset("Front Squat", "Barbell"),
    WorkoutPreset("Deadlifts", "Barbell"),
    WorkoutPreset("Sumo Deadlifts", "Barbell"),
    WorkoutPreset("Romanian Deadlifts", "Barbell"),
    WorkoutPreset("Overhead Press (Barbell)", "Barbell"),
    WorkoutPreset("Push Press", "Barbell"),
    WorkoutPreset("Bent-Over Rows", "Barbell"),
    WorkoutPreset("Pendlay Rows", "Barbell"),
    WorkoutPreset("Good Mornings", "Barbell"),
    WorkoutPreset("Hip Thrusts", "Barbell"),
    WorkoutPreset("Barbell Curls", "Barbell"),
    WorkoutPreset("Barbell Shrugs", "Barbell"),

    // --- Machines / Cables ---
    WorkoutPreset("Lat Pulldown", "Machines/Cables"),
    WorkoutPreset("Cable Rows", "Machines/Cables"),
    WorkoutPreset("Chest Press Machine", "Machines/Cables"),
    WorkoutPreset("Pec Deck Machine", "Machines/Cables"),
    WorkoutPreset("Leg Press", "Machines/Cables"),
    WorkoutPreset("Leg Curls", "Machines/Cables"),
    WorkoutPreset("Leg Extensions", "Machines/Cables"),
    WorkoutPreset("Cable Crossovers", "Machines/Cables"),
    WorkoutPreset("Cable Bicep Curls", "Machines/Cables"),
    WorkoutPreset("Tricep Pushdowns", "Machines/Cables"),
    WorkoutPreset("Shoulder Press Machine", "Machines/Cables"),
    WorkoutPreset("Hack Squat", "Machines/Cables"),
    WorkoutPreset("Seated Calf Raise Machine", "Machines/Cables"),

    // --- Cardio / Full Body ---
//    WorkoutPreset("Running (Treadmill)", "Cardio"),
//    WorkoutPreset("Stair Climber", "Cardio"),
//    WorkoutPreset("Elliptical Trainer", "Cardio"),
//    WorkoutPreset("Jumping Jacks", "Cardio"),
//    WorkoutPreset("High Knees", "Cardio"),
//    WorkoutPreset("Box Jumps", "Cardio"),
//    WorkoutPreset("Rowing Machine", "Cardio"),
//    WorkoutPreset("Stationary Bike", "Cardio"),
//    WorkoutPreset("Jump Rope", "Cardio"),
//    WorkoutPreset("Swimming", "Cardio")
)
@Serializable
data class WorkoutRoutine(
    val name: String,
    val presetNames: List<String>
)
val workoutRoutines = listOf(
    WorkoutRoutine(
        name = "Push Day",
        presetNames = listOf(

        )
    ),
    WorkoutRoutine(
        name = "Pull Day",
        presetNames = listOf(

        )
    ),
    WorkoutRoutine(
        name = "Leg Day",
        presetNames = listOf(

        )
    )
)
fun WorkoutRoutine.toPresets(allPresets: List<WorkoutPreset>): List<WorkoutPreset> =
    presetNames.mapNotNull { name ->
        allPresets.firstOrNull { it.name == name }
    }

