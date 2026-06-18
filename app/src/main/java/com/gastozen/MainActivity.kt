package com.gastozen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gastozen.data.db.AppDatabase
import com.gastozen.ui.AppViewModelFactory
import com.gastozen.ui.theme.GastoZenTheme
import com.gastozen.util.NotificationHelper
import com.gastozen.util.PendingComprovante
import com.gastozen.util.PendingPix
import com.gastozen.util.PixReceiptParser
import com.gastozen.worker.RecorrenteWorker

class MainActivity : ComponentActivity() {

    private lateinit var factory: AppViewModelFactory

    // Flags observáveis pelo Compose — garantem recomposição via onNewIntent também
    private var pixPendente by mutableStateOf(false)
    private var comprovantePendente by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(this)
        factory = AppViewModelFactory(db, application)
        NotificationHelper.createChannels(this)
        RecorrenteWorker.schedule(this)

        handleIncomingIntent(intent)

        setContent {
            GastoZenTheme {
                GastoZenApp(
                    factory = factory,
                    pixPendente = pixPendente,
                    onPixHandled = { pixPendente = false },
                    comprovantePendente = comprovantePendente,
                    onComprovanteHandled = { comprovantePendente = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return

        when {
            intent.type == "text/plain" -> {
                val texto = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                val recibo = PixReceiptParser.parse(texto) ?: return
                PendingPix.set(recibo)
                pixPendente = true
            }
            intent.type?.startsWith("image/") == true -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) ?: return
                PendingComprovante.set(PendingComprovante.Content.Imagem(uri))
                comprovantePendente = true
            }
            intent.type == "application/pdf" -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) ?: return
                PendingComprovante.set(PendingComprovante.Content.Pdf(uri))
                comprovantePendente = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoZenApp(
    factory: AppViewModelFactory,
    pixPendente: Boolean,
    onPixHandled: () -> Unit,
    comprovantePendente: Boolean,
    onComprovanteHandled: () -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val bottomItems = listOf(
        Triple(Routes.DASHBOARD,      Icons.Default.Home,        "Início"),
        Triple(Routes.HISTORICO,      Icons.Default.History,     "Histórico"),
        Triple(Routes.CARTOES,        Icons.Default.CreditCard,  "Cartões"),
        Triple(Routes.DESPESAS_FIXAS, Icons.Default.Receipt,     "Fixas"),
        Triple(Routes.CONFIGURACOES,  Icons.Default.Settings,    "Config")
    )

    val showBottomBar = currentRoute in bottomItems.map { it.first }

    // Navega para o lançamento assim que houver PIX pendente
    LaunchedEffect(pixPendente) {
        if (pixPendente) {
            navController.navigate(Routes.NOVO_LANCAMENTO)
            onPixHandled()
        }
    }

    // Navega para a tela de comprovante (imagem/PDF)
    LaunchedEffect(comprovantePendente) {
        if (comprovantePendente) {
            navController.navigate(Routes.RECEBER_COMPROVANTE)
            onComprovanteHandled()
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
