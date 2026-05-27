package com.manoj.backgroundvideorecorder.features.security.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoj.backgroundvideorecorder.core.designsystem.components.*
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var aliasExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.pinSetupStep) {
        if (state.pinSetupStep != PinSetupStep.NONE) {
            showPinDialog = true
        } else {
            showPinDialog = false
            pinText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Privacy & Security")

        // Lock & Access Protection Card
        BvrCard {
            Column {
                SectionHeader(title = "Access Protection")

                SwitchSettingItem(
                    title = "App Lock PIN Protection",
                    description = "Require PIN to open the app.",
                    checked = state.isAppLockEnabled,
                    onCheckedChange = {
                        if (!state.hasPinSet) viewModel.startPinSetup()
                        else viewModel.setAppLockEnabled(it)
                    }
                )

                if (state.hasPinSet) {
                    TextButton(
                        onClick = { viewModel.startPinSetup() },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Change Access PIN")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                SwitchSettingItem(
                    title = "Biometric Authentication",
                    description = "Use fingerprint or face unlock if available.",
                    checked = state.isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                // Session Timeout Slider
                Text(
                    text = "Auto-Lock Timeout: ${if (state.sessionTimeoutMinutes == 0) "Immediate" else "${state.sessionTimeoutMinutes} min"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = state.sessionTimeoutMinutes.toFloat(),
                    onValueChange = { viewModel.setSessionTimeout(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 6,
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Stealth Disguise Card
        BvrCard {
            Column {
                SectionHeader(title = "Stealth & Disguise")

                Text(
                    text = "App Icon & Name Disguise",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Disguise this application on your home launcher screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aliasExpanded = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val aliasLabel = when (state.activeLauncherAlias) {
                                "REAL" -> "Default BVR Icon"
                                "CALCULATOR" -> "Calculator"
                                "NOTES" -> "Notes"
                                "CALENDAR" -> "Calendar"
                                else -> "Default BVR Icon"
                            }
                            Text(text = aliasLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    DropdownMenu(
                        expanded = aliasExpanded,
                        onDismissRequest = { aliasExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default BVR Icon") },
                            onClick = { viewModel.setLauncherAlias("REAL"); aliasExpanded = false; Toast.makeText(context, "Icon switching...", Toast.LENGTH_LONG).show() }
                        )
                        DropdownMenuItem(
                            text = { Text("Calculator Disguise") },
                            onClick = { viewModel.setLauncherAlias("CALCULATOR"); aliasExpanded = false; Toast.makeText(context, "Icon switching...", Toast.LENGTH_LONG).show() }
                        )
                        DropdownMenuItem(
                            text = { Text("Notes Disguise") },
                            onClick = { viewModel.setLauncherAlias("NOTES"); aliasExpanded = false; Toast.makeText(context, "Icon switching...", Toast.LENGTH_LONG).show() }
                        )
                        DropdownMenuItem(
                            text = { Text("Calendar Disguise") },
                            onClick = { viewModel.setLauncherAlias("CALENDAR"); aliasExpanded = false; Toast.makeText(context, "Icon switching...", Toast.LENGTH_LONG).show() }
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                SwitchSettingItem(
                    title = "Sensitive Notifications",
                    description = "Replaces recording status text with generic label 'Service Running' in background.",
                    checked = state.isPrivateNotificationMode,
                    onCheckedChange = { viewModel.setPrivateNotificationMode(it) }
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                SwitchSettingItem(
                    title = "Hidden Preview Mode",
                    description = "Hides standard live camera feedback (black preview box) during application record sessions.",
                    checked = state.isHiddenPreviewMode,
                    onCheckedChange = { viewModel.setHiddenPreviewMode(it) }
                )
            }
        }

        // Encryption Storage Card
        BvrCard {
            Column {
                SectionHeader(title = "Secure Storage")

                SwitchSettingItem(
                    title = "Encrypt New Recordings",
                    description = "Uses AES-256 GCM to secure completed video segments using the hardware-backed KeyStore.",
                    checked = state.isEncryptRecordingsEnabled,
                    onCheckedChange = { viewModel.setEncryptRecordingsEnabled(it) }
                )
            }
        }

        // Audit Logs Card
        BvrCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Access Audit Log")

                    if (state.auditLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (state.auditLogs.isEmpty()) {
                    Text(
                        text = "No audit log entries recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.auditLogs.forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.action.contains("FAIL")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (log.detail.isNotEmpty()) {
                                    Text(
                                        text = log.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Divider(
                                    modifier = Modifier.padding(top = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog for PIN Creation Setup
    if (showPinDialog) {
        val titleText = if (state.pinSetupStep == PinSetupStep.ENTER_NEW_PIN) "Enter New PIN" else "Confirm New PIN"
        val descriptionText = if (state.pinSetupStep == PinSetupStep.ENTER_NEW_PIN) "Choose a secure 4 to 6 digit numeric code." else "Re-enter your numeric PIN to confirm."

        AlertDialog(
            onDismissRequest = { viewModel.cancelPinSetup() },
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 6) {
                                pinText = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        placeholder = { Text("••••") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinText.length in 4..6) {
                            val pinToSubmit = pinText
                            pinText = ""
                            viewModel.handlePinInput(pinToSubmit)
                        } else {
                            Toast.makeText(context, "PIN must be between 4 and 6 digits", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPinSetup() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
