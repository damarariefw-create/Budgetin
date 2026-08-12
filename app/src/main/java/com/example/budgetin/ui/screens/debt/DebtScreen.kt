package com.example.budgetin.ui.screens.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.ui.components.DeleteButton
import com.example.budgetin.ui.components.EmptyState
import com.example.budgetin.ui.screens.add.FormHeader
import com.example.budgetin.ui.theme.DebtOrange
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.util.DateUtil
import com.example.budgetin.util.Money

/** Layar daftar hutang-piutang: ringkasan total, daftar, lunasi & hapus. */
@Composable
fun DebtScreen(
    onAddDebt: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel(),
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val accountNames = viewModel.accountName(debts, accounts)
    var showSettled by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Debt?>(null) }

    val visible = remember(debts, showSettled) {
        if (showSettled) debts.filter { it.isSettled } else debts.filter { !it.isSettled }
    }

    ResponsiveContainer {
    Column(Modifier.fillMaxSize()) {
        FormHeader("Hutang & Piutang 🧾", onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onAddDebt) {
                Text("+ Tambah", fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DebtSummaryCard(
                label = "Hutang",
                emoji = "💸",
                amount = viewModel.totalOwe(debts),
                accent = ExpenseRed,
                modifier = Modifier.weight(1f),
            )
            DebtSummaryCard(
                label = "Piutang",
                emoji = "💰",
                amount = viewModel.totalOwed(debts),
                accent = IncomeGreen,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                label = "Aktif (${debts.count { !it.isSettled }})",
                selected = !showSettled,
                onClick = { showSettled = false },
            )
            FilterChip(
                label = "Selesai (${debts.count { it.isSettled }})",
                selected = showSettled,
                onClick = { showSettled = true },
            )
        }

        if (visible.isEmpty()) {
            EmptyState(
                emoji = if (showSettled) "✅" else "🧾",
                title = if (showSettled) "Belum ada yang lunas" else "Belum ada catatan",
                message = if (showSettled) {
                    "Hutang/piutang yang sudah lunas akan tampil di sini."
                } else {
                    "Tekan + lalu pilih Hutang & Piutang untuk mencatat."
                },
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(visible, key = { it.id }) { debt ->
                    DebtRow(
                        debt = debt,
                        loanAccount = debt.accountId?.let { accountNames[it] },
                        settleAccount = debt.settledAccountId?.let { accountNames[it] },
                        onToggleSettled = { viewModel.toggleSettled(debt) },
                        onDelete = { pendingDelete = debt },
                    )
                }
            }
        }
    }
    }

    if (visible.isEmpty() && debts.isNotEmpty()) {
        Text(
            text = if (showSettled) "Tarik chip \"Aktif\" untuk melihat daftar berjalan."
            else "Tarik chip \"Selesai\" untuk melihat riwayat pelunasan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }

    pendingDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus catatan?") },
            text = {
                Text(
                    "Catatan \"${debt.counterpartName}\" senilai " +
                        "${Money.format(debt.amount)} akan dihapus permanen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(debt)
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

@Composable
private fun DebtSummaryCard(
    label: String,
    emoji: String,
    amount: Double,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(14.dp),
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DebtRow(
    debt: Debt,
    loanAccount: String?,
    settleAccount: String?,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOwed = debt.type == DebtType.OWED
    val accent = if (isOwed) IncomeGreen else DebtOrange
    val amountSign = if (isOwed) "+" else "−"

    val walletText = buildString {
        if (loanAccount != null) {
            append(if (isOwed) "Pinjam dari " else "Masuk ke ")
            append(loanAccount)
        }
        if (settleAccount != null) {
            if (loanAccount != null) append(" · ")
            append(if (isOwed) "Lunas ke " else "Lunas dari ")
            append(settleAccount)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (debt.isSettled) 0.55f else 1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = if (isOwed) "💰" else "💸", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = debt.counterpartName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(
                            when {
                                debt.isSettled -> "Lunas ✓"
                                debt.dueDate != null -> "Jatuh tempo ${DateUtil.shortDate(debt.dueDate)}"
                                else -> "Belum lunas"
                            }
                        )
                        if (walletText.isNotEmpty()) append(" · $walletText")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!debt.isSettled) {
                TextButton(onClick = onToggleSettled) {
                    Text("Lunasi", color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "$amountSign ${Money.format(debt.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DeleteButton(onClick = onDelete)
        }
    }
}
