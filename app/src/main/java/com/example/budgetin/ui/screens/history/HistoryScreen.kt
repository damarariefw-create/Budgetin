package com.example.budgetin.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.ui.components.DeleteButton
import com.example.budgetin.ui.components.EmptyState
import com.example.budgetin.ui.components.TransactionRow
import com.example.budgetin.ui.screens.add.DateSelector
import com.example.budgetin.ui.screens.add.FormHeader
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.util.DateUtil
import com.example.budgetin.util.Money
import java.util.Calendar

@Composable
fun HistoryScreen(
    accountId: String? = null,
    onClearFilter: () -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val filterAccountName by viewModel.filterAccountName.collectAsStateWithLifecycle()
    val mains by viewModel.mains.collectAsStateWithLifecycle()
    val branchesByParent by viewModel.branchesByParent.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) { viewModel.setFilter(accountId) }

    val grouped = remember(transactions) { groupByDay(transactions) }
    val isFiltered = accountId != null

    ResponsiveContainer {
    Column(Modifier.fillMaxSize()) {
        if (isFiltered) {
            FormHeader("Riwayat Dompet 📋", onClearFilter)
        } else {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                Text(
                    text = "Riwayat 📋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (transactions.isEmpty()) "Belum ada transaksi"
                    else "${transactions.size} transaksi tercatat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FilterBar(
            showFilters = showFilters,
            activeCount = filter.activeCount,
            onToggle = { showFilters = !showFilters },
            onReset = viewModel::reset,
        )

        if (showFilters) {
            FilterPanel(
                filter = filter,
                mains = mains,
                branchesByParent = branchesByParent,
                categories = categories,
                onSetType = viewModel::setType,
                onSetAccount = viewModel::setAccount,
                onSetCompleted = viewModel::setIsCompleted,
                onSetCategory = viewModel::setCategory,
                onSetDateRange = viewModel::setDateRange,
                onSetMinAmount = viewModel::setMinAmount,
                onSetMaxAmount = viewModel::setMaxAmount,
                onReset = viewModel::reset,
            )
        }

        if (isFiltered) {
            FilterChipRow(
                accountName = filterAccountName ?: "Dompet",
                onClear = onClearFilter,
                count = transactions.size,
            )
        }

        if (grouped.isEmpty()) {
            EmptyState(
                emoji = "🗒️",
                title = if (isFiltered) "Belum ada transaksi dompet ini" else "Riwayat kosong",
                message = if (isFiltered) {
                    "Transaksi keluar/masuk dari dompet ini akan muncul di sini."
                } else {
                    "Semua transaksi yang kamu catat akan muncul di sini, dikelompokkan per tanggal."
                },
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                grouped.forEach { (dayKey, dayTransactions) ->
                    item(key = "header_$dayKey") {
                        DayHeader(
                            dateKey = dayKey,
                            transactions = dayTransactions,
                        )
                    }
                    items(dayTransactions.size, key = { index -> dayTransactions[index].id }) { index ->
                        val transaction = dayTransactions[index]
                        TransactionRow(
                            transaction = transaction,
                            onClick = { onEditTransaction(transaction) },
                            trailing = { DeleteButton(onClick = { pendingDelete = transaction }) },
                        )
                    }
                }
            }
        }
    }
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus transaksi?") },
            text = {
                Text(
                    "Transaksi \"${transaction.category}\" senilai " +
                        "${Money.format(transaction.amount)} akan dihapus permanen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(transaction)
                    pendingDelete = null
                }) {
                    Text("Hapus", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Batal")
                }
            },
        )
    }
}

/** Baris tombol Filter + badge jumlah kriteria aktif. */
@Composable
private fun FilterBar(
    showFilters: Boolean,
    activeCount: Int,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (showFilters || activeCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (activeCount > 0) "Filter ($activeCount)" else "Filter",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (showFilters || activeCount > 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (showFilters) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (showFilters || activeCount > 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (activeCount > 0) {
            TextButton(onClick = onReset) {
                Text("Reset", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Panel filter lanjutan: jenis, status, dompet, kategori, rentang tanggal, nominal. */
@Composable
private fun FilterPanel(
    filter: HistoryFilter,
    mains: List<com.example.budgetin.data.model.Account>,
    branchesByParent: Map<String, List<com.example.budgetin.data.model.Account>>,
    categories: List<Category>,
    onSetType: (TransactionType?) -> Unit,
    onSetAccount: (String?) -> Unit,
    onSetCompleted: (Boolean?) -> Unit,
    onSetCategory: (String?) -> Unit,
    onSetDateRange: (Long?, Long?) -> Unit,
    onSetMinAmount: (Double?) -> Unit,
    onSetMaxAmount: (Double?) -> Unit,
    onReset: () -> Unit,
) {
    var minText by remember { mutableStateOf(filter.minAmount?.toLong()?.toString() ?: "") }
    var maxText by remember { mutableStateOf(filter.maxAmount?.toLong()?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FilterSection("Jenis") {
            SelectorChips(
                options = listOf("Semua", "Pemasukan", "Pengeluaran", "Transfer"),
                selected = when (filter.type) {
                    null -> "Semua"
                    TransactionType.INCOME -> "Pemasukan"
                    TransactionType.EXPENSE -> "Pengeluaran"
                    else -> "Transfer"
                },
            ) { label ->
                onSetType(
                    when (label) {
                        "Pemasukan" -> TransactionType.INCOME
                        "Pengeluaran" -> TransactionType.EXPENSE
                        "Transfer" -> TransactionType.TRANSFER
                        else -> null
                    }
                )
            }
        }

        FilterSection("Status") {
            SelectorChips(
                options = listOf("Semua", "Sudah terjadi", "Belum terjadi"),
                selected = when (filter.isCompleted) {
                    null -> "Semua"
                    true -> "Sudah terjadi"
                    else -> "Belum terjadi"
                },
            ) { label ->
                onSetCompleted(
                    when (label) {
                        "Sudah terjadi" -> true
                        "Belum terjadi" -> false
                        else -> null
                    }
                )
            }
        }

        val selectedAccountId = filter.accountId
        val branchOf = selectedAccountId?.let { id ->
            branchesByParent.entries.firstOrNull { (_, branches) -> branches.any { it.id == id } }
        }
        val selectedMainId = branchOf?.key ?: selectedAccountId

        FilterSection("Dompet") {
            SelectorChips(
                options = listOf("Semua") + mains.map { it.name },
                selected = selectedMainId?.let { id ->
                    mains.firstOrNull { it.id == id }?.name ?: "Semua"
                } ?: "Semua",
            ) { label ->
                if (label == "Semua") onSetAccount(null)
                else onSetAccount(mains.firstOrNull { it.name == label }?.id)
            }
        }

        val branches = selectedMainId?.let { branchesByParent[it] } ?: emptyList()
        if (branches.isNotEmpty()) {
            FilterSection("Branch") {
                SelectorChips(
                    options = listOf("Semua") + branches.map { it.name },
                    selected = if (branchOf != null) {
                        branches.firstOrNull { it.id == selectedAccountId }?.name ?: "Semua"
                    } else "Semua",
                ) { label ->
                    if (label == "Semua") selectedMainId?.let(onSetAccount)
                    else onSetAccount(branches.firstOrNull { it.name == label }?.id)
                }
            }
        }

        FilterSection("Kategori") {
            SelectorChips(
                options = listOf("Semua") + categories.map { "${it.emoji} ${it.name}" },
                selected = filter.categoryId?.let { id ->
                    categories.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.name}" } ?: "Semua"
                } ?: "Semua",
            ) { label ->
                if (label == "Semua") onSetCategory(null)
                else onSetCategory(categories.firstOrNull { "${it.emoji} ${it.name}" == label }?.id)
            }
        }

        FilterSection("Rentang tanggal") {
            DateSelector(
                selected = filter.dateFrom ?: System.currentTimeMillis(),
                onSelect = { from -> onSetDateRange(from, filter.dateTo) },
            )
            DateSelector(
                selected = filter.dateTo ?: System.currentTimeMillis(),
                onSelect = { to -> onSetDateRange(filter.dateFrom, to) },
            )
        }

        FilterSection("Nominal (opsional)") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = {
                        minText = it.filter { c -> c.isDigit() }.take(12)
                        onSetMinAmount(minText.toLongOrNull()?.toDouble())
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = {
                        maxText = it.filter { c -> c.isDigit() }.take(12)
                        onSetMaxAmount(maxText.toLongOrNull()?.toDouble())
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Maks") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onReset) {
                Text("Reset semua", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Chip dompet aktif: nama + jumlah transaksi, tombol X untuk kembali ke semua. */
@Composable
private fun FilterChipRow(
    accountName: String,
    count: Int,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClear)
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$accountName · $count transaksi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus filter",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Kelompokkan transaksi per hari (kunci: tengah malam tanggal lokal). */
private fun groupByDay(transactions: List<Transaction>): List<Pair<Long, List<Transaction>>> =
    transactions
        .groupBy { transaction ->
            Calendar.getInstance().apply { timeInMillis = transaction.timestamp }.let { cal ->
                DateUtil.dateOnlyMillis(cal)
            }
        }
        .toSortedMap(compareByDescending { it })
        .map { (day, list) -> day to list }

@Composable
private fun DayHeader(dateKey: Long, transactions: List<Transaction>) {
    val dayIncome = transactions.filter { it.type.name == "INCOME" }.sumOf { it.amount }
    val dayExpense = transactions.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }

    Column(Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Text(
            text = DateUtil.fullDate(dateKey),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = buildString {
                if (dayIncome > 0) append("+${Money.format(dayIncome)}  ")
                if (dayExpense > 0) append("−${Money.format(dayExpense)}")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
