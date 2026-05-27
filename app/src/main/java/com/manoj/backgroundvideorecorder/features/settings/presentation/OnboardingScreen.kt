package com.manoj.backgroundvideorecorder.features.settings.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.manoj.backgroundvideorecorder.core.common.BatteryOptimizationHelper

@Composable
fun OnboardingScreen(
    onDismiss: () -> Unit,
    batteryHelper: BatteryOptimizationHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamera by remember { mutableStateOf(false) }
    var hasAudio by remember { mutableStateOf(false) }
    var hasNotification by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }
    var hasBatteryOpt by remember { mutableStateOf(false) }

    fun checkAllPermissions() {
        hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        hasOverlay = Settings.canDrawOverlays(context)
        hasBatteryOpt = batteryHelper.isIgnoringBatteryOptimizations()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkAllPermissions()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCamera = granted }
    )

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasAudio = granted }
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotification = granted }
    )

    val isCriticalGranted = hasCamera && hasAudio

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to BVR Platform",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.SpacerTextSpacing())

            Text(
                text = "To enable stealth and autonomous recording features, please authorize the following permissions.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionRow(
                title = "Camera Access (Required)",
                description = "Allows the background service to capture video feed without open windows.",
                isGranted = hasCamera,
                icon = Icons.Default.CameraAlt,
                onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Microphone Access (Required)",
                description = "Used to record high quality audio feed alongside video tracks.",
                isGranted = hasAudio,
                icon = Icons.Default.Mic,
                onRequest = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(12.dp))
                PermissionRow(
                    title = "Notifications Permission",
                    description = "Required to keep the recording service running in the background.",
                    isGranted = hasNotification,
                    icon = Icons.Default.Notifications,
                    onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Draw Over Other Apps",
                description = "Enables launch overlays and hidden/stealth recording previews.",
                isGranted = hasOverlay,
                icon = Icons.Default.Layers,
                onRequest = {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Battery Optimization Bypass",
                description = "Disables background app restrictions to prevent sudden service stops.",
                isGranted = hasBatteryOpt,
                icon = Icons.Default.BatteryAlert,
                onRequest = {
                    try {
                        context.startActivity(batteryHelper.getBatteryOptimizationIntent())
                    } catch (e: Exception) {
                        // ignore/fallback
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { if (isCriticalGranted) onDismiss() },
                enabled = isCriticalGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isCriticalGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera & Microphone permissions must be granted to continue.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Grant",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun Modifier.SpacerTextSpacing(): Modifier = this.height(8.dp)
