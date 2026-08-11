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
private const val REST_TIMER_TICK_MILLIS = 1_000L
private const val HR_SAMPLE_HISTORY_LIMIT = 12
private const val MIN_VALID_HEART_RATE_BPM = 35
private const val MAX_VALID_HEART_RATE_BPM = 230
private const val HIGH_REST_HEART_RATE_BPM = 125
private const val MIN_ELEVATED_REST_HEART_RATE_BPM = 115
private const val MIN_HR_DROP_FOR_FAST_RECOVERY_BPM = 18
private const val MIN_FAST_RECOVERY_RATE_BPM_PER_MINUTE = 12.0
private const val HIGH_HR_TICKS_BEFORE_EXTENSION = 5
private const val MAX_HR_REST_EXTENSION_MILLIS = 30_000L
private const val MAX_FAST_RECOVERY_REDUCTION_MILLIS = 30_000L

private data class HeartRateSample(
    val bpm: Int,
    val capturedAtElapsedRealtime: Long
)

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
    private val restHeartRateSamples = ArrayDeque<HeartRateSample>()
    private var restStartHeartRateBpm: Int? = null
    private var consecutiveHighHeartRateTicks = 0
    private var addedForHighHeartRateMillis = 0L
    private var reducedForFastRecoveryMillis = 0L

    /**
     * Starts a rest period and records the heart rate immediately after the set when it is
     * available. That value is used only as a recovery baseline; invalid or missing readings
     * leave the normal timer behavior intact.
     */
    fun startRest(
        heartRateBpm: Int? = null,
        nowMillis: Long = SystemClock.elapsedRealtime()
    ) {
        restStartedAtElapsedRealtime = nowMillis
        activeSetStartedAtElapsedRealtime = null
        restHeartRateSamples.clear()
        restStartHeartRateBpm = heartRateBpm?.takeIf(::isValidHeartRate)
        consecutiveHighHeartRateTicks = 0
        addedForHighHeartRateMillis = 0L
        reducedForFastRecoveryMillis = 0L
        restStartHeartRateBpm?.let { recordRestHeartRate(it, nowMillis) }
    }

    /**
     * Produces the next rest-timer value from the most recent live heart-rate reading.
     *
     * A persistently elevated heart rate holds the countdown and adds one second after every
     * five high-HR ticks (up to 30 seconds). A rapid, safe recovery removes an extra second
     * per tick while at least the minimum auto-rest duration remains. With no reliable HR data,
     * this is exactly a normal one-second countdown.
     */
    fun nextRestCountdownMillis(
        remainingMillis: Long,
        heartRateBpm: Int?,
        nowMillis: Long = SystemClock.elapsedRealtime()
    ): Long {
        val safeRemaining = remainingMillis.coerceAtLeast(0L)
        if (safeRemaining == 0L) return 0L

        val heartRate = heartRateBpm?.takeIf(::isValidHeartRate)
        if (heartRate == null) {
            consecutiveHighHeartRateTicks = 0
            return (safeRemaining - REST_TIMER_TICK_MILLIS).coerceAtLeast(0L)
        }

        recordRestHeartRate(heartRate, nowMillis)
        val smoothedHeartRate = recentRestHeartRateAverage() ?: heartRate
        val restStartedAt = restStartedAtElapsedRealtime ?: nowMillis
        val elapsedRestMillis = (nowMillis - restStartedAt).coerceAtLeast(REST_TIMER_TICK_MILLIS)
        val startHeartRate = restStartHeartRateBpm ?: smoothedHeartRate.also {
            restStartHeartRateBpm = it
        }
        val recoveredBpm = (startHeartRate - smoothedHeartRate).coerceAtLeast(0)
        val recoveryRatePerMinute = recoveredBpm * 60_000.0 / elapsedRestMillis.toDouble()
        val highHeartRateThreshold = maxOf(
            HIGH_REST_HEART_RATE_BPM,
            (startHeartRate * 0.82).roundToLong().toInt()
        )
        val isStillElevated = smoothedHeartRate >= highHeartRateThreshold ||
            (smoothedHeartRate >= MIN_ELEVATED_REST_HEART_RATE_BPM &&
                elapsedRestMillis >= 15_000L && recoveredBpm < 8)

        if (isStillElevated) {
            consecutiveHighHeartRateTicks += 1
            val canExtend = addedForHighHeartRateMillis < MAX_HR_REST_EXTENSION_MILLIS
            return if (canExtend && consecutiveHighHeartRateTicks % HIGH_HR_TICKS_BEFORE_EXTENSION == 0) {
                addedForHighHeartRateMillis += REST_TIMER_TICK_MILLIS
                (safeRemaining + REST_TIMER_TICK_MILLIS).coerceAtMost(MAX_AUTO_REST_TIME_MILLIS)
            } else {
                // Do not consume the rest period until the heart rate has started to recover.
                safeRemaining
            }
        }

        consecutiveHighHeartRateTicks = 0
        val isRapidRecovery = recoveredBpm >= MIN_HR_DROP_FOR_FAST_RECOVERY_BPM &&
            recoveryRatePerMinute >= MIN_FAST_RECOVERY_RATE_BPM_PER_MINUTE &&
            smoothedHeartRate <= HIGH_REST_HEART_RATE_BPM
        val canAccelerate = safeRemaining > MIN_AUTO_REST_TIME_MILLIS &&
            reducedForFastRecoveryMillis < MAX_FAST_RECOVERY_REDUCTION_MILLIS

        return if (isRapidRecovery && canAccelerate) {
            reducedForFastRecoveryMillis += REST_TIMER_TICK_MILLIS
            (safeRemaining - (REST_TIMER_TICK_MILLIS * 2L))
                .coerceAtLeast(MIN_AUTO_REST_TIME_MILLIS)
        } else {
            (safeRemaining - REST_TIMER_TICK_MILLIS).coerceAtLeast(0L)
        }
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
        clearRestHeartRateState()

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
        clearRestHeartRateState()
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

    private fun isValidHeartRate(bpm: Int): Boolean =
        bpm in MIN_VALID_HEART_RATE_BPM..MAX_VALID_HEART_RATE_BPM

    private fun recordRestHeartRate(bpm: Int, nowMillis: Long) {
        if (restHeartRateSamples.size >= HR_SAMPLE_HISTORY_LIMIT) {
            restHeartRateSamples.removeFirst()
        }
        restHeartRateSamples.addLast(HeartRateSample(bpm, nowMillis))
    }

    private fun recentRestHeartRateAverage(): Int? {
        if (restHeartRateSamples.isEmpty()) return null
        return restHeartRateSamples
            .takeLast(3)
            .map { it.bpm }
            .average()
            .roundToLong()
            .toInt()
    }

    private fun clearRestHeartRateState() {
        restHeartRateSamples.clear()
        restStartHeartRateBpm = null
        consecutiveHighHeartRateTicks = 0
        addedForHighHeartRateMillis = 0L
        reducedForFastRecoveryMillis = 0L
    }
}
