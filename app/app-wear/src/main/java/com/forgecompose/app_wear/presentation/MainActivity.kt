// app-wear/src/main/java/com/example/app_wear/MainActivity.kt
package com.forgecompose.app_wear

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.app_wear.presentation.CrimsonWearTheme
import com.forgecompose.app_wear.presentation.HrRepository
import com.forgecompose.app_wear.presentation.HrViewModel


import com.forgecompose.app_wear.ui.HrScreenPro

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearHome() }
    }
}


/** Returns true if at least one Wear node is currently connected */

/** Compose entry using the crimson UI + HrViewModel */
@SuppressLint("ContextCastToActivity")
@Composable
fun WearHome() {
    val activity = LocalContext.current as ComponentActivity

    // --- Permissions ----
    val requiredPerms = remember {
        buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.BODY_SENSORS_BACKGROUND)
        }.toTypedArray()
    }
    var hasPerms by remember { mutableStateOf(hasAll(activity, requiredPerms)) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPerms = requiredPerms.all { result[it] == true } || hasAll(activity, requiredPerms)
    }

    // --- ViewModel wired to repository ---
    val vm = viewModel<HrViewModel>(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val ctx = activity.applicationContext
                return HrViewModel(HrRepository(ctx), ctx) as T
            }
        }
    )

    val bpm by vm.bpm.collectAsState()
    val inExercise by vm.inExercise.collectAsState()




    val connectedToPhone = true
    val sensorOk = bpm != null

    CrimsonWearTheme {
        if (!hasPerms) {

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { permLauncher.launch(requiredPerms) }) {
                    Text("Grant heart-rate permission")
                }
            }
        } else {

            HrScreenPro(
                bpm = bpm,
                inExercise = inExercise,
                avgBpm = null,
                minBpm = null,
                maxBpm = null,
                sensorAvailable = sensorOk,
                connectedToPhone = connectedToPhone,
                onStart = { vm.start() },
                onStop = { vm.stop() }
            )
        }
    }
}

private fun hasAll(ctx: ComponentActivity, perms: Array<String>): Boolean =
    perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
