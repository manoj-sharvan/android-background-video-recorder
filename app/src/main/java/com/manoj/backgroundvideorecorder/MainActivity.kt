package com.manoj.backgroundvideorecorder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
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
import com.manoj.backgroundvideorecorder.features.security.presentation.SecurityViewModel
import com.manoj.backgroundvideorecorder.features.security.presentation.LockScreen
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import com.manoj.backgroundvideorecorder.features.security.domain.BiometricAuthManager
import com.manoj.backgroundvideorecorder.features.security.domain.AuditLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var securityRepository: SecurityRepository

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var auditLogger: AuditLogger

    @Inject
    lateinit var batteryHelper: com.manoj.backgroundvideorecorder.core.common.BatteryOptimizationHelper

    private var isLocked by mutableStateOf(false)
    private var lastBackgroundTimeMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock on first startup if lock is enabled and PIN is set
        if (securityRepository.isAppLockEnabled() && !securityRepository.getPinHash().isNullOrEmpty()) {
            isLocked = true
        }

        setContent {
            BackgroundVideoRecorderTheme {
                if (isLocked) {
                    LockScreen(
                        onUnlockSuccess = { isLocked = false },
                        securityRepository = securityRepository,
                        biometricAuthManager = biometricAuthManager,
                        auditLogger = auditLogger,
                        activity = this
                    )
                } else {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var showOnboarding by androidx.compose.runtime.saveable.rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        )
                    }

                    if (showOnboarding) {
                        com.manoj.backgroundvideorecorder.features.settings.presentation.OnboardingScreen(
                            onDismiss = { showOnboarding = false },
                            batteryHelper = batteryHelper
                        )
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        val screens = listOf(
                            Screen.Recording,
                            Screen.Schedules,
                            Screen.Storage,
                            Screen.Security,
                            Screen.Settings
                        )

                        val recordingViewModel: RecordingViewModel = hiltViewModel()
                        val schedulesViewModel: SchedulesViewModel = hiltViewModel()
                        val storageViewModel: StorageViewModel = hiltViewModel()
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        val securityViewModel: SecurityViewModel = hiltViewModel()

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
                                securityViewModel = securityViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimeMillis = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (securityRepository.isAppLockEnabled() && !securityRepository.getPinHash().isNullOrEmpty()) {
            val timeoutMillis = securityRepository.getSessionTimeoutMinutes() * 60 * 1000L
            val elapsed = System.currentTimeMillis() - lastBackgroundTimeMillis
            if (lastBackgroundTimeMillis == 0L || elapsed >= timeoutMillis) {
                isLocked = true
            }
        }
    }
}
