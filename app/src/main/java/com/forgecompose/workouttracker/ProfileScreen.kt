package com.forgecompose.workouttracker

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
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.forgecompose.workouttracker.ui.components.blurAnim.intensity
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.format.TextStyle
import java.net.URL
import androidx.core.content.edit
import kotlinx.coroutines.flow.collect



data class PersonalRecord(val exerciseName: String, val maxWeight: Double)

class SurveyPromptManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_TAKEN = "last_survey_timestamp"
    private val COOLDOWN_MS = Duration.ofDays(7).toMillis()

    fun shouldShowPrompt(): Boolean {
        val lastTaken = prefs.getLong(KEY_LAST_TAKEN, 0L)
        val now = System.currentTimeMillis()
        return (now - lastTaken) > COOLDOWN_MS || lastTaken == 0L
    }

    fun markSurveyTaken() {
        prefs.edit { putLong(KEY_LAST_TAKEN, System.currentTimeMillis()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: WorkoutListViewModel,
    badgeViewModel: BadgeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager(context) }
    val surveyManager = remember { SurveyPromptManager(context) }
    val auth = remember { Firebase.auth }
    val credentialManager = remember { CredentialManager.create(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val isGoogleLinked = remember(currentUser) { isGoogleProviderLinked(currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Theme Hooks
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    val userAge = remember { prefsManager.getAge() }
    val userHeight = remember { prefsManager.getHeight() }
    val userWeight = remember { prefsManager.getWeight() }
    val userName = remember { prefsManager.getName() }
    val displayName = remember(currentUser, userName) {
        currentUser?.displayName?.takeIf { it.isNotBlank() } ?: userName
    }
    val profilePhotoUrl = remember(currentUser) { currentUser?.photoUrl?.toString() }
    val stages = rememberColdStartStages()
    val badges by badgeViewModel.badges.collectAsState()

    var showSurveyPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showSurveyPrompt = surveyManager.shouldShowPrompt()
    }

    val (personalRecords, recentWorkouts) = remember(uiState) {
        if (uiState is WorkoutListUiState.Success) {
            val workouts = (uiState as WorkoutListUiState.Success).workouts
            val prs = workouts
                .asSequence()
                .filter { (it.weight ?: 0.0) > 0.0 }
                .groupBy { it.name }
                .map { (name, workoutList) -> PersonalRecord(name, workoutList.maxOf { it.weight!! }) }
                .sortedByDescending { it.maxWeight }
                .take(4)
            val recent = workouts.take(4)
            prs to recent
        } else {
            emptyList<PersonalRecord>() to emptyList<Workout>()
        }
    }



    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) { Brush.linearGradient(colors = introColors) }
    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")

    LaunchedEffect(Unit) {
        showIntro = false
        Firebase.crashlytics.setCustomKey("current_screen", "Profile Screen")
    }

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles
    val shouldAnimate = stages.afterFirstFrame
    var animationClock by remember { mutableStateOf(0f) }

    LaunchedEffect(shouldAnimate, movingEffectsEnabled) {
        if (shouldAnimate && movingEffectsEnabled) {
            var lastFrameTime = 0L
            while (true) {
                val currentTime = withFrameNanos { it }
                if (lastFrameTime != 0L) {
                    val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                    animationClock += deltaTime
                }
                lastFrameTime = currentTime
                delay(42)
            }
        }
    }

    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )
    val blurToApply = remember(blurAnim) { if (blurAnim < 0.6.dp) 0.dp else blurAnim.coerceAtMost(60.dp) }
    val listState = rememberLazyListState()
    var topBarVisible by remember { mutableStateOf(true) }
    var lastScrollPosition by remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val currentPosition = index * 10_000 + offset
                topBarVisible = currentPosition <= lastScrollPosition || currentPosition == 0
                lastScrollPosition = currentPosition
            }
    }

    val nameStyle = MaterialTheme.typography.headlineMedium.copy(
        shadow = Shadow(color = Color.White.copy(alpha = 0.3f), blurRadius = 8f)
    )
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val forgeBackdrop = rememberForgeBackdrop()
    CompositionLocalProvider(LocalForgeBackdrop provides forgeBackdrop) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurToApply > 0.dp) {
                    Modifier.blur(blurToApply)
                } else Modifier
            )
    ) {
        AnimatedBackdrop(
            modifier = Modifier
                .matchParentSize()
                .forgeBackdropSource(forgeBackdrop),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves = movingEffectsEnabled,
            enableAnimation = movingEffectsEnabled,
        )
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = topBarVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Profile",
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            theme.primary,
                                            theme.primary.copy(alpha = 1f),
                                            Color.White
                                        )
                                    ),
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate("Settings") },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(theme.secondary.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Pass theme colors to Avatar
                            GlowingAvatar(
                                icon = Icons.Default.Person,
                                primaryColor = theme.primary,
                                secondaryColor = theme.background,
                                photoUrl = profilePhotoUrl
                            )
                            Text(
                                displayName,
                                style = nameStyle,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (showSurveyPrompt && stages.after600ms) {
                        item {
                            SurveyPromptCard(
                                onClick = {
                                    surveyManager.markSurveyTaken()
                                    showSurveyPrompt = false
                                    navController.navigate("Survey")
                                }
                            )
                        }
                    }

                    item {
                        ProfileSectionCard(
                            title = "Badges",
                            themeColors = theme, // Pass theme
                            action = {
                                CardActionButton("View All") { navController.navigate("badges") }
                            }
                        ) {
                            BadgeSection(
                                badges = badges,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (stages.after200ms && personalRecords.isNotEmpty()) {
                        item {
                            ProfileSectionCard(
                                title = "Personal Records",
                                themeColors = theme, // Pass theme
                                action = {
                                    CardActionButton("More") { navController.navigate("RepMax") }
                                }
                            ) {
                                personalRecords.forEach { pr ->
                                    ProfileStatRow(
                                        label = pr.exerciseName,
                                        value = "${String.format("%.1f", pr.maxWeight)} kg",
                                        icon = Icons.Default.Stars,
                                        iconTint = Color(0xFFfce18a), // Gold for Stars logic usually stays
                                        textColor = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (stages.after100ms && recentWorkouts.isNotEmpty()) {
                        item {
                            ProfileSectionCard(
                                title = "Recent Activity",
                                themeColors = theme, // Pass theme
                                action = {
                                    CardActionButton("View History") { navController.navigate("WorkoutHistory") }
                                }
                            ) {
                                recentWorkouts.forEach { workout ->
                                    val date = remember(workout.date) { dateFormatter.format(Date(workout.date)) }
                                    // Dynamic tint for History icon
                                    ProfileStatRow(
                                        label = workout.name,
                                        value = date,
                                        icon = Icons.Default.History,
                                        iconTint = theme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (stages.after400ms) {
                        item {
                            ProfileSectionCard(
                                title = "Body Stats",
                                themeColors = theme, // Pass theme
                                action = {
                                    CardActionButton("Edit Profile") { navController.navigate("EditUserStats") }
                                }
                            ) {
                                ProfileStatRow("Age", userAge, Icons.Default.Person, iconTint = theme.primary)
                                ProfileStatRow("Height", "$userHeight cm", Icons.Default.Height, iconTint = theme.primary)
                                ProfileStatRow("Weight", "$userWeight kg", Icons.Default.MonitorWeight, iconTint = theme.primary)
                                ProfileStatRow("Experience", prefsManager.getExperience(), Icons.Default.DataExploration, iconTint = theme.primary)
                            }
                        }
                    }
                    item {
                        ProfileSectionCard(
                            title = "Account",
                            themeColors = theme
                        ) {
                            val actionLabel = if (isGoogleLinked) "Google Linked" else "Link Google Account"
                            Button(
                                onClick = {
                                    if (isGoogleLinked) {
                                        Toast.makeText(context, "Google account already linked", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val webClientIdRes = context.resources.getIdentifier(
                                        "default_web_client_id",
                                        "string",
                                        context.packageName
                                    )
                                    if (webClientIdRes == 0) {
                                        Toast.makeText(context, "Missing default_web_client_id", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val webClientId = context.getString(webClientIdRes).trim()
                                    if (webClientId.isBlank()) {
                                        Toast.makeText(context, "Invalid Google client id", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val activity = context as? android.app.Activity
                                    if (activity == null) {
                                        Toast.makeText(context, "Unable to open Google sign-in on this screen", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val googleSignInOption = GetSignInWithGoogleOption.Builder(webClientId)
                                        .setNonce(System.currentTimeMillis().toString())
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleSignInOption)
                                        .build()

                                    val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setNonce(System.currentTimeMillis().toString())
                                        .build()
                                    val fallbackRequest = GetCredentialRequest.Builder()
                                        .addCredentialOption(fallbackGoogleIdOption)
                                        .build()

                                    scope.launch {
                                        try {
                                            val result = try {
                                                credentialManager.getCredential(activity, request)
                                            } catch (e: GetCredentialException) {
                                                Log.w("ProfileScreen", "Primary Google sign-in failed: ${e.type}", e)
                                                credentialManager.getCredential(activity, fallbackRequest)
                                            }
                                            val rawCredential = result.credential
                                            if (rawCredential !is CustomCredential ||
                                                (rawCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL &&
                                                    rawCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)
                                            ) {
                                                Log.w("ProfileScreen", "Unexpected credential type: ${rawCredential.type}")
                                                Toast.makeText(context, "Google credential unavailable", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            val googleCredential = GoogleIdTokenCredential.createFrom(rawCredential.data)
                                            val idToken = googleCredential.idToken
                                            if (idToken.isBlank()) {
                                                Toast.makeText(context, "Google token is missing", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                                            val user = auth.currentUser
                                            when {
                                                user == null -> {
                                                    auth.signInWithCredential(credential)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Signed in with Google", Toast.LENGTH_SHORT).show()
                                                            Firebase.crashlytics.setUserId(it.user?.uid ?: "")
                                                        }
                                                        .addOnFailureListener {
                                                            Log.e("ProfileScreen", "Firebase Google sign-in failed", it)
                                                            Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_LONG).show()
                                                        }
                                                }
                                                user.isAnonymous -> {
                                                    user.linkWithCredential(credential)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Google account linked", Toast.LENGTH_SHORT).show()
                                                            Firebase.crashlytics.setUserId(it.user?.uid ?: "")
                                                        }
                                                        .addOnFailureListener { err ->
                                                            if (err is FirebaseAuthUserCollisionException) {
                                                                auth.signInWithCredential(credential)
                                                                    .addOnSuccessListener {
                                                                        Toast.makeText(context, "Signed in to existing Google account", Toast.LENGTH_SHORT).show()
                                                                        Firebase.crashlytics.setUserId(it.user?.uid ?: "")
                                                                    }
                                                                    .addOnFailureListener {
                                                                        Log.e("ProfileScreen", "Existing Google account sign-in failed", it)
                                                                        Toast.makeText(context, "Unable to link Google account", Toast.LENGTH_LONG).show()
                                                                    }
                                                            } else {
                                                                Log.e("ProfileScreen", "Anonymous Google link failed", err)
                                                                Toast.makeText(context, "Unable to link Google account", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                }
                                                isGoogleProviderLinked(user) -> {
                                                    Toast.makeText(context, "Google account already linked", Toast.LENGTH_SHORT).show()
                                                }
                                                else -> {
                                                    user.linkWithCredential(credential)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Google account linked", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .addOnFailureListener {
                                                            Log.e("ProfileScreen", "Google account link failed", it)
                                                            Toast.makeText(context, "Unable to link Google account", Toast.LENGTH_LONG).show()
                                                        }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ProfileScreen", "Google sign-in failed", e)
                                            Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.secondary.copy(alpha = if (isGoogleLinked) 0.35f else 0.55f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(actionLabel, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

            }
        }

            FloatingTaskbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navController = navController,
                cornerRadius = 32.dp,
                iconAlpha = 1f,
                uiState = uiState
            )

    }
    }
}

// ... [SurveyPromptCard remains the same - Gold is usually a distinct CTA color] ...
@Composable
fun SurveyPromptCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .clip(RoundedCornerShape(24.dp))
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = listOf(
                      appearanceOptions.colors.primary.copy(alpha = 0.15f),
                        appearanceOptions.colors.primary.copy(alpha = 0.05f)
                    )
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        appearanceOptions.colors.primary.copy(alpha = glowAlpha),
                        appearanceOptions.colors.primary.copy(alpha = glowAlpha * 0.7f)
                    )
                )

                onDrawBehind {
                    drawRoundRect(brush, cornerRadius = CornerRadius(24.dp.toPx()))
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(appearanceOptions.colors.background.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint =     appearanceOptions.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Update Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Refresh your recovery goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = appearanceOptions.colors.primary
            )
        }
    }
}


@Composable
private fun GlowingAvatar(
    icon: ImageVector,
    primaryColor: Color,
    secondaryColor: Color,
    photoUrl: String? = null
) {
    val profileBitmap by produceState<Bitmap?>(initialValue = null, photoUrl) {
        value = if (photoUrl.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { decodeAvatarSampled(photoUrl) }.getOrNull()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .drawWithCache {
                val bgBrush = Brush.radialGradient(
                    0.0f to secondaryColor.copy(alpha = 0.72f),
                    0.55f to secondaryColor.copy(alpha = 0.34f),
                    1.0f to Color.Transparent,
                    center = Offset(size.width * 0.42f, size.height * 0.35f),
                    radius = size.minDimension * 2.2f
                )
                // Border uses Primary
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        secondaryColor.copy(alpha = 0.3f)
                    )
                )
                onDrawBehind {
                    drawCircle(bgBrush)
                    drawCircle(borderBrush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (profileBitmap != null) {
            Image(
                bitmap = profileBitmap!!.asImageBitmap(),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "User Avatar",
                tint = primaryColor, // Use Primary for the Icon
                modifier = Modifier
                    .size(60.dp)
                    .drawWithCache {
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                            radius = size.minDimension
                        )
                        onDrawBehind {
                            drawCircle(glowBrush)
                        }
                    }
            )
        }
    }
}

// Decoded avatar is rendered at ~108.dp; sampling keeps memory bounded for large source images.
private fun decodeAvatarSampled(url: String, targetPx: Int = 512): Bitmap? {
    val bytes = URL(url).openStream().use { it.readBytes() }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetPx &&
        bounds.outHeight / (sample * 2) >= targetPx) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

@Composable
private fun ProfileSectionCard(
    title: String,
    themeColors: ColorSchemeAppTheme,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                // Dynamic Background based on theme
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.background.copy(alpha = 0.6f),
                        themeColors.background.copy(alpha = 0.8f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                // Dynamic Border based on theme
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.primary.copy(alpha = 0.3f),
                        themeColors.secondary.copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            action?.invoke()
        }

        // Dynamic Divider
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = themeColors.primary.copy(alpha = 0.3f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun CardActionButton(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    TextButton(
        onClick = {
            onClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileStatRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFFFF3B30), // Default value if not provided
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

private fun isGoogleProviderLinked(user: com.google.firebase.auth.FirebaseUser?): Boolean {
    if (user == null || user.isAnonymous) return false
    return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
}
