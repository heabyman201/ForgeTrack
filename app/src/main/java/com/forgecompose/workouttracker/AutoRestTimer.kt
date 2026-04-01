package com.forgecompose.workouttracker

import android.os.SystemClock
import kotlin.math.roundToLong

private const val MIN_AUTO_REST_TIME_MILLIS = 15_000L
private const val MIN_AUTO_REST_ADJUSTMENT_MILLIS = 5_000L
private const val MAX_AUTO_REST_ADJUSTMENT_MILLIS = 30_000L
private const val MAX_AUTO_REST_TIME_MILLIS = 300_000L
private const val SLOW_SET_BUFFER_MILLIS = 10_000L
private const val SLOW_SET_BUFFER_RATIO = 1.25
private const val AUTO_REST_HISTORY_LIMIT = 6

private fun millisToTimeParts(totalMillis: Long): Triple<Long, Long, Long> {
    val safeMillis = totalMillis.coerceAtLeast(0L)
    val hours = safeMillis / (1000L * 60L * 60L)
    val minutes = (safeMillis / (1000L * 60L)) % 60L
    val seconds = (safeMillis / 1000L) % 60L
    return Triple(hours, minutes, seconds)
}

private fun timePartsToMillis(hours: Long, minutes: Long, seconds: Long): Long {
    return ((hours * 60L * 60L) + (minutes * 60L) + seconds) * 1000L
}

private fun normalizeRestMillis(totalMillis: Long): Long {
    val (hours, minutes, seconds) = millisToTimeParts(totalMillis)
    return timePartsToMillis(hours, minutes, seconds)
}

private fun calculateAdaptiveRestTime(
    currentRestMillis: Long,
    observedRestMillis: Long,
    skipped: Boolean
): Long {
    val safeCurrentRest = currentRestMillis.coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
    if (!skipped) return safeCurrentRest

    val earlyExitMillis = (safeCurrentRest - observedRestMillis).coerceAtLeast(0L)
    if (earlyExitMillis < MIN_AUTO_REST_ADJUSTMENT_MILLIS) return safeCurrentRest

    val shavedMillis = (earlyExitMillis / 2L)
        .coerceIn(MIN_AUTO_REST_ADJUSTMENT_MILLIS, MAX_AUTO_REST_ADJUSTMENT_MILLIS)
    return normalizeRestMillis(
        (safeCurrentRest - shavedMillis).coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
    )
}

private fun calculateExtendedRestTime(
    currentRestMillis: Long,
    baselineSetMillis: Long,
    observedSetMillis: Long
): Long {
    val safeCurrentRest = currentRestMillis.coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
    val slowByRatio = observedSetMillis >= (baselineSetMillis * SLOW_SET_BUFFER_RATIO).roundToLong()
    val overtimeMillis = observedSetMillis - baselineSetMillis
    if (!slowByRatio || overtimeMillis < SLOW_SET_BUFFER_MILLIS) return safeCurrentRest

    val addedMillis = (overtimeMillis / 2L)
        .coerceIn(MIN_AUTO_REST_ADJUSTMENT_MILLIS, MAX_AUTO_REST_ADJUSTMENT_MILLIS)
    return normalizeRestMillis(
        (safeCurrentRest + addedMillis).coerceAtMost(MAX_AUTO_REST_TIME_MILLIS)
    )
}

object AutoRestTimer {
    private var restStartedAtElapsedRealtime: Long? = null
    private var activeSetStartedAtElapsedRealtime: Long? = null
    private val postRestSetDurationsMillis = ArrayDeque<Long>()

    fun startRest(nowMillis: Long = SystemClock.elapsedRealtime()) {
        restStartedAtElapsedRealtime = nowMillis
        activeSetStartedAtElapsedRealtime = null
    }

    fun finishRest(
        currentRestMillis: Long,
        skipped: Boolean,
        autoAdjustEnabled: Boolean,
        nowMillis: Long = SystemClock.elapsedRealtime()
    ): Long {
        val safeCurrentRest = currentRestMillis.coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
        val restStartedAt = restStartedAtElapsedRealtime
        restStartedAtElapsedRealtime = null
        activeSetStartedAtElapsedRealtime = nowMillis

        if (!autoAdjustEnabled) return safeCurrentRest

        val observedRestMillis = (nowMillis - (restStartedAt ?: nowMillis))
            .coerceAtLeast(1000L)

        return calculateAdaptiveRestTime(
            currentRestMillis = safeCurrentRest,
            observedRestMillis = observedRestMillis,
            skipped = skipped
        )
    }

    fun finishSet(
        currentRestMillis: Long,
        autoAdjustEnabled: Boolean,
        nowMillis: Long = SystemClock.elapsedRealtime()
    ): Long {
        val safeCurrentRest = currentRestMillis.coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
        val activeSetStartedAt = activeSetStartedAtElapsedRealtime ?: return safeCurrentRest
        activeSetStartedAtElapsedRealtime = null

        val observedSetMillis = (nowMillis - activeSetStartedAt).coerceAtLeast(1000L)
        val baselineSetMillis = recentSetBaselineMillis()
        recordPostRestSetDuration(observedSetMillis)

        if (!autoAdjustEnabled || baselineSetMillis == null) return safeCurrentRest

        return calculateExtendedRestTime(
            currentRestMillis = safeCurrentRest,
            baselineSetMillis = baselineSetMillis,
            observedSetMillis = observedSetMillis
        )
    }

    fun reset() {
        restStartedAtElapsedRealtime = null
        activeSetStartedAtElapsedRealtime = null
        postRestSetDurationsMillis.clear()
    }

    private fun recentSetBaselineMillis(): Long? {
        if (postRestSetDurationsMillis.isEmpty()) return null
        return postRestSetDurationsMillis
            .toList()
            .takeLast(3)
            .average()
            .roundToLong()
            .coerceAtLeast(1000L)
    }

    private fun recordPostRestSetDuration(durationMillis: Long) {
        if (postRestSetDurationsMillis.size >= AUTO_REST_HISTORY_LIMIT) {
            postRestSetDurationsMillis.removeFirst()
        }
        postRestSetDurationsMillis.addLast(durationMillis)
    }
}
