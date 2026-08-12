package com.example.budgetin.ui.screens.add

import androidx.compose.runtime.Composable
import com.example.budgetin.data.model.TransactionType

/**
 * Pintu masuk form tambah data.
 * mode "income" / "expense" -> IncomeExpenseForm
 * mode "transfer"           -> TransferForm (keypad + biaya admin)
 * mode "debt"               -> DebtForm (hutang & piutang)
 */
@Composable
fun AddTransactionScreen(
    mode: String = "expense",
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    when (mode) {
        "transfer" -> TransferForm(onBack = onBack, onSaved = onSaved)
        "debt" -> DebtForm(onBack = onBack, onSaved = onSaved)
        "income" -> IncomeExpenseForm(
            initialType = TransactionType.INCOME,
            onBack = onBack,
            onSaved = onSaved,
        )

        else -> IncomeExpenseForm(
            initialType = TransactionType.EXPENSE,
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}
