package com.manvoy.pointtracker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CloudSyncDialog(
    currentConfig: CloudSyncConfig,
    syncState: SyncState,
    onDismiss: () -> Unit,
    onSaveConfig: (CloudSyncConfig) -> Unit,
    onSyncNow: (CloudSyncConfig) -> Unit,
    onForcePush: (CloudSyncConfig) -> Unit,
    onForcePull: (CloudSyncConfig) -> Unit
) {
    val scope = rememberCoroutineScope()

    var workerUrl by remember { mutableStateOf(currentConfig.workerUrl) }
    var apiToken by remember { mutableStateOf(currentConfig.apiToken) }
    var syncKey by remember { mutableStateOf(currentConfig.syncKey.ifBlank { "pribadi" }) }
    var autoSyncEnabled by remember { mutableStateOf(currentConfig.autoSyncEnabled) }
    var autoSyncInterval by remember { mutableIntStateOf(currentConfig.autoSyncIntervalMinutes) }
    var showToken by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun buildDraftConfig(): CloudSyncConfig {
        return currentConfig.copy(
            workerUrl = workerUrl.trim(),
            apiToken = apiToken.trim(),
            syncKey = syncKey.trim().ifBlank { "pribadi" },
            autoSyncEnabled = autoSyncEnabled,
            autoSyncIntervalMinutes = autoSyncInterval
        )
    }

    val isBusy = isTesting || syncState == SyncState.SYNCING

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sinkronisasi Cloudflare Worker",
                fontWeight = FontWeight.Bold,
                color = MandiriNavy,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val statusBg = when (syncState) {
                    SyncState.SUCCESS -> Color(0xFFEAF9F0)
                    SyncState.ERROR -> Color(0xFFFDECEC)
                    SyncState.SYNCING -> Color(0xFFEAF2FC)
                    SyncState.IDLE -> SoftBg
                }
                val statusColor = when (syncState) {
                    SyncState.SUCCESS -> Color(0xFF1B7A43)
                    SyncState.ERROR -> Color(0xFFC62828)
                    SyncState.SYNCING -> MandiriBlue
                    SyncState.IDLE -> Color(0xFF475569)
                }
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (syncState == SyncState.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MandiriBlue
                            )
                        } else {
                            Icon(
                                imageVector = when (syncState) {
                                    SyncState.SUCCESS -> Icons.Default.CheckCircle
                                    SyncState.ERROR -> Icons.Default.Warning
                                    else -> Icons.Default.CloudOff
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    syncState == SyncState.SYNCING -> "Menyinkronkan data..."
                                    currentConfig.lastSyncMessage.isNotBlank() -> currentConfig.lastSyncMessage
                                    currentConfig.isConfigured -> "Siap disinkronkan"
                                    else -> "Belum terhubung ke Cloudflare Worker"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                            Text(
                                text = "Sync terakhir: ${formatDateTimeLocal(currentConfig.lastSyncAt)}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = workerUrl,
                    onValueChange = {
                        workerUrl = it
                        testResult = null
                    },
                    label = { Text("URL Cloudflare Worker") },
                    placeholder = { Text("https://manvoy-sync.akun.workers.dev") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = {
                        apiToken = it
                        testResult = null
                    },
                    label = { Text("Secret Token Pribadi (SYNC_SECRET)") },
                    placeholder = { Text("Masukkan secret token rahasia") },
                    singleLine = true,
                    visualTransformation = if (showToken) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showToken) "Sembunyikan token" else "Tampilkan token"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = syncKey,
                    onValueChange = { syncKey = it },
                    label = { Text("ID Profil / Ruang Data") },
                    placeholder = { Text("pribadi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (testResult != null) {
                    val (ok, msg) = testResult!!
                    Surface(
                        color = if (ok) Color(0xFFEAF9F0) else Color(0xFFFDECEC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (ok) Color(0xFF1B7A43) else Color(0xFFC62828),
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Sync Otomatis",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Sync saat app dibuka & setiap perubahan data",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MandiriNavy
                        )
                    )
                }

                if (autoSyncEnabled) {
                    Text(
                        text = "Interval Auto-Sync Berkala",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiriNavy
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Saat berubah", 5 to "5 mnt", 15 to "15 mnt", 30 to "30 mnt").forEach { (mins, lbl) ->
                            FilterChip(
                                selected = autoSyncInterval == mins,
                                onClick = { autoSyncInterval = mins },
                                label = { Text(lbl, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MandiriNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val draft = buildDraftConfig()
                            isTesting = true
                            testResult = null
                            scope.launch {
                                testResult = CloudSyncManager.testConnection(draft)
                                isTesting = false
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Tes Koneksi", fontSize = 12.sp, color = MandiriNavy)
                        }
                    }

                    Button(
                        onClick = {
                            val draft = buildDraftConfig()
                            onSaveConfig(draft)
                            onSyncNow(draft)
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MandiriBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Sync Sekarang", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val draft = buildDraftConfig()
                            onSaveConfig(draft)
                            onForcePush(draft)
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MandiriNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Paksa", fontSize = 11.sp, color = MandiriNavy)
                    }

                    OutlinedButton(
                        onClick = {
                            val draft = buildDraftConfig()
                            onSaveConfig(draft)
                            onForcePull(draft)
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = BonvoyCoral
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ambil Cloud", fontSize = 11.sp, color = BonvoyCoral)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tip: Deploy script cloudflare-worker/worker.js ke akun Cloudflare pribadimu, buat KV binding MANVOY_KV, lalu atur Secret SYNC_SECRET.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(buildDraftConfig())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MandiriNavy)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
