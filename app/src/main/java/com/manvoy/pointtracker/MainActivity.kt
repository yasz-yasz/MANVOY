package com.manvoy.pointtracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal val MandiriNavy = Color(0xFF002D5E)
internal val MandiriBlue = Color(0xFF004C97)
internal val TabIndicatorBlue = Color(0xFF007AFF)
internal val MandiriGold = Color(0xFFF4B400)
internal val BonvoyCoral = Color(0xFFE87A5D)
internal val SuccessGreen = Color(0xFF2ECC71)
internal val SoftBg = Color(0xFFF4F6FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = MandiriNavy,
                    secondary = BonvoyCoral,
                    tertiary = MandiriGold,
                    background = SoftBg,
                    surface = Color.White
                )
            ) {
                ManvoyTrackerScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManvoyTrackerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var transactions by remember { mutableStateOf(TransactionStorage.loadTransactions(context)) }
    var deletedIds by remember { mutableStateOf(TransactionStorage.loadDeletedTransactions(context)) }
    var billingConfig by remember { mutableStateOf(TransactionStorage.loadBillingConfig(context)) }
    var syncConfig by remember { mutableStateOf(TransactionStorage.loadSyncConfig(context)) }
    var syncState by remember {
        mutableStateOf(if (syncConfig.lastSyncAt > 0L) SyncState.SUCCESS else SyncState.IDLE)
    }

    var selectedTabId by remember { mutableStateOf(TAB_ID_UNBILLED) }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var showBillingSettings by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    val periodTabs = remember(billingConfig, transactions) {
        buildBillingPeriodTabs(billingConfig, transactions)
    }

    val activeTab = remember(periodTabs, selectedTabId) {
        periodTabs.firstOrNull { it.id == selectedTabId }
            ?: periodTabs.firstOrNull { it.id == TAB_ID_UNBILLED }
            ?: periodTabs.first()
    }

    LaunchedEffect(activeTab.id) {
        if (selectedTabId != activeTab.id) {
            selectedTabId = activeTab.id
        }
    }

    val periodTransactions = remember(transactions, activeTab) {
        filterTransactionsByPeriod(transactions, activeTab)
    }

    val filteredTransactions = remember(periodTransactions, selectedFilter) {
        if (selectedFilter == null) periodTransactions
        else periodTransactions.filter { it.type == selectedFilter }
    }

    val periodSummary = remember(periodTransactions) {
        calculatePointsSummary(periodTransactions)
    }

    fun applySyncResult(result: SyncResult, activeCfg: CloudSyncConfig, showToast: Boolean) {
        if (result.success) {
            result.transactions?.let { transactions = it }
            result.deletedIds?.let { deletedIds = it }
            result.billingConfig?.let { billingConfig = it }
            val updatedCfg = activeCfg.copy(
                lastSyncAt = result.syncedAt,
                lastSyncMessage = result.message
            )
            syncConfig = updatedCfg
            TransactionStorage.saveSyncConfig(context, updatedCfg)
            syncState = SyncState.SUCCESS
        } else {
            val updatedCfg = activeCfg.copy(lastSyncMessage = result.message)
            syncConfig = updatedCfg
            TransactionStorage.saveSyncConfig(context, updatedCfg)
            syncState = SyncState.ERROR
        }
        if (showToast) {
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun runTwoWaySync(
        cfg: CloudSyncConfig = syncConfig,
        txList: List<TransactionItem> = transactions,
        delMap: Map<String, Long> = deletedIds,
        billCfg: BillingConfig = billingConfig,
        showToast: Boolean = false
    ) {
        if (!cfg.isConfigured || syncState == SyncState.SYNCING) return
        scope.launch {
            syncState = SyncState.SYNCING
            val result = CloudSyncManager.syncTwoWay(
                context = context,
                config = cfg,
                localTransactions = txList,
                localDeleted = delMap,
                localBillingConfig = billCfg
            )
            applySyncResult(result, cfg, showToast)
        }
    }

    LaunchedEffect(
        syncConfig.autoSyncEnabled,
        syncConfig.workerUrl,
        syncConfig.apiToken,
        syncConfig.syncKey,
        syncConfig.autoSyncIntervalMinutes
    ) {
        if (syncConfig.autoSyncEnabled && syncConfig.isConfigured) {
            runTwoWaySync(showToast = false)
            if (syncConfig.autoSyncIntervalMinutes > 0) {
                while (true) {
                    delay(syncConfig.autoSyncIntervalMinutes * 60_000L)
                    if (syncConfig.autoSyncEnabled && syncConfig.isConfigured) {
                        runTwoWaySync(showToast = false)
                    }
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val periodTitle = if (activeTab.isUnbilled) {
                "Belum Tertagih (${activeTab.rangeLabel})"
            } else if (activeTab.isAll) {
                "Semua Periode"
            } else {
                activeTab.rangeLabel
            }
            val ok = PdfExporter.exportToPdf(context, uri, filteredTransactions, periodTitle)
            Toast.makeText(
                context,
                if (ok) "PDF berhasil diekspor!" else "Gagal mengekspor PDF.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        containerColor = SoftBg,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Tracker Poin Manvoy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Mandiri × Marriott Bonvoy",
                                fontSize = 11.sp,
                                color = Color(0xFFD0E2F5)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MandiriNavy,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { showSyncDialog = true }) {
                            when {
                                syncState == SyncState.SYNCING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MandiriGold
                                    )
                                }
                                !syncConfig.isConfigured -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Sinkronisasi Cloud",
                                        tint = Color(0xFFBBD6F2)
                                    )
                                }
                                syncState == SyncState.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.SyncProblem,
                                        contentDescription = "Sinkronisasi Gagal",
                                        tint = BonvoyCoral
                                    )
                                }
                                syncState == SyncState.SUCCESS -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Tersinkronisasi",
                                        tint = SuccessGreen
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sinkronisasi Cloud",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (filteredTransactions.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Belum ada transaksi di periode ini untuk diekspor",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    val safePeriod = activeTab.label
                                        .replace(Regex("[^A-Za-z0-9]+"), "_")
                                        .trim('_')
                                    pdfLauncher.launch("Manvoy_${safePeriod}_$stamp.pdf")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Ekspor PDF"
                            )
                        }
                    }
                )

                BillingPeriodTabBar(
                    tabs = periodTabs,
                    selectedTabId = activeTab.id,
                    onSelectTab = { newId ->
                        selectedTabId = newId
                    },
                    onOpenSettings = { showBillingSettings = true }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showAddDialog = true
                },
                containerColor = BonvoyCoral,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "summary_card") {
                SummaryCard(
                    summary = periodSummary,
                    activeTab = activeTab,
                    onClickPeriodSettings = { showBillingSettings = true }
                )
            }

            item(key = "filter_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Semua (${periodTransactions.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MandiriNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                    TransactionType.entries.forEach { type ->
                        val count = periodTransactions.count { it.type == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = {
                                selectedFilter = if (selectedFilter == type) null else type
                            },
                            label = { Text("${type.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiriNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada transaksi di periode ${activeTab.label}.
Tekan tombol + untuk mencatat transaksi baru.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TransactionCard(
                            item = item,
                            onEdit = {
                                editingTransaction = item
                                showAddDialog = true
                            },
                            onDelete = {
                                val now = System.currentTimeMillis()
                                val updated = transactions.filterNot { it.id == item.id }
                                val updatedDeleted = deletedIds + (item.id to now)
                                transactions = updated
                                deletedIds = updatedDeleted
                                TransactionStorage.saveTransactions(context, updated)
                                TransactionStorage.saveDeletedTransactions(context, updatedDeleted)
                                if (syncConfig.autoSyncEnabled && syncConfig.isConfigured) {
                                    runTwoWaySync(
                                        txList = updated,
                                        delMap = updatedDeleted,
                                        showToast = false
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOrEditTransactionDialog(
            initialItem = editingTransaction,
            defaultDateMillis = defaultDateMillisForTab(activeTab),
            onDismiss = {
                showAddDialog = false
                editingTransaction = null
            },
            onSave = { savedItem ->
                val updated = (listOf(savedItem) + transactions.filterNot { it.id == savedItem.id })
                    .sortedWith(
                        compareByDescending<TransactionItem> { it.dateMillis }
                            .thenByDescending { it.updatedAt }
                    )
                val updatedDeleted = deletedIds - savedItem.id
                transactions = updated
                deletedIds = updatedDeleted
                TransactionStorage.saveTransactions(context, updated)
                TransactionStorage.saveDeletedTransactions(context, updatedDeleted)
                showAddDialog = false
                editingTransaction = null
                if (syncConfig.autoSyncEnabled && syncConfig.isConfigured) {
                    runTwoWaySync(
                        txList = updated,
                        delMap = updatedDeleted,
                        showToast = false
                    )
                }
            }
        )
    }

    if (showBillingSettings) {
        BillingPeriodSettingsDialog(
            currentConfig = billingConfig,
            onDismiss = { showBillingSettings = false },
            onSave = { newConfig ->
                billingConfig = newConfig
                TransactionStorage.saveBillingConfig(context, newConfig)
                showBillingSettings = false
                Toast.makeText(
                    context,
                    "Periode billing diperbarui (mulai tanggal ${newConfig.startDay}).",
                    Toast.LENGTH_SHORT
                ).show()
                if (syncConfig.autoSyncEnabled && syncConfig.isConfigured) {
                    runTwoWaySync(billCfg = newConfig, showToast = false)
                }
            }
        )
    }

    if (showSyncDialog) {
        CloudSyncDialog(
            currentConfig = syncConfig,
            syncState = syncState,
            onDismiss = { showSyncDialog = false },
            onSaveConfig = { updatedCfg ->
                syncConfig = updatedCfg
                TransactionStorage.saveSyncConfig(context, updatedCfg)
            },
            onSyncNow = { cfg ->
                runTwoWaySync(cfg = cfg, showToast = true)
            },
            onForcePush = { cfg ->
                if (!cfg.isConfigured || syncState == SyncState.SYNCING) return@CloudSyncDialog
                scope.launch {
                    syncState = SyncState.SYNCING
                    val result = CloudSyncManager.pushOverwrite(
                        context = context,
                        config = cfg,
                        localTransactions = transactions,
                        localDeleted = deletedIds,
                        localBillingConfig = billingConfig
                    )
                    applySyncResult(result, cfg, showToast = true)
                }
            },
            onForcePull = { cfg ->
                if (!cfg.isConfigured || syncState == SyncState.SYNCING) return@CloudSyncDialog
                scope.launch {
                    syncState = SyncState.SYNCING
                    val result = CloudSyncManager.pullOverwrite(
                        context = context,
                        config = cfg
                    )
                    applySyncResult(result, cfg, showToast = true)
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    summary: PointsSummary,
    activeTab: BillingPeriodTab,
    onClickPeriodSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MandiriNavy, MandiriBlue)
                )
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onClickPeriodSettings)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MandiriGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (activeTab.isUnbilled) {
                                "Belum Tertagih • ${activeTab.rangeLabel}"
                            } else {
                                activeTab.rangeLabel
                            },
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    color = MandiriGold,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${summary.transactionCount} Transaksi",
                        color = MandiriNavy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "TOTAL PEROLEHAN POIN",
                color = Color(0xFFBBD6F2),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatNumber(summary.totalPoints),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Poin Bonvoy",
                    color = BonvoyCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Text(
                text = "Poin Dasar: ${formatNumber(summary.basePoints)}  •  Bonus 30Jt: +${formatNumber(summary.bonusPoints)}",
                color = Color(0xFFD6E4F5),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.11f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (summary.isBonusUnlocked) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.EmojiEvents
                                },
                                contentDescription = null,
                                tint = if (summary.isBonusUnlocked) SuccessGreen else MandiriGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (summary.isBonusUnlocked) {
                                    "Bonus Rp30 Jt Tercapai!"
                                } else {
                                    "Target Bonus Rp30 Juta"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (summary.isBonusUnlocked) {
                                "+2.500 Poin"
                            } else {
                                "${(summary.bonusProgress * 100).toInt()}%"
                            },
                            color = if (summary.isBonusUnlocked) SuccessGreen else MandiriGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    LinearProgressIndicator(
                        progress = { summary.bonusProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = if (summary.isBonusUnlocked) SuccessGreen else MandiriGold,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = if (summary.isBonusUnlocked) {
                            "Selamat! Bonus 2.500 poin otomatis ditambahkan ke total poin periode ini."
                        } else {
                            "Kurang ${formatRupiah(summary.remainingForBonus)} lagi untuk bonus +2.500 poin"
                        },
                        color = Color(0xFFD6E4F5),
                        fontSize = 10.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Transaksi",
                        color = Color(0xFFBBD6F2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatRupiah(summary.totalAmount),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Earning Rate Rata-rata",
                        color = Color(0xFFBBD6F2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatRupiahPerPoint(summary.rupiahPerPoint),
                        color = MandiriGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formatPointsPer20k(summary.pointsPer20k),
                        color = Color(0xFFD6E4F5),
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    item: TransactionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (item.type) {
        TransactionType.REGULER -> MandiriBlue
        TransactionType.AIRFARE -> Color(0xFF00796B)
        TransactionType.VALAS -> Color(0xFF6A1B9A)
        TransactionType.MARRIOTT -> BonvoyCoral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.type.label} (${item.type.pointsPer20k} pt/20rb)",
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateUtc(item.dateMillis),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A2433)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatRupiah(item.amount),
                    fontSize = 13.sp,
                    color = Color(0xFF4A5A6D)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (item.points > 0) MandiriGold.copy(alpha = 0.22f) else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+${formatNumber(item.points)} Poin",
                        color = if (item.points > 0) MandiriNavy else Color.Gray,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditTransactionDialog(
    initialItem: TransactionItem?,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (TransactionItem) -> Unit
) {
    val isEdit = initialItem != null
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var amountText by remember { mutableStateOf(initialItem?.amount?.toString() ?: "") }
    var selectedDateMillis by remember {
        mutableLongStateOf(initialItem?.dateMillis ?: defaultDateMillis)
    }
    var selectedType by remember { mutableStateOf(initialItem?.type ?: TransactionType.REGULER) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val estimatedPoints = calculateManvoyPoints(parsedAmount, selectedType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Transaksi" else "Tambah Transaksi",
                fontWeight = FontWeight.Bold,
                color = MandiriNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nama Transaksi") },
                    placeholder = { Text("Contoh: Hotel Marriott / Tiket Pesawat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MandiriNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tanggal: ${formatDateUtc(selectedDateMillis)}",
                        color = MandiriNavy,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal Transaksi (Rp)") },
                    placeholder = { Text("Contoh: 39999") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showTypeDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Jenis: ${selectedType.label} (${selectedType.pointsPer20k} poin/20rb)",
                            color = MandiriNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MandiriNavy
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        TransactionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text("${type.label} (${type.pointsPer20k} poin / Rp20.000)")
                                },
                                onClick = {
                                    selectedType = type
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Surface(
                    color = SoftBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Estimasi Perolehan Poin",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "+${formatNumber(estimatedPoints)} Poin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (estimatedPoints > 0) MandiriNavy else BonvoyCoral
                        )
                        Text(
                            text = if (parsedAmount in 1..19_999L) {
                                "Di bawah Rp20.000 belum mendapatkan poin"
                            } else {
                                "Dihitung per kelipatan penuh Rp20.000 (${parsedAmount / POINT_MULTIPLE_AMOUNT}x kelipatan)"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF5D6D7E)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Nama transaksi wajib diisi."
                        return@Button
                    }
                    if (parsedAmount <= 0L) {
                        errorMessage = "Nominal transaksi harus lebih dari 0."
                        return@Button
                    }
                    onSave(
                        TransactionItem(
                            id = initialItem?.id ?: UUID.randomUUID().toString(),
                            name = trimmedName,
                            dateMillis = selectedDateMillis,
                            amount = parsedAmount,
                            type = selectedType,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MandiriNavy)
            ) {
                Text(if (isEdit) "Perbarui" else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
