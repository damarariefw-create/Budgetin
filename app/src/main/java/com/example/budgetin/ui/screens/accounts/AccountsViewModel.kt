package com.example.budgetin.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Daftar dompet/rekening (aktif & arsip) beserta saldo berjalan. */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    val accounts: StateFlow<List<Pair<Account, Double>>> =
        repository.observeBalancesIncludingArchived()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total saldo = jumlah Dompet Utama (Main) saja; saldo main sudah gabungan branch-nya. */
    fun totalBalance(accounts: List<Pair<Account, Double>>): Double =
        accounts.filter { !it.first.isArchived && it.first.parentId == null && it.first.includeInTotal }
            .sumOf { it.second }

    fun archive(account: Account) {
        viewModelScope.launch { repository.archive(account.id) }
    }

    fun restore(account: Account) {
        viewModelScope.launch { repository.restore(account.id) }
    }

    fun delete(account: Account) {
        viewModelScope.launch {
            repository.delete(account.id)
            // Langsung kirim penghapusan ke server agar dompet & transaksinya
            // tidak ditarik kembali saat sinkron berikutnya / di perangkat lain.
            syncRepository.syncAll()
        }
    }
}
