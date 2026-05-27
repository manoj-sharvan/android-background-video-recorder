package com.manoj.backgroundvideorecorder.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Recording : Screen("recording", "Record", Icons.Default.PlayArrow)
    object Schedules : Screen("schedules", "Schedules", Icons.AutoMirrored.Filled.List)
    object Storage : Screen("storage", "Storage", Icons.Default.Home)
    object Security : Screen("security", "Security", Icons.Default.Security)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
