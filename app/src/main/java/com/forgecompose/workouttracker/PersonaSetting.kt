package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
object PersonaPrefs {
    private const val FILE = "ai_prefs_secure"
    private const val KEY_PERSONA = "persona_mode"
    private const val KEY_ENABLED = "persona_enabled"
    // New Key to store the name of the file user imported (e.g. "llama-3.bin")
    private const val KEY_MODEL_DISPLAY_NAME = "model_display_name"

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

    // New methods for Model Name storage
    fun saveModelName(name: String) {
        runCatching { prefs().edit { putString(KEY_MODEL_DISPLAY_NAME, name) } }
    }

    fun getModelName(): String? {
        return runCatching { prefs().getString(KEY_MODEL_DISPLAY_NAME, null) }.getOrNull()
    }

    fun clearModelName() {
        runCatching { prefs().edit { remove(KEY_MODEL_DISPLAY_NAME) } }
    }

    fun bootstrapInto(global: MutableState<dynamicModel.PersonaConfig>, defaultMode: String = "coach", defaultEnabled: Boolean = true) {
        val persisted = readConfig(defaultMode, defaultEnabled)
        if (global.value != persisted) global.value = persisted
    }
}

object EdgeModelManager {
    // We rename ANY imported file to this generic name so the Inference Engine
    // always knows where to look, regardless of the original file name.
    private const val GENERIC_MODEL_FILENAME = "current_imported_model.bin"

    private val _importProgress = MutableStateFlow(0f)
    val importProgress = _importProgress.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady = _isModelReady.asStateFlow()

    // Exposed flow for the UI to show the current file name
    private val _currentModelName = MutableStateFlow<String?>(null)
    val currentModelName = _currentModelName.asStateFlow()

    private var internalFile: File? = null

    fun init(context: Context) {
        internalFile = File(context.filesDir, GENERIC_MODEL_FILENAME)
        // Load the display name from prefs if available
        _currentModelName.value = PersonaPrefs.getModelName()
        checkModelStatus()
    }

    private fun checkModelStatus() {
        if (internalFile?.exists() == true && internalFile?.length() ?: 0L > 0) {
            _isModelReady.value = true
            _importProgress.value = 1f
        } else {
            _isModelReady.value = false
            _importProgress.value = 0f
            _currentModelName.value = null // Clear name if file is gone
            PersonaPrefs.clearModelName()
        }
    }

    fun getModelPath(): String? = internalFile?.absolutePath

    fun importModelFromUri(context: Context, sourceUri: Uri) {
        if (_isImporting.value) return

        CoroutineScope(Dispatchers.IO).launch {
            _isImporting.value = true
            _importProgress.value = 0f

            try {
                val contentResolver = context.contentResolver

                // 1. Get file size and Original Name
                var fileSize = -1L
                var originalName = "Unknown Model"

                contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        if (nameIndex != -1) originalName = cursor.getString(nameIndex)
                    }
                }

                // 2. Stream copy to our generic internal file
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(internalFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            if (fileSize > 0) {
                                _importProgress.value = totalBytes.toFloat() / fileSize
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    // Save the friendly name for the UI
                    PersonaPrefs.saveModelName(originalName)
                    _currentModelName.value = originalName

                    checkModelStatus()
                    _isImporting.value = false
                }

                Log.d("EdgeAI", "Import successful. Renamed '$originalName' to '$GENERIC_MODEL_FILENAME'")

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isImporting.value = false
                    _importProgress.value = 0f
                }
            }
        }
    }

    fun deleteModel() {
        internalFile?.let {
            if (it.exists()) {
                it.delete()
                checkModelStatus()
            }
        }
    }
}

fun String.sanitizePersona(): String = when (val v = lowercase()) {
    "coach","drill","companion","companion_plus","hype","minimal","nerd","monk","scientist","surgeon_apex" -> v
    else -> "coach"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current

    // Initialize Logic
    LaunchedEffect(Unit) {
        PersonaPrefs.init(context)
        EdgeModelManager.init(context)
    }

    val cfg by dynamicModel.personaConfig

    // --- Theme Hook ---
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val staticGradientBrush = remember(theme) {
        Brush.radialGradient(
            colors = listOf(
                theme.secondary.copy(alpha = 0.5f),
                theme.tertiary.copy(alpha = 0.8f),
                theme.background
            ),
            radius = 1200f,
            center = Offset(0.5f, 0.4f)
        )
    }

    val performanceOptions = remember { PerformanceOptionsManager.current }
    val movingEnabled = performanceOptions.collectAsState().value.movingGradientAndParticles

    // Edge Model State
    val isImporting by EdgeModelManager.isImporting.collectAsState()
    val importProgress by EdgeModelManager.importProgress.collectAsState()
    val isModelReady by EdgeModelManager.isModelReady.collectAsState()
    val currentModelName by EdgeModelManager.currentModelName.collectAsState()


    // --- FILE PICKER LAUNCHER ---
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // We accept any file, relying on the user to pick a valid .bin
        uri?.let { EdgeModelManager.importModelFromUri(context, it) }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = staticGradientBrush,
            introAlpha = 0f,
            enableWaves = movingEnabled,
            enableAnimation = movingEnabled,
        )
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
                // 1. Master Toggle
                item {
                    PersonaSectionCard(title = "Assistant", theme = theme) {
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
                                    else "Assistant disabled.",
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
                                    checkedThumbColor = theme.primary,
                                    checkedTrackColor = theme.secondary
                                )
                            )
                        }
                    }
                }

                // 2. Generic Edge Intelligence Section
                item {
                    PersonaSectionCard(title = "Offline AI Model", theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = currentModelName ?: "No Model Loaded",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        when {
                                            isImporting -> "Importing model file..."
                                            isModelReady -> "Ready for inference."
                                            else -> "Import a compatible .bin file (MediaPipe)."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isModelReady) theme.primary else Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isModelReady) EdgeModelManager.deleteModel()
                                        else filePickerLauncher.launch("*/*") // Accept any file, copied internally
                                    },
                                    enabled = !isImporting,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = if (isModelReady) Color.Red.copy(alpha = 0.8f) else theme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isModelReady) Icons.Default.Delete else Icons.Default.FileUpload,
                                        contentDescription = "Manage Model"
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isImporting) {
                                Column(Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { importProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = theme.primary,
                                        trackColor = theme.primary.copy(alpha = 0.2f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${(importProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.primary,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }

                            if (isModelReady) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(theme.secondary.copy(alpha=0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Memory, null, tint = theme.primary, modifier = Modifier.size(16.dp))
                                    Text("Running on Device (CPU/NPU)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                }
                            }
                        }
                    }
                }

                // 3. Persona Selection (unchanged)
                item {
                    val enabled = cfg.enabled
                    val alpha = if (enabled) 1f else 0.45f
                    PersonaSectionCard(title = "Persona Style", bodyAlpha = alpha, theme = theme) {
                        val onSelect: (String) -> Unit = { mode ->
                            val updated = cfg.copy(mode = mode.sanitizePersona())
                            dynamicModel.personaConfig.value = updated
                            PersonaPrefs.writeConfig(updated)
                        }

                        val personas = listOf(
                            Triple("Supportive Coach", "Friendly, encouraging, practical", "coach"),
                            Triple("Disciplined Trainer", "Crisp, direct, safety-first", "drill"),
                            Triple("Companion", "Warm, supportive, practical", "companion"),
                            Triple("Companion Plus", "Energetic, upbeat, playful", "companion_plus"),
                            Triple("Hype Master", "High energy, punchy lines", "hype"),
                            Triple("Minimal", "One-line, straight to point", "minimal"),
                            Triple("Nerd Scholar", "Precise, geeky metaphors", "nerd"),
                            Triple("Zen Monk", "Calm, reflective, minimal", "monk"),
                            Triple("Scientist", "Evidence-driven, biohacker tone", "scientist"),
                            Triple("Surgeon Apex", "Cold, calculated and precise", "surgeon_apex")
                        )

                        personas.forEachIndexed { index, (title, sub, key) ->
                            PersonaOptionRow(
                                title = title,
                                subtitle = sub,
                                value = key,
                                selected = cfg.mode,
                                enabled = enabled,
                                onSelect = onSelect,
                                theme = theme
                            )
                            if (index < personas.lastIndex) PersonaDivider(theme)
                        }
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
    theme: ColorSchemeAppTheme,
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
                        theme.secondary.copy(alpha = 0.8f),
                        theme.tertiary.copy(alpha = 0.95f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.2f),
                        theme.secondary.copy(alpha = 0.1f)
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
            color = theme.primary.copy(alpha = 0.3f)
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
private fun PersonaDivider(theme: ColorSchemeAppTheme) {
    HorizontalDivider(color = theme.primary.copy(alpha = 0.1f))
}

@Composable
private fun PersonaOptionRow(
    title: String,
    subtitle: String,
    value: String,
    selected: String,
    enabled: Boolean,
    theme: ColorSchemeAppTheme,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    val accent = theme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .let { m -> if (enabled) m.clickable { onSelect(value) } else m }
            .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(16.dp))
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