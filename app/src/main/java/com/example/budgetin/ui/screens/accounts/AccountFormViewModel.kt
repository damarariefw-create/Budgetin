package com.example.budgetin.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.model.Account
import com.example.budgetin.data.model.AccountType
import com.example.budgetin.data.model.SyncState
import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Form dompet/rekening: muat data lama saat edit, kelola cabang, simpan lalu sinkronkan. */
@HiltViewModel
class AccountFormViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _existing = MutableStateFlow<Account?>(null)
    val existing: StateFlow<Account?> = _existing.asStateFlow()

    /** Daftar Dompet Utama (dipakai label "Cabang dari ..."). */
    val mains: StateFlow<List<Account>> =
        repository.observeMains()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cabang milik rekening yang sedang diedit (hanya utk Dompet Utama). */
    val branches: StateFlow<List<Account>> =
        _existing
            .flatMapLatest { account ->
                if (account?.parentId == null && account != null) {
                    repository.observeChildren(account.id)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(accountId: String?) {
        if (accountId == null) return
        viewModelScope.launch {
            _existing.value = repository.getById(accountId)
        }
    }

    fun save(
        id: String?,
        name: String,
        type: AccountType,
        balance: Double,
        icon: String,
        color: Long,
        includeInTotal: Boolean,
        parentId: String?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            if (id == null) {
                repository.add(
                    Account(
                        name = name,
                        type = type,
                        balance = balance,
                        icon = icon,
                        color = color,
                        includeInTotal = includeInTotal,
                        parentId = parentId,
                        syncState = SyncState.PENDING,
                    )
                )
            } else {
                val old = _existing.value
                repository.update(
                    Account(
                        name = name,
                        type = type,
                        balance = balance,
                        icon = icon,
                        color = color,
                        includeInTotal = includeInTotal,
                        parentId = parentId,
                        isArchived = old?.isArchived ?: false,
                        syncState = SyncState.PENDING,
                        createdAt = old?.createdAt ?: timestamp,
                        updatedAt = timestamp,
                        id = id,
                    )
                )
            }
            syncRepository.syncAll()
            onSaved()
        }
    }

    /** Tambah cabang baru di dalam rekening induk [parentId] (nama + nominal). */
    fun addBranch(parentId: String, name: String, balance: Double) {
        viewModelScope.launch {
            val parent = _existing.value
            repository.add(
                Account(
                    name = name,
                    type = parent?.type ?: AccountType.CASH,
                    balance = balance,
                    icon = "📂",
                    color = parent?.color ?: 0xFF10B981,
                    includeInTotal = true,
                    parentId = parentId,
                    syncState = SyncState.PENDING,
                )
            )
            syncRepository.syncAll()
        }
    }

    /** Ubah nama & nominal cabang. */
    fun updateBranch(branch: Account, name: String, balance: Double) {
        viewModelScope.launch {
            repository.update(
                branch.copy(
                    name = name,
                    balance = balance,
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING,
                )
            )
            syncRepository.syncAll()
        }
    }

    /** Hapus cabang. */
    fun deleteBranch(branch: Account) {
        viewModelScope.launch {
            repository.delete(branch.id)
            syncRepository.syncAll()
        }
    }
}
