package com.forgecompose.app_wear.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.forgecompose.app_wear.passive.registerPassiveHr
import com.forgecompose.app_wear.passive.unregisterPassiveHr
import com.forgecompose.app_wear.presentation.theme.hasHeartRatePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object HrMonitorRuntime {
    val bpm = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val running = kotlinx.coroutines.flow.MutableStateFlow(false)
    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}

class HrMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectJob: Job? = null
    private var explicitStopRequested: Boolean = false
    private var lastUiBpm: Int? = null
    private var lastUiPushMs: Long = 0L
    private var lastDataLayerBpm: Int? = null
    private var lastDataLayerPushMs: Long = 0L
    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WorkoutTracker:HrMonitorWakeLock"
            )
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            when (intent?.action) {
                ACTION_STOP -> {
                    explicitStopRequested = true
                    rememberExplicitStop(true)
                    stopMonitoring(endExercise = true, clearRuntime = true)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                ACTION_START -> {
                    explicitStopRequested = false
                    rememberExplicitStop(false)
                    startForeground(NOTIFICATION_ID, buildNotification("Starting HR monitor"))
                    startMonitoring(forceStart = false)
                }
                ACTION_FORCE_START -> {
                    explicitStopRequested = false
                    rememberExplicitStop(false)
                    startForeground(NOTIFICATION_ID, buildNotification("Force starting HR monitor"))
                    startMonitoring(forceStart = true)
                }
                else -> {
                    explicitStopRequested = readExplicitStop()
                    startForeground(NOTIFICATION_ID, buildNotification("HR monitor active"))
                    if (!explicitStopRequested && !HrMonitorRuntime.running.value) {
                        startMonitoring(forceStart = false)
                    }
                }
            }
        }.onFailure {
            HrMonitorRuntime.running.value = false
            val cause = it.message?.take(80) ?: "unknown"
            HrMonitorRuntime.error.value = "Start failed: $cause"
            stopSelf()
        }
        return if (explicitStopRequested) START_NOT_STICKY else START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring(endExercise = false, clearRuntime = false)
        if (!explicitStopRequested && HrMonitorRuntime.running.value) {
            val restartIntent = Intent(applicationContext, HrMonitorService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(restartIntent)
                } else {
                    applicationContext.startService(restartIntent)
                }
            }
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!explicitStopRequested) {
            runCatching {
                val restartIntent = Intent(applicationContext, HrMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(restartIntent)
                } else {
                    applicationContext.startService(restartIntent)
                }
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startMonitoring(forceStart: Boolean = false) {
        if (collectJob?.isActive == true) return
        collectJob?.cancel()
        collectJob = null
        val appContext = applicationContext
        if (!forceStart && !hasRequiredPermissions(appContext)) {
            HrMonitorRuntime.running.value = false
            HrMonitorRuntime.error.value = "Grant Sensors permission"
            stopSelf()
            return
        }
        HrMonitorRuntime.error.value = null
        if (!wakeLock.isHeld) wakeLock.acquire()
        collectJob = serviceScope.launch {
            val repo = HrRepository(appContext)
            val sync = WearHrSync(appContext)
            runCatching { registerPassiveHr(appContext) }

            while (kotlinx.coroutines.currentCoroutineContext().isActive && !explicitStopRequested) {
                val started = runCatching { repo.ensureHrExerciseStarted() }
                if (started.isFailure) {
                    HrMonitorRuntime.running.value = false
                    HrMonitorRuntime.error.value = "Unable to start HR sensor"
                    updateNotification("Waiting for HR sensor")
                    kotlinx.coroutines.delay(RECOVERY_DELAY_MS)
                    continue
                }
                HrMonitorRuntime.running.value = true
                updateNotification("HR monitor active")

                val streamResult = runCatching {
                    repo.heartRateStream()
                        .filterNotNull()
                        .conflate()
                        .sample(2000)
                        .onEach { bpm ->
                            val now = System.currentTimeMillis()

                            // UI refresh budget: avoid high-frequency recomposition for tiny HR jitter.
                            val shouldUpdateUi =
                                lastUiBpm == null ||
                                    kotlin.math.abs(bpm - (lastUiBpm ?: bpm)) >= 3 ||
                                    (now - lastUiPushMs) >= 4000L
                            if (shouldUpdateUi) {
                                HrMonitorRuntime.bpm.value = bpm
                                lastUiBpm = bpm
                                lastUiPushMs = now
                            }

                            // DataLayer budget: push less often unless BPM changed meaningfully.
                            val shouldSendToPhone =
                                lastDataLayerBpm == null ||
                                    kotlin.math.abs(bpm - (lastDataLayerBpm ?: bpm)) >= 4 ||
                                    (now - lastDataLayerPushMs) >= 5000L
                            if (shouldSendToPhone) {
                                runCatching { sync.sendBpm(bpm) }
                                lastDataLayerBpm = bpm
                                lastDataLayerPushMs = now
                            }
                        }
                        .collect {}
                }

                if (!kotlinx.coroutines.currentCoroutineContext().isActive || explicitStopRequested) {
                    break
                }

                HrMonitorRuntime.running.value = false
                HrMonitorRuntime.error.value = "HR stream interrupted, recovering"
                updateNotification("Recovering HR monitor")
                runCatching { repo.endExercise() }
                if (streamResult.exceptionOrNull() != null) {
                    kotlinx.coroutines.delay(RECOVERY_DELAY_MS)
                }
            }
        }
    }

    private fun stopMonitoring(endExercise: Boolean, clearRuntime: Boolean) {
        collectJob?.cancel()
        collectJob = null
        if (wakeLock.isHeld) wakeLock.release()
        serviceScope.launch {
            if (endExercise) {
                runCatching { HrRepository(applicationContext).endExercise() }
            }
            if (clearRuntime) {
                runCatching { unregisterPassiveHr(applicationContext) }
            }
            if (clearRuntime) {
                HrMonitorRuntime.running.value = false
                HrMonitorRuntime.bpm.value = null
                HrMonitorRuntime.error.value = null
            }
            lastUiBpm = null
            lastUiPushMs = 0L
            lastDataLayerBpm = null
            lastDataLayerPushMs = 0L
        }
    }

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Heart Rate Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Heart Rate Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun rememberExplicitStop(value: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPLICIT_STOP, value)
            .apply()
    }

    private fun readExplicitStop(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXPLICIT_STOP, false)
    }

    companion object {
        private const val CHANNEL_ID = "hr_monitor_channel"
        private const val NOTIFICATION_ID = 4401
        private const val PREFS_NAME = "hr_monitor_prefs"
        private const val KEY_EXPLICIT_STOP = "explicit_stop"
        private const val RECOVERY_DELAY_MS = 2_000L
        const val ACTION_START = "com.forgecompose.app_wear.hr.START"
        const val ACTION_FORCE_START = "com.forgecompose.app_wear.hr.FORCE_START"
        const val ACTION_STOP = "com.forgecompose.app_wear.hr.STOP"

        fun isExplicitStopRequested(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_EXPLICIT_STOP, false)
        }
    }
}

private fun hasRequiredPermissions(context: Context): Boolean {
    return hasHeartRatePermission(context)
}
