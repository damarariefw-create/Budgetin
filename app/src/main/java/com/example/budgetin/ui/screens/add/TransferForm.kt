package com.example.budgetin.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.ui.components.CalculatorAmountField
import com.example.budgetin.ui.theme.ResponsiveContainer
import com.example.budgetin.ui.theme.TransferBlue

/** Form Transfer antar akun: nominal (keypad), biaya admin, akun asal & tujuan. */
@Composable
fun TransferForm(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    initialTransaction: Transaction? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val branchesByParent by viewModel.branchesByParent.collectAsStateWithLifecycle()
    val isEditing = initialTransaction != null

    var amountText by rememberSaveable {
        mutableStateOf(initialTransaction?.amount?.toLong()?.toString() ?: "")
    }
    var adminFee by rememberSaveable {
        mutableStateOf(initialTransaction?.adminFee?.toLong() ?: 0L)
    }
    var fromId by rememberSaveable { mutableStateOf(initialTransaction?.accountId) }
    var toId by rememberSaveable { mutableStateOf(initialTransaction?.transferToAccountId) }
    var note by rememberSaveable { mutableStateOf(initialTransaction?.note ?: "") }
    var selectedDate by rememberSaveable {
        mutableStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var isCompleted by rememberSaveable {
        mutableStateOf(initialTransaction?.isCompleted ?: true)
    }

    val from = allAccounts.firstOrNull { it.id == fromId }
    val to = allAccounts.firstOrNull { it.id == toId }
    val amount = com.example.budgetin.util.Calculator.evaluate(amountText) ?: 0.0
    val valid = amount > 0 && from != null && to != null && from.id != to.id && !isSaving

    ResponsiveContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        FormHeader(if (isEditing) "Edit Transfer" else "Transfer 🔁", onBack)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CalculatorAmountField(
                value = amountText,
                onValueChange = { raw -> amountText = raw.take(80) },
            )

            Text(
                text = "Biaya Admin",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FeeChips(options = listOf(0L, 2_500L, 4_500L, 6_500L), selected = adminFee) {
                adminFee = it
            }

            Text(
                text = "Dari Akun",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AccountBranchPicker(
                mains = accounts,
                branchesByParent = branchesByParent,
                selectedId = fromId,
                onSelect = {
                    fromId = it
                    if (toId == it) toId = null
                },
            )

            Text(
                text = "Ke Akun",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AccountBranchPicker(
                mains = accounts,
                branchesByParent = branchesByParent,
                selectedId = toId,
                onSelect = {
                    toId = it
                    if (fromId == it) fromId = null
                },
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Contoh: Tarik tunai") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            DateSelector(selected = selectedDate, onSelect = { selectedDate = it })

            CompletedToggle(isCompleted = isCompleted, onCheckedChange = { isCompleted = it })

            Button(
                onClick = {
                    if (!isSaving && from != null && to != null) {
                        isSaving = true
                        if (isEditing && initialTransaction != null) {
                            viewModel.updateTransfer(
                                transactionId = initialTransaction.id,
                                amount = amount,
                                adminFee = adminFee.toDouble(),
                                from = from,
                                to = to,
                                note = note,
                                timestamp = selectedDate,
                                isCompleted = isCompleted,
                                onSaved = {
                                    isSaving = false
                                    onSaved()
                                },
                            )
                        } else {
                            viewModel.saveTransfer(
                                amount = amount,
                                adminFee = adminFee.toDouble(),
                                from = from,
                                to = to,
                                note = note,
                                timestamp = selectedDate,
                                isCompleted = isCompleted,
                                onSaved = {
                                    isSaving = false
                                    onSaved()
                                },
                            )
                        }
                    }
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Simpan Perubahan" else "Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}
