package com.example.budgetin.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budgetin.ui.auth.AuthScreen
import com.example.budgetin.ui.components.QuickAction
import com.example.budgetin.ui.components.QuickActionSheet
import com.example.budgetin.ui.screens.accounts.AccountFormScreen
import com.example.budgetin.ui.screens.accounts.AccountsScreen
import com.example.budgetin.ui.screens.add.AddTransactionScreen
import com.example.budgetin.ui.screens.add.EditTransactionScreen
import com.example.budgetin.ui.screens.dashboard.DashboardScreen
import com.example.budgetin.ui.screens.debt.DebtScreen
import com.example.budgetin.ui.screens.history.HistoryScreen
import com.example.budgetin.ui.screens.more.MoreScreen
import com.example.budgetin.ui.screens.statistics.StatisticsScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Titik masuk aplikasi: gerbang autentikasi.
 * - Initializing -> splash
 * - Authenticated -> MainApp (bottom nav)
 * - lainnya (NotAuthenticated/RefreshFailure) -> AuthScreen
 */
@Composable
fun BudgetinApp(sessionStatus: StateFlow<SessionStatus>) {
    val status by sessionStatus.collectAsStateWithLifecycle()

    when (status) {
        is SessionStatus.Initializing -> SplashScreen()
        is SessionStatus.Authenticated -> MainApp()
        else -> AuthScreen()
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTIONS_FILTERED = "transactions/{accountId}"
    const val MORE = "more"
    const val STATISTICS = "statistics"
    const val ADD_TRANSACTION = "add"
    const val EDIT_TRANSACTION = "edit/{txId}"
    const val DEBTS = "debts"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_FORM = "account_form"
    const val ACCOUNT_EDIT = "account_form/{accountId}"
}

/** Tab utama pada bottom bar. */
private data class TabItem(
    val route: String,
    val emoji: String,
    val label: String,
)

private val tabs = listOf(
    TabItem(Routes.DASHBOARD, "🏠", "Beranda"),
    TabItem(Routes.TRANSACTIONS, "📋", "Transaksi"),
    TabItem(Routes.MORE, "⚙️", "Lainnya"),
)

/** Konten utama (hanya tampil setelah login sukses). */
@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainScreen = tabs.any { it.route == currentRoute }
    var showQuickActions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isMainScreen) {
                BottomBar(currentRoute = currentRoute, navController = navController)
            }
        },
        floatingActionButton = {
            if (isMainScreen) {
                FloatingActionButton(
                    onClick = { showQuickActions = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aksi cepat")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(220)) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenHistory = { navController.navigate(Routes.TRANSACTIONS) },
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenAccountTransactions = { account ->
                        navController.navigate("transactions/${account.id}") {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.TRANSACTIONS) {
                HistoryScreen(
                    onEditTransaction = { transaction ->
                        navController.navigate("edit/${transaction.id}")
                    },
                )
            }
            composable(
                route = Routes.TRANSACTIONS_FILTERED,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            ) { entry ->
                HistoryScreen(
                    accountId = entry.arguments?.getString("accountId"),
                    onClearFilter = { navController.popBackStack() },
                    onEditTransaction = { transaction ->
                        navController.navigate("edit/${transaction.id}")
                    },
                )
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    onAddAccount = { navController.navigate(Routes.ACCOUNT_FORM) },
                    onEditAccount = { account ->
                        navController.navigate("${Routes.ACCOUNT_FORM}/${account.id}")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ACCOUNT_FORM) {
                AccountFormScreen(
                    accountId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ACCOUNT_EDIT,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            ) { entry ->
                AccountFormScreen(
                    accountId = entry.arguments?.getString("accountId"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                    onOpenDebts = { navController.navigate(Routes.DEBTS) },
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                )
            }
            composable(Routes.DEBTS) {
                DebtScreen(
                    onAddDebt = {
                        navController.navigate("${Routes.ADD_TRANSACTION}/debt") {
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.STATISTICS) { StatisticsScreen() }
            composable(
                route = "${Routes.ADD_TRANSACTION}/{txType}",
                arguments = listOf(navArgument("txType") { type = NavType.StringType }),
            ) { entry ->
                val txType = entry.arguments?.getString("txType") ?: "expense"
                AddTransactionScreen(
                    mode = txType,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Routes.EDIT_TRANSACTION,
                arguments = listOf(navArgument("txId") { type = NavType.StringType }),
            ) { entry ->
                val txId = entry.arguments?.getString("txId") ?: return@composable
                EditTransactionScreen(
                    transactionId = txId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }

    if (showQuickActions) {
        QuickActionSheet(
            onDismiss = { showQuickActions = false },
            onAction = { action: QuickAction ->
                showQuickActions = false
                navController.navigate("${Routes.ADD_TRANSACTION}/${action.route}") {
                    launchSingleTop = true
                }
            },
        )
    }
}

@Composable
private fun BottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Text(
                        text = tab.emoji,
                        fontSize = 20.sp,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}
