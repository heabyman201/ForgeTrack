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
enum class ThemeCharacter(val label: String) {
    ENERGETIC("Energetic"),
    DARK("Dark"),
    MYSTERIOUS("Mysterious"),
    NOIR("Noir"),
    LIGHT("Light"),
    COLOURFUL("Colourful")
}
@Immutable
data class AppearanceOptionsAppTheme(
    val selectedTheme: ColorThemeApp,
    val customColors: ColorSchemeAppTheme,
    val themeCharacter: ThemeCharacter = ThemeCharacter.ENERGETIC
) {
    val colors: ColorSchemeAppTheme
        get() = if (selectedTheme == ColorThemeApp.Custom) customColors else selectedTheme.colors

    companion object {
        val Defaults = AppearanceOptionsAppTheme(
            selectedTheme = ColorThemeApp.Default,
            customColors = ColorThemeApp.Default.colors,
            themeCharacter = ThemeCharacter.ENERGETIC
        )
    }
}

enum class ColorThemeApp(val themeName: String, val colors: ColorSchemeAppTheme, val character: ThemeCharacter = ThemeCharacter.ENERGETIC) {
    Default(
        "Default",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF5252),
            secondary = Color(0xFF1E1E1E),
            tertiary = Color(0xFF2D1414),
            background = Color(0xFF090909)
        ),
        ThemeCharacter.ENERGETIC
    ),
    Ocean(
        "Ocean",
        ColorSchemeAppTheme(
            primary = Color(0xFF00D4FF),
            secondary = Color(0xFF141E22),
            tertiary = Color(0xFF0A2E3D),
            background = Color(0xFF06090B)
        ),
        ThemeCharacter.DARK

    ),
    DeepSea(
        "Deep Sea",
        ColorSchemeAppTheme(
            primary = Color(0xFF00FFD2),
            secondary = Color(0xFF141F1D),
            tertiary = Color(0xFF0A2E29),
            background = Color(0xFF06090A)
        ),
        ThemeCharacter.DARK
    ),
    Frost(
        "Frost",
        ColorSchemeAppTheme(
            primary = Color(0xFFB8F0FF),
            secondary = Color(0xFF181E20),
            tertiary = Color(0xFF1E2C32),
            background = Color(0xFF07090A)
        ),
        ThemeCharacter.DARK
    ),
    Forest(
        "Forest",
        ColorSchemeAppTheme(
            primary = Color(0xFF52D468),
            secondary = Color(0xFF161E18),
            tertiary = Color(0xFF14301D),
            background = Color(0xFF070A08)
        ),
        ThemeCharacter.DARK
    ),
    Zen(
        "Zen",
        ColorSchemeAppTheme(
            primary = Color(0xFFB8C9A3),
            secondary = Color(0xFF1A1E18),
            tertiary = Color(0xFF252D1E),
            background = Color(0xFF090A08)
        )
        ,
        ThemeCharacter.NOIR

    ),
    Sunset(
        "Sunset",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFB627),
            secondary = Color(0xFF1E1B14),
            tertiary = Color(0xFF301E10),
            background = Color(0xFF0A0704)
        )
        ,
        ThemeCharacter.COLOURFUL

    ),
    Ember(
        "Ember",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFC107),
            secondary = Color(0xFF1E1A12),
            tertiary = Color(0xFF301F0C),
            background = Color(0xFF0A0703)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Campfire(
        "Campfire",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFEB3B),
            secondary = Color(0xFF1E1D14),
            tertiary = Color(0xFF302A0F),
            background = Color(0xFF0A0903)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Void(
        "Void",
        ColorSchemeAppTheme(
            primary = Color(0xFFE8EDF2),
            secondary = Color(0xFF1C1E20),
            tertiary = Color(0xFF272D32),
            background = Color(0xFF08090A)
        ),
        ThemeCharacter.DARK
    ),
    Midnight(
        "Midnight",
        ColorSchemeAppTheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF1B1822),
            tertiary = Color(0xFF251733),
            background = Color(0xFF09070C)
        ),
        ThemeCharacter.MYSTERIOUS
    ),
    Twilight(
        "Twilight",
        ColorSchemeAppTheme(
            primary = Color(0xFFC7D2FE),
            secondary = Color(0xFF1A1A22),
            tertiary = Color(0xFF281A33),
            background = Color(0xFF09080C)
        ),
        ThemeCharacter.MYSTERIOUS
    ),
    Cyber(
        "Cyber",
        ColorSchemeAppTheme(
            primary = Color(0xFF00FFAA),
            secondary = Color(0xFF141E1B),
            tertiary = Color(0xFF0F2E2A),
            background = Color(0xFF06090A)
        ),
        ThemeCharacter.ENERGETIC
    ),
    Synthwave(
        "Synthwave",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF2D78),
            secondary = Color(0xFF1E1419),
            tertiary = Color(0xFF301022),
            background = Color(0xFF0A0407)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Retro(
        "Retro",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFBF00),
            secondary = Color(0xFF1E1B14),
            tertiary = Color(0xFF302510),
            background = Color(0xFF0A0804)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Noir(
        "Noir",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFF1E1E1E),
            tertiary = Color(0xFF2C2C2C),
            background = Color(0xFF070707)
        ),
        ThemeCharacter.NOIR
    ),
    Ink(
        "Ink",
        ColorSchemeAppTheme(
            primary = Color(0xFFF5F0E8),
            secondary = Color(0xFF1E1D1A),
            tertiary = Color(0xFF2C2922),
            background = Color(0xFF0A0908)
        ),
        ThemeCharacter.NOIR
    ),
    Slate(
        "Slate",
        ColorSchemeAppTheme(
            primary = Color(0xFFAEC6CF),
            secondary = Color(0xFF1A1E20),
            tertiary = Color(0xFF242D32),
            background = Color(0xFF08090A)
        ),
        ThemeCharacter.DARK
    ),
    Storm(
        "Storm",
        ColorSchemeAppTheme(
            primary = Color(0xFFB0C4D8),
            secondary = Color(0xFF1A1D20),
            tertiary = Color(0xFF232B33),
            background = Color(0xFF08090B)
        ),
        ThemeCharacter.DARK
    ),
    Mint(
        "Mint",
        ColorSchemeAppTheme(
            primary = Color(0xFF3DFFC0),
            secondary = Color(0xFF141E1B),
            tertiary = Color(0xFF0F2E27),
            background = Color(0xFF060A09)
        ),
        ThemeCharacter.ENERGETIC
    ),
    OldPaper(
        "Old Paper",
        ColorSchemeAppTheme(
            primary = Color(0xFFE8C47A),
            secondary = Color(0xFF1E1B15),
            tertiary = Color(0xFF302616),
            background = Color(0xFF0C0906)
        ),
        ThemeCharacter.NOIR
    ),
    Coffee(
        "Coffee",
        ColorSchemeAppTheme(
            primary = Color(0xFFD4956A),
            secondary = Color(0xFF1E1813),
            tertiary = Color(0xFF301F14),
            background = Color(0xFF0A0705)
        ),
        ThemeCharacter.NOIR
    ),
    Vampire(
        "Vampire",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF1A1A),
            secondary = Color(0xFF1E1414),
            tertiary = Color(0xFF301010),
            background = Color(0xFF0A0505)
        ),
        ThemeCharacter.DARK
    ),
    PastelNight(
        "Pastel Night",
        ColorSchemeAppTheme(
            primary = Color(0xFFCFB7FF),
            secondary = Color(0xFF1B1A22),
            tertiary = Color(0xFF271E33),
            background = Color(0xFF09080C)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Neon(
        "Neon",
        ColorSchemeAppTheme(
            primary = Color(0xFF39FF14),
            secondary = Color(0xFF161E14),
            tertiary = Color(0xFF153010),
            background = Color(0xFF070A06)
        ),
        ThemeCharacter.ENERGETIC
    ),
    Galaxy(
        "Galaxy",
        ColorSchemeAppTheme(
            primary = Color(0xFFD4A8FF),
            secondary = Color(0xFF1B1822),
            tertiary = Color(0xFF261A33),
            background = Color(0xFF09070C)
        ),
        ThemeCharacter.MYSTERIOUS
    ),
    Abyss(
        "Abyss",
        ColorSchemeAppTheme(
            primary = Color(0xFF00FFE5),
            secondary = Color(0xFF141E1E),
            tertiary = Color(0xFF0A2E2C),
            background = Color(0xFF06090A)
        ),
        ThemeCharacter.DARK
    ),
    Volcano(
        "Volcano",
        ColorSchemeAppTheme(
            primary = Color(0xFFFF4500),
            secondary = Color(0xFF1E1612),
            tertiary = Color(0xFF30160A),
            background = Color(0xFF0A0503)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Aurora(
        "Aurora",
        ColorSchemeAppTheme(
            primary = Color(0xFF00FF88),
            secondary = Color(0xFF141E1A),
            tertiary = Color(0xFF0F2E22),
            background = Color(0xFF06090A)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Matrix(
        "Matrix",
        ColorSchemeAppTheme(
            primary = Color(0xFF00FF41),
            secondary = Color(0xFF141E16),
            tertiary = Color(0xFF0F3018),
            background = Color(0xFF060A07)
        ),
        ThemeCharacter.MYSTERIOUS
    ),
    Lavender(
        "Lavender",
        ColorSchemeAppTheme(
            primary = Color(0xFFE0AAFF),
            secondary = Color(0xFF1C1922),
            tertiary = Color(0xFF281A33),
            background = Color(0xFF09070C)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Gold(
        "Gold",
        ColorSchemeAppTheme(
            primary = Color(0xFFFFD700),
            secondary = Color(0xFF1E1C14),
            tertiary = Color(0xFF302810),
            background = Color(0xFF0A0804)
        ),
        ThemeCharacter.COLOURFUL
    ),
    Custom(
        "Custom",
        ColorSchemeAppTheme(
            primary = Color.White,
            secondary = Color.Gray,
            tertiary = Color.DarkGray,
            background = Color.Black
        ),
        ThemeCharacter.LIGHT
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

    // null = show all characters
    var selectedCharacter by remember { mutableStateOf<ThemeCharacter?>(null) }
    val visibleThemes = remember(selectedCharacter) {
        ColorThemeApp.entries.filter { theme ->
            selectedCharacter == null ||
                theme == ColorThemeApp.Custom ||
                theme.character == selectedCharacter
        }
    }

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
                    CharacterFilterChips(
                        selectedCharacter = selectedCharacter,
                        onCharacterSelected = { selectedCharacter = it },
                        accentColor = themeColors.primary
                    )
                }

                item {
                    SettingsSectionCardHealth(
                        title = "App Appearance",
                        themeColors = themeColors
                    ) {
                        visibleThemes.forEach { theme ->
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CharacterFilterChips(
    selectedCharacter: ThemeCharacter?,
    onCharacterSelected: (ThemeCharacter?) -> Unit,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Filter by character",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CharacterFilterChip(
                label = "All",
                selected = selectedCharacter == null,
                accentColor = accentColor,
                onClick = { onCharacterSelected(null) }
            )
            ThemeCharacter.entries.forEach { character ->
                CharacterFilterChip(
                    label = character.label,
                    selected = selectedCharacter == character,
                    accentColor = accentColor,
                    onClick = {
                        onCharacterSelected(if (selectedCharacter == character) null else character)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = Color.White.copy(alpha = 0.8f),
            selectedContainerColor = accentColor.copy(alpha = 0.25f),
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = accentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.2f),
            selectedBorderColor = accentColor.copy(alpha = 0.6f)
        )
    )
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
                        themeColors.background.copy(alpha = 0.8f),
                        themeColors.background.copy(alpha = 0.9f)
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