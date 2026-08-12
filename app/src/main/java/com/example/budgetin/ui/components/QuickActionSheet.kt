package com.example.budgetin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetin.ui.theme.DebtOrange
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.TransferBlue

/** Aksi cepat yang ditampilkan di bottom sheet dari tombol +. */
enum class QuickAction(
    val route: String,
    val label: String,
    val emoji: String,
    val color: Color,
) {
    TRANSFER("transfer", "Transfer", "🔁", TransferBlue),
    INCOME("income", "Penerimaan", "💰", IncomeGreen),
    EXPENSE("expense", "Pengeluaran", "💸", ExpenseRed),
    DEBT("debt", "Hutang & Piutang", "🧾", DebtOrange),
}

/** Bottom sheet aksi cepat: Transfer, Penerimaan, Pengeluaran, Hutang & Piutang. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionSheet(
    onDismiss: () -> Unit,
    onAction: (QuickAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Aksi Cepat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(24.dp))

            val firstRow = listOf(QuickAction.TRANSFER, QuickAction.INCOME)
            val secondRow = listOf(QuickAction.EXPENSE, QuickAction.DEBT)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                firstRow.forEach { action ->
                    QuickActionItem(
                        action = action,
                        onClick = { onAction(action) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                secondRow.forEach { action ->
                    QuickActionItem(
                        action = action,
                        onClick = { onAction(action) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(action.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = action.emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
