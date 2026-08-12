package com.example.budgetin.data

import com.example.budgetin.data.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inisialisasi data global saat aplikasi start: kategori default.
 * Data milik user (dompet/transaksi/hutang) dikelola [SessionDataManager]
 * mengikuti user yang sedang login.
 * Di-*inject* lewat EntryPoint pada Application agar jalan sekali per proses.
 */
@Singleton
class DataBootstrap @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun run() {
        categoryRepository.ensureSeeded()
        categoryRepository.reconcileFromRemote()
    }
}
