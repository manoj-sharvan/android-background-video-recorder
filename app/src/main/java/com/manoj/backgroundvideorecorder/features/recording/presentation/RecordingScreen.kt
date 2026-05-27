package com.manoj.backgroundvideorecorder.features.recording.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.manoj.backgroundvideorecorder.background.service.BackgroundRecordingService
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkGreen
import com.manoj.backgroundvideorecorder.core.designsystem.theme.RedDot
import com.manoj.backgroundvideorecorder.core.designsystem.theme.SteelGray

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermission(isGranted)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateAudioPermission(isGranted)
    }

    // Verify permissions on screen launch
    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updateCameraPermission(hasCamera)
        viewModel.updateAudioPermission(hasAudio)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Video Recorder",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isRecording) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (state.isRecording) "Recording Active" else "System Ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.isRecording) "Duration: ${formatDuration(state.recordingDurationSeconds)}" else "Stealth Mode Ready",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (state.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(alphaAnim)
                            .clip(CircleShape)
                            .background(RedDot)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DarkGreen)
                    )
                }
            }
        }

        // Camera Preview Placeholder Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state.isStealthMode || state.isHiddenPreview) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Preview Hidden",
                            tint = SteelGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.isStealthMode) "Stealth Mode Active\nPreview is Hidden" else "Hidden Preview Mode Active\nNo Live Feed Shown",
                            color = SteelGray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Camera Preview Area\n[CameraX Bound to Service]",
                        color = SteelGray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Quick Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Recorder Configurations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Camera Selector Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Source",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.cameraFacing == 0) "Back Camera active" else "Front Camera active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.switchCamera() },
                        enabled = !state.isRecording,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(text = if (state.cameraFacing == 0) "USE FRONT" else "USE BACK", fontSize = 12.sp)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Stealth Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stealth Recording Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hides UI overlays and preview during capture.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isStealthMode,
                        onCheckedChange = { viewModel.toggleStealthMode() }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Battery Optimizations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Disable to prevent Android from closing the recording service in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(text = "Configure", fontSize = 12.sp)
                    }
                }
            }
        }

        // Permissions Status List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "System Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PermissionStatusItem(
                    title = "Camera Permission",
                    description = "Required to capture background video streams.",
                    granted = state.cameraPermissionGranted,
                    onRequest = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionStatusItem(
                    title = "Microphone Permission",
                    description = "Required to record high fidelity audio sync.",
                    granted = state.audioPermissionGranted,
                    onRequest = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Trigger Button
        Button(
            onClick = {
                if (state.cameraPermissionGranted && state.audioPermissionGranted) {
                    val intent = Intent(context, BackgroundRecordingService::class.java).apply {
                        if (state.isRecording) {
                            action = BackgroundRecordingService.ACTION_STOP_RECORDING
                        } else {
                            action = BackgroundRecordingService.ACTION_START_RECORDING
                            putExtra(BackgroundRecordingService.EXTRA_CAMERA_FACING, state.cameraFacing)
                            putExtra(BackgroundRecordingService.EXTRA_STEALTH_MODE, state.isStealthMode)
                            putExtra(BackgroundRecordingService.EXTRA_RESOLUTION, "1080p")
                            putExtra(BackgroundRecordingService.EXTRA_ENABLE_AUDIO, true)
                        }
                    }
                    if (state.isRecording) {
                        context.startService(intent)
                    } else {
                        ContextCompat.startForegroundService(context, intent)
                    }
                } else {
                    if (!state.cameraPermissionGranted) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    if (!state.audioPermissionGranted) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Trigger Recording"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isRecording) "STOP BACKGROUND RECORDING" else "START BACKGROUND RECORDING",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PermissionStatusItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Granted", color = DarkGreen, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Request Permission",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Grant", fontSize = 12.sp)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
