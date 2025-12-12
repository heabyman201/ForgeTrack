package com.forgecompose.app_wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
val AppPrimary = Color(0xFFE53935)
val AppSecondary = Color(0xFF652121)
val AppTertiary = Color(0xFF2B0E0E)
val AppBackground = Color(0xFF120707)

internal val WearColorPalette = Colors(
    primary = AppPrimary,
    primaryVariant = AppSecondary,
    secondary = AppSecondary,
    secondaryVariant = AppTertiary,
    background = AppBackground,
    surface = AppTertiary,
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)
@Composable
fun WorkoutTrackerTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}