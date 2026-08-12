package com.example.budgetin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/** State UI untuk layar autentikasi. */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Status sesi real-time dari Supabase (menggerakkan routing auth). */
    val sessionStatus = supabase.auth.sessionStatus

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
            }.onSuccess {
                _uiState.value = AuthUiState.Idle
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.errorMessage())
            }
        }
    }

    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                    data = buildJsonObject { put("full_name", fullName.trim()) }
                }
            }.onSuccess { user ->
                // null = auto-confirm aktif (langsung login), bukan null = perlu verifikasi email
                if (user == null) {
                    _uiState.value = AuthUiState.Idle
                } else {
                    _uiState.value = AuthUiState.Error(
                        "Pendaftaran berhasil. Cek email kamu untuk konfirmasi akun."
                    )
                }
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.errorMessage())
            }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    private fun Throwable.errorMessage(): String {
        val msg = message ?: "Terjadi kesalahan. Coba lagi."
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Email atau kata sandi salah."
            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Email belum dikonfirmasi. Cek kotak masuk kamu."
            msg.contains("already registered", ignoreCase = true) ||
                msg.contains("already been registered", ignoreCase = true) ->
                "Email sudah terdaftar. Silakan masuk."
            msg.contains("Password should be at least", ignoreCase = true) ->
                "Kata sandi minimal 6 karakter."
            msg.contains("over_request_rate_limit", ignoreCase = true) ->
                "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
            else -> msg
        }
    }
}
