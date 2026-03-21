package com.forgecompose.workouttracker.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFB71C1C),
    onPrimaryContainer = Color(0xFFFFEBEE),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFC62828),
    onSecondaryContainer = Color(0xFFFFEBEE),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1565C0),
    onTertiaryContainer = Color(0xFFE3F2FD),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFFC4C4C4),
    outline = Color(0xFF424242)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFFC62828),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFFB71C1C),
    tertiary = Color(0xFF1976D2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3F2FD),
    onTertiaryContainer = Color(0xFF0D47A1),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF495057),
    outline = Color(0xFFDEE2E6)
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = true,

    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}