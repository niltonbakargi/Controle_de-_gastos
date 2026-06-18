package com.gastozen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gastozen.data.db.AppDatabase
import com.gastozen.ui.AppViewModelFactory
import com.gastozen.ui.lancamento.LancamentoViewModel
import com.gastozen.ui.theme.GastoZenTheme
import com.gastozen.util.NotificationHelper
import com.gastozen.util.PixParser
import com.gastozen.worker.RecorrenteWorker

class MainActivity : ComponentActivity() {

    private lateinit var factory: AppViewModelFactory
    private var pixSharedText: String? = null
    private var pixSharedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init
        val db = AppDatabase.getInstance(this)
        factory = AppViewModelFactory(db)
        NotificationHelper.createChannels(this)
        RecorrenteWorker.schedule(this)

        // Handle share intent
        handleIncomingIntent(intent)

        setContent {
            GastoZenTheme {
                GastoZenApp(
                    factory = factory,
                    pixText = pixSharedText,
                    onPixHandled = { pixSharedText = null; pixSharedImageUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    pixSharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                } else if (intent.type?.startsWith("image/") == true) {
                    @Suppress("DEPRECATION")
                    pixSharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoZenApp(
    factory: AppViewModelFactory,
    pixText: String?,
    onPixHandled: () -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Bottom navigation items
    val bottomItems = listOf(
        Triple(Routes.DASHBOARD, Icons.Default.Home, "Início"),
        Triple(Routes.HISTORICO, Icons.Default.History, "Histórico"),
        Triple(Routes.METAS, Icons.Default.Flag, "Metas"),
        Triple(Routes.CONFIGURACOES, Icons.Default.Settings, "Config")
    )

    val showBottomBar = currentRoute in bottomItems.map { it.first }

    // Navigate to lancamento if PIX shared
    LaunchedEffect(pixText) {
        if (pixText != null) {
            navController.navigate(Routes.NOVO_LANCAMENTO)
            onPixHandled()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            factory = factory,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
