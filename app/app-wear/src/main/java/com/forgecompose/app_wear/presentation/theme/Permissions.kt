package com.forgecompose.app_wear.presentation.theme



import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val READ_HEART_RATE_PERMISSION = "android.permission.health.READ_HEART_RATE"

fun requiredSensorPermissions(): List<String> = listOf(
    if (Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL) {
        READ_HEART_RATE_PERMISSION
    } else {
        Manifest.permission.BODY_SENSORS
    }
)

fun hasHeartRatePermission(context: Context): Boolean {
    val permission = requiredSensorPermissions().single()
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun hasHeartRatePermission(grants: Map<String, Boolean>): Boolean {
    return grants[requiredSensorPermissions().single()] == true
}

@SuppressLint("ContextCastToActivity")
@Composable
fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val activity = LocalContext.current as ComponentActivity
    var granted by remember { mutableStateOf(false) }
    val requiredPerms = remember { requiredSensorPermissions() }

    val launcher = remember {
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { res ->
            granted = hasHeartRatePermission(res)
            onResult(granted)
        }
    }
    return { launcher.launch(requiredPerms.toTypedArray()) }
}

private const val ANDROID_16_API_LEVEL = 36
