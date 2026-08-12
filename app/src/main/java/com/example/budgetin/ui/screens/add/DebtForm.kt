package com.example.budgetin.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.ui.components.CalculatorAmountField
import com.example.budgetin.ui.theme.ExpenseRed
import com.example.budgetin.ui.theme.IncomeGreen
import com.example.budgetin.ui.theme.ResponsiveContainer

/** Form Hutang & Piutang: nama lawan, arah, dompet terkait, nominal (keypad), jatuh tempo opsional. */
@Composable
fun DebtForm(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val branchesByParent by viewModel.branchesByParent.collectAsStateWithLifecycle()
    var debtType by rememberSaveable { mutableStateOf(DebtType.OWED) }
    var name by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf<String?>(null) }
    var settledAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var hasDueDate by rememberSaveable { mutableStateOf(false) }
    var dueDate by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val amount = com.example.budgetin.util.Calculator.evaluate(amountText) ?: 0.0
    val valid = amount > 0 && name.isNotBlank() && !isSaving

    ResponsiveContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        FormHeader("Hutang & Piutang 🧾", onBack)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TypeToggle(
                selectedLabel = debtType.label,
                options = listOf(
                    DebtType.OWED.label to IncomeGreen,
                    DebtType.OWE.label to ExpenseRed,
                ),
                onSelect = { label ->
                    debtType = if (label == DebtType.OWED.label) DebtType.OWED else DebtType.OWE
                },
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama / Pihak") },
                placeholder = { Text("Contoh: Budi, Toko ABC") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            CalculatorAmountField(
                value = amountText,
                onValueChange = { raw -> amountText = raw.take(80) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dompet pinjaman (opsional)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (debtType == DebtType.OWED)
                        "Dana dipinjamkan diambil dari dompet ini."
                    else
                        "Uang pinjaman masuk ke dompet ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AccountBranchPicker(
                    mains = accounts,
                    branchesByParent = branchesByParent,
                    selectedId = accountId,
                    onSelect = { id -> accountId = if (accountId == id) null else id },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dompet pelunasan (opsional)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (debtType == DebtType.OWED)
                        "Uang yang dikembalikan masuk ke dompet ini."
                    else
                        "Pelunasan diambil dari dompet ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AccountBranchPicker(
                    mains = accounts,
                    branchesByParent = branchesByParent,
                    selectedId = settledAccountId,
                    onSelect = { id -> settledAccountId = if (settledAccountId == id) null else id },
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Contoh: Cicilan bulan ini") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Jatuh tempo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Opsional",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = hasDueDate,
                    onCheckedChange = { hasDueDate = it },
                )
            }

            if (hasDueDate) {
                DateSelector(selected = dueDate, onSelect = { dueDate = it })
            }

            Button(
                onClick = {
                    if (valid) {
                        isSaving = true
                        viewModel.saveDebt(
                            name = name,
                            type = debtType,
                            amount = amount,
                            note = note,
                            dueDate = if (hasDueDate) dueDate else null,
                            accountId = accountId,
                            settledAccountId = settledAccountId,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (debtType == DebtType.OWED) IncomeGreen else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (debtType == DebtType.OWED) "Simpan Piutang" else "Simpan Hutang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}
