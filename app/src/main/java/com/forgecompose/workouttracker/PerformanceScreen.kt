package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.ui.components.*

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.ui.components.blurAnim.intensity
import com.forgecompose.workouttracker.ui.components.blurAnim.length
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

private val Context.perfDataStore by preferencesDataStore("performance_options")

@Immutable
data class PerformanceOptions(
    val blurEnabled: Boolean,
    val taskbarAnimations: Boolean,
    val movingGradientAndParticles: Boolean,
    val blurLengthMs: Long,
    val navEffects: Boolean,
    val showBodyHeatmap: Boolean,
    val maxSuggestions: Int
) {
    companion object {
        val Defaults = PerformanceOptions(
            blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            taskbarAnimations = true,
            movingGradientAndParticles = true,
            blurLengthMs = 700L,
            navEffects = true,
            showBodyHeatmap = true,
            3
        )
    }
}

object PerformanceOptionsManager {
    private val keyBlurEnabled = booleanPreferencesKey("blurEnabled")
    private val keyTaskbarAnimations = booleanPreferencesKey("taskbarAnimations")
    private val keyMovingGradientParticles = booleanPreferencesKey("movingGradientAndParticles")
    private val keyBlurLengthMs = longPreferencesKey("blurLengthMs")

    private val keyNavEffects = booleanPreferencesKey("navEffects")

    private val keyMaxSuggestions = intPreferencesKey("maxSuggestions")
    private val keyShowBodyHeatmap = booleanPreferencesKey("showBodyHeatmap")

    private val saved = MutableStateFlow(PerformanceOptions.Defaults)
    val current: StateFlow<PerformanceOptions> = saved

    private val isForeground = MutableStateFlow(true)
    private val isScreenOn = MutableStateFlow(true)

    private val _effective = MutableStateFlow(PerformanceOptions.Defaults)
    val effective: StateFlow<PerformanceOptions> = _effective

    private var initJob: Job? = null
    private var initRuntime: Boolean = false
    fun startEffectiveOptionsGating(context: Context){
        CoroutineScope(Dispatchers.Default).launch {
            combine(saved, isForeground, isScreenOn) { s, fg, scr ->
                val allow = fg && scr
                s.copy(
                    blurEnabled = s.blurEnabled && allow,
                    taskbarAnimations = s.taskbarAnimations && allow,
                    movingGradientAndParticles = s.movingGradientAndParticles && allow,
                    navEffects = s.navEffects && allow,
                    maxSuggestions = s.maxSuggestions
                )
            }.distinctUntilChanged().collect { _effective.value = it }
        }
    }
    fun initialize(context: Context) {
        if (initJob != null) return
        initJob = CoroutineScope(Dispatchers.IO).launch {
            context.perfDataStore.data
                .map { p ->
                    PerformanceOptions(
                        blurEnabled = p[keyBlurEnabled] ?: PerformanceOptions.Defaults.blurEnabled,
                        taskbarAnimations = p[keyTaskbarAnimations] ?: PerformanceOptions.Defaults.taskbarAnimations,
                        movingGradientAndParticles = p[keyMovingGradientParticles] ?: PerformanceOptions.Defaults.movingGradientAndParticles,
                        blurLengthMs = p[keyBlurLengthMs] ?: PerformanceOptions.Defaults.blurLengthMs,
                        navEffects = p[keyNavEffects] ?: PerformanceOptions.Defaults.navEffects,
                        showBodyHeatmap = p[keyShowBodyHeatmap] ?: PerformanceOptions.Defaults.showBodyHeatmap,
                        maxSuggestions = p[keyMaxSuggestions] ?: PerformanceOptions.Defaults.maxSuggestions

                    )
                }
                .collectLatest { saved.value = it }
        }
        initializeRuntimeOverrides(context.applicationContext)
        startEffectiveOptionsGating(context)
    }

    private fun initializeRuntimeOverrides(appCtx: Context) {
        if (initRuntime) return
        initRuntime = true

        val pm = appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenOn.value = pm.isInteractive

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                isForeground.emit(true)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { isForeground.value = true }
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { isForeground.value = false }
            }
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        appCtx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> isScreenOn.value = true
                    Intent.ACTION_SCREEN_OFF -> isScreenOn.value = false
                }
            }
        }, filter)
    }

    fun flow(context: Context): Flow<PerformanceOptions> = current
    fun effectiveFlow(context: Context): Flow<PerformanceOptions> = effective

    suspend fun set(context: Context, v: PerformanceOptions) {
        context.perfDataStore.edit { p ->
            p[keyBlurEnabled] = v.blurEnabled
            p[keyTaskbarAnimations] = v.taskbarAnimations
            p[keyMovingGradientParticles] = v.movingGradientAndParticles
            p[keyBlurLengthMs] = v.blurLengthMs
            p[keyNavEffects] = v.navEffects
            p[keyShowBodyHeatmap] = v.showBodyHeatmap
            p[keyMaxSuggestions] = v.maxSuggestions
        }
        saved.value = v
    }

    suspend fun setBlurEnabled(context: Context, enabled: Boolean) {
        context.perfDataStore.edit { it[keyBlurEnabled] = enabled }
        saved.value = saved.value.copy(blurEnabled = enabled)
    }

    suspend fun setTaskbarAnimations(context: Context, enabled: Boolean) {
        context.perfDataStore.edit { it[keyTaskbarAnimations] = enabled }
        saved.value = saved.value.copy(taskbarAnimations = enabled)
    }

    suspend fun setMovingGradientAndParticles(context: Context, enabled: Boolean) {
        context.perfDataStore.edit { it[keyMovingGradientParticles] = enabled }
        saved.value = saved.value.copy(movingGradientAndParticles = enabled)
    }

    suspend fun setMaxSuggestions(context: Context, max: Int) {
        context.perfDataStore.edit { it[keyMaxSuggestions] = max }
    }

    suspend fun setBlurLengthMs(context: Context, ms: Long) {
        context.perfDataStore.edit { it[keyBlurLengthMs] = ms }
        saved.value = saved.value.copy(blurLengthMs = ms)
    }
    suspend fun setNavEffectsOn(context: Context, enabled: Boolean){
        context.perfDataStore.edit { it[keyNavEffects] = enabled }
        saved.value = saved.value.copy(navEffects = enabled)
    }

    suspend fun setShowBodyHeatmap(context: Context, enabled: Boolean) {
        context.perfDataStore.edit { it[keyShowBodyHeatmap] = enabled }
        saved.value = saved.value.copy(showBodyHeatmap = enabled)
    }
}

class PerformanceOptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    val savedOptions = PerformanceOptionsManager.flow(ctx).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerformanceOptions.Defaults
    )
    val effectiveOptions = PerformanceOptionsManager.effectiveFlow(ctx).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerformanceOptions.Defaults
    )

    fun update(transform: (PerformanceOptions) -> PerformanceOptions) {
        val next = transform(savedOptions.value)
        viewModelScope.launch(Dispatchers.IO) { PerformanceOptionsManager.set(ctx, next) }
    }

    fun setBlurEnabled(b: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setBlurEnabled(ctx, b)
    }
    fun setTaskbarAnimations(b: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setTaskbarAnimations(ctx, b)
    }
    fun setMovingGradientAndParticles(b: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setMovingGradientAndParticles(ctx, b)
    }
    fun setMaxSuggestions(max: Int) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setMaxSuggestions(ctx, max)
    }
    fun setBlurLengthMs(ms: Long) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setBlurLengthMs(ctx, ms.coerceIn(300L, 1200L))
    }
    fun setNavEffectsOn(b: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setNavEffectsOn(ctx, b)
    }
    fun setShowBodyHeatmap(b: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        PerformanceOptionsManager.setShowBodyHeatmap(ctx, b)
    }

}

private enum class ResourceImpact(val label: String, val icon: ImageVector, val color: Color) {
    CPU("CPU", Icons.Default.Architecture, Color(0xFFF2994A)),
    GPU("GPU", Icons.Default.DataObject, Color(0xFF2D9CDB)),
    BATTERY("Battery", Icons.Default.BatteryChargingFull, Color(0xFFEB5757))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceOptionsScreen(
    navController: NavController,
    vm: PerformanceOptionsViewModel = viewModel(),
) {
    val saved by vm.savedOptions.collectAsState()
    val effective by vm.effectiveOptions.collectAsState()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(
        targetValue = if (showIntro) 0f else 1f,
        animationSpec = tween(650, easing = LinearEasing),
        label = "introFade"
    )
    LaunchedEffect(Unit) { showIntro = false;
        Firebase.crashlytics.setCustomKey("current_screen", "PerformanceSettings")}

    val staticGradientBrush = remember(theme) {
        Brush.radialGradient(
            colors = listOf(theme.secondary.copy(alpha = 0.8f), theme.tertiary, theme.background),
            radius = 1200f,
            center = Offset(0.5f, 0.4f)
        )
    }

    val blurTarget: Dp = if (effective.blurEnabled) intensity.value else 0.dp
    val blurAnim by animateDpAsState(
        targetValue = blurTarget,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )

    val movingEffectsEnabled = effective.movingGradientAndParticles

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        AnimatedBackdrop(
            modifier = Modifier.fillMaxSize(),
            introBrush = staticGradientBrush,
            introAlpha = 1f - introProgress,
            enableWaves = movingEffectsEnabled,
            enableAnimation = movingEffectsEnabled,
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Performance", fontWeight = FontWeight.Bold) },
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
                   PerformanceSection(
                       vm = vm,
                       navController = navController,
                       theme = theme,
                       saved = saved
                   )

                }
                item {
                    SettingsSectionCardHealth(title = "Homescreen", theme = theme) {
                        MaxSuggestionsRow(
                            true,
                            saved.maxSuggestions,
                            onChange = {
                               max -> vm.setMaxSuggestions(max)
                            },
                            theme = theme

                        )
                    }
                }
                item {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            scope.launch { PerformanceOptionsManager.set(context, PerformanceOptions.Defaults) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary.copy(alpha = 0.4f),
                            contentColor = Color.White
                        )
                    ) { Text("Reset to Recommended") }
                }
            }
        }
    }
}
@Composable
private fun MaxSuggestionsRow(
    enabled: Boolean,
    currentMax: Int,
    onChange: (Int) -> Unit,
    theme: ColorSchemeAppTheme
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Max Suggestions and Favourites",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "$currentMax",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        SettingProgressBar(
            enabled = enabled,
            currentMax = currentMax,
            onChangeI = onChange,
            firstText = "Less Suggestions",
            stepSize = 1/2,
            valueRange = 1f..4f,
            secondText = "More Suggestions",
            theme = theme
        )
    }
}
@Composable
fun SettingProgressBar(
    enabled: Boolean,
    currentMax: Int,
    onChangeI: (Int) -> Unit? = {},
    onChangeL: (Long) -> Unit? = {},
    firstText: String,
    stepSize: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    secondText: String,
    theme: ColorSchemeAppTheme
){
    Slider(
        value = currentMax.toFloat().coerceIn(valueRange),
        onValueChange = { v -> onChangeI(v.toInt())
            onChangeL(v.toLong())},
        valueRange = valueRange,
        steps = stepSize,
        enabled = enabled,
        colors = SliderDefaults.colors(
            activeTrackColor = theme.primary,
            inactiveTrackColor = theme.secondary,
            thumbColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(firstText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        Text(secondText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
    }

}
@Composable
private fun BlurLengthRow(
    enabled: Boolean,
    currentMs: Long,
    onChange: (Long) -> Unit,
    theme: ColorSchemeAppTheme
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blur Speed",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "${currentMs}ms",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        SettingProgressBar(
            enabled = enabled,
            currentMax = currentMs.toInt(),
            onChangeL = onChange,
            firstText = "Slow",
            stepSize = 8,
            valueRange = 300f..1200f,
            secondText = "Fast",
            theme = theme
        )
    }
}

@Composable
private fun SettingsSectionCardHealth(
    title: String,
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
                    colors = listOf(theme.background.copy(alpha = 0.6f), theme.background.copy(alpha = 0.95f)),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        theme.background.copy(alpha = 0.2f),
                        theme.background.copy(alpha = 0.1f)
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}
@Composable
fun PerformanceSection(vm: PerformanceOptionsViewModel, navController: NavController, theme: ColorSchemeAppTheme,
              saved: PerformanceOptions){
    SettingsSectionCardHealth(title = "Display & Animation", theme = theme) {
        PerformanceToggleRow(
            label = "Enable Blur",
            checked = saved.blurEnabled,
            icon = Icons.Default.BlurOn,
            impacts = listOf(ResourceImpact.GPU),
            enabled = true,
            theme = theme
        ) { b -> vm.setBlurEnabled(b) }

        Spacer(Modifier.height(6.dp))

        BlurLengthRow(
            enabled = saved.blurEnabled,
            currentMs = saved.blurLengthMs,
            onChange = { ms -> vm.setBlurLengthMs(ms) },
            theme = theme
        )

        PerformanceToggleRow(
            label = "Taskbar Animations",
            checked = saved.taskbarAnimations,
            icon = Icons.Default.Animation,
            impacts = listOf(ResourceImpact.GPU),
            enabled = true,
            theme = theme
        ) { b -> vm.setTaskbarAnimations(b) }

        PerformanceToggleRow(
            label = "Particles",
            checked = saved.movingGradientAndParticles,
            icon = Icons.Default.Grain,
            impacts = listOf(ResourceImpact.GPU, ResourceImpact.BATTERY),
            enabled = true,
            theme = theme
        ) { b -> vm.setMovingGradientAndParticles(b) }
        PerformanceToggleRow(
            label = "Navigation Effects",
            checked = saved.navEffects,
            icon = Icons.Default.Brush,
            impacts = listOf(ResourceImpact.GPU),
            enabled = true,
            theme = theme
        ) {
                b -> vm.setNavEffectsOn(b)
        }
    }
}
@Composable
private fun PerformanceToggleRow(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    impacts: List<ResourceImpact>,
    enabled: Boolean,
    theme: ColorSchemeAppTheme,
    onToggle: (Boolean) -> Unit,

    ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = theme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.primary,
                    checkedTrackColor = theme.secondary,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                ),
                enabled = enabled
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            impacts.forEach { impact ->
                ResourceChip(impact)
            }
        }
    }
}

@Composable
private fun ResourceChip(impact: ResourceImpact) {
    Row(
        modifier = Modifier
            .background(impact.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = impact.icon,
            contentDescription = impact.label,
            tint = impact.color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = impact.label,
            color = impact.color,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
