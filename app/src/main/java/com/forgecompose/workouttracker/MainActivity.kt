package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.coaching.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*



import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.RenderEffect.createBlurEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.ai.PersonaPrefs.readPersona
import com.forgecompose.workouttracker.ui.components.blurAnim.intensity
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import com.google.common.math.IntMath.pow
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.collections.emptyList
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.system.exitProcess

var startDestination = "home"

private data class WorkoutPatternSuggestion(
    val workoutName: String,
    val sessionsTracked: Int,
    val avgIntensity: Int,
    val avgRpe: Int,
    val avgFatigue: Int,
    val avgRestSeconds: Int,
    val avgGapDays: Double,
    val daysSinceLast: Double,
    val rationale: String
) {
    val promptSummary: String
        get() = buildString {
            append("Typical user pattern points to ")
            append(workoutName)
            append(". Usually repeats every ")
            append(String.format("%.1f", avgGapDays))
            append(" days, averages intensity ")
            append(avgIntensity)
            append("/10, RPE ")
            append(avgRpe)
            append("/10, fatigue ")
            append(avgFatigue)
            append("/10, and rest around ")
            append(avgRestSeconds)
            append("s. ")
            append(rationale)
        }

    val homeLine: String
        get() = "Next workout: $workoutName, intensity $avgIntensity/10, rest ${avgRestSeconds}s."
}

private fun Workout.patternIntensityScore(): Int {
    return intensityScore
        ?: sessionRpe
        ?: rpe
        ?: run {
            val rest = restPeriodSeconds ?: 90
            val restBonus = when {
                rest <= 60 -> 2
                rest <= 90 -> 1
                rest >= 180 -> -1
                else -> 0
            }
            (5 + restBonus).coerceIn(1, 10)
        }
}

private fun buildWorkoutPatternSuggestion(
    workouts: List<Workout>,
    latestWorkout: Workout?
): WorkoutPatternSuggestion? {
    if (workouts.isEmpty()) return null
    val now = System.currentTimeMillis()
    val latestFatigue = latestWorkout?.fatigueLevel ?: 0
    val latestIntensity = latestWorkout?.patternIntensityScore() ?: 0

    return workouts
        .filter { it.name.isNotBlank() }
        .groupBy { it.name.trim() }
        .mapNotNull { (name, entries) ->
            val sorted = entries.sortedByDescending { it.date }
            val count = sorted.size
            if (count == 0) return@mapNotNull null

            val intensityValues = sorted.map { it.patternIntensityScore() }
            val avgIntensity = intensityValues.average().roundToInt().coerceIn(1, 10)
            val avgRpe = sorted.mapNotNull { it.sessionRpe ?: it.rpe }.averageOrNull()?.roundToInt() ?: avgIntensity
            val avgFatigue = sorted.mapNotNull { it.fatigueLevel }.averageOrNull()?.roundToInt() ?: avgRpe
            val avgRest = sorted.mapNotNull { it.restPeriodSeconds }.averageOrNull()?.roundToInt() ?: 90
            val ascending = sorted.sortedBy { it.date }
            val gapDays = ascending.zipWithNext { previous, current ->
                ((current.date - previous.date).toDouble() / DateUtils.DAY_IN_MILLIS.toDouble()).coerceAtLeast(0.25)
            }
            val avgGapDays = if (gapDays.isNotEmpty()) gapDays.average() else 3.0
            val lastDate = sorted.first().date
            val daysSinceLast = ((now - lastDate).toDouble() / DateUtils.DAY_IN_MILLIS.toDouble()).coerceAtLeast(0.0)
            val isLatestWorkout = latestWorkout?.name?.equals(name, ignoreCase = true) == true
            val readinessRatio = (daysSinceLast / avgGapDays.coerceAtLeast(0.75)).coerceIn(0.0, 2.4)
            val frequencyScore = count.coerceAtMost(8) * 1.2
            val readinessScore = readinessRatio * 2.3
            val recoveryBias = when {
                latestFatigue >= 7 || latestIntensity >= 8 -> if (isLatestWorkout) -1.4 else 1.2
                isLatestWorkout -> 0.6
                else -> 0.8
            }
            val patternBias = when {
                avgIntensity <= 6 && avgFatigue <= 6 -> 0.7
                avgIntensity >= 8 && latestFatigue >= 7 -> -0.6
                else -> 0.2
            }
            val score = frequencyScore + readinessScore + recoveryBias + patternBias
            val rationale = when {
                readinessRatio >= 1.1 -> "$name is due again based on the user's normal rotation."
                latestFatigue >= 7 || latestIntensity >= 8 -> "$name usually fits better after harder sessions."
                else -> "$name is the most stable pattern in recent training."
            }

            WorkoutPatternSuggestion(
                workoutName = name,
                sessionsTracked = count,
                avgIntensity = avgIntensity,
                avgRpe = avgRpe.coerceIn(1, 10),
                avgFatigue = avgFatigue.coerceIn(1, 10),
                avgRestSeconds = avgRest.coerceAtLeast(30),
                avgGapDays = avgGapDays,
                daysSinceLast = daysSinceLast,
                rationale = rationale
            ) to score
        }
        .maxByOrNull { it.second }
        ?.first
}

private fun Iterable<Int>.averageOrNull(): Double? {
    var count = 0
    var sum = 0.0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

class MainActivity : ComponentActivity() {

    private lateinit var onboardingManager: OnboardingManager
    private lateinit var userPreferencesManager: UserPreferencesManager


    private val mergedRepository by lazy {

        val localRepo = (application as MyApplication).workoutRepository


        MergedWorkoutRepository(localRepo)
    }
    private val workoutRepository by lazy {
        (application as MyApplication).workoutRepository
    }


    private val mainScreenViewModel: MainScreenViewModel by viewModels {
        MainScreenViewModelFactory(mergedRepository)
    }


    private val workoutListViewModel: WorkoutListViewModel by viewModels {
        WorkoutListViewModelFactory(mergedRepository)
    }


    private val badgeViewModel: BadgeViewModel by viewModels {
        BadgeViewModelFactory(
            badgeStorage = PersistentBadgeStorage(applicationContext)
        )
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isTaskRoot) {
            finish()
            return
        }


        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        onboardingManager = OnboardingManager(this)
        userPreferencesManager = UserPreferencesManager(this)

        startDestination = if (onboardingManager.isFirstTimeLaunch()) {
            "onboarding"
        } else {
            "HomeScreen"
        }

        setContent {
            WorkoutTrackerTheme {
                val context = LocalContext.current
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* no-op for now */ }

                var showNotifDialog by remember { mutableStateOf(false) }
                @Composable
                fun checkNotificationPermission() {
                    if (showNotifDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotifDialog = false },
                            title = { Text("Enable Notifications") },
                            text = { Text("Allow notifications so we can show workout timers and progress.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showNotifDialog = false
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) { Text("Allow") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNotifDialog = false }) { Text("Not now") }
                            }
                        )
                    }

                }
                val initialRoute = remember { 
                    intent?.getStringExtra("navigation_route") 
                }




                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val missing = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                        showNotifDialog = missing
                    } else {
                        showNotifDialog = false
                    }
                }
               checkNotificationPermission()
                MainScreen(
                    viewModel = workoutListViewModel,
                    viewModel2 = mainScreenViewModel,
                    badgeViewModel = badgeViewModel,
                    initialRoute = initialRoute
                )
            }
        }
    }
}


    object Routes {
    const val DetailedWorkout = "DetailedWorkout"
    const val ArgId = "workoutId"
    val DetailedWorkoutRoute = "$DetailedWorkout/{$ArgId}"
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(viewModel: WorkoutListViewModel, viewModel2: MainScreenViewModel,badgeViewModel: BadgeViewModel, initialRoute: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onboardingManager = remember { OnboardingManager(context) }
    val userPreferencesManager = remember { UserPreferencesManager(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                badgeViewModel.checkPendingNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val slideSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )
    val badgeList by badgeViewModel.badges.collectAsState()
    val fadeInSpec = tween<Float>(durationMillis = 140, easing = LinearEasing)
    val fadeOutSpec = tween<Float>(durationMillis = 120, easing = LinearEasing)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (personalRecords, recentWorkouts) = remember(uiState) {
        if (uiState is WorkoutListUiState.Success) {
            val workouts = (uiState as WorkoutListUiState.Success).workouts
            val prs = workouts
                .asSequence()
                .filter { (it.weight ?: 0.0) > 0.0 }
                .groupBy { it.name }
                .map { (name, workoutList) -> PersonalRecord(name, workoutList.maxOf { it.weight!! }) }
                .sortedByDescending { it.maxWeight }
            val recent = workouts.take(5)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
    }
    val allWorkouts = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()

    val totalWorkouts = remember(uiState) {
        allWorkouts.size
    }

    val unlockQueue = remember { mutableStateListOf<BadgeUiState>() }

    LaunchedEffect(badgeViewModel) {
        badgeViewModel.newUnlockEvent.collect { badge ->
            unlockQueue.add(badge)
        }
    }


    LaunchedEffect(initialRoute) {
        if (initialRoute == "workout_selection") {
            navController.navigate("WorkoutSelector")
        }
    }

    LaunchedEffect(totalWorkouts) {

        badgeViewModel.syncTotalWorkouts(totalWorkouts)
    }
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = theme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(300, easing = EaseOutQuart)
                ) + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 400f
                    ),
                    transformOrigin = TransformOrigin.Center
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(250, easing = EaseInCubic)
                ) + scaleOut(
                    targetScale = 1.08f,
                    animationSpec = tween(250, easing = EaseInCubic),
                    transformOrigin = TransformOrigin.Center
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(300, easing = EaseOutQuart)
                ) + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 400f
                    ),
                    transformOrigin = TransformOrigin.Center
                )
            },
            popExitTransition = {
                fadeOut(
                    animationSpec = tween(250, easing = EaseInCubic)
                ) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(250, easing = EaseInCubic),
                    transformOrigin = TransformOrigin.Center
                )
            }
        ) {
            composable("HomeScreen") {
                WorkoutListScreen(viewModel, navController = navController, viewModel2)
            }
            composable("onboarding") {
                OnboardingScreen { age, height, weight, name, experience, preferredStyle, importantMuscles ->
                    onboardingManager.setFirstTimeLaunch(false)
                    userPreferencesManager.saveUserData(
                        name = name,
                        age = age,
                        height = height,
                        weight = weight,
                        experience = experience,
                        importantMuscles = importantMuscles,
                        preferredStyle = preferredStyle
                    )
                    navController.navigate("HomeScreen") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
            composable("Survey") {
                RecommendationSurveyScreen(navController = navController)
            }
            composable("RepPredictor"){
                RepPredictorScreen(navController = navController)
            }
            composable(
                route = "WorkoutSelector",
                enterTransition = {
                    if (initialState.destination.route == "HomeScreen") {
                        slideInHorizontally(
                            initialOffsetX = { it / 3 },
                            animationSpec = tween(400, easing = EaseOutQuart)
                        ) + fadeIn(animationSpec = tween(300))
                    } else {
                        fadeIn(animationSpec = tween(300, easing = EaseOutQuart)) +
                                scaleIn(
                                    initialScale = 0.90f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 380f
                                    ),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "HomeScreen") {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = EaseInCubic)
                        ) + fadeOut(animationSpec = tween(200))
                    } else {
                        fadeOut(animationSpec = tween(250, easing = EaseInCubic)) +
                                scaleOut(
                                    targetScale = 1.06f,
                                    animationSpec = tween(250, easing = EaseInCubic),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },


                ) {
                WorkoutSelector(viewModel, navController)
            }
            composable("WorkoutHistory",
                enterTransition = {

                    if (initialState.destination.route == "UserProfile") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },

                exitTransition = {
                    if (targetState.destination.route == "UserProfile") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(200, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) { WorkoutHistory(viewModel, navController) }
            composable(
                Routes.DetailedWorkoutRoute,
                arguments = listOf(navArgument(Routes.ArgId) { type = NavType.LongType })
            ) { WorkoutDetailScreen(navController, viewModel2, viewModel) }
            composable("UserProfile") { UserProfileScreen(navController, viewModel,badgeViewModel) }
            composable("PersonaSettings",
                enterTransition = {

                    if (initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
                // PersonaSettings
                exitTransition = {
                    if (targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) { PersonaSettingsScreen(navController = navController) }
            composable("EditUserStats",
                enterTransition = {
                    if (initialState.destination.route == "UserProfile" || initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
                // EditUserStats
                exitTransition = {
                    if (targetState.destination.route == "UserProfile" || targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) { EditUserStats(navController) }
            composable("MuscleGroup",


                ) { ProfileMuscleStatusRoute(navController, viewModel2, viewModel) }
            composable("Coaching") { CoachingRoute(navController, viewModel2, viewModel) }
            composable("CoachingGoals") { CoachingGoalsScreen(navController) }
            composable("RepMax",
                enterTransition = {
                    if (initialState.destination.route == "UserProfile") {
                        slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },

                        exitTransition = {
                    if (targetState.destination.route == "UserProfile") {
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) {
                OneRepMaxEstimator(
                    personalRecords = personalRecords,
                    navController = navController,

                )
            }
            composable("WeeklySummary") { WeeklySummaryScreen(
                navController = navController,
                tapeFlow = SummaryPDE.tapeFlow(),
                firstDayOfWeek = DayOfWeek.SUNDAY,
                zoneId = java.time.ZoneId.systemDefault()
            ) }
            composable("PerformanceOptions",
                enterTransition = {

                    if (initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
                // PerformanceOptions
                exitTransition = {
                    if (targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) {

                PerformanceOptionsScreen(navController = navController)
            }
            composable("MemoryMonitor",
                enterTransition = {
                    if (initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) {
                MemoryMonitorScreen(navController = navController)
            }
            composable("AppearanceScreen",
                enterTransition = {

                    if (initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },

                exitTransition = {
                    if (targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ){
                AppearanceScreen(
                    navController = navController
                )
            }
            composable("Settings") {
              SettingsScreen(navController = navController)
            }
            composable("HealthConnect",
                enterTransition = {

                    if (initialState.destination.route == "Settings") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },

                exitTransition = {
                    if (targetState.destination.route == "Settings") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(800, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },
            ) {
                HealthConnectScreen(navController = navController)
            }
            composable("badges",
                enterTransition = {

                    if (initialState.destination.route == "UserProfile") {
                        slideInHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            initialOffsetX = { it }
                        )
                    } else {
                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },

                exitTransition = {
                    if (targetState.destination.route == "UserProfile") {
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = LinearEasing),
                            targetOffsetX = { it }
                        )
                    } else {
                        fadeOut(animationSpec = fadeOutSpec) +
                                scaleOut(
                                    targetScale = 1.04f,
                                    animationSpec = tween(200, easing = LinearEasing),
                                    transformOrigin = TransformOrigin.Center
                                )
                    }
                },) {
                BadgesScreen(
                    navController = navController,
                    badgeViewModel = badgeViewModel
                )
            }



        }

        if (unlockQueue.isNotEmpty()) {
            val firstBadge = unlockQueue.first()
            key(firstBadge.id) {
                BadgeUnlockAnimation(
                    badgeTitle = firstBadge.title,
                    onAnimationFinished = {
                        badgeViewModel.onNotificationShown(firstBadge.id)
                        unlockQueue.removeAt(0)
                    }
                )
            }
        }

    }
}

private enum class ExerciseGlyphType {
    Press,
    Pull,
    Squat,
    Hinge,
    Arms,
    Core,
    Cardio,
    General
}

private fun exerciseGlyphTypeFor(name: String): ExerciseGlyphType {
    val normalized = name.lowercase()
    return when {
        normalized.contains("bench") || normalized.contains("press") || normalized.contains("push-up") || normalized.contains("push up") || normalized.contains("dip") || normalized.contains("fly") ->
            ExerciseGlyphType.Press
        normalized.contains("pull") || normalized.contains("row") || normalized.contains("pulldown") || normalized.contains("chin") ->
            ExerciseGlyphType.Pull
        normalized.contains("squat") || normalized.contains("lunge") || normalized.contains("step-up") || normalized.contains("step up") || normalized.contains("leg press") || normalized.contains("hack squat") ->
            ExerciseGlyphType.Squat
        normalized.contains("deadlift") || normalized.contains("rdl") || normalized.contains("romanian") || normalized.contains("good morning") || normalized.contains("hip thrust") || normalized.contains("glute bridge") || normalized.contains("swing") ->
            ExerciseGlyphType.Hinge
        normalized.contains("curl") || normalized.contains("extension") || normalized.contains("pushdown") || normalized.contains("kickback") || normalized.contains("raise") || normalized.contains("shrug") ->
            ExerciseGlyphType.Arms
        normalized.contains("plank") || normalized.contains("crunch") || normalized.contains("leg raise") || normalized.contains("twist") || normalized.contains("rollout") ->
            ExerciseGlyphType.Core
        normalized.contains("run") || normalized.contains("bike") || normalized.contains("swim") || normalized.contains("stair") || normalized.contains("jump") || normalized.contains("burpee") || normalized.contains("mountain climber") ->
            ExerciseGlyphType.Cardio
        else -> ExerciseGlyphType.General
    }
}

private fun primaryMuscleLabelFor(name: String): String {
    val normalized = name.lowercase()
    return when {
        normalized.contains("bench") || normalized.contains("push-up") || normalized.contains("push up") || normalized.contains("dip") || normalized.contains("fly") || normalized.contains("chest press") ->
            "Chest"
        normalized.contains("incline") || normalized.contains("overhead press") || normalized.contains("arnold press") || normalized.contains("lateral raise") || normalized.contains("front raise") || normalized.contains("shoulder press") ->
            "Shoulders"
        normalized.contains("pull") || normalized.contains("pulldown") || normalized.contains("row") || normalized.contains("chin") ->
            "Back"
        normalized.contains("curl") ->
            "Biceps"
        normalized.contains("extension") || normalized.contains("pushdown") || normalized.contains("kickback") || normalized.contains("skull crusher") ->
            "Triceps"
        normalized.contains("squat") || normalized.contains("leg press") || normalized.contains("leg extension") || normalized.contains("step-up") || normalized.contains("step up") ->
            "Quads"
        normalized.contains("deadlift") || normalized.contains("rdl") || normalized.contains("romanian") || normalized.contains("leg curl") || normalized.contains("good morning") ->
            "Hamstrings"
        normalized.contains("hip thrust") || normalized.contains("glute bridge") || normalized.contains("abduction") ->
            "Glutes"
        normalized.contains("calf") ->
            "Calves"
        normalized.contains("plank") || normalized.contains("crunch") || normalized.contains("leg raise") || normalized.contains("twist") || normalized.contains("rollout") ->
            "Core"
        normalized.contains("shrug") ->
            "Traps"
        normalized.contains("run") || normalized.contains("bike") || normalized.contains("swim") || normalized.contains("stair") || normalized.contains("jump") || normalized.contains("burpee") || normalized.contains("mountain climber") ->
            "Cardio"
        else -> "Full Body"
    }
}

private fun formatLastLoggedLabel(lastUsed: Long): String {
    if (lastUsed <= 0L) return "New"
    val relative = DateUtils.getRelativeTimeSpanString(
        lastUsed,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
    return relative
        .replace(" minutes ago", " mins ago")
        .replace(" minute ago", " min ago")
        .replace(" hours ago", " hrs ago")
        .replace(" hour ago", " hr ago")
}

@Composable
private fun ExerciseGlyphBadge(
    workoutName: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val glyphType = remember(workoutName) { exerciseGlyphTypeFor(workoutName) }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = min(size.width, size.height) * 0.10f
            val headRadius = size.minDimension * 0.12f
            val headCenter = Offset(size.width * 0.5f, size.height * 0.22f)
            val shoulderY = size.height * 0.38f
            val hipY = size.height * 0.58f
            val footY = size.height * 0.88f
            val color = Color.White.copy(alpha = 0.92f)

            fun limb(start: Offset, end: Offset) {
                drawLine(color = color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
            }

            drawCircle(color = color, radius = headRadius, center = headCenter)
            limb(Offset(size.width * 0.5f, headCenter.y + headRadius), Offset(size.width * 0.5f, hipY))

            when (glyphType) {
                ExerciseGlyphType.Press -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.24f, size.height * 0.34f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.76f, size.height * 0.34f))
                    limb(Offset(size.width * 0.18f, size.height * 0.30f), Offset(size.width * 0.82f, size.height * 0.30f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.34f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.66f, footY))
                }
                ExerciseGlyphType.Pull -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.26f, size.height * 0.46f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.74f, size.height * 0.46f))
                    limb(Offset(size.width * 0.18f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.18f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.36f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.64f, footY))
                }
                ExerciseGlyphType.Squat -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.20f, size.height * 0.38f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.80f, size.height * 0.38f))
                    limb(Offset(size.width * 0.18f, size.height * 0.34f), Offset(size.width * 0.82f, size.height * 0.34f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.32f, size.height * 0.74f))
                    limb(Offset(size.width * 0.32f, size.height * 0.74f), Offset(size.width * 0.22f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.68f, size.height * 0.74f))
                    limb(Offset(size.width * 0.68f, size.height * 0.74f), Offset(size.width * 0.78f, footY))
                }
                ExerciseGlyphType.Hinge -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.26f, size.height * 0.48f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.74f, size.height * 0.48f))
                    limb(Offset(size.width * 0.22f, size.height * 0.52f), Offset(size.width * 0.78f, size.height * 0.52f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.34f, size.height * 0.80f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.66f, size.height * 0.80f))
                }
                ExerciseGlyphType.Arms -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.28f, size.height * 0.44f))
                    limb(Offset(size.width * 0.28f, size.height * 0.44f), Offset(size.width * 0.38f, size.height * 0.28f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.72f, size.height * 0.44f))
                    limb(Offset(size.width * 0.72f, size.height * 0.44f), Offset(size.width * 0.62f, size.height * 0.28f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.38f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.62f, footY))
                }
                ExerciseGlyphType.Core -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.28f, size.height * 0.50f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.72f, size.height * 0.50f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.24f, size.height * 0.82f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.76f, size.height * 0.82f))
                }
                ExerciseGlyphType.Cardio -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.28f, size.height * 0.44f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.72f, size.height * 0.34f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.34f, size.height * 0.76f))
                    limb(Offset(size.width * 0.34f, size.height * 0.76f), Offset(size.width * 0.20f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.72f, size.height * 0.68f))
                    limb(Offset(size.width * 0.72f, size.height * 0.68f), Offset(size.width * 0.84f, size.height * 0.56f))
                }
                ExerciseGlyphType.General -> {
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.28f, size.height * 0.46f))
                    limb(Offset(size.width * 0.5f, shoulderY), Offset(size.width * 0.72f, size.height * 0.46f))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.36f, footY))
                    limb(Offset(size.width * 0.5f, hipY), Offset(size.width * 0.64f, footY))
                }
            }
        }
    }
}

@Stable
    class ColdStartStages {
        var afterFirstFrame by mutableStateOf(false)
        var after100ms by mutableStateOf(false)
        var after200ms by mutableStateOf(false)
        var after400ms by mutableStateOf(false)
        var after600ms by mutableStateOf(false)
        var after700ms by mutableStateOf(false)

        var after800ms by mutableStateOf(false)
    }

    @Composable
    fun rememberColdStartStages(): ColdStartStages {
        val s = remember { ColdStartStages() }
        LaunchedEffect(Unit) {
            withFrameNanos { }          // let first frame present
            s.afterFirstFrame = true
            delay(100)
            s.after100ms = true
            delay(100)
            s.after200ms = true
            delay(200)
            s.after400ms = true
            delay(200)
            s.after600ms = true
            delay(100)
            s.after700ms = true
            delay(100)
            s.after800ms = true
        }
        return s
    }
@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    viewModel: WorkoutListViewModel,
    navController: NavController,
    viewModel2: MainScreenViewModel,
) {

    val context = LocalContext.current

    // Theme Subscription
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val blurEnabled = performanceOptions.blurEnabled
    val maxSuggestions = performanceOptions.maxSuggestions
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestName by viewModel2.latestWorkoutName.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val stages = rememberColdStartStages()
    var isExpanded by remember { mutableStateOf(false) }
    val animationDuration = 300
    val cornerRadius by animateDpAsState(targetValue = if (isExpanded) 0.dp else 32.dp, animationSpec = tween(durationMillis = animationDuration), label = "fabCornerRadius")
    val iconAlpha by animateFloatAsState(targetValue = if (isExpanded) 0f else 1f, animationSpec = tween(durationMillis = animationDuration / 2), label = "fabIconAlpha")
    val shouldAnimateBackdrop = stages.afterFirstFrame && movingEffectsEnabled
    val showIntroState = remember { mutableStateOf(true) }
    val showIntro by showIntroState
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(700, easing = LinearEasing), label = "introProgress")

    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) { Brush.horizontalGradient(colors = introColors) }

    LaunchedEffect(Unit) { showIntroState.value = false ;
        Firebase.crashlytics.setCustomKey("current_screen", "HomeScreen")}
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(animationDuration.toLong())
            navController.navigate("WorkoutSelector")
            delay(200)
            isExpanded = false
        }
    }
    val analytics = Firebase.analytics
    LaunchedEffect(Unit) {
        analytics.logEvent("opened_home") { param("source", "cold_start") }
        readPersona(dynamicModel.personaConfig.value.mode)
    }
    val workoutPremadeRandom = remember { workoutPresets.shuffled().take(3).map { it.name } }
    fun NavController.openWorkout(id: Long) { navigate("${Routes.DetailedWorkout}/$id") }
    val aiEnabled = dynamicModel.personaConfig.value.enabled
    val prefsManager = remember { UserPreferencesManager(context) }
    val userWeight = remember { prefsManager.getWeight() }
    val userExperience = remember { prefsManager.getExperience() }
    val userName = remember { prefsManager.getName() }
    val userPreferredStyle = remember { prefsManager.getPreferredStyle() }

    val (personalRecords, recentWorkouts) = remember(uiState) {
        if (uiState is WorkoutListUiState.Success) {
            val workouts = (uiState as WorkoutListUiState.Success).workouts
            val prs = workouts.asSequence().filter { (it.weight ?: 0.0) > 0.0 }.groupBy { it.name }.map { (name, list) -> PersonalRecord(name, list.maxOf { it.weight!! }) }.sortedByDescending { it.maxWeight }
            val recent = workouts.take(5)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
    }
    val allWorkouts = remember(uiState) {
        (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
    }
    // Coaching link: share the activity-scoped CoachingViewModel so the home advice
    // card can surface the plan/goal the user built on the Coaching screen, with live
    // per-muscle progress derived from the same engine.
    val coachVm = rememberCoachingViewModel()
    val coachingInsights by coachVm.insights.collectAsState()
    LaunchedEffect(allWorkouts.size) {
        if (allWorkouts.isNotEmpty()) coachVm.computeSignals(context, allWorkouts)
    }
    val coachingPromptContext = remember(coachingInsights) {
        if (coachingInsights.isEmpty) ""
        else buildString {
            append(" The athlete is actively following an AI coaching plan — base your advice on it. ")
            coachingInsights.scheduleLines.firstOrNull()?.let { append("$it. ") }
            coachingInsights.goalLines.firstOrNull()?.let { append("$it. ") }
            if (coachingInsights.progressLines.isNotEmpty()) {
                append("Muscle progress vs targets: ${coachingInsights.progressLines.joinToString("; ")}. ")
            }
            if (coachingInsights.exerciseLines.isNotEmpty()) {
                append("Today's planned exercises: ${coachingInsights.exerciseLines.joinToString("; ")}. ")
            }
            append("Speak to their schedule, their muscle goal, their per-muscle progress, or today's exercises directly.")
        }
    }
    val latestWorkout = latestName ?: recentWorkouts.firstOrNull()
    val previousSameWorkout = remember(latestWorkout, allWorkouts) {
        latestWorkout?.let { workout ->
            allWorkouts
                .asSequence()
                .filter { it.id != workout.id && it.name == workout.name }
                .sortedByDescending { it.date }
                .firstOrNull()
        }
    }
    val latestWorkoutName = latestWorkout?.name?.trim().orEmpty()
    val latestWeight = latestWorkout?.weight ?: 0.0
    val previousWeight = previousSameWorkout?.weight ?: 0.0
    val latestReps = latestWorkout?.reps ?: 0
    val previousReps = previousSameWorkout?.reps ?: 0
    val latestRpe = latestWorkout?.sessionRpe ?: latestWorkout?.rpe ?: 0
    val latestFatigue = latestWorkout?.fatigueLevel ?: 0
    val workoutDirection = remember(
        latestWorkoutName,
        previousSameWorkout?.id,
        latestWeight,
        previousWeight,
        latestReps,
        previousReps,
        latestRpe,
        latestFatigue
    ) {
        when {
            latestWorkoutName.isBlank() -> "baseline"
            previousSameWorkout == null -> "baseline"
            latestFatigue >= 7 || latestRpe >= 8 -> "back_off"
            latestWeight > previousWeight || latestReps > previousReps -> "load_up"
            latestWeight < previousWeight || latestReps < previousReps -> "back_off"
            else -> "maintain"
        }
    }
    val patternSuggestion = remember(allWorkouts, latestWorkout?.id, latestWorkout?.date) {
        buildWorkoutPatternSuggestion(allWorkouts, latestWorkout)
    }
    val preferredStyleLabel = remember(userPreferredStyle) {
        when (userPreferredStyle) {
            "Both" -> "weights and cardio"
            else -> userPreferredStyle.lowercase()
        }
    }
    val fitnessContext = remember(
        latestWorkoutName,
        workoutDirection,
        userName,
        userExperience,
        userPreferredStyle,
        userWeight,
        patternSuggestion?.workoutName,
        patternSuggestion?.avgIntensity,
        patternSuggestion?.avgRestSeconds
    ) {
        buildString {
            append("You are the home-screen fitness coach. ")
            append("Speak like a real person in natural language, not like a robot. ")
            append("Mention the workout name directly. ")
            append("Keep the response conversational, short, and easy to read. ")
            append("Return 7 distinct coaching lines, each one sentence, each under 12 words. ")
            append("Do not add a preamble, disclaimer, or markdown. ")
            append("Do not contradict yourself inside the same batch. ")
            append("Follow the latest trend and learned memory for this workout over generic progression advice. ")
            append("If the latest trend is load_up, only recommend harder work when the current stats still look clean. ")
            append("If fatigue rises, RPE climbs, reps fall, intensity drops, or recovery looks worse, back off instead of forcing heavier load. ")
            append("If the latest trend is back_off or deload, do not recommend adding weight. ")
            append("If the latest trend is maintain, keep the advice steady and avoid sudden direction changes. ")
            append("Workout name: ")
            append(latestWorkoutName.ifBlank { "latest workout" })
            append(". ")
            append("Workout direction: ")
            append(workoutDirection)
            append(". ")
            append("Athlete: ")
            append(userName.ifBlank { "User" })
            append(", experience ")
            append(userExperience.ifBlank { "unknown" })
            append(", prefers ")
            append(preferredStyleLabel)
            append(", bodyweight ")
            append(userWeight.ifBlank { "-" })
            append(" kg.")
            append(" ")
            append(
                patternSuggestion?.promptSummary
                    ?: "Still learning the user's preferred in-workout intensity and workout rotation."
            )
        }
    }
    val homeAdviceState = useGeminiAdviceGenerator(contextPrompt = fitnessContext)
    val homeAdviceFallback = remember(latestWorkout, previousSameWorkout, patternSuggestion?.workoutName) {
        when {
            latestWorkout == null -> "Log your next session to get a quick AI cue."
            patternSuggestion != null ->
                "${patternSuggestion.workoutName} fits your current rhythm. Match your usual pace."
            previousSameWorkout != null && (latestWorkout.weight ?: 0.0) > (previousSameWorkout.weight ?: 0.0) ->
                "Last session improved. Keep momentum but stay crisp."
            previousSameWorkout != null && (latestWorkout.weight ?: 0.0) < (previousSameWorkout.weight ?: 0.0) ->
                "Ease back in and clean up form before pushing load."
            else -> "Use your last workout to guide your next move."
        }
    }
    LaunchedEffect(aiEnabled, latestWorkout?.id, latestWorkout?.date, previousSameWorkout?.id, coachingPromptContext) {
        val workout = latestWorkout ?: return@LaunchedEffect
        if (!aiEnabled) return@LaunchedEffect

        val durationMinutes = ((workout.durationMillis ?: 0L) / 60_000L).coerceAtLeast(0L)
        val useNextWorkoutAngle = (((workout.id) + (workout.date / DateUtils.DAY_IN_MILLIS).toInt()) and 1) == 0
        val directionLine = when (workoutDirection) {
            "load_up" -> "The latest trend still supports progression."
            "back_off" -> "The latest trend says to ease off or deload."
            "maintain" -> "The latest trend says to hold steady."
            else -> "Treat this as a baseline session."
        }
        val comparisonLine = previousSameWorkout?.let { previous ->
            val weightDelta = ((workout.weight ?: 0.0) - (previous.weight ?: 0.0))
            val repsDelta = (workout.reps ?: 0) - (previous.reps ?: 0)
            "Previous same workout: ${previous.weight ?: 0.0} kg, ${previous.reps ?: 0} reps. Delta: ${String.format("%.1f", weightDelta)} kg, ${repsDelta} reps."
        } ?: "No previous matching workout yet."
        val angleInstruction = if (useNextWorkoutAngle && patternSuggestion != null) {
            "Focus this batch on the next likely workout and how the user usually performs it. Mention the next workout suggestion directly."
        } else {
            "Focus this batch on the latest workout performance and what it means right now."
        }

        homeAdviceState.generateBatch(
            "Workout: ${workout.name}. Speak naturally and mention the workout name once. Do not sound robotic. $angleInstruction",
            "Latest performance: ${workout.weight ?: 0.0} kg, ${workout.sets ?: 0} sets, ${workout.reps ?: 0} reps, ${workout.distance ?: 0.0} km, ${durationMinutes} min, RPE ${workout.sessionRpe ?: workout.rpe ?: 0}, fatigue ${workout.fatigueLevel ?: 0}.",
            "Profile: ${userExperience.ifBlank { "unknown experience" }}, prefers $preferredStyleLabel, bodyweight ${userWeight.ifBlank { "-" }} kg. $comparisonLine $directionLine. ${patternSuggestion?.promptSummary ?: "No clear pattern recommendation yet."} Do not reverse the advice direction unless the data clearly changes.$coachingPromptContext"
        )
    }
    val scope = rememberCoroutineScope()
    val usageTracker = remember { PresetUsageTracker(context) }
    val usageMap by usageTracker.usageFlow.collectAsState(initial = emptyMap())
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    // Dynamic Border Brush
    val borderBrushStatic = remember(theme) {
        Brush.linearGradient(
            colors = listOf(
                theme.secondary.copy(alpha = 0.92f),
                theme.primary.copy(alpha = 0.10f)
            )
        )
    }

    val blurLength = length.value.toInt()
    val blurIntro by animateDpAsState(
        if (showIntro && blurEnabled) 32.dp else 0.dp,
        animationSpec = tween(durationMillis = blurLength, easing = LinearEasing),
        label = "blurIntro"
    )
    val density = LocalDensity.current


    val blurRadiusPx = remember(blurIntro, blurEnabled, density) {
        if (!blurEnabled) {
            0f
        } else {
            with(density) { blurIntro.toPx() }
                .takeIf { it.isFinite() && it >= 0f }

                ?.coerceAtMost(80f)
                ?: 0f
        }
    }


    val isLite = remember(movingEffectsEnabled, stages.after600ms) { !movingEffectsEnabled || !stages.after600ms }
    WorkoutTrackerTheme {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        renderEffect = if (blurRadiusPx > 0f) {
                            createBlurEffect(
                                blurRadiusPx,
                                blurRadiusPx,
                                Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        } else {
                            null
                        }
                    }
            ) {
                AnimatedBackdrop(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(paddingValues),
                    introBrush = introBrush,
                    introAlpha = 1f - introProgress,
                    enableWaves = stages.after600ms && shouldAnimateBackdrop,
                    enableAnimation = stages.after600ms && shouldAnimateBackdrop,
                    showSmallOrbs = true
                )
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val currentState = uiState) {
                        is WorkoutListUiState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = theme.primary.copy(alpha = 0.9f),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                        is WorkoutListUiState.Success -> {
                            Spacer(Modifier.height(12.dp))
                            val cardShape20 = remember { RoundedCornerShape(20.dp) }
                            fun formatTime(ms: Long): String {
                                val hours = ms / (1000 * 60 * 60)
                                val minutes = (ms / (1000 * 60)) % 60
                                val seconds = (ms / 1000) % 60
                                return String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            }
                            val cardioExerciseNames = remember {
                                listOf("Running (Treadmill)", "Stair Climber", "Elliptical Trainer", "Rowing Machine", "Stationary Bike", "Swimming")
                            }
                            val haptics = LocalHapticFeedback.current
                            val interactionSource = remember { MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val pressScale by animateFloatAsState(
                                targetValue = if (pressed) 0.975f else 1f,
                                animationSpec = tween(120, easing = FastOutSlowInEasing),
                                label = "cardPressScale"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .graphicsLayer {
                                        scaleX = pressScale
                                        scaleY = pressScale
                                    }
                                    .border(width = 1.dp, brush = borderBrushStatic, shape = cardShape20)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                shape = cardShape20,
                                colors = CardDefaults.cardColors(containerColor = surface.copy(alpha = 0.3f))
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    val workoutForCard = latestWorkout
                                    val time = formatTime(workoutForCard?.durationMillis ?: 0L)
                                    AdviceSectionUser(
                                        advice = homeAdviceState.currentAdvice.ifBlank { homeAdviceFallback },
                                        isAdviceLoading = aiEnabled && homeAdviceState.isLoading && !homeAdviceState.hasAdvice,
                                        modifier = Modifier.fillMaxSize(),
                                        lastWorkoutName = workoutForCard?.name ?: "No workouts yet.",
                                        extraLines = if (workoutForCard?.name in cardioExerciseNames) {
                                            buildList {
                                                add("Distance: ${workoutForCard?.distance ?: 0.0} km")
                                                add("Time: $time")
                                            }
                                        } else {
                                            buildList {
                                                add("Weight: ${workoutForCard?.weight ?: 0.0} kg")
                                                add("Sets/Reps: ${workoutForCard?.sets ?: 0} x ${workoutForCard?.reps ?: 0}")
                                                add("Time: $time")
                                                if (workoutForCard?.intensityScore != null) {
                                                    add("Intensity: ${workoutForCard.intensityScore}/10")
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            QuickStartWorkout(navController = navController)

                            // Dynamic Divider Brush
                            val dividerBrush = remember(theme) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        theme.primary,
                                        Color.White.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(brush = dividerBrush)
                            )
                            val favoritePresets by FavoritePresetStore.flow(context).collectAsState(initial = emptySet())
                            val topPresets: List<Pair<String, UsageStat>> by remember(usageMap, workoutPremadeRandom, favoritePresets) {
                                derivedStateOf {
                                    val favoritesList = favoritePresets.map { it to (usageMap[it] ?: UsageStat(0, 0L)) }
                                    val remainingSlots = maxSuggestions - favoritesList.size
                                    if (remainingSlots <= 0) {
                                        favoritesList.take(maxSuggestions)
                                    } else {
                                        val now = System.currentTimeMillis()
                                        val maxCount = (usageMap.values.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                                        fun recencyScore(ts: Long): Float {
                                            if (ts <= 0L) return 0f
                                            val days = (now - ts).coerceAtLeast(0L) / 86_400_000f
                                            return 1f / (1f + days)
                                        }
                                        val suggested = usageMap.entries
                                            .filter { it.key !in favoritePresets }
                                            .sortedWith(
                                                compareByDescending<Map.Entry<String, UsageStat>> {
                                                    val c = it.value.count.toFloat() / maxCount
                                                    val r = recencyScore(it.value.lastUsed)
                                                    c * 0.6f + r * 0.4f
                                                }
                                                    .thenByDescending { it.value.lastUsed }
                                                    .thenBy { it.key.lowercase() }
                                            )
                                            .take(remainingSlots)
                                            .map { it.key to it.value }
                                        val combined = favoritesList + suggested
                                        if (combined.isEmpty()) {
                                            workoutPremadeRandom.take(maxSuggestions).map { it to UsageStat(0, 0L) }
                                        } else {
                                            combined
                                        }
                                    }
                                }
                            }
                            val cardShape16 = remember { RoundedCornerShape(16.dp) }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(topPresets, key = { _, it -> it.first }, contentType = { _, _ -> "preset" }) { index, (workoutName, useCount) ->
                                    val haptics = LocalHapticFeedback.current
                                    val scope = rememberCoroutineScope()
                                    var isLaunching by remember { mutableStateOf(false) }


                                    val entranceAlpha = remember { Animatable(0f) }
                                    val entranceSlide = remember { Animatable(40f) }


                                    val launchProgress = animateFloatAsState(
                                        targetValue = if (isLaunching) 1f else 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.6f),
                                        label = "launchProgress"
                                    )

                                    LaunchedEffect(Unit) {
                                        delay(index * 35L)
                                        launch { entranceAlpha.animateTo(1f, tween(400)) }
                                        launch { entranceSlide.animateTo(0f, spring(0.75f, 200f)) }
                                    }

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    val rowGradient = remember(theme, isLaunching) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                theme.primary.copy(alpha = if (isLaunching) 0.18f else 0.12f),
                                                theme.secondary.copy(alpha = if (isLaunching) 0.14f else 0.08f),
                                                surface.copy(alpha = 0.10f)
                                            )
                                        )
                                    }

                                    val pressScale by animateFloatAsState(
                                        targetValue = if (pressed) 0.96f else 1f,
                                        animationSpec = tween(100),
                                        label = "pressScale"
                                    )

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(82.dp)
                                            .graphicsLayer {

                                                alpha = entranceAlpha.value
                                                translationY = entranceSlide.value


                                                val finalScale = pressScale + (launchProgress.value * 0.08f)
                                                scaleX = finalScale
                                                scaleY = finalScale
                                            }
                                            .drawBehind {

                                                if (launchProgress.value > 0.01f) {
                                                    val glowAlpha = (launchProgress.value * 0.4f)
                                                    drawRoundRect(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(theme.primary.copy(alpha = glowAlpha), Color.Transparent),
                                                            center = center,
                                                            radius = size.width * 1.2f
                                                        ),
                                                        size = size,
                                                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                                    )
                                                }
                                            }
                                            .border(
                                                width = 1.dp + (launchProgress.value.dp * 1.5f),
                                                brush = if (isLaunching) borderBrushStatic else borderBrushStatic,
                                                shape = cardShape16
                                            )
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE && !isLaunching) {
                                                    isLaunching = true
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                                    workout.value = workoutName
                                                    scope.launch { usageTracker.increment(workout.value) }
                                                    scope.launch {

                                                        delay(180)
                                                        val intent = Intent(context, WorkoutActivity::class.java).apply {
                                                            putExtra("WORKOUT_NAME", workoutName)
                                                        }
                                                        startActivity(context, intent, null)


                                                        delay(300)
                                                        isLaunching = false
                                                    }
                                                }
                                            },
                                        shape = cardShape16,
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Transparent
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(rowGradient)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
//                                                    ExerciseGlyphBadge(
//                                                        workoutName = workoutName,
//                                                        accent = theme.primary
//                                                    )
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = workoutName,
                                                            fontSize = 18.sp,
                                                            color = onSurface,
                                                            fontWeight = if (isLaunching) FontWeight.Bold else FontWeight.Normal,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = primaryMuscleLabelFor(workoutName),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(
                                                                        Brush.horizontalGradient(
                                                                            listOf(
                                                                                theme.primary.copy(alpha = 0.22f),
                                                                                theme.primary.copy(alpha = 0.10f)
                                                                            )
                                                                        )
                                                                    )
                                                                    .border(
                                                                        width = 0.75.dp,
                                                                        color = Color.White.copy(alpha = 0.12f),
                                                                        shape = RoundedCornerShape(10.dp)
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                            Text(
                                                                text = formatLastLoggedLabel(useCount.lastUsed),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = Color.White.copy(alpha = 0.92f),
                                                                maxLines = 1,
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(
                                                                        Brush.horizontalGradient(
                                                                            listOf(
                                                                                Color.White.copy(alpha = 0.10f),
                                                                                theme.secondary.copy(alpha = 0.08f)
                                                                            )
                                                                        )
                                                                    )
                                                                    .border(
                                                                        width = 0.75.dp,
                                                                        color = Color.White.copy(alpha = 0.10f),
                                                                        shape = RoundedCornerShape(10.dp)
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                val isFavorite = workoutName in favoritePresets
                                                if (isFavorite || useCount.count > 0) {
                                                    val chipBackground = if (isFavorite) {
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                theme.primary.copy(alpha = 0.6f),
                                                                theme.secondary.copy(alpha = 0.35f)
                                                            )
                                                        )
                                                    } else {
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                theme.primary.copy(alpha = 0.18f),
                                                                theme.primary.copy(alpha = 0.10f)
                                                            )
                                                        )
                                                    }
                                                    val chipBorderColor = if (isFavorite) Color.White.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.12f)
                                                    Text(
                                                        text = if (isFavorite) "★ Favorite" else "Suggested",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        modifier = Modifier
                                                            .graphicsLayer {
                                                                alpha = if (isFavorite) 0.92f + (launchProgress.value * 0.08f) else 0.62f + (launchProgress.value * 0.2f)
                                                                scaleX = 1f + (launchProgress.value * if (isFavorite) 0.14f else 0.08f)
                                                                scaleY = 1f + (launchProgress.value * if (isFavorite) 0.14f else 0.08f)
                                                            }
                                                            .drawWithCache {
                                                                val glowBrush = Brush.radialGradient(
                                                                    colors = if (isFavorite) {
                                                                        listOf(
                                                                            theme.primary.copy(alpha = 0.50f),
                                                                            theme.secondary.copy(alpha = 0.28f),
                                                                            Color.Transparent
                                                                        )
                                                                    } else {
                                                                        listOf(
                                                                            theme.primary.copy(alpha = 0.10f),
                                                                            Color.Transparent
                                                                        )
                                                                    },
                                                                    center = Offset(size.width / 2f, size.height / 2f),
                                                                    radius = size.maxDimension * if (isFavorite) 1.45f else 1.1f
                                                                )
                                                                onDrawBehind {
                                                                    drawRoundRect(
                                                                        brush = glowBrush,
                                                                        topLeft = Offset.Zero,
                                                                        size = size,
                                                                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                                                                    )
                                                                }
                                                            }
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(chipBackground)
                                                            .border(
                                                                width = if (isFavorite) 1.dp else 0.75.dp,
                                                                color = chipBorderColor,
                                                                shape = RoundedCornerShape(10.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(brush = dividerBrush)
                            )
                            if (stages.after600ms && currentState.workouts.isNotEmpty()) {
                                val WRs = if (
                                    maxSuggestions <= 3
                                ) 2 else 1
                                WeightHistoryGraph(
                                    recentWorkouts = currentState.workouts.take(WRs),
                                    allWorkouts = currentState.workouts
                                )
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                        is WorkoutListUiState.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                            }
                        }
                    }
                }

                    FloatingTaskbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        navController = navController,
                        cornerRadius = cornerRadius,
                        iconAlpha = iconAlpha,
                        uiState = uiState
                    )

            }
        }
    }
}


@Composable
fun LowCostBackdrop(
    modifier: Modifier = Modifier,
    introBrush: Brush,
    introAlpha: Float
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .drawWithCache {
                onDrawBehind {
                    drawRect(introBrush, alpha = introAlpha.coerceIn(0f, 1f))
                }
            }
    )
}





@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
    )
}

enum class Trend { UP, DOWN, FLAT, NONE }
object taskbarOverride {
        var shouldOverrideVisiblity = mutableStateOf(
            false
        )
    }
