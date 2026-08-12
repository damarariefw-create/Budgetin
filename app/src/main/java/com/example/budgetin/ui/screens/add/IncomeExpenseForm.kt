package com.example.budgetin.ui.screens.add

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.RecurringPeriod
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.ui.components.CalculatorAmountField
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer

/** Form Pemasukan & Pengeluaran: nominal, kategori, akun, rutin (income), tanggal. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncomeExpenseForm(
    initialType: TransactionType,
    viewModel: AddTransactionViewModel = hiltViewModel(),
    initialTransaction: Transaction? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val branchesByParent by viewModel.branchesByParent.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val isEditing = initialTransaction != null

    var type by rememberSaveable {
        mutableStateOf(initialTransaction?.type ?: initialType)
    }
    var amountText by rememberSaveable {
        mutableStateOf(initialTransaction?.amount?.toLong()?.toString() ?: "")
    }
    var selectedCategoryId by rememberSaveable {
        mutableStateOf(initialTransaction?.categoryId)
    }
    var selectedAccountId by rememberSaveable {
        mutableStateOf(initialTransaction?.accountId)
    }
    var note by rememberSaveable {
        mutableStateOf(initialTransaction?.note ?: "")
    }
    var selectedDate by rememberSaveable {
        mutableStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var isRecurring by rememberSaveable {
        mutableStateOf(initialTransaction?.isRecurring ?: false)
    }
    var recurringPeriod by rememberSaveable {
        mutableStateOf(initialTransaction?.recurringPeriod ?: RecurringPeriod.MONTHLY)
    }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var isCompleted by rememberSaveable {
        mutableStateOf(initialTransaction?.isCompleted ?: true)
    }

    val categories = if (type == TransactionType.INCOME) incomeCategories else expenseCategories
    val category = categories.firstOrNull { it.id == selectedCategoryId }
    val account = allAccounts.firstOrNull { it.id == selectedAccountId }
    val amount = com.example.budgetin.util.Calculator.evaluate(amountText) ?: 0.0

    ResponsiveContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        FormHeader(if (isEditing) "Edit Transaksi" else "Tambah Transaksi", onBack)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TypeToggle(
                selectedLabel = if (type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran",
                options = listOf(
                    "Pemasukan" to IncomeGreen,
                    "Pengeluaran" to ExpenseRed,
                ),
                onSelect = { label ->
                    type = if (label == "Pemasukan") TransactionType.INCOME else TransactionType.EXPENSE
                    selectedCategoryId = null
                },
            )

            CalculatorAmountField(
                value = amountText,
                onValueChange = { raw -> amountText = raw.take(80) },
            )

            Text(
                text = "Kategori",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CategoryChips(categories = categories, selectedId = selectedCategoryId) {
                selectedCategoryId = it
            }

            Text(
                text = "Dari Akun",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AccountBranchPicker(
                mains = accounts,
                branchesByParent = branchesByParent,
                selectedId = selectedAccountId,
                onSelect = { selectedAccountId = it },
            )

            if (type == TransactionType.INCOME) {
                RecurringSection(
                    isRecurring = isRecurring,
                    period = recurringPeriod,
                    onRecurringChange = { isRecurring = it },
                    onPeriodChange = { recurringPeriod = it },
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Contoh: Makan siang di kantin") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            DateSelector(selected = selectedDate, onSelect = { selectedDate = it })

            CompletedToggle(isCompleted = isCompleted, onCheckedChange = { isCompleted = it })

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        val cat = category
                        val acc = account
                        if (cat != null && acc != null) {
                            if (isEditing && initialTransaction != null) {
                                viewModel.updateTransaction(
                                    transactionId = initialTransaction.id,
                                    type = type,
                                    amount = amount,
                                    adminFee = initialTransaction.adminFee,
                                    category = cat,
                                    account = acc,
                                    note = note,
                                    timestamp = selectedDate,
                                    isRecurring = isRecurring,
                                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                                    isCompleted = isCompleted,
                                    onSaved = {
                                        isSaving = false
                                        onSaved()
                                    },
                                )
                            } else {
                                viewModel.saveTransaction(
                                    type = type,
                                    amount = amount,
                                    adminFee = 0.0,
                                    category = cat,
                                    account = acc,
                                    note = note,
                                    timestamp = selectedDate,
                                    isRecurring = isRecurring,
                                    recurringPeriod = if (isRecurring) recurringPeriod else null,
                                    isCompleted = isCompleted,
                                    onSaved = {
                                        isSaving = false
                                        onSaved()
                                    },
                                )
                            }
                        }
                    }
                },
                enabled = amount > 0 && category != null && account != null && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.INCOME) IncomeGreen
                    else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Simpan Perubahan" else "Simpan Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}

/** Saklar transaksi rutin + pilihan periode. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringSection(
    isRecurring: Boolean,
    period: RecurringPeriod,
    onRecurringChange: (Boolean) -> Unit,
    onPeriodChange: (RecurringPeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Transaksi rutin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Catat ulang otomatis setiap periode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isRecurring, onCheckedChange = onRecurringChange)
        }

        if (isRecurring) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurringPeriod.entries.forEach { p ->
                    val selected = p == period
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onPeriodChange(p) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = p.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
