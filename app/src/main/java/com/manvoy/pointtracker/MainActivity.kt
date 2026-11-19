package com.manvoy.pointtracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MandiriNavy = Color(0xFF002D5E)
private val MandiriBlue = Color(0xFF004C97)
private val MandiriGold = Color(0xFFF4B400)
private val BonvoyCoral = Color(0xFFE87A5D)
private val SoftBg = Color(0xFFF4F6FA)

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
    var transactions by remember { mutableStateOf(TransactionStorage.loadTransactions(context)) }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        if (selectedFilter == null) transactions
        else transactions.filter { it.type == selectedFilter }
    }

    val totalAmount = remember(transactions) { transactions.sumOf { it.amount } }
    val totalPoints = remember(transactions) { transactions.sumOf { it.points } }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val ok = PdfExporter.exportToPdf(context, uri, filteredTransactions)
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
                    IconButton(
                        onClick = {
                            if (filteredTransactions.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Belum ada transaksi untuk diekspor",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                pdfLauncher.launch("Tracker_Poin_Manvoy_$stamp.pdf")
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BonvoyCoral,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SummaryCard(
                totalTransactions = transactions.size,
                totalAmount = totalAmount,
                totalPoints = totalPoints
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Semua (${transactions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MandiriNavy,
                        selectedLabelColor = Color.White
                    )
                )
                TransactionType.entries.forEach { type ->
                    val count = transactions.count { it.type == type }
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

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi.\nTekan tombol + untuk mencatat transaksi baru.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { item ->
                        TransactionCard(
                            item = item,
                            onDelete = {
                                val updated = transactions.filterNot { it.id == item.id }
                                transactions = updated
                                TransactionStorage.saveTransactions(context, updated)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                val updated = (listOf(newItem) + transactions).sortedByDescending { it.dateMillis }
                transactions = updated
                TransactionStorage.saveTransactions(context, updated)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SummaryCard(
    totalTransactions: Int,
    totalAmount: Long,
    totalPoints: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                Text(
                    text = "TOTAL PEROLEHAN POIN",
                    color = Color(0xFFBBD6F2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Surface(
                    color = MandiriGold,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "$totalTransactions Transaksi",
                        color = MandiriNavy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatNumber(totalPoints),
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Nominal Transaksi",
                        color = Color(0xFFBBD6F2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatRupiah(totalAmount),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rate per Rp20.000",
                        color = Color(0xFFBBD6F2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Reg 3 • Lainnya 5",
                        color = MandiriGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    item: TransactionItem,
    onDelete: () -> Unit
) {
    val badgeColor = when (item.type) {
        TransactionType.REGULER -> MandiriBlue
        TransactionType.AIRFARE -> Color(0xFF00796B)
        TransactionType.VALAS -> Color(0xFF6A1B9A)
        TransactionType.MARRIOTT -> BonvoyCoral
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(todayUtcMillis()) }
    var selectedType by remember { mutableStateOf(TransactionType.REGULER) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val estimatedPoints = calculateManvoyPoints(parsedAmount, selectedType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Transaksi",
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
                                "Dihitung per kelipatan penuh Rp20.000 (${parsedAmount / 20_000L}x kelipatan)"
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
                            name = trimmedName,
                            dateMillis = selectedDateMillis,
                            amount = parsedAmount,
                            type = selectedType
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
