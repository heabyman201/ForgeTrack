package com.forgecompose.workouttracker



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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.PersonaPrefs.readPersona
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
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
import kotlin.math.roundToInt
import kotlin.system.exitProcess

var startDestination = "home"

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
            badgeStorage = InMemoryBadgeStorage()
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

                // Handle widget intents
                val initialRoute = remember { 
                    intent?.getStringExtra("navigation_route") 
                }

                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* no-op for now */ }

                var showNotifDialog by remember { mutableStateOf(false) }

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
    val onboardingManager = remember { OnboardingManager(context) }
    val userPreferencesManager = remember { UserPreferencesManager(context) }

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

    LaunchedEffect(initialRoute) {
        if (initialRoute == "workout_selection") {
            navController.navigate("WorkoutSelector")
        }
    }

    LaunchedEffect(totalWorkouts) {

        badgeViewModel.syncTotalWorkouts(totalWorkouts)
    }
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = theme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn(animationSpec = fadeInSpec) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                            transformOrigin = TransformOrigin.Center
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = fadeOutSpec) +
                        scaleOut(
                            targetScale = 1.04f,
                            animationSpec = tween(800, easing = LinearEasing),
                            transformOrigin = TransformOrigin.Center
                        )
            },
            popEnterTransition = {
                fadeIn(animationSpec = fadeInSpec) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                            transformOrigin = TransformOrigin.Center
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = fadeOutSpec) +
                        scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(800, easing = LinearEasing),
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
            composable(
                route = "WorkoutSelector",
                enterTransition = {

                    if (initialState.destination.route == "HomeScreen") {
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
                // WorkoutSelector
                exitTransition = {
                    if (targetState.destination.route == "HomeScreen") {
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
    val theme = appearanceOptions.selectedTheme.colors

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val blurEnabled = performanceOptions.blurEnabled
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
    val fitnessContext = "You are a fitness coach. The user provides their weight, last workout, and weight of the workout(Weights are in kg). Give them advice on what to do next. Call them by their name., Max 20 words"
    val prefsManager = remember { UserPreferencesManager(context) }
    val userWeight = remember { prefsManager.getWeight() }
    val userAge = remember { prefsManager.getAge() }
    val userHeight = remember { prefsManager.getHeight() }
    val userExperience = remember { prefsManager.getExperience() }
    val userName = remember { prefsManager.getName() }
    var userPreferredStyle = remember { prefsManager.getPreferredStyle() }
    val userImportantMuscles = remember { prefsManager.getImportantMuscles() }

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
    fun setPrefStyle() {
        when (userPreferredStyle) {
            "Weights" -> { userPreferredStyle = "Weights" }
            "Cardio" -> { userPreferredStyle = "Cardio" }
            "Both" -> { userPreferredStyle = "Weights and cardio" }
        }
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
            modifier = Modifier.fillMaxSize()
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
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedBackdrop(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(paddingValues),
                    introBrush = introBrush,
                    introAlpha = 1f - introProgress,
                    enableWaves = stages.after600ms && shouldAnimateBackdrop,
                    enableAnimation = stages.after600ms && shouldAnimateBackdrop
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
                            Spacer(Modifier.height(32.dp))
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
                                    val time = formatTime(latestName?.durationMillis?.toLong() ?: 0)
                                    AdviceSectionUser(
                                        advice = "advice",
                                        modifier = Modifier.fillMaxSize(),
                                        lastWorkoutName = latestName?.name ?: "No workouts yet.",
                                        extraLines = if (latestName?.name in cardioExerciseNames) {
                                            listOf("Distance : ${latestName?.distance} Km", "Time : $time")
                                        } else {
                                            listOf("Weight : ${latestName?.weight ?: "0"} Kg", "Time : $time")
                                        },
                                        navController = navController
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
                                    val remainingSlots = 3 - favoritesList.size
                                    if (remainingSlots <= 0) {
                                        favoritesList.take(3)
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
                                            workoutPremadeRandom.take(3).map { it to UsageStat(0, 0L) }
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
                                items(topPresets, key = { it.first }, contentType = { "preset" }) { (workoutName, useCount) ->
                                    val haptics = LocalHapticFeedback.current
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    val pressScale by animateFloatAsState(
                                        targetValue = if (pressed) 0.975f else 1f,
                                        animationSpec = tween(120, easing = FastOutSlowInEasing),
                                        label = "presetPressScale"
                                    )

                                    var expanded by remember { mutableStateOf(false) }
                                    val targetHeight by animateDpAsState(
                                        targetValue = if (expanded) 140.dp else 65.dp,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "presetExpandHeight"
                                    )

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(targetHeight)
                                            .graphicsLayer {
                                                scaleX = pressScale
                                                scaleY = pressScale
                                            }
                                            .border(width = 1.dp, brush = borderBrushStatic, shape = cardShape16)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {

                                                if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                    workout.value = workoutName
                                                    val intent = Intent(context, WorkoutActivity::class.java).apply {
                                                        putExtra("WORKOUT_NAME", workoutName)
                                                    }
                                                    startActivity(context, intent, null)
                                                }
                                            },
                                        shape = cardShape16,
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE)
                                                surface.copy(alpha = 0.22f)
                                            else Color.DarkGray
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateContentSize(
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = workoutName, fontSize = 18.sp, color = onSurface)
                                                    val isFavorite = workoutName in favoritePresets
                                                    val showChip = isFavorite || useCount.count > 0 || useCount.lastUsed > 0L
                                                    if (showChip) {
                                                        Text(
                                                            text = if (isFavorite) "Favorite" else "Suggested",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = onSurface.copy(alpha = 0.9f),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(theme.primary.copy(alpha = 0.25f)) // Theme tint
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
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
                                WeightHistoryGraph(workouts = currentState.workouts.take(2))
                            }
                        }
                        is WorkoutListUiState.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
                if (stages.after100ms) {
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
