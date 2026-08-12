package com.example.budgetin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sumber ID user yang sedang login.
 * - [idFlow]: berubah otomatis saat user ganti, dipakai untuk query real-time.
 * - [id]: nilai seketika untuk operasi insert/update.
 * Saat belum login, keduanya menghasilkan string kosong (data tidak tampil).
 */
@Singleton
class CurrentUser @Inject constructor(
    supabase: SupabaseClient,
) {
    private val auth = supabase.auth

    val idFlow: Flow<String> = auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id ?: "" }
        .distinctUntilChanged()

    fun id(): String = auth.currentUserOrNull()?.id ?: ""
}
