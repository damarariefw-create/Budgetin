package com.example.budgetin.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.CategoryRepository
import com.example.budgetin.data.repository.SyncRepository
import com.example.budgetin.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Kriteria filter lanjutan riwayat transaksi; semua field opsional. */
data class HistoryFilter(
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val type: TransactionType? = null,
    val accountId: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val categoryId: String? = null,
    val isCompleted: Boolean? = null,
) {
    /** Berapa banyak kriteria aktif (untuk badge pada tombol Filter). */
    val activeCount: Int =
        listOf(dateFrom, dateTo, type, accountId, minAmount, maxAmount, categoryId, isCompleted)
            .count { it != null }
}

/** Riwayat transaksi dengan filter lanjutan (tanggal, jenis, dompet, nominal, status) + aksi hapus. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    /** Nama dompet yang difilter dari layar lain (null = semua transaksi). */
    private val filterName = MutableStateFlow<String?>(null)
    val filterAccountName: StateFlow<String?> = filterName.asStateFlow()

    /** Dompet Utama (Main) untuk filter dompet. */
    val mains: StateFlow<List<Account>> =
        accountRepository.observeMains()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Branch per dompet induk, untuk filter bertingkat (dompet → branch). */
    val branchesByParent: StateFlow<Map<String, List<Account>>> =
        accountRepository.observeAll()
            .map { all -> all.filter { it.parentId != null }.groupBy { it.parentId!! } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val categories: StateFlow<List<Category>> =
        categoryRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<Transaction>> =
        combine(repository.observeAll(), _filter) { all, filter -> all.filter { matches(it, filter) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun matches(t: Transaction, f: HistoryFilter): Boolean {
        if (f.dateFrom != null && t.timestamp < f.dateFrom) return false
        if (f.dateTo != null && t.timestamp > f.dateTo) return false
        if (f.type != null && t.type != f.type) return false
        if (f.accountId != null && t.accountId != f.accountId && t.transferToAccountId != f.accountId) {
            return false
        }
        val value = if (t.type == TransactionType.INCOME) t.amount - t.adminFee else t.amount + t.adminFee
        if (f.minAmount != null && value < f.minAmount) return false
        if (f.maxAmount != null && value > f.maxAmount) return false
        if (f.categoryId != null && t.categoryId != f.categoryId) return false
        if (f.isCompleted != null && t.isCompleted != f.isCompleted) return false
        return true
    }

    /** Terapkan filter dompet dari layar lain; [accountId] null = tampilkan semua. */
    fun setFilter(accountId: String?) {
        if (accountId == null) {
            _filter.value = _filter.value.copy(accountId = null)
            filterName.value = null
            return
        }
        _filter.value = _filter.value.copy(accountId = accountId)
        viewModelScope.launch {
            filterName.value = accountRepository.getById(accountId)?.name
        }
    }

    fun setType(type: TransactionType?) { _filter.value = _filter.value.copy(type = type) }
    fun setAccount(accountId: String?) { _filter.value = _filter.value.copy(accountId = accountId) }
    fun setIsCompleted(completed: Boolean?) { _filter.value = _filter.value.copy(isCompleted = completed) }
    fun setCategory(categoryId: String?) { _filter.value = _filter.value.copy(categoryId = categoryId) }

    fun setDateRange(from: Long?, to: Long?) {
        _filter.value = _filter.value.copy(dateFrom = from, dateTo = to)
    }

    fun setMinAmount(value: Double?) { _filter.value = _filter.value.copy(minAmount = value) }
    fun setMaxAmount(value: Double?) { _filter.value = _filter.value.copy(maxAmount = value) }

    fun reset() {
        _filter.value = HistoryFilter()
        filterName.value = null
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            // Langsung kirim penghapusan ke server agar saldo dompet tetap
            // sesuai dan transaksi tidak ditarik kembali saat sinkron berikutnya.
            syncRepository.syncAll()
        }
    }
}
