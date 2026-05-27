package com.manoj.backgroundvideorecorder.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.manoj.backgroundvideorecorder.features.recording.presentation.RecordingScreen
import com.manoj.backgroundvideorecorder.features.recording.presentation.RecordingViewModel
import com.manoj.backgroundvideorecorder.features.schedules.presentation.SchedulesScreen
import com.manoj.backgroundvideorecorder.features.schedules.presentation.SchedulesViewModel
import com.manoj.backgroundvideorecorder.features.settings.presentation.SettingsScreen
import com.manoj.backgroundvideorecorder.features.settings.presentation.SettingsViewModel
import com.manoj.backgroundvideorecorder.features.storage.presentation.StorageScreen
import com.manoj.backgroundvideorecorder.features.storage.presentation.StorageViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    recordingViewModel: RecordingViewModel,
    schedulesViewModel: SchedulesViewModel,
    storageViewModel: StorageViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Recording.route,
        modifier = modifier
    ) {
        composable(Screen.Recording.route) {
            RecordingScreen(viewModel = recordingViewModel)
        }
        composable(Screen.Schedules.route) {
            SchedulesScreen(viewModel = schedulesViewModel)
        }
        composable(Screen.Storage.route) {
            StorageScreen(viewModel = storageViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
