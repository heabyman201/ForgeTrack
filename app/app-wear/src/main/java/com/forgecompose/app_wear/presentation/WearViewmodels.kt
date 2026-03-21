package com.forgecompose.app_wear.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Auto-generated ID
    val name: String,
    var date: Long,
    var durationMillis: Long?,
    var startTime: Long,
    var endTime: Long?,
    var status: WorkoutStatus,
    var weight: Double?,
    var sets: Int?,
    var reps: Int?,
    var distance: Double?,
    var notes: String?
)
enum class WorkoutStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}


class WorkoutStatusConverter {
    @TypeConverter
    fun fromStatus(status: WorkoutStatus): String {
        return status.name
    }
    @TypeConverter
    fun toStatus(statusName: String): WorkoutStatus {
        return WorkoutStatus.valueOf(statusName)
    }
}

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["name"], unique = true), // Ensures exercise names are unique
        Index(value = ["muscleGroup"]),
        Index(value = ["exerciseType"])
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val exerciseType: String
)
enum class MuscleGroup {
    CHEST, LEGS, BACK, SHOULDERS, ARMS_BICEPS, ARMS_TRICEPS, CORE, FULL_BODY, CARDIO, OTHER
}
enum class ExerciseType {
    STRENGTH, CARDIO, PLYOMETRIC, STRETCHING, OLYMPIC_LIFTING, STRONGMAN, OTHER
}
class ExerciseTypeConverter {
    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)
}
class MuscleGroupConverter {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup?): String? = value?.name

    @TypeConverter
    fun toMuscleGroup(value: String?): MuscleGroup? = value?.let { MuscleGroup.valueOf(it) }
}
@Database(
    entities = [
        Workout::class,
        Exercise::class,
        WorkoutExercise::class,
        ExerciseSet::class

    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(
    WorkoutStatusConverter::class,
    MuscleGroupConverter::class,
    ExerciseTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long // Returns the new rowId

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutById(workoutId: Long): Flow<Workout?> // Observe a single workout

    @Query("SELECT * FROM workouts ORDER BY date DESC, startTime DESC")
    fun getAllWorkouts(): Flow<List<Workout>> // Observe all workouts, newest first

    @Query("SELECT * FROM workouts WHERE status = :status ORDER BY date DESC, startTime DESC")
    fun getWorkoutsByStatus(status: WorkoutStatus): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<Workout>>

    @Query("SELECT * FROM workouts ORDER BY startTime DESC LIMIT 1")
    fun getLatestWorkout(): Flow<Workout?> // Get the most recently started workout

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getWorkoutCount(): Int

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("SELECT name FROM workouts ORDER BY id DESC LIMIT 1")
    fun getLatestWorkoutName(): Flow<String?>
}
@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>


    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchExercisesByName(searchQuery: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE exerciseType = :exerciseType ORDER BY name ASC")
    fun getExercisesByType(exerciseType: ExerciseType): Flow<List<Exercise>>


    @Query("SELECT DISTINCT name FROM exercises ORDER BY name ASC")
    fun getAllExerciseNames(): Flow<List<String>>
}
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT

        )
    ],
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["workoutId", "exerciseId"]) // Composite index
    ]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long, // Foreign key referencing Exercise.id
    var orderInWorkout: Int = 0,
    var notes: String? = null
)
@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],      // Primary key in the WorkoutExercise table
            childColumns = ["workoutExerciseId"], // Foreign key in this table
            onDelete = ForeignKey.CASCADE // If a WorkoutExercise record is deleted,
            // all its sets are also deleted.
        )
    ],
    indices = [Index(value = ["workoutExerciseId"])] // Index for quicker lookups by workoutExerciseId
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val workoutExerciseId: Long, // Foreign key referencing WorkoutExercise.id

    var setNumber: Int, // The number of this set (e.g., 1, 2, 3)

    var reps: Int?,      // Number of repetitions performed. Nullable for time/distance based sets.
    var weight: Double?,  // Weight used (e.g., in kg or lbs). Nullable for bodyweight/cardio.
    // Note: Unit (kg/lbs) is assumed to be consistent based on user settings
    // or you might add a 'weightUnit: String?' field.

    var distance: Double?, // Distance covered (e.g., km, miles). Nullable for strength exercises.
    // Note: Unit (km/miles) assumed consistent or add 'distanceUnit: String?'.

    var durationSeconds: Int?, // Duration of the set in seconds (e.g., for planks, timed runs). Nullable.

    var restTimeSeconds: Int?, // Optional: Rest taken in seconds *after* this set.

    var isCompleted: Boolean = true, // Was the set completed as planned? (Could be an enum for more states)

    var notes: String? = null // Optional notes specific to this set (e.g., "Felt easy", "Struggled on last rep")
)
@Dao
interface WorkoutExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(workoutExercises: List<WorkoutExercise>)

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise)

    @Query("SELECT * FROM workout_exercises WHERE id = :workoutExerciseId")
    fun getWorkoutExerciseById(workoutExerciseId: Long): Flow<WorkoutExercise?>

    // Get all exercises logged for a specific workout, ordered by their sequence in the workout
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderInWorkout ASC")
    fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>>

    // You might also want a query to get the last orderInWorkout for a given workoutId
    // to help with adding new exercises in the correct order.
    @Query("SELECT MAX(orderInWorkout) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getLastOrderInWorkout(workoutId: Long): Int?
    @Query("SELECT COUNT(*) FROM workouts") // adjust table name
    suspend fun getWorkoutCount(): Int
    // Delete all WorkoutExercise entries for a specific workoutId (e.g., when a workout is cleared or reset)
    // This is often handled by onDelete = ForeignKey.CASCADE on the WorkoutExercise entity for workoutId,
    // but an explicit method can sometimes be useful.
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)
}
@Dao
interface ExerciseSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(exerciseSet: ExerciseSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(exerciseSets: List<ExerciseSet>) // For adding multiple sets at once

    @Update
    suspend fun updateSet(exerciseSet: ExerciseSet)

    @Delete
    suspend fun deleteSet(exerciseSet: ExerciseSet)

    @Query("SELECT * FROM exercise_sets WHERE id = :setId")
    fun getSetById(setId: Long): Flow<ExerciseSet?>

    // Get all sets for a specific logged exercise, ordered by set number
    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    fun getSetsForWorkoutExercise(workoutExerciseId: Long): Flow<List<ExerciseSet>>

    // Delete all sets for a specific workoutExerciseId
    // This is often handled by onDelete = ForeignKey.CASCADE on the ExerciseSet entity,
    // but an explicit method can be useful for specific scenarios.
    @Query("DELETE FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteSetsForWorkoutExercise(workoutExerciseId: Long)

    // Potentially useful: Get the last set number for a given workoutExerciseId
    @Query("SELECT MAX(setNumber) FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun getLastSetNumber(workoutExerciseId: Long): Int?
}

sealed interface WorkoutListUiState {
    object Loading : WorkoutListUiState
    data class Success(val workouts: List<Workout>) : WorkoutListUiState
    data class Error(val message: String) : WorkoutListUiState
}
class WorkoutListViewModel(
    private val workoutRepository: WorkoutRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutListUiState>(WorkoutListUiState.Loading)
    val uiState: StateFlow<WorkoutListUiState> = _uiState.asStateFlow()

    init {
        // Optimized: Don't load all workouts immediately if not needed.
        // For simple list display, we might want to paginate or limit.
        // For now, keeping it but ensure repository is efficient.
        // loadWorkouts()
    }

    // Load only when needed
    fun loadWorkouts() {
        viewModelScope.launch {
            _uiState.value = WorkoutListUiState.Loading
            workoutRepository.getAllWorkouts() // This returns a Flow
                .catch { exception ->
                    _uiState.value =
                        WorkoutListUiState.Error(exception.message ?: "An unknown error occurred")
                }
                .collect { workouts ->
                    _uiState.value = WorkoutListUiState.Success(workouts)
                }
        }
    }

    // Example action: Add a new dummy workout
    fun addSampleWorkout(name: String, status: WorkoutStatus, durationMillis: Long? = null
                         ,weight: Double?,sets: Int?,reps: Int?,distance: Double?,notes: String?) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val newWorkout = Workout(
                    name = name,
                    date = currentTime,
                    startTime = currentTime,
                    endTime = null, // Not ended yet
                    durationMillis = durationMillis,
                    status = WorkoutStatus.COMPLETED,
                    weight = weight,
                    sets = sets,
                    reps = reps,
                    distance = distance,
                    notes = notes
                )
                workoutRepository.insertWorkout(newWorkout)


            } catch (e: Exception) {
                // Handle error, maybe expose it via another StateFlow or an event
                _uiState.value = WorkoutListUiState.Error(e.message ?: "Failed to add workout")
            }
        }
    }
    fun saveToFirestore(
        name: String,
        status: WorkoutStatus,
        durationMillis: Long? = null,
        weight: Double?,
        sets: Int?,
        reps: Int?,
        distance: Double?,
        notes: String?,
        goalWeight: Double? = null,
        goalReps: Int? = null,
        goalSets: Int? = null
    ) {
        val db = Firebase.firestore
        val now = System.currentTimeMillis()
        db.collection("workouts")
            .add(
                hashMapOf(
                    "name" to name,
                    "status" to status.name,
                    "date" to Timestamp.now(),
                    "startTime" to now,
                    "endTime" to now,
                    "durationMillis" to durationMillis,
                    "weight" to weight,
                    "sets" to sets,
                    "reps" to reps,
                    "distance" to distance,
                    "notes" to notes,
                    "goalWeight" to goalWeight,
                    "goalReps" to goalReps,
                    "goalSets" to goalSets
                )

            )
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }
    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteWorkout(workout)
            } catch (e: Exception) {
                _uiState.value = WorkoutListUiState.Error(e.message ?: "Failed to delete workout")
            }
        }
    }
    suspend fun getTotalWorkoutCount(): Int {
        return workoutRepository.getWorkoutCount()
    }
    fun deleteAllWorkouts() {
        viewModelScope.launch {
            workoutRepository.deleteAllWorkouts()
        }
    }
}


class WorkoutListViewModelFactory(
    private val workoutRepository: WorkoutRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutListViewModel(workoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MainScreenViewModel(
    workoutRepository: WorkoutRepo // Inject your repository
) : ViewModel() {

    // Optimized: Use SharingStarted.WhileSubscribed to avoid keeping the connection open when not needed
    // 5000ms is a good default to survive configuration changes
    val latestWorkoutName: StateFlow<Workout?> = workoutRepository.getLatestWorkout()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
}

class MainScreenViewModelFactory(private val repository: WorkoutRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainScreenViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Interface (Optional, but good practice)
interface WorkoutRepo {
    fun getAllWorkouts(): Flow<List<Workout>>
    fun getLatestWorkout(): Flow<Workout?>
    suspend fun insertWorkout(workout: Workout): Long
    suspend fun deleteWorkout(workout: Workout)
    suspend fun deleteAllWorkouts()
    suspend fun getWorkoutCount(): Int
}

// Implementation
class WorkoutRepositoryImpl(private val workoutDao: WorkoutDao) : WorkoutRepo {

    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts()
    }

    override fun getLatestWorkout(): Flow<Workout?> {
        return workoutDao.getLatestWorkout()
    }

    override suspend fun insertWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout)
    }

    override suspend fun deleteWorkout(workout: Workout) {
        workoutDao.deleteWorkout(workout)
    }

    override suspend fun deleteAllWorkouts() {
        workoutDao.deleteAllWorkouts()
    }

    override suspend fun getWorkoutCount(): Int {
        return workoutDao.getWorkoutCount()
    }
}
