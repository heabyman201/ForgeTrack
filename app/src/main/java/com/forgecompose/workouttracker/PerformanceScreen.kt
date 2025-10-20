package com.forgecompose.workouttracker

import android.app.Application
import android.content.Context
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
import androidx.compose.ui.draw.blur
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
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forgecompose.workouttracker.blurAnim.intensity
import com.forgecompose.workouttracker.blurAnim.length
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

private val Context.perfDataStore by preferencesDataStore("performance_options")

@Immutable
data class PerformanceOptions(
    val blurEnabled: Boolean,
    val taskbarAnimations: Boolean,
    val movingGradientAndParticles: Boolean
) {
    val blurDp: Dp get() = if (blurEnabled) 17.dp else 0.dp
    companion object {
        val Defaults = PerformanceOptions(
            blurEnabled = true,
            taskbarAnimations = true,
            movingGradientAndParticles = true
        )
    }
}

object PerformanceOptionsManager {
    private val keyBlurEnabled = booleanPreferencesKey("blurEnabled")
    private val keyTaskbarAnimations = booleanPreferencesKey("taskbarAnimations")
    private val keyMovingGradientParticles = booleanPreferencesKey("movingGradientAndParticles")

    private val snapshot = MutableStateFlow(PerformanceOptions.Defaults)
    val current: StateFlow<PerformanceOptions> = snapshot

    private var initJob: Job? = null

    fun initialize(context: Context) {
        if (initJob != null) return
        initJob = CoroutineScope(Dispatchers.IO).launch {
            context.perfDataStore.data
                .map { p ->
                    PerformanceOptions(
                        blurEnabled = p[keyBlurEnabled] ?: PerformanceOptions.Defaults.blurEnabled,
                        taskbarAnimations = p[keyTaskbarAnimations] ?: PerformanceOptions.Defaults.taskbarAnimations,
                        movingGradientAndParticles = p[keyMovingGradientParticles] ?: PerformanceOptions.Defaults.movingGradientAndParticles
                    )
                }
                .collectLatest { snapshot.value = it }
        }
    }

    fun flow(context: Context): Flow<PerformanceOptions> = current

    suspend fun set(context: Context, v: PerformanceOptions) {
        context.perfDataStore.edit { p ->
            p[keyBlurEnabled] = v.blurEnabled
            p[keyTaskbarAnimations] = v.taskbarAnimations
            p[keyMovingGradientParticles] = v.movingGradientAndParticles
        }
        snapshot.value = v
    }
}

class PerformanceOptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    val options = PerformanceOptionsManager.flow(ctx).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerformanceOptions.Defaults
    )
    fun update(transform: (PerformanceOptions) -> PerformanceOptions) {
        val next = transform(options.value)
        viewModelScope.launch(Dispatchers.IO) {
            PerformanceOptionsManager.set(ctx, next)
        }
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
    val opts by vm.options.collectAsState()
    val scope = rememberCoroutineScope()

    var showIntro by remember { mutableStateOf(true) }
    val introProgress by animateFloatAsState(targetValue = if (showIntro) 0f else 1f, animationSpec = tween(650, easing = LinearEasing), label = "introFade")
    LaunchedEffect(Unit) { showIntro = false }

    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2A0F0F), Color(0xFF3D0000), Color(0xFF060202)),
            radius = 1200f,
            center = Offset(0.5f, 0.4f)
        )
    }
    val blurAnim by animateDpAsState(
        if (showIntro) intensity.value else 0.dp,
        animationSpec = tween(length.value.toInt()),
        label = "blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(blurAnim)
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
                    SettingsSectionCard(title = "Display & Animation") {
                        PerformanceToggleRow(
                            label = "Enable Blur",
                            checked = opts.blurEnabled,
                            icon = Icons.Default.BlurOn,
                            impacts = listOf(ResourceImpact.GPU)
                        ) { b -> vm.update { it.copy(blurEnabled = b) } }

                        PerformanceToggleRow(
                            label = "Taskbar Animations",
                            checked = opts.taskbarAnimations,
                            icon = Icons.Default.Animation,
                            impacts = listOf(ResourceImpact.GPU)
                        ) { b -> vm.update { it.copy(taskbarAnimations = b) } }

                        PerformanceToggleRow(
                            label = "Gradient & Particles",
                            checked = opts.movingGradientAndParticles,
                            icon = Icons.Default.Grain,
                            impacts = listOf(ResourceImpact.GPU, ResourceImpact.BATTERY)
                        ) { b -> vm.update { it.copy(movingGradientAndParticles = b) } }
                    }
                }
                item {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            scope.launch {
                                PerformanceOptionsManager.set(context, PerformanceOptions.Defaults)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Reset to Recommended")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun PerformanceToggleRow(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    impacts: List<ResourceImpact>,
    onToggle: (Boolean) -> Unit
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
                    tint = Color(0xFFFF3B30)
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
                    checkedThumbColor = Color(0xFFFF3B30),
                    checkedTrackColor = Color(0xFF8B0000),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
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
