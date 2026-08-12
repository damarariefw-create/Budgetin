package com.example.budgetin.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Account
import com.example.budgetin.ui.components.BudgetinCard
import com.example.budgetin.ui.components.DonutChart
import com.example.budgetin.ui.components.EmptyState
import com.example.budgetin.ui.components.LegendRow
import com.example.budgetin.ui.components.SectionTitle
import com.example.budgetin.ui.components.TransactionRow
import com.example.budgetin.ui.theme.BalanceGradientEnd
import com.example.budgetin.ui.theme.BalanceGradientStart
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.ui.theme.amountFontSize
import com.example.budgetin.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAccountTransactions: (Account) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val expenseByCategory by viewModel.expenseByCategory.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val ownBalances by viewModel.ownBalances.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var detailAccount by remember { mutableStateOf<Account?>(null) }

    // Geser ke bawah untuk menyinkronkan data ke/dari Supabase.
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        ResponsiveContainer {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        item {
            Column {
                Text(
                    text = if (userName.isBlank()) "Halo! 👋" else "Halo, $userName! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = viewModel.monthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            BalanceCard(balance = balance, income = monthIncome, expense = monthExpense)
        }

        item {
            AccountsCarousel(
                accounts = accounts,
                onManage = onOpenAccounts,
                onAccountClick = onOpenAccountTransactions,
                onShowDetail = { detailAccount = it },
            )
        }

        item {
            BudgetinCard {
                SectionTitle(title = "Ringkasan Bulan Ini", subtitle = viewModel.monthLabel)
                Spacer(Modifier.height(16.dp))
                Row {
                    MonthSummaryItem(
                        label = "Pemasukan",
                        amount = monthIncome,
                        isExpense = false,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    MonthSummaryItem(
                        label = "Pengeluaran",
                        amount = monthExpense,
                        isExpense = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            BudgetinCard {
                SectionTitle(title = "Pengeluaran per Kategori", subtitle = viewModel.monthLabel)
                Spacer(Modifier.height(16.dp))
                if (expenseByCategory.isEmpty()) {
                    EmptyState(
                        emoji = "🎉",
                        title = "Tidak ada pengeluaran",
                        message = "Kategori pengeluaran bulan ini akan muncul di sini.",
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            data = expenseByCategory,
                            modifier = Modifier.size(120.dp),
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            expenseByCategory.take(4).forEach { LegendRow(it) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            BudgetinCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(title = "Transaksi Terakhir", modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) {
                        Text("Lihat Semua →")
                    }
                }
                if (recentTransactions.isEmpty()) {
                    EmptyState(
                        emoji = "📝",
                        title = "Belum ada transaksi",
                        message = "Tekan tombol + untuk mencatat transaksi pertamamu.",
                    )
                } else {
                    recentTransactions.forEach { transaction ->
                        TransactionRow(transaction = transaction)
                    }
                }
            }
        }
        }
    }
}

    detailAccount?.let { main ->
        AccountDetailDialog(
            account = main,
            ownBalance = ownBalances.firstOrNull { it.first.id == main.id }?.second ?: 0.0,
            branches = ownBalances.filter { it.first.parentId == main.id },
            onDismiss = { detailAccount = null },
        )
    }
}

/** Carousel horizontal dompet aktif di bawah saldo utama. */
@Composable
private fun AccountsCarousel(
    accounts: List<Pair<Account, Double>>,
    onManage: () -> Unit,
    onAccountClick: (Account) -> Unit,
    onShowDetail: (Account) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title = "Dompet Saya", modifier = Modifier.weight(1f))
            TextButton(onClick = onManage) {
                Text("Kelola →")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (accounts.isEmpty()) {
            EmptyState(
                emoji = "👛",
                title = "Belum ada dompet",
                message = "Tekan Kelola untuk menambah dompetmu.",
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(accounts, key = { it.first.id }) { (account, balance) ->
                    WalletCard(
                        account = account,
                        balance = balance,
                        onClick = { onAccountClick(account) },
                        onShowDetail = { onShowDetail(account) },
                    )
                }
            }
        }
    }
}

/** Kartu satu dompet pada carousel. Dompet yang dikecualikan tampil pudar.
 * Titik tiga di pojok kanan atas membuka rincian nominal dompet + cabangnya. */
@Composable
private fun WalletCard(
    account: Account,
    balance: Double,
    onClick: () -> Unit,
    onShowDetail: () -> Unit,
) {
    val accent = Color(account.color)
    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (account.includeInTotal) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = account.icon, fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (account.includeInTotal) account.type.label else "Tidak dihitung",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShowDetail) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Detail ${account.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = Money.format(balance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Dialog rincian dompet: nominal dompet itu sendiri + nominal tiap cabangnya. */
@Composable
private fun AccountDetailDialog(
    account: Account,
    ownBalance: Double,
    branches: List<Pair<Account, Double>>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${account.icon} ${account.name}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailMoneyRow(
                    label = "Nominal dompet",
                    amount = ownBalance,
                    account = account,
                )
                if (branches.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Cabang",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    branches.forEach { (branch, balance) ->
                        DetailMoneyRow(
                            label = branch.name,
                            amount = balance,
                            account = branch,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = Money.format(ownBalance + branches.sumOf { it.second }),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
    )
}

@Composable
private fun DetailMoneyRow(
    label: String,
    amount: Double,
    account: Account,
) {
    val accent = Color(account.color)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = account.icon, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = Money.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Kartu saldo dengan latar gradien hijau. */
@Composable
private fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(BalanceGradientStart, BalanceGradientEnd)
                )
            )
            .padding(24.dp),
    ) {
        Column {
            Text(
                text = "Saldo Total",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            val balanceText = Money.format(balance)
            Text(
                text = balanceText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = amountFontSize(balanceText, MaterialTheme.typography.headlineMedium.fontSize)
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(18.dp))
            Row {
                BalanceFlowItem(
                    label = "Pemasukan",
                    amount = income,
                    isExpense = false,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                BalanceFlowItem(
                    label = "Pengeluaran",
                    amount = expense,
                    isExpense = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BalanceFlowItem(
    label: String,
    amount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
) {
    val arrowColor = if (isExpense) Color(0xFFFFC9C4) else Color(0xFFB9F7DC)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = if (isExpense) "▼" else "▲", fontSize = 11.sp, color = arrowColor)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthSummaryItem(
    label: String,
    amount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (isExpense) ExpenseRed else IncomeGreen
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(14.dp),
    ) {
        Text(
            text = if (isExpense) "▼" else "▲",
            fontSize = 13.sp,
            color = accent,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
