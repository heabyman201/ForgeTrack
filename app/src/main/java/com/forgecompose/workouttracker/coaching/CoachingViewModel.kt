package com.forgecompose.workouttracker.coaching

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forgecompose.workouttracker.ai.AiModelProvider
import com.forgecompose.workouttracker.ai.PersonaPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Owns coaching state for the UI: the selected LLM provider mode, the live muscle
 * signals, the current/most-recent generated [WeeklyPlan], the goal list, and the
 * plan-generation lifecycle. Plans and goals are persisted through [CoachingStore].
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
