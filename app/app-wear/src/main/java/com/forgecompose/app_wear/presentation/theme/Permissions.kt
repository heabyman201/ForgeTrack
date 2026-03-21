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

fun requiredSensorPermissions(): List<String> = buildList {
    add(Manifest.permission.BODY_SENSORS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        add(READ_HEART_RATE_PERMISSION)
    }
}

fun hasHeartRatePermission(context: Context): Boolean {
    val bodySensorsGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
    val readHeartRateGranted =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(context, READ_HEART_RATE_PERMISSION) == PackageManager.PERMISSION_GRANTED
    return bodySensorsGranted || readHeartRateGranted
}

fun hasHeartRatePermission(grants: Map<String, Boolean>): Boolean {
    val bodySensorsGranted = grants[Manifest.permission.BODY_SENSORS] == true
    val readHeartRateGranted = grants[READ_HEART_RATE_PERMISSION] == true
    return bodySensorsGranted || readHeartRateGranted
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
