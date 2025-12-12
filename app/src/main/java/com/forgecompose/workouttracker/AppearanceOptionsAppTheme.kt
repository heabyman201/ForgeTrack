package com.forgecompose.workouttracker

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.appearanceDataStore by preferencesDataStore("appearance_options")

@Immutable
data class AppearanceOptionsAppTheme(
    val selectedTheme: ColorThemeApp,
    val customColors: ColorSchemeAppTheme
) {
    val colors: ColorSchemeAppTheme
        get() = if (selectedTheme == ColorThemeApp.Custom) customColors else selectedTheme.colors

    companion object {
        val Defaults = AppearanceOptionsAppTheme(
            selectedTheme = ColorThemeApp.Default,
            customColors = ColorThemeApp.Default.colors
        )
    }
}

enum class ColorThemeApp(val themeName: String, val colors: ColorSchemeAppTheme) {
    Default(
        "Default",
        ColorSchemeAppTheme(
            primary = Color(0xFFEF5350),
            secondary = Color(0xFF210B0B),
            tertiary = Color(0xFF110000),
            background = Color(0xFF000000)
        )
    ),
    Ocean(
        "Ocean",
        ColorSchemeAppTheme(
            primary = Color(0xFF0277BD),
            secondary = Color(0xFF06121A),
            tertiary = Color(0xFF02070D),
            background = Color(0xFF000000)
        )
    ),
    DeepSea(
        "Deep Sea",
        ColorSchemeAppTheme(
            primary = Color(0xFF01579B),
            secondary = Color(0xFF05101A),
            tertiary = Color(0xFF02070D),
            background = Color(0xFF000205)
        )
    ),
    // Replaced Glacier with a softer version
    Frost(
        "Frost",
        ColorSchemeAppTheme(
            primary = Color(0xFF80DEEA),
            secondary = Color(0xFF003840),
            tertiary = Color(0xFF00252A),
            background = Color(0xFF001214)
        )
    ),
    Forest(
        "Forest",
        ColorSchemeAppTheme(
            primary = Color(0xFF66BB6A),
            secondary = Color(0xFF0E230E),
            tertiary = Color(0xFF030E03),
            background = Color(0xFF020A03)
        )
    ),
    Zen(
        "Zen",
        ColorSchemeAppTheme(
            primary = Color(0xFFA5D6A7),
            secondary = Color(0xFF1B301D),
            tertiary = Color(0xFF0E1A0F),
            background = Color(0xFF080F08)
        )
    ),
    Sunset(
        "Sunset",
        ColorSchemeAppTheme(
            primary = Color(0xFFD84315),
            secondary = Color(0xFF280C05),
            tertiary = Color(0xFF180A03),
            background = Color(0xFF000000)
        )
    ),
    Ember(
        "Ember",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF7043),
            secondary = Color(0xFF2D160E),
            tertiary = Color(0xFF1A0B06),
            background = Color(0xFF0D0402)
        )
    ),
    Campfire(
        "Campfire",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFB74D),
            secondary = Color(0xFF3E2008),
            tertiary = Color(0xFF241203),
            background = Color(0xFF140801)
        )
    ),
    Void(
        "Void",
        ColorSchemeAppTheme(
            primary = Color(0xFF90A4AE),
            secondary = Color(0xFF161C20),
            tertiary = Color(0xFF0B0E10),
            background = Color(0xFF000000)
        )
    ),
    Midnight(
        "Midnight",
        ColorSchemeAppTheme(
            primary = Color(0xFF7E57C2),
            secondary = Color(0xFF140D21),
            tertiary = Color(0xFF0A0415),
            background = Color(0xFF000000)
        )
    ),
    Twilight(
        "Twilight",
        ColorSchemeAppTheme(
            primary = Color(0xFF9FA8DA),
            secondary = Color(0xFF1A1C30),
            tertiary = Color(0xFF0E0F1A),
            background = Color(0xFF06060C)
        )
    ),
    Cyber(
        "Cyber",
        ColorSchemeAppTheme(
            primary = Color(0xFF00ACC1),
            secondary = Color(0xFF0D3236),
            tertiary = Color(0xFF05181A),
            background = Color(0xFF010809)
        )
    ),
    Synthwave(
        "Synthwave",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF4081),
            secondary = Color(0xFF330B1C),
            tertiary = Color(0xFF1A040D),
            background = Color(0xFF10002B)
        )
    ),
    Retro(
        "Retro",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFCA28),
            secondary = Color(0xFF382A02),
            tertiary = Color(0xFF211800),
            background = Color(0xFF120C00)
        )
    ),
    Noir(
        "Noir",
        ColorSchemeAppTheme(
            primary = Color(0xFFBDBDBD),
            secondary = Color(0xFF212121),
            tertiary = Color(0xFF121212),
            background = Color(0xFF000000)
        )
    ),
    Ink(
        "Ink",
        ColorSchemeAppTheme(
            primary = Color(0xFF78909C),
            secondary = Color(0xFF101518),
            tertiary = Color(0xFF080A0C),
            background = Color(0xFF020202)
        )
    ),
    Slate(
        "Slate",
        ColorSchemeAppTheme(
            primary = Color(0xFF78909C),
            secondary = Color(0xFF263238),
            tertiary = Color(0xFF101619),
            background = Color(0xFF0B0E0F)
        )
    ),
    Storm(
        "Storm",
        ColorSchemeAppTheme(
            primary = Color(0xFF546E7A),
            secondary = Color(0xFF1C2529),
            tertiary = Color(0xFF0E1315),
            background = Color(0xFF080B0C)
        )
    ),
    Mint(
        "Mint",
        ColorSchemeAppTheme(
            primary = Color(0xFF4DB6AC),
            secondary = Color(0xFF003028),
            tertiary = Color(0xFF001A15),
            background = Color(0xFF000D0B)
        )
    ),
    OldPaper(
        "Old Paper",
        ColorSchemeAppTheme(
            primary = Color(0xFFD7CCC8),
            secondary = Color(0xFF3E3532),
            tertiary = Color(0xFF231D1B),
            background = Color(0xFF14100F)
        )
    ),
    Coffee(
        "Coffee",
        ColorSchemeAppTheme(
            primary = Color(0xFFA1887F),
            secondary = Color(0xFF3E2723),
            tertiary = Color(0xFF261613),
            background = Color(0xFF120A08)
        )
    ),
    Vampire(
        "Vampire",
        ColorSchemeAppTheme(
            primary = Color(0xFFC62828),
            secondary = Color(0xFF290000),
            tertiary = Color(0xFF170000),
            background = Color(0xFF050000)
        )
    ),
    PastelNight(
        "Pastel Night",
        ColorSchemeAppTheme(
            primary = Color(0xFFCE93D8),
            secondary = Color(0xFF2D1B30),
            tertiary = Color(0xFF180D1A),
            background = Color(0xFF100812)
        )
    ),
    Custom(
        "Custom",
        ColorSchemeAppTheme(
            primary = Color.White,
            secondary = Color.Gray,
            tertiary = Color.DarkGray,
            background = Color.Black
        )
    )
}

@Immutable
data class ColorSchemeAppTheme(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color
)

object AppearanceOptionsManagerAppTheme {
    private val keySelectedTheme = stringPreferencesKey("selectedTheme")
    private val keyCustomPrimary = longPreferencesKey("custom_primary")
    private val keyCustomSecondary = longPreferencesKey("custom_secondary")
    private val keyCustomTertiary = longPreferencesKey("custom_tertiary")
    private val keyCustomBackground = longPreferencesKey("custom_background")

    fun flow(context: Context): Flow<AppearanceOptionsAppTheme> {
        return context.appearanceDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val themeName = preferences[keySelectedTheme] ?: AppearanceOptionsAppTheme.Defaults.selectedTheme.name
                val theme = try {
                    ColorThemeApp.valueOf(themeName)
                } catch (e: IllegalArgumentException) {
                    ColorThemeApp.Default
                }

                val defaultCustom = ColorThemeApp.Custom.colors
                val customPrimary = preferences[keyCustomPrimary]?.let { Color(it.toULong()) } ?: defaultCustom.primary
                val customSecondary = preferences[keyCustomSecondary]?.let { Color(it.toULong()) } ?: defaultCustom.secondary
                val customTertiary = preferences[keyCustomTertiary]?.let { Color(it.toULong()) } ?: defaultCustom.tertiary
                val customBackground = preferences[keyCustomBackground]?.let { Color(it.toULong()) } ?: defaultCustom.background

                AppearanceOptionsAppTheme(
                    selectedTheme = theme,
                    customColors = ColorSchemeAppTheme(customPrimary, customSecondary, customTertiary, customBackground)
                )
            }
    }

    suspend fun set(context: Context, v: AppearanceOptionsAppTheme) {
        context.appearanceDataStore.edit { p ->
            p[keySelectedTheme] = v.selectedTheme.name
            p[keyCustomPrimary] = v.customColors.primary.value.toLong()
            p[keyCustomSecondary] = v.customColors.secondary.value.toLong()
            p[keyCustomTertiary] = v.customColors.tertiary.value.toLong()
            p[keyCustomBackground] = v.customColors.background.value.toLong()
        }
    }
}

class AppearanceViewModelAppTheme(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    val options = AppearanceOptionsManagerAppTheme.flow(ctx).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceOptionsAppTheme.Defaults
    )

    fun update(transform: (AppearanceOptionsAppTheme) -> AppearanceOptionsAppTheme) {
        val next = transform(options.value)
        viewModelScope.launch(Dispatchers.IO) {
            AppearanceOptionsManagerAppTheme.set(ctx, next)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    navController: NavController,
    vm: AppearanceViewModelAppTheme = viewModel(),
) {
    val opts by vm.options.collectAsState()
    val themeColors = opts.colors

    val dynamicGradientBrush = remember(themeColors) {
        Brush.radialGradient(
            colors = listOf(
                themeColors.secondary,
                themeColors.tertiary,
                themeColors.background
            ),
            radius = 1500f,
            center = Offset(0.5f, 0.4f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(themeColors.background)
                    drawRect(dynamicGradientBrush, alpha = 0.6f)
                }
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Appearance", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SettingsSectionCardHealth(
                        title = "App Appearance",
                        themeColors = themeColors
                    ) {
                        ColorThemeApp.values().forEach { theme ->
                            ColorThemeRow(
                                theme = theme,
                                isSelected = opts.selectedTheme == theme,
                                onSelected = { vm.update { it.copy(selectedTheme = theme) } },
                                accentColor = if (theme == ColorThemeApp.Custom && opts.selectedTheme == ColorThemeApp.Custom) opts.customColors.primary else theme.colors.primary
                            )
                        }
                    }
                }

                if (opts.selectedTheme == ColorThemeApp.Custom) {
                    item {
                        CustomThemeEditor(
                            currentColors = opts.customColors,
                            onColorChanged = { newColors ->
                                vm.update { it.copy(customColors = newColors) }
                            },
                            uiAccentColor = themeColors.primary
                        )
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ColorThemeRow(
    theme: ColorThemeApp,
    isSelected: Boolean,
    onSelected: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.themeName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (theme == ColorThemeApp.Custom) {
                    Text("Tap to edit", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                } else {
                    ColorCircle(color = theme.colors.primary)
                    ColorCircle(color = theme.colors.secondary)
                    ColorCircle(color = theme.colors.tertiary)
                }
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CustomThemeEditor(
    currentColors: ColorSchemeAppTheme,
    onColorChanged: (ColorSchemeAppTheme) -> Unit,
    uiAccentColor: Color
) {
    var editingColorType by remember { mutableStateOf<String?>(null) }
    var colorToEdit by remember { mutableStateOf(Color.White) }

    if (editingColorType != null) {
        ColorPickerDialog(
            initialColor = colorToEdit,
            onDismiss = { editingColorType = null },
            onColorSelected = { newColor ->
                val newScheme = when (editingColorType) {
                    "Primary" -> currentColors.copy(primary = newColor)
                    "Secondary" -> currentColors.copy(secondary = newColor)
                    "Tertiary" -> currentColors.copy(tertiary = newColor)
                    "Background" -> currentColors.copy(background = newColor)
                    else -> currentColors
                }
                onColorChanged(newScheme)
                editingColorType = null
            },
            title = "Edit $editingColorType"
        )
    }

    SettingsSectionCardHealth(
        title = "Edit Custom Theme",
        themeColors = currentColors
    ) {
        CustomColorRow("Primary", currentColors.primary) {
            colorToEdit = currentColors.primary
            editingColorType = "Primary"
        }
        CustomColorRow("Secondary", currentColors.secondary) {
            colorToEdit = currentColors.secondary
            editingColorType = "Secondary"
        }
        CustomColorRow("Tertiary", currentColors.tertiary) {
            colorToEdit = currentColors.tertiary
            editingColorType = "Tertiary"
        }
        CustomColorRow("Background", currentColors.background) {
            colorToEdit = currentColors.background
            editingColorType = "Background"
        }
    }
}

@Composable
private fun CustomColorRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorCircle(color = color)
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    title: String
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                )

                Column {
                    Text("Red: ${(red * 255).toInt()}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.7f))
                    )

                    Text("Green: ${(green * 255).toInt()}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.7f))
                    )

                    Text("Blue: ${(blue * 255).toInt()}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.7f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentColor) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun ColorCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
private fun SettingsSectionCardHealth(
    title: String,
    themeColors: ColorSchemeAppTheme,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.secondary.copy(alpha = 0.8f),
                        themeColors.tertiary.copy(alpha = 0.9f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.primary.copy(alpha = 0.4f),
                        themeColors.secondary.copy(alpha = 0.2f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx))
                    drawRoundRect(brush = borderBrush, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRpx))
                }
            }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = themeColors.primary.copy(alpha = 0.3f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}