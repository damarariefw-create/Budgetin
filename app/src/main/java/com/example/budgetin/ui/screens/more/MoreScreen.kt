package com.example.budgetin.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.ui.components.BudgetinCard
import com.example.budgetin.ui.theme.DebtOrange
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer

private data class MoreItem(
    val label: String,
    val emoji: String,
    val color: Color,
)

@Composable
fun MoreScreen(
    onOpenStatistics: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenAccounts: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val isSigningOut by viewModel.isSigningOut.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncResult by viewModel.syncResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncResult) {
        val message = syncResult ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Tutup",
            duration = SnackbarDuration.Long,
        )
        viewModel.dismissSyncResult()
    }

    Box(Modifier.fillMaxSize()) {
    ResponsiveContainer {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Text(
                text = "Lainnya ⚙️",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Menu dan pengaturan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            BudgetinCard {
                MoreMenuRow(
                    item = MoreItem("Statistik & Analisis", "📊", Color(0xFF8B5CF6)),
                    onClick = onOpenStatistics,
                )
            }
            Spacer(Modifier.height(16.dp))
            BudgetinCard {
                MoreMenuRow(
                    item = MoreItem("Dompet & Rekening", "👛", IncomeGreen),
                    onClick = onOpenAccounts,
                )
            }
            Spacer(Modifier.height(16.dp))
            BudgetinCard {
                MoreMenuRow(
                    item = MoreItem("Hutang & Piutang", "🧾", DebtOrange),
                    onClick = onOpenDebts,
                )
            }
            Spacer(Modifier.height(16.dp))
            BudgetinCard {
                MoreMenuRow(
                    item = MoreItem(
                        label = if (isSyncing) "Menyinkronkan…" else "Sinkronkan Sekarang",
                        emoji = "🔄",
                        color = Color(0xFF06B6D4),
                    ),
                    showProgress = isSyncing,
                    onClick = viewModel::syncNow,
                )
            }
            Spacer(Modifier.height(16.dp))
            BudgetinCard {
                MoreMenuRow(
                    item = MoreItem("Keluar", "🚪", ExpenseRed),
                    onClick = { showLogoutDialog = true },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Budgetin v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar?") },
            text = { Text("Kamu akan kembali ke layar masuk. Data lokal tetap tersimpan.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut()
                }) {
                    Text("Keluar", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            },
        )
    }

    if (isSigningOut) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Keluar…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Menunggu sesi ditutup")
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun MoreMenuRow(
    item: MoreItem,
    onClick: () -> Unit,
    showProgress: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !showProgress, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.label == "Keluar") ExpenseRed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (showProgress) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(text = "›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
