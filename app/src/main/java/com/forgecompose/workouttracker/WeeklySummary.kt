package com.forgecompose.workouttracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

@Stable
data class WorkoutEntry(val instant: Instant, val localDate: LocalDate, val name: String, val durationSec: Long, val weightKg: Double?)
@Stable
data class DaySummary(val date: LocalDate, val totalSec: Long, val count: Int)
@Stable
data class WorkoutAggregate(val name: String, val totalSec: Long, val count: Int, val avgWeightKg: Double?)
@Stable
data class WeeklySummary(val weekStart: LocalDate, val weekEnd: LocalDate, val days: List<DaySummary>, val totalSec: Long, val totalCount: Int, val byWorkout: List<WorkoutAggregate>)

private val tsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'", Locale.US)

private fun parseLine(line: String, zone: ZoneId): WorkoutEntry? {
    val r = Regex("""\[(.+?)]\s+workout:\s+name=(.*?)\s+\|\s+time=(.*?)\s+\|\s+weight=(.*)""")
    val m = r.matchEntire(line.trim()) ?: return null
    val ts = m.groupValues[1].trim()
    var name = m.groupValues[2].trim()

    val stateRegex = Regex("""MutableState\(value=(.*?)\)@.*""")
    val stateMatch = stateRegex.matchEntire(name)
    if (stateMatch != null && stateMatch.groupValues.size > 1) {
        name = stateMatch.groupValues[1]
    }
    name = name.ifBlank { "Unnamed" }

    val timeStr = m.groupValues[3].trim()
    val weightStr = m.groupValues[4].trim()
    val instant = try { LocalDateTime.parse(ts, tsFormatter).atZone(ZoneOffset.UTC).toInstant() } catch (_: Throwable) { return null }
    val localDate = instant.atZone(zone).toLocalDate()
    val durationSec = parseDurationToSeconds(timeStr) ?: return null
    val weightKg = parseWeight(weightStr)
    return WorkoutEntry(instant, localDate, name, durationSec, weightKg)
}

private fun parseDurationToSeconds(s: String): Long? {
    var rest = s.trim()
    var h = 0L
    var m = 0L
    var sec = 0L
    Regex("""(\d+)h""").find(rest)?.let { h = it.groupValues[1].toLong(); rest = rest.replace(it.value, "") }
    Regex("""(\d+)m""").find(rest)?.let { m = it.groupValues[1].toLong(); rest = rest.replace(it.value, "") }
    Regex("""(\d+)s""").find(rest)?.let { sec = it.groupValues[1].toLong() }
    return h * 3600 + m * 60 + sec
}

private fun parseWeight(w: String): Double? {
    if (w.equals("BW", true)) return null
    val m = Regex("""(-?\d+(?:\.\d+)?)\s*kg""", RegexOption.IGNORE_CASE).find(w) ?: return null
    return m.groupValues[1].toDoubleOrNull()
}

private fun entriesFromTape(tape: String, zone: ZoneId): List<WorkoutEntry> =
    tape.lineSequence().mapNotNull { parseLine(it, zone) }.sortedBy { it.instant }.toList()

private fun startOfWeek(date: LocalDate, firstDay: DayOfWeek): LocalDate {
    val wf = WeekFields.of(firstDay, 1)
    return date.with(wf.dayOfWeek(), 1)
}

private fun summarizeWeek(entries: List<WorkoutEntry>, weekStart: LocalDate): WeeklySummary {
    val weekEnd = weekStart.plusDays(6)
    val inWeek = entries.filter { it.localDate in weekStart..weekEnd }
    val byDay = (0..6).associate { d -> weekStart.plusDays(d.toLong()) to mutableListOf<WorkoutEntry>() }
    inWeek.forEach { e -> byDay[e.localDate]?.add(e) }
    val days = byDay.entries.sortedBy { it.key }.map { (d, list) -> DaySummary(d, list.sumOf { it.durationSec }, list.size) }
    val byWorkout = inWeek.groupBy { it.name }.map { (name, list) ->
        val totalSec = list.sumOf { it.durationSec }
        val count = list.size
        val avg = list.mapNotNull { it.weightKg }.takeIf { it.isNotEmpty() }?.average()
        WorkoutAggregate(name, totalSec, count, avg)
    }.sortedWith(compareByDescending<WorkoutAggregate> { it.totalSec }.thenBy { it.name.lowercase(Locale.getDefault()) })
    val totalSec = days.sumOf { it.totalSec }
    val totalCount = days.sumOf { it.count }
    return WeeklySummary(weekStart, weekEnd, days, totalSec, totalCount, byWorkout)
}

private fun formatHMS(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return buildString {
        if (h > 0) append("${h}h ")
        if (m > 0) append("${m}m ")
        append("${s}s")
    }.trim()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklySummaryScreen(
    tapeFlow: Flow<String>,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    zoneId: ZoneId = ZoneId.systemDefault(),
    navController: NavController
) {
    val lazyListState = rememberLazyListState()

    val crimson = Color(0xFF4A0000)
    val purple = Color(0xFF4A004A)

    val animatedColor by remember {
        derivedStateOf {
            val scrollOffset = lazyListState.firstVisibleItemIndex * 400f + lazyListState.firstVisibleItemScrollOffset
            val fraction = (scrollOffset / 1200f).coerceIn(0f, 1f)
            lerp(crimson, purple, FastOutSlowInEasing.transform(fraction))
        }
    }

    val animatedGradientBrush = remember(animatedColor) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF0A0404), Color(0xFF2A0F0F), animatedColor.copy(alpha = 0.8f), animatedColor, Color(0xFF060202)),
            radius = 1200f,
            center = Offset(0.5f, 0.3f)
        )
    }

    val overlayBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF4A0000).copy(alpha = 0.2f), Color.Transparent, Color(0xFF2A0F0F).copy(alpha = 0.15f), Color.Transparent)
        )
    }

    val parsedFlow = remember(tapeFlow, zoneId) { tapeFlow.onStart { emit("") }.map { entriesFromTape(it, zoneId) } }
    val allEntries by parsedFlow.collectAsState(initial = emptyList())
    var weekOffset by remember { mutableStateOf(0) }
    val today = remember { LocalDate.now(zoneId) }
    val currentWeekStart = remember(today, firstDayOfWeek) { startOfWeek(today, firstDayOfWeek) }
    val selectedWeekStart = remember(currentWeekStart, weekOffset) { currentWeekStart.plusWeeks(weekOffset.toLong()) }
    val summary = remember(allEntries, selectedWeekStart) { summarizeWeek(allEntries, selectedWeekStart) }
    val canGoForward = remember(weekOffset) { weekOffset < 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${summary.weekStart} — ${summary.weekEnd}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { weekOffset -= 1 }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous week", tint = Color.White)
                    }
                    IconButton(onClick = { if (canGoForward) weekOffset += 1 }, enabled = canGoForward) {
                        Icon(
                            Icons.Outlined.ArrowForward,
                            contentDescription = "Next week",
                            tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060202))
            .background(animatedGradientBrush)
            .background(overlayBrush)
    ) { padding ->
        if (allEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item(key = "summary-header") {
                    GlassCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                            SummaryHeader(totalSec = summary.totalSec, totalCount = summary.totalCount)
                        }
                    }
                }
                item(key = "days") {
                    GlassCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("This Week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                            DayStrip(days = summary.days)
                        }
                    }
                }
                if (summary.byWorkout.isNotEmpty()) {
                    item(key = "by-workout-title") {
                        Text("By Workout", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp), color = Color.White)
                    }
                    items(summary.byWorkout, key = { it.name }) { agg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                            exit = fadeOut(),

                        ) {
                            WorkoutRow(agg)
                        }
                    }
                } else {
                    item(key = "no-by-workout") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No data this week", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingTaskbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navController = navController,
                cornerRadius = 34.dp,
                iconAlpha = 1f,
                uiState = androidx.compose.runtime.remember { WorkoutListUiState.Success(emptyList()) }
            )
        }
    }
}




@Composable
private fun SummaryHeader(totalSec: Long, totalCount: Int) {
    val time = remember(totalSec) { formatHMS(totalSec) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "Workouts",
            value = totalCount.toString(),
            icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Color(0xFFF48A8A)) },
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Time",
            value = time,
            icon = { Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFD32F2F))) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val crimsonContainerColor = Color(0xFF4A0000)
    Card(
        modifier.heightIn(min = 84.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = crimsonContainerColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(crimsonContainerColor.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }
    }
}

@Composable
private fun DayStrip(days: List<DaySummary>) {
    val scroll = rememberScrollState()
    val maxSec = remember(days) { max(1L, days.maxOfOrNull { it.totalSec } ?: 1L) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        days.forEach { d ->
            DayPill(
                label = d.date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() },
                value = d.totalSec,
                maxValue = maxSec,
                sub = if (d.count > 0) "${d.count}" else "0"
            )
        }
    }
}

@Composable
private fun DayPill(label: String, value: Long, maxValue: Long, sub: String) {
    val h = 64.dp
    val barWidth = 10.dp
    val pct = if (maxValue <= 0) 0f else (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .height(h)
                .width(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val density = LocalDensity.current
            val barHeightPx = with(density) { (h * pct).toPx() }
            val barWidthPx = with(density) { barWidth.toPx() }
            val cornerRadiusPx = with(density) { 6.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2f
                drawRoundRect(
                    color = Color(0xFF8B0000),
                    topLeft = Offset(x - barWidthPx / 2f, size.height - barHeightPx),
                    size = androidx.compose.ui.geometry.Size(barWidthPx, barHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkoutRow(a: WorkoutAggregate) {
    val safeName = remember(a.name) { a.name.ifBlank { "Unnamed" } }
    val time = remember(a.totalSec) { formatHMS(a.totalSec) }
    val weight = a.avgWeightKg?.let { "${(it * 10.0).roundToInt() / 10.0}kg" } ?: "BW"
    val crimsonColor = Color(0xFF4A0000)
    val crimsonHighlight = Color(0xFFF48A8A)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(crimsonColor.copy(alpha = 0.4f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(crimsonColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                safeName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = crimsonHighlight
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                safeName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(time)
                Chip("${a.count}x")
                Chip(weight)
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF6A0000).copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

object SummaryPDE { fun tapeFlow(): Flow<String> = PDE.tapeFlow() }