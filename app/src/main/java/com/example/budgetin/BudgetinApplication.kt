package com.example.budgetin

import android.app.Application
import com.example.budgetin.data.DataBootstrap
import com.example.budgetin.data.SessionDataManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class BudgetinApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            EntryPointAccessors
                .fromApplication(this@BudgetinApplication, BootstrapEntryPoint::class.java)
                .dataBootstrap()
                .run()
        }
        EntryPointAccessors
            .fromApplication(this@BudgetinApplication, BootstrapEntryPoint::class.java)
            .sessionDataManager()
            .start()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootstrapEntryPoint {
        fun dataBootstrap(): DataBootstrap
        fun sessionDataManager(): SessionDataManager
    }
}
