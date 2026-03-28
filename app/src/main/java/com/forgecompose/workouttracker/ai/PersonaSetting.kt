package com.forgecompose.workouttracker.ai

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class AiModelProvider(val key: String) {
    EDGE_ON_DEVICE("edge_on_device"),
    LOCAL_NETWORK("local_network"),
    GOOGLE_AI_STUDIO("google_ai_studio");

    companion object {
        fun fromKey(key: String?): AiModelProvider {
            return entries.firstOrNull { it.key == key } ?: EDGE_ON_DEVICE
        }
    }
}

data class LocalLlmConfig(
    val ipAddress: String = "",
    val port: String = "11434",
    val selectedModel: String = ""
)

object PersonaPrefs {
    private const val FILE = "ai_prefs_secure"
    private const val KEY_PERSONA = "persona_mode"
    private const val KEY_ENABLED = "persona_enabled"
    // New Key to store the name of the file user imported (e.g. "llama-3.bin")
    private const val KEY_MODEL_DISPLAY_NAME = "model_display_name"
    private const val KEY_MODEL_PROVIDER = "model_provider"
    private const val KEY_LOCAL_IP = "local_llm_ip"
    private const val KEY_LOCAL_PORT = "local_llm_port"
    private const val KEY_LOCAL_MODEL = "local_llm_model"
    private const val KEY_GF_UNLOCKED = "persona_gf_unlocked"

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
        val value = (prefs().getString(KEY_PERSONA, default) ?: default).sanitizePersona()
        if (value == "girlfriend_like" && !isGirlfriendPersonaUnlocked()) default.sanitizePersona() else value
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

    fun readModelProvider(default: AiModelProvider = AiModelProvider.EDGE_ON_DEVICE): AiModelProvider = try {
        AiModelProvider.fromKey(prefs().getString(KEY_MODEL_PROVIDER, default.key))
    } catch (_: Throwable) {
        default
    }

    fun writeModelProvider(provider: AiModelProvider) {
        runCatching { prefs().edit { putString(KEY_MODEL_PROVIDER, provider.key) } }
    }

    fun readLocalLlmConfig(default: LocalLlmConfig = LocalLlmConfig()): LocalLlmConfig = try {
        LocalLlmConfig(
            ipAddress = prefs().getString(KEY_LOCAL_IP, default.ipAddress) ?: default.ipAddress,
            port = prefs().getString(KEY_LOCAL_PORT, default.port) ?: default.port,
            selectedModel = prefs().getString(KEY_LOCAL_MODEL, default.selectedModel) ?: default.selectedModel
        )
    } catch (_: Throwable) {
        default
    }

    fun writeLocalLlmConfig(config: LocalLlmConfig) {
        runCatching {
            prefs().edit {
                putString(KEY_LOCAL_IP, config.ipAddress.trim())
                putString(KEY_LOCAL_PORT, config.port.trim())
                putString(KEY_LOCAL_MODEL, config.selectedModel.trim())
            }
        }
    }

    fun isGirlfriendPersonaUnlocked(default: Boolean = false): Boolean = try {
        prefs().getBoolean(KEY_GF_UNLOCKED, default)
    } catch (_: Throwable) { default }

    fun setGirlfriendPersonaUnlocked(unlocked: Boolean) {
        runCatching { prefs().edit { putBoolean(KEY_GF_UNLOCKED, unlocked) } }
    }

    fun bootstrapInto(global: MutableState<dynamicModel.PersonaConfig>, defaultMode: String = "coach", defaultEnabled: Boolean = true) {
        val persisted = readConfig(defaultMode, defaultEnabled)
        if (global.value != persisted) global.value = persisted
    }
}

fun sanitizeLocalHostInput(rawHost: String): String {
    return rawHost
        .trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
        .substringBefore(":")
}

fun sanitizeLocalPortInput(rawPort: String): String {
    val trimmed = rawPort.trim()
    val parsed = trimmed.toIntOrNull() ?: return ""
    return if (parsed in 1..65535) parsed.toString() else ""
}

fun buildLocalLlmRootUrl(ipAddress: String, port: String): String? {
    val host = sanitizeLocalHostInput(ipAddress)
    val safePort = sanitizeLocalPortInput(port)
    if (host.isBlank() || safePort.isBlank()) return null
    return "http://$host:$safePort"
}

fun buildLocalLlmOpenAiBaseUrl(ipAddress: String, port: String): String? {
    val root = buildLocalLlmRootUrl(ipAddress, port) ?: return null
    return "$root/v1"
}

fun hasGoogleAiStudioApiKey(): Boolean = BuildConfig.GOOGLE_AI_STUDIO_API_KEY.isNotBlank()

object LocalLlmManager {
    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels = _isFetchingModels.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    fun clearError() {
        _lastError.value = null
    }

    fun discoverModels(ipAddress: String, port: String) {
        if (_isFetchingModels.value) return
        val rootUrl = buildLocalLlmRootUrl(ipAddress, port)
        if (rootUrl == null) {
            _lastError.value = "Enter a valid IP address and port."
            _availableModels.value = emptyList()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isFetchingModels.value = true
            _lastError.value = null
            try {
                val openAiModels = fetchOpenAiModels("$rootUrl/v1/models")
                val models = if (openAiModels.isNotEmpty()) openAiModels else fetchOllamaModels("$rootUrl/api/tags")
                withContext(Dispatchers.Main) {
                    _availableModels.value = models.distinct().sorted()
                    if (_availableModels.value.isEmpty()) {
                        _lastError.value = "No models found on the target machine."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _availableModels.value = emptyList()
                    _lastError.value = e.message ?: "Failed to fetch models from local machine."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isFetchingModels.value = false
                }
            }
        }
    }

    private fun fetchOpenAiModels(endpoint: String): List<String> {
        val response = httpGet(endpoint)
        val json = JSONObject(response)
        val data = json.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) add(id)
            }
        }
    }

    private fun fetchOllamaModels(endpoint: String): List<String> {
        val response = httpGet(endpoint)
        val json = JSONObject(response)
        val models = json.optJSONArray("models") ?: return emptyList()
        return buildList {
            for (i in 0 until models.length()) {
                val modelObj = models.optJSONObject(i)
                val name = modelObj?.optString("name").orEmpty()
                if (name.isNotBlank()) add(name)
            }
        }
    }

    private fun httpGet(endpoint: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code while reading models.")
            }
            return body
        } finally {
            connection.disconnect()
        }
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
    "coach","drill","companion","companion_plus","hype","minimal","nerd","monk","scientist","surgeon_apex","girlfriend_like","uncensored" -> v
    else -> "coach"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current

    remember(context) {
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
    val localModels by LocalLlmManager.availableModels.collectAsState()
    val isFetchingLocalModels by LocalLlmManager.isFetchingModels.collectAsState()
    val localModelError by LocalLlmManager.lastError.collectAsState()

    var modelProvider by remember { mutableStateOf(PersonaPrefs.readModelProvider()) }
    val initialLocalConfig = remember { PersonaPrefs.readLocalLlmConfig() }
    var localIpAddress by remember { mutableStateOf(initialLocalConfig.ipAddress) }
    var localPort by remember { mutableStateOf(initialLocalConfig.port) }
    var selectedLocalModel by remember { mutableStateOf(initialLocalConfig.selectedModel) }
    var girlfriendUnlocked by remember { mutableStateOf(PersonaPrefs.isGirlfriendPersonaUnlocked()) }
    var providerTapCount by rememberSaveable { mutableIntStateOf(0) }
    var aiToggleTapCount by rememberSaveable { mutableIntStateOf(0) }

    fun registerGirlfriendUnlockTap(isProviderTap: Boolean) {
        if (girlfriendUnlocked) return
        if (isProviderTap) providerTapCount += 1 else aiToggleTapCount += 1
        if (providerTapCount >= 6 && aiToggleTapCount >= 3) {
            girlfriendUnlocked = true
            PersonaPrefs.setGirlfriendPersonaUnlocked(true)
            Toast.makeText(context, "Secret persona unlocked.", Toast.LENGTH_SHORT).show()
        }
    }


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
                                    registerGirlfriendUnlockTap(isProviderTap = false)
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
                    PersonaSectionCard(title = "Model Source", theme = theme) {
                        val providers = listOf(
                            AiModelProvider.EDGE_ON_DEVICE to "On-device model (.bin)",
                            AiModelProvider.LOCAL_NETWORK to "Local network LLM server",
                            AiModelProvider.GOOGLE_AI_STUDIO to "Google AI Studio API (fixed Gemini 3.1 Flash-Lite)"
                        )
                        providers.forEach { (provider, subtitle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        registerGirlfriendUnlockTap(isProviderTap = true)
                                        modelProvider = provider
                                        PersonaPrefs.writeModelProvider(provider)
                                    }
                                    .background(
                                        if (provider == modelProvider) theme.primary.copy(alpha = 0.14f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = when (provider) {
                                            AiModelProvider.EDGE_ON_DEVICE -> "On Device"
                                            AiModelProvider.LOCAL_NETWORK -> "Local Server"
                                            AiModelProvider.GOOGLE_AI_STUDIO -> "Google AI Studio"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                RadioButton(
                                    selected = provider == modelProvider,
                                    onClick = {
                                        registerGirlfriendUnlockTap(isProviderTap = true)
                                        modelProvider = provider
                                        PersonaPrefs.writeModelProvider(provider)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                                )
                            }
                        }
                    }
                }

                item {
                    if (modelProvider == AiModelProvider.EDGE_ON_DEVICE) {
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
                                            else filePickerLauncher.launch("*/*")
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
                                            .background(theme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.Memory, null, tint = theme.primary, modifier = Modifier.size(16.dp))
                                        Text("Running on Device (CPU/NPU)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                    }
                                }
                            }
                        }
                    } else if (modelProvider == AiModelProvider.LOCAL_NETWORK) {
                        PersonaSectionCard(title = "Local LLM", theme = theme) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = localIpAddress,
                                    onValueChange = {
                                        localIpAddress = it
                                        LocalLlmManager.clearError()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("IP Address") },
                                    placeholder = { Text("192.168.1.12") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = localPort,
                                    onValueChange = {
                                        localPort = it.filter { ch -> ch.isDigit() }
                                        LocalLlmManager.clearError()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Port") },
                                    placeholder = { Text("11434") },
                                    singleLine = true
                                )
                                Text(
                                    text = buildLocalLlmOpenAiBaseUrl(localIpAddress, localPort)?.let { "$it/chat/completions" }
                                        ?: "Endpoint will be auto-built as http://<ip>:<port>/v1",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Button(
                                    onClick = {
                                        val sanitizedIp = sanitizeLocalHostInput(localIpAddress)
                                        val sanitizedPort = sanitizeLocalPortInput(localPort)
                                        localIpAddress = sanitizedIp
                                        if (sanitizedPort.isNotBlank()) localPort = sanitizedPort
                                        val updated = LocalLlmConfig(
                                            ipAddress = sanitizedIp,
                                            port = if (sanitizedPort.isNotBlank()) sanitizedPort else localPort,
                                            selectedModel = selectedLocalModel
                                        )
                                        PersonaPrefs.writeLocalLlmConfig(updated)
                                        LocalLlmManager.discoverModels(updated.ipAddress, updated.port)
                                    },
                                    enabled = !isFetchingLocalModels
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isFetchingLocalModels) "Checking..." else "Find Models")
                                }

                                if (isFetchingLocalModels) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.primary)
                                }

                                if (!localModelError.isNullOrBlank()) {
                                    Text(
                                        text = localModelError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Red.copy(alpha = 0.85f)
                                    )
                                }

                                if (localModels.isNotEmpty()) {
                                    Text(
                                        text = "Available models",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    localModels.forEach { modelId ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedLocalModel = modelId
                                                    PersonaPrefs.writeLocalLlmConfig(
                                                        LocalLlmConfig(
                                                            ipAddress = localIpAddress,
                                                            port = localPort,
                                                            selectedModel = modelId
                                                        )
                                                    )
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                modelId,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            RadioButton(
                                                selected = selectedLocalModel == modelId,
                                                onClick = {
                                                    selectedLocalModel = modelId
                                                    PersonaPrefs.writeLocalLlmConfig(
                                                        LocalLlmConfig(
                                                            ipAddress = localIpAddress,
                                                            port = localPort,
                                                            selectedModel = modelId
                                                        )
                                                    )
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        PersonaSectionCard(title = "Google AI Studio", theme = theme) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Uses the build-time API key from Gradle/local.properties.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.84f)
                                )
                                Text(
                                    text = "Model: Gemini 3.1 Flash-Lite",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                            Triple("Uncensored", "Raw and direct language, no sugarcoating", "uncensored"),
                            Triple("Minimal", "One-line, straight to point", "minimal"),
                            Triple("Nerd Scholar", "Precise, geeky metaphors", "nerd"),
                            Triple("Zen Monk", "Calm, reflective, minimal", "monk"),
                            Triple("Scientist", "Evidence-driven, biohacker tone", "scientist"),
                            Triple("Surgeon Apex", "Cold, calculated and precise", "surgeon_apex")
                        ).toMutableList().apply {
                            if (girlfriendUnlocked) {
                                add(4, Triple("PERSONA_5", "", "girlfriend_like"))
                            }
                        }

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
                        theme.background.copy(alpha = 0.8f),
                        theme.background.copy(alpha = 0.95f)
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
