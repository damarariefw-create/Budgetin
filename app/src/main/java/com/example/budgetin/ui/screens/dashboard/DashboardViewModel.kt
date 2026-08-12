package com.example.budgetin.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.SessionDataManager
import com.example.budgetin.data.local.CategoryTotal
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.TransactionRepository
import com.example.budgetin.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Calendar
import javax.inject.Inject

/** Ringkasan satu halaman dashboard: saldo, ringkasan bulan ini, transaksi terakhir. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: TransactionRepository,
    accountRepository: AccountRepository,
    private val sessionDataManager: SessionDataManager,
    supabase: SupabaseClient,
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val monthStart = DateUtil.startOfMonth(now).timeInMillis
    private val monthEnd = DateUtil.endOfMonth(now).timeInMillis

    val monthLabel: String = DateUtil.monthLabel(now)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Saldo total = jumlah saldo Dompet Utama saja (sudah termasuk cabangnya). */
    val balance: StateFlow<Double> =
        accountRepository.observeMainBalances()
            .map { pairs -> pairs.filter { it.first.includeInTotal }.sumOf { it.second } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** Daftar Dompet Utama aktif + saldo gabungan (untuk carousel di Beranda). */
    val accounts: StateFlow<List<Pair<Account, Double>>> =
        accountRepository.observeMainBalances()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saldo milik sendiri tiap akun (tanpa agregasi branch) — untuk detail dompet. */
    val ownBalances: StateFlow<List<Pair<Account, Double>>> =
        accountRepository.observeOwnBalances()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthIncome: StateFlow<Double> =
        repository.observeIncomeBetween(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val monthExpense: StateFlow<Double> =
        repository.observeExpenseBetween(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> =
        repository.observeRecent(5)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pengeluaran per kategori bulan ini, terurut dari terbesar. */
    val expenseByCategory: StateFlow<List<CategoryTotal>> =
        repository.observeExpenseByCategory(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Nama depan pengguna dari profil Supabase (untuk sapaan di Beranda). */
    val userName: StateFlow<String> = MutableStateFlow(
        supabase.auth.currentUserOrNull()?.let { user ->
            user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?.substringBefore(" ")
                ?: user.email?.substringBefore("@")
        }.orEmpty()
    ).asStateFlow()

    /** Pull-to-refresh: kirim data lokal yang belum terkirim lalu tarik data terbaru. */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { sessionDataManager.runSync() }
            _isRefreshing.value = false
        }
    }
}
