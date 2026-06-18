package com.gastozen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gastozen.ui.AppViewModelFactory
import com.gastozen.ui.categorias.CategoriasScreen
import com.gastozen.ui.categorias.CategoriasViewModel
import com.gastozen.ui.configuracoes.ConfiguracoesScreen
import com.gastozen.ui.configuracoes.ConfiguracoesViewModel
import com.gastozen.ui.configuracoes.RecorrentesScreen
import com.gastozen.ui.configuracoes.RecorrentesViewModel
import com.gastozen.ui.dashboard.DashboardScreen
import com.gastozen.ui.dashboard.DashboardViewModel
import com.gastozen.ui.historico.HistoricoScreen
import com.gastozen.ui.historico.HistoricoViewModel
import com.gastozen.ui.lancamento.LancamentoScreen
import com.gastozen.ui.lancamento.LancamentoViewModel
import com.gastozen.ui.lancamento.QrCodeScreen
import com.gastozen.ui.lancamento.QrCodeViewModel
import com.gastozen.ui.metas.MetasScreen
import com.gastozen.ui.metas.MetasViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val NOVO_LANCAMENTO = "lancamento/novo"
    const val QR_CODE = "lancamento/qrcode"
    const val CATEGORIAS = "categorias"
    const val METAS = "metas"
    const val HISTORICO = "historico"
    const val CONFIGURACOES = "configuracoes"
    const val RECORRENTES = "recorrentes"
}

@Composable
fun MainNavGraph(
    navController: NavHostController,
    factory: AppViewModelFactory,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier
    ) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(
                viewModel = vm,
                onNovoLancamento = { navController.navigate(Routes.NOVO_LANCAMENTO) },
                onVerLancamento = { /* TODO detail screen */ },
                onQrCode = { navController.navigate(Routes.QR_CODE) }
            )
        }

        composable(Routes.NOVO_LANCAMENTO) {
            val vm: LancamentoViewModel = viewModel(factory = factory)
            LancamentoScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QR_CODE) {
            val vm: QrCodeViewModel = viewModel(factory = factory)
            QrCodeScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNfeDetected = { navController.popBackStack() }
            )
        }

        composable(Routes.CATEGORIAS) {
            val vm: CategoriasViewModel = viewModel(factory = factory)
            CategoriasScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.METAS) {
            val vm: MetasViewModel = viewModel(factory = factory)
            MetasScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORICO) {
            val vm: HistoricoViewModel = viewModel(factory = factory)
            HistoricoScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onVerLancamento = { }
            )
        }

        composable(Routes.CONFIGURACOES) {
            val vm: ConfiguracoesViewModel = viewModel(factory = factory)
            ConfiguracoesScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onCategorias = { navController.navigate(Routes.CATEGORIAS) },
                onRecorrentes = { navController.navigate(Routes.RECORRENTES) }
            )
        }

        composable(Routes.RECORRENTES) {
            val vm: RecorrentesViewModel = viewModel(factory = factory)
            RecorrentesScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
