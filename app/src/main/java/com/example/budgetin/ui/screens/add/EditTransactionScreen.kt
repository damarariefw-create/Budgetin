package com.example.budgetin.ui.screens.add

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.TransactionType

/**
 * Layar edit transaksi: memuat transaksi berdasarkan id lalu menampilkan
 * form yang sesuai (IncomeExpenseForm / TransferForm) dengan data terisi.
 */
@Composable
fun EditTransactionScreen(
    transactionId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val editing by viewModel.editing.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) { viewModel.loadForEdit(transactionId) }

    val transaction = editing
    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (transaction.type == TransactionType.TRANSFER) {
        TransferForm(
            viewModel = viewModel,
            initialTransaction = transaction,
            onBack = onBack,
            onSaved = onSaved,
        )
    } else {
        IncomeExpenseForm(
            initialType = transaction.type,
            viewModel = viewModel,
            initialTransaction = transaction,
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}
