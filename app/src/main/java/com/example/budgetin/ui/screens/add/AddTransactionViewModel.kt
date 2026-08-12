package com.example.budgetin.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.Category
import com.example.budgetin.data.model.Debt
import com.example.budgetin.data.model.DebtType
import com.example.budgetin.data.model.RecurringPeriod
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.model.Transaction
import com.example.budgetin.data.model.TransactionType
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.CategoryRepository
import com.example.budgetin.data.repository.DebtRepository
import com.example.budgetin.data.repository.SyncRepository
import com.example.budgetin.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Menyimpan/mengubah transaksi, transfer, hutang-piutang + memicu sinkronisasi. */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val debtRepository: DebtRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    /** Transaksi yang sedang diedit (di layar edit). Null saat mode tambah. */
    private val _editing = MutableStateFlow<Transaction?>(null)
    val editing: StateFlow<Transaction?> = _editing.asStateFlow()

    fun loadForEdit(id: String) {
        viewModelScope.launch {
            _editing.value = transactionRepository.getById(id)
        }
    }

    fun clearEdit() {
        _editing.value = null
    }

    /** Dompet Utama (Main) saja — branch tidak ditampilkan langsung di pilihan akun. */
    val accounts: StateFlow<List<Account>> =
        accountRepository.observeMains()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Semua dompet (Main + Branch) untuk resolusi objek akun saat menyimpan. */
    val allAccounts: StateFlow<List<Account>> =
        accountRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Branch per dompet induk, untuk pilihan akun bertingkat (main → branch). */
    val branchesByParent: StateFlow<Map<String, List<Account>>> =
        accountRepository.observeAll()
            .map { all -> all.filter { it.parentId != null }.groupBy { it.parentId!! } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val expenseCategories: StateFlow<List<Category>> =
        categoryRepository.observeByType(TransactionType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<Category>> =
        categoryRepository.observeByType(TransactionType.INCOME)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTransaction(
        type: TransactionType,
        amount: Double,
        adminFee: Double,
        category: Category,
        account: Account,
        note: String,
        timestamp: Long,
        isRecurring: Boolean,
        recurringPeriod: RecurringPeriod?,
        isCompleted: Boolean = true,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            transactionRepository.add(
                Transaction(
                    type = type,
                    amount = amount,
                    adminFee = adminFee,
                    category = category.name,
                    categoryId = category.id,
                    accountId = account.id,
                    note = note.trim(),
                    timestamp = timestamp,
                    isRecurring = isRecurring,
                    recurringPeriod = recurringPeriod,
                    isCompleted = isCompleted,
                    syncState = SyncState.PENDING,
                )
            )
            syncRepository.syncAll()
            onSaved()
        }
    }

    fun saveTransfer(
        amount: Double,
        adminFee: Double,
        from: Account,
        to: Account,
        note: String,
        timestamp: Long,
        isCompleted: Boolean = true,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            transactionRepository.add(
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    adminFee = adminFee,
                    category = "Transfer",
                    accountId = from.id,
                    transferToAccountId = to.id,
                    note = note.trim(),
                    timestamp = timestamp,
                    isCompleted = isCompleted,
                    syncState = SyncState.PENDING,
                )
            )
            syncRepository.syncAll()
            onSaved()
        }
    }

    fun updateTransaction(
        transactionId: String,
        type: TransactionType,
        amount: Double,
        adminFee: Double,
        category: Category,
        account: Account,
        note: String,
        timestamp: Long,
        isRecurring: Boolean,
        recurringPeriod: RecurringPeriod?,
        isCompleted: Boolean = true,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val existing = transactionRepository.getById(transactionId) ?: return@launch
            transactionRepository.update(
                existing.copy(
                    type = type,
                    amount = amount,
                    adminFee = adminFee,
                    category = category.name,
                    categoryId = category.id,
                    accountId = account.id,
                    note = note.trim(),
                    timestamp = timestamp,
                    isRecurring = isRecurring,
                    recurringPeriod = recurringPeriod,
                    isCompleted = isCompleted,
                    syncState = SyncState.PENDING,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            syncRepository.syncAll()
            onSaved()
        }
    }

    fun updateTransfer(
        transactionId: String,
        amount: Double,
        adminFee: Double,
        from: Account,
        to: Account,
        note: String,
        timestamp: Long,
        isCompleted: Boolean = true,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val existing = transactionRepository.getById(transactionId) ?: return@launch
            transactionRepository.update(
                existing.copy(
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    adminFee = adminFee,
                    category = "Transfer",
                    accountId = from.id,
                    transferToAccountId = to.id,
                    note = note.trim(),
                    timestamp = timestamp,
                    isCompleted = isCompleted,
                    syncState = SyncState.PENDING,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            syncRepository.syncAll()
            onSaved()
        }
    }

    fun saveDebt(
        name: String,
        type: DebtType,
        amount: Double,
        note: String,
        dueDate: Long?,
        accountId: String?,
        settledAccountId: String?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            debtRepository.add(
                Debt(
                    counterpartName = name.trim(),
                    type = type,
                    amount = amount,
                    note = note.trim(),
                    dueDate = dueDate,
                    accountId = accountId,
                    settledAccountId = settledAccountId,
                    syncState = SyncState.PENDING,
                )
            )
            syncRepository.syncAll()
            onSaved()
        }
    }
}
