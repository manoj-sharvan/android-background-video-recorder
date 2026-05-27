package com.manoj.backgroundvideorecorder.features.recording.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.manoj.backgroundvideorecorder.background.service.BackgroundRecordingService
import com.manoj.backgroundvideorecorder.core.designsystem.components.*
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkBackground

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
    ) { isGranted -> viewModel.updateCameraPermission(isGranted) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> viewModel.updateAudioPermission(isGranted) }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.updateCameraPermission(hasCamera)
        viewModel.updateAudioPermission(hasAudio)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Capture Dashboard")

        // Status Card
        BvrCard(
            variant = if (state.isRecording) CardVariant.DANGER else CardVariant.NORMAL
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (state.isRecording) "Recording Active" else "System Ready",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.isRecording) formatDuration(state.recordingDurationSeconds) else "Ready for stealth capture",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Camera Preview Wrapper
        BvrCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isStealthMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hidden",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stealth Mode Active\nPreview is Hidden",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "[CameraX Bound to Service]",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Controls
        BvrCard {
            Column {
                SectionHeader(title = "Configuration")

                SwitchSettingItem(
                    title = "Front Camera",
                    description = "Use front-facing camera for recording",
                    checked = state.cameraFacing == 0, // 0 = front usually (this depends on CameraSelector mapping, assumed front here for demo)
                    onCheckedChange = { viewModel.switchCamera() }
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                SwitchSettingItem(
                    title = "Stealth Mode",
                    description = "Hides UI overlays during capture",
                    checked = state.isStealthMode,
                    onCheckedChange = { viewModel.toggleStealthMode() }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Record FAB
        RecordFab(
            isRecording = state.isRecording,
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
                    if (!state.cameraPermissionGranted) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    if (!state.audioPermissionGranted) audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
