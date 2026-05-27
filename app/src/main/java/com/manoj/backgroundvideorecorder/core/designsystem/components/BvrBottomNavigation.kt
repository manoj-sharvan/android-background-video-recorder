package com.manoj.backgroundvideorecorder.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkBackground

data class BvrNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val defaultBvrNavItems = listOf(
    BvrNavItem("Record", Icons.Default.FiberManualRecord, "record"),
    BvrNavItem("Schedule", Icons.Default.Schedule, "schedule"),
    BvrNavItem("Storage", Icons.Default.Folder, "storage"),
    BvrNavItem("Security", Icons.Default.Security, "security"),
    BvrNavItem("Settings", Icons.Default.Settings, "settings")
)

@Composable
fun BvrBottomNavigation(
    items: List<BvrNavItem> = defaultBvrNavItems,
    selectedRoute: String,
    onItemClick: (String) -> Unit
) {
    NavigationBar(
        containerColor = DarkBackground,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selectedRoute == item.route,
                onClick = { onItemClick(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
