package com.manoj.backgroundvideorecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manoj.backgroundvideorecorder.core.designsystem.theme.BackgroundVideoRecorderTheme
import com.manoj.backgroundvideorecorder.core.navigation.AppNavGraph
import com.manoj.backgroundvideorecorder.core.navigation.Screen
import com.manoj.backgroundvideorecorder.features.recording.presentation.RecordingViewModel
import com.manoj.backgroundvideorecorder.features.schedules.presentation.SchedulesViewModel
import com.manoj.backgroundvideorecorder.features.settings.presentation.SettingsViewModel
import com.manoj.backgroundvideorecorder.features.storage.presentation.StorageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackgroundVideoRecorderTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val screens = listOf(
                    Screen.Recording,
                    Screen.Schedules,
                    Screen.Storage,
                    Screen.Settings
                )

                val recordingViewModel: RecordingViewModel = hiltViewModel()
                val schedulesViewModel: SchedulesViewModel = hiltViewModel()
                val storageViewModel: StorageViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            screens.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(text = screen.title)
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        recordingViewModel = recordingViewModel,
                        schedulesViewModel = schedulesViewModel,
                        storageViewModel = storageViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
