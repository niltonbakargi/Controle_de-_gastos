package com.gastozen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gastozen.ui.AppViewModelFactory
import com.gastozen.ui.categorias.CategoriasScreen
import com.gastozen.ui.categorias.CategoriasViewModel
import com.gastozen.ui.configuracoes.ConfiguracoesScreen
import com.gastozen.ui.configuracoes.ConfiguracoesViewModel
import com.gastozen.ui.configuracoes.RecorrentesScreen
import com.gastozen.ui.configuracoes.RecorrentesViewModel
import com.gastozen.ui.dashboard.DashboardScreen
import com.gastozen.ui.dashboard.DashboardViewModel
import com.gastozen.ui.dashboard.GastosCategoriaScreen
import com.gastozen.ui.historico.HistoricoScreen
import com.gastozen.ui.historico.HistoricoViewModel
import com.gastozen.ui.comprovante.ReceberComprovanteScreen
import com.gastozen.ui.comprovante.ReceberComprovanteViewModel
import com.gastozen.ui.lancamento.ClassificarProdutosScreen
import com.gastozen.ui.lancamento.LancamentoScreen
import com.gastozen.ui.lancamento.LancamentoViewModel
import com.gastozen.ui.lancamento.QrCodeScreen
import com.gastozen.ui.lancamento.QrCodeViewModel
import com.gastozen.ui.metas.MetasScreen
import com.gastozen.ui.metas.MetasViewModel
import com.gastozen.ui.produtos.ProdutosCompradosScreen
import com.gastozen.ui.produtos.ProdutosCompradosViewModel

object Routes {
    const val DASHBOARD              = "dashboard"
    const val NOVO_LANCAMENTO        = "lancamento/novo"
    const val QR_CODE                = "lancamento/qrcode"
    const val CATEGORIAS             = "categorias"
    const val METAS                  = "metas"
    const val HISTORICO              = "historico"
    const val CONFIGURACOES          = "configuracoes"
    const val RECORRENTES            = "recorrentes"
    const val PRODUTOS_COMPRADOS     = "produtos_comprados"
    const val CLASSIFICAR_PRODUTOS   = "classificar_produtos/{lancamentoId}"
    const val GASTOS_CATEGORIA       = "gastos_categoria/{categoriaId}/{year}/{month}"
    const val RECEBER_COMPROVANTE    = "receber_comprovante"

    fun classificarProdutos(lancamentoId: Long) = "classificar_produtos/$lancamentoId"
    fun gastosCategoria(categoriaId: Long, year: Int, month: Int) =
        "gastos_categoria/$categoriaId/$year/$month"
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
                onVerLancamento = { },
                onQrCode = { navController.navigate(Routes.QR_CODE) },
                onProdutosComprados = { navController.navigate(Routes.PRODUTOS_COMPRADOS) },
                onCategoria = { catId, year, month ->
                    navController.navigate(Routes.gastosCategoria(catId, year, month))
                }
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
                onNfeDetected = { navController.popBackStack() },
                onClassificarProdutos = { lancamentoId ->
                    navController.navigate(Routes.classificarProdutos(lancamentoId)) {
                        popUpTo(Routes.QR_CODE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CLASSIFICAR_PRODUTOS,
            arguments = listOf(navArgument("lancamentoId") { type = NavType.LongType })
        ) { backStack ->
            val lancamentoId = backStack.arguments?.getLong("lancamentoId") ?: return@composable
            val vm = remember(lancamentoId) { factory.criarClassificarProdutosViewModel(lancamentoId) }
            ClassificarProdutosScreen(
                viewModel = vm,
                onConcluido = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.GASTOS_CATEGORIA,
            arguments = listOf(
                navArgument("categoriaId") { type = NavType.LongType },
                navArgument("year")        { type = NavType.IntType  },
                navArgument("month")       { type = NavType.IntType  }
            )
        ) { backStack ->
            val categoriaId = backStack.arguments?.getLong("categoriaId") ?: return@composable
            val year        = backStack.arguments?.getInt("year")         ?: return@composable
            val month       = backStack.arguments?.getInt("month")        ?: return@composable
            val vm = remember(categoriaId, year, month) {
                factory.criarGastosCategoriaViewModel(categoriaId, year, month)
            }
            GastosCategoriaScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRODUTOS_COMPRADOS) {
            val vm: ProdutosCompradosViewModel = viewModel(factory = factory)
            ProdutosCompradosScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
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

        composable(Routes.RECEBER_COMPROVANTE) {
            val vm: ReceberComprovanteViewModel = viewModel(factory = factory)
            ReceberComprovanteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
