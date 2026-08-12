package com.example.budgetin.data

import com.example.budgetin.data.repository.AccountRepository
import com.example.budgetin.data.repository.CategoryRepository
import com.example.budgetin.data.repository.DebtRepository
import com.example.budgetin.data.repository.SyncRepository
import com.example.budgetin.data.repository.TransactionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menjaga data lokal selaras dengan user yang sedang login.
 * Saat sesi berubah ke user baru (atau sesi pulih saat app start):
 * - aduk data lama tanpa user (hasil migrasi) ke user tsb,
 * - kirim (push) perubahan lokal yang belum terkirim ke Supabase,
 * - tarik (pull) dompet, transaksi, hutang-piutang & kategori custom dari server
 *   agar semua perangkat dengan akun yang sama selalu sama isinya,
 * - buat akun "Dompet" default hanya bila user benar-benar baru.
 *
 * Dipanggil [start] dari Application agar mengikuti login/logout selama app hidup,
 * dan [syncNow] saat app masuk ke foreground / tombol "Sinkronkan" ditekan.
 */
@Singleton
class SessionDataManager @Inject constructor(
    supabase: SupabaseClient,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val debtRepository: DebtRepository,
    private val categoryRepository: CategoryRepository,
    private val syncRepository: SyncRepository,
) {
    private val auth = supabase.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    fun start() {
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    runSync()
                }
            }
        }
    }

    /** Sinkronkan (push + pull) sekali; aman dipanggil berulang dari mana saja. */
    suspend fun runSync() {
        syncMutex.withLock { prepareUserData() }
    }

    /** Pemicu sinkron fire-and-forget (dipakai auto-sync saat app masuk foreground). */
    fun syncNow() {
        scope.launch { runSync() }
    }

    private suspend fun prepareUserData() {
        accountRepository.adoptOrphans()
        transactionRepository.adoptOrphans()
        debtRepository.adoptOrphans()
        // 1) Kirim perubahan lokal yang belum terkirim ke server.
        syncRepository.syncAll()
        // 2) Selaraskan kategori default & tarik kategori custom supaya nama
        //    kategori transaksi hasil pull akurat.
        categoryRepository.reconcileFromRemote()
        categoryRepository.reconcileCustomFromRemote()
        // 3) Tarik dompet & transaksi dari server agar semua perangkat dengan
        //    akun yang sama selalu selaras (multi-perangkat), lalu lengkapi nama
        //    kategori transaksi yang mungkin masih kosong.
        accountRepository.reconcileFromRemote()
        transactionRepository.reconcileFromRemote()
        syncRepository.backfillTransactionCategories()
        debtRepository.reconcileFromRemote()
        // 4) Buat dompet default hanya bila user benar-benar baru, lalu kirim.
        accountRepository.ensureDefault()
        syncRepository.syncAll()
    }
}
