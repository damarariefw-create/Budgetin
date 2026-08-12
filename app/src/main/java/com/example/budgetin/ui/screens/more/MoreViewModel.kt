package com.example.budgetin.ui.screens.more

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetin.data.SessionDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionDataManager: SessionDataManager,
) : ViewModel() {

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<String?>(null)
    val syncResult: StateFlow<String?> = _syncResult.asStateFlow()

    fun dismissSyncResult() {
        _syncResult.value = null
    }

    /**
     * Paksa sinkron (push + pull) sekarang. Dipakai tombol "Sinkronkan" agar
     * hapusan/ubah data dari perangkat lain langsung ditarik tanpa menunggu
     * app masuk foreground / login ulang.
     */
    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResult.value = null
            val message = try {
                sessionDataManager.runSync()
                "Sinkron selesai ✓"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MoreViewModel", "Sinkron manual gagal", e)
                "Sinkron gagal: ${e.message ?: "periksa koneksi internet"}"
            } finally {
                _isSyncing.value = false
            }
            _syncResult.value = message
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isSigningOut.value = true
            runCatching { supabase.auth.signOut() }
            _isSigningOut.value = false
        }
    }
}
