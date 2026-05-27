package com.manoj.backgroundvideorecorder.features.storage.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.core.designsystem.components.SectionHeader
import com.manoj.backgroundvideorecorder.core.designsystem.components.StorageAnalyticsCard
import com.manoj.backgroundvideorecorder.core.designsystem.components.VideoListItem
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkBackground
import com.manoj.backgroundvideorecorder.core.designsystem.theme.SteelGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageScreen(
    viewModel: StorageViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Storage Management")

        // Storage Analytics
        val appUsed = state.totalSize.toFloat()
        val freeSpace = state.deviceFreeSpaceBytes.toFloat()
        val totalRecordedAndFree = appUsed + freeSpace
        val progress = if (totalRecordedAndFree > 0) appUsed / totalRecordedAndFree else 0f

        StorageAnalyticsCard(
            usedSpaceText = formatBytes(state.totalSize),
            freeSpaceText = formatBytes(state.deviceFreeSpaceBytes),
            totalVideos = state.records.size,
            progress = progress
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search & Filter controls
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search by name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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

            var sortExpanded by remember { mutableStateOf(false) }
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
                    Text(text = sortLabel, fontSize = 12.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Arrow Drop Down")
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Newest First") }, onClick = { viewModel.setSortBy("date_desc"); sortExpanded = false })
                    DropdownMenuItem(text = { Text("Oldest First") }, onClick = { viewModel.setSortBy("date_asc"); sortExpanded = false })
                    DropdownMenuItem(text = { Text("Largest First") }, onClick = { viewModel.setSortBy("size_desc"); sortExpanded = false })
                    DropdownMenuItem(text = { Text("Smallest First") }, onClick = { viewModel.setSortBy("size_asc"); sortExpanded = false })
                    DropdownMenuItem(text = { Text("Longest First") }, onClick = { viewModel.setSortBy("duration_desc"); sortExpanded = false })
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
            }
        } else {
            val timeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.records) { record ->
                    val formattedTime = timeFormat.format(Date(record.timestamp))
                    val formattedDuration = String.format(
                        "%02d:%02d",
                        (record.durationMillis / 1000) / 60,
                        (record.durationMillis / 1000) % 60
                    )
                    
                    VideoListItem(
                        fileName = record.fileName,
                        durationText = formattedDuration,
                        sizeText = formatBytes(record.fileSize),
                        dateText = formattedTime,
                        isEncrypted = record.isEncrypted,
                        onClick = { /* View Video */ },
                        onDelete = { viewModel.deleteRecord(record) },
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val k = 1024.0
    val sizes = arrayOf("Bytes", "KB", "MB", "GB")
    val i = kotlin.math.floor(kotlin.math.log(bytes.toDouble(), k)).toInt()
    return String.format(Locale.getDefault(), "%.2f %s", bytes / java.lang.Math.pow(k, i.toDouble()), sizes[i])
}

