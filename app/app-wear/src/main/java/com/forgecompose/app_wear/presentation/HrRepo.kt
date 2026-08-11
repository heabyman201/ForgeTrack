package com.forgecompose.app_wear.presentation

import android.content.Context
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Heart-rate source backed by Samsung Health Sensor SDK.
 *
 * The tracker is intentionally owned by the foreground HR service rather than an Activity so it
 * keeps producing workout samples while the watch display is off. Samsung batches continuous
 * samples in that state, so [heartRateStream] periodically flushes the tracker to keep phone and
 * watch workout statistics current.
 */
class HrRepository(context: Context) {
    private val appContext = context.applicationContext
    private val connectionMutex = Mutex()
    private val trackerMutex = Mutex()

    @Volatile
    private var trackingService: HealthTrackingService? = null

    @Volatile
    private var heartRateTracker: HealthTracker? = null

    @Volatile
    private var connected = false

    @Volatile
    private var connectionAttempt: CompletableDeferred<Unit>? = null

    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            connected = true
            connectionAttempt?.complete(Unit)
        }

        override fun onConnectionEnded() {
            connected = false
        }

        override fun onConnectionFailed(exception: HealthTrackerException) {
            connected = false
            connectionAttempt?.completeExceptionally(exception)
        }
    }

    /** True when this Galaxy Watch exposes Samsung's continuous heart-rate tracker. */
    fun supportsHr(): Flow<Boolean> = flow {
        connectToHealthPlatform()
        emit(supportsContinuousHeartRate())
    }

    /** Connects to Health Platform and prepares Samsung's continuous HR tracker. */
    suspend fun startHrExercise(): Boolean = trackerMutex.withLock {
        if (heartRateTracker != null && connected) return@withLock true

        connectToHealthPlatform()
        val service = checkNotNull(trackingService) {
            "Samsung Health Platform connection was not created"
        }
        check(supportsContinuousHeartRate()) {
            "Samsung continuous heart-rate tracking is not supported on this watch"
        }

        heartRateTracker = service.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
        true
    }

    suspend fun ensureHrExerciseStarted(): Boolean {
        return if (heartRateTracker != null && connected) true else startHrExercise()
    }

    /** Stops Samsung tracking and releases the Health Platform service connection. */
    suspend fun endExercise() = trackerMutex.withLock {
        runCatching { heartRateTracker?.unsetEventListener() }
        heartRateTracker = null
        runCatching { trackingService?.disconnectService() }
        trackingService = null
        connected = false
        connectionAttempt = null
    }

    /** Streams valid Samsung BioActive Sensor heart-rate readings as BPM. */
    fun heartRateStream(): Flow<Int?> = callbackFlow {
        val tracker = heartRateTracker
        if (tracker == null) {
            close(IllegalStateException("Samsung heart-rate tracker has not been started"))
            return@callbackFlow
        }

        val listener = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: List<com.samsung.android.service.health.tracking.data.DataPoint>) {
                dataPoints.forEach { point ->
                    val status = point.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)
                    val bpm = point.getValue(ValueKey.HeartRateSet.HEART_RATE)
                    if (status == STATUS_SUCCESS && bpm in MIN_VALID_BPM..MAX_VALID_BPM) {
                        trySend(bpm)
                    }
                }
            }

            override fun onFlushCompleted() = Unit

            override fun onError(error: HealthTracker.TrackerError) {
                close(IllegalStateException("Samsung heart-rate tracker error: $error"))
            }
        }

        runCatching { tracker.setEventListener(listener) }
            .onFailure {
                close(it)
                return@callbackFlow
            }

        // HEART_RATE_CONTINUOUS batches while the screen is off. A regular flush provides fresh
        // workout stats without falling back to AndroidX Health Services or SensorManager.
        val flushJob = launch(Dispatchers.Default) {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                runCatching { tracker.flush() }
            }
        }

        awaitClose {
            flushJob.cancel()
            runCatching { tracker.unsetEventListener() }
        }
    }.distinctUntilChanged()

    private suspend fun connectToHealthPlatform() {
        val attempt = connectionMutex.withLock {
            if (connected && trackingService != null) return

            connectionAttempt
                ?.takeUnless { it.isCompleted }
                ?: CompletableDeferred<Unit>().also { deferred ->
                    connectionAttempt = deferred
                    val service = HealthTrackingService(connectionListener, appContext)
                    trackingService = service
                    runCatching { service.connectService() }
                        .onFailure { deferred.completeExceptionally(it) }
                }
        }

        try {
            withTimeout(CONNECTION_TIMEOUT_MS) { attempt.await() }
        } catch (error: Throwable) {
            connectionMutex.withLock {
                if (connectionAttempt === attempt) {
                    runCatching { trackingService?.disconnectService() }
                    trackingService = null
                    connectionAttempt = null
                    connected = false
                }
            }
            throw error
        }
    }

    private fun supportsContinuousHeartRate(): Boolean {
        val supported = checkNotNull(trackingService)
            .getTrackingCapability()
            .supportHealthTrackerTypes
        return HealthTrackerType.HEART_RATE_CONTINUOUS in supported
    }

    private companion object {
        private const val STATUS_SUCCESS = 1
        private const val MIN_VALID_BPM = 25
        private const val MAX_VALID_BPM = 250
        private const val FLUSH_INTERVAL_MS = 3_000L
        private const val CONNECTION_TIMEOUT_MS = 15_000L
    }
}
