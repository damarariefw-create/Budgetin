package com.example.budgetin.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Memaparkan status sesi Supabase agar UI bisa memilih antara
 * layar autentikasi dan konten utama (splash → auth → main).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    supabase: SupabaseClient,
) : ViewModel() {
    val sessionStatus: StateFlow<SessionStatus> = supabase.auth.sessionStatus
}
