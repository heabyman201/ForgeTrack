package com.forgecompose.workouttracker

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@SuppressLint("StaticFieldLeak")
object PDE {
    private val Context.pdeStore: DataStore<Preferences> by preferencesDataStore(name = "pde_store")
    private lateinit var app: Context
    private val KEY_TAPE = stringPreferencesKey("pde_tape")

    private var maxCharsTotal: Int = 16_000

    fun init(context: Context, maxCharsTotal: Int = 16_000) {
        this.app = context.applicationContext
        this.maxCharsTotal = maxCharsTotal.coerceAtLeast(1_000)
    }

    /**
     * Log a single workout entry.
     * @param workoutName e.g., "Bench Press"
     * @param timeMillis  duration in milliseconds (non-negative)
     * @param weightKg    null => bodyweight; otherwise kg (e.g., 60f)
     */
    suspend fun logWorkout(workoutName: String, timeMillis: Long, weightKg: Float?) {
        ensureInit()

        val ts = nowStamp()
        val name = normText(workoutName).take(48).ifBlank { "Unnamed" }
        val timeStr = formatDuration(timeMillis.coerceAtLeast(0))
        val weightStr = weightKg?.let { formatWeight(it) } ?: "BW"

        val line = "[$ts] workout: name=$name | time=$timeStr | weight=$weightStr\n"

        app.pdeStore.edit { prefs ->
            val current = prefs[KEY_TAPE].orEmpty()
            val next = trimToLimit(current + line, maxCharsTotal)
            prefs[KEY_TAPE] = next
        }
    }

    suspend fun readTape(): String = tapeFlow().first()

    fun tapeFlow(): Flow<String> = app.pdeStore.data.map { it[KEY_TAPE].orEmpty() }

    suspend fun clear() {
        ensureInit()
        app.pdeStore.edit { it[KEY_TAPE] = "" }
    }

    private fun ensureInit() {
        check(::app.isInitialized) { "PDE.init(context) must be called before use." }
    }

    private fun nowStamp(): String {
        val zdt = Instant.now().atZone(ZoneId.of("UTC"))
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'", Locale.US).format(zdt)
    }

    private fun normText(s: String): String =
        s.replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun formatWeight(w: Float): String {
        val v = if (w.isNaN() || w.isInfinite()) 0f else w
        val asInt = v.toInt()
        return if (abs(v % 1f) == 0f) "${asInt}kg"
        else "${"%.1f".format(Locale.US, v)}kg"
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            append("${s}s")
        }.trim()
    }

    private fun trimToLimit(s: String, limit: Int): String {
        if (s.length <= limit) return s
        val tail = s.takeLast(limit)
        val cut = tail.indexOf('\n')
        return if (cut == -1) tail else tail.substring(cut + 1)
    }
}
