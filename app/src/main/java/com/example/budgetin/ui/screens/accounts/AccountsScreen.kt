package com.example.budgetin.ui.screens.accounts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Account
import com.example.budgetin.ui.components.DeleteButton
import com.example.budgetin.ui.components.EmptyState
import com.example.budgetin.ui.screens.add.FormHeader
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.util.Money

/** Layar pengelolaan dompet/rekening: ringkasan, daftar, arsip & hapus. */
@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showArchived by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    val visible = remember(accounts, showArchived) {
        accounts.filter { (account, _) -> account.isArchived == showArchived }
    }
    val namesById = remember(accounts) {
        accounts.associate { it.first.id to it.first.name }
    }

    ResponsiveContainer {
    Column(Modifier.fillMaxSize()) {
        FormHeader("Dompet & Rekening 👛", onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onAddAccount) {
                Text("+ Tambah", fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TotalBalanceCard(
                emoji = "💎",
                label = "Total Saldo",
                amount = viewModel.totalBalance(accounts),
                modifier = Modifier.weight(1f),
            )
            TotalBalanceCard(
                emoji = "🗂️",
                label = "Arsip",
                amount = accounts.count { it.first.isArchived }.toDouble(),
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
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
                label = "Aktif (${accounts.count { !it.first.isArchived }})",
                selected = !showArchived,
                onClick = { showArchived = false },
            )
            FilterChip(
                label = "Arsip (${accounts.count { it.first.isArchived }})",
                selected = showArchived,
                onClick = { showArchived = true },
            )
        }

        if (visible.isEmpty()) {
            EmptyState(
                emoji = if (showArchived) "🗂️" else "👛",
                title = if (showArchived) "Arsip kosong" else "Belum ada dompet",
                message = if (showArchived) {
                    "Dompet yang kamu arsipkan akan tampil di sini."
                } else {
                    "Tekan + Tambah untuk membuat dompet atau rekeningmu sendiri."
                },
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.first.id }) { (account, balance) ->
                    AccountRow(
                        account = account,
                        balance = balance,
                        parentName = account.parentId?.let { namesById[it] },
                        indent = account.parentId != null,
                        archived = showArchived,
                        onClick = { onEditAccount(account) },
                        onArchive = { viewModel.archive(account) },
                        onRestore = { viewModel.restore(account) },
                        onDelete = { pendingDelete = account },
                    )
                }
            }
        }
    }
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus dompet?") },
            text = {
                val hasBranches = account.parentId == null &&
                    accounts.any { it.first.parentId == account.id }
                Text(
                    buildString {
                        append("Dompet \"${account.name}\" ")
                        if (hasBranches) append("beserta cabang-cabangnya dan ")
                        append(
                            "seluruh transaksinya akan dihapus permanen dan ikut terhapus " +
                                "di perangkat lain dengan akun yang sama. Tindakan ini " +
                                "tidak bisa dibatalkan."
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(account)
                    pendingDelete = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
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
private fun TotalBalanceCard(
    emoji: String,
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
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
            text = if (label == "Arsip") amount.toInt().toString() else Money.format(amount),
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
private fun AccountRow(
    account: Account,
    balance: Double,
    parentName: String?,
    indent: Boolean,
    archived: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (archived) 0.6f else 1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (indent) 36.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (indent) 36.dp else 44.dp)
                    .clip(CircleShape)
                    .background(Color(account.color).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = account.icon, fontSize = if (indent) 16.sp else 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        parentName != null -> "Cabang dari $parentName"
                        account.includeInTotal -> account.type.label
                        else -> "${account.type.label} · Tidak dihitung"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (archived) {
                    TextButton(onClick = onRestore) {
                        Text("Pulihkan", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (archived) {
                DeleteButton(onClick = onDelete)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onArchive) {
                        Text("Arsipkan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DeleteButton(onClick = onDelete)
                }
            }
        }
    }
}
