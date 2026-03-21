package com.forgecompose.workouttracker.workout

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import kotlinx.coroutines.flow.Flow

interface WorkoutRepo {
    fun getAllWorkouts(): Flow<List<Workout>>
    fun getWorkoutById(workoutId: Long): Flow<Workout?>
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>>
    fun getLatestWorkout(): Flow<Workout?>
    suspend fun insertWorkout(workout: Workout): Long
    suspend fun updateWorkout(workout: Workout)
    suspend fun updateWorkoutNotes(workoutId: Long, notes: String?)
    suspend fun deleteWorkout(workout: Workout)

    suspend fun deleteAllWorkouts()

    suspend fun hasWorkoutWithExternalTag(externalTag: String): Boolean

    fun getLatestWorkoutName(): Flow<String?>

    // --- WorkoutExercise Methods ---
    fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>>
    suspend fun getWorkoutExerciseById(workoutExerciseId: Long): Flow<WorkoutExercise?> // Changed to Flow if needed for observation
    suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Long
    suspend fun updateLoggedExercise(workoutExercise: WorkoutExercise)
    suspend fun removeLoggedExercise(workoutExercise: WorkoutExercise)
    suspend fun getLastOrderInWorkout(workoutId: Long): Int?

    suspend fun getWorkoutCount(): Int = getWorkoutCount()
    // --- ExerciseSet Methods ---
    fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<ExerciseSet>>
    suspend fun getSetById(setId: Long): Flow<ExerciseSet?> // Changed to Flow if needed for observation
    suspend fun addSetToLoggedExercise(exerciseSet: ExerciseSet): Long
    suspend fun updateSet(exerciseSet: ExerciseSet)
    suspend fun removeSet(exerciseSet: ExerciseSet)
    suspend fun getLastSetNumber(workoutExerciseId: Long): Int?

}
class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseSetDao: ExerciseSetDao,

) : WorkoutRepo {

    // --- Workout Methods ---
    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts()
    }

    override fun getWorkoutById(workoutId: Long): Flow<Workout?> {
        return workoutDao.getWorkoutById(workoutId)
    }
    override fun getLatestWorkoutName(): Flow<String?> {
        return workoutDao.getLatestWorkoutName()
    }
    override suspend fun deleteAllWorkouts() {
        workoutDao.deleteAll()
    }

    override suspend fun hasWorkoutWithExternalTag(externalTag: String): Boolean {
        return workoutDao.countWorkoutsWithExternalTag(externalTag) > 0
    }

    override fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>> {
        return workoutDao.getWorkoutsByStatus(status)
    }

    override fun getLatestWorkout(): Flow<Workout?> {
        return workoutDao.getLatestWorkout()
    }

    override suspend fun insertWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout)
    }

    override suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    override suspend fun updateWorkoutNotes(workoutId: Long, notes: String?) {
        workoutDao.updateWorkoutNotes(workoutId, notes)
    }

    override suspend fun deleteWorkout(workout: Workout) {
        // Note: Due to onDelete = ForeignKey.CASCADE, deleting a workout will also
        // delete its associated WorkoutExercises, which in turn will delete their ExerciseSets.
        workoutDao.deleteWorkout(workout)
    }



    // --- WorkoutExercise Methods ---
    override fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>> {
        return workoutExerciseDao.getExercisesForWorkout(workoutId)
    }

    override suspend fun getWorkoutExerciseById(workoutExerciseId: Long): Flow<WorkoutExercise?> {
        return workoutExerciseDao.getWorkoutExerciseById(workoutExerciseId)
    }

    override suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Long {
        return workoutExerciseDao.insertWorkoutExercise(workoutExercise)
    }

    override suspend fun updateLoggedExercise(workoutExercise: WorkoutExercise) {
        workoutExerciseDao.updateWorkoutExercise(workoutExercise)
    }

    override suspend fun removeLoggedExercise(workoutExercise: WorkoutExercise) {
        // Note: Due to onDelete = ForeignKey.CASCADE on ExerciseSet for workoutExerciseId,
        // deleting a WorkoutExercise will also delete its associated ExerciseSets.
        workoutExerciseDao.deleteWorkoutExercise(workoutExercise)
    }

    override suspend fun getLastOrderInWorkout(workoutId: Long): Int? {
        return workoutExerciseDao.getLastOrderInWorkout(workoutId)
    }

    // --- ExerciseSet Methods ---
    override fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<ExerciseSet>> {
        return exerciseSetDao.getSetsForWorkoutExercise(workoutExerciseId)
    }

    override suspend fun getSetById(setId: Long): Flow<ExerciseSet?> {
        return exerciseSetDao.getSetById(setId)
    }

    override suspend fun addSetToLoggedExercise(exerciseSet: ExerciseSet): Long {
        return exerciseSetDao.insertSet(exerciseSet)
    }

    override suspend fun updateSet(exerciseSet: ExerciseSet) {
        exerciseSetDao.updateSet(exerciseSet)
    }

    override suspend fun removeSet(exerciseSet: ExerciseSet) {
        exerciseSetDao.deleteSet(exerciseSet)
    }

    override suspend fun getLastSetNumber(workoutExerciseId: Long): Int? {
        return exerciseSetDao.getLastSetNumber(workoutExerciseId)
    }
}
interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>
    fun searchExercisesByName(searchQuery: String): Flow<List<Exercise>>
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>>
    fun getExercisesByType(exerciseType: ExerciseType): Flow<List<Exercise>>
    fun getExercisesByCustomFlag(isCustom: Boolean): Flow<List<Exercise>>
    fun getAllExerciseNames(): Flow<List<String>>

    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun insertOrUpdateExercises(exercises: List<Exercise>) // For pre-populating data
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
    suspend fun deleteAllCustomExercises()
}
class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises()
    }

    override fun getExerciseById(exerciseId: Long): Flow<Exercise?> {
        return exerciseDao.getExerciseById(exerciseId)
    }

    override fun searchExercisesByName(searchQuery: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercisesByName(searchQuery)
    }

    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByMuscleGroup(muscleGroup)
    }

    override fun getExercisesByType(exerciseType: ExerciseType): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByType(exerciseType)
    }

    override fun getExercisesByCustomFlag(isCustom: Boolean): Flow<List<Exercise>> {
        TODO("Not yet implemented")
    }


    override fun getAllExerciseNames(): Flow<List<String>> {
        return exerciseDao.getAllExerciseNames()
    }

    override suspend fun insertExercise(exercise: Exercise): Long {
        return exerciseDao.insertExercise(exercise)
    }

    override suspend fun insertOrUpdateExercises(exercises: List<Exercise>) {
        exerciseDao.insertOrUpdateExercises(exercises)
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateExercise(exercise)
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        // Remember: If an exercise is referenced in WorkoutExercise and onDelete is RESTRICT,
        // this operation will fail. You might need to handle this case in your ViewModel/UI.
        exerciseDao.deleteExercise(exercise)
    }

    override suspend fun deleteAllCustomExercises() {
        TODO("Not yet implemented")
    }


}
