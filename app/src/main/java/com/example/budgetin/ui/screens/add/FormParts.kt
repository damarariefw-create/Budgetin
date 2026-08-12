package com.example.budgetin.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.Category
import com.example.budgetin.util.DateUtil
import java.text.NumberFormat
import java.util.Locale

/** Header konsisten untuk semua form tambah data. */
@Composable
fun FormHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Input nominal dengan format ribuan Indonesia. [value] berisi digit mentah. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val amount = value.toDoubleOrNull() ?: 0.0
    val formatter = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    OutlinedTextField(
        value = if (value.isEmpty()) "" else formatter.format(amount),
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Nominal") },
        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
        placeholder = { Text("0") },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
    )
}

/** Tampilan nominal besar (untuk form yang memakai keypad angka). */
@Composable
fun AmountDisplay(
    amountText: String,
    modifier: Modifier = Modifier,
) {
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val formatter = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    Text(
        text = "Rp ${if (amountText.isEmpty()) "0" else formatter.format(amount)}",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Chip nominal cepat. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAmountChips(
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        listOf(10_000L, 25_000L, 50_000L, 100_000L, 250_000L, 500_000L).forEach { quick ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPick(quick) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = formatter.format(quick),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Pemilih dua pilihan (Pemasukan/Pengeluaran atau Hutang/Piutang). */
@Composable
fun TypeToggle(
    selectedLabel: String,
    options: List<Pair<String, Color>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, accent) ->
            val selected = label == selectedLabel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(label) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index < options.lastIndex) Spacer(Modifier.width(12.dp))
        }
    }
}

/** Saklar status transaksi: Sudah / Belum terjadi. */
@Composable
fun CompletedToggle(
    isCompleted: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isCompleted) "Sudah terjadi" else "Belum terjadi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Jika belum terjadi, saldo tidak ikut terpengaruh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isCompleted, onCheckedChange = onCheckedChange)
    }
}

/** Pilihan akun bertingkat: dompet utama dulu, lalu cabang milik dompet yang dipilih.
 *
 * Jika tidak ada cabang yang dipilih, transaksi masuk ke dompet utama itu sendiri.
 * [selectedId] bisa berisi id dompet utama maupun id cabang (saat mengedit transaksi lama).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountBranchPicker(
    mains: List<Account>,
    branchesByParent: Map<String, List<Account>>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    if (mains.isEmpty()) {
        Text(
            text = "Belum ada akun. Buka menu Lainnya untuk menambahkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val allBranches = branchesByParent.values.flatten()
    val selectedIsBranch = selectedId != null && allBranches.any { it.id == selectedId }
    val selectedMainId = if (selectedIsBranch) {
        branchesByParent.entries.firstOrNull { (_, list) -> list.any { it.id == selectedId } }?.key
    } else {
        selectedId
    }

    AccountChips(accounts = mains, selectedId = selectedMainId, onSelect = onSelect)

    val branches = selectedMainId?.let { branchesByParent[it].orEmpty() } ?: emptyList()
    if (branches.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cabang (opsional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Jika tidak memilih cabang, transaksi masuk ke dompet utama.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        AccountChips(accounts = branches, selectedId = selectedId, onSelect = onSelect)
    }
}

/** Pilihan akun (dari/ke) sebagai chip. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountChips(
    accounts: List<Account>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    if (accounts.isEmpty()) {
        Text(
            text = "Belum ada akun. Buka menu Lainnya untuk menambahkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        accounts.forEach { account ->
            val selected = account.id == selectedId
            val accent = Color(account.color)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 2.dp,
                        color = if (selected) accent else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(account.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = account.icon, fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Pilihan kategori sebagai chip emoji. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            val accent = Color(category.color)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 2.dp,
                        color = if (selected) accent else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(text = category.emoji, fontSize = 22.sp)
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Chip biaya admin (Rp 0 / Rp 2.500 / dst). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeeChips(
    options: List<Long>,
    selected: Long,
    modifier: Modifier = Modifier,
    onSelect: (Long) -> Unit,
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        options.forEach { fee ->
            val isSelected = fee == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(fee) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (fee == 0L) "Rp 0" else "Rp ${formatter.format(fee)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Pemilih tanggal (dialog DatePicker bawaan). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showPicker = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Tanggal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = DateUtil.fullDate(selected),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtil.toUtcDatePickerMillis(selected),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSelect(DateUtil.fromUtcDatePickerMillis(it)) }
                    showPicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
