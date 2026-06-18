package com.gastozen.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gastozen.data.db.AppDatabase
import com.gastozen.data.repository.*
import com.gastozen.domain.usecase.*
import com.gastozen.ui.categorias.CategoriasViewModel
import com.gastozen.ui.comprovante.ReceberComprovanteViewModel
import com.gastozen.ui.configuracoes.ConfiguracoesViewModel
import com.gastozen.ui.configuracoes.RecorrentesViewModel
import com.gastozen.ui.dashboard.DashboardViewModel
import com.gastozen.ui.dashboard.GastosCategoriaViewModel
import com.gastozen.ui.historico.HistoricoViewModel
import com.gastozen.ui.lancamento.ClassificarProdutosViewModel
import com.gastozen.ui.lancamento.LancamentoViewModel
import com.gastozen.ui.lancamento.QrCodeViewModel
import com.gastozen.ui.metas.MetasViewModel
import com.gastozen.ui.produtos.ProdutosCompradosViewModel

class AppViewModelFactory(
    private val db: AppDatabase,
    private val application: Application
) : ViewModelProvider.Factory {

    private val contaRepo      by lazy { ContaRepository(db.contaDao()) }
    private val categoriaRepo  by lazy { CategoriaRepository(db.categoriaDao()) }
    private val lancamentoRepo by lazy { LancamentoRepository(db.lancamentoDao()) }
    private val produtoRepo    by lazy { ProdutoCompradoRepository(db.produtoCompradoDao()) }
    private val regraRepo      by lazy { RegraCategoriaRepository(db.regraCategoriaDao()) }
    private val recorrenteRepo by lazy { RecorrenteRepository(db.recorrenteDao()) }

    private val criarUseCase  by lazy { CriarLancamentoUseCase(lancamentoRepo, produtoRepo) }
    private val regraUseCase  by lazy { SalvarRegraCategoriaUseCase(regraRepo) }
    private val classificarUseCase by lazy { ClassificarProdutoUseCase(produtoRepo, regraUseCase) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(lancamentoRepo) as T

            modelClass.isAssignableFrom(LancamentoViewModel::class.java) ->
                LancamentoViewModel(criarUseCase, contaRepo, categoriaRepo, recorrenteRepo) as T

            modelClass.isAssignableFrom(QrCodeViewModel::class.java) ->
                QrCodeViewModel(categoriaRepo, regraRepo, produtoRepo, criarUseCase, regraUseCase) as T

            modelClass.isAssignableFrom(CategoriasViewModel::class.java) ->
                CategoriasViewModel(categoriaRepo) as T

            modelClass.isAssignableFrom(MetasViewModel::class.java) ->
                MetasViewModel(categoriaRepo, lancamentoRepo) as T

            modelClass.isAssignableFrom(HistoricoViewModel::class.java) ->
                HistoricoViewModel(lancamentoRepo) as T

            modelClass.isAssignableFrom(ConfiguracoesViewModel::class.java) ->
                ConfiguracoesViewModel(lancamentoRepo) as T

            modelClass.isAssignableFrom(RecorrentesViewModel::class.java) ->
                RecorrentesViewModel(recorrenteRepo) as T

            modelClass.isAssignableFrom(ProdutosCompradosViewModel::class.java) ->
                ProdutosCompradosViewModel(produtoRepo) as T

            modelClass.isAssignableFrom(ReceberComprovanteViewModel::class.java) ->
                ReceberComprovanteViewModel(application, categoriaRepo, criarUseCase) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    /** ClassificarProdutosViewModel precisa de lancamentoId — criado fora do ciclo padrão */
    fun criarClassificarProdutosViewModel(lancamentoId: Long): ClassificarProdutosViewModel =
        ClassificarProdutosViewModel(lancamentoId, produtoRepo, categoriaRepo, classificarUseCase)

    /** GastosCategoriaViewModel precisa de categoriaId + período */
    fun criarGastosCategoriaViewModel(categoriaId: Long, year: Int, month: Int): GastosCategoriaViewModel =
        GastosCategoriaViewModel(categoriaId, year, month, lancamentoRepo, produtoRepo, categoriaRepo)
}
