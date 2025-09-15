package com.forgecompose.app_wear.presentation


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color





// Core crimson palette
private val Crimson = Color(0xFFB71C1C)   // deep crimson
private val CrimsonLight = Color(0xFFEF5350)
private val CrimsonDark = Color(0xFF7F0000)

private val DarkColors = darkColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    secondary = CrimsonLight,
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFA726),         // warm amber accent
    onTertiary = Color.Black,
    surface = Color(0xFF101010),          // charcoal
    onSurface = Color(0xFFEFEFEF),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = CrimsonDark,
    onError = Color.White
)

@Composable
fun CrimsonWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
