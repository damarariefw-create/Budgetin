package com.example.budgetin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.budgetin.data.SessionDataManager
import com.example.budgetin.ui.navigation.BudgetinApp
import com.example.budgetin.ui.navigation.SessionViewModel
import com.example.budgetin.ui.theme.BudgetinTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject lateinit var sessionDataManager: SessionDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Auto-sinkron setiap kali app kembali ke foreground
        // (push + pull ke Supabase), agar data selalu terkini.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionDataManager.syncNow()
            }
        }
        setContent {
            BudgetinTheme {
                BudgetinApp(sessionStatus = sessionViewModel.sessionStatus)
            }
        }
    }
}
