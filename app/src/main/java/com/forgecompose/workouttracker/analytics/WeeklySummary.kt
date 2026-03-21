package com.forgecompose.workouttracker.analytics

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    tapeFlow: Flow<String>,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    zoneId: ZoneId = ZoneId.systemDefault(),
    navController: NavController
) {
    val context = LocalContext.current

    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val cfg = LocalConfiguration.current
    val widthDp = cfg.screenWidthDp
    val heightDp = cfg.screenHeightDp
    val isTall = heightDp >= 600
    val isTwoPane = widthDp >= 840 || (widthDp >= 600 && isTall)
    val compactTitle = widthDp < 360


    val density = LocalDensity.current
    val maxDimension = remember(density, widthDp, heightDp) {
        with(density) { max(widthDp.dp.toPx(), heightDp.dp.toPx()) }
    }


    val ambientGlowBrush = remember(theme, maxDimension) {
        Brush.radialGradient(
            colors = listOf(
                theme.secondary.copy(alpha = 0.15f),
                theme.background.copy(alpha = 0.7f),
                theme.background
            ),
            center = Offset(0.3f, 0.4f),
            radius = maxDimension * 1.2f
        )
    }


    val detailedStreakBrush = remember(theme) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to theme.tertiary.copy(alpha = 0.05f),
                0.3f to Color.Transparent,
                0.5f to theme.secondary.copy(alpha = 0.08f),
                0.7f to Color.Transparent,
                1.0f to theme.tertiary.copy(alpha = 0.1f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // Diagonal
        )
    }

    // Layer 3: A subtle, repeating linear gradient for fine texture
    val fineTextureBrush = remember(theme) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                theme.secondary.copy(alpha = 0.03f),
                Color.Transparent
            ),
            startY = 0f,
            endY = 100f, // Small repeat interval
            tileMode = TileMode.Repeated
        )
    }

    // Layer 4: A corner vignette to focus attention towards the center
    val vignetteBrush = remember(theme, maxDimension) {
        Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                theme.background.copy(alpha = 0.5f)
            ),
            center = Offset(1f, 1f), // Bottom-right corner in relative coordinates
            radius = maxDimension * 0.8f
        )
    }


    val parsedFlow = remember(tapeFlow, zoneId) { tapeFlow.onStart { emit("") }.map { entriesFromTape(it, zoneId) } }
    val allEntries by parsedFlow.collectAsState(initial = emptyList())

    val today = remember { LocalDate.now(zoneId) }
    val currentWeekStart = remember(today, firstDayOfWeek) { startOfWeek(today, firstDayOfWeek) }
    var selectedWeekStart by remember { mutableStateOf(currentWeekStart) }
    var isCalendarExpanded by remember { mutableStateOf(false) }

    val summary = remember(allEntries, selectedWeekStart) { summarizeWeek(allEntries, selectedWeekStart) }
    val canGoForward = remember(selectedWeekStart, currentWeekStart) { selectedWeekStart < currentWeekStart }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "${summary.weekStart} — ${summary.weekEnd}",
                            style = if (compactTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = if (compactTitle) 0.sp else 0.2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedWeekStart = selectedWeekStart.minusWeeks(1) }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous week", tint = Color.White)
                        }
                        IconButton(onClick = { isCalendarExpanded = !isCalendarExpanded }) {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = "Select Week",
                                tint = if (isCalendarExpanded) theme.primary else Color.White
                            )
                        }
                        IconButton(onClick = { if (canGoForward) selectedWeekStart = selectedWeekStart.plusWeeks(1) }, enabled = canGoForward) {
                            Icon(
                                Icons.Outlined.ArrowForward,
                                contentDescription = "Next week",
                                tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                AnimatedVisibility(
                    visible = isCalendarExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    InlineCalendar(
                        selectedWeekStart = selectedWeekStart,
                        onWeekSelected = {
                            selectedWeekStart = it
                            isCalendarExpanded = false
                        },
                        firstDayOfWeek = firstDayOfWeek,
                        theme = theme
                    )
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            // Apply the layered, detailed, static background
            .background(theme.background) // Base solid color
            .background(ambientGlowBrush)   // Layer 1: Ambient depth
            .background(detailedStreakBrush) // Layer 2: Color streaks
            .background(fineTextureBrush)    // Layer 3: Fine texture
            .background(vignetteBrush)       // Layer 4: Corner vignette
    ) { padding ->
        if (allEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (isTwoPane) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        item(key = "summary-header") {
                            GlassCard {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    SummaryHeader(totalSec = summary.totalSec, totalCount = summary.totalCount, theme = theme)
                                }
                            }
                        }
                        item(key = "days") {
                            GlassCard {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("This Week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    DayStrip(days = summary.days, theme = theme)
                                }
                            }
                        }
                    }
                    LazyColumn(
                        state = rememberLazyListState(), // Use a separate state for the second column
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        if (summary.byWorkout.isNotEmpty()) {
                            item(key = "by-workout-title") {
                                Text("By Workout", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            items(summary.byWorkout, key = { it.name }) { agg ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                                    exit = fadeOut()
                                ) {
                                    WorkoutRow(agg, theme = theme)
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
            } else {
                LazyColumn(
                    state = rememberLazyListState(), // Use a separate state
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = if (widthDp >= 400) 16.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item(key = "summary-header") {
                        GlassCard {
                            Column(Modifier.padding(if (widthDp >= 400) 20.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Summary", style = if (widthDp >= 400) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                SummaryHeader(totalSec = summary.totalSec, totalCount = summary.totalCount, theme = theme)
                            }
                        }
                    }
                    item(key = "days") {
                        GlassCard {
                            Column(Modifier.padding(if (widthDp >= 400) 20.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("This Week", style = if (widthDp >= 400) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                DayStrip(days = summary.days, theme = theme)
                            }
                        }
                    }
                    if (summary.byWorkout.isNotEmpty()) {
                        item(key = "by-workout-title") {
                            Text("By Workout", style = if (widthDp >= 400) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        items(summary.byWorkout, key = { it.name }) { agg ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                                exit = fadeOut()
                            ) {
                                WorkoutRow(agg, theme = theme)
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
        }
    }
}

@Composable
fun InlineCalendar(
    selectedWeekStart: LocalDate,
    onWeekSelected: (LocalDate) -> Unit,
    firstDayOfWeek: DayOfWeek,
    theme: ColorSchemeAppTheme
) {
    var viewMonth by remember(selectedWeekStart) { mutableStateOf(YearMonth.from(selectedWeekStart)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewMonth = viewMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
            }
            Text(
                "${viewMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${viewMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { viewMonth = viewMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val days = WeekFields.of(firstDayOfWeek, 1).firstDayOfWeek
            for (i in 0..6) {
                val d = days.plus(i.toLong())
                Text(
                    text = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.secondary.copy(alpha = 0.7f)
                )
            }
        }

        val firstDayOfMonth = viewMonth.atDay(1)
        val firstDayOfGrid = firstDayOfMonth.minusDays(
            ((firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong()
        )

        val selectedWeekEnd = selectedWeekStart.plusDays(6)

        Column {
            for (w in 0 until 6) {
                val weekStartDate = firstDayOfGrid.plusDays((w * 7).toLong())
                // Stop rendering if row is completely outside current month
                if (weekStartDate.month != viewMonth.month && weekStartDate.plusDays(6).month != viewMonth.month && w > 3) break

                val isSelectedWeek = (selectedWeekStart <= weekStartDate.plusDays(6) && selectedWeekEnd >= weekStartDate)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelectedWeek) theme.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onWeekSelected(weekStartDate) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        for (d in 0 until 7) {
                            val date = weekStartDate.plusDays(d.toLong())
                            val isToday = date == LocalDate.now()
                            val isCurrentMonth = date.month == viewMonth.month

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isToday) {
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(theme.secondary.copy(alpha = 0.6f))
                                    )
                                }
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrentMonth) Color.White else Color.White.copy(alpha = 0.3f),
                                    fontWeight = if (isSelectedWeek) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(totalSec: Long, totalCount: Int, theme: ColorSchemeAppTheme) {
    val time = remember(totalSec) { formatHMS(totalSec) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "Workouts",
            value = totalCount.toString(),
            icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = theme.primary.copy(alpha = 0.8f)) },
            modifier = Modifier.weight(1f),
            theme = theme
        )
        StatCard(
            title = "Time",
            value = time,
            icon = { Box(Modifier.size(20.dp).clip(CircleShape).background(theme.primary.copy(alpha = 0.9f))) },
            modifier = Modifier.weight(1f),
            theme = theme
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: @Composable () -> Unit, modifier: Modifier = Modifier, theme: ColorSchemeAppTheme) {
    val cardBg = theme.secondary
    Card(
        modifier.heightIn(min = 84.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cardBg.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }
}

@Composable
private fun DayStrip(days: List<DaySummary>, theme: ColorSchemeAppTheme) {
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
                sub = if (d.count > 0) "${d.count}" else "0",
                theme = theme
            )
        }
    }
}

@Composable
private fun DayPill(label: String, value: Long, maxValue: Long, sub: String, theme: ColorSchemeAppTheme) {
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
                    color = theme.primary,
                    topLeft = Offset(x - barWidthPx / 2f, size.height - barHeightPx),
                    size = androidx.compose.ui.geometry.Size(barWidthPx, barHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun WorkoutRow(a: WorkoutAggregate, theme: ColorSchemeAppTheme) {
    val cfg = LocalConfiguration.current
    val widthDp = cfg.screenWidthDp
    val safeName = remember(a.name) { a.name.ifBlank { "Unnamed" } }
    val time = remember(a.totalSec) { formatHMS(a.totalSec) }
    val weight = a.avgWeightKg?.let { "${(it * 10.0).roundToInt() / 10.0}kg" } ?: "BW"

    val cardBg = theme.secondary
    val highlight = theme.primary

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg.copy(alpha = 0.4f))
            .padding(horizontal = if (widthDp >= 400) 14.dp else 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(if (widthDp >= 400) 40.dp else 36.dp)
                .clip(CircleShape)
                .background(cardBg.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                safeName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = highlight,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(if (widthDp >= 400) 12.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                safeName,
                style = if (widthDp >= 400) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(time, theme)
                Chip("${a.count}x", theme)
                Chip(weight, theme)
            }
        }
    }
}

@Composable
private fun Chip(text: String, theme: ColorSchemeAppTheme) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(theme.tertiary.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

object SummaryPDE { fun tapeFlow(): Flow<String> = PDE.tapeFlow() }