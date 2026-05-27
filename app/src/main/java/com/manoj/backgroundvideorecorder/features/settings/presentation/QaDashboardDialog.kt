package com.manoj.backgroundvideorecorder.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val leakedReferences = mutableListOf<Any>()

@Composable
fun QaDashboardDialog(
    onDismiss: () -> Unit,
    auditLogDao: AuditLogDao,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var logMessage by remember { mutableStateOf("Ready to simulate...") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "QA / Stress-Testing Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Use these debug tools to simulate system failures and verify watchdog, leak detection, and log integrity layers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Status: $logMessage",
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        logMessage = "Simulating ANR (blocking main thread for 6s)..."
                        AppLogger.w("QA Dashboard: Simulating ANR by blocking main thread.")
                        Thread.sleep(6000)
                        logMessage = "ANR test complete."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger Test ANR (Stall 6s)")
                }

                Button(
                    onClick = {
                        logMessage = "Simulated memory leak (retaining objects)..."
                        AppLogger.w("QA Dashboard: Leaking objects to static list.")
                        repeat(50) {
                            leakedReferences.add(ByteArray(1024 * 1024))
                        }
                        logMessage = "Leaked references stored. Watch LeakCanary!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger Test Memory Leak")
                }

                Button(
                    onClick = {
                        logMessage = "Writing integrity failure warning to DB..."
                        scope.launch(Dispatchers.IO) {
                            try {
                                auditLogDao.insert(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        action = "Database Warning (Simulated)",
                                        detail = "QA simulated a DB/KeyStore failure. Please verify diagnostics screen."
                                    )
                                )
                                logMessage = "Simulated failure warning logged to DB."
                            } catch (e: Exception) {
                                AppLogger.e(e, "Simulating DB write failed")
                                logMessage = "Simulation DB insert failed."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate DB/KeyStore Failure")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
