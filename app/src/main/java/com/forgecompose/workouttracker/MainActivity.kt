package com.forgecompose.workouttracker



import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.forgecompose.workouttracker.ConnectedWorkout.WorkoutMode
import com.forgecompose.workouttracker.ConnectedWorkout.workout
import com.forgecompose.workouttracker.PersonaPrefs.readPersona
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.collections.emptyList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
private fun lastAndPrevSameName(workouts: List<Workout>): Pair<Workout?, Workout?> {
    val last = workouts.maxByOrNull { it.date } ?: return null to null
    val prevSame = workouts
        .asSequence()
        .filter { it.name == last.name && it.date < last.date }
        .maxByOrNull { it.date }
    return last to prevSame
}





fun maxWeightForName(workouts: List<Workout>, exerciseName: String): Double {
    return workouts.asSequence()
        .filter { it.name == exerciseName }
        .map { it.weight ?: 0.0 }
        .maxOrNull() ?: 0.0
}


val LocalHazeState = staticCompositionLocalOf<HazeState> {
    error("LocalHazeState not provided. Wrap your screen in ProvideHaze.")
}
var startDestination = "home"
class MainActivity : ComponentActivity() {
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var userPreferencesManager: UserPreferencesManager
    private val mainScreenViewModel: MainScreenViewModel by viewModels {
        val application = application as MyApplication
        MainScreenViewModelFactory(application.workoutRepository)
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

        val application = application as MyApplication
        val workoutRepository = application.workoutRepository
        val factory = WorkoutListViewModelFactory(workoutRepository)

        val workoutListViewModel: WorkoutListViewModel by viewModels { factory }
        val secondViewModel = MainScreenViewModel(workoutRepository)

        startDestination = if (onboardingManager.isFirstTimeLaunch()) {
            "onboarding"
        } else {
            "HomeScreen"
        }

        userPreferencesManager = UserPreferencesManager(this)

        setContent {
            WorkoutTrackerTheme {
                val context = LocalContext.current

                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {

                }

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
                    viewModel2 = secondViewModel
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
fun MainScreen(viewModel: WorkoutListViewModel, viewModel2: MainScreenViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }
    val userPreferencesManager = remember { UserPreferencesManager(context) }

    val slideSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF230C0C)
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

                        fadeIn(animationSpec = fadeInSpec) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
                                    transformOrigin = TransformOrigin.Center
                                )

                },
                // WorkoutHistory
                exitTransition = {
                    fadeOut(animationSpec = fadeOutSpec) +
                            scaleOut(
                                targetScale = 1.04f,
                                animationSpec = tween(800, easing = LinearEasing),
                                transformOrigin = TransformOrigin.Center
                            )
                },
            ) { WorkoutHistory(viewModel, navController) }
            composable(
                Routes.DetailedWorkoutRoute,
                arguments = listOf(navArgument(Routes.ArgId) { type = NavType.LongType })
            ) { WorkoutDetailScreen(navController, viewModel2, viewModel) }
            composable("UserProfile") { UserProfileScreen(navController, viewModel) }
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
                    navController = navController
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
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeApi::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun WorkoutListScreen(
    viewModel: WorkoutListViewModel,
    navController: NavController,
    viewModel2: MainScreenViewModel,
) {
    val context = LocalContext.current
    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestName by viewModel2.latestWorkoutName.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val stages = rememberColdStartStages()
    var isExpanded by remember { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val animationDuration = 300
    val cornerRadius by animateDpAsState(targetValue = if (isExpanded) 0.dp else 32.dp, animationSpec = tween(durationMillis = animationDuration), label = "fabCornerRadius")
    val iconAlpha by animateFloatAsState(targetValue = if (isExpanded) 0f else 1f, animationSpec = tween(durationMillis = animationDuration / 2), label = "fabIconAlpha")
    val shouldAnimate = stages.afterFirstFrame && !isExpanded
    var animationClock by remember { mutableStateOf(0f) }
    LaunchedEffect(shouldAnimate, movingEffectsEnabled) {
        if (shouldAnimate && movingEffectsEnabled) {
            val frameNs = 41_666_667L
            var nextTick = System.nanoTime() + frameNs
            while (true) {
                val now = System.nanoTime()
                if (now >= nextTick) {
                    animationClock += 1f / 24f
                    withFrameNanos { }
                    nextTick += frameNs
                    if (now - nextTick > frameNs * 4) nextTick = now + frameNs
                } else {
                    val sleepMs = ((nextTick - now) / 1_000_000L).coerceAtLeast(0L)
                    if (sleepMs > 0L) delay(sleepMs)
                }
            }
        }
    }
    fun q(v: Float, steps: Int = 24) = (v * steps).roundToInt() / steps.toFloat()
    val fullPi = 2f * PI.toFloat()
    val waveOffset = remember(animationClock) { (animationClock * fullPi / 22f) % fullPi }
    val pulseAlpha = remember(animationClock) { 0.25f + 0.10f * sin(animationClock * fullPi / 8f) }
    val glowIntensity = remember(animationClock) { 0.4f + 0.2f * sin(animationClock * fullPi / 6f) }
    val gradientProgress = remember(animationClock) { (animationClock / 15f) % 2f }
    val gradientOffset = remember(gradientProgress) { if (gradientProgress > 1f) 2f - gradientProgress else gradientProgress }
    val showIntroState = remember { mutableStateOf(true) }
    val showIntro by showIntroState
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(700, easing = LinearEasing), label = "introProgress")
    val hour = remember { LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) { Brush.linearGradient(colors = introColors, start = Offset(Float.POSITIVE_INFINITY, 0f), end = Offset.Zero) }
    LaunchedEffect(Unit) { showIntroState.value = false }
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
    var (advice, _, generateAdvice) = useGeminiAdviceGenerator(contextPrompt = fitnessContext)
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
    LaunchedEffect(userName, userAge, userWeight, userHeight, userExperience, personalRecords, recentWorkouts) {
        val healthConnectManager = HealthConnectManager(context)
        val tape: String = PDE.readTape()
        setPrefStyle()
        val healthDataSummary = if (healthConnectManager.hasAllPermissions()) {
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS)
            val summaryBuilder = StringBuilder("\n\nHealth data (last 30 days):\n")
            var dataFound = false
            val sleepSessions = healthConnectManager.readSleepSessions(startTime, endTime)
            if (sleepSessions.isNotEmpty()) {
                val averageSleepDurationHours = sleepSessions.map { Duration.between(it.startTime, it.endTime).toMinutes() }.average() / 60.0
                summaryBuilder.append("Average Sleep: %.1f h/night\n".format(averageSleepDurationHours))
                dataFound = true
            }
            val restingHeartRates = healthConnectManager.readRestingHeartRate(startTime, endTime)
            if (restingHeartRates.isNotEmpty()) {
                val averageRhr = restingHeartRates.map { it.beatsPerMinute }.average()
                summaryBuilder.append("Average Resting HR: %.0f bpm\n".format(averageRhr))
                dataFound = true
            }
            val bodyFatReadings = healthConnectManager.readBodyFat(startTime, endTime)
            bodyFatReadings.maxByOrNull { it.time }?.let {
                summaryBuilder.append("Latest Body Fat: %.1f%%\n".format(it.percentage.value))
                dataFound = true
            }
            val oxygenSaturationReadings = healthConnectManager.readOxygenSaturation(startTime, endTime)
            if (oxygenSaturationReadings.isNotEmpty()) {
                val averageSpo2 = oxygenSaturationReadings.map { it.percentage.value }.average()
                summaryBuilder.append("Average SpO₂: %.1f%%\n".format(averageSpo2))
                dataFound = true
            }
            if (dataFound) summaryBuilder.toString() else ""
        } else {
            ""
        }
        generateAdvice(
            """
Rules:
Respond instantly using given data only. No questions, no follow-ups, no assumptions.
Output ≤2 lines: 1 for performance summary, 1 for short focus recommendation.
""".trimIndent(),
            """
Profile:
$userName, $userAge y, $userWeight kg, $userHeight cm, $userExperience y exp
PRs: $personalRecords | Style: $userPreferredStyle | Focus: $userImportantMuscles
Recent: $recentWorkouts
$healthDataSummary
""".trimIndent(),
            """
Format:
Line 1 – Overall status (e.g. “Steady strength rise, mild recovery lag.”)
Line 2 – Next focus (e.g. “Prioritize sleep and progressive overload.”)
No extra text or interaction.
""".trimIndent()
        )
    }
    val haze = remember { HazeState() }
    val clampedGlow by remember(glowIntensity) { derivedStateOf { q(glowIntensity.coerceIn(0f, 1f), 18) } }
    val clampedPulse by remember(pulseAlpha) { derivedStateOf { q(pulseAlpha.coerceIn(0f, 1f), 18) } }
    val clampedGrad by remember(gradientOffset) { derivedStateOf { q(gradientOffset.coerceIn(0f, 1f), 18) } }
    val wavePath = remember { Path() }
    val particleSeed = remember { Random(42) }
    val particles = remember {
        List(12) { i ->
            val baseX = i / 12f
            val yOff = 0.15f + particleSeed.nextFloat() * 0.25f
            val r = 1.8f + particleSeed.nextFloat() * 2.0f
            Triple(baseX, yOff, r)
        }
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val dividerColorBase = remember(onSurface) { onSurface.copy(alpha = 0.2f) }
    val blurAnim by animateDpAsState(if (showIntro) intensity.value else 0.dp, animationSpec = tween(length.longValue.toInt()), label = "blur")
    WorkoutTrackerTheme {
        CompositionLocalProvider(LocalHazeState provides haze) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer{
                        renderEffect = RenderEffect.createBlurEffect(blurAnim.toPx(), blurAnim.toPx(), Shader.TileMode.CLAMP).asComposeRenderEffect()
                    }
                    .hazeSource(state = haze)
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .drawWithCache {
                                val bgBrush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF702727).copy(alpha = 0.85f + clampedGrad * 0.45f),
                                        Color(0xFF3A1515).copy(alpha = 0.7f + clampedGrad * 0.3f),
                                        Color(0xFF2A0D0D).copy(alpha = 0.8f + clampedGrad * 0.2f),
                                        Color(0xFF1A0808).copy(alpha = 0.9f + clampedGrad * 0.1f),
                                        Color(0xFF0D0404)
                                    ),
                                    radius = 1200f + (clampedGrad * 400f),
                                    center = Offset(0.3f + clampedGrad * 0.4f, 0.2f + clampedGrad * 0.3f)
                                )
                                onDrawBehind {
                                    drawRect(bgBrush)
                                    if (stages.after600ms && shouldAnimate && movingEffectsEnabled) {
                                        val baseAlpha = clampedPulse
                                        val g = clampedGlow
                                        val w = size.width
                                        val h = size.height
                                        val waveHeight = 90f
                                        for (layer in 0..2) {
                                            val layerOffset = waveOffset + (layer * PI.toFloat() / 4)
                                            val layerAlpha = baseAlpha * (0.25f + layer * 0.12f) * g
                                            val layerColor = when (layer) {
                                                0 -> Color(0xFF4A1A1A).copy(alpha = layerAlpha)
                                                1 -> Color(0xFF3A1515).copy(alpha = layerAlpha * 0.8f)
                                                else -> Color(0xFF2A0D0D).copy(alpha = layerAlpha * 0.6f)
                                            }
                                            wavePath.rewind()
                                            val baseY = h * (0.22f + layer * 0.16f)
                                            val steps = max(36, (w / 28f).roundToInt())
                                            val dx = w / steps
                                            var x = 0f
                                            wavePath.moveTo(0f, baseY + sin(layerOffset) * waveHeight * (0.55f + layer * 0.22f) * g)
                                            repeat(steps + 1) {
                                                val t = x / w
                                                val phase = t * 3f * PI.toFloat() + layerOffset
                                                val y = baseY + sin(phase) * waveHeight * (0.55f + layer * 0.22f) * g
                                                wavePath.lineTo(x, y)
                                                x += dx
                                            }
                                            wavePath.lineTo(w, h)
                                            wavePath.lineTo(0f, h)
                                            wavePath.close()
                                            drawPath(path = wavePath, color = layerColor)
                                        }
                                        val baseAlphaP = baseAlpha
                                        val gP = g
                                        particles.forEachIndexed { i, (baseX, yOff, r) ->
                                            val px = w * baseX + sin(waveOffset * 0.7f + i) * 60f * gP
                                            val py = h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                                            val a = baseAlphaP * (0.35f + sin(waveOffset + i) * 0.25f) * gP
                                            drawCircle(Color.White.copy(alpha = a), r, Offset(px, py))
                                        }
                                    }
                                    if (introProgress < 1f) {
                                        drawRect(introBrush, alpha = 1f - introProgress)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (val currentState = uiState) {
                            is WorkoutListUiState.Loading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f + clampedGlow * 0.2f), strokeWidth = 3.dp)
                                }
                            }
                            is WorkoutListUiState.Success -> {
                                if (currentState.workouts.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = 96.dp),
                                            verticalArrangement = Arrangement.Top
                                        ) {
                                            Spacer(Modifier.height(32.dp))
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val isPressed by interactionSource.collectIsPressedAsState()
                                            val scale by animateFloatAsState(targetValue = if (isPressed) 0.985f else 1f, label = "cardScale_empty")
                                            val cardShape20 = remember { RoundedCornerShape(20.dp) }
                                            val borderBrushMain by remember(clampedPulse, clampedGlow) {
                                                mutableStateOf(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF622121).copy(alpha = 0.86f + clampedPulse * 0.12f + clampedGlow * 0.12f),
                                                            Color.White.copy(alpha = 0.08f + clampedPulse * 0.06f + clampedGlow * 0.06f)
                                                        )
                                                    )
                                                )
                                            }
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .haze(state = haze)
                                                    .height(140.dp)
                                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                                    .border(1.dp, borderBrushMain, cardShape20)
                                                    .clickable(interactionSource = interactionSource, indication = null) {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                shape = cardShape20,
                                                colors = CardDefaults.cardColors(containerColor = surface.copy(alpha = 0.28f + clampedPulse * 0.08f + clampedGlow * 0.08f))
                                            ) {
                                                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.clickable {
                                                    navController.navigate(Routes.DetailedWorkout.replace("{${Routes.ArgId}}", "${latestName?.id}"))
                                                }) {
                                                    AdviceSectionUser(
                                                        advice = advice,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clickable {
                                                                navController.navigate(Routes.DetailedWorkoutRoute.replace("{${Routes.ArgId}}", "${latestName?.id}"))
                                                            },
                                                        lastWorkoutName = "No workouts yet.",
                                                        extraLines = listOf("Get started: Quick Start below", "Or choose a preset from the list"),
                                                        navController = navController
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            QuickStartWorkout(navController = navController)
                                            val dividerBrush = remember {
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF9B111E),
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
                                            val cardShape16 = remember { RoundedCornerShape(16.dp) }
                                            val suggested = remember(workoutPremadeRandom) { workoutPremadeRandom.take(3) }
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                suggested.forEach { workoutName ->
                                                    val itemInteraction = remember { MutableInteractionSource() }
                                                    val itemPressed by itemInteraction.collectIsPressedAsState()
                                                    val itemScale by animateFloatAsState(targetValue = if (itemPressed) 0.985f else 1f, label = "cardScaleItem_empty")
                                                    val borderBrushItem by remember(clampedPulse, clampedGlow) {
                                                        mutableStateOf(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFF622121).copy(alpha = 0.86f + clampedPulse * 0.12f + clampedGlow * 0.12f),
                                                                    Color.White.copy(alpha = 0.08f + clampedPulse * 0.06f + clampedGlow * 0.06f)
                                                                )
                                                            )
                                                        )
                                                    }
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(65.dp)
                                                            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                                                            .border(1.dp, borderBrushItem, cardShape16)
                                                            .clickable(interactionSource = itemInteraction, indication = null) {
                                                                if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    scope.launch { usageTracker.increment(workoutName) }
                                                                    workout.value = workoutName
                                                                    val intent = Intent(context, WorkoutActivity::class.java).apply { putExtra("WORKOUT_NAME", workoutName) }
                                                                    startActivity(context, intent, null)
                                                                }
                                                            },
                                                        shape = cardShape16,
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE)
                                                                surface.copy(alpha = 0.14f + clampedPulse * 0.05f + clampedGlow * 0.05f)
                                                            else Color.DarkGray
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 20.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(text = workoutName, fontSize = 18.sp, color = onSurface)
                                                                Text(
                                                                    text = "Suggested",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = onSurface.copy(alpha = 0.8f),
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(onSurface.copy(alpha = 0.08f))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
                                                            }
                                                        }
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
                                } else {
                                    Spacer(Modifier.height(32.dp))
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.985f else 1f, label = "cardScale")
                                    val cardShape20 = remember { RoundedCornerShape(20.dp) }
                                    val borderBrushMain by remember(clampedPulse, clampedGlow) {
                                        mutableStateOf(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF622121).copy(alpha = 0.86f + clampedPulse * 0.12f + clampedGlow * 0.12f),
                                                    Color.White.copy(alpha = 0.08f + clampedPulse * 0.06f + clampedGlow * 0.06f)
                                                )
                                            )
                                        )
                                    }
                                    fun formatTime(ms: Long): String {
                                        val hours = ms / (1000 * 60 * 60)
                                        val minutes = (ms / (1000 * 60)) % 60
                                        val seconds = (ms / 1000) % 60
                                        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
                                    }
                                    val cardioExerciseNames = remember {
                                        listOf("Running (Treadmill)", "Stair Climber", "Elliptical Trainer", "Rowing Machine", "Stationary Bike", "Swimming")
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .haze(state = haze)
                                            .height(140.dp)
                                            .graphicsLayer { scaleX = scale; scaleY = scale }
                                            .border(width = 1.dp, brush = borderBrushMain, shape = cardShape20)
                                            .clickable(interactionSource = interactionSource, indication = null) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                        shape = cardShape20,
                                        colors = CardDefaults.cardColors(containerColor = surface.copy(alpha = 0.28f + clampedPulse * 0.08f + clampedGlow * 0.08f))
                                    ) {
                                        Column(verticalArrangement = Arrangement.Center) {
                                            val time = formatTime(latestName?.durationMillis?.toLong() ?: 0)
                                            AdviceSectionUser(
                                                advice = advice,
                                                modifier = Modifier.fillMaxSize(),
                                                lastWorkoutName = latestName?.name ?: "No workouts yet.",
                                                extraLines = if (latestName?.name in cardioExerciseNames) {
                                                    listOf("Distance : ${latestName?.distance} Km", "Time : $time")
                                                } else {
                                                    listOf("Weight : ${latestName?.weight} Kg", "Time : $time")
                                                },
                                                navController = navController
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    QuickStartWorkout(navController = navController)
                                    val dividerBrush = remember {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF9B111E),
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
                                            val scope = rememberCoroutineScope()
                                            val itemInteraction = remember { MutableInteractionSource() }
                                            val itemPressed by itemInteraction.collectIsPressedAsState()
                                            val itemScale by animateFloatAsState(targetValue = if (itemPressed) 0.985f else 1f, label = "cardScaleItem")
                                            val expand = remember { Animatable(0f) }
                                            val isExpanding = expand.isRunning || expand.value > 0f
                                            val h by remember { derivedStateOf { 65.dp + (160.dp - 65.dp) * expand.value } }
                                            val contentAlpha by animateFloatAsState(targetValue = if (isExpanding) 0.0f else 1f, animationSpec = tween(180), label = "contentAlpha")
                                            val borderBrushItem by remember(clampedPulse, clampedGlow, expand.value) {
                                                mutableStateOf(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF622121).copy(alpha = (0.86f + clampedPulse * 0.12f + clampedGlow * 0.12f) * (1f - 0.2f * expand.value)),
                                                            Color.White.copy(alpha = (0.08f + clampedPulse * 0.06f + clampedGlow * 0.06f) * (1f - 0.2f * expand.value))
                                                        )
                                                    )
                                                )
                                            }
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(h)
                                                    .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                                                    .border(width = 1.dp, brush = borderBrushItem, shape = cardShape16)
                                                    .zIndex(if (isExpanding) 1f else 0f)
                                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                                        if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE && !isExpanding) {
                                                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                            workout.value = workoutName
                                                            scope.launch {
                                                                expand.animateTo(0.85f, tween(200))
                                                                val intent = Intent(context, WorkoutActivity::class.java).apply { putExtra("WORKOUT_NAME", workoutName) }
                                                                startActivity(context, intent, null)
                                                                expand.animateTo(1f, tween(650))
                                                                expand.snapTo(0f)
                                                            }
                                                        }
                                                    },
                                                shape = cardShape16,
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (ConnectedWorkout.currentMode.value == ConnectedWorkout.WorkoutMode.INACTIVE)
                                                        surface.copy(alpha = 0.14f + clampedPulse * 0.05f + clampedGlow * 0.05f)
                                                    else Color.DarkGray
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .graphicsLayer { alpha = contentAlpha },
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
                                                                color = onSurface.copy(alpha = 0.8f),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(onSurface.copy(alpha = 0.08f))
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
                                    if (stages.after600ms && shouldAnimate) {
                                        WeightHistoryGraph(workouts = currentState.workouts.take(2))
                                    }
                                }
                            }
                            is WorkoutListUiState.Error -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }
                    if (uiState is WorkoutListUiState.Success) {
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
    }
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
