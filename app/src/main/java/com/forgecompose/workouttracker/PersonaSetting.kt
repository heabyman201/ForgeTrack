package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
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
    ): dynamicModel.PersonaConfig = dynamicModel.PersonaConfig(
        mode = readPersona(defaultMode),
        enabled = readEnabled(defaultEnabled)
    )

    fun writeConfig(config: dynamicModel.PersonaConfig) {
        runCatching {
            prefs().edit {
                putString(KEY_PERSONA, config.mode.sanitizePersona())
                putBoolean(KEY_ENABLED, config.enabled)
            }
        }
    }

    fun bootstrapInto(global: MutableState<dynamicModel.PersonaConfig>, defaultMode: String = "coach", defaultEnabled: Boolean = true) {
        val persisted = readConfig(defaultMode, defaultEnabled)
        if (global.value != persisted) global.value = persisted
    }
}

fun String.sanitizePersona(): String = when (val v = lowercase()) {
    "coach","drill","companion","companion_plus","hype","minimal","nerd","monk","scientist" -> v
    else -> "coach"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current
    PersonaPrefs.init(context)
    val cfg by dynamicModel.personaConfig

    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2A0F0F), Color(0xFF3D0000), Color(0xFF060202)),
            radius = 1200f,
            center = Offset(0.5f, 0.4f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(Color(0xFF060202))
                    drawRect(staticGradientBrush)
                }
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("AI Persona", fontWeight = FontWeight.Bold) },
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
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    PersonaSectionCard(title = "Assistant") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Enable AI", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (cfg.enabled) "Assistant uses the selected persona."
                                    else "Assistant disabled. No requests will be sent.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                            Switch(
                                checked = cfg.enabled,
                                onCheckedChange = { enabled ->
                                    val updated = cfg.copy(enabled = enabled)
                                    dynamicModel.personaConfig.value = updated
                                    PersonaPrefs.writeConfig(updated)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF3B30),
                                    checkedTrackColor = Color(0xFF8B0000),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                item {
                    val enabled = cfg.enabled
                    val alpha = if (enabled) 1f else 0.45f
                    PersonaSectionCard(title = "Persona Style", bodyAlpha = alpha) {
                        val onSelect: (String) -> Unit = { mode ->
                            val updated = cfg.copy(mode = mode.sanitizePersona())
                            dynamicModel.personaConfig.value = updated
                            PersonaPrefs.writeConfig(updated)
                        }

                        PersonaOptionRow(
                            title = "Supportive Coach",
                            subtitle = "Friendly, encouraging, practical",
                            value = "coach",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Disciplined Trainer",
                            subtitle = "Crisp, direct, safety-first",
                            value = "drill",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Training Buddy",
                            subtitle = "Warm, supportive, practical",
                            value = "companion",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Hype Companion",
                            subtitle = "Energetic, upbeat, playful",
                            value = "companion_plus",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Hype Master",
                            subtitle = "High energy, punchy lines",
                            value = "hype",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Minimal",
                            subtitle = "One-line, straight to point",
                            value = "minimal",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Nerd Scholar",
                            subtitle = "Precise, geeky metaphors",
                            value = "nerd",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Zen Monk",
                            subtitle = "Calm, reflective, minimal",
                            value = "monk",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                        PersonaDivider()
                        PersonaOptionRow(
                            title = "Scientist",
                            subtitle = "Evidence-driven, biohacker tone",
                            value = "scientist",
                            selected = cfg.mode,
                            enabled = enabled,
                            onSelect = onSelect
                        )
                    }
                }

                item {
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

@Composable
private fun PersonaSectionCard(
    title: String,
    bodyAlpha: Float = 1f,
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
                    colors = listOf(Color(0xFF180909).copy(alpha = 0.9f), Color(0xFF100404).copy(alpha = 0.95f)),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5555).copy(alpha = 0.2f),
                        Color(0xFF8B0000).copy(alpha = 0.1f)
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
            color = Color(0xFFFF3535).copy(alpha = 0.3f)
        )
        Column(
            modifier = Modifier.graphicsLayer { this.alpha = bodyAlpha },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PersonaDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}

@Composable
private fun PersonaOptionRow(
    title: String,
    subtitle: String,
    value: String,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    val accent = Color(0xFFFF3B30)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .let { m -> if (enabled) m.clickable { onSelect(value) } else m }
            .background(if (isSelected) Color(0x22FF3B30) else Color.Transparent, RoundedCornerShape(16.dp))
            .drawWithCache {
                val r = 16.dp.toPx()
                val stroke = 1.dp.toPx()
                onDrawBehind {
                    drawRoundRect(
                        color = if (isSelected) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                        style = Stroke(width = stroke),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = Color.White
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = { if (enabled) onSelect(value) },
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = Color.White.copy(alpha = 0.65f),
                disabledSelectedColor = Color.White.copy(alpha = 0.35f),
                disabledUnselectedColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}
