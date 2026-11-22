package com.manvoy.pointtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BillingPeriodTabBar(
    tabs: List<BillingPeriodTab>,
    selectedTabId: String,
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = remember(tabs, selectedTabId) {
        tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedIndex, tabs.size) {
        if (tabs.isNotEmpty() && selectedIndex in tabs.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(tabs, key = { _, tab -> tab.id }) { _, tab ->
                        val isSelected = tab.id == selectedTabId
                        Column(
                            modifier = Modifier
                                .clickable { onSelectTab(tab.id) }
                                .padding(horizontal = 6.dp)
                                .width(IntrinsicSize.Max),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(13.dp))
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(if (isSelected) TabIndicatorBlue else Color.Transparent)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFE2E8F0))
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Atur Periode Billing",
                        tint = MandiriNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFE5EAF0), thickness = 1.dp)
        }
    }
}

@Composable
fun BillingPeriodSettingsDialog(
    currentConfig: BillingConfig,
    onDismiss: () -> Unit,
    onSave: (BillingConfig) -> Unit
) {
    var startDay by remember { mutableIntStateOf(currentConfig.startDay.coerceIn(1, 31)) }
    var pastPeriodsCount by remember { mutableIntStateOf(currentConfig.pastPeriodsCount.coerceIn(1, 24)) }
    var showAllTab by remember { mutableStateOf(currentConfig.showAllTab) }
    var customPeriods by remember { mutableStateOf(currentConfig.customPeriods) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    val today = remember { todayLocalDate() }
    val previewCurrentYm = remember(startDay, today) { findCycleStartYearMonth(today, startDay) }
    val previewCurrentBounds = remember(previewCurrentYm, startDay) {
        getBillingCycleBounds(previewCurrentYm, startDay)
    }
    val previewPrevBounds = remember(previewCurrentYm, startDay) {
        getBillingCycleBounds(previewCurrentYm.minusMonths(1), startDay)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pengaturan Periode Billing",
                fontWeight = FontWeight.Bold,
                color = MandiriNavy,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tanggal Mulai Siklus Tagihan",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MandiriNavy
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (startDay > 1) startDay-- },
                        enabled = startDay > 1,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Kurangi tanggal",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tanggal $startDay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MandiriNavy
                        )
                        Text(
                            text = if (startDay == 1) "Tutup buku: Akhir bulan"
                            else "Tutup buku: Tanggal ${startDay - 1}",
                            fontSize = 11.sp,
                            color = Color(0xFF5D6D7E)
                        )
                    }

                    OutlinedButton(
                        onClick = { if (startDay < 31) startDay++ },
                        enabled = startDay < 31,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah tanggal",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick preset tanggal mulai
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 6, 11, 16, 21, 26).forEach { preset ->
                        FilterChip(
                            selected = startDay == preset,
                            onClick = { startDay = preset },
                            label = { Text("Tgl $preset", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiriNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Preview siklus billing
                Surface(
                    color = SoftBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Preview Siklus Billing:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiriNavy
                        )
                        Text(
                            text = "• Sebelumnya: ${formatPeriodRangeLabel(previewPrevBounds.first, previewPrevBounds.second)}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "• Belum Tertagih: ${formatPeriodRangeLabel(previewCurrentBounds.first, previewCurrentBounds.second)}",
                            fontSize = 11.5.sp,
                            color = TabIndicatorBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Jumlah riwayat periode
                Text(
                    text = "Jumlah Riwayat Periode di Tab Bar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MandiriNavy
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 6, 12, 18).forEach { count ->
                        FilterChip(
                            selected = pastPeriodsCount == count,
                            onClick = { pastPeriodsCount = count },
                            label = { Text("$count Bulan", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MandiriNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Toggle tab Semua
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tampilkan Tab 'Semua'",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Lihat akumulasi seluruh periode",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = showAllTab,
                        onCheckedChange = { showAllTab = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MandiriNavy
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Periode Custom Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Periode Custom (${customPeriods.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MandiriNavy
                    )
                    TextButton(onClick = { showAddCustomDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp)
                    }
                }

                if (customPeriods.isEmpty()) {
                    Text(
                        text = "Opsional: tambahkan rentang tanggal khusus (mis. promo atau periode manual).",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                } else {
                    customPeriods.forEach { cp ->
                        Surface(
                            color = SoftBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cp.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MandiriNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatDateUtc(cp.startMillis)} - ${formatDateUtc(cp.endMillis)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF5D6D7E)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        customPeriods = customPeriods.filterNot { it.id == cp.id }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus periode custom",
                                        tint = BonvoyCoral,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BillingConfig(
                            startDay = startDay,
                            pastPeriodsCount = pastPeriodsCount,
                            showAllTab = showAllTab,
                            customPeriods = customPeriods,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MandiriNavy)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showAddCustomDialog) {
        AddCustomPeriodDialog(
            onDismiss = { showAddCustomDialog = false },
            onAdd = { newCustom ->
                customPeriods = customPeriods + newCustom
                showAddCustomDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomPeriodDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomBillingPeriod) -> Unit
) {
    val today = remember { todayLocalDate() }
    var label by remember { mutableStateOf("") }
    var startMillis by remember { mutableLongStateOf(localDateToUtcMillis(today.withDayOfMonth(1))) }
    var endMillis by remember { mutableLongStateOf(localDateToUtcMillis(today)) }
    var pickMode by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Periode Custom",
                fontWeight = FontWeight.Bold,
                color = MandiriNavy,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        errorMessage = null
                    },
                    label = { Text("Nama Tab (Opsional)") },
                    placeholder = { Text("Contoh: Promo Q4 / Trip Khusus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { pickMode = "start" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MandiriNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai: ${formatDateUtc(startMillis)}",
                        color = MandiriNavy,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = { pickMode = "end" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MandiriNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selesai: ${formatDateUtc(endMillis)}",
                        color = MandiriNavy,
                        modifier = Modifier.weight(1f)
                    )
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
                    if (endMillis < startMillis) {
                        errorMessage = "Tanggal selesai tidak boleh sebelum tanggal mulai."
                        return@Button
                    }
                    val startDate = utcMillisToLocalDate(startMillis)
                    val endDate = utcMillisToLocalDate(endMillis)
                    val finalLabel = label.trim().ifBlank {
                        formatPeriodTabLabel(startDate, endDate, today.year)
                    }
                    onAdd(
                        CustomBillingPeriod(
                            label = finalLabel,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MandiriNavy)
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (pickMode != null) {
        val isStart = pickMode == "start"
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (isStart) startMillis else endMillis
        )
        DatePickerDialog(
            onDismissRequest = { pickMode = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { chosen ->
                            if (isStart) startMillis = chosen else endMillis = chosen
                            errorMessage = null
                        }
                        pickMode = null
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickMode = null }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
