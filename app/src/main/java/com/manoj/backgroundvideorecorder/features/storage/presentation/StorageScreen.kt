package com.manoj.backgroundvideorecorder.features.storage.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.core.designsystem.theme.SteelGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent

@Composable
fun StorageScreen(
    viewModel: StorageViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Storage Management",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Storage metrics summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Application Storage Used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBytes(state.totalSize),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar: Application Space vs Device Free Space
                val appUsed = state.totalSize.toFloat()
                val freeSpace = state.deviceFreeSpaceBytes.toFloat()
                val totalRecordedAndFree = appUsed + freeSpace
                val progress = if (totalRecordedAndFree > 0) appUsed / totalRecordedAndFree else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "App: ${formatBytes(state.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Free: ${formatBytes(state.deviceFreeSpaceBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Storage Analytics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.records.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val avgSize = if (state.records.isNotEmpty()) state.totalSize / state.records.size else 0L
                        Text(
                            text = formatBytes(avgSize),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Avg File Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Search & Filter controls
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search by name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = state.filterProtected,
                onClick = { viewModel.setFilterProtected(!state.filterProtected) },
                label = { Text("Protected Only") },
                leadingIcon = if (state.filterProtected) {
                    { Icon(Icons.Default.Lock, contentDescription = "Protected", modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            var sortExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val sortLabel = when (state.sortBy) {
                "date_desc" -> "Newest First"
                "date_asc" -> "Oldest First"
                "size_desc" -> "Largest First"
                "size_asc" -> "Smallest First"
                "duration_desc" -> "Longest First"
                else -> "Newest First"
            }

            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                OutlinedButton(
                    onClick = { sortExpanded = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Sort: $sortLabel", fontSize = 12.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Arrow Drop Down")
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Newest First") },
                        onClick = {
                            viewModel.setSortBy("date_desc")
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest First") },
                        onClick = {
                            viewModel.setSortBy("date_asc")
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Largest First") },
                        onClick = {
                            viewModel.setSortBy("size_desc")
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Smallest First") },
                        onClick = {
                            viewModel.setSortBy("size_asc")
                            sortExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Longest First") },
                        onClick = {
                            viewModel.setSortBy("duration_desc")
                            sortExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons row
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val demoId = (100..999).random()
                        viewModel.addDemoRecord(
                            filePath = context.filesDir.absolutePath + "/recordings/BVR_REC_$demoId.mp4",
                            size = (5 * 1024 * 1024..50 * 1024 * 1024).random().toLong(),
                            duration = (15000..95000).random().toLong()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Demo Log")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Mock Entry", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.runManualOrphanCleanup() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isCleaning
                ) {
                    if (state.isCleaning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Clean Orphans")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (state.isCleaning) "Cleaning..." else "Clean Orphans", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.clearCache() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isCleaning
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear Cache")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Trigger Retention Cleanup Policies", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "No Records",
                    tint = SteelGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No saved videos log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recorded video file history will show up here. You can click 'Mock Entry' above to test database binding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SteelGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.records) { record ->
                    VideoRecordItemCard(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record) },
                        onToggleProtection = { viewModel.toggleProtection(record) },
                        onShare = {
                            val intent = viewModel.getShareIntent(record)
                            if (intent != null) {
                                val chooser = Intent.createChooser(intent, "Share Video Recording")
                                context.startActivity(chooser)
                            }
                        }
                    )
                }
            }
        }
    }
}

// Extension size helper
private fun Modifier.size(size: androidx.compose.ui.unit.Dp) = this.width(size).height(size)

@Composable
fun VideoRecordItemCard(
    record: VideoRecordEntity,
    onDelete: () -> Unit,
    onToggleProtection: () -> Unit,
    onShare: () -> Unit
) {
    val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(record.timestamp))
    val formattedDuration = String.format(
        "%02d:%02d",
        (record.durationMillis / 1000) / 60,
        (record.durationMillis / 1000) % 60
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Duration: $formattedDuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatBytes(record.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleProtection) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Toggle Protection",
                        tint = if (record.isProtected) MaterialTheme.colorScheme.primary else SteelGray.copy(alpha = 0.3f)
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Record",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Record",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val k = 1024.0
    val sizes = arrayOf("Bytes", "KB", "MB", "GB")
    val i = kotlin.math.floor(kotlin.math.log(bytes.toDouble(), k)).toInt()
    return String.format(Locale.getDefault(), "%.2f %s", bytes / java.lang.Math.pow(k, i.toDouble()), sizes[i])
}

