package com.forgecompose.workouttracker



import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.forgecompose.workouttracker.ui.theme.WorkoutTrackerTheme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
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




// use the MAX of this exercise for the bar scale (looks nicer than “max of whole dataset”)
fun maxWeightForName(workouts: List<Workout>, exerciseName: String): Double {
    return workouts.asSequence()
        .filter { it.name == exerciseName }
        .map { it.weight ?: 0.0 }
        .maxOrNull() ?: 0.0
}

@Composable
private fun UnderlineGlow(
    width: Dp,
    thickness: Dp,
    color: Color,
    glowRadius: Dp,
    modifier: Modifier = Modifier
) {
    // Solid core line
    Box(
        modifier = modifier
            .size(width = width, height = thickness)
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
    )

    // Glow bleed (blurred copy behind)
    Box(
        modifier = modifier

            .graphicsLayer {
                // Slightly bigger than the core line to let the glow spread
                scaleX = 1.15f
                scaleY = 1.8f
                // Android 12L+/13+: RenderEffect blur gives a soft bleed
                renderEffect = RenderEffect.createBlurEffect(
                    glowRadius.toPx(), glowRadius.toPx(),
                    Shader.TileMode.DECAL
                ).asComposeRenderEffect()
                alpha = 0.75f
            }
            .background(color.copy(alpha = 0.55f), RoundedCornerShape(percent = 50))
    )
}

fun Modifier.pulsingBorder(pulse: Float, radius: Dp): Modifier = composed {
    // Call rememberUpdatedState in the Composable scope
    val currentPulse by rememberUpdatedState(pulse)

    this.drawWithCache {
        // Use the remembered state here
        // val p by rememberUpdatedState(pulse) // <- INCORRECT: Remove this line
        val r = radius.toPx()
        onDrawWithContent {
            drawContent()
            val brush = Brush.linearGradient(
                listOf(
                    // Use currentPulse.value or just currentPulse if its type is already Float
                    Color.White.copy(alpha = 0.2f + currentPulse),
                    Color.White.copy(alpha = 0.1f + currentPulse * 0.3f)
                )
            )
            drawRoundRect(
                brush = brush,
                style = Stroke(width = 1.dp.toPx()),
                cornerRadius = CornerRadius(r, r)
            )
        }
    }
}
@Composable
private fun StraightUnderlineGlow(
    width: Dp,
    thickness: Dp,
    color: Color,
    glowRadius: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        // glow (behind)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = width, height = thickness)
                .graphicsLayer {
                    // make the glow spread without changing the core line size
                    scaleX = 1.25f
                    scaleY = 2.2f
                    renderEffect = RenderEffect.createBlurEffect(
                        glowRadius.toPx(), glowRadius.toPx(),
                        Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                    alpha = 0.7f
                }
                .background(color.copy(alpha = 0.6f), RoundedCornerShape(percent = 50))
        )
        // core straight line (on top)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = width, height = thickness)
                .clip(RoundedCornerShape(percent = 50))
                .background(color)
        )
    }
}


// 1) Put this near the top of the file
private object TaskbarMotion {
    // Visibility + icon timings
    const val EnterSlideMs = 900
    const val EnterFadeMs  = 500
    const val ExitSlideMs  = 1100
    const val ExitFadeMs   = 900

    const val IconGlowMs   = 420
    const val IconScaleMs  = 360

    // Wobble amounts (subtle = smoother)
    const val RouteNudgePx = 10      // was 14
    const val TapNudgePx   = 8       // was 10

    // Springs: slow & smooth (no bounce, very low stiffness)
    val PosSpring   = spring<Offset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
    val ScaleSpring = spring<Offset>(dampingRatio = Spring.DampingRatioNoBouncy,        stiffness = Spring.StiffnessVeryLow)
    val SkewSpring  = spring<Offset>(dampingRatio = Spring.DampingRatioNoBouncy,        stiffness = Spring.StiffnessVeryLow)

    // Easing that feels gentle
    val Ease = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // slow-out
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


                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (!granted) {
                        Toast.makeText(
                            context,
                            "Permission is required for full functionality.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                LaunchedEffect(Unit) {

                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
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



    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF230C0C)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = slideSpec
                ) + fadeIn(animationSpec = fadeInSpec) +
                        scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = slideSpec
                ) + fadeOut(animationSpec = fadeOutSpec)
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = slideSpec
                ) + fadeIn(animationSpec = fadeInSpec) +
                        scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = slideSpec
                ) + fadeOut(animationSpec = fadeOutSpec)
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

            composable("WorkoutSelector") { WorkoutSelector(viewModel, navController) }
            composable("WorkoutHistory") { WorkoutHistory(viewModel, navController) }
            composable(Routes.DetailedWorkoutRoute, arguments = listOf(navArgument(Routes.ArgId) { type = NavType.LongType })) { WorkoutDetailScreen(navController, viewModel2, viewModel) }
            composable("UserProfile") { UserProfileScreen(navController, viewModel) }
            composable("PersonaSettings") { PersonaSettingsScreen(navController = navController) }
            composable("EditUserStats") { EditUserStats(navController) }
            composable("MuscleGroup") { ProfileMuscleStatusRoute(navController, viewModel2,viewModel) }
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


@Composable
fun AdviceSectionUser(
    advice: String,
    lastWorkoutName: String,
    extraLines: List<String>,                 // ← NEW: lines shown under name when AI is OFF
    maxExtraLines: Int = 4,                   // ← cap how many to show
    modifier: Modifier = Modifier
) {
    val aiEnabled = dynamicModel.personaConfig.value.enabled

    val cold = rememberColdStartStages()
    val glowTransition = rememberInfiniteTransition(label = "adviceGlow")
    val glow by glowTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    var isLoading by remember { mutableStateOf(aiEnabled) }
    val cardShape = remember { RoundedCornerShape(16.dp) }

    LaunchedEffect(aiEnabled) {
        isLoading = aiEnabled
        if (aiEnabled) {
            delay(1000)
            isLoading = false
        }
    }

    var canExpand by remember { mutableStateOf(false) }
    val expandAnimation by animateDpAsState(
        if (canExpand) 220.dp else 120.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "adviceExpand"
    )

    val accent = if (aiEnabled) Color(0xFFFF3B30) else Color(0xFFAB4747)
    val borderGlow = if (aiEnabled) glow else 0.35f

    Surface(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = Color(0xFF8B0000),
                spotColor = Color(0xFF8B0000)
            ),
        shape = cardShape,
        color = Color(0xFF120707).copy(alpha = 0.75f)
    ) {
        Box(
            modifier = Modifier
                .size(expandAnimation)
                .drawWithCache {
                    val borderBrush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFF5555).copy(alpha = 0.4f * borderGlow),
                            Color(0xFF8B0000).copy(alpha = 0.25f * borderGlow)
                        )
                    )
                    val bgBrush = Brush.radialGradient(
                        listOf(
                            Color(0xFF3A0E0E).copy(alpha = 0.35f * borderGlow),
                            Color(0xFF120707).copy(alpha = 0.85f)
                        )
                    )
                    onDrawBehind {
                        drawRoundRect(
                            brush = bgBrush,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            style = Stroke(width = 1.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                }
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { canExpand = !canExpand }
        ) {
            if (cold.after200ms) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (aiEnabled) Icons.Rounded.AutoAwesome else Icons.Default.Flag,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(27.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))

                    if (aiEnabled) {
                        Text(
                            text = advice,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 24.dp)
                        )
                    } else {
                        // ── AI OFF: show label, big name, then extra lines
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Last workout :",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastWorkoutName.ifBlank { "None" },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            val linesToShow = remember(extraLines, maxExtraLines) {
                                extraLines.filter { it.isNotBlank() }.take(maxExtraLines)
                            }
                            if (linesToShow.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                linesToShow.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (aiEnabled && isLoading) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 8.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 2.dp),
                        color = Color(0xFF8B0000),
                        trackColor = Color.Black.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}










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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val latestName by viewModel2.latestWorkoutName.collectAsStateWithLifecycle()
        val haptics = LocalHapticFeedback.current
        val context = LocalContext.current
        val stages = rememberColdStartStages()
        var isExpanded by remember { mutableStateOf(false) }

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val animationDuration = 100

        val cornerRadius by animateDpAsState(
            targetValue = if (isExpanded) 0.dp else 32.dp,
            animationSpec = tween(durationMillis = animationDuration),
            label = "fabCornerRadius"
        )
        val iconAlpha by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 1f,
            animationSpec = tween(durationMillis = animationDuration / 2),
            label = "fabIconAlpha"
        )

        val shouldAnimate = stages.afterFirstFrame && !isExpanded
        val infiniteTransition: InfiniteTransition? =
            if (shouldAnimate) rememberInfiniteTransition(label = "bg") else null

        val gradientOffset by (infiniteTransition?.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
            label = "gradientOffset"
        ) ?: remember { mutableStateOf(0f) })

        val waveOffset by (infiniteTransition?.animateFloat(
            initialValue = 0f, targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
            label = "waveOffset"
        ) ?: remember { mutableStateOf(0f) })

        val pulseAlpha by (infiniteTransition?.animateFloat(
            initialValue = 0.15f, targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseAlpha"
        ) ?: remember { mutableStateOf(0.2f) })

        val glowIntensity by (infiniteTransition?.animateFloat(
            initialValue = 0.2f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowIntensity"
        ) ?: remember { mutableStateOf(0.3f) })

        var showIntro by remember { mutableStateOf(true) }
        val introProgress by animateFloatAsState(
            targetValue = if (showIntro) 0f else 1f,
            animationSpec = tween(700, easing = LinearEasing),
            label = "introProgress"
        )
        val hour = remember { LocalTime.now().hour }
        val introColors = remember(hour) {
            when (hour) {
                in 5..10 -> listOf(
                    Color(0xFF2B1A00),
                    Color(0xFF3C2405),
                    Color(0xFF5A360A),
                    Color(0xFF7A4A12)
                )
                in 11..16 -> listOf(
                    Color(0xFF332300),
                    Color(0xFF4A3408),
                    Color(0xFF6B4B0F),
                    Color(0xFF8C6217)
                )
                in 17..20 -> listOf(
                    Color(0xFF1A0614),
                    Color(0xFF2A0A20),
                    Color(0xFF3D0F2D),
                    Color(0xFF52153A)
                )
                else -> listOf(
                    Color(0xFF02040A),
                    Color(0xFF0A1324),
                    Color(0xFF15243D),
                    Color(0xFF1E3352)
                )
            }
        }
        val introBrush = remember(introColors) {
            Brush.linearGradient(
                colors = introColors,
                start = Offset(Float.POSITIVE_INFINITY, 0f), // Start from the right edge
                end = Offset.Zero // End at the top-left (or adjust as needed for desired angle)
            )
        }

        LaunchedEffect(Unit) { showIntro = false }

        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                delay(animationDuration.toLong())
                navController.navigate("WorkoutSelector")
                delay(200)
                isExpanded = false
            }
        }
        LaunchedEffect(Unit) {
            readPersona(dynamicModel.personaMode.value)
        }

        val workoutPremadeRandom = remember { workoutPresets.shuffled().take(3).map { it.name } }

        fun NavController.openWorkout(id: Long) {
            navigate("${Routes.DetailedWorkout}/$id")
        }

        val fitnessContext =
            "You are a fitness coach. The user provides their weight, last workout, and weight of the workout(Weights are in kg). Give them advice on what to do next. Call them by their name., Max 20 words"

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
                val prs = workouts.asSequence()
                    .filter { (it.weight ?: 0.0) > 0.0 }
                    .groupBy { it.name }
                    .map { (name, list) -> PersonalRecord(name, list.maxOf { it.weight!! }) }
                    .sortedByDescending { it.maxWeight }
                val recent = workouts.take(5)
                prs to recent
            } else {
                emptyList<PersonalRecord>() to emptyList<Workout>()
            }
        }
    fun setPrefStyle(){
        when (userPreferredStyle) {
            "Weights" -> {
                userPreferredStyle = "Weights"
            }
            "Cardio" -> {
                userPreferredStyle = "Cardio"
            }
            "Both" -> {
                userPreferredStyle = "Weights and cardio"
            }

        }
    }
        val scope = rememberCoroutineScope()
        val usageTracker = remember { PresetUsageTracker(context) }
        val usageMap by usageTracker.usageFlow.collectAsState(initial = emptyMap())
        LaunchedEffect(userName, userAge, userWeight, userHeight, userExperience, personalRecords, recentWorkouts) {
            WellnessAI.startBackgroundJobs(scope)
            setPrefStyle()
            generateAdvice(
                """
    The user’s profile:
    – Name: $userName
    – Age: $userAge
    – Weight: $userWeight kg
    – Height: $userHeight cm
    – Training experience: $userExperience
    """.trimIndent(),

                """
    The user’s training preferences:
    – Current personal records: $personalRecords
    – Preferred training style: $userPreferredStyle
    – Muscles they care most about: $userImportantMuscles
    """.trimIndent(),

                """
    Recent workout history for this user: $recentWorkouts

    Use this information to create advice that feels personal and tailored to their current fitness level, goals, and style.
    Be encouraging, practical, and specific (not generic). Mention progress opportunities, form cues, or recovery tips that match their profile.
    """.trimIndent()
            )

        }

        val haze = remember { HazeState() }
        val clampedGlow by remember { derivedStateOf { glowIntensity.coerceIn(0f, 1f) } }
        val clampedPulse by remember { derivedStateOf { pulseAlpha.coerceIn(0f, 1f) } }
        val clampedGrad by remember { derivedStateOf { gradientOffset.coerceIn(0f, 1f) } }

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

        WorkoutTrackerTheme {
            CompositionLocalProvider(LocalHazeState provides haze) {
                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
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
                                            Color(0xFF4D1B1B).copy(alpha = 0.85f + clampedGrad * 0.45f),
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
                                        if (stages.after600ms && shouldAnimate) {
                                            val baseAlpha = clampedPulse
                                            val g = clampedGlow
                                            val w = size.width
                                            val h = size.height
                                            for (layer in 0..2) {
                                                val layerOffset = waveOffset + (layer * PI.toFloat() / 4)
                                                val layerAlpha = baseAlpha * (0.25f + layer * 0.12f) * g
                                                val layerColor = when (layer) {
                                                    0 -> Color(0xFF4A1A1A).copy(alpha = layerAlpha)
                                                    1 -> Color(0xFF3A1515).copy(alpha = layerAlpha * 0.8f)
                                                    else -> Color(0xFF2A0D0D).copy(alpha = layerAlpha * 0.6f)
                                                }
                                                wavePath.reset()
                                                val baseY = h * (0.22f + layer * 0.16f)
                                                val step = (w / 36f).coerceAtLeast(10f)
                                                var x = 0f
                                                val waveHeight = 90f
                                                while (x <= w) {
                                                    val t = x / w
                                                    val phase = t * 3f * PI.toFloat() + layerOffset
                                                    val y = baseY + sin(phase) * waveHeight * (0.55f + layer * 0.22f) * g
                                                    wavePath.lineTo(x, y)
                                                    x += step
                                                }
                                                wavePath.lineTo(w, h)
                                                wavePath.lineTo(0f, h)
                                                wavePath.close()
                                                drawPath(path = wavePath, color = layerColor)
                                            }
                                            particles.forEachIndexed { i, (baseX, yOff, r) ->
                                                val px = w * baseX + sin(waveOffset * 0.7f + i) * 60f * g
                                                val py = h * yOff + cos(waveOffset * 0.5f + i * 0.3f) * 60f
                                                val alpha = baseAlpha * (0.35f + sin(waveOffset + i) * 0.25f) * g
                                                drawCircle(Color.White.copy(alpha = alpha), r, Offset(px, py))
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
                                        CircularProgressIndicator(
                                            color = Color.White.copy(alpha = 0.8f + clampedGlow * 0.2f),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                                is WorkoutListUiState.Success -> {
                                    if (currentState.workouts.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(bottom = 96.dp), // reserve space for taskbar
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
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = surface.copy(alpha = 0.28f + clampedPulse * 0.08f + clampedGlow * 0.08f)
                                                    )
                                                ) {
                                                    Column(verticalArrangement = Arrangement.Center) {
                                                        AdviceSectionUser(
                                                            advice = advice,
                                                            modifier = Modifier.fillMaxSize(),
                                                            lastWorkoutName = "No workouts yet.",
                                                            extraLines = listOf(
                                                                "Get started: Quick Start below",
                                                                "Or choose a preset from the list"
                                                            ),
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
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    suggested.forEach { workoutName ->
                                                        val itemInteraction = remember { MutableInteractionSource() }
                                                        val itemPressed by itemInteraction.collectIsPressedAsState()
                                                        val itemScale by animateFloatAsState(
                                                            targetValue = if (itemPressed) 0.985f else 1f,
                                                            label = "cardScaleItem_empty"
                                                        )
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
                                                                    if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        workout.value = workoutName
                                                                        val intent = Intent(context, WorkoutActivity::class.java).apply {
                                                                            putExtra("WORKOUT_NAME", workoutName)
                                                                        }
                                                                        startActivity(context, intent, null)
                                                                    }
                                                                },
                                                            shape = cardShape16,
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE)
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
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter),
                                                navController = navController,
                                                cornerRadius = cornerRadius,
                                                iconAlpha = iconAlpha,
                                                uiState = uiState
                                            )
                                        }
                                    }

                                    else {
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
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .haze(state = haze)
                                                .height(140.dp)
                                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                                .border(
                                                    width = 1.dp,
                                                    brush = borderBrushMain,
                                                    shape = cardShape20
                                                )
                                                .clickable(interactionSource = interactionSource, indication = null) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            shape = cardShape20,
                                            colors = CardDefaults.cardColors(
                                                containerColor = surface.copy(
                                                    alpha = 0.28f + clampedPulse * 0.08f + clampedGlow * 0.08f
                                                )
                                            )
                                        ) {
                                            Column(verticalArrangement = Arrangement.Center) {

                                                AdviceSectionUser(
                                                    advice = advice,
                                                    modifier = Modifier.fillMaxSize(),
                                                    lastWorkoutName = latestName?.name
                                                        ?: "No workouts yet.",
                                                    extraLines = listOf(
                                                        "Weight : ${latestName?.weight}",
                                                        "Status : ${latestName?.status}"
                                                    ),

                                                )
                                            }
                                        }

Spacer(
    modifier = Modifier.height(16.dp)
)
                                      QuickStartWorkout(
                                          navController = navController,
                                      )
                                        val dividerBrush = remember {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF9B111E), // Crimson Red
                                                    Color.White.copy(alpha = 0.4f) // Washed out white
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

                                        val topPresets by remember(usageMap, workoutPremadeRandom) {
                                            derivedStateOf {
                                                if (usageMap.isEmpty()) workoutPremadeRandom.take(3).map { it to 0 }
                                                else usageMap.entries
                                                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                                                    .take(3)
                                                    .map { it.key to it.value }
                                            }
                                        }

                                        val cardShape16 = remember { RoundedCornerShape(16.dp) }

                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(topPresets, key = { it.first }, contentType = { "preset" }) { (workoutName, useCount) ->
                                                val itemInteraction = remember { MutableInteractionSource() }
                                                val itemPressed by itemInteraction.collectIsPressedAsState()
                                                val itemScale by animateFloatAsState(
                                                    targetValue = if (itemPressed) 0.985f else 1f,
                                                    label = "cardScaleItem"
                                                )
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
                                                        .border(
                                                            width = 1.dp,
                                                            brush = borderBrushItem,
                                                            shape = cardShape16
                                                        )
                                                        .clickable(interactionSource = itemInteraction, indication = null) {
                                                            if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) {
                                                                haptics.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                                workout.value = workoutName
                                                                val intent = Intent(
                                                                    context,
                                                                    WorkoutActivity::class.java
                                                                ).apply {
                                                                    putExtra(
                                                                        "WORKOUT_NAME",
                                                                        workoutName
                                                                    )
                                                                }
                                                                startActivity(context, intent, null)
                                                            }
                                                        },
                                                    shape = cardShape16,
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (ConnectedWorkout.currentMode.value == WorkoutMode.INACTIVE) surface.copy(
                                                            alpha = 0.14f + clampedPulse * 0.05f + clampedGlow * 0.05f
                                                        ) else Color.DarkGray
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
                                                            if (useCount > 0) {
                                                                Text(
                                                                    text = "×$useCount",
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

                        if (uiState is WorkoutListUiState.Success ) {
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistory(
    viewModel: WorkoutListViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset(Float.POSITIVE_INFINITY, 0f),
            end = Offset.Zero
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF0A0404),
                Color(0xFF2A0F0F),
                Color(0xFF3D0000),
                Color(0xFF4A0000),
                Color(0xFF060202)
            ),
            radius = 1000f,
            center = Offset(0.5f, 0.4f)
        )
    }
    val secondaryStaticBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0000).copy(alpha = 0.2f),
                Color.Transparent,
                Color(0xFF2A0F0F).copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    }

    fun NavController.openWorkout(id: Long) {
        navigate("${Routes.DetailedWorkout}/$id")
    }

    LaunchedEffect(Unit) {
        taskbarOverride.shouldOverrideVisiblity.value = false
    }

    val workouts = remember(uiState) {
        (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
    }
    val cold = rememberColdStartStages()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Workout History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete All", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmation = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val base = Color(0xFF060202)
                onDrawBehind {
                    drawRect(base)
                    drawRect(staticGradientBrush)
                    drawRect(secondaryStaticBrush)
                    if (introProgress < 1f) drawRect(introBrush, alpha = 1f - introProgress)
                }
            }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is WorkoutListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WorkoutListUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is WorkoutListUiState.Success -> {
                    if (workouts.isEmpty()) {
                        EmptyState()
                    } else {
                        WorkoutHistoryList(
                            workouts = workouts,
                            onWorkoutClicked = { workout ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("selectedWorkoutId", workout.id)
                                navController.openWorkout(workout.id.toLong())
                            },
                            onDeleteClicked = { workout ->
                                scope.launch(Dispatchers.IO) { viewModel.deleteWorkout(workout) }
                            }
                        )
                    }
                }
            }

            val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            if (cold.after100ms) {
                FloatingTaskbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = navPad + 2.dp),
                    navController = navController,
                    cornerRadius = 34.dp,
                    iconAlpha = 1f,
                    uiState = uiState
                )
            }
        }
    }

    val haptics = LocalHapticFeedback.current
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch(Dispatchers.IO) { viewModel.deleteAllWorkouts() }
                showDeleteConfirmation = false
            },
            onDismiss = {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                showDeleteConfirmation = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutHistoryList(
    workouts: List<Workout>,
    onWorkoutClicked: (Workout) -> Unit,
    onDeleteClicked: (Workout) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val onItemClick = rememberUpdatedState(onWorkoutClicked)
    val onItemDelete = rememberUpdatedState(onDeleteClicked)
val cold = rememberColdStartStages()
    if (cold.after200ms) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = workouts,
                key = { it.id },
                contentType = { "workout" }
            ) { workout ->
                WorkoutHistoryItem(
                    workout = workout,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onItemClick.value(workout)
                    },
                    onDelete = { onItemDelete.value(workout) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryItem(
    workout: Workout,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(24.dp) }
    val baseDark = remember { Color(0xFF1A1A1A).copy(alpha = 0.55f) }
    val accentGlow = remember { Color(0xFFFF3535) }
    val interactionSource = remember { MutableInteractionSource() }

    val durationText = remember(workout.durationMillis) {
        val safe = workout.durationMillis ?: 0L
        val hours = (safe / 3_600_000).toInt()
        val minutes = ((safe / 60_000) % 60).toInt()
        val seconds = ((safe / 1_000) % 60).toInt()
        if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"
    }
val cold = rememberColdStartStages()
    if (cold.after100ms) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = baseDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duration: $durationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete workout",
                        tint = accentGlow.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}


@Composable
    private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete all workout history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun EmptyState() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Your workout history is empty.\nCompleted sessions will appear here.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }











@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF0A0404),
                Color(0xFF2A0F0F),
                Color(0xFF3D0000),
                Color(0xFF4A0000),
                Color(0xFF060202)
            ),
            radius = 1000f,
            center = Offset(0.5f, 0.4f)
        )
    }
    val secondaryStaticBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0000).copy(alpha = 0.2f),
                Color.Transparent,
                Color(0xFF2A0F0F).copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    }
    val haze = remember { HazeState() }
    CompositionLocalProvider(LocalHazeState provides haze) {

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedId: Int? = navController
            .previousBackStackEntry
            ?.savedStateHandle
            ?.get<Int>("selectedWorkoutId")

        val allWorkouts = remember(uiState) { (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty() }
        val selectedWorkout by remember(selectedId, allWorkouts) {
            derivedStateOf { allWorkouts.firstOrNull { it.id == selectedId } }
        }

        val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

        val sameNameWorkouts by remember(selectedWorkout, allWorkouts) {
            derivedStateOf {
                if (selectedWorkout != null)
                    allWorkouts.asSequence().filter { it.name == selectedWorkout!!.name }.sortedBy { it.startTime }.toList()
                else emptyList()
            }
        }

        val previousSame by remember(selectedWorkout, sameNameWorkouts) {
            derivedStateOf {
                selectedWorkout?.let { it1 -> sameNameWorkouts.lastOrNull { it.startTime < selectedWorkout!!.startTime } }
            }
        }

        val bestDurationForSame by remember(sameNameWorkouts) {
            derivedStateOf { sameNameWorkouts.mapNotNull { it.durationMillis }.maxOrNull() }
        }

        val weekStartEnd by remember(selectedWorkout) {
            derivedStateOf {
                if (selectedWorkout == null) null else {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedWorkout!!.date }
                    cal.firstDayOfWeek = Calendar.MONDAY
                    cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    val end = start + TimeUnit.DAYS.toMillis(6)
                    start to end
                }
            }
        }

        val thisWeekCount by remember(weekStartEnd, allWorkouts) {
            derivedStateOf {
                weekStartEnd?.let { (start, end) ->
                    allWorkouts.count { it.date in start until end }
                } ?: 0
            }
        }

        val streak by remember(selectedWorkout, allWorkouts) {
            derivedStateOf { if (selectedWorkout == null) 0 else computeStreak(selectedWorkout!!.date, allWorkouts) }
        }

        val chartSeriesSig by remember(allWorkouts) {
            derivedStateOf {
                allWorkouts.fold(1L) { acc, w ->
                    acc * 31 + w.id + w.date + (w.durationMillis ?: 0L) + (w.weight?.toLong() ?: 0L)
                }
            }
        }
        val cachedAllWorkouts = remember(chartSeriesSig) { allWorkouts.toList() }
val cold = rememberColdStartStages()
        LaunchedEffect(Unit) { taskbarOverride.shouldOverrideVisiblity.value = false }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            selectedWorkout?.name ?: "Workout Summary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060202))
                .background(staticGradientBrush)
                .background(secondaryStaticBrush)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = haze, zIndex = 2f)
            ) {
                when (uiState) {
                    is WorkoutListUiState.Loading -> LoadingBlock(padding)
                    is WorkoutListUiState.Error -> ErrorBlock(padding)
                    is WorkoutListUiState.Success -> {
                        if (selectedWorkout == null) {
                            MissingBlock(padding)
                        } else {
                            if (cold.after200ms) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {

                                    if (cold.after700ms) {
                                        item {
                                            GlassCard {
                                                Column(Modifier.padding(20.dp)) {
                                                    Text(
                                                        text = selectedWorkout!!.name,
                                                        style = MaterialTheme.typography.displaySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = Color.White
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    Column(
                                                        Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        StatusChip(selectedWorkout!!.status)
                                                        Spacer(Modifier.width(8.dp))
                                                        InfoChip(
                                                            label = dateFormat.format(
                                                                Date(
                                                                    selectedWorkout!!.date
                                                                )
                                                            ),
                                                            icon = Icons.Default.DateRange
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        InfoChip(
                                                            label = selectedWorkout!!.weight?.let { "${it} kg" }
                                                                ?: "--",
                                                            icon = Icons.Default.MonitorWeight
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (cold.after400ms) {
                                        item {
                                            GlassCard {
                                                Column(Modifier.padding(20.dp)) {
                                                    SectionTitle("Timing")
                                                    val dur =
                                                        formatDuration(selectedWorkout!!.durationMillis)
                                                    val start =
                                                        timeFormat.format(Date(selectedWorkout!!.startTime))
                                                    val end = selectedWorkout!!.endTime?.let {
                                                        timeFormat.format(Date(it))
                                                    } ?: "--"
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        LabeledStat("Duration", dur)
                                                        LabeledStat("Start", start)
                                                        LabeledStat("End", end)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (cold.after600ms) {
                                        item {
                                            GlassCard {
                                                Column(Modifier.padding(20.dp)) {
                                                    SectionTitle("Highlights")
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        LabeledStat(
                                                            "This Week",
                                                            thisWeekCount.toString()
                                                        )
                                                        LabeledStat("Streak", "${streak}d")
                                                        LabeledStat(
                                                            "Last Time",
                                                            previousSame?.let {
                                                                fmtAgo(
                                                                    it.date,
                                                                    selectedWorkout!!.date
                                                                )
                                                            } ?: "--"
                                                        )
                                                        LabeledStat(
                                                            "Best (Same)",
                                                            bestDurationForSame?.let {
                                                                formatDuration(
                                                                    it
                                                                )
                                                            }
                                                                ?: "--"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (cachedAllWorkouts.isNotEmpty() && cold.after700ms) {

                                        item {
                                            GlassCard {
                                                Column(Modifier.padding(20.dp)) {
                                                    SectionTitle("Progression")
                                                    key(selectedWorkout!!.name, chartSeriesSig) {
                                                        ExerciseWeightProgressionGraph(
                                                            exerciseName = selectedWorkout!!.name,
                                                            workouts = cachedAllWorkouts
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (sameNameWorkouts.isNotEmpty() && cold.after800ms) {

                                        item {
                                            GlassCard {
                                                Column(Modifier.padding(20.dp)) {
                                                    SectionTitle("Recent Sessions")
                                                    Spacer(Modifier.height(6.dp))

                                                    val recentSessions =
                                                        remember(sameNameWorkouts) {
                                                            sameNameWorkouts.takeLast(5)
                                                                .asReversed()
                                                        }

                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        recentSessions.forEach { w ->
                                                            Row(
                                                                Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    dateFormat.format(Date(w.date)),
                                                                    color = Color.White,
                                                                    style = MaterialTheme.typography.bodyLarge
                                                                )
                                                                Text(
                                                                    formatDuration(w.durationMillis),
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                                        alpha = 0.85f
                                                                    ),
                                                                    style = MaterialTheme.typography.bodyLarge
                                                                )
                                                            }
                                                            Divider(color = Color.White.copy(alpha = 0.06f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        val haptics = LocalHapticFeedback.current
                                        GlassCard {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        viewModel.deleteWorkout(selectedWorkout!!)
                                                        navController.navigateUp()
                                                    }
                                                ) {
                                                    Text(
                                                        "Delete",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    item { Spacer(Modifier.height(32.dp)) }
                                }
                            }
                        }
                    }
                }
                if (cold.after100ms) {
                    FloatingTaskbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        navController = navController,
                        cornerRadius = 34.dp,
                        iconAlpha = 1f,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

/* ======= UI helpers (reusable, lightweight) ======= */

    @Composable
    private fun GlassCard(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val shape = RoundedCornerShape(24.dp)


        val baseDark = Color(0xFF1A1A1A).copy(alpha = 0.25f)
        val accentGlow = Color(0xFFFF3535)




        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)


                .background(baseDark)


        ) {
            content()
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    @Composable
    private fun LabeledStat(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    private fun InfoChip(label: String, icon: ImageVector) {
        AssistChip(
            onClick = {},
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.White.copy(alpha = 0.08f),
                labelColor = Color.White,
                leadingIconContentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            border = null
        )
    }

    @Composable
    private fun StatusChip(status: WorkoutStatus) {
        val (bg, fg) = when (status) {
            WorkoutStatus.PLANNED -> Color(0xFF1E3A5F) to Color(0xFFB3D4FF)
            WorkoutStatus.IN_PROGRESS -> Color(0xFF5F3A1E) to Color(0xFFFFD7A3)
            WorkoutStatus.COMPLETED -> Color(0xFF1E5F3A) to Color(0xFFB3FFD4)
            WorkoutStatus.SKIPPED -> Color(0xFF5F1E1E) to Color(0xFFFFB3B3)
        }
        AssistChip(
            onClick = {},
            label = { Text(status.name.replace('_', ' ')) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = bg.copy(alpha = 0.28f),
                labelColor = fg
            ),
            modifier = Modifier.fillMaxWidth(),
            border = null
        )

    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMuscleStatusRoute(
    navController: NavController,
    viewModel2: MainScreenViewModel,
    viewModel: WorkoutListViewModel
) {
    val cold = rememberColdStartStages()
    val haze = remember { HazeState() }


    val hour = remember { java.time.LocalTime.now().hour }
    val introColors = remember(hour) {
        when (hour) {
            in 5..10 -> listOf(Color(0xFF2B1A00), Color(0xFF3C2405), Color(0xFF5A360A), Color(0xFF7A4A12))
            in 11..16 -> listOf(Color(0xFF332300), Color(0xFF4A3408), Color(0xFF6B4B0F), Color(0xFF8C6217))
            in 17..20 -> listOf(Color(0xFF1A0614), Color(0xFF2A0A20), Color(0xFF3D0F2D), Color(0xFF52153A))
            else -> listOf(Color(0xFF02040A), Color(0xFF0A1324), Color(0xFF15243D), Color(0xFF1E3352))
        }
    }
    val introBrush = remember(introColors) {
        Brush.linearGradient(
            colors = introColors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "introFade"
    )
    LaunchedEffect(Unit) { showIntro = false }


    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF0A0404),
                Color(0xFF2A0F0F),
                Color(0xFF3D0000),
                Color(0xFF4A0000),
                Color(0xFF060202)
            ),
            radius = 1000f,
            center = Offset(0.5f, 0.4f)
        )
    }
    val secondaryStaticBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A0000).copy(alpha = 0.2f),
                Color.Transparent,
                Color(0xFF2A0F0F).copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    }
    val fitnessContext = """
        You are a fitness coach. 
        Given the user's last workouts, return JSON with these keys: 
        "trained":[..], "overtrained":[..], "lacking":[..].
        Use muscle group names: pecs, delts, biceps, triceps, lats, traps, abs, forearms,
        quads, hamstrings, glutes, calves, upper back, lower back.
    """.trimIndent()
    val (advice, _, generateAdvice) = useGeminiAdviceGenerator(
        contextPrompt = "Summarize weekly muscle loads into compact JSON only."
    )

    CompositionLocalProvider(LocalHazeState provides haze) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val allWorkouts = (uiState as? WorkoutListUiState.Success)?.workouts.orEmpty()
        val recent = remember(uiState) {
            allWorkouts
                .sortedByDescending { it.date }
                .take(40)
                .map { w ->
                    WorkoutSummary(
                        date = Instant.ofEpochMilli(w.date),
                        name = w.name,
                        exercises = listOf(w.name.lowercase())
                    )
                }
        }
        val weeklyLoads = remember(recent) { computeWeeklyLoads(Instant.now(), recent) }

        LaunchedEffect(weeklyLoads) {
            val prompt = buildMuscleJsonPrompt(weeklyLoads)
            generateAdvice(prompt, "", "")
        }
        LaunchedEffect(Unit) { taskbarOverride.shouldOverrideVisiblity.value = false }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        // Static background layers
                        drawRect(Color(0xFF060202))
                        drawRect(staticGradientBrush)
                        drawRect(secondaryStaticBrush)

                        // Animated intro layer on top
                        if (introProgress < 1f) {
                            drawRect(brush = introBrush, alpha = 1f - introProgress)
                        }
                    }
                }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Muscle Status",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                containerColor = Color.Transparent, // Make scaffold transparent
                modifier = Modifier.fillMaxSize()
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = haze, zIndex = 2f)
                ) {
                    when (uiState) {
                        is WorkoutListUiState.Loading -> LoadingBlock(padding)
                        is WorkoutListUiState.Error -> ErrorBlock(padding)
                        is WorkoutListUiState.Success -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                if (cold.after100ms) {
                                    item {
                                        GlassCard {
                                            Column(Modifier.padding(20.dp)) {
                                                SectionTitle("Summary")
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val weekStartEnd = remember(allWorkouts) {
                                                        if (allWorkouts.isEmpty()) null else {
                                                            val cal = Calendar.getInstance().apply {
                                                                timeInMillis =
                                                                    System.currentTimeMillis()
                                                                firstDayOfWeek = Calendar.MONDAY
                                                                set(
                                                                    Calendar.DAY_OF_WEEK,
                                                                    Calendar.MONDAY
                                                                )
                                                                set(Calendar.HOUR_OF_DAY, 0); set(
                                                                Calendar.MINUTE,
                                                                0
                                                            )
                                                                set(
                                                                    Calendar.SECOND,
                                                                    0
                                                                ); set(Calendar.MILLISECOND, 0)
                                                            }
                                                            val start = cal.timeInMillis
                                                            val end = start + TimeUnit.DAYS.toMillis(6)
                                                            start to end
                                                        }
                                                    }
                                                    val thisWeekCount =
                                                        weekStartEnd?.let { (start, end) ->
                                                            allWorkouts.count { it.date in start until end }
                                                        } ?: 0
                                                    val streak = if (allWorkouts.isEmpty()) 0
                                                    else computeStreak(
                                                        System.currentTimeMillis(),
                                                        allWorkouts
                                                    )
                                                    LabeledStat("This Week", thisWeekCount.toString())
                                                    LabeledStat("Streak", "${streak}d")
                                                    LabeledStat("Workouts", allWorkouts.size.toString())
                                                }
                                            }
                                        }
                                    }
                                }
                                if (cold.after200ms) {
                                    item {
                                        GlassCard {
                                            Column(Modifier.padding(20.dp)) {
                                                SectionTitle("Muscle Load")
                                                MuscleStatusSection(
                                                    recentWorkouts = recent,
                                                    advicePayload = advice ?: "",
                                                    nowEpochMillis = System.currentTimeMillis(),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                    item { Spacer(Modifier.height(32.dp)) }
                                }
                            }
                        }
                    }
                    if (cold.afterFirstFrame) {
                        FloatingTaskbar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            navController = navController,
                            cornerRadius = 34.dp,
                            iconAlpha = 1f,
                            uiState = uiState
                        )
                    }
                }
            }
        }
    }
}




    /* ======= Render-blocks for UI state ======= */

    @Composable
    private fun LoadingBlock(padding: PaddingValues) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    }

    @Composable
    private fun ErrorBlock(padding: PaddingValues) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Couldn’t load workouts.", color = MaterialTheme.colorScheme.error)
        }
    }

    @Composable
    private fun MissingBlock(padding: PaddingValues) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Workout not found.", color = Color.White.copy(alpha = 0.85f))
        }
    }

    /* ======= Small utilities (pure functions) ======= */

    private fun formatDuration(durationMillis: Long?): String {
        val d = durationMillis ?: 0L
        val h = TimeUnit.MILLISECONDS.toHours(d).toInt()
        val m = TimeUnit.MILLISECONDS.toMinutes(d).toInt() % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(d).toInt() % 60
        return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
    }

    private fun fmtAgo(previous: Long, current: Long): String {
        val diff = current - previous
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        return when {
            days > 0 -> "${days}d ${hours}h"
            else -> "${hours}h"
        }
    }

    private fun computeStreak(anchorDate: Long, workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0
        val dayMillis = 86_400_000L

        // normalize dates to midnight
        fun dayStart(t: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = t }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val daysSet = workouts.map { dayStart(it.date) }.toHashSet()
        var streak = 0
        var cur = dayStart(anchorDate)
        while (daysSet.contains(cur)) {
            streak++
            cur -= dayMillis
        }
        return streak
    }

@Composable
fun WeightHistoryGraph(
    workouts: List<Workout>
) {
    // Keep only entries with weight > 0
    val weighted = remember(workouts) { workouts.filter { (it.weight ?: 0.0) > 0.0 } }

    // Group by exercise name and sort each group by time ASC (stable “timeline”)
    val byNameSortedAsc = remember(weighted) {
        weighted.groupBy { it.name }.mapValues { (_, list) -> list.sortedBy { it.date } }
    }

    // For each workout id, remember the immediately previous workout (same name)
    val prevById: Map<Long, Workout?> = remember(byNameSortedAsc) {
        buildMap<Long, Workout?> {
            for ((_, list) in byNameSortedAsc) {
                var last: Workout? = null
                for (w in list) {
                    put(w.id.toLong(), last)   // previous same-name (may be null)
                    last = w
                }
            }
        }
    }

    // Max historical weight per exercise (for meaningful bar scaling)
    val maxByName: Map<String, Double> = remember(byNameSortedAsc) {
        byNameSortedAsc.mapValues { (_, list) ->
            list.maxOfOrNull { it.weight ?: 0.0 }?.takeIf { it > 0.0 } ?: 1.0
        }
    }

    // Pick last 3 sessions overall (any exercise), newest first
    val last3 = remember(weighted) { weighted.sortedByDescending { it.date }.take(3) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Recent Weights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            if (last3.isEmpty()) {
                Text(
                    "No workouts with tracked weight found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                return@Column
            }

            // Short list: avoid nested scrolling weirdness
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(items = last3, key = { it.id }) { w ->
                    val prevSame = prevById[w.id.toLong()]
                    val maxForThis = maxByName[w.name] ?: 1.0
                    GraphBar(
                        workout = w,
                        maxWeight = maxForThis,
                        prevSameNameWeight = prevById[w.id.toLong()]?.weight,   // primary comparison
                        baselineWeight = null                           // or fallback: personal record / rolling avg
                    )

                }
            }
        }
    }
}




enum class Trend { UP, DOWN, FLAT, NONE }
@Composable
fun LatestOnlyBar(allWorkouts: List<Workout>) {
    val (last, prevSame) = remember(allWorkouts) { lastAndPrevSameName(allWorkouts) }
    if (last != null) {
        val maxForThisExercise = remember(allWorkouts, last.name) {
            maxWeightForName(allWorkouts, last.name).coerceAtLeast(last.weight ?: 0.0)
        }
        GraphBar(
            workout = last,
            maxWeight = maxForThisExercise,
            prevSameNameWeight = prevSame?.weight
        )
    }
}

@Composable
private fun GraphBar(
    workout: Workout,
    maxWeight: Double,
    prevSameNameWeight: Double?,
    // NEW: optional fallback when prevSameNameWeight is null
    baselineWeight: Double? = null,
    // NEW: treat differences below this as "flat"
    epsilonKg: Double = 0.1
) {
    // ----- Numbers -----
    val current = (workout.weight ?: 0.0)
    val safeMax = if (maxWeight > 0) maxWeight else 1.0
    val target = remember(workout.id, current, safeMax) {
        (current / safeMax).toFloat().coerceIn(0f, 1f)
    }
    var start by remember { mutableStateOf(0f) }
    val fill by animateFloatAsState(
        targetValue = start,
        animationSpec = tween(900, 100, LinearOutSlowInEasing),
        label = "barFillAnimation"
    )
    LaunchedEffect(target) { start = target }

    // ----- Trend (prefer previous same-name; fall back to baseline) -----
    // Snap both to 0.1 kg steps so 0.05 jitter doesn't kill the arrow.
    fun snap01(x: Double) = kotlin.math.round(x * 10.0) / 10.0

    val basis: Double? = prevSameNameWeight ?: baselineWeight
    val deltaRaw: Double? = basis?.let { current - it }
    val deltaSnapped: Double? = deltaRaw?.let { snap01(it) }

    val trend = when {
        deltaSnapped == null -> Trend.NONE
        kotlin.math.abs(deltaSnapped) < epsilonKg -> Trend.FLAT
        deltaSnapped > 0                          -> Trend.UP
        else                                      -> Trend.DOWN
    }

    val good = Color(0xFF2ECC71)
    val bad  = Color(0xFFE74C3C)
    val neutral = MaterialTheme.colorScheme.outline
    val trendColor = when (trend) {
        Trend.UP   -> good
        Trend.DOWN -> bad
        Trend.FLAT -> neutral
        Trend.NONE -> neutral
    }
    // Render a neutral glyph even for NONE so the user understands “no baseline”
    val trendIcon = when (trend) {
        Trend.UP   -> Icons.Outlined.ArrowUpward
        Trend.DOWN -> Icons.Outlined.ArrowDownward
        Trend.FLAT -> Icons.Outlined.HorizontalRule
        Trend.NONE -> Icons.Outlined.HorizontalRule // <- changed from null
    }
    val deltaText = deltaSnapped?.let {
        val sign = if (it > 0) "+" else if (it < 0) "−" else ""
        "$sign${"%.1f".format(kotlin.math.abs(it))} kg"
    } ?: "—" // <- make it explicit when there’s no basis

    // ----- Visuals -----
    val glow = Color(0xFFA43434)
    val barBrush = remember { Brush.horizontalGradient(listOf(Color(0xFF5A1E1E), glow)) }
    val trackBrush = remember { Brush.verticalGradient(listOf(Color(0xFF160C0C), Color(0xFF1E0E0E))) }
    val innerHighlight = remember {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.08f),
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.10f)
        )
    }

    val shimmer = rememberInfiniteTransition(label = "barShimmer")
    val shimmerDuration = remember { (2200..3600).random() }
    val sweepX by shimmer.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(shimmerDuration, easing = LinearEasing)),
        label = "sweepX"
    )

    // ===== Layout =====
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        // TOP ROW ONLY: name • current • (arrow + delta)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.1f kg".format(current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(8.dp))
                // Always show some glyph; color stays neutral if no basis/flat
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(4.dp))
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // BAR (no trend UI down here)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(CircleShape)
                .background(trackBrush)
                .drawWithCache {
                    val corner = CornerRadius(size.minDimension, size.minDimension)
                    onDrawBehind {
                        drawRoundRect(brush = innerHighlight, cornerRadius = corner, alpha = 1f)
                    }
                }
                .padding(horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .graphicsLayer {
                        shape = CircleShape
                        clip = true
                        shadowElevation = 10.dp.toPx()
                        spotShadowColor = glow.copy(alpha = 0.45f)
                        ambientShadowColor = glow.copy(alpha = 0.30f)
                    }
                    .background(barBrush)
                    .drawWithCache {
                        val corner = CornerRadius(size.minDimension, size.minDimension)
                        onDrawWithContent {
                            drawContent()

                            // sheen sweep
                            val w = size.width
                            val h = size.height
                            val startX = (w * (sweepX - 0.10f)).coerceIn(-w, w * 1.2f)
                            val endX = (w * (sweepX + 0.10f)).coerceIn(-w, w * 1.2f)
                            val sheen = Brush.linearGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(endX, 0f)
                            )
                            drawRoundRect(
                                brush = sheen,
                                topLeft = Offset(startX, 0f),
                                size = Size(endX - startX, h),
                                cornerRadius = corner
                            )

                            // tip glow
                            if (fill > 0f) {
                                val tipX = size.width
                                drawRect(
                                    brush = Brush.radialGradient(
                                        listOf(glow.copy(alpha = 0.75f), Color.Transparent),
                                        center = Offset(tipX, size.height / 2f),
                                        radius = 22f
                                    )
                                )
                            }
                        }
                    }
            )
        }
    }
}















@Composable
    fun ExerciseWeightProgressionGraph(
        exerciseName: String,
        workouts: List<Workout>
    ) {
        // 1. Filter and sort the data for the specific exercise
        val exerciseData = remember(workouts, exerciseName) {
            workouts
                .filter { it.name == exerciseName && (it.weight ?: 0.0) > 0.0 }
                .sortedBy { it.date }
        }

        // 2. Determine the min/max values for scaling the graph
        val maxWeight = remember(exerciseData) { exerciseData.maxOfOrNull { it.weight!! } ?: 0.0 }
        val minWeight = remember(exerciseData) { exerciseData.minOfOrNull { it.weight!! } ?: 0.0 }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Progression for $exerciseName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (exerciseData.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Not enough data to show a progression.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LineChart(
                        data = exerciseData,
                        maxWeight = maxWeight,
                        minWeight = minWeight
                    )
                }
            }
        }
    }
@Composable
fun QuickStartWorkout(navController: NavController){
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current


    val textBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFCBFC3),
                Color.White.copy(alpha = 0.9f)
            )
        )
    }

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            navController.navigate("WorkoutSelector")
        },

        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Start a new workout",
                modifier = Modifier.weight(1f),

                style = TextStyle(
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    brush = textBrush
                )
            )
            Icon(
                Icons.Default.ArrowForward, contentDescription = null,
                tint = Color.White,

                modifier = Modifier.padding(end = 8.dp).size(32.dp)
            )
        }
    }
}
    /**
     * The core composable responsible for drawing the line chart on a Canvas.
     */
    @Composable
    private fun LineChart(
        data: List<Workout>,
        maxWeight: Double,
        minWeight: Double
    ) {
        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        val density = LocalDensity.current

        val textPaint = remember {
            Paint().apply {
                color = android.graphics.Color.argb(200, 255, 255, 255)
                textSize = with(density) { 12.sp.toPx() }
                textAlign = Paint.Align.CENTER
            }
        }
        val tooltipTextPaint = remember {
            Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = with(density) { 14.sp.toPx() }
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
        }

        val lineBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFFF85757), Color(0xFFD32F2F))) }
        val areaBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF9B111E).copy(alpha = 0.4f), Color.Transparent)) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val pointRadius = with(density) { 15.dp.toPx() }
                        val closestIndex = data
                            .mapIndexed { index, workout ->
                                val yAxisRange = (maxWeight - minWeight).coerceAtLeast(1.0)
                                val xAxisSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                                val pointX = index * xAxisSpacing
                                val pointY = size.height - (((workout.weight ?: 0.0) - minWeight) / yAxisRange * size.height).toFloat()
                                val distance = (tapOffset - Offset(pointX.toFloat(), pointY)).getDistance()
                                index to distance
                            }
                            .minByOrNull { it.second }
                            ?.takeIf { it.second < pointRadius }
                            ?.first
                        selectedIndex = closestIndex
                    }
                }
        ) {
            if (data.size < 2) return@Canvas

            val yAxisRange = (maxWeight - minWeight).coerceAtLeast(1.0)
            val xAxisSpacing = size.width / (data.size - 1)
            val points = data.mapIndexed { index, workout ->
                val x = index * xAxisSpacing
                val y = size.height - (((workout.weight ?: 0.0) - minWeight) / yAxisRange * size.height).toFloat()
                Offset(x.toFloat(), y)
            }

            val areaPath = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(path = areaPath, brush = areaBrush)

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path = linePath, brush = lineBrush, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(16f)))

            points.forEachIndexed { index, point ->
                val isSelected = selectedIndex == index
                val radius = if (isSelected) 12f else 7f
                drawCircle(color = Color.White.copy(alpha = if (isSelected) 0.9f else 0.5f), radius = radius + 3f, center = point)
                drawCircle(brush = lineBrush, radius = radius, center = point)
            }

            // --- Smart Label Logic ---
            val maxLabels = (size.width / with(density) { 70.dp.toPx() }).toInt().coerceAtMost(data.size)
            val step = (data.size - 1) / (maxLabels - 1).coerceAtLeast(1)
            val indicesToLabel = (0 until maxLabels).map { (it * step).coerceAtMost(data.size - 1) }.distinct()

            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            indicesToLabel.forEach { index ->
                val x = points[index].x
                drawContext.canvas.nativeCanvas.drawText(dateFormat.format(Date(data[index].date)), x, size.height + 60f, textPaint)
            }


            selectedIndex?.let { index ->
                val selectedPoint = points[index]
                val selectedWorkout = data[index]
                val tooltipText = "${selectedWorkout.weight} kg"

                val tooltipWidth = tooltipTextPaint.measureText(tooltipText) + 24.dp.toPx()
                val tooltipHeight = 40.dp.toPx()
                val tooltipRect = RoundRect(
                    left = (selectedPoint.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth),
                    top = selectedPoint.y - tooltipHeight - 12.dp.toPx(),
                    right = (selectedPoint.x + tooltipWidth / 2).coerceIn(tooltipWidth, size.width),
                    bottom = selectedPoint.y - 12.dp.toPx(),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawRoundRect(color = Color(0xFF1A0808), topLeft = Offset(tooltipRect.left, tooltipRect.top), size = Size(tooltipRect.width, tooltipRect.height), cornerRadius = tooltipRect.topLeftCornerRadius, alpha = 0.9f)
                drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
            }
        }
    }

object taskbarOverride {
        var shouldOverrideVisiblity = mutableStateOf(
            false
        )
    }
