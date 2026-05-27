package com.manoj.backgroundvideorecorder.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Recording : Screen("recording", "Record", Icons.Default.PlayArrow)
    object Schedules : Screen("schedules", "Schedules", Icons.Default.List)
    object Storage : Screen("storage", "Storage", Icons.Default.Home)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
