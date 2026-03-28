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

import android.content.Context
import android.os.Environment
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
import androidx.health.connect.client.records.ExerciseSessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    var date: Long,
    var durationMillis: Long?,
    var startTime: Long,
    var endTime: Long?,
    var status: WorkoutStatus,

    // --- Existing Summary Fields ---
    var weight: Double?,
    var sets: Int?,
    var reps: Int?,
    var distance: Double?,
    var notes: String?,
    var rpe: Int?, // Kept for backward compatibility (or average RPE)
    var tempo: String?,
    var restPeriodSeconds: Int?,
    var heartRateAvg: Int?,
    var heartRateMax: Int?,
    var heartRateTimeline: String? = null,
    var caloriesBurned: Int?,
    var equipmentUsed: String?,
    var weatherConditions: String?,
    var fatigueLevel: Int?,
    var sleepQualityScore: Int?,

    // --- NEW SMART ALGO FIELDS ---
    // The "Baghdad Heat" factor. Used to penalize recovery if temp > 38C
    var trainingEnvironment: String? = null,

    // Global Systemic Fatigue (1-10). Distinct from average RPE.
    // Represents how "fried" the CNS feels after the session.
    var sessionRpe: Int? = null,

    // Calculated by your Algo later. Represents total neurological cost.
    var systemicDrainScore: Float? = null,
    var intensityScore: Int? = null,
    var timingFatigueScore: Int? = null
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
    version = 7,
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

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workouts ADD COLUMN rpe INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN tempo TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN restPeriodSeconds INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN heartRateAvg INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN heartRateMax INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN caloriesBurned INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN equipmentUsed TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN weatherConditions TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN fatigueLevel INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN sleepQualityScore INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add the 3 new columns. SQLite doesn't support adding multiple columns in one statement easily, so we do 3.
                database.execSQL("ALTER TABLE workouts ADD COLUMN trainingEnvironment TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN sessionRpe INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN systemicDrainScore REAL DEFAULT NULL")
            }
        }
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workouts ADD COLUMN heartRateTimeline TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workouts ADD COLUMN intensityScore INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE workouts ADD COLUMN timingFatigueScore INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_tracker"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

    @Query("UPDATE workouts SET notes = :notes WHERE id = :workoutId")
    suspend fun updateWorkoutNotes(workoutId: Long, notes: String?)

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

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts() // Use with caution
    @Query("SELECT name FROM workouts ORDER BY id DESC LIMIT 1")
    fun getLatestWorkoutName(): Flow<String?>

    @Query("SELECT COUNT(*) FROM workouts WHERE notes LIKE '%' || :externalTag || '%'")
    suspend fun countWorkoutsWithExternalTag(externalTag: String): Int
}
@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Or .REPLACE if you want to update on conflict
    suspend fun insertExercise(exercise: Exercise): Long // Returns the new rowId, -1 if ignored

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExercises(exercises: List<Exercise>) // For inserting/updating a list (e.g. predefined)

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
        loadWorkouts()
    }

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

    fun syncHealthConnectWorkouts(context: Context, daysBack: Long = 365) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = HealthConnectManager(context.applicationContext)
                if (manager.checkAvailability() != HealthConnectAvailability.INSTALLED) return@launch
                if (!manager.hasAllPermissions()) return@launch

                val end = Instant.now()
                val start = end.minus(daysBack, ChronoUnit.DAYS)
                val sessions = manager.readExerciseSessions(start, end)
                val heartRateRecords = manager.readHeartRateRecords(start, end)
                val calorieRecords = manager.readTotalCalories(start, end)
                val userPrefs = UserPreferencesManager(context.applicationContext)
                val genericMapping = userPrefs.getHealthConnectGenericWorkoutMapping()
                val existingWorkouts = workoutRepository.getAllWorkouts().first().toMutableList()
                purgeDuplicateHealthConnectImports(existingWorkouts, workoutRepository)

                sessions.forEach { session ->
                    val sourcePackage = session.metadata.dataOrigin.packageName
                    if (sourcePackage.startsWith(context.packageName)) return@forEach

                    val rawId = session.metadata.id.takeIf { it.isNotBlank() }
                        ?: "${session.startTime.toEpochMilli()}_${session.endTime.toEpochMilli()}_${session.exerciseType}"
                    val externalTag = "[HC_ID:$rawId]"
                    val alreadyImported = workoutRepository.hasWorkoutWithExternalTag(externalTag)
                    if (alreadyImported) return@forEach

                    val sessionTitle = normalizeHealthConnectSessionTitle(
                        session.title,
                        healthConnectExerciseTypeLabel(session.exerciseType),
                        genericMapping
                    )
                    val startMillis = session.startTime.toEpochMilli()
                    val endMillis = session.endTime.toEpochMilli()
                    val durationMillis = Duration.between(session.startTime, session.endTime)
                        .toMillis()
                        .coerceAtLeast(0L)
                    val heartRateStats = extractHeartRateStatsForSession(
                        sessionStart = session.startTime,
                        sessionEnd = session.endTime,
                        records = heartRateRecords
                    )
                    val sessionCalories = estimateCaloriesForSession(
                        sessionStart = session.startTime,
                        sessionEnd = session.endTime,
                        records = calorieRecords
                    )
                    val weeklyFrequency = estimateWeeklyFrequencyAt(
                        existingWorkouts = existingWorkouts,
                        anchorMillis = startMillis
                    )
                    val inferredSessionRpe = inferSessionRpe(
                        heartRateAvg = heartRateStats.avg,
                        heartRateMax = heartRateStats.max,
                        caloriesBurned = sessionCalories,
                        durationMillis = durationMillis,
                        weeklyFrequency = weeklyFrequency
                    )
                    val inferredFatigue = inferSessionFatigue(
                        sessionRpe = inferredSessionRpe,
                        caloriesBurned = sessionCalories,
                        heartRateAvg = heartRateStats.avg,
                        heartRateMax = heartRateStats.max,
                        durationMillis = durationMillis,
                        weeklyFrequency = weeklyFrequency
                    )
                    val inferredSystemicDrain = inferSystemicDrainForImport(
                        sessionRpe = inferredSessionRpe,
                        fatigueLevel = inferredFatigue,
                        caloriesBurned = sessionCalories,
                        durationMillis = durationMillis,
                        heartRateAvg = heartRateStats.avg,
                        heartRateMax = heartRateStats.max,
                        weeklyFrequency = weeklyFrequency
                    )
                    val duplicateByTimeAndDate = existingWorkouts.any { workout ->
                        workout.isLikelyDuplicateOf(
                            sessionTitle = sessionTitle,
                            sessionStartMillis = startMillis,
                            sessionEndMillis = endMillis,
                            sessionDurationMillis = durationMillis
                        )
                    }
                    if (duplicateByTimeAndDate) return@forEach

                    val importedWorkout = Workout(
                        name = sessionTitle,
                        date = startMillis,
                        startTime = startMillis,
                        endTime = endMillis,
                        durationMillis = durationMillis,
                        status = WorkoutStatus.COMPLETED,
                        weight = null,
                        sets = null,
                        reps = null,
                        distance = null,
                        notes = "[HC_EXTERNAL]$externalTag[source:$sourcePackage]",
                        rpe = null,
                        tempo = null,
                        restPeriodSeconds = null,
                        heartRateAvg = heartRateStats.avg,
                        heartRateMax = heartRateStats.max,
                        heartRateTimeline = null,
                        caloriesBurned = sessionCalories,
                        equipmentUsed = null,
                        weatherConditions = null,
                        fatigueLevel = inferredFatigue,
                        sleepQualityScore = null,
                        trainingEnvironment = null,
                        sessionRpe = inferredSessionRpe,
                        systemicDrainScore = inferredSystemicDrain
                    )
                    val insertedId = workoutRepository.insertWorkout(importedWorkout)
                    existingWorkouts += importedWorkout.copy(id = insertedId.toInt())
                }
            } catch (_: Exception) {
                // Ignore sync failures; UI should remain responsive.
            }
        }
    }


    fun LogWorkout(
        name: String,
        status: WorkoutStatus,
        durationMillis: Long? = null,
        weight: Double? = null,
        sets: Int? = null,
        reps: Int? = null,
        distance: Double? = null,
        notes: String? = null,
        rpe: Int? = null, // This is the old/average RPE
        tempo: String? = null,
        restPeriodSeconds: Int? = null,
        heartRateAvg: Int? = null,
        heartRateMax: Int? = null,
        heartRateTimeline: String? = null,
        caloriesBurned: Int? = null,
        equipmentUsed: String? = null,
        weatherConditions: String? = null,
        fatigueLevel: Int? = null,
        sleepQualityScore: Int? = null,

        // --- NEW PARAMS ---
        trainingEnvironment: String? = null,
        sessionRpe: Int? = null,
        systemicDrainScore: Float? = null,
        intensityScore: Int? = null,
        timingFatigueScore: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                val newWorkout = Workout(
                    name = name,
                    date = currentTime,
                    startTime = currentTime,
                    endTime = null,
                    durationMillis = durationMillis,
                    status = WorkoutStatus.COMPLETED,
                    weight = weight,
                    sets = sets,
                    reps = reps,
                    distance = distance,
                    notes = notes,
                    rpe = rpe,
                    tempo = tempo,
                    restPeriodSeconds = restPeriodSeconds,
                    heartRateAvg = heartRateAvg,
                    heartRateMax = heartRateMax,
                    heartRateTimeline = heartRateTimeline,
                    caloriesBurned = caloriesBurned,
                    equipmentUsed = equipmentUsed,
                    weatherConditions = weatherConditions,
                    fatigueLevel = fatigueLevel,
                    sleepQualityScore = sleepQualityScore,

                    // --- MAPPING NEW FIELDS ---
                    trainingEnvironment = trainingEnvironment,
                    sessionRpe = sessionRpe,
                    systemicDrainScore = systemicDrainScore,
                    intensityScore = intensityScore,
                    timingFatigueScore = timingFatigueScore
                )
                workoutRepository.insertWorkout(newWorkout)
            } catch (e: Exception) {
                _uiState.value = WorkoutListUiState.Error(e.message ?: "Failed to add workout")
            }
        }
    }
    fun updateWorkoutNotes(workoutId: Long, notes: String?) {
        viewModelScope.launch {
            try {
                workoutRepository.updateWorkoutNotes(
                    workoutId = workoutId,
                    notes = notes?.trim()?.ifBlank { null }
                )
            } catch (e: Exception) {
                _uiState.value = WorkoutListUiState.Error(e.message ?: "Failed to update workout notes")
            }
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
    fun exportWorkoutsToCsv(context: Context, range: ExportDateRange, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (startDate, endDate) = when (range) {
                    ExportDateRange.LAST_WEEK -> LocalDate.now().minus(1, ChronoUnit.WEEKS).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to System.currentTimeMillis()
                    ExportDateRange.LAST_MONTH -> LocalDate.now().minus(1, ChronoUnit.MONTHS).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to System.currentTimeMillis()
                    ExportDateRange.LAST_YEAR -> LocalDate.now().minus(1, ChronoUnit.YEARS).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to System.currentTimeMillis()
                    ExportDateRange.LIFETIME -> 0L to System.currentTimeMillis()
                }

                val workouts = if (range == ExportDateRange.LIFETIME) {
                    workoutRepository.getAllWorkouts().first()
                } else {
                    workoutRepository.getAllWorkouts().first()
                }

                if (workouts.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult("No workouts to export for the selected range.")
                    }
                    return@launch
                }

                val csvHeader = "ID,Name,Date,Duration (ms),Start Time,End Time,Status,Weight,Sets,Reps,Distance,Notes\n"
                val fileName = "workout_history_${System.currentTimeMillis()}.csv"
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val file = File(documentsDir, fileName)

                FileWriter(file).use { writer ->
                    writer.append(csvHeader)
                    workouts.forEach { workout ->
                        writer.append("${workout.id},${workout.name},${workout.date},${workout.durationMillis ?: ""},${workout.startTime},${workout.endTime ?: ""},${workout.status},${workout.weight ?: ""},${workout.sets ?: ""},${workout.reps ?: ""},${workout.distance ?: ""},${workout.notes?.replace(",", ";") ?: ""}\n")
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult("Export successful! Saved to Documents/$fileName")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Export failed: ${e.message}")
                }
            }
        }
    }
}

private fun healthConnectExerciseTypeLabel(type: Int): String {
    return when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
        else -> "Cardio Workout"
    }
}

private fun normalizeHealthConnectSessionTitle(
    rawTitle: String?,
    fallbackTypeLabel: String,
    genericMapping: HealthConnectGenericWorkoutMapping
): String {
    val normalized = rawTitle?.trim().orEmpty()
    if (normalized.isBlank()) return fallbackTypeLabel

    val genericTitles = setOf(
        "workout",
        "other workout",
        "exercise",
        "training",
        "training session",
        "session"
    )
    if (normalized.lowercase() !in genericTitles) return normalized

    return when (genericMapping) {
        HealthConnectGenericWorkoutMapping.AUTO -> fallbackTypeLabel
        HealthConnectGenericWorkoutMapping.STRENGTH -> "Strength Training"
        HealthConnectGenericWorkoutMapping.CARDIO -> "Cardio Workout"
        HealthConnectGenericWorkoutMapping.HIIT -> "HIIT Workout"
    }
}

private data class SessionHeartRateStats(val avg: Int?, val max: Int?)

private fun extractHeartRateStatsForSession(
    sessionStart: Instant,
    sessionEnd: Instant,
    records: List<androidx.health.connect.client.records.HeartRateRecord>
): SessionHeartRateStats {
    val samples = records.asSequence()
        .flatMap { it.samples.asSequence() }
        .filter { sample ->
            (sample.time == sessionStart || sample.time.isAfter(sessionStart)) &&
                (sample.time == sessionEnd || sample.time.isBefore(sessionEnd))
        }
        .map { it.beatsPerMinute }
        .toList()

    if (samples.isEmpty()) return SessionHeartRateStats(avg = null, max = null)
    return SessionHeartRateStats(
        avg = samples.average().roundToInt(),
        max = samples.maxOrNull()?.toInt()
    )
}

private fun estimateCaloriesForSession(
    sessionStart: Instant,
    sessionEnd: Instant,
    records: List<androidx.health.connect.client.records.TotalCaloriesBurnedRecord>
): Int? {
    val sessionStartMs = sessionStart.toEpochMilli()
    val sessionEndMs = sessionEnd.toEpochMilli()
    if (sessionEndMs <= sessionStartMs) return null

    val kcal = records.sumOf { rec ->
        val recStartMs = rec.startTime.toEpochMilli()
        val recEndMs = rec.endTime.toEpochMilli()
        val recDurationMs = (recEndMs - recStartMs).coerceAtLeast(1L)
        val overlapMs = max(0L, min(sessionEndMs, recEndMs) - max(sessionStartMs, recStartMs))
        if (overlapMs <= 0L) {
            0.0
        } else {
            val ratio = overlapMs.toDouble() / recDurationMs.toDouble()
            rec.energy.inKilocalories * ratio
        }
    }

    return kcal.takeIf { it > 0.0 }?.roundToInt()
}

private fun inferSessionRpe(
    heartRateAvg: Int?,
    heartRateMax: Int?,
    caloriesBurned: Int?,
    durationMillis: Long,
    weeklyFrequency: Float
): Int? {
    val durationMinutes = (durationMillis / 60000.0).coerceAtLeast(1.0)
    val kcal = caloriesBurned ?: 0
    val kcalPerHour = if (kcal > 0) kcal / (durationMinutes / 60.0) else 0.0

    val avgHrValue = heartRateAvg ?: 0
    val maxHrValue = heartRateMax ?: 0
    val hrAvgLoad = if (avgHrValue > 0) {
        ((avgHrValue.toDouble() - 95.0) / 70.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val hrPeakLoad = if (maxHrValue > 0) {
        ((maxHrValue.toDouble() - 130.0) / 65.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val kcalDensityLoad = when {
        kcalPerHour >= 900.0 -> 1.2
        kcalPerHour >= 700.0 -> 0.9
        kcalPerHour >= 500.0 -> 0.6
        kcalPerHour >= 350.0 -> 0.3
        else -> 0.0
    }
    val durationLoad = when {
        durationMinutes >= 120.0 -> 1.0
        durationMinutes >= 90.0 -> 0.75
        durationMinutes >= 60.0 -> 0.45
        durationMinutes >= 35.0 -> 0.2
        else -> 0.0
    }
    val frequencyLoad = when {
        weeklyFrequency >= 6f -> 0.9
        weeklyFrequency >= 4.5f -> 0.5
        weeklyFrequency <= 1.0f -> -0.2
        else -> 0.0
    }

    val score = 4.2 +
        (hrAvgLoad * 2.0) +
        (hrPeakLoad * 1.4) +
        (kcalDensityLoad * 1.8) +
        durationLoad +
        frequencyLoad

    return score.roundToInt().coerceIn(3, 10)
}

private fun inferSessionFatigue(
    sessionRpe: Int?,
    caloriesBurned: Int?,
    heartRateAvg: Int?,
    heartRateMax: Int?,
    durationMillis: Long,
    weeklyFrequency: Float
): Int? {
    if (sessionRpe == null) return null
    val durationMinutes = (durationMillis / 60000.0).coerceAtLeast(1.0)
    val kcal = caloriesBurned ?: 0
    val kcalPerHour = if (kcal > 0) kcal / (durationMinutes / 60.0) else 0.0

    val avgHrValue = heartRateAvg ?: 0
    val maxHrValue = heartRateMax ?: 0
    val hrAvgLoad = if (avgHrValue > 0) {
        ((avgHrValue.toDouble() - 95.0) / 70.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val hrPeakLoad = if (maxHrValue > 0) {
        ((maxHrValue.toDouble() - 130.0) / 65.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val hrStrain = (hrAvgLoad * 0.65) + (hrPeakLoad * 0.35)

    val kcalFatigue = when {
        kcalPerHour >= 900.0 -> 1.2
        kcalPerHour >= 700.0 -> 0.9
        kcalPerHour >= 500.0 -> 0.5
        else -> 0.0
    }
    val durationPenalty = when {
        durationMinutes >= 120.0 -> 1.2
        durationMinutes >= 90.0 -> 0.9
        durationMinutes >= 60.0 -> 0.6
        durationMinutes >= 35.0 -> 0.3
        else -> 0.0
    }
    val frequencyPenalty = when {
        weeklyFrequency >= 6f -> 1.0
        weeklyFrequency >= 4.5f -> 0.6
        else -> 0.0
    }

    val fatigue = (sessionRpe * 0.72) + (hrStrain * 2.2) + (kcalFatigue * 1.4) + durationPenalty + frequencyPenalty
    return fatigue.roundToInt().coerceIn(1, 10)
}

private fun inferSystemicDrainForImport(
    sessionRpe: Int?,
    fatigueLevel: Int?,
    caloriesBurned: Int?,
    durationMillis: Long,
    heartRateAvg: Int?,
    heartRateMax: Int?,
    weeklyFrequency: Float
): Float {
    val durationMinutes = (durationMillis / 60000.0).coerceAtLeast(1.0)
    val kcal = caloriesBurned ?: 0
    val kcalPerHour = if (kcal > 0) kcal / (durationMinutes / 60.0) else 0.0

    val avgHrValue = heartRateAvg ?: 0
    val maxHrValue = heartRateMax ?: 0
    val hrAvgLoad = if (avgHrValue > 0) {
        ((avgHrValue.toDouble() - 95.0) / 70.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val hrPeakLoad = if (maxHrValue > 0) {
        ((maxHrValue.toDouble() - 130.0) / 65.0).coerceIn(0.0, 1.3)
    } else {
        0.0
    }
    val hrStrain = (hrAvgLoad * 0.65) + (hrPeakLoad * 0.35)

    val rpeNorm = ((sessionRpe ?: 5) / 10.0).coerceIn(0.2, 1.0)
    val fatigueNorm = ((fatigueLevel ?: sessionRpe ?: 5) / 10.0).coerceIn(0.2, 1.0)
    val energyNorm = (kcalPerHour / 950.0).coerceIn(0.0, 1.2)
    val durationNorm = (durationMinutes / 90.0).coerceIn(0.2, 1.2)
    val frequencyNorm = (weeklyFrequency / 5.0).coerceIn(0.2, 1.4).toDouble()

    val drain = (
        (rpeNorm * 35.0) +
            (fatigueNorm * 30.0) +
            (hrStrain * 12.0) +
            (energyNorm * 11.0) +
            (durationNorm * 7.0) +
            (frequencyNorm * 5.0)
        )
    return drain.toFloat().coerceIn(0f, 100f)
}

private fun estimateWeeklyFrequencyAt(existingWorkouts: List<Workout>, anchorMillis: Long): Float {
    val lookbackMillis = Duration.ofDays(28).toMillis()
    val startMillis = anchorMillis - lookbackMillis
    val recentCount = existingWorkouts.count { workout ->
        workout.startTime in startMillis until anchorMillis
    }
    return (recentCount.toFloat() / 4f).coerceAtLeast(0f)
}

private const val HEALTH_CONNECT_DUPLICATE_TOLERANCE_MS = 3 * 60 * 1000L

private fun Workout.isLikelyDuplicateOf(
    sessionTitle: String,
    sessionStartMillis: Long,
    sessionEndMillis: Long,
    sessionDurationMillis: Long
): Boolean {
    val existingEnd = endTime ?: (startTime + (durationMillis ?: 0L))
    val existingDuration = (durationMillis ?: (existingEnd - startTime)).coerceAtLeast(0L)
    val sameDay = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate() ==
        Instant.ofEpochMilli(sessionStartMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    if (!sameDay) return false

    val startDiff = abs(startTime - sessionStartMillis)
    val endDiff = abs(existingEnd - sessionEndMillis)
    val durationDiff = abs(existingDuration - sessionDurationMillis)
    val sameName = name.equals(sessionTitle, ignoreCase = true)

    return (sameName && startDiff <= HEALTH_CONNECT_DUPLICATE_TOLERANCE_MS && endDiff <= HEALTH_CONNECT_DUPLICATE_TOLERANCE_MS) ||
        (startDiff <= HEALTH_CONNECT_DUPLICATE_TOLERANCE_MS && durationDiff <= HEALTH_CONNECT_DUPLICATE_TOLERANCE_MS)
}

private fun Workout.isHealthConnectImport(): Boolean {
    return notes?.contains("[HC_EXTERNAL]") == true && notes?.contains("[HC_ID:") == true
}

private suspend fun purgeDuplicateHealthConnectImports(
    existingWorkouts: MutableList<Workout>,
    workoutRepository: WorkoutRepo
) {
    val importedWorkouts = existingWorkouts.filter { it.isHealthConnectImport() }
    importedWorkouts.forEach { imported ->
        val importedEnd = imported.endTime ?: (imported.startTime + (imported.durationMillis ?: 0L))
        val importedDuration = (imported.durationMillis ?: (importedEnd - imported.startTime)).coerceAtLeast(0L)
        val duplicateLocalExists = existingWorkouts.any { candidate ->
            candidate.id != imported.id &&
                !candidate.isHealthConnectImport() &&
                candidate.isLikelyDuplicateOf(
                    sessionTitle = imported.name,
                    sessionStartMillis = imported.startTime,
                    sessionEndMillis = importedEnd,
                    sessionDurationMillis = importedDuration
                )
        }
        if (duplicateLocalExists) {
            workoutRepository.deleteWorkout(imported)
            existingWorkouts.removeAll { it.id == imported.id }
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

data class PrFlags(
    val strengthPr: Boolean = false,
    val volumePr: Boolean = false,
    val repsPr: Boolean = false,
    val setsPr: Boolean = false
) {
    val any get() = strengthPr || volumePr || repsPr || setsPr
}

data class PrResult(
    val isStrengthPr: Boolean,
    val isVolumePr: Boolean,
    val prevBestE1rm: Float?,
    val prevBestVolume: Float?
)

private fun epley1RM(weight: Float, reps: Int): Float {
    if (weight <= 0f || reps <= 0) return 0f
    return weight * (1f + reps / 30f)
}

fun checkPrForExercise(
    allWorkouts: List<Workout>,
    exerciseName: String,
    newWeight: Float,
    newReps: Int,
    newSets: Int
): PrResult {
    // Guard against garbage input
    if (newWeight <= 0f || newReps <= 0 || newSets <= 0) {
        return PrResult(
            isStrengthPr = false,
            isVolumePr = false,
            prevBestE1rm = null,
            prevBestVolume = null
        )
    }

    val newE1rm = epley1RM(newWeight, newReps)
    val newVolume = newWeight * newReps * newSets

    val previous = allWorkouts
        .asSequence()
        .filter { it.name == exerciseName }
        .mapNotNull { w ->
            val weight = w.weight?.toFloat()
            val reps = w.reps
            val sets = w.sets

            // Skip invalid or incomplete entries
            if (weight == null || weight <= 0f || reps!! <= 0 || sets!! <= 0) {
                null
            } else {
                Triple(weight, reps, sets)
            }
        }
        .toList()

    val bestPrevE1rm = previous.maxOfOrNull { (weight, reps, _) ->
        epley1RM(weight, reps)
    }

    val bestPrevVolume = previous.maxOfOrNull { (weight, reps, sets) ->
        weight * reps * sets
    }

    val isStrengthPr = bestPrevE1rm == null || newE1rm > bestPrevE1rm
    val isVolumePr = bestPrevVolume == null || newVolume > bestPrevVolume

    return PrResult(
        isStrengthPr = isStrengthPr,
        isVolumePr = isVolumePr,
        prevBestE1rm = bestPrevE1rm,
        prevBestVolume = bestPrevVolume
    )
}


