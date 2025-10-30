package com.forgecompose.workouttracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: HealthConnectViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HealthConnectViewModel(HealthConnectManager(context.applicationContext))
            }
        }
    )

    val availability by viewModel.availability.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { result ->
            viewModel.checkAvailabilityAndPermissions()
        }
    )

    LaunchedEffect(Unit) {
        viewModel.checkAvailabilityAndPermissions()
    }

    val staticGradientBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2A0F0F), Color(0xFF3D0000), Color(0xFF060202)),
            radius = 1200f,
            center = Offset(0.5f, 0.4f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(Color(0xFF060202))
                    drawRect(staticGradientBrush)
                }
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Health Connect", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    SettingsSectionCard(title = "Sync Workouts") {
                        HealthConnectStatusContent(
                            availability = availability,
                            permissionsGranted = permissionsGranted,
                            onInstallClick = { installHealthConnect(context) },
                            onGrantPermissionsClick = {
                                permissionLauncher.launch(
                                    HealthConnectManager.REQUIRED_PERMISSIONS
                                )
                            },
                            onDisconnectClick = {
                                viewModel.revokePermissions()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthConnectStatusContent(
    availability: HealthConnectAvailability,
    permissionsGranted: Boolean,
    onInstallClick: () -> Unit,
    onGrantPermissionsClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    val statusText: String
    val buttonText: String
    val onButtonClick: () -> Unit
    val isEnabled: Boolean

    when (availability) {
        HealthConnectAvailability.NOT_SUPPORTED -> {
            statusText = "Not supported on this device."
            buttonText = "Not Available"
            onButtonClick = {}
            isEnabled = false
        }
        HealthConnectAvailability.NOT_INSTALLED -> {
            statusText = "Health Connect app is not installed."
            buttonText = "Install App"
            onButtonClick = onInstallClick
            isEnabled = true
        }
        HealthConnectAvailability.UPDATE_REQUIRED -> {
            statusText = "Health Connect app requires an update."
            buttonText = "Update App"
            onButtonClick = onInstallClick
            isEnabled = true
        }
        HealthConnectAvailability.INSTALLED -> {
            if (!permissionsGranted) {
                statusText = "Permissions are required to sync data."
                buttonText = "Grant Permissions"
                onButtonClick = onGrantPermissionsClick
                isEnabled = true
            } else {
                statusText = "Your workouts are syncing automatically."
                buttonText = "Disconnect"
                onButtonClick = onDisconnectClick
                isEnabled = true
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Health Connect Icon",
            tint = if (permissionsGranted) Color(0xFFFF3B30) else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = statusText,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = onButtonClick,
            enabled = isEnabled,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.medium
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .shadow(
                    elevation = 4.dp,
                    shape = MaterialTheme.shapes.medium,
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}
private fun installHealthConnect(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        setPackage("com.android.vending")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
        )
        context.startActivity(webIntent)
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 24.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val cornerRpx = cornerRadius.toPx()
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF180909).copy(alpha = 0.9f),
                        Color(0xFF100404).copy(alpha = 0.95f)
                    ),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.width * 1.5f
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5555).copy(alpha = 0.2f),
                        Color(0xFF8B0000).copy(alpha = 0.1f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = bgBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRpx)
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color(0xFFFF3535).copy(alpha = 0.3f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
