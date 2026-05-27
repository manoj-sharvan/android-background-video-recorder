package com.manoj.backgroundvideorecorder.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var showQaDashboard by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Camera Options Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Video Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Video Resolution
                Text(
                    text = "Video Resolution",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val resolutions = listOf("480p", "720p", "1080p")
                    resolutions.forEach { res ->
                        val isSelected = state.videoResolution == res
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateResolution(res) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Max Recording Duration Slider
                Text(
                    text = "Max Recording Duration: ${state.maxRecordingDurationMinutes} min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = state.maxRecordingDurationMinutes.toFloat(),
                    onValueChange = { viewModel.updateDuration(it.toInt()) },
                    valueRange = 1f..60f,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Stealth & Notification Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Stealth & Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Run in Stealth Mode Default
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stealth Mode by Default",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Start recording in stealth background mode immediately on app triggers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.runInStealthModeByDefault,
                        onCheckedChange = { viewModel.updateStealthMode(it) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Show persistent notification icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Status Notification",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keeps a status notification when background capture processes are running (Highly Recommended).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.showNotificationIcon,
                        onCheckedChange = { viewModel.updateNotificationIcon(it) }
                    )
                }
            }
        }

        // Advanced Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Advanced Capture Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Split Videos Interval Slider
                Text(
                    text = "Segment Duration: " + if (state.splitVideosIntervalMinutes == 0) "Disabled" else "${state.splitVideosIntervalMinutes} min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = state.splitVideosIntervalMinutes.toFloat(),
                    onValueChange = { viewModel.updateSplitInterval(it.toInt()) },
                    valueRange = 0f..30f,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Segment Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Split recordings automatically into smaller parts to prevent data loss or exceeding file sizes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Storage Retention Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Storage & Retention Policies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Quota Limit Slider
                val maxStorageMbText = if (state.maxStorageMb == 0) "Disabled" else if (state.maxStorageMb >= 1024) "${state.maxStorageMb / 1024} GB" else "${state.maxStorageMb} MB"
                Text(
                    text = "Storage Quota: $maxStorageMbText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = when (state.maxStorageMb) {
                        0 -> 0f
                        500 -> 1f
                        1024 -> 2f
                        2048 -> 3f
                        5120 -> 4f
                        10240 -> 5f
                        else -> 2f
                    },
                    onValueChange = { floatVal ->
                        val mb = when (floatVal.toInt()) {
                            0 -> 0
                            1 -> 500
                            2 -> 1024
                            3 -> 2048
                            4 -> 5120
                            5 -> 10240
                            else -> 1024
                        }
                        viewModel.updateMaxStorageMb(mb)
                    },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 2. Count Limit Slider
                val maxFileCountText = if (state.maxFileCount == 0) "Disabled" else "${state.maxFileCount} files"
                Text(
                    text = "Maximum Video Files: $maxFileCountText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = when (state.maxFileCount) {
                        0 -> 0f
                        10 -> 1f
                        25 -> 2f
                        50 -> 3f
                        100 -> 4f
                        200 -> 5f
                        else -> 3f
                    },
                    onValueChange = { floatVal ->
                        val count = when (floatVal.toInt()) {
                            0 -> 0
                            1 -> 10
                            2 -> 25
                            3 -> 50
                            4 -> 100
                            5 -> 200
                            else -> 50
                        }
                        viewModel.updateMaxFileCount(count)
                    },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 3. Auto Delete Expiration Policy Slider
                val autoDeleteText = if (state.autoDeleteDays == 0) "Keep Forever" else if (state.autoDeleteDays == 1) "1 Day" else "${state.autoDeleteDays} Days"
                Text(
                    text = "Auto-Delete Expiration: $autoDeleteText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = when (state.autoDeleteDays) {
                        0 -> 0f
                        1 -> 1f
                        7 -> 2f
                        30 -> 3f
                        else -> 0f
                    },
                    onValueChange = { floatVal ->
                        val days = when (floatVal.toInt()) {
                            0 -> 0
                            1 -> 1
                            2 -> 7
                            3 -> 30
                            else -> 0
                        }
                        viewModel.updateAutoDeleteDays(days)
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 4. Minimum Reserved Space
                Text(
                    text = "Emergency Reserved Buffer: ${state.minimumReservedSpaceMb} MB",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = when (state.minimumReservedSpaceMb) {
                        100 -> 0f
                        250 -> 1f
                        500 -> 2f
                        1000 -> 3f
                        else -> 1f
                    },
                    onValueChange = { floatVal ->
                        val mb = when (floatVal.toInt()) {
                            0 -> 100
                            1 -> 250
                            2 -> 500
                            3 -> 1000
                            else -> 250
                        }
                        viewModel.updateMinimumReservedSpaceMb(mb)
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        val context = LocalContext.current

        // Battery & OEM Reliability Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = "Battery Optimization",
                        tint = if (state.isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Battery & OEM Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Device Manufacturer: ${state.deviceManufacturer.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Android aggressively terminates background services to save battery. To ensure scheduled recordings or long background video capture works reliably, you must whitelist BVR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(viewModel.requestBatteryWhitelistIntent())
                        } catch (e: Exception) {
                            // fallback
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isBatteryOptimizationIgnored) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = if (state.isBatteryOptimizationIgnored) "Battery Whitelisted" else "Configure Whitelist")
                }

                if (state.hasOemBatterySettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = viewModel.requestOemBatterySettingsIntent()
                            if (intent != null) {
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Configure OEM Auto-Start Settings",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Diagnostics & System Health Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diagnostics & System Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Memory usage:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = state.memoryUsageText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Battery Status:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = state.batteryLevelText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Storage Available:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = state.storageAvailableText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.exportDiagnosticLogs { uri ->
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostics Zip"))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isExporting
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export logs",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (state.isExporting) "Generating zip..." else "Export Diagnostic Report")
                }
                val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showQaDashboard = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "QA Dashboard",
                            tint = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Open QA Stress-Testing Dashboard", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        if (showQaDashboard) {
            QaDashboardDialog(
                onDismiss = { showQaDashboard = false },
                auditLogDao = viewModel.auditLogDao
            )
        }
    }
}

// Custom alignment box to wrap resolution strings
@Composable
fun Box(
    modifier: Modifier,
    contentAlignment: Alignment,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

private fun Modifier.size(size: androidx.compose.ui.unit.Dp) = this.width(size).height(size)
