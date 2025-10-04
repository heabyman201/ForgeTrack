package com.forgecompose.workouttracker

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PerfTuning {
    data class Snapshot(
        val mediaPerfClass: Int,
        val isLowRam: Boolean,
        val memClassMb: Int,
        val cpuCores: Int
    )

    fun snapshot(context: Context): Snapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mediaClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.VERSION.MEDIA_PERFORMANCE_CLASS
        } else 0

        return Snapshot(
            mediaPerfClass = mediaClass,
            isLowRam = am.isLowRamDevice,
            memClassMb = am.memoryClass,
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }

    fun defaultIntensityDp(s: Snapshot): Dp = when {
        s.mediaPerfClass >= 13 -> 22.dp
        s.mediaPerfClass >= 12 -> 18.dp
        s.isLowRam || s.memClassMb < 256 -> 12.dp
        s.cpuCores <= 4 -> 12.dp
        else -> 16.dp
    }

    fun defaultIntensityScalar(s: Snapshot): Float = when {
        s.mediaPerfClass >= 13 -> 1.15f
        s.mediaPerfClass >= 12 -> 1.0f
        s.isLowRam || s.memClassMb < 256 || s.cpuCores <= 4 -> 0.8f
        else -> 0.9f
    }
}

object blurAnim {
    var length = mutableLongStateOf(700L)
    var intensity = mutableStateOf(14.dp)

    @Volatile private var initialized = false

    fun init(appContext: Context) {
        if (initialized) return
        val snap = PerfTuning.snapshot(appContext)

        intensity.value = PerfTuning.defaultIntensityDp(snap)


        length.longValue = when {
            snap.mediaPerfClass >= 13 -> 750L
            snap.mediaPerfClass >= 12 -> 700L
            snap.isLowRam || snap.memClassMb < 256 || snap.cpuCores <= 4 -> 650L
            else -> 700L
        }

        initialized = true
    }
}
