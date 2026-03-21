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

import android.app.Notification
import android.app.Notification.Action
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class WorkoutForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "workout_progress_channel"
        const val CHANNEL_NAME = "Workout Progress"
        const val NOTIF_ID = 1337
        const val ACTION_START = "com.example.workouttracker.ACTION_START"
        const val ACTION_STOP = "com.example.workouttracker.ACTION_STOP"

        fun start(context: Context) {
            val i = Intent(context, WorkoutForegroundService::class.java).apply { action = ACTION_START }
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, WorkoutForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(i)
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == WorkoutForegroundService::class.java.name }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastA: String = ""
    private var lastB: String = ""
    private var lastC: Int = -1
    private var lastD: Int = -1
    private var lastE: Boolean = false
    private var screenOn: Boolean = true
    private var powerSave: Boolean = false
    private val accentColorInt = "#8B0000".toColorInt()

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_USER_PRESENT -> screenOn = true
                Intent.ACTION_SCREEN_OFF -> screenOn = false
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                    powerSave = pm.isPowerSaveMode
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        powerSave = pm.isPowerSaveMode
        screenOn = pm.isInteractive
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> startTicking()
        }
        return START_STICKY
    }

    private fun currentInterval(): Long {
        return when {

            powerSave && !screenOn -> 10000L
            !screenOn -> 6000L
            powerSave -> 2500L
            else -> 1250L
        }
    }


    private fun startTicking() {
        val notif = buildNotification()
        startForeground(NOTIF_ID, notif)
        scope.launch {
            while (isActive) {
                val tuple = computeTuple()
                val changed = tuple.a != lastA || tuple.b != lastB || tuple.c != lastC || tuple.d != lastD || tuple.e != lastE
                if (changed) {
                    lastA = tuple.a
                    lastB = tuple.b
                    lastC = tuple.c
                    lastD = tuple.d
                    lastE = tuple.e
                    val updated = buildNotification(tuple)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, updated)
                }
                delay(currentInterval())
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    private fun computeTuple(): Quintuple<String, String, Int, Int, Boolean> {
        return when (ConnectedWorkout.GoalType) {
            "Time" -> {
                val goalMs = max(ConnectedWorkout.GoalTime.value, 0L)
                val currMs = max(ConnectedWorkout.CurrentTime.value, 0L)
                val goalSec = (goalMs / 1000L).coerceAtLeast(1L)
                val currSec = (currMs / 1000L).coerceIn(0L, goalSec)
                val ratio = (currSec.toDouble() / goalSec.toDouble()).coerceIn(0.0, 1.0)
                val pm = 1000
                val p = (ratio * pm).toInt()
                val txt = "${formatHMS(goalMs - currMs)} remaining"
                val sub = if (goalMs > 0L) "${formatHMS(currMs)}/${formatHMS(goalMs)} elapsed" else "Counting"
                Quintuple(txt, sub, p, pm, true)
            }
            "Reps" -> {
                val goalSets = max(ConnectedWorkout.GoalSets.intValue, 0)
                val goalReps = max(ConnectedWorkout.GoalReps.intValue, 0)
                val currSets = ConnectedWorkout.CurrentSets.intValue
                val currReps = ConnectedWorkout.CurrentReps.intValue
                val safeSetGoal = goalSets.coerceAtLeast(1)
                val safeRepGoal = goalReps.coerceAtLeast(1)
                val setRatio = (currSets.toDouble() / safeSetGoal.toDouble()).coerceIn(0.0, 1.0)
                val repRatio = (currReps.toDouble() / safeRepGoal.toDouble()).coerceIn(0.0, 1.0)
                val ratio = ((setRatio + repRatio) / 2.0).coerceIn(0.0, 1.0)
                val pm = 1000
                val p = (ratio * pm).toInt()
                val setsText = "$currSets/${if (goalSets > 0) goalSets else "?"}"
                val repsText = if (goalReps > 0) "$currReps/$goalReps" else "$currReps"
                val txt = "Set $setsText • Reps $repsText"
                val sub = "Weight ${ConnectedWorkout.CurrentWeight.value}"
                Quintuple(txt, sub, p, pm, true)
            }
            "Distance" -> {
                val curr = ConnectedWorkout.currentDistance.value
                val goal = ConnectedWorkout.GoalDistance.value
                val safeGoal = if (goal > 0.0) goal else 0.1
                val ratio = (curr / safeGoal).coerceIn(0.0, 1.0)
                val pm = 1000
                val p = (ratio * pm).toInt()
                val txt = if (goal > 0.0) String.format("%.2f / %.2f km", curr, goal) else String.format("%.2f km", curr)
                val sub = "Tracking steps…"
                Quintuple(txt, sub, p, pm, goal > 0.0)
            }
            else -> Quintuple("In progress", "", 0, 1000, false)
        }
    }

    private fun buildNotification(tupleOverride: Quintuple<String, String, Int, Int, Boolean>? = null): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, NotificationDispatcherActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val title = when (ConnectedWorkout.GoalType) {
            "Time" -> "Workout • Timer"
            "Reps" -> "Workout • Sets & Reps"
            "Distance" -> "Workout • Distance"
            else -> "Workout"
        }
        val tuple = tupleOverride ?: computeTuple()
        val text = tuple.a
        val sub = tuple.b
        val progress = tuple.c
        val progressMax = tuple.d
        val showProgress = tuple.e

//        if (Build.VERSION.SDK_INT >= 36 && showProgress && progressMax > 0) {
//            val clamped = progress.coerceIn(0, progressMax)
//            val frac = (clamped.toFloat() / progressMax.toFloat()).coerceIn(0f, 1f)
//            val style = Notification.ProgressStyle().apply {
//                setProgress(progressMax)
//                setProgressPoints(listOf(Notification.ProgressStyle.Point(frac.toInt()).setColor(android.graphics.Color.RED)))
//            }
//            val builder = Notification.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_launcher_mono)
//                .setContentTitle(title)
//                .setContentText(text)
//                .setStyle(style)
//                .setOngoing(true)
//                .setOnlyAlertOnce(true)
//                .setColor(accentColorInt)
//                .setContentIntent(openAppPendingIntent)
//                .setExtras(Bundle().apply { putBoolean("android.extra.REQUEST_PROMOTED_ONGOING", true) })
//                .addAction(Action.Builder(null, "Stop", stopPI).build())
//                .setProgress(progressMax, clamped, false)
//            return builder.build().apply { flags = flags or Notification.FLAG_ONGOING_EVENT }
//        } else {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$sub"))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setColor(accentColorInt)
                .setContentIntent(openAppPendingIntent)
                .setExtras(Bundle().apply { putBoolean("android.extra.REQUEST_PROMOTED_ONGOING", true) })
                .addAction(0, "Stop", stopPI)
            if (showProgress && progressMax > 0) {
                val clamped = progress.coerceIn(0, progressMax)
                builder.setProgress(progressMax, clamped, false)
            }
            return builder.build()

    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Shows live workout progress"
                enableVibration(false)
                enableLights(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun formatHMS(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }
}
