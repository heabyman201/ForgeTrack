package com.forgecompose.workouttracker

import android.app.Application
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val Context.appearanceDataStore by preferencesDataStore("appearance_options")

@Immutable
data class AppearanceOptions(
    val selectedTheme: ColorThemeApp
) {
    companion object {
        val Defaults = AppearanceOptionsAppTheme(
            selectedTheme = ColorThemeApp.Default
        )
    }
}

enum class ColorTheme(val themeName: String, val colors: ColorSchemeAppTheme) {
    Default(
        "Default",
        ColorSchemeAppTheme(
            primary = Color(0xFFE53935),
            secondary = Color(0xFFD81B60),
            tertiary = Color(0xFF8E24AA),
            background = Color(0xFF121212)
        )
    ),
    Ocean(
        "Ocean",
        ColorSchemeAppTheme(
            primary = Color(0xFF03A9F4),
            secondary = Color(0xFF00BCD4),
            tertiary = Color(0xFF009688),
            background = Color(0xFF121212)
        )
    ),
    Forest(
        "Forest",
        ColorSchemeAppTheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF8BC34A),
            tertiary = Color(0xFFCDDC39),
            background = Color(0xFF121212)
        )
    )
}

@Immutable
data class ColorScheme(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color
)

object AppearanceOptionsManager {
    private val keySelectedTheme = stringPreferencesKey("selectedTheme")

    private val snapshot = MutableStateFlow(AppearanceOptionsAppTheme.Defaults)
    val current: StateFlow<AppearanceOptionsAppTheme> = snapshot

    private var initJob: Job? = null

    fun initialize(context: Context) {
        if (initJob != null) return
        initJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            context.appearanceDataStore.data
                .map { p ->
                    val themeName = p[keySelectedTheme] ?: AppearanceOptionsAppTheme.Defaults.selectedTheme.name
                    val theme = try {
                        ColorThemeApp.valueOf(themeName)
                    } catch (e: IllegalArgumentException) {
                        AppearanceOptionsAppTheme.Defaults.selectedTheme
                    }
                    AppearanceOptionsAppTheme(selectedTheme = theme)
                }
                .collectLatest { snapshot.value = it }
        }
    }

    fun flow(context: Context): Flow<AppearanceOptionsAppTheme> = current
}

class AppearanceViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    val options = AppearanceOptionsManagerAppTheme.flow(ctx).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceOptionsAppTheme.Defaults
    )
}


@Composable
fun CircularTimerProgressBar(
    progress: Float,
    hype: Float,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModelAppTheme = viewModel()
) {
    val appearanceOptions by appearanceViewModel.options.collectAsState()
    val colorScheme = appearanceOptions.selectedTheme.colors

    val STEP = 0.25f
    val lastStepIdx = remember { mutableIntStateOf(-1) }
    val lastProgress = remember { mutableStateOf(0f) }
    val ripple = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(progress) {
        val pNow = progress.coerceIn(0f, 1f)
        val pPrev = lastProgress.value
        lastProgress.value = pNow

        if (pNow > pPrev + 1e-4f) {
            val idx = (pNow / STEP).toInt()
            if (idx > lastStepIdx.intValue) {
                lastStepIdx.intValue = idx
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                launch {
                    ripple.snapTo(0f)
                    ripple.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                    )
                    ripple.snapTo(0f)
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 20.dp.toPx()
        val p = progress.coerceIn(0f, 1f)

        val safeInset = minOf(strokeWidth / 2f, size.minDimension / 2f - 1f)
        inset(safeInset) {
            val c = center
            val radius = size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0E0E10), Color(0xFF1A1A1F)),
                    center = c,
                    radius = radius + strokeWidth / 2f
                ),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            for (i in 0 until 60) {
                val angle = i * 6f
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val tickColor = Color.White.copy(alpha = if (isMajor) 0.35f else 0.18f)
                val startR = radius - strokeWidth / 2f
                val endR = startR + tickLen
                withTransform({ rotate(angle, c) }) {
                    drawLine(
                        color = tickColor,
                        start = Offset(c.x, c.y - startR),
                        end = Offset(c.x, c.y - endR),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (p > 0f) {
                val sweep = 360f * p
                val arcTopLeft = Offset(c.x - radius, c.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                drawArc(
                    color = Color.White.copy(alpha = 0.08f + 0.07f * hype.coerceIn(0f, 1f)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            colorScheme.secondary,
                            colorScheme.primary,
                            colorScheme.tertiary
                        ),
                        center = c
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val angleRad = Math.toRadians((sweep - 90f).toDouble()).toFloat()
                val capCenter = Offset(
                    x = c.x + radius * cos(angleRad),
                    y = c.y + radius * sin(angleRad)
                )
                val capHaloR = strokeWidth * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colorScheme.primary.copy(alpha = 0.7f), Color.Transparent),
                        center = capCenter,
                        radius = capHaloR
                    ),
                    radius = capHaloR,
                    center = capCenter
                )
                drawCircle(color = Color.White, radius = strokeWidth / 3f, center = capCenter)
            }

            val rv = ripple.value
            if (rv > 0f) {
                val startR = radius + strokeWidth * 0.1f
                val endR = radius * 1.6f
                val ringR = lerp(startR, endR, rv)
                val ringW = lerp(strokeWidth * 0.8f, strokeWidth * 0.2f, rv)
                val alpha = (1f - rv) * 0.35f

                drawCircle(
                    color = colorScheme.primary.copy(alpha = alpha),
                    radius = ringR,
                    style = Stroke(width = ringW, cap = StrokeCap.Round)
                )
            }
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}