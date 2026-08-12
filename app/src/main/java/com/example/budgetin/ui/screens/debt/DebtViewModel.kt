package com.example.budgetin.ui.screens.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Daftar hutang-piutang + ringkasan total. */
@HiltViewModel
class DebtViewModel @Inject constructor(
    private val repository: DebtRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val debts: StateFlow<List<Debt>> =
        repository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepository.observeAllIncludingArchived()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Nama dompet pinjaman & pelunasan, untuk tampilan di baris hutang/piutang. */
    fun accountName(debts: List<Debt>, accounts: List<Account>): Map<String, String> =
        debts.flatMap { listOfNotNull(it.accountId, it.settledAccountId) }
            .distinct()
            .associateWith { id -> accounts.firstOrNull { it.id == id }?.name ?: "" }

    /** Total hutang yang masih aktif (saya berhutang). */
    fun totalOwe(debts: List<Debt>): Double =
        debts.filter { it.type == DebtType.OWE && !it.isSettled }.sumOf { it.amount }

    /** Total piutang yang masih aktif (orang lain berhutang ke saya). */
    fun totalOwed(debts: List<Debt>): Double =
        debts.filter { it.type == DebtType.OWED && !it.isSettled }.sumOf { it.amount }

    fun toggleSettled(debt: Debt) {
        viewModelScope.launch {
            repository.settle(debt)
        }
    }

    fun delete(debt: Debt) {
        viewModelScope.launch {
            repository.delete(debt)
        }
    }
}
