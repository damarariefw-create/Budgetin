package com.example.budgetin.ui.screens.accounts

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountType
import com.example.budgetin.ui.components.CalculatorAmountField
import com.example.budgetin.ui.screens.add.FormHeader
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.util.Calculator
import com.example.budgetin.util.Money

private val accountIcons = listOf("👛", "💳", "🏦", "💰", "🪙", "💵", "📱", "🏪")
private val accountColors = listOf(
    0xFF00A86B, 0xFF10B981, 0xFF3B82F6, 0xFF8B5CF6,
    0xFFEC4899, 0xFFF59E0B, 0xFFEF4444, 0xFF06B6D4,
)

/**
 * Form buat/edit dompet/rekening: nama, jenis, saldo awal, ikon, warna,
 * plus pengelolaan cabang/sub-rekening di dalam rekening utama.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFormScreen(
    accountId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AccountFormViewModel = hiltViewModel(),
) {
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val branches by viewModel.branches.collectAsStateWithLifecycle()
    val mains by viewModel.mains.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(AccountType.CASH) }
    var icon by rememberSaveable { mutableStateOf("👛") }
    var color by rememberSaveable { mutableStateOf(0xFF00A86B) }
    var includeInTotal by rememberSaveable { mutableStateOf(true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    var showBranchDialog by remember { mutableStateOf(false) }
    var dialogBranch by remember { mutableStateOf<Account?>(null) }
    var pendingBranchDelete by remember { mutableStateOf<Account?>(null) }

    var branchName by remember(dialogBranch, showBranchDialog) {
        mutableStateOf(dialogBranch?.name ?: "")
    }
    var branchAmountText by remember(dialogBranch, showBranchDialog) {
        mutableStateOf(dialogBranch?.balance?.toLong()?.toString() ?: "")
    }

    LaunchedEffect(accountId) { viewModel.load(accountId) }
    LaunchedEffect(existing) {
        val account = existing ?: return@LaunchedEffect
        name = account.name
        amountText = account.balance.toLong().toString()
        type = account.type
        icon = account.icon
        color = account.color
        includeInTotal = account.includeInTotal
    }

    val editingBranch = existing?.parentId != null
    val parentName = mains.find { it.id == existing?.parentId }?.name
    val amount = com.example.budgetin.util.Calculator.evaluate(amountText) ?: 0.0
    val valid = name.isNotBlank() && !isSaving

    ResponsiveContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        FormHeader(
            when {
                accountId == null -> "Tambah Dompet 👛"
                editingBranch -> "Edit Cabang 📂"
                else -> "Edit Dompet 👛"
            },
            onBack,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (editingBranch) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "📂", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Cabang dari ${parentName ?: "rekening lain"}. " +
                                "Nominal ini terakumulasi ke saldo rekening utama.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (editingBranch) "Nama cabang" else "Nama dompet/rekening") },
                placeholder = { Text("Contoh: BCA, GoPay, Dompet") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Text(
                text = "Jenis",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountType.entries.forEach { t ->
                    val selected = t == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { type = t }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = t.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = "Saldo awal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Sisa saldo dihitung otomatis dari transaksi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CalculatorAmountField(
                value = amountText,
                onValueChange = { raw -> amountText = raw.take(80) },
            )

            Text(
                text = "Ikon",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accountIcons.forEach { emoji ->
                    val selected = emoji == icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color(color).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 2.dp,
                                color = if (selected) Color(color) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { icon = emoji },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            Text(
                text = "Warna",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accountColors.forEach { c ->
                    val selected = c == color
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                width = 3.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { color = c },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            if (!editingBranch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Akumulasikan ke saldo utama",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Matikan bila saldo dompet ini tidak ikut dihitung di Beranda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = includeInTotal,
                        onCheckedChange = { includeInTotal = it },
                    )
                }
            }

            if (existing != null && !editingBranch) {
                BranchManager(
                    branches = branches,
                    onAdd = {
                        dialogBranch = null
                        showBranchDialog = true
                    },
                    onEdit = { branch ->
                        dialogBranch = branch
                        showBranchDialog = true
                    },
                    onDelete = { pendingBranchDelete = it },
                )
            }

            Button(
                onClick = {
                    if (valid) {
                        isSaving = true
                        viewModel.save(
                            id = accountId,
                            name = name,
                            type = type,
                            balance = amount,
                            icon = icon,
                            color = color,
                            includeInTotal = includeInTotal,
                            parentId = existing?.parentId,
                            onSaved = {
                                isSaving = false
                                onSaved()
                            },
                        )
                    }
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Simpan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }

    if (showBranchDialog) {
        val isEdit = dialogBranch != null
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text(if (isEdit) "Edit Cabang" else "Tambah Cabang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it.take(30) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama cabang") },
                        placeholder = { Text("Contoh: Tabungan, Lainnya") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = branchAmountText,
                        onValueChange = { branchAmountText = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nominal") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = existing
                        val trimmed = branchName.trim()
                        if (current != null && trimmed.isNotBlank()) {
                            val branchAmount = Calculator.evaluate(branchAmountText) ?: 0.0
                            if (dialogBranch == null) {
                                viewModel.addBranch(current.id, trimmed, branchAmount)
                            } else {
                                viewModel.updateBranch(dialogBranch!!, trimmed, branchAmount)
                            }
                            showBranchDialog = false
                        }
                    },
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBranchDialog = false }) {
                    Text("Batal")
                }
            },
        )
    }

    pendingBranchDelete?.let { branch ->
        AlertDialog(
            onDismissRequest = { pendingBranchDelete = null },
            title = { Text("Hapus cabang?") },
            text = {
                Text(
                    "Cabang \"${branch.name}\" beserta seluruh transaksinya akan dihapus " +
                        "permanen dan ikut terhapus di perangkat lain dengan akun yang sama. " +
                        "Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBranch(branch)
                    pendingBranchDelete = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBranchDelete = null }) {
                    Text("Batal")
                }
            },
        )
    }
}

/** Seksi pengelolaan cabang/sub-rekening untuk dompet utama. */
@Composable
private fun BranchManager(
    branches: List<Account>,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
    onDelete: (Account) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Cabang / Sub-rekening",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tambahkan beberapa saldo tersendiri di dalam rekening ini, " +
                "contoh: Tabungan, Lainnya. Semua cabang terakumulasi ke saldo rekening.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (branches.isEmpty()) {
            Text(
                text = "Belum ada cabang.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            branches.forEach { branch ->
                BranchRow(
                    branch = branch,
                    onEdit = { onEdit(branch) },
                    onDelete = { onDelete(branch) },
                )
            }
        }
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Tambah Cabang")
        }
    }
}

/** Satu baris cabang: ikon, nama, nominal, tombol edit & hapus. */
@Composable
private fun BranchRow(
    branch: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(branch.color).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = branch.icon, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = branch.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Saldo awal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Money.format(branch.balance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit cabang",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus cabang",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
