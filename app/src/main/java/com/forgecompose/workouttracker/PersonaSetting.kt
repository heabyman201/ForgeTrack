package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import dev.chrisbanes.haze.HazeState


// =====================================================
// Reactive persona object (mode + global AI enable flag)
// =====================================================
data class PersonaConfig(
    val mode: String,      // "coach", "drill", "companion", ...
    val enabled: Boolean   // master AI switch
)

// =======================================
// Encrypted storage for persona + enabled
// =======================================
object PersonaPrefs {
    private const val FILE = "ai_prefs_secure"
    private const val KEY_PERSONA = "persona_mode"
    private const val KEY_ENABLED = "persona_enabled"

    @Volatile private var ready = false
    private lateinit var appContext: Context
    private val lock = Any()
    @Volatile private var cachedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        if (ready) return
        synchronized(lock) {
            if (ready) return
            appContext = context.applicationContext
            ready = true
        }
    }

    private fun prefs(): SharedPreferences {
        check(ready) { "PersonaPrefs.init(context) must be called first" }
        cachedPrefs?.let { return it }
        synchronized(lock) {
            cachedPrefs?.let { return it }
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val p = EncryptedSharedPreferences.create(
                appContext,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            cachedPrefs = p
            return p
        }
    }

    fun readPersona(default: String = "coach"): String = try {
        (prefs().getString(KEY_PERSONA, default) ?: default).sanitizePersona()
    } catch (_: Throwable) { default.sanitizePersona() }

    private fun readEnabled(default: Boolean = true): Boolean = try {
        prefs().getBoolean(KEY_ENABLED, default)
    } catch (_: Throwable) { default }

    fun readConfig(
        defaultMode: String = "coach",
        defaultEnabled: Boolean = false
    ): PersonaConfig = PersonaConfig(
        mode = readPersona(defaultMode),
        enabled = readEnabled(defaultEnabled)
    )

    fun writeConfig(config: PersonaConfig) {
        runCatching {
            prefs().edit {
                putString(KEY_PERSONA, config.mode.sanitizePersona())
                putBoolean(KEY_ENABLED, config.enabled)
            }
        }
    }

    /** Call once at app start or before UI reads global state. */
    fun bootstrapInto(global: MutableState<PersonaConfig>, defaultMode: String = "coach", defaultEnabled: Boolean = true) {
        val persisted = readConfig(defaultMode, defaultEnabled)
        if (global.value != persisted) global.value = persisted
    }
}

// Keep your sanitizer
fun String.sanitizePersona(): String = when (this.lowercase()) {
    "coach","drill","companion","companion_plus","hype","minimal","nerd","monk","scientist" -> this
    else -> "coach"
}

// =======================================
// Persona Settings Screen (drop-in)
// =======================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current
    PersonaPrefs.init(context)

    // Assume dynamicModel exposes:
    //   val personaConfig: MutableState<PersonaConfig> = mutableStateOf(PersonaConfig("coach", true))
    //   (You can also keep a legacy personaMode proxy if needed.)
    // Make a local working copy synced to global + persisted.
    var localConfig by remember {
        mutableStateOf(
            PersonaPrefs.readConfig(
                defaultMode = dynamicModel.personaConfig.value.mode,
                defaultEnabled = dynamicModel.personaConfig.value.enabled
            )
        )
    }

    // local -> global
    LaunchedEffect(localConfig) {
        if (dynamicModel.personaConfig.value != localConfig) {
            dynamicModel.personaConfig.value = localConfig
        }
    }
    // global -> local
    LaunchedEffect(dynamicModel.personaConfig.value) {
        val normalized = dynamicModel.personaConfig.value.copy(
            mode = dynamicModel.personaConfig.value.mode.sanitizePersona()
        )
        if (normalized != localConfig) localConfig = normalized
    }

    // === Aesthetic background (unchanged) ===
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0404).copy(alpha = 0.8f))
                .background(staticGradientBrush)
                .background(secondaryStaticBrush)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "AI Persona",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
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
                contentWindowInsets = WindowInsets(0)
            ) { padding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Choose how the AI talks — or turn it off entirely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )

                    // ===== Master Enable/Disable Switch =====
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.55f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text("Enable AI", color = Color.White, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    if (localConfig.enabled) "Assistant responses will use your selected persona."
                                    else "Assistant is disabled: no requests will be sent.",
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = localConfig.enabled,
                                    onCheckedChange = { enabled ->
                                        val updated = localConfig.copy(enabled = enabled)
                                        localConfig = updated
                                        PersonaPrefs.writeConfig(updated)
                                    }
                                )
                            }
                        )
                    }

                    // ===== Persona List Card =====
                    val listAlpha = if (localConfig.enabled) 1f else 0.4f
                    val listClickable = localConfig.enabled

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = listAlpha },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.55f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                    ) {
                        // Hairline border & gentle highlight
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val corner = 20.dp.toPx()
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.White.copy(alpha = 0.06f),
                                            0.4f to Color.Transparent
                                        ),
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                    val stroke = Stroke(width = 1.dp.toPx())
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.08f),
                                        style = stroke,
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                    drawRoundRect(
                                        color = Color.Black.copy(alpha = 0.20f),
                                        style = Stroke(width = 1.dp.toPx()),
                                        topLeft = Offset(0.5f, 0.5f),
                                        size = Size(size.width - 1f, size.height - 1f),
                                        cornerRadius = CornerRadius(corner - 0.5f, corner - 0.5f)
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                PersonaOptionRowThemed(
                                    title = "Supportive Coach",
                                    subtitle = "Friendly, encouraging, practical",
                                    value = "coach",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Disciplined Trainer",
                                    subtitle = "Crisp, direct, safety-first",
                                    value = "drill",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Warm Companion",
                                    subtitle = "Playful, gentle, PG-13",
                                    value = "companion",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
//                                Divider(color = Color.White.copy(alpha = 0.06f))
//                                PersonaOptionRowThemed(
//                                    title = "Spicy Companion",
//                                    subtitle = "Flirty, cheeky, still PG-13",
//                                    value = "companion_plus",
//                                    selected = localConfig.mode,
//                                    enabled = listClickable
//                                ) { select ->
//                                    val updated = localConfig.copy(mode = select.sanitizePersona())
//                                    localConfig = updated
//                                    PersonaPrefs.writeConfig(updated)
//                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Hype Master",
                                    subtitle = "High energy, short punchy lines",
                                    value = "hype",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Minimal",
                                    subtitle = "One-line, straight to the point",
                                    value = "minimal",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Nerd Scholar",
                                    subtitle = "Geeky metaphors, precise wording",
                                    value = "nerd",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Zen Monk",
                                    subtitle = "Calm, reflective, almost meditative",
                                    value = "monk",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                                PersonaOptionRowThemed(
                                    title = "Scientist",
                                    subtitle = "Fact-based, nerdy, biohacker tone",
                                    value = "scientist",
                                    selected = localConfig.mode,
                                    enabled = listClickable
                                ) { select ->
                                    val updated = localConfig.copy(mode = select.sanitizePersona())
                                    localConfig = updated
                                    PersonaPrefs.writeConfig(updated)
                                }
                            }
                        }
                    }

                    Text(
                        "Encrypted on-device. Applies instantly across the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

/**
 * Persona row styled to match the dark/glass theme.
 */
@Composable
private fun PersonaOptionRowThemed(
    title: String,
    subtitle: String,
    value: String,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    val accent = Color(0xFFFF3B30)

    ListItem(
        headlineContent = {
            Text(
                title,
                style = if (isSelected) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.bodyLarge, // Note: if you don't have titleMedium/bodyLarge in your theme, adjust here.
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = Color.White
            )
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = { if (enabled) onSelect(value) },
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accent,
                    unselectedColor = Color.White.copy(alpha = 0.6f),
                    disabledSelectedColor = Color.White.copy(alpha = 0.3f),
                    disabledUnselectedColor = Color.White.copy(alpha = 0.2f)
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .let { base -> if (enabled) base.clickable { onSelect(value) } else base }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
