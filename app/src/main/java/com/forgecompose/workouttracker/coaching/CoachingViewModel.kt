package com.forgecompose.workouttracker.coaching

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forgecompose.workouttracker.ai.AiModelProvider
import com.forgecompose.workouttracker.ai.PersonaPrefs
import com.forgecompose.workouttracker.ai.WorkoutRecommendation
import com.forgecompose.workouttracker.health.HealthConnectManager
import com.forgecompose.workouttracker.muscle.MuscleLoad
import com.forgecompose.workouttracker.muscle.WorkoutSummary
import com.forgecompose.workouttracker.muscle.buildUserProfile
import com.forgecompose.workouttracker.muscle.deriveMuscleLoadsStepwise
import com.forgecompose.workouttracker.muscle.SprintGoalPreferences
import com.forgecompose.workouttracker.muscle.parseSurveyTape
import com.forgecompose.workouttracker.profile.SurveyTape
import com.forgecompose.workouttracker.profile.UserPreferencesManager
import com.forgecompose.workouttracker.workout.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Owns coaching state for the UI: the selected LLM provider mode, the live muscle
 * signals, the current/most-recent generated [WeeklyPlan], the goal list, and the
 * plan-generation lifecycle. Plans and goals are persisted through [CoachingStore].
 *
 * Signal derivation (the same engine the Muscle Status screen uses) lives here so
 * every coaching surface — the Coaching screen, the Goals screen, and the home
 * advice card — shares one computation and one cache. [insights] folds the plan,
 * goals, and signals into ready-to-display [CoachingInsights].
 */
class CoachingViewModel(application: Application) : AndroidViewModel(application) {

    private val _genState = MutableStateFlow<PlanGenState>(PlanGenState.Idle)
    val genState: StateFlow<PlanGenState> = _genState.asStateFlow()

    private val _currentPlan = MutableStateFlow<WeeklyPlan?>(null)
    val currentPlan: StateFlow<WeeklyPlan?> = _currentPlan.asStateFlow()

    private val _goals = MutableStateFlow<List<CoachingGoal>>(emptyList())
    val goals: StateFlow<List<CoachingGoal>> = _goals.asStateFlow()

    private val _provider = MutableStateFlow(AiModelProvider.EDGE_ON_DEVICE)
    val provider: StateFlow<AiModelProvider> = _provider.asStateFlow()

    /** Latest computed muscle signals, fed into the next generation request. */
    private val _signals = MutableStateFlow<List<MuscleSignal>>(emptyList())
    val signals: StateFlow<List<MuscleSignal>> = _signals.asStateFlow()

    private val _recovery = MutableStateFlow(RecoverySnapshot())
    val recovery: StateFlow<RecoverySnapshot> = _recovery.asStateFlow()

    private val _planRequestBase = MutableStateFlow<PlanRequest?>(null)
    val planRequestBase: StateFlow<PlanRequest?> = _planRequestBase.asStateFlow()

    private val _computingSignals = MutableStateFlow(false)
    val computingSignals: StateFlow<Boolean> = _computingSignals.asStateFlow()

    /** Best-effort on-device model read; null when the TFLite asset isn't bundled. */
    private val _recommendation = MutableStateFlow<WorkoutRecommendation?>(null)
    val recommendation: StateFlow<WorkoutRecommendation?> = _recommendation.asStateFlow()

    /** Folds plan + goals + signals into display-ready coaching insights. */
    val insights: StateFlow<CoachingInsights> =
        combine(_currentPlan, _goals, _signals, _recommendation) { plan, goals, signals, rec ->
            buildCoachingInsights(plan, goals, signals, recommendation = rec)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CoachingInsights.EMPTY)

    /** Guards against recomputing signals when nothing about the history changed. */
    private var lastComputedWorkoutCount = -1

    init {
        PersonaPrefs.init(application)
        _provider.value = PersonaPrefs.readModelProvider()
        _currentPlan.value = CoachingStore.loadCurrentPlan(application)
        _goals.value = CoachingStore.loadGoals(application)
    }

    fun setProvider(p: AiModelProvider) {
        _provider.value = p
        PersonaPrefs.writeModelProvider(p)
    }

    fun providerReady(p: AiModelProvider): Boolean = CoachingEngine.providerReady(p)

    fun updateSignals(signals: List<MuscleSignal>) {
        _signals.value = signals
    }

    /**
     * Derives the live muscle signals + recovery snapshot from the workout history
     * (reusing the Muscle Status engine) and prepares a [PlanRequest] template.
     *
     * Safe to call from any screen: it no-ops while already running and skips work
     * when the history is unchanged unless [force] is set.
     */
    fun computeSignals(context: Context, workouts: List<Workout>, force: Boolean = false) {
        if (_computingSignals.value) return
        if (workouts.isEmpty()) return
        if (!force && _signals.value.isNotEmpty() && lastComputedWorkoutCount == workouts.size) return

        val appContext = context.applicationContext
        _computingSignals.value = true
        viewModelScope.launch {
            try {
                val now = Instant.now()
                val start = now.minus(30, ChronoUnit.DAYS)

                val recent = workouts.sortedByDescending { it.date }.take(40).map { w ->
                    val token = buildString {
                        append(w.name.lowercase())
                        w.sets?.takeIf { it > 0 }?.let { append(" $it sets") }
                        w.reps?.takeIf { it > 0 }?.let { append(" $it reps") }
                        (w.sessionRpe ?: w.rpe)?.takeIf { it > 0 }?.let { append(" rpe $it") }
                        if ((w.weight ?: 0.0) > 0.0) append(" heavy")
                    }
                    WorkoutSummary(
                        date = Instant.ofEpochMilli(w.date),
                        name = w.name,
                        exercises = listOf(token),
                        environment = w.trainingEnvironment,
                        sessionRpe = w.sessionRpe ?: w.rpe,
                        fatigueLevel = w.fatigueLevel,
                        restPeriodSeconds = w.restPeriodSeconds,
                        durationMinutes = w.durationMillis?.takeIf { it > 0 }?.let { it / 60000f },
                        sets = w.sets, reps = w.reps, weight = w.weight, distance = w.distance,
                        heartRateAvg = w.heartRateAvg, heartRateMax = w.heartRateMax,
                        systemicDrainScore = w.systemicDrainScore
                    )
                }

                SurveyTape.init(appContext)
                val prefsManager = UserPreferencesManager(appContext)
                val profile = buildUserProfile(prefsManager)
                val surveyContent = SurveyTape.readTape()
                val survey = parseSurveyTape(surveyContent)
                val sprintGoal = SprintGoalPreferences(appContext).load()
                val hc = HealthConnectManager(appContext)

                val (loads, factors) = runCatching {
                    deriveMuscleLoadsStepwise(
                        now = now,
                        recent = recent,
                        profile = profile,
                        surveyTapeContent = surveyContent,
                        sprintGoal = sprintGoal,
                        onStep = { _, _, _, _ -> },
                        sleepSessions = hc.readSleepSessions(start, now),
                        oxygenSaturations = hc.readOxygenSaturation(start, now),
                        nutrition = hc.readNutrition(now.minus(2, ChronoUnit.DAYS), now),
                        bodyFat = hc.readBodyFat(start, now),
                        caloriesBurned = hc.readTotalCalories(now.minus(7, ChronoUnit.DAYS), now),
                        readHeartRate = { s, e -> hc.readHeartRateRecords(s, e) }
                    )
                }.getOrNull() ?: (emptyList<MuscleLoad>() to null)

                val mapped = loads.map { l ->
                    MuscleSignal(
                        muscle = l.group.name,
                        band = l.band.name,
                        weeklyProgress = l.weeklyProgress,
                        weeklyTarget = l.weeklyTarget,
                        injuryRiskPct = (l.injuryRisk * 100f).roundToInt().coerceIn(0, 100),
                        adaptationScore = l.adaptationScore,
                        consistencyScore = l.consistencyScore,
                        developmentScore = l.developmentScore,
                        lastTrainedAgo = l.lastTrainedAgo
                    )
                }

                val recoverySnapshot = factors?.let {
                    RecoverySnapshot(
                        recoveryEfficacy = it.recoveryEfficacy,
                        sleepHours = it.sleepHours,
                        restingHeartRate = it.restingHeartRate,
                        proteinGrams = it.proteinGrams,
                        environmentStress = it.recentEnvironmentStress
                    )
                } ?: _recovery.value

                _signals.value = mapped
                _recovery.value = recoverySnapshot
                _planRequestBase.value = PlanRequest(
                    userPrompt = "",
                    goalTitle = _goals.value.firstOrNull { it.statusEnum == GoalStatus.ACTIVE }?.title.orEmpty(),
                    signals = mapped,
                    recovery = recoverySnapshot,
                    experience = profile.experience,
                    preferredStyle = profile.preferredStyle,
                    importantMuscles = profile.importantMuscles.map { it.name },
                    daysAvailable = parseDays(survey.daysAvailable),
                    sessionMinutes = parseMinutes(survey.sessionLength),
                    equipment = survey.equipmentAccess
                )
                lastComputedWorkoutCount = workouts.size

                // Best-effort on-device model read — never blocks or breaks the flow.
                val trainingWeek = estimateTrainingWeek(workouts, now)
                _recommendation.value = withContext(Dispatchers.IO) {
                    recommendationFromSignals(appContext, mapped, recoverySnapshot, trainingWeek)
                }
            } finally {
                _computingSignals.value = false
            }
        }
    }

    private fun estimateTrainingWeek(workouts: List<Workout>, now: Instant): Float {
        val earliest = workouts.minOfOrNull { it.date } ?: return 1f
        // Instant only supports up to DAYS, so derive weeks from days.
        val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(earliest), now)
        return ((days / 7) + 1).toFloat().coerceIn(1f, 52f)
    }

    fun generatePlan(request: PlanRequest) {
        if (_genState.value is PlanGenState.Loading) return
        val app = getApplication<Application>()
        val provider = _provider.value
        viewModelScope.launch {
            _genState.value = PlanGenState.Loading("Starting")
            val plan = runCatching {
                CoachingEngine.generatePlan(
                    context = app,
                    provider = provider,
                    request = request
                ) { stage -> _genState.value = PlanGenState.Loading(stage) }
            }.getOrElse {
                // Last-resort safety: never leave the user without a plan.
                CoachingEngine.buildHeuristicPlan(request)
            }
            CoachingStore.saveCurrentPlan(app, plan)
            _currentPlan.value = plan
            _genState.value = PlanGenState.Success(plan)
        }
    }

    /** Convenience: build a plan from the cached request template + a free-text prompt. */
    fun generatePlan(userPrompt: String) {
        val base = _planRequestBase.value ?: return
        generatePlan(
            base.copy(
                userPrompt = userPrompt.trim(),
                signals = _signals.value,
                recovery = _recovery.value
            )
        )
    }

    fun dismissGenState() {
        _genState.value = PlanGenState.Idle
    }

    // ---- Goals ----

    fun addGoal(title: String, description: String, focusMuscles: List<String>, targetWeeks: Int) {
        if (title.isBlank()) return
        val goal = CoachingGoal(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            focusMuscles = focusMuscles,
            targetWeeks = targetWeeks.coerceIn(1, 52),
            createdAt = System.currentTimeMillis(),
            progressPct = 0,
            status = GoalStatus.ACTIVE.name
        )
        persistGoals(_goals.value + goal)
    }

    fun updateGoalProgress(id: String, progressPct: Int) {
        persistGoals(_goals.value.map {
            if (it.id == id) {
                val clamped = progressPct.coerceIn(0, 100)
                it.copy(
                    progressPct = clamped,
                    status = if (clamped >= 100) GoalStatus.ACHIEVED.name else it.status
                )
            } else it
        })
    }

    fun setGoalStatus(id: String, status: GoalStatus) {
        persistGoals(_goals.value.map { if (it.id == id) it.copy(status = status.name) else it })
    }

    fun removeGoal(id: String) {
        persistGoals(_goals.value.filterNot { it.id == id })
    }

    private fun persistGoals(updated: List<CoachingGoal>) {
        _goals.value = updated
        CoachingStore.saveGoals(getApplication(), updated)
    }
}
