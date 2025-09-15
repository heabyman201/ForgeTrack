package com.forgecompose.app_wear.presentation.theme



import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

val REQUIRED_PERMS = buildList {
    add(Manifest.permission.BODY_SENSORS)
    if (Build.VERSION.SDK_INT >= 33) {
        add(Manifest.permission.BODY_SENSORS_BACKGROUND)
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun rememberPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val activity = LocalContext.current as ComponentActivity
    var granted by remember { mutableStateOf(false) }

    val launcher = remember {
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { res ->
            granted = REQUIRED_PERMS.all { res[it] == true }
            onResult(granted)
        }
    }
    return { launcher.launch(REQUIRED_PERMS.toTypedArray()) }
}
