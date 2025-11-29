package com.forgecompose.workouttracker

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.time.LocalTime

@Composable
fun BadgesScreen(
    navController: NavController,
    badgeViewModel: BadgeViewModel
) {
    val context = LocalContext.current

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val badges by badgeViewModel.badges.collectAsState()

    val unlocked = remember(badges) { badges.filter { it.isUnlocked } }
    val inProgress = remember(badges) { badges.filter { !it.isUnlocked } }

    val totalCount = badges.size
    val unlockedCount = unlocked.size
    val showIntroState = remember { mutableStateOf(true) }
    val showIntro by showIntroState
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(700, easing = LinearEasing), label = "introProgress")

    // --- Dynamic Intro Colors ---
    val introColors = remember(theme) {
        listOf(
            theme.secondary.copy(alpha = 0.8f),
            theme.tertiary,
            theme.background,
            theme.background
        )
    }
    val introBrush = remember(introColors) { Brush.horizontalGradient(colors = introColors) }

    val performanceOptions by PerformanceOptionsManager.flow(context).collectAsState(initial = PerformanceOptions.Defaults)
    val movingEffectsEnabled = performanceOptions.movingGradientAndParticles

    LaunchedEffect(Unit) {
        showIntroState.value = false
        Firebase.crashlytics.setCustomKey("current_screen", "Badges screen")
    }

    Scaffold(
        topBar = {
            BadgesTopBar(
                onBack = { navController.popBackStack() },
                theme = theme
            )
        },
        containerColor = Color.Transparent,

        ) { padding ->
        AnimatedBackdrop(
            modifier = Modifier.padding(padding),
            introBrush = introBrush,
            introAlpha = 1f - introProgress,
            enableWaves =  movingEffectsEnabled,
            enableAnimation =  movingEffectsEnabled
        )
        if (badges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No badges yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.primary.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    BadgesStatsCard(
                        total = totalCount,
                        unlocked = unlockedCount,
                        theme = theme
                    )
                }

                if (unlocked.isNotEmpty()) {
                    item {
                        SectionTitleBadge(
                            title = "Unlocked",
                            subtitle = "Badges you've earned.",
                            theme = theme
                        )
                    }

                    items(unlocked, key = { it.id }) { badge ->
                        BadgeCard(
                            badge = badge,
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (inProgress.isNotEmpty()) {
                    item {
                        SectionTitleBadge(
                            title = "In Progress",
                            subtitle = "", // Empty subtitle for in-progress section
                            theme = theme
                        )
                    }

                    items(inProgress, key = { it.id }) { badge ->
                        BadgeCard(
                            badge = badge,
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgesTopBar(
    onBack: () -> Unit,
    theme: ColorSchemeAppTheme
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Badges",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.primary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun BadgesStatsCard(
    total: Int,
    unlocked: Int,
    modifier: Modifier = Modifier,
    theme: ColorSchemeAppTheme
) {
    val cardShape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            theme.secondary.copy(alpha = 0.35f),
                            theme.tertiary.copy(alpha = 0.16f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Badge Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.primary
                    )
                    Text(
                        text = "Keep logging workouts to unlock more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primary.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$unlocked / $total",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.primary
                    )
                    Text(
                        text = "Unlocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.primary.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitleBadge(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    theme: ColorSchemeAppTheme
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = theme.primary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = theme.primary.copy(alpha = 0.7f)
            )
        }
    }
}