package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Debug
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AppMemorySnapshot(
    val usedHeapMb: Int,
    val committedHeapMb: Int,
    val maxHeapMb: Int,
    val nativeHeapMb: Int,
    val totalPssMb: Int,
    val dalvikPssMb: Int,
    val nativePssMb: Int,
    val otherPssMb: Int,
    val privateDirtyMb: Int,
    val graphicsMb: Int,
    val codeMb: Int,
    val stackMb: Int,
    val systemMb: Int,
    val availSystemMb: Int,
    val thresholdMb: Int,
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val lowMemory: Boolean,
    val capturedAtMs: Long
)

data class MemoryMonitorState(
    val isMonitoring: Boolean = false,
    val latestSnapshot: AppMemorySnapshot? = null,
    val history: List<AppMemorySnapshot> = emptyList(),
    val sessionStartSnapshot: AppMemorySnapshot? = null,
    val anchorSnapshot: AppMemorySnapshot? = null,
    val peakSinceAnchorMb: Int? = null,
    val workoutEndSnapshot: AppMemorySnapshot? = null,
    val peakSinceWorkoutEndMb: Int? = null
)

fun captureAppMemorySnapshot(context: Context): AppMemorySnapshot {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runtime = Runtime.getRuntime()
    val processMemory = activityManager.getProcessMemoryInfo(intArrayOf(Process.myPid())).firstOrNull()
    val systemMemory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)

    val usedHeapBytes = runtime.totalMemory() - runtime.freeMemory()
    val committedHeapBytes = runtime.totalMemory()
    val maxHeapBytes = runtime.maxMemory()
    val nativeHeapBytes = Debug.getNativeHeapAllocatedSize()

    fun bytesToMb(bytes: Long): Int = (bytes / (1024L * 1024L)).toInt()
    fun kbToMb(kb: Int): Int = (kb / 1024f).toInt()
    fun statKb(key: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0
        return processMemory
            ?.memoryStats
            ?.get(key)
            ?.toIntOrNull()
            ?: 0
    }

    return AppMemorySnapshot(
        usedHeapMb = bytesToMb(usedHeapBytes),
        committedHeapMb = bytesToMb(committedHeapBytes),
        maxHeapMb = bytesToMb(maxHeapBytes),
        nativeHeapMb = bytesToMb(nativeHeapBytes),
        totalPssMb = kbToMb(processMemory?.totalPss ?: 0),
        dalvikPssMb = kbToMb(processMemory?.dalvikPss ?: 0),
        nativePssMb = kbToMb(processMemory?.nativePss ?: 0),
        otherPssMb = kbToMb(processMemory?.otherPss ?: 0),
        privateDirtyMb = kbToMb(processMemory?.totalPrivateDirty ?: 0),
        graphicsMb = kbToMb(statKb("summary.graphics")),
        codeMb = kbToMb(statKb("summary.code")),
        stackMb = kbToMb(statKb("summary.stack")),
        systemMb = kbToMb(statKb("summary.system")),
        availSystemMb = bytesToMb(systemMemory.availMem),
        thresholdMb = bytesToMb(systemMemory.threshold),
        memoryClassMb = activityManager.memoryClass,
        largeMemoryClassMb = activityManager.largeMemoryClass,
        lowMemory = systemMemory.lowMemory,
        capturedAtMs = SystemClock.elapsedRealtime()
    )
}

object AppMemoryMonitorStore {
    private const val MAX_HISTORY = 60

    private val _state = MutableStateFlow(MemoryMonitorState())
    val state: StateFlow<MemoryMonitorState> = _state.asStateFlow()

    private var previousWorkoutMode = ConnectedWorkout.currentMode.value

    fun onMonitoringStarted(initialSnapshot: AppMemorySnapshot) {
        previousWorkoutMode = ConnectedWorkout.currentMode.value
        _state.value = _state.value.copy(
            isMonitoring = true,
            latestSnapshot = initialSnapshot,
            history = listOf(initialSnapshot),
            sessionStartSnapshot = initialSnapshot,
            anchorSnapshot = initialSnapshot,
            peakSinceAnchorMb = initialSnapshot.totalPssMb,
            workoutEndSnapshot = null,
            peakSinceWorkoutEndMb = null
        )
    }

    fun onMonitoringStopped() {
        _state.value = _state.value.copy(isMonitoring = false)
    }

    fun markAnchor(snapshot: AppMemorySnapshot? = _state.value.latestSnapshot) {
        snapshot ?: return
        _state.value = _state.value.copy(
            anchorSnapshot = snapshot,
            peakSinceAnchorMb = snapshot.totalPssMb
        )
    }

    fun recordSnapshot(snapshot: AppMemorySnapshot, currentWorkoutMode: ConnectedWorkout.WorkoutMode) {
        val currentState = _state.value
        val nextHistory = (currentState.history + snapshot).takeLast(MAX_HISTORY)
        val workoutJustEnded =
            previousWorkoutMode != ConnectedWorkout.WorkoutMode.INACTIVE &&
                currentWorkoutMode == ConnectedWorkout.WorkoutMode.INACTIVE

        val workoutEndSnapshot = if (workoutJustEnded) snapshot else currentState.workoutEndSnapshot
        val peakSinceWorkoutEndMb = when {
            workoutJustEnded -> snapshot.totalPssMb
            workoutEndSnapshot != null -> maxOf(currentState.peakSinceWorkoutEndMb ?: workoutEndSnapshot.totalPssMb, snapshot.totalPssMb)
            else -> null
        }
        val anchorSnapshot = currentState.anchorSnapshot ?: currentState.sessionStartSnapshot ?: snapshot
        val peakSinceAnchorMb = maxOf(currentState.peakSinceAnchorMb ?: anchorSnapshot.totalPssMb, snapshot.totalPssMb)

        _state.value = currentState.copy(
            isMonitoring = true,
            latestSnapshot = snapshot,
            history = nextHistory,
            sessionStartSnapshot = currentState.sessionStartSnapshot ?: snapshot,
            anchorSnapshot = anchorSnapshot,
            peakSinceAnchorMb = peakSinceAnchorMb,
            workoutEndSnapshot = workoutEndSnapshot,
            peakSinceWorkoutEndMb = peakSinceWorkoutEndMb
        )
        previousWorkoutMode = currentWorkoutMode
    }
}

private fun formatMemoryDelta(deltaMb: Int): String {
    return if (deltaMb > 0) "+${deltaMb} MB" else "${deltaMb} MB"
}

class MemoryMonitorForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "memory_monitor_channel"
        private const val CHANNEL_NAME = "Memory Monitor"
        private const val NOTIFICATION_ID = 1441
        private const val ACTION_START = "com.forgecompose.workouttracker.memory.START"
        private const val ACTION_STOP = "com.forgecompose.workouttracker.memory.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MemoryMonitorForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MemoryMonitorForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == MemoryMonitorForegroundService::class.java.name }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private val accentColorInt = "#8B0000".toColorInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            ACTION_START, null -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        AppMemoryMonitorStore.onMonitoringStopped()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) {
            updateNotification(AppMemoryMonitorStore.state.value.latestSnapshot)
            return
        }

        val initialSnapshot = captureAppMemorySnapshot(this)
        AppMemoryMonitorStore.onMonitoringStarted(initialSnapshot)
        startForeground(NOTIFICATION_ID, buildNotification(initialSnapshot))

        monitorJob = scope.launch {
            while (isActive) {
                val snapshot = captureAppMemorySnapshot(this@MemoryMonitorForegroundService)
                AppMemoryMonitorStore.recordSnapshot(snapshot, ConnectedWorkout.currentMode.value)
                updateNotification(snapshot)
                delay(1000)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        AppMemoryMonitorStore.onMonitoringStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(snapshot: AppMemorySnapshot?) {
        snapshot ?: return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun buildNotification(snapshot: AppMemorySnapshot): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, MemoryMonitorForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val state = AppMemoryMonitorStore.state.value
        val anchorDelta = state.anchorSnapshot?.let { snapshot.totalPssMb - it.totalPssMb }
        val content = "PSS ${snapshot.totalPssMb} MB • Heap ${snapshot.usedHeapMb} MB"
        val subText = buildString {
            if (anchorDelta != null) {
                append(formatMemoryDelta(anchorDelta))
                append(" since anchor")
            } else {
                append("Sampling app memory")
            }
            if (snapshot.lowMemory) {
                append(" • Low memory")
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_mono)
            .setContentTitle("Memory Monitor Active")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content\n$subText"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(accentColorInt)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live app memory usage"
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
