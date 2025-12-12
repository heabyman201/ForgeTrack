package com.forgecompose.app_wear.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun HeartRateMonitor(
    weightKg: Double = 48.0,
    age: Int = 16,
    isMale: Boolean = true
) {
    val context = LocalContext.current
    var heartRate by remember { mutableFloatStateOf(0f) }
    var caloriesBurnt by remember { mutableFloatStateOf(0f) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasPermission = it }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    if (hasPermission) {
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.firstOrNull()?.let {
                        heartRate = it
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (hrSensor != null) {
                sensorManager.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1000L)
                if (heartRate > 0) {
                    val calPerMin = if (isMale) {
                        (-55.0969 + (0.6309 * heartRate) + (0.1988 * weightKg) + (0.2017 * age)) / 4.184
                    } else {
                        (-20.4022 + (0.4472 * heartRate) + (0.1263 * weightKg) + (0.074 * age)) / 4.184
                    }
                    caloriesBurnt += (calPerMin / 60).toFloat()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Heart Rate",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
            Text(
                text = "${heartRate.toInt()} BPM",
                style = MaterialTheme.typography.title1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Calories",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
            Text(
                text = String.format("%.1f kcal", caloriesBurnt),
                style = MaterialTheme.typography.title2
            )
        }

        if (!hasPermission) {
            Text(
                text = "Sensors permission needed",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}